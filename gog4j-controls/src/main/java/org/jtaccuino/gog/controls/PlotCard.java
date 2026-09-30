/*
 * Copyright 2026 JTaccuino project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jtaccuino.gog.controls;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.CacheHint;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.jtaccuino.gog.ComposedPlot;
import org.jtaccuino.gog.GgFigurePane;
import org.jtaccuino.gog.Plot;
import org.jtaccuino.gog.PlotMatrix;
import org.jtaccuino.gog.RenderMode;
import org.jtaccuino.gog.interaction.Orbit3d;
import org.jtaccuino.gog.interaction.Orbit3dOptions;
import org.jtaccuino.gog.theme.Theme;

/**
 * A reusable plot card: a title header, a chip row of tags, the rendered plot,
 * an optional render-timing footer, the description, and a {@link SourceDrawer}
 * docked along the card's bottom border. Clicking a tag chip invokes the
 * {@link #setOnTagSelected} callback. The plot is built lazily off the JavaFX
 * application thread from a {@link PlotSpec} factory, so filtering and
 * scrolling stay responsive; a loading indicator is shown until the plot is
 * ready. The card is data-agnostic (the factory may build any figure the
 * library's {@code DataExtractorRegistry} can render) and does not depend on a
 * concrete DataFrame type.
 */
public class PlotCard extends StackPane {

    private final Label titleLabel = new Label();
    private final FlowPane tagsRow = new FlowPane(6, 4);
    private final Label descriptionLabel = new Label();
    private final Label failureLabel = new Label();
    private final Label timingCaption = new Label("Rendered in:");
    private final Label timingLabel = new Label();
    private final HBox timingRow = new HBox(4, timingCaption, timingLabel);
    private final Label loadingLabel = new Label("Loading\u2026");
    private final Pane plotPane = new Pane();
    private final SourceDrawer sourceDrawer = new SourceDrawer();

    private static final double PLOT_HEIGHT = 520.0;

    // A/B knob (-Dsampler.cache=none|speed|default|quality) selecting the
    // figure cache strategy. Caching is off by default: the figure's canvas is
    // created and painted only after the node is attached to the scene (see
    // GgFigurePane), and a cached node texture captured around that transition
    // could stay blank on some GPU pipelines. Opt in with -Dsampler.cache=speed
    // for scroll profiling.
    private static final String CACHE_MODE = System.getProperty("sampler.cache", "none");

    private static final List<String> CHIP_COLORS =
            List.of("#4c72b0", "#dd8452", "#55a868", "#c44e52", "#8172b3", "#937860", "#da8bc3", "#8c8c8c");

    // Heavy factory work is shared across a small pool instead of one thread
    // per card: scrolling recycles cells rapidly, and a fresh thread per
    // recycled cell would spawn dozens of competing loaders.
    private static final ExecutorService LOADER = Executors.newFixedThreadPool(2, r -> {
        var t = new Thread(r, "plot-loader");
        t.setDaemon(true);
        return t;
    });
    // Debounce load starts so a fast scroll that recycles the same cell many
    // times cancels the pending start before the factory ever runs.
    private static final long LOAD_SETTLE_MS = 120;
    private static final ScheduledExecutorService DEBOUNCE = newSingleDaemonScheduler(
            "plot-debounce-");

    private RenderMode renderMode = RenderMode.FAST;
    private Consumer<String> onTagSelected;
    private Function<String, String> tagColor;
    private PlotLoadProgress progress = PlotLoadProgress.NONE;
    private Task<Object> currentTask;
    private ScheduledFuture<?> pendingStart;
    private long loadGeneration;
    // The spec currently bound to this card. Set when a load starts and cleared
    // when the card is emptied; a re-target to the equal spec (which a host's
    // list does whenever a keystroke re-filters while a card stays visible) must
    // be a no-op on the model — the plot is still the one already rendered or in
    // flight.
    private PlotSpec currentSpec;
    private boolean timingUpdateScheduled;
    // Whether this card is currently inside its host's viewport. Cards that
    // scroll out of view cancel in-flight work and release the rendered figure
    // (and its backing canvas), so browsing the whole catalogue never builds or
    // pins more than the visible set of plots; scrolling back in re-loads.
    private boolean viewportVisible = true;
    // Whether a figure for the current spec is installed in the plot pane.
    private boolean figureInstalled;

    // ─── Render gate ─────────────────────────────────────────────────────
    // Adding a finished figure to a card drives a synchronous layout+render on
    // the JavaFX thread. When a paused scroll lets several (heavy, diamond-
    // sized) loads land in the same frame, stacking those installs back to
    // back freezes the UI for their combined render time. The gate instead
    // runs at most one install per FX pulse, so a burst spreads across frames
    // and the scroll keeps breathing. The queue is keyed by card so a card
    // re-targeted while its install is queued replaces the stale entry with the
    // newest one — the pending set can never exceed the number of visible
    // cards, and stale installs never burn a pulse.
    private static final ConcurrentHashMap<PlotCard, Runnable> PENDING_INSTALLS = new ConcurrentHashMap<>();
    private static volatile boolean drainScheduled;

    /**
     * Creates an empty card; call {@link #setSpec} to populate it.
     */
    @SuppressWarnings("this-escape")
    public PlotCard() {
        getStylesheets().add(getClass().getResource("sampler.css").toExternalForm());
        getStyleClass().add("sampler-card");

        titleLabel.setWrapText(true);
        titleLabel.getStyleClass().add("sampler-card-title");

        descriptionLabel.setWrapText(true);
        descriptionLabel.getStyleClass().add("sampler-card-description");
        descriptionLabel.setManaged(false);

        failureLabel.setWrapText(true);
        failureLabel.getStyleClass().add("sampler-card-failure");
        failureLabel.setMaxWidth(Double.MAX_VALUE);

        timingCaption.getStyleClass().add("sampler-card-timing");
        timingLabel.getStyleClass().add("sampler-card-timing");
        timingRow.setManaged(false);
        timingRow.setVisible(false);

        loadingLabel.getStyleClass().add("sampler-card-loading");
        failureLabel.setLayoutX(0);
        failureLabel.setLayoutY(0);
        // Center the loading/failure indicators within the (non-resizing) plot
        // pane: a plain Pane performs no child layout, so the indicators track
        // the pane size through bindings instead of relying on a layout pass.
        var xPosition = plotPane.widthProperty().subtract(loadingLabel.widthProperty()).divide(2);
        var yPosition = plotPane.heightProperty().subtract(loadingLabel.heightProperty()).divide(2);
        loadingLabel.layoutXProperty().bind(xPosition);
        loadingLabel.layoutYProperty().bind(yPosition);
        failureLabel.layoutXProperty().bind(xPosition);
        failureLabel.layoutYProperty().bind(yPosition);

        // Opaque white backdrop so the loading placeholder and empty phases never
        // composite over a transparent region behind the installed figure.
        plotPane.getStyleClass().add("sampler-plot-backdrop");
        plotPane.setPrefHeight(PLOT_HEIGHT);
        plotPane.setMinHeight(PLOT_HEIGHT);

        var footer = new HBox(12, timingRow);
        footer.setAlignment(Pos.BASELINE_LEFT);

        // The card body: title, tag chips, plot, render timing, description.
        // A reserved band at the bottom keeps the last description line clear
        // of the collapsed source-drawer handle bar.
        var content = new VBox(8);
        content.setPadding(new Insets(14, 14, 36, 14));
        // Child order: 0 title, 1 tag chips, 2 plot pane, 3 render timing,
        // 4 description.
        content.getChildren().addAll(titleLabel, tagsRow, plotPane, footer, descriptionLabel);

        // The source drawer docks to the card's bottom border: it overlays the
        // content column and, when opened, slides its code panel up over the
        // description and the lower part of the plot. It keeps its own natural
        // height and never stretches to the card's height.
        getChildren().addAll(content, sourceDrawer);
        StackPane.setAlignment(sourceDrawer, Pos.BOTTOM_CENTER);
        sourceDrawer.setMaxHeight(Region.USE_PREF_SIZE);
        // A dark theme selection re-themes every visible card's plot: the card
        // re-runs its load so the figure is rebuilt under the shared theme.
        SourceTheme.CURRENT.addListener((obs, oldTheme, newTheme) -> {
            if (currentSpec != null) {
                reload();
            }
        });
    }

    /**
     * Applies the {@code -Dsampler.cache} strategy to a freshly installed
     * figure node. Prism blits the cached texture on translation-only movement
     * once the cached image stays valid, so the hint matters less than whether
     * per-frame invalidation (canvas dirtiness or texture thrash) keeps the
     * cache rebuilding.
     *
     * @param node the installed figure region
     */
    private static void applyCache(Region node) {
        switch (CACHE_MODE) {
            case "none" -> node.setCache(false);
            case "default" -> {
                node.setCache(true);
                node.setCacheHint(CacheHint.DEFAULT);
            }
            case "quality" -> {
                node.setCache(true);
                node.setCacheHint(CacheHint.QUALITY);
            }
            default -> {
                node.setCache(true);
                node.setCacheHint(CacheHint.SPEED);
            }
        }
    }

    /**
     * Self-heals a laid-out card whose plot pane is empty. The viewport gate and
     * the load debounce can otherwise leave a visible card with a bound spec, an
     * empty pane and no placeholder (a cancelled/recycled load that never got
     * restarted, a stale visibility verdict, a released figure), which shows as
     * a permanently blank plot area. Because the ListView only lays out cells
     * that are on screen, running this from {@link #layoutChildren()} means
     * every visible card is guaranteed to (re)start its load; it is a no-op
     * while a placeholder or figure occupies the pane.
     */
    private void ensureLoaded() {
        if (currentSpec == null || !plotPane.getChildren().isEmpty()) {
            return;
        }
        if (pendingStart != null && !pendingStart.isDone()) {
            return;
        }
        if (currentTask != null && !currentTask.isDone()) {
            return;
        }
        installLoadingPlaceholder();
        startLoad(currentSpec);
    }

    @Override
    protected void layoutChildren() {
        super.layoutChildren();
        ensureLoaded();
    }

    /**
     * Queues {@code install} as the single pending install of this card for the
     * next FX pulse, replacing any older pending install of the same card.
     * Idempotent for concurrent callers: only one draining runnable is ever
     * scheduled at a time.
     */
    private void queueInstall(Runnable install) {
        if (PENDING_INSTALLS.put(this, install) == null) {
            progress.enqueueInstall();
        }
        if (!drainScheduled) {
            drainScheduled = true;
            Platform.runLater(PlotCard::drainOne);
        }
    }

    /** Runs one queued install and reschedules for the next pulse if more wait. */
    private static void drainOne() {
        drainScheduled = false;
        var iterator = PENDING_INSTALLS.entrySet().iterator();
        if (!iterator.hasNext()) {
            return;
        }
        var entry = iterator.next();
        iterator.remove();
        entry.getKey().progress.dequeueInstall();
        entry.getValue().run();
        if (!PENDING_INSTALLS.isEmpty() && !drainScheduled) {
            drainScheduled = true;
            Platform.runLater(PlotCard::drainOne);
        }
    }

    /**
     * Sets the callback invoked when a tag chip is clicked, or {@code null} to
     * make the chips inert.
     *
     * @param onTagSelected the handler receiving the clicked tag label
     */
    public void setOnTagSelected(Consumer<String> onTagSelected) {
        this.onTagSelected = onTagSelected;
    }

    /**
     * Sets the chip background colour for a tag label, or {@code null} to fall
     * back to a stable auto-chosen colour.
     *
     * @param tagColor maps a tag label to a CSS colour
     */
    public void setTagColor(Function<String, String> tagColor) {
        this.tagColor = tagColor;
    }

    /**
     * Sets the shared load-progress sink this card reports its plot work to, or
     * {@code null} to use a no-op.
     *
     * @param progress the progress sink
     */
    public void setLoadProgress(PlotLoadProgress progress) {
        this.progress = progress == null ? PlotLoadProgress.NONE : progress;
    }

    /**
     * The in-card source drawer; populate it via {@link #setSpec} and toggle it
     * through its {@link SourceDrawer#toggle() nudge}.
     *
     * @return the card's source drawer
     */
    public SourceDrawer sourceDrawer() {
        return sourceDrawer;
    }

    /**
     * The render mode currently applied to the spec's figure.
     *
     * @return the active render mode
     */
    public RenderMode renderMode() {
        return renderMode;
    }

    /**
     * Re-targets this card to the given spec, cancelling any in-flight plot
     * load and restarting one for the new spec. Pass {@code null} to clear.
     * <p>
     * When a host re-filters its list, it re-targets the visible cells with the
     * same spec instances, so a spec that survives the re-filter arrives as the
     * <em>same</em> instance. In that case this is a no-op — the card keeps the
     * figure already rendered (or the load already in flight) and neither the
     * debounce nor the render-gate queue is touched. Cards only do real work
     * when the set of shown specs actually changes (plots added or removed).
     *
     * @param spec the spec to show, or {@code null} to clear the card
     */
    public void setSpec(PlotSpec spec) {
        if (spec != null && spec.equals(currentSpec)) {
            return;
        }
        cancelLoad();
        // A timing update may still be pending from the previous figure (its
        // Platform.runLater may not have run yet). Clear the scheduled flag so
        // that pending update cannot swallow the render-complete callback of
        // the figure about to be installed — otherwise the new figure's timing
        // would be dropped when the stale update later bails on the generation
        // check, leaving the timing row permanently hidden.
        timingUpdateScheduled = false;
        plotPane.getChildren().clear();
        // The pane is empty again; drop any stale "figure installed" state so a
        // later viewport re-entry is not fooled into skipping the reload.
        figureInstalled = false;
        failureLabel.setText("");
        timingLabel.setText("");
        timingRow.setManaged(false);
        timingRow.setVisible(false);
        if (spec == null) {
            currentSpec = null;
            figureInstalled = false;
            titleLabel.setText("");
            tagsRow.getChildren().clear();
            descriptionLabel.setText("");
            descriptionLabel.setManaged(false);
            sourceDrawer.setSource(null, null);
            return;
        }

        currentSpec = spec;
        titleLabel.setText(spec.title());
        descriptionLabel.setText(spec.description());
        descriptionLabel.setManaged(spec.description() != null && !spec.description().isBlank());
        tagsRow.getChildren().setAll(spec.tags().stream().map(this::tagChip).toList());
        sourceDrawer.setSource(spec.methodName(), spec.source());

        if (viewportVisible) {
            installLoadingPlaceholder();
            startLoad(spec);
        }
    }

    /**
     * Marks whether this card lies inside the host's viewport. An off-screen
     * card cancels any in-flight plot load and releases the rendered figure
     * (and its backing canvas), so scrolling past the whole catalogue never
     * builds or pins more than the visible set of plots; scrolling back into
     * view restarts the (debounced) load for the still-bound spec.
     *
     * @param visible whether the card is currently inside the viewport
     */
    public void setViewportVisible(boolean visible) {
        if (visible == viewportVisible) {
            return;
        }
        viewportVisible = visible;
        if (!visible) {
            cancelLoad();
            releaseFigure();
        } else if (currentSpec != null && plotPane.getChildren().isEmpty()) {
            // Re-enter only when the pane is actually empty: the boolean install
            // flag can lag the scene graph, and a card that comes back with an
            // installed (or loading) child must not be reset.
            installLoadingPlaceholder();
            startLoad(currentSpec);
        }
    }

    /**
     * Drops the installed figure (and its backing canvas) and resets the
     * per-card render state, keeping the title, tags, description and source
     * drawer intact.
     */
    private void releaseFigure() {
        figureInstalled = false;
        timingUpdateScheduled = false;
        plotPane.getChildren().clear();
        failureLabel.setText("");
        timingLabel.setText("");
        timingRow.setManaged(false);
        timingRow.setVisible(false);
    }

    private void installLoadingPlaceholder() {
        if (!plotPane.getChildren().contains(loadingLabel)) {
            plotPane.getChildren().add(loadingLabel);
        }
    }

    /**
     * Changes the A/B render mode used for the spec's plot and, when a figure
     * is already installed or loading, re-runs the load under the new mode so
     * the visible cards reflect the toggle immediately. The toggle drives a
     * load the same way a filter change does (debounced, gated), so a mode flip
     * across many visible cards stays responsive.
     *
     * @param mode the render mode to apply to future figures
     */
    public void setRenderMode(RenderMode mode) {
        if (Objects.equals(mode, renderMode)) {
            return;
        }
        renderMode = mode;
        if (currentSpec != null) {
            reload();
        }
    }

    /**
     * Re-runs the load of the current spec, replacing the installed figure
     * (e.g. when the render mode or the shared light/dark theme changed).
     */
    private void reload() {
        // Off-screen cards hold no figure; their load restarts on re-entering
        // the viewport (setViewportVisible) under the then-current mode/theme.
        if (!viewportVisible) {
            return;
        }
        cancelLoad();
        timingUpdateScheduled = false;
        figureInstalled = false;
        plotPane.getChildren().clear();
        failureLabel.setText("");
        timingLabel.setText("");
        timingRow.setManaged(false);
        timingRow.setVisible(false);
        plotPane.getChildren().add(loadingLabel);
        startLoad(currentSpec);
    }

    /**
     * A small clickable chip rendering one tag label. Clicking it applies a
     * single-tag filter via {@link #onTagSelected}; the cursor changes to
     * signal the interaction.
     */
    private Label tagChip(String tag) {
        var label = new Label(tag);
        var color = tagColor != null ? tagColor.apply(tag) : CHIP_COLORS.get(Math.floorMod(tag.hashCode(), CHIP_COLORS.size()));
        label.setStyle("-fx-background-color: " + color
                + "; -fx-background-radius: 10; -fx-padding: 2 8 2 8; -fx-font-size: 11px;"
                + "-fx-cursor: hand;");
        label.setOnMouseClicked(e -> {
            if (onTagSelected != null) {
                onTagSelected.accept(tag);
            }
        });
        return label;
    }

    private void startLoad(PlotSpec spec) {
        final long generation = ++loadGeneration;
        if (pendingStart != null) {
            pendingStart.cancel(false);
        }
        // Wait for the cell to "settle" before kicking off the factory. During
        // a scroll the same cell is re-targeted many times; only the last
        // re-target that survives through the settle window starts real work.
        pendingStart = DEBOUNCE.schedule(() -> startTask(spec, generation),
                LOAD_SETTLE_MS, TimeUnit.MILLISECONDS);
    }

    private void startTask(PlotSpec spec, long generation) {
        if (generation != loadGeneration) {
            return;
        }
        var task = new Task<Object>() {
            @Override
            protected Object call() {
                var node = spec.factory().get();
                // Apply the current A/B render mode (FAST default, FULL for
                // fidelity comparison). Vector export overrides to FULL anyway.
                if (node instanceof Plot<?> plot) {
                    plot.renderMode(renderMode);
                } else if (node instanceof PlotMatrix<?> matrix) {
                    matrix.renderMode(renderMode);
                } else if (node instanceof ComposedPlot composed) {
                    composed.renderMode(renderMode);
                }
                // Warm the deterministic prepare phase (scale resolution, stat
                // fits, per-layer GroupedSmooths for LOESS-by-group) here on the
                // loader thread instead of during the first FX-thread layout.
                // The figure then paints its canvas with work already done.
                // This applies to any GgFigurePane: Plot, PlotMatrix, ComposedPlot.
                if (node instanceof GgFigurePane figure) {
                    // A dark app theme switches every figure to the library's
                    // dark theme, cascading through matrix cells and composed
                    // leaves; light mode keeps each spec's own theme. Apply it
                    // before prepare() so the prepared scales, guides and layer
                    // contexts are resolved against the theme that will render.
                    if (SourceTheme.CURRENT.get().isDark()) {
                        figure.applyTheme(Theme.theme_dark());
                    }
                    figure.prepareAsync();
                }
                return node;
            }
        };
        currentTask = task;
        task.setOnSucceeded(e -> {
            progress.endLoad();
            if (generation != loadGeneration) {
                return;
            }
            // Buffer the node install through the render gate so a burst of
            // completed loads does not stack layout+render synchronously in one
            // FX pulse (the scroll-lag symptom). Generation is re-checked when
            // the install actually runs, in case the cell was re-targeted while
            // queued.
            queueInstall(() -> {
                if (generation != loadGeneration) {
                    return;
                }
                var node = (Region) task.getValue();
                double w = Math.max(1, Math.round(plotPane.getWidth()));
                node.setPrefWidth(w);
                node.setPrefHeight(PLOT_HEIGHT);
                node.setMinWidth(0);
                node.setMinHeight(PLOT_HEIGHT);
                // A plain Pane never resizes its children during layout, so the
                // figure's bounds must be pinned here exactly once. Pinning to a
                // pixel-rounded width keeps the backing canvas size stable across
                // pulses — the StackPane previously re-resized the child on every
                // layout pass, oscillating the canvas size and forcing Prism to
                // clear (BaseGraphics.clear) and re-render the whole figure each
                // frame.
                node.resize(w, PLOT_HEIGHT);
                // Follow window/card width changes (whole pixels) so the figure
                // still reflows on resize, but ignore sub-pixel flutters that
                // round back to the same width — those previously reached the
                // figure every pulse through the StackPane layout pass.
                plotPane.widthProperty().addListener((obs, o, n) -> {
                    double rw = Math.max(1, Math.round(n.doubleValue()));
                    if (rw != node.getWidth()) {
                        node.resize(rw, PLOT_HEIGHT);
                        // Repaint at the new width without waiting for a layout
                        // pass (a plain Pane does not lay out its children).
                        if (node instanceof GgFigurePane figure) {
                            figure.markDirty();
                        }
                    }
                });
                plotPane.getChildren().clear();
                plotPane.getChildren().add(node);
                figureInstalled = true;
                // Cache the rendered figure so scrolling a virtualized list
                // blits a cached texture per frame instead of replaying the
                // backing Canvas display list (clear + every recorded draw op)
                // on the render thread. Prism invalidates the cache when the
                // node is resized, so the width listener above still re-renders
                // on whole-pixel changes. The exact mode is the -Dsampler.cache
                // A/B knob so profile runs can compare cache strategies.
                applyCache(node);
                // The figure paints its canvas during a later layout pass on the FX
                // thread; the render-completion callback is fired then with the
                // measured duration, so no polling is needed. At most one timing
                // update is queued per card per pulse so a resize-driven burst of
                // renders coalesces into a single label change.
                if (node instanceof GgFigurePane figure) {
                    figure.setOnRenderComplete(() -> {
                        if (generation == loadGeneration && !timingUpdateScheduled) {
                            timingUpdateScheduled = true;
                            // Defer the label/managed/visible mutations out of the
                            // current layoutChildren() pass. Setting managed or
                            // visible during layout would trigger another layout
                            // round on the same pulse, compounding the stall.
                            Platform.runLater(() -> {
                                timingUpdateScheduled = false;
                                if (generation == loadGeneration) {
                                    timingLabel.setText(formatDuration(
                                            figure.lastRenderNanos()));
                                    timingRow.setManaged(true);
                                    timingRow.setVisible(true);
                                }
                            });
                        }
                    });
                    // Attach an orbit controller so 3-D examples are draggable.
                    if (node instanceof Plot<?> plot && spec.tags().stream()
                            .anyMatch(t -> t.equalsIgnoreCase("Orbit"))) {
                        try {
                            Orbit3d.attach(plot, Orbit3dOptions.defaults());
                        } catch (IllegalArgumentException not3d) {
                            // Tagged as orbit but not a 3-D figure; no gesture.
                        }
                    }
                    // Paint now. The figure is added to a plain Pane, which never
                    // lays out its children, so layoutChildren()'s redraw() is not
                    // guaranteed to run when a sized node is installed after the
                    // card has already been laid out (the common case for
                    // slow-loading plots). markDirty() (not redraw()) forces the
                    // paint even though a stale canvas may already exist from an
                    // off-scene render, so the on-screen canvas cannot stay blank.
                    figure.markDirty();
                }
            });
        });
        task.setOnFailed(e -> {
            progress.endLoad();
            if (generation != loadGeneration) {
                return;
            }
            plotPane.getChildren().clear();
            failureLabel.setText("Could not render plot: " + task.getException());
            plotPane.getChildren().add(failureLabel);
        });
        task.setOnCancelled(e -> {
            progress.endLoad();
        });
        progress.beginLoad();
        LOADER.execute(task);
    }

    private void cancelLoad() {
        loadGeneration++;
        if (pendingStart != null) {
            pendingStart.cancel(false);
            pendingStart = null;
        }
        if (currentTask != null) {
            currentTask.cancel();
            currentTask = null;
        }
    }

    /**
     * Creates a single-threaded scheduler of daemon threads used for debouncing
     * plot load starts, so the debounce thread never keeps the host alive.
     *
     * @return the shared scheduler
     */
    private static ScheduledExecutorService newSingleDaemonScheduler(String prefix) {
        return Executors.newSingleThreadScheduledExecutor(r -> {
            var t = new Thread(r, prefix + System.nanoTime());
            t.setDaemon(true);
            return t;
        });
    }

    /**
     * Formats an elapsed time in nanoseconds as a short human-readable string.
     */
    private static String formatDuration(long nanos) {
        var ms = nanos / 1_000_000.0;
        return ms >= 1000 ? String.format(Locale.ROOT, "%.1f s", ms / 1000.0)
                : String.format(Locale.ROOT, "%.0f ms", ms);
    }
}
