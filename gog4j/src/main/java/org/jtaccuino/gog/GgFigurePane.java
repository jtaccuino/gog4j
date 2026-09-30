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
package org.jtaccuino.gog;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.scene.canvas.Canvas;
import javafx.scene.layout.Background;
import javafx.scene.layout.Pane;
import org.jtaccuino.gog.render.DrawSurface;
import org.jtaccuino.gog.render.FxDrawSurface;
import org.jtaccuino.gog.theme.Theme;

/**
 * The shared JavaFX {@link Pane} lifecycle for the three library figure types —
 * a {@link Plot}, a {@link PlotMatrix}, and a {@link ComposedPlot}. Every
 * figure draws itself onto a single backing {@link Canvas} held here, so the
 * resize/repaint pulse ({@link #layoutChildren()}) and the repaint entry point
 * ({@link #redraw()}) live in exactly one place: they rebuild a fresh canvas at
 * the node's current size and delegate the actual drawing to
 * {@link #renderTo(DrawSurface, double, double)}, which each subclass provides.
 */
public sealed abstract class GgFigurePane extends Pane implements GgFigure
        permits Plot, GridFigurePane {

    /** The single backing canvas this figure repaints on every pulse. */
    protected Canvas canvas;

    // Diagnostic hook (-Dsampler.debugReplay=true): counts redraw() invocations
    // so a profile run can distinguish FX-side per-frame re-render churn from
    // render-thread-only cache rebuilding. Disabled entirely unless the flag is
    // set; the reporter thread prints the per-second rate.
    private static final boolean DEBUG_REPLAY = Boolean.getBoolean("sampler.debugReplay");
    private static final AtomicLong REPLAY_COUNT = new AtomicLong();

    static {
        if (DEBUG_REPLAY) {
            var reporter = Executors.newSingleThreadScheduledExecutor(r -> {
                var t = new Thread(r, "redraw-reporter");
                t.setDaemon(true);
                return t;
            });
            var reporterFuture = reporter.scheduleAtFixedRate(() -> System.out.println(
                    "[sampler.debugReplay] redraw() calls in last second: " + REPLAY_COUNT.getAndSet(0)),
                    1, 1, TimeUnit.SECONDS);
            Runtime.getRuntime().addShutdownHook(new Thread(() -> reporterFuture.cancel(false)));
        }
    }

    // The figure's resolved theme. Populated at most once (see
    // setThemeIfAbsent) during descriptor finalization — the first of
    // constructor/prepareAsync/renderTo that can resolve it wins — and fired
    // to the background listener below, which installs an opaque pane
    // background derived from the theme's paneBackground so Prism can treat
    // the figure region as opaque.
    private final ReadOnlyObjectWrapper<Theme> theme = new ReadOnlyObjectWrapper<>();

    // Wall-clock time of the last completed renderTo call, in nanoseconds.
    // Written on whatever thread renders and read from the FX thread by the
    // sampler UI to report how long a plot took to paint.
    private volatile long lastRenderNanos;

    // Optional notification fired once a render pass completes. Set by the
    // sampler UI so it can show the timing without polling lastRenderNanos.
    private volatile Runnable onRenderComplete;

    /** Whether a {@link #markDirty()} call is waiting for the next repaint. */
    private boolean dirty = false;

    /**
     * Applies the transparent pane styling shared by every figure type so the
     * figure itself never paints a rectangle over its parent.
     */
    @SuppressWarnings("this-escape")
    protected GgFigurePane() {
        // The region starts with the Pane default (transparent) so a figure
        // can sit on any backdrop; the first theme resolution replaces it with
        // an opaque, theme-derived background.
        theme.addListener((obs, prev, t) -> {
            if (t != null) {
                setBackground(Background.fill(t.paneBackground()));
            }
        });
        // Repaint the first time the figure is attached to a Scene. A figure is
        // prepared on the sampler's loader thread and its size pinned while it
        // is still detached; that off-scene render can be blank, and a plain
        // Pane does not lay out its children, so the size memo below would
        // otherwise suppress every later repaint and leave the plot area blank.
        // markDirty() forces a fresh attached paint.
        sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                markDirty();
            }
        });
        // Align the figure's local layout (and thus the backing canvas, which
        // Prism clears and re-renders on every resize) to the device-pixel
        // grid. Otherwise a Retina display feeds fractional logical dimensions
        // through layoutChildren, the exact-size guard below fails on a
        // sub-pixel difference every pulse, and the canvas backing store is
        // cleared (com/sun/prism/impl/BaseGraphics.clear) and fully
        // re-rendered each frame.
        setSnapToPixel(true);
    }

    /**
     * The resolved theme of this figure, or {@code null} until it is resolved
     * during descriptor finalization. Figures with an unresolved theme render
     * against a transparent pane; once resolved the pane background becomes an
     * opaque {@link Theme#paneBackground()}.
     *
     * @return the figure's theme property
     */
    public final ReadOnlyObjectProperty<Theme> themeProperty() {
        return theme.getReadOnlyProperty();
    }

    /**
     * Resolves this figure's theme, replacing any previously resolved value.
     * Called by subclasses once their descriptor finalizes (a {@link Plot}
     * resolves its theme at construction; the grid figures resolve it when
     * their cells/leaves are prepared).
     *
     * @param theme the figure's resolved theme
     */
    protected final void setTheme(Theme theme) {
        this.theme.set(theme);
    }

    /**
     * Resolves this figure's theme unless one is already resolved, so the
     * first of the constructor/prepare/render paths to reach a theme wins and
     * the pane background is applied exactly once.
     *
     * @param theme the figure's resolved theme
     */
    protected final void setThemeIfAbsent(Theme theme) {
        if (this.theme.get() == null) {
            this.theme.set(theme);
        }
    }

    /**
     * Applies the given theme to this figure and repaints it at its current
     * size. Re-resolves the figure's resolved theme so the opaque pane
     * background follows the switch, then redraws the backing canvas. Grid
     * figures ({@code PlotMatrix}, {@code ComposedPlot}) cascade the theme down
     * to their contained plots, so a single call re-themes the whole figure.
     *
     * @param theme the theme to apply
     */
    public void applyTheme(Theme theme) {
        setTheme(theme);
        redraw();
    }

    /**
     * Registers a callback fired after every completed {@link #renderTo} pass,
     * or {@code null} to clear it. The callback runs on the thread that renders
     * (the JavaFX application thread for UI-driven redraws).
     *
     * @param onRenderComplete the render-completion callback, or {@code null}
     */
    public void setOnRenderComplete(Runnable onRenderComplete) {
        this.onRenderComplete = onRenderComplete;
    }

    /**
     * Records the duration of a completed {@link #renderTo} pass, in
     * nanoseconds. Called by each subclass at the end of its render.
     *
     * @param startNanos the {@link System#nanoTime()} reading taken just before
     *     the render pass began
     */
    protected final void recordRenderNanos(long startNanos) {
        lastRenderNanos = System.nanoTime() - startNanos;
        if (onRenderComplete != null) {
            onRenderComplete.run();
        }
    }

    /**
     * Returns the wall-clock time in nanoseconds of the most recently completed
     * {@link #renderTo} call, or {@code 0} if this figure has not rendered yet.
     *
     * @return the last render duration in nanoseconds
     */
    public long lastRenderNanos() {
        return lastRenderNanos;
    }

    /**
     * Pre-computes the deterministic preparation phase on the calling thread
     * (typically the sampler's loader pool) so the first
     * {@link #layoutChildren() → redraw()} on the JavaFX thread skips it.
     * Subclasses override this to prepare their contained {@code Plot}s and
     * caches; the default is a no-op for figures with no preparation cost.
     * <p>
     * Idempotent and safe to call repeatedly from any thread.
     */
    public void prepareAsync() {
        // Default no-op; overridden by Plot, PlotMatrix, ComposedPlot.
    }

    @Override
    protected final void layoutChildren() {
        super.layoutChildren();
        if (this.isVisible() && this.getParent().isVisible()) {
            this.redraw();
        }
    }

    @Override
    public final void markDirty() {
        dirty = true;
        redraw();
    }

    @Override
    public final void redraw() {
        if (DEBUG_REPLAY) {
            REPLAY_COUNT.incrementAndGet();
        }
        // Snap the layout size to the device-pixel grid before comparing or
        // allocating the canvas. Without this, fractional layout dimensions on
        // Retina displays defeat the exact-size guard below by a sub-pixel
        // amount on every pulse, resizing the canvas each frame and forcing
        // Prism to clear (BaseGraphics.clear) and re-render the whole backing
        // store.
        double width = Math.max(0, Math.round(this.getWidth()));
        double height = Math.max(0, Math.round(this.getHeight()));
        if (width <= 0 || height <= 0) {
            return;
        }
        // Performance: if already drawn at this size keep everything as is
        // instead of rebuilding the canvas on every pulse. A markDirty() call
        // clears that memo so descriptor mutations still reach the canvas.
        if (!dirty
                && canvas != null
                && Double.compare(width, canvas.getWidth()) == 0
                && Double.compare(height, canvas.getHeight()) == 0) {
            return;
        }
        dirty = false;
        getChildren().removeIf(node -> node instanceof Canvas);
        canvas = new Canvas(width, height);
        canvas.setMouseTransparent(true);
        renderTo(new FxDrawSurface(canvas.getGraphicsContext2D()), width, height);
        getChildren().add(0, canvas);
    }
}
