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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import org.jtaccuino.gog.coord.Coord;
import org.jtaccuino.gog.coord.Coord2D;
import org.jtaccuino.gog.coord.Coord3D;
import org.jtaccuino.gog.coord.CoordFixed;
import org.jtaccuino.gog.coord.CoordPolar;
import org.jtaccuino.gog.coord.CubeFace;
import org.jtaccuino.gog.coord.Light3d;
import org.jtaccuino.gog.data.Values;
import org.jtaccuino.gog.facet.FacetGrid;
import org.jtaccuino.gog.facet.FacetSpec;
import org.jtaccuino.gog.facet.FacetValues;
import org.jtaccuino.gog.facet.GridOptions;
import org.jtaccuino.gog.guide.Guide;
import org.jtaccuino.gog.guide.Guide.GuideMetrics;
import org.jtaccuino.gog.guide.GuideAlpha;
import org.jtaccuino.gog.guide.GuideColorbar;
import org.jtaccuino.gog.guide.GuideColorsteps;
import org.jtaccuino.gog.guide.GuideData;
import org.jtaccuino.gog.guide.GuideLegend;
import org.jtaccuino.gog.guide.GuideStyle;
import org.jtaccuino.gog.jfr.CellExtentCollectionEvent;
import org.jtaccuino.gog.jfr.FacetLayoutEvent;
import org.jtaccuino.gog.jfr.FacetPanelEvent;
import org.jtaccuino.gog.jfr.FacetPartitionEvent;
import org.jtaccuino.gog.jfr.GuideCollectionEvent;
import org.jtaccuino.gog.jfr.LayerPrepareEvent;
import org.jtaccuino.gog.jfr.LayerRenderEvent;
import org.jtaccuino.gog.jfr.LayoutEvent;
import org.jtaccuino.gog.jfr.PanelPrepEvent;
import org.jtaccuino.gog.jfr.PlotRenderEvent;
import org.jtaccuino.gog.jfr.PreparePhaseEvent;
import org.jtaccuino.gog.labs.LabsSpec;
import org.jtaccuino.gog.layer.ConstantColorLayer;
import org.jtaccuino.gog.layer.GeomAbline;
import org.jtaccuino.gog.layer.GeomDensity2d;
import org.jtaccuino.gog.layer.GeomFunction;
import org.jtaccuino.gog.layer.GeomHline;
import org.jtaccuino.gog.layer.GeomLine;
import org.jtaccuino.gog.layer.GeomPoint;
import org.jtaccuino.gog.layer.GeomPoint3d;
import org.jtaccuino.gog.layer.GeomPolygon3d;
import org.jtaccuino.gog.layer.GeomSegment3d;
import org.jtaccuino.gog.layer.GeomSmooth;
import org.jtaccuino.gog.layer.GeomText3d;
import org.jtaccuino.gog.layer.GeomTile;
import org.jtaccuino.gog.layer.GeomVline;
import org.jtaccuino.gog.layer.Layer;
import org.jtaccuino.gog.layer.LayerConfigurator;
import org.jtaccuino.gog.layer.LayerData;
import org.jtaccuino.gog.layer.PanelContext;
import org.jtaccuino.gog.layer.PlotContext;
import org.jtaccuino.gog.layer.PointShape;
import org.jtaccuino.gog.layer.PositionAdjust;
import org.jtaccuino.gog.layer.StackableGeom;
import org.jtaccuino.gog.layer.StatHost;
import org.jtaccuino.gog.render.DrawSurface;
import org.jtaccuino.gog.render.FxDrawSurface;
import org.jtaccuino.gog.render.SvgDrawSurface;
import org.jtaccuino.gog.scale.ColorScale;
import org.jtaccuino.gog.scale.ContinuousColorScale;
import org.jtaccuino.gog.scale.DiscreteColorScale;
import org.jtaccuino.gog.scale.Expansion;
import org.jtaccuino.gog.scale.ResolvedScales;
import org.jtaccuino.gog.scale.Scale;
import org.jtaccuino.gog.scale.ScaleConfigurator;
import org.jtaccuino.gog.scale.ScaleSpec;
import org.jtaccuino.gog.scale.ScaleTransform;
import org.jtaccuino.gog.scale.SizeScale;
import org.jtaccuino.gog.spi.DataExtractor;
import org.jtaccuino.gog.stat.Stat;
import org.jtaccuino.gog.stat.StatData;
import org.jtaccuino.gog.theme.AxisStyle;
import org.jtaccuino.gog.theme.GuideOverflow;
import org.jtaccuino.gog.theme.GuidePosition;
import org.jtaccuino.gog.theme.Theme;
import org.jtaccuino.gog.theme.ThemeConfigurator;

/**
 * Core Grammar of Graphics plot component extending JavaFX {@link Pane}.
 * <p>
 * {@code Plot} manages dataset extraction, aesthetic mappings, geometry layers,
 * scale transformations, faceting, themes, labels, legends, and
 * hardware-accelerated canvas rendering.
 *
 * @param <DF> the DataFrame type representing the underlying dataset
 */
    public non-sealed class Plot<DF> extends GgFigurePane implements ConfigTarget<DF> {

    // The mutable spec all declarative state lives in; Plot renders and hovers
    // against it but never owns the data, mapping, layers, scales, theme,
    // labels, guides or coordinates itself.
    private final PlotDescriptor<DF> descriptor;

    // Per-render lifecycle state: the eagerly resolved shared scales, the
    // per-layer plot contexts, and the LayerData each layer prepared over the
    // global data. Alive between renderTo calls so hover hit-testing reuses
    // the exact state the visible image was drawn with.
    private volatile ResolvedScales<DF> currentScales;
    private final Map<Layer<DF>, PlotContext<DF>> plotContexts = new ConcurrentHashMap<>();
    private final Map<Layer<DF>, LayerData> preparedData = new ConcurrentHashMap<>();

    // preparation is deterministic over the immutable descriptor, so it is
    // computed at most once per Plot and never re-run on repeated redraws;
    // set by prepare() and read by the FX thread and the sampler loader.
    private volatile boolean prepared;
    // Serializes the prepare() critical section. prepareAsync() runs on the
    // sampler's loader thread while layout/redraw runs on the JavaFX thread;
    // without this lock both could pass the `prepared` guard and interleave
    // preparedData.clear()/put() and the currentScales/plotContexts rebuild,
    // leaving layers without prepared data and rendering empty panels.
    private final Object prepareLock = new Object();

    private HBox hoverTooltipContainer = null;
    private Label tooltipTextLabel = null;
    private Node tooltipGlyph = null;

    /**
     * Neutral key colour used for linetype-only legend key glyphs.
     */
    private static final Color NEUTRAL_KEY_COLOR = Color.web("#666666");

    // Retain computed panel scales for hover hit-testing
    private final List<PanelScaleRegistry<DF>> panelScales = new ArrayList<>();

    // Helper record to keep per-panel scales in memory
    private record PanelScaleRegistry<DF>(Object facetKey, DF partitionDf, Scale sx, Scale sy) {
    }

    /**
     * Constructs a {@code Plot} for the given spec.
     *
     * @param descriptor the immutable-at-construction spec this plot renders
     */
    Plot(PlotDescriptor<DF> descriptor) {
        this.descriptor = descriptor;
        setTheme(descriptor.theme());

        // Build a combined container (HBox) for the tooltip
        hoverTooltipContainer = new HBox(8); // 8 pixel spacing between dot and text
        hoverTooltipContainer.setAlignment(Pos.CENTER_LEFT);
        hoverTooltipContainer.setStyle(
                "-fx-background-color: rgba(30, 30, 30, 0.88);"
                + // Dark, elegant anthracite
                "-fx-padding: 6 12 6 12;"
                + "-fx-background-radius: 5;"
                + "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.4), 6, 0, 0, 3);"
        );
        hoverTooltipContainer.setVisible(false);
        hoverTooltipContainer.setMouseTransparent(true);

        // The small colored indicator dot (circle)
        // The small indicator marker in front of the tooltip: mirrors the
        // hovered point's colour, and its point shape when the shape aesthetic
        // is mapped (e.g. a manual shape scale)
        tooltipGlyph = buildTooltipGlyph(Color.TRANSPARENT, PointShape.CIRCLE);

        tooltipTextLabel = new Label();
        tooltipTextLabel.setStyle(
                "-fx-text-fill: #ffffff;"
                + "-fx-font-family: 'System';"
                + "-fx-font-size: 11px;"
                + "-fx-font-weight: bold;"
                + "-fx-line-spacing: 2;"
        );

        hoverTooltipContainer.getChildren().addAll(tooltipGlyph, tooltipTextLabel);
        getChildren().add(hoverTooltipContainer);

        setOnMouseMoved(e -> {
            var mx = e.getX();
            var my = e.getY();
            String foundTooltip = null;

            for (var registry : panelScales) {
                var facetValues = facetValuesForPanel(registry.facetKey());
                for (var layer : descriptor.geoms()) {
                    var panelCtx = new PanelContext<>(plotContextFor(layer),
                            registry.partitionDf(), facetValues, registry.sx(), registry.sy(),
                            descriptor.renderMode());
                    var layerData = preparedData.getOrDefault(layer, LayerData.NONE);
                    var faceGuard = beginFaceRender(layer);
                    try {
                        var txt = layer.locate(panelCtx, layerData, mx, my);
                        if (txt != null) {
                            foundTooltip = txt;
                            var glyph = buildTooltipGlyph(resolveTooltipColor(txt),
                                    resolveTooltipShape(txt));
                            hoverTooltipContainer.getChildren().set(0, glyph);
                            tooltipGlyph = glyph;
                            break;
                        }
                    } finally {
                        faceGuard.close();
                    }
                }
                if (foundTooltip != null) {
                    break;
                }
            }

            if (foundTooltip != null) {
                tooltipTextLabel.setText(applyLabelMappings(foundTooltip));
                hoverTooltipContainer.setVisible(true);
                hoverTooltipContainer.setLayoutX(mx + 12);
                hoverTooltipContainer.setLayoutY(my + 15);
                hoverTooltipContainer.toFront();
            } else {
                hoverTooltipContainer.setVisible(false);
            }
        });

        setOnMouseExited(e -> hoverTooltipContainer.setVisible(false));

        widthProperty().addListener((obs, oldVal, newVal) -> redraw());
        heightProperty().addListener((obs, oldVal, newVal) -> redraw());
    }

    /**
     * Adds one or more geometry layer configurators to the plot pipeline.
     *
     * @param configurators one or more geometry configurators (e.g.
     * {@code point()}, {@code line()}, {@code bar()})
     * @return this {@code Plot} instance for fluid chaining
     */
    @SafeVarargs
    @SuppressWarnings("unchecked")
    public final Plot<DF> geoms(LayerConfigurator<? super DF>... configurators) {
        for (var configurator : configurators) {
            // Allows safe configuration passthrough
            ((LayerConfigurator<DF>) configurator).configure(this);
        }
        return this;
    }

    /**
     * Adds one or more geometry layer configurators supplied as a collection,
     * for callers that assemble configurators dynamically.
     *
     * @param configurators the geometry configurators (e.g. {@code point()},
     * {@code line()}, {@code bar()})
     * @return this {@code Plot} instance for fluid chaining
     */
    @SuppressWarnings("unchecked")
    public final Plot<DF> geoms(Collection<? extends LayerConfigurator<? super DF>> configurators) {
        for (var configurator : configurators) {
            // Allows safe configuration passthrough
            ((LayerConfigurator<DF>) configurator).configure(this);
        }
        return this;
    }

    /**
     * Configures multi-panel grid faceting for the plot.
     *
     * @param facetSpec the facet layout specification (e.g.,
     * {@code Facets.wrap("col", cols)})
     * @return this {@code Plot} instance for fluid chaining
     */
    public Plot<DF> facets(FacetSpec facetSpec) {
        descriptor.facets(facetSpec);
        return this;
    }

    /**
     * Sets declarative plot labels (title, subtitle, X/Y axis labels, caption).
     *
     * @param labsSpec the label specification
     * @return this {@code Plot} instance for fluid chaining
     */
    public Plot<DF> labs(LabsSpec labsSpec) {
        descriptor.labs(labsSpec);
        return this;
    }

    /**
     * Sets the plot title using a concise label spec.
     *
     * @param title the main plot title
     * @return this {@code Plot} instance for fluid chaining
     */
    public Plot<DF> labs(String title) {
        descriptor.labs(title);
        return this;
    }

    /**
     * Sets the plot title and axis labels using a concise label spec.
     *
     * @param title the main plot title
     * @param xLabel the X-axis label
     * @param yLabel the Y-axis label
     * @return this {@code Plot} instance for fluid chaining
     */
    public Plot<DF> labs(String title, String xLabel, String yLabel) {
        descriptor.labs(title, xLabel, yLabel);
        return this;
    }

    /**
     * Applies a predefined descriptor.theme() to customize plot appearance.
     *
     * @param customTheme the descriptor.theme() instance (e.g.,
     * {@code Theme.theme_gray()}, {@code new DarkTheme()})
     * @return this {@code Plot} instance for fluid chaining
     */
    public Plot<DF> theme(Theme customTheme) {
        descriptor.theme(customTheme);
        return this;
    }

    /**
     * Applies an inline descriptor.theme() modification via a lambda
     * configurator expression.
     *
     * @param config a lambda configurator (e.g.,
     * {@code t -> t.plotBackground(Color.WHITE)})
     * @return this {@code Plot} instance for fluid chaining
     */
    public Plot<DF> theme(ThemeConfigurator config) {
        // 1. Create a safe, isolated derivation from the current plot theme
        var derived = Theme.derive(getTheme());

        // 2. Apply user lambda to the copy
        config.configure(derived);

        // 3. Store the mutated but completely isolated DerivedTheme back
        descriptor.theme(derived);

        return this;
    }

    /**
     * Sets the quality-vs-speed tradeoff for interactive rendering. When the
     * plot renders to a vector backend ({@code SvgDrawSurface}) the effective
     * mode is always {@link RenderMode#FULL}, so exported or zoomed vector
     * output never loses fidelity regardless of this setting.
     *
     * @param renderMode the {@link RenderMode} to apply
     * @return this {@code Plot} instance for fluid chaining
     */
    public Plot<DF> renderMode(RenderMode renderMode) {
        descriptor.renderMode(renderMode);
        return this;
    }

    /**
     * The configured quality-vs-speed tradeoff for interactive rendering.
     *
     * @return the configured {@link RenderMode}
     */
    public RenderMode renderMode() {
        return descriptor.renderMode();
    }

    /**
     * Returns the currently applied descriptor.theme().
     *
     * @return the active {@link Theme}
     */
    public Theme getTheme() {
        return descriptor.theme();
    }

    /**
     * Applies the given theme to this plot: it replaces the descriptor's theme
     * (which the render path reads), re-resolves the figure's theme so the pane
     * background follows, and repaints the backing canvas.
     *
     * @param customTheme the theme to apply
     */
    @Override
    public void applyTheme(Theme customTheme) {
        descriptor.theme(customTheme);
        super.applyTheme(customTheme);
    }

    /**
     * Returns the spec this plot renders. A composite node
     * ({@code ComposedPlot}) uses the descriptor to build aligned
     * {@link PlotDescriptor#copy() copies} of the plot (stamping shared
     * {@link PlotDescriptor#panelInsets(double, double, double, double)} for
     * panel alignment, or suppressing guides for a collected legend) without
     * touching this node's transient rendering state.
     *
     * @return this plot's descriptor
     */
    public PlotDescriptor<DF> descriptor() {
        return descriptor;
    }

    /**
     * The natural panel insets this plot would use when rendered at the given
     * size: the left margin grows to fit the widest y-tick label (or a
     * right-hand y axis moves it right), the top reserves the axis-title offset
     * for a top-positioned x axis, and the bottom carries the x-axis label
     * zone. Guide strips are <em>not</em> included, so a compositor can stamp
     * these base insets on a {@link PlotDescriptor#copy()} and let each plot's
     * own guides grow the right/top margins once more without double-counting.
     *
     * @param plotWidth the width the plot would render at
     * @param plotHeight the height the plot would render at
     * @return the base panel insets (left, right, top, bottom)
     */
    public PlotDescriptor.PanelInsets insetsFor(double plotWidth, double plotHeight) {
        if (plotWidth <= 0 || plotHeight <= 0) {
            return new PlotDescriptor.PanelInsets(0.0, 0.0, 0.0, 0.0);
        }
        var fixed = descriptor.panelInsets();
        if (fixed != null) {
            return fixed;
        }
        // Mirror the base margins of computeLayoutInternal, minus the guide
        // strips: the grown y-label width, the top-positioned x-axis offset,
        // and the axis label zones.
        prepare();
        double predicted = predictedWidestYLabelWidth();
        boolean yRight = gridAxisYSwitched()
                || (descriptor.facet() == null && descriptor.scaleSpec().isYAxisRight());
        double grown = Math.max(descriptor.theme().axisLabelZone(),
                (double) Math.round(10.0 + predicted + 30.0));
        double ml = Math.max(descriptor.theme().axisLabelZone(), yRight ? 0.0 : grown);
        boolean xTop = gridAxisXSwitched()
                || (descriptor.facet() == null && descriptor.scaleSpec().isXAxisTop());
        double mt = descriptor.theme().axisLabelZone()
                + (xTop ? descriptor.theme().axisTitleOffset() : 0.0);
        double mr = Math.max(descriptor.theme().axisLabelZone(), yRight ? grown : 0.0);
        double mb = descriptor.theme().axisLabelZone();
        return new PlotDescriptor.PanelInsets(ml, mr, mt, mb);
    }

    /**
     * Configures axis scale breaks and limits.
     *
     * @param configurator the scale configurator
     * @return this {@code Plot} instance for fluid chaining
     */
    public Plot<DF> scales(ScaleConfigurator configurator) {
        configurator.configure(this);
        return this;
    }

    /**
     * Configures the coordinate system transformation for the plot.
     *
     * @param coord the coordinate system (e.g.,
     * {@code CoordCartesian.cartesian()} or {@code Coords.coordFlip()})
     * @return this {@code Plot} instance for fluid chaining
     */
    public Plot<DF> coord(Coord coord) {
        descriptor.coord(coord);
        return this;
    }

    /**
     * Configures the plot-level 3D light source used to shade the polygon 3D
     * geometries.
     * <p>
     * The plot-level light is an alternative to the coord-level
     * {@code coord3d().light(…)} — supplying both throws an
     * {@link IllegalArgumentException} at coord-apply time — and is overridden
     * by any layer-level {@code light(...)} on the polygon geometries.
     *
     * @param light the {@link Light3d}, or {@code null} for none
     * @return this {@code Plot} instance for fluid chaining
     */
    public Plot<DF> light(Light3d light) {
        descriptor.light(light);
        return this;
    }

    /** {@return the plot-level 3D light, or {@code null} when unset} */
    public Light3d light() {
        return descriptor.light();
    }

    /**
     * Configures the per-scale guide registry, following the
     * {@code guides()} function: register or suppress the guide for each
     * aesthetic (e.g. {@code Guides.none()} to hide all guides).
     *
     * @param guides the registry of guide overrides and suppressions
     * @return this {@code Plot} instance for fluid chaining
     */
    public Plot<DF> guides(Guides guides) {
        descriptor.guides(guides);
        return this;
    }

    /**
     * Merges aesthetic-to-guide mappings into the guide registry — the fluent
     * way to configure individual descriptor.guides(), following the
     * {@code guides(colour = Guides.guideLegend(), ...)}:
     *
     * <pre>{@code
     * .guides(guide(COLOR, guideLegend().ncol(2)),
     *         guide(FILL, guideColorbar()))
     * }</pre>
     * <p>
     * A later mapping for an already-configured aesthetic replaces the earlier
     * one.
     *
     * @param mappings the aesthetic/guide pairs to merge into the registry
     * @return this {@code Plot} instance for fluid chaining
     */
    public Plot<DF> guides(Guides.Mapping... mappings) {
        return guides(descriptor.guides().set(mappings));
    }

    /**
     * Registers a canvas geometry layer into the internal rendering pipeline.
     *
     * @param layer the canvas layer instance to register
     */
    @Override
    public ConfigTarget<DF> registerInternalLayer(Layer<DF> layer) {
        descriptor.registerInternalLayer(layer);
        return this;
    }

    /**
     * Registers a canvas geometry layer with a local aesthetic mapping
     * override.
     * <p>
     * The local mapping shadows the plot-global {@code aes()} on a per-geom
     * basis: aesthetics it sets override the global ones for this layer only
     * (see {@link Aes#overrideWith(Aes)}), so different geometry layers can
     * render against different mappings even though they share one plot.
     *
     * @param layer the canvas layer instance to register
     * @param localAes local aesthetic mappings overriding global mappings
     */
    @Override
    public ConfigTarget<DF> registerInternalLayer(Layer<DF> layer, Aes localAes) {
        descriptor.registerInternalLayer(layer, localAes);
        return this;
    }

    /**
     * Registers one or more data-free annotation layers into the plot. This is
     * the gog4j counterpart of the {@code annotate()}: each layer carries
     * its own scalar coordinates and draws directly onto the panel via the
     * active coordinate system.
     *
     * @param layers the annotation layers to register
     * @return this {@code Plot} instance for fluid chaining
     */
    @SuppressWarnings({"unchecked", "varargs", "rawtypes"})
    public Plot<DF> annotate(Layer... layers) {
        for (var layer : layers) {
            registerInternalLayer((Layer<DF>) layer);
        }
        return this;
    }

    /**
     * Assembles an all-in-one layer from a stat-consuming geometry, a stat, a
     * position adjustment, a layer-local mapping, and a params bag — the gog4j
     * counterpart of the
     * {@code Plot#layer(...)}.
     * <p>
     * The {@code geom} must implement {@link StatHost} (the bar-family geoms      {@code Geoms.bar()}, {@code Geoms.col()}, {@code Geoms.histogram()},
     * {@code Geoms.freqpoly()} do): it is switched into stat-consuming mode and
     * draws the {@code stat}'s output rather than raw columns, so the stat's
     * computed variables can be referenced from {@code mapping} via
     * {@link AesValue#afterStat(AesValue.ComputedVariable)} (e.g.
     * {@code aes().fill(AesValue.afterStat(AesValue.ComputedVariable.COUNT))}).
     * The {@code mapping} is merged over the plot-global {@code aes()} for this
     * layer only (the {@code aesthetic inheritance}); {@code params} carries both
     * the stat's numeric config and the geom's constant aesthetic defaults.
     * <p>
     * Example:
     * <pre>{@code
     * plot.layer(Geoms.barGeom(), Stats.count(), Positions.identity(), aes(),
     *         LayerParams.builder().fill(Color.web("#3182bd")).build());
     * }</pre>
     *
     * @param geom the stat-consuming geometry layer
     * @param stat the statistic to run
     * @param position the position adjustment
     * @param mapping the layer-local aesthetic mapping, merged over the global
     * one
     * @param params the stat config and geom aesthetic defaults
     * @return this {@code Plot} instance for fluid chaining
     */
    @Override
    public Plot<DF> layer(Layer<DF> geom, Stat<DF> stat, PositionAdjust position,
            Aes mapping, LayerParams params) {
        descriptor.layer(geom, stat, position, mapping, params);
        return this;
    }

    /**
     * Convenience overload of
     * {@link #layer(Layer, Stat, PositionAdjust, Aes, LayerParams)} with the
     * identity position and an empty params bag.
     *
     * @param geom the stat-consuming geometry layer
     * @param stat the statistic to run
     * @param mapping the layer-local aesthetic mapping, merged over the global
     * one
     * @return this {@code Plot} instance for fluid chaining
     */
    public Plot<DF> layer(Layer<DF> geom, Stat<DF> stat, Aes mapping) {
        return layer(geom, stat, new PositionAdjust.Identity(), mapping, LayerParams.empty());
    }

    /**
     * Binds a geometry with its {@linkplain Layer#defaultStat() default stat}
     * and position adjustment — the ergonomic form of
     * {@link #layer(Layer, Stat, PositionAdjust, Aes, LayerParams)} for the
     * common case. A {@code GeomBar} therefore runs {@code Stats.count()}, a
     * {@code GeomCol} draws its raw values, and so on, just as the default's
     * per-geom defaults would.
     *
     * @param geom the stat-consuming geometry layer
     * @param mapping the layer-local aesthetic mapping, merged over the global
     * one
     * @return this {@code Plot} instance for fluid chaining
     */
    public Plot<DF> layer(Layer<DF> geom, Aes mapping) {
        var stat = geom.defaultStat();
        if (geom instanceof StackableGeom) {
            return layer(geom, stat, ((StackableGeom<DF>) geom).positionAdjust(), mapping, LayerParams.empty());
        }
        return layer(geom, stat, new PositionAdjust.Identity(), mapping, LayerParams.empty());
    }

    /**
     * Binds a geometry with its {@linkplain Layer#defaultStat() default stat}
     * and no layer-local mapping (the plot-global {@code aes()} applies).
     *
     * @param geom the stat-consuming geometry layer
     * @return this {@code Plot} instance for fluid chaining
     */
    public Plot<DF> layer(Layer<DF> geom) {
        return layer(geom, null);
    }

    /**
     * The effective aesthetic mapping for a layer: its registered local mapping
     * overlaid on the plot-global {@code aes()}, or the global mapping itself
     * when the layer carries no local override.
     *
     * @param layer the registered geometry layer
     * @return the merged {@code Aes} the layer should render against
     */
    private Aes effectiveAes(Layer<DF> layer) {
        var local = descriptor.localAesByLayer().get(layer);
        return local == null ? descriptor.aes() : descriptor.aes().overrideWith(local);
    }

    /**
     * Guards a layer configured with {@code positionOnFace()}: while the
     * guard is open, the 3-D coordinate system projects the layer's marks onto
     * the configured cube face instead of their natural 3-D positions. A 3-D
     * layer (mapping a {@code z} aesthetic, {@code axes = null}) is flattened
     * onto the face; a 2-D layer (x/y only, two in-face axes) has its x/y
     * placed along the face plane. Closing the guard restores the coordinate
     * system for the next layer.
     *
     * @param layer the layer about to render
     * @return the face-guard, possibly a no-op, to close after the layer drew
     */
    private FaceRenderGuard beginFaceRender(Layer<DF> layer) {
        if (!(layer.positionAdjust() instanceof PositionAdjust.PositionOnFace pf)) {
            return FaceRenderGuard.NONE;
        }
        if (!(descriptor.coord() instanceof Coord3D coord3d)) {
            throw new IllegalArgumentException(
                    "positionOnFace requires a 3-D coordinate system; bind coord3d() (or a 3-D geom) to the plot");
        }
        if (zAes() == null) {
            throw new IllegalArgumentException(
                    "positionOnFace requires a 3-D scene: map a z aesthetic (e.g. via aes().z(\"...\") or Geoms.point3d())");
        }
        boolean native3d = isNative3d(layer);
        if (pf.axes() == null && !native3d) {
            throw new IllegalArgumentException("positionOnFace on a 2-D layer needs the two in-face axes, e.g. "
                    + "Positions.positionOnFace(CubeFace.ZMIN, \"x\", \"y\")");
        }
        if (pf.axes() != null && native3d) {
            throw new IllegalArgumentException("positionOnFace on a 3-D layer flattens it and cannot take layer axes; "
                    + "omit them, e.g. Positions.positionOnFace(CubeFace.ZMIN)");
        }
        coord3d.beginFaceRender(pf.face(), pf.axes());
        return new FaceRenderGuard(coord3d, pf.face());
    }

    /**
     * Whether the layer is a native 3-D geometry — one that projects every mark
     * through the cube camera rather than placing x/y marks on the panel plane.
     * The plot uses this to distinguish {@code positionOnFace} flattening
     * (3-D layers, {@code axes = null}) from 2-D layer placement (two in-face
     * axes), instead of relying on the merged aesthetic mapping where a global
     * {@code z} column would misclassify 2-D layers.
     */
    private boolean isNative3d(Layer<DF> layer) {
        return layer instanceof GeomPoint3d || layer instanceof GeomSegment3d
                || layer instanceof GeomPolygon3d || layer instanceof GeomText3d;
    }

    /**
     * Closes over a transient {@link Coord3D#beginFaceRender} state so a
     * {@code positionOnFace} layer leaves the coordinate system untouched for
     * the layers around it. The guard can also clip the layer's marks to the
     * projected face polygon — the face plane of the cube — so marks never
     * spill past the cube borders.
     */
    private static final class FaceRenderGuard implements AutoCloseable {
        private static final FaceRenderGuard NONE = new FaceRenderGuard(null, null);

        private final Coord3D coord3d;
        private final CubeFace face;

        private FaceRenderGuard(Coord3D coord3d, CubeFace face) {
            this.coord3d = coord3d;
            this.face = face;
        }

        /**
         * Opens a clip to the projected face polygon, returning a guard that
         * restores the previous clip when closed. A no-op for layers not placed
         * on a face.
         *
         * @param gc the target drawing surface
         * @return the clip guard to close after the layer drew
         */
        ClipGuard clip(DrawSurface gc) {
            if (coord3d == null || face == null) {
                return () -> { };
            }
            gc.save();
            double[] p = coord3d.facePolygon(face);
            gc.beginPath();
            gc.moveTo(p[0], p[1]);
            gc.lineTo(p[2], p[3]);
            gc.lineTo(p[4], p[5]);
            gc.lineTo(p[6], p[7]);
            gc.closePath();
            gc.clip();
            return gc::restore;
        }

        @Override
        public void close() {
            if (coord3d != null) {
                coord3d.endFaceRender();
            }
        }
    }

    /** Restores a clip opened by {@link FaceRenderGuard#clip}. */
    @FunctionalInterface
    private interface ClipGuard {
        /** Restores the previously active clip. */
        void close();
    }

    /**
     * The z aesthetic that drives the 3-D cube: the plot-global mapping, or —
     * when a layer maps a computed z ({@code Geoms.bar3d()}'s default
     * {@code z = afterStat(count)}) — the first layer that maps one.
     *
     * @return the effective z aesthetic value, or {@code null} when unset
     */
    private String zAes() {
        if (descriptor.aes().z() != null) {
            return descriptor.aes().z();
        }
        for (var layer : descriptor.geoms()) {
            var z = effectiveAes(layer).z();
            if (z != null) {
                return z;
            }
        }
        return null;
    }

    /**
     * Configures the 3-D cube for the current panel: resolves the z domain
     * (a raw column through the panel extractor, a computed column such as
     * {@code bar_3d}'s {@code count} through the stat-merged scales), sets the
     * data bounds and axis labels, and draws the cube background.
     */
    private void setupCoord3d(Coord3D coord3d, DF df, double dataMinX, double dataMaxX,
                              double dataMinY, double dataMaxY, boolean xDiscrete, List<?> xCats,
                              boolean yDiscrete, List<?> yCats,
                              ScaleTransform xAxisTransform, ScaleTransform yAxisTransform,
                              double innerXMin, double innerXMax,
                              double innerYMax, double innerYMin,
                              double canvasW, double canvasH, DrawSurface gc) {
        String zCol = zAes();
        if (zCol == null) {
            return;
        }
        if (coord3d.getLight() != null && descriptor.light() != null) {
            throw new IllegalArgumentException(
                    "supply the 3D light on either coord3d(light=...) or Plot.light(...), not both");
        }
        String bare = Aes.statColumn(zCol);
        boolean raw = !descriptor.extractor().getColumn(df, bare).isEmpty();
        MinMax zMinMax = raw ? descriptor.extractor().getMinMax(df, bare) : scales().numericRange(bare);
        if (zMinMax == null) {
            zMinMax = MinMax.UNIT;
        }
        // Explicit z limits (zlim / scaleZContinuous(limits=...)) override the
        // data-driven z domain, mirroring the x/y limit handling.
        var zLims = descriptor.scaleSpec().getZLimits();
        if (zLims != null) {
            zMinMax = zLims;
        }
        var applyExpansion = descriptor.geoms().stream().anyMatch(Layer::wantsDefaultExpansion);
        var zBounds = applyExpansion && coord3d.expand()
                ? expandBounds(zMinMax, descriptor.scaleSpec().getZExpand(), null)
                : zMinMax;
        for (var layer : descriptor.geoms()) {
            if (layer instanceof GeomPolygon3d<?>) {
                var zWanted = ((GeomPolygon3d<DF>) layer).expandSolidZ(zBounds, plotContextFor(layer));
                zBounds = new MinMax(Math.min(zBounds.min(), zWanted.min()),
                        Math.max(zBounds.max(), zWanted.max()));
            }
        }
        // Continuous x/y get the same default scale headroom as z, so the
        // outer pretty ticks and gridlines sit inside the cube
        // instead of poking past its corners. Discrete banded axes are already
        // padded to [-0.5, N-0.5] and must stay flush, so they are not widened.
        var xBounds = new MinMax(dataMinX, dataMaxX);
        var yBounds = new MinMax(dataMinY, dataMaxY);
        if (applyExpansion && coord3d.expand()) {
            if (!xDiscrete) {
                xBounds = expandBounds(xBounds, descriptor.scaleSpec().getXExpand(), xAxisTransform);
            }
            if (!yDiscrete) {
                yBounds = expandBounds(yBounds, descriptor.scaleSpec().getYExpand(), yAxisTransform);
            }
        }
        coord3d.setDataBounds(xBounds.min(), xBounds.max(), yBounds.min(), yBounds.max(),
                zBounds.min(), zBounds.max());
        if (xDiscrete && xCats != null) {
            coord3d.setXCategories(xCats);
        }
        if (yDiscrete && yCats != null) {
            coord3d.setYCategories(yCats);
        }
        if (raw && descriptor.extractor().columnType(df, bare) != DataExtractor.ColumnType.NUMBER) {
            var zCats = descriptor.extractor().getColumn(df, bare).stream()
                    .filter(Objects::nonNull)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            coord3d.setZCategories(new ArrayList<>(zCats));
        }
        coord3d.xLabel(descriptor.aes().x() != null ? descriptor.aes().x() : "x");
        coord3d.yLabel(descriptor.aes().y() != null ? descriptor.aes().y() : "y");
        coord3d.zLabel(bare);
        if (descriptor.scaleSpec().hasZBreaks()) {
            coord3d.zBreaks(descriptor.scaleSpec().getZBreaks());
            if (descriptor.scaleSpec().getZLabels() != null) {
                coord3d.zLabels(descriptor.scaleSpec().getZLabels());
            }
        }
        coord3d.prepare(descriptor.theme());
        coord3d.setPanelBounds(innerXMin, innerYMax, innerXMax - innerXMin, innerYMin - innerYMax);
        coord3d.setCanvasBounds(0, 0, canvasW, canvasH);
        coord3d.renderBackground(gc);
    }

    /**
     * The distinct, sorted constant colour labels carried by the data-less
     * {@link GeomFunction} layers. Each such layer maps its {@code color}
     * aesthetic to a label rather than a data column, and the labels gathered
     * here drive both the palette assignment and the synthetic legend keys.
     *
     * @return the sorted distinct function colour labels
     */
    private List<String> functionColorLabels() {
        return descriptor.geoms().stream()
                .filter(GeomFunction.class::isInstance)
                .map(this::effectiveAes)
                .map(Aes::color)
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .toList();
    }

    /**
     * Resolves the automatic palette colour for every label-bearing function
     * layer and stores it on the layer, so its curve strokes with the same
     * colour its legend key carries. Must run before both rendering and guide
     * collection so the stroke and legend agree.
     */
    private void assignFunctionColors() {
        var labels = functionColorLabels();
        for (var layer : descriptor.geoms()) {
            if (layer instanceof GeomFunction<DF> gf) {
                var color = effectiveAes(layer).color();
                gf.setAssignedColor(color == null ? null : Scale.resolveConstantColor(labels, color, descriptor.theme().categoricalPalette()));
            }
        }
    }

    /**
     * Returns the plot scale specification.
     *
     * @return the {@link ScaleSpec} instance
     */
    @Override
    public ScaleSpec getScaleSpec() {
        return descriptor.scaleSpec();
    }

    /**
     * Applies an {@link Expansion} to a raw data-range pair. For a transformed
     * (non-identity) scale the expansion runs in *transformed* space —
     * following convention — then maps back to data units, so a logarithmic axis
     * whose data start close to its lower bound never walks its domain into
     * non-positive territory (which the log clamp would otherwise pin to a
     * subnormal and flood with spurious decades).
     *
     * @param bounds the {@code [min, max]} data range
     * @param expansion the expansion to apply
     * @param transform the axis scale transform ({@code null} or identity =
     * linear)
     * @return the expanded range
     */
    private static MinMax expandBounds(MinMax bounds, Expansion expansion, ScaleTransform transform) {
        if (transform == null || Objects.equals(transform, ScaleTransform.IDENTITY)) {
            return expansion.expand(bounds.min(), bounds.max());
        }
        var lo = transform.forward(transform.clampDomain(bounds.min()));
        var hi = transform.forward(transform.clampDomain(bounds.max()));
        var expanded = expansion.expand(lo, hi);
        return new MinMax(transform.inverse(expanded.min()), transform.inverse(expanded.max()));
    }

    /**
     * The raw column a position aesthetic maps directly, or {@code null} when
     * the aesthetic is referential (afterStat / afterScale).
     */
    private static String positionColumn(AesValue value) {
        return AesValue.rawColumn(value);
    }

    /**
     * The raw column that seeds the given position axis. A direct mapping
     * trains from its own column; an afterScale reference to the other axis
     * reuses that axis's raw column so a forwarded position still gets a
     * sensible domain.
     */
    private static String positionTrainColumn(Aes aes, boolean xAxis) {
        var value = xAxis ? aes.xValue() : aes.yValue();
        var raw = AesValue.rawColumn(value);
        if (raw != null) {
            return raw;
        }
        if (value instanceof AesValue.AfterScale ref
                && ref.aesthetic() == (xAxis ? Aesthetic.Y : Aesthetic.X)) {
            return AesValue.rawColumn(xAxis ? aes.yValue() : aes.xValue());
        }
        return null;
    }

    /**
     * The receiving-axis domain of a continuous position aesthetic. Direct
     * mappings report their raw column range. An afterScale reference instead
     * maps the referenced column through the source axis transform, so
     * forwarded values land in the domain of the axis they feed.
     *
     * @param sourceTransform the source axis transform, or {@code null} when
     * the position maps a raw column
     */
    private static <DF> MinMax positionBounds(DataExtractor<DF> extractor, DF df, Aes aes,
            boolean xAxis, String trainColumn, ScaleTransform sourceTransform) {
        var position = xAxis ? aes.xValue() : aes.yValue();
        if (position instanceof AesValue.AfterScale) {
            if (sourceTransform == null || trainColumn == null) {
                return MinMax.UNIT;
            }
            double min = Double.POSITIVE_INFINITY;
            double max = Double.NEGATIVE_INFINITY;
            for (var v : extractor.getColumn(df, trainColumn)) {
                if (v == null) {
                    continue;
                }
                var d = Values.toDouble(v, Double.NaN);
                if (Double.isFinite(d)) {
                    var t = sourceTransform.forward(d);
                    min = Math.min(min, t);
                    max = Math.max(max, t);
                }
            }
            return min <= max ? new MinMax(min, max) : MinMax.UNIT;
        }
        return extractor.getMinMax(df, trainColumn);
    }

    /**
     * The effective scale transform of an axis: the {@code coordTrans} override
     * wins over the scale spec, and a transformed discrete axis is rejected (a
     * coordinate transform needs a continuous scale).
     */
    private static ScaleTransform axisTransform(Coord coord, ScaleTransform fromSpec, boolean discrete, String axis) {
        if (coord instanceof Coord2D coord2d) {
            var override = axis.equals("x") ? coord2d.transX() : coord2d.transY();
            if (override != null) {
                if (discrete) {
                    throw new IllegalArgumentException(
                            "coordTrans requires a continuous scale on every transformed axis");
                }
                return override;
            }
        }
        return fromSpec;
    }

    /**
     * The width of a data range in transformed (scale) units, mirroring the
     * span {@link Scale} maps onto screen pixels: forward-transform both
     * clamped endpoints and measure the distance between them.
     */
    private static double transformedSpan(double lo, double hi, ScaleTransform transform) {
        double a = transform.forward(transform.clampDomain(lo));
        double b = transform.forward(transform.clampDomain(hi));
        return Math.abs(b - a);
    }

    /**
     * Draws the complete plot onto an arbitrary surface at the given size.
     * <p>
     * This is the single rendering pipeline: {@link #redraw()} points it at a
     * JavaFX canvas, while an exporter can point it at a vector backend and get
     * output that is identical by construction rather than by maintenance.
     *
     * @param surface the surface to draw onto
     * @param plotWidth the width to lay the plot out at, in pixels
     * @param plotHeight the height to lay the plot out at, in pixels
     */
    @Override
    public void renderTo(DrawSurface surface, double plotWidth, double plotHeight) {
        if (plotWidth <= 0 || plotHeight <= 0) {
            return;
        }

        var renderStart = System.nanoTime();
        var renderEvt = new PlotRenderEvent();
        boolean enabled = renderEvt.isEnabled();
        if (enabled) {
            if (descriptor.facet() != null) {
                renderEvt.facetMode = descriptor.facet().isGrid() ? "grid" : "wrap";
            } else {
                renderEvt.facetMode = "none";
            }
            renderEvt.layerCount = descriptor.geoms().size();
        }
        renderEvt.begin();
        try {
            renderToInternal(surface, plotWidth, plotHeight, enabled ? renderEvt : null);
        } finally {
            renderEvt.end();
            renderEvt.commit();
            recordRenderNanos(renderStart);
        }
    }

    private void renderToInternal(DrawSurface surface, double plotWidth, double plotHeight,
            PlotRenderEvent renderEvt) {
        if (plotWidth <= 0 || plotHeight <= 0) {
            return;
        }

        // 1. Partition data for facets
        Map<Object, DF> partitions = null;
        FacetGrid<DF> grid = null;
        int numPanels;
        if (descriptor.facet() != null && descriptor.facet().isGrid()) {
            grid = descriptor.extractor().partitionGrid(descriptor.data(), descriptor.facet().getRowVars(), descriptor.facet().getColVars(),
                    resolvedMarginVars(descriptor.facet().getOptions()));
            numPanels = grid.numRows() * grid.numCols();
        } else {
            var partEvt = new FacetPartitionEvent();
            partEvt.rows = descriptor.extractor().getRowCount(descriptor.data());
            partEvt.begin();
            try {
                partitions = partitionData();
            } finally {
                partEvt.groups = partitions.size();
                partEvt.end();
                partEvt.commit();
            }
            numPanels = partitions.size();
        }

        if (renderEvt != null) {
            renderEvt.panelCount = numPanels;
        }

        // Prepare phase: eagerly resolve the shared scales once per render
        // pass, then hand every layer its plot-scoped context so it can compute
        // its per-render state over the global data. The prepared LayerData is
        // threaded through the panel render/locate calls that follow.
        prepare();

        // Resolve palette colours for label-bearing function layers up front so
        // their strokes and legend keys agree in a single pass.
        assignFunctionColors();

        // 2. Ordered, unclipped render layers back to front
        var guideInlays = resolveInlayPositions(collectGuideInlays(descriptor.guides()));
        boolean timing = Boolean.parseBoolean(System.getenv().getOrDefault("GOG_TIMING", "false"));
        long t0 = System.nanoTime();
        // Wide y-tick labels cannot fit beside their rotated title in the
        // 50px base zone; the default grows the left margin to make room. The
        // width is predicted from the data before any drawing so a single
        // render pass suffices.
        double predicted = predictedWidestYLabelWidth();
        // The grown margin stays integral: fractional panel origins push
        // every primitive onto subpixels, costing ~10x in the raster paths.
        // A right-hand y axis needs the grown margin there instead of on the
        // left, where its labels no longer sit.
        boolean yRight = gridAxisYSwitched() || (descriptor.facet() == null && descriptor.scaleSpec().isYAxisRight());
        var fixedInsets = descriptor.panelInsets();
        double grown = Math.max(descriptor.theme().axisLabelZone(), (double) Math.round(10.0 + predicted + 30.0));
        double minMarginRight = yRight ? grown : 0.0;
        PlotLayout layout;
        if (fixedInsets != null) {
            // computeLayoutInternal honors descriptor.panelInsets() over the
            // min-margin arguments, so plain zeros are safe here.
            layout = computeLayout(plotWidth, plotHeight, guideInlays, 0.0, 0.0);
        } else {
            double minMarginLeft = yRight ? 0.0 : grown;
            layout = computeLayout(plotWidth, plotHeight, guideInlays,
                    Math.max(descriptor.theme().axisLabelZone(), minMarginLeft),
                    minMarginRight);
        }
        long t1 = System.nanoTime();
        // Stat-driven y axes (e.g. a density's computed count) carry no mapped
        // column to predict from; a probe pass into a scratch surface harvests
        // the real tick-label width before anything is committed. The result
        // is cached per canvas size — the stat scale depends on the binning,
        // which depends on the panel geometry.
        if (fixedInsets == null
                && descriptor.coord() instanceof Coord2D c2d && descriptor.aes().y() == null && !descriptor.coord().isFlipped()) {
            if (probeWidth < 0 || probeCanvasW != plotWidth || probeCanvasH != plotHeight) {
                var scratch = createScratchSurface(surface, plotWidth, plotHeight);
                if (grid != null) {
                    drawGridPanels(scratch, grid, plotWidth, plotHeight, layout);
                } else {
                    drawFacetsGrid(scratch, partitions, numPanels, plotWidth, plotHeight, layout);
                }
                probeWidth = c2d.widestYLabelWidth();
                probeCanvasW = plotWidth;
                probeCanvasH = plotHeight;
            }
            layout = computeLayout(plotWidth, plotHeight, guideInlays,
                    yRight ? descriptor.theme().axisLabelZone() : Math.max(descriptor.theme().axisLabelZone(),
                            (double) Math.round(10.0 + probeWidth + 30.0)),
                    minMarginRight);
        }
        if (descriptor.isGuidesOnly()) {
            drawGuides(surface, layout);
            return;
        }
        drawPlotBackground(surface, plotWidth, plotHeight);
        drawMainTitle(surface, plotWidth, plotHeight);
        drawGuides(surface, layout);
        long t2 = System.nanoTime();
        if (grid != null) {
            drawGridPanels(surface, grid, plotWidth, plotHeight, layout);
        } else {
            drawFacetsGrid(surface, partitions, numPanels, plotWidth, plotHeight, layout);
        }
        long t3 = System.nanoTime();
        drawAxisTitles(surface, plotWidth, plotHeight, layout);
        drawInsideGuides(surface, layout);
        if (timing) {
            System.out.printf("TIMING predict=%.0fus guides=%.0fus facets=%.0fus titles+inside=%.0fus%n",
                    (t1 - t0) / 1e3, (t2 - t1) / 1e3, (t3 - t2) / 1e3,
                    (System.nanoTime() - t3) / 1e3);
        }
    }

    /**
     * A throwaway surface for measurement passes, matching the real one's kind.
     */
    private DrawSurface createScratchSurface(DrawSurface surface, double width, double height) {
        if (surface instanceof SvgDrawSurface) {
            return new SvgDrawSurface(width, height, null);
        }
        var canvas = new Canvas(width, height);
        return new FxDrawSurface(canvas.getGraphicsContext2D());
    }

    /**
     * The effective quality-vs-speed tradeoff for a rendering pass: a vector
     * backend always renders {@link RenderMode#FULL} (exported or zoomed SVG
     * shows every detail regardless of the interactive mode), while an
     * interactive Canvas backend uses the plot's configured mode.
     *
     * @param surface the target drawing surface
     * @return the effective {@link RenderMode}
     */
    private RenderMode effectiveRenderMode(DrawSurface surface) {
        return surface instanceof SvgDrawSurface ? RenderMode.FULL : descriptor.renderMode();
    }

    /**
     * Pre-computes the layer preparation phase on the calling thread, if it has
     * not run yet. {@link #prepare()} is deterministic over the immutable spec,
     * so the expensive scale/stat work can be done once — e.g. on the sampler's
     * loader thread — and every subsequent redraw on the JavaFX thread skips
     * it. Idempotent and safe to call repeatedly.
     */
    @Override
    public void prepareAsync() {
        prepare();
    }

    /**
     * Runs the preparation phase of the layer lifecycle: eagerly resolves the
     * shared scales over the global DataFrame once, builds each layer's
     * plot-scoped context, and lets every layer compute its per-render state
     * over the global data. The resolved scales, contexts, and prepared
     * {@link LayerData} are retained so the facet panel rendering, legend, and
     * hover hit-testing all reuse exactly what was prepared.
     */
    private void prepare() {
        synchronized (prepareLock) {
            if (prepared) {
                return;
            }
            prepareLocked();
        }
    }

    /**
     * The prepare() body, run at most once under {@link #prepareLock}. Split
     * out so the guard and the {@code prepared} publication are atomic with the
     * body, preventing a concurrent loader-thread prepare from clearing the
     * maps mid-render.
     */
    private void prepareLocked() {
        currentScales = ResolvedScales.forData(descriptor.data(), descriptor.extractor(), descriptor.scaleSpec(), descriptor.labs(), descriptor.theme());
        plotContexts.clear();
        preparedData.clear();
        var evt = new PreparePhaseEvent();
        evt.layerCount = descriptor.geoms().size();
        evt.begin();
        try {
            for (int i = 0; i < descriptor.geoms().size(); i++) {
                var layer = descriptor.geoms().get(i);
                var layerEvt = new LayerPrepareEvent();
                layerEvt.layerType = layer.getClass().getSimpleName();
                layerEvt.layerIndex = i;
                layerEvt.begin();
                try {
                    var ctx = plotContextFor(layer);
                    preparedData.put(layer, layer.prepare(ctx));
                } finally {
                    layerEvt.end();
                    layerEvt.commit();
                }
            }
        } finally {
            evt.end();
            evt.commit();
        }
        var extra = new HashMap<String, List<?>>();
        for (var layer : descriptor.geoms()) {
            var ld = preparedData.get(layer);
            if (ld instanceof LayerData.StatLayerData<?> sld) {
                for (var name : sld.statData().columnNames()) {
                    extra.putIfAbsent(name, sld.statData().column(name));
                }
            }
        }
        if (!extra.isEmpty()) {
            currentScales = ResolvedScales.forData(descriptor.data(), descriptor.extractor(), descriptor.scaleSpec(), descriptor.labs(), extra, descriptor.theme());
            plotContexts.clear();
            for (var layer : descriptor.geoms()) {
                plotContextFor(layer);
            }
        }
        // An afterStat(...) expression fill shades against the expression's own
        // output, so register its scale up front — guide collection runs before
        // any panel draws and must see the same domain the bars will use.
        for (var layer : descriptor.geoms()) {
            var ld = preparedData.get(layer);
            if (!(ld instanceof LayerData.StatLayerData<?> sld)) {
                continue;
            }
            var effective = effectiveAes(layer);
            registerExpressionColourScale(currentScales, sld.statData(), descriptor.aes().colorValue());
            registerExpressionColourScale(currentScales, sld.statData(), descriptor.aes().fillValue());
            registerExpressionColourScale(currentScales, sld.statData(), effective.colorValue());
            registerExpressionColourScale(currentScales, sld.statData(), effective.fillValue());
        }
        prepared = true;
    }

    /**
     * Registers the continuous colour scale of an {@code afterStat} expression
     * value over its computed output, so colourbar guides can reuse the domain
     * the expression's rows are painted with. Plain stat references resolve
     * lazily from the stat column itself and need no pre-registration.
     *
     * @param scales the resolved scales to register the scale on
     * @param statData stat output the expression evaluates against
     * @param value the aesthetic value to inspect
     */
    private void registerExpressionColourScale(ResolvedScales<DF> scales, StatData statData, AesValue value) {
        if (!(value instanceof AesValue.AfterStatExpr expr) || value.column() == null) {
            return;
        }
        try {
            scales.registerContinuousValues(value.column(), expr.evaluate(statData));
        } catch (IllegalArgumentException missing) {
            // The stat output this layer prepared does not expose the
            // expression's required column; its rows carry no mapping here.
        }
    }

    /**
     * The plot-scoped context for a layer, carrying the layer's effective
     * aesthetic mapping and the shared resolved scales. Contexts are built once
     * during {@link #prepare()} and reused in every panel.
     *
     * @param layer the registered geometry layer
     * @return the layer's {@link PlotContext}
     */
    private PlotContext<DF> plotContextFor(Layer<DF> layer) {
        return plotContexts.computeIfAbsent(layer,
                l -> new PlotContext<>(descriptor.data(), descriptor.extractor(), effectiveAes(l),
                        descriptor.scaleSpec(), descriptor.labs(), descriptor.coord(), descriptor.theme(),
                        scales(), descriptor.renderMode(), descriptor.light()));
    }

    /**
     * The shared resolved scales, built lazily on first access.
     */
    private ResolvedScales<DF> scales() {
        if (currentScales == null) {
            currentScales = ResolvedScales.forData(descriptor.data(), descriptor.extractor(),
                    descriptor.scaleSpec(), descriptor.labs(), descriptor.theme());
        }
        return currentScales;
    }

    /**
     * Memoized {@link #predictedWidestYLabelWidth()}; data is fixed per plot.
     */
    private Double predictedYLabelCache;

    /**
     * Cached stat-axis probe result, keyed on the canvas size it was measured
     * at.
     */
    private double probeWidth = -1.0;
    private double probeCanvasW = -1.0;
    private double probeCanvasH = -1.0;

    /**
     * Widest y-axis tick label across the grid's label-drawing panels, from the
     * last grid pass.
     */
    private double gridWidestYLabelWidth = -1.0;

    /**
     * Predicts the widest y-axis tick label from the mapped column's global
     * range or categories — an upper-bound sibling of the per-panel labels
     * Coord2D measures while rendering. Facet subsets can only narrow the value
     * range, so this never underestimates materially.
     */
    private double predictedWidestYLabelWidth() {
        if (predictedYLabelCache != null) {
            return predictedYLabelCache;
        }
        if (!(descriptor.coord() instanceof Coord2D) || descriptor.extractor() == null) {
            return 0.0;
        }
        String column = descriptor.coord().isFlipped() ? positionTrainColumn(descriptor.aes(), true)
                : positionTrainColumn(descriptor.aes(), false);
        if (column == null) {
            return 0.0;
        }
        var helper = new Text();
        helper.setFont(descriptor.theme().tickLabelFont());
        var type = descriptor.extractor().columnType(descriptor.data(), column);
        var values = descriptor.extractor().getColumn(descriptor.data(), column);
        if (type == DataExtractor.ColumnType.NUMBER) {
            double min = Double.MAX_VALUE;
            double max = -Double.MAX_VALUE;
            for (var v : values) {
                if (v instanceof Number n) {
                    min = Math.min(min, n.doubleValue());
                    max = Math.max(max, n.doubleValue());
                }
            }
            if (min > max) {
                predictedYLabelCache = 0.0;
                return predictedYLabelCache;
            }
            var scale = new Scale(min, max, 0, 100, descriptor.scaleSpec().getYTransform());
            double widest = 0.0;
            for (var tick : scale.calculateTicks(5, null)) {
                helper.setText(Scale.formatTick(tick));
                widest = Math.max(widest, helper.getLayoutBounds().getWidth());
            }
            predictedYLabelCache = widest;
            return predictedYLabelCache;
        }
        if (type == DataExtractor.ColumnType.DATE) {
            predictedYLabelCache = 20.0; // four-digit years, like Coord2D's date labels
            return predictedYLabelCache;
        }
        double widest = 0.0;
        for (var category : Scale.uniqueCategories(descriptor.data(), descriptor.extractor(), column)) {
            helper.setText(descriptor.labs().map(String.valueOf(category)));
            widest = Math.max(widest, helper.getLayoutBounds().getWidth());
        }
        predictedYLabelCache = widest;
        return predictedYLabelCache;
    }

    /**
     * Layer 1: The undisturbed global plot background.
     */
    private void drawPlotBackground(DrawSurface gc, double width, double height) {
        gc.save();
        gc.setFill(descriptor.theme().paneBackground());
        gc.fillRect(0, 0, width, height);
        gc.restore();
    }

    /**
     * Layer 2: Fine-grained, reactive plot typography (Labs). Layer 2a: The
     * main plot title, centered over the full canvas.
     */
    private void drawMainTitle(DrawSurface gc, double plotWidth, @SuppressWarnings("unused") double plotHeight) {
        var titleText = descriptor.labs().titleIsEmpty() ? "" : descriptor.labs().title();

        gc.save();
        gc.setTextAlign(TextAlignment.CENTER);
        if (titleText != null) {
            gc.setFill(descriptor.theme().titleColor());
            gc.setFont(descriptor.theme().titleFont());
            gc.fillText(titleText, plotWidth / 2.0, descriptor.theme().titleOffset());
        }
        gc.restore();
    }

    /**
     * Layer 2b: The axis titles, positioned after the facet grid has rendered
     * so the coordinate system's tick-label measurements are available.
     */
    private void drawAxisTitles(DrawSurface gc, double plotWidth, double plotHeight, PlotLayout layout) {
        var resolvedLabels = descriptor.options().resolvedForStandalone();
        var xLabelText = descriptor.labs().xLabelIsEmpty() ? (descriptor.coord().isFlipped() ? descriptor.aes().y() : descriptor.aes().x()) : descriptor.labs().xLabel();
        var yLabelText = descriptor.labs().yLabelIsEmpty() ? (descriptor.coord().isFlipped() ? descriptor.aes().x() : descriptor.aes().y()) : descriptor.labs().yLabel();

        // Axis titles center on the panel, not the canvas.
        double mlF = layout.marginLeft();
        double mtF = layout.marginTop();
        double mrF = layout.marginRight();
        double mbF = layout.marginBottom();
        double panelLeft = mlF;
        double panelRight = plotWidth - mrF;
        double panelTop = mtF;
        double panelBottom = plotHeight - mbF;

        // Switched grid axes ride on the outer edge of the grid; a plain
        // plot's scales can ask for the top or right edge explicitly.
        boolean xTop = gridAxisXSwitched() || (descriptor.facet() == null && descriptor.scaleSpec().isXAxisTop());
        boolean yRight = gridAxisYSwitched() || (descriptor.facet() == null && descriptor.scaleSpec().isYAxisRight());

        gc.save();
        gc.setTextAlign(TextAlignment.CENTER);
        var axisStyle = AxisStyle.from(descriptor.theme());

        // X-axis title at the bottom (default) or top (switched) — skip for
        // 3D (rendered inside Coord3D) and polar (spokes carry the meaning;
        // a pie has no horizontal axis). Sits inside the 50px label zone; a
        // bottom guide strip claims the canvas edge below it.
        if (xLabelText != null && resolvedLabels.drawXTitles() && !(descriptor.coord() instanceof Coord3D) && !(descriptor.coord() instanceof CoordPolar)) {
            gc.setFill(axisStyle.titleColor());
            gc.setFont(axisStyle.titleFont());
            double xTitleY = xTop ? panelTop - descriptor.theme().axisTitleOffset() : panelBottom + descriptor.theme().axisTitleOffset();
            gc.fillText(xLabelText, (panelLeft + panelRight) / 2.0, xTitleY);
        }

        // 3. Y-axis title on the left (default) or right (switched) — skip
        //    for 3D (rendered inside Coord3D) and polar, which only has
        //    angular meaning.
        if (yLabelText != null && resolvedLabels.drawYTitles() && !(descriptor.coord() instanceof Coord3D) && !(descriptor.coord() instanceof CoordPolar)) {
            gc.setFill(axisStyle.titleColor());
            gc.setFont(axisStyle.titleFont());
            if (yRight) {
                gc.translate(yAxisTitleRight(layout, plotWidth), (panelTop + panelBottom) / 2.0);
                gc.rotate(90);
            } else {
                gc.translate(yAxisTitleX(layout), (panelTop + panelBottom) / 2.0);
                gc.rotate(-90);
            }
            gc.fillText(yLabelText, 0, 0);
        }

        gc.restore();
    }

    /**
     * {@return whether the grid's x-axis has been switched to the top}
     */
    private boolean gridAxisXSwitched() {
        if (descriptor.facet() == null || !descriptor.facet().isGrid()) {
            return false;
        }
        return descriptor.facet().getOptions().switchPreset() == GridOptions.GridSwitch.X
                || descriptor.facet().getOptions().switchPreset() == GridOptions.GridSwitch.BOTH;
    }

    /**
     * {@return whether the grid's y-axis has been switched to the right}
     */
    private boolean gridAxisYSwitched() {
        if (descriptor.facet() == null || !descriptor.facet().isGrid()) {
            return false;
        }
        return descriptor.facet().getOptions().switchPreset() == GridOptions.GridSwitch.Y
                || descriptor.facet().getOptions().switchPreset() == GridOptions.GridSwitch.BOTH;
    }

    /**
     * Computes the rotated y-axis title's right-hand pivot: centred in the free
     * zone between the panel's right edge and the right-hand tick labels (plus
     * any right guide strip), mirroring {@link #yAxisTitleX}.
     */
    private double yAxisTitleRight(PlotLayout layout, double plotWidth) {
        double maxXPix = plotWidth - layout.marginRight();
        boolean grid = descriptor.facet() != null && descriptor.facet().isGrid();
        double widest = grid
                ? Math.max(gridWidestYLabelWidth, 0.0)
                : (descriptor.coord() instanceof Coord2D c2d ? c2d.widestYLabelWidth() : 0.0);
        // Labels end at maxXPix + 10; the rotated title's glyphs extend ~half
        // its ascent around the pivot, so 8px keeps them clear of the label edge.
        double labelDerived = maxXPix + 10.0 + widest + 8.0;
        double stripLeft = -1.0;
        for (var strip : layout.strips()) {
            if (strip.position() == GuidePosition.RIGHT) {
                stripLeft = strip.x();
            }
        }
        if (stripLeft < 0) {
            return Math.min(labelDerived, plotWidth - 6.0);
        }
        double lo = Math.min(labelDerived, stripLeft - 8.0);
        double hi = stripLeft - 8.0;
        return (lo + hi) / 2.0;
    }

    /**
     * Computes the rotated y-axis title's pivot: one margin right of the widest
     * y-tick label (which ends at {@code minXPix - 10}), clamped to stay clear
     * of a left guide strip when one occupies the canvas edge.
     */
    private double yAxisTitleX(PlotLayout layout) {
        double minXPix = layout.marginLeft();
        boolean grid = descriptor.facet() != null && descriptor.facet().isGrid();
        double widest = grid
                ? Math.max(gridWidestYLabelWidth, 0.0)
                : (descriptor.coord() instanceof Coord2D c2d ? c2d.widestYLabelWidth() : 0.0);
        double stripRight = -1.0;
        for (var strip : layout.strips()) {
            if (strip.position() == GuidePosition.LEFT) {
                stripRight = strip.x() + strip.width()
                        + (descriptor.theme().guideBoxColor() != null ? descriptor.theme().guideBoxMargin() : 0.0);
            }
        }
        return yAxisTitleX(minXPix, widest, stripRight);
    }

    /**
     * Computes the rotated y-axis title's pivot. Without a left strip it sits
     * one margin right of the widest tick label; with one, it centers in the
     * free zone between the strip and the labels — hugging the labels when that
     * zone collapses.
     */
    static double yAxisTitleX(double minXPix, double widestLabelWidth, double leftStripRight) {
        // The rotated title's glyphs extend ~half its ascent around the
        // pivot, so 8px keeps them clear of the label edge.
        double labelDerived = minXPix - 10.0 - widestLabelWidth - 8.0;
        if (leftStripRight < 0) {
            return Math.max(labelDerived, 6.0);
        }
        double lo = leftStripRight + 8.0;
        double hi = Math.max(labelDerived, lo);
        return (lo + hi) / 2.0;
    }

    /**
     * The left strip's inset from the canvas edge: a small breathing margin, or
     * two box margins when a box frame needs its own room.
     */
    private double leftStripInset(double boxPad) {
        return boxPad > 0 ? 2 * boxPad : descriptor.theme().guideStripPadding();
    }

    /**
     * Layer 3: The combined guide rendering. Each scale that carries a colour
     * mapping emits its guide — a discrete legend for categorical columns, a
     * colourbar with ticks for continuous numeric or date columns — built
     * against the plot data and styled from the active descriptor.theme().
     * Explicit {@link Guides} overrides and suppressions replace the scale's
     * default guide.
     */
    private void drawGuides(DrawSurface gc, PlotLayout layout) {
        for (var strip : layout.strips()) {
            if (strip.position() != GuidePosition.INSIDE) {
                drawStrip(gc, strip);
            }
        }
    }

    /**
     * Draws the inside descriptor.guides() over the panel. Runs after the facet
     * grid so floating legends sit on top of the data layers.
     */
    private void drawInsideGuides(DrawSurface gc, PlotLayout layout) {
        for (var strip : layout.strips()) {
            if (strip.position() == GuidePosition.INSIDE) {
                drawStrip(gc, strip);
            }
        }
    }

    /**
     * The side a guide renders on: its override, or the theme's default.
     */
    private GuidePosition resolvedPosition(Guide<?> guide) {
        return guide.position() != null ? guide.position() : descriptor.theme().guidePosition();
    }

    /**
     * Stamps each inlay's resolved strip side onto a copy of its guide so the
     * guide's own position agrees with where it actually renders. The direction
     * a guide draws with — and hence the geometry it reserves — depends on this
     * position ({@link Guide#effectiveDirection()}), so a vertically-flowing
     * guide on a top or bottom strip must know it sits there even when that
     * side came from the descriptor.theme()'s default rather than a per-guide
     * override. The copies leave the caller's guide objects untouched.
     */
    private List<GuideInlay<?>> resolveInlayPositions(List<GuideInlay<?>> inlays) {
        var resolved = new ArrayList<GuideInlay<?>>(inlays.size());
        for (var inlay : inlays) {
            resolved.add(resolvePosition(inlay));
        }
        return resolved;
    }

    /**
     * Captures an inlay's type so the resolved guide copy keeps its shape.
     */
    private <D extends GuideData> GuideInlay<D> resolvePosition(GuideInlay<D> inlay) {
        return new GuideInlay<>(inlay.guide().position(resolvedPosition(inlay.guide())), inlay.data(),
                inlay.labelFont(), inlay.titleFont(), inlay.metrics());
    }

    /**
     * Draws one guide strip: the optional surrounding box plus its guides.
     */
    private void drawStrip(DrawSurface gc, GuideStrip strip) {
        if (strip.inlays().isEmpty()) {
            return;
        }

        if (descriptor.theme().guideBoxColor() != null) {
            double pad = descriptor.theme().guideBoxMargin();
            gc.setFill(descriptor.theme().guideBoxColor());
            gc.fillRect(strip.x() - pad, strip.y() - pad, strip.width() + 2 * pad, strip.height() + 2 * pad);
            gc.setStroke(descriptor.theme().guideBoxBorderColor());
            gc.setLineWidth(0.5);
            gc.strokeRect(strip.x() - pad, strip.y() - pad, strip.width() + 2 * pad, strip.height() + 2 * pad);
        }

        if (strip.position() == GuidePosition.TOP || strip.position() == GuidePosition.BOTTOM) {
            double x = strip.x();
            var widths = allocateStripWidths(strip.inlays(), strip.width());
            var inlays = strip.inlays();
            for (int i = 0; i < inlays.size(); i++) {
                double w = widths.get(i);
                inlays.get(i).render(gc, x, strip.y(), w, GuideStyle.from(descriptor.theme(), inlays.get(i).guide()));
                x += w + descriptor.theme().guideSpacing();
            }
        } else {
            drawVerticalStrip(gc, strip);
        }
    }

    /**
     * Draws the descriptor.guides() of a vertical strip. Strips shrunk by the
     * overflow policy render inside a scaled slot: {@link DrawSurface#scale}
     * shrinks the geometry (and, under {@link GuideOverflow#SCALE_UNIFORM}, the
     * labels too), while {@link GuideOverflow#SCALE_GEOMETRY} inflates the
     * labels by the inverse factor so only geometry shrinks.
     */
    private void drawVerticalStrip(DrawSurface gc, GuideStrip strip) {
        boolean uniform = descriptor.theme().guideOverflow() == GuideOverflow.SCALE_UNIFORM;
        double y = strip.y();
        for (var inlay : strip.inlays()) {
            double h = inlay.measure(strip.width());
            if (strip.scale() >= 1.0) {
                inlay.render(gc, strip.x(), y, strip.width(),
                        GuideStyle.from(descriptor.theme(), inlay.guide()));
                y += h + descriptor.theme().guideSpacing();
                continue;
            }
            double s = strip.scale();
            var style = GuideStyle.from(descriptor.theme(), inlay.guide());
            gc.save();
            gc.translate(strip.x(), y);
            gc.scale(s, s);
            if (!uniform) {
                style = style.withFontScale(1.0 / s);
            }
            inlay.render(gc, 0, 0, strip.width(), style);
            gc.restore();
            y += h * s + descriptor.theme().guideSpacing();
        }
    }

    /**
     * A guide paired with the data built for the current plot. The shared type
     * parameter guarantees the guide is only ever measured or rendered against
     * the data subtype it declares. Carries the descriptor.theme()'s guide key
     * font so the guide measures its labels precisely; the font is pushed onto
     * whichever guide instance the inlay currently holds at measurement time,
     * so it stays correct even when a copy (e.g. a repositioned guide) was
     * made.
     */
    private record GuideInlay<D extends GuideData>(Guide<D> guide, D data, Font labelFont,
            Font titleFont, GuideMetrics metrics) {

        /**
         * @return a copy of this inlay carrying different data
         */
        GuideInlay<D> withData(D newData) {
            return new GuideInlay<>(guide, newData, labelFont, titleFont, metrics);
        }

        /**
         * @see Guide#preferredWidth
         */
        double preferredWidth() {
            return guide().withLabelFont(labelFont).withTitleFont(titleFont).withMetrics(metrics).preferredWidth(data());
        }

        /**
         * @see Guide#measure
         */
        double measure(double width) {
            return guide().withLabelFont(labelFont).withTitleFont(titleFont).withMetrics(metrics).measure(data(), width);
        }

        /**
         * @see Guide#render
         */
        void render(DrawSurface gc, double x, double y, double width, GuideStyle style) {
            guide().withTitleFont(titleFont).withMetrics(metrics).render(gc, data(), x, y, width, style);
        }
    }

    /**
     * The four panel margins and the strips the guides are drawn into.
     */
    private record PlotLayout(double marginLeft, double marginTop, double marginRight,
            double marginBottom, List<GuideStrip> strips) {

        static PlotLayout empty(double ml, double mt, double mr, double mb) {
            return new PlotLayout(ml, mt, mr, mb, List.of());
        }
    }

    /**
     * The reserved rectangle a guide strip occupies, per position.
     */
    private record GuideStrip(GuidePosition position, double x, double y,
            double width, double height, double scale,
            List<GuideInlay<?>> inlays) {}

    /**
     * Computes the panel margins and the guide strips for the current
     * descriptor.guides(). Each guide is placed on its own override position or
     * the descriptor.theme()'s; every occupied side gets its own strip, sized
     * from that group's measured extents, and the matching panel margin grows
     * by exactly that much, so descriptor.guides() never overlap data and plots
     * without descriptor.guides() reclaim the reserved space. INSIDE strips
     * float over the panel without claiming margins.
     */
    private PlotLayout computeLayout(double plotWidth, double plotHeight, List<GuideInlay<?>> inlays,
            double minMarginLeft, double minMarginRight) {
        var evt = new LayoutEvent();
        evt.begin();
        try {
            var layout = computeLayoutInternal(plotWidth, plotHeight, inlays, minMarginLeft, minMarginRight);
            evt.stripCount = layout.strips().size();
            return layout;
        } finally {
            evt.end();
            evt.commit();
        }
    }

    /**
     * See {@link #computeLayout(double, double, List, double, double)} for
     * fixed insets.
     */
    private PlotLayout computeLayoutInternal(double plotWidth, double plotHeight, List<GuideInlay<?>> inlays,
            double minMarginLeft, double minMarginRight) {
        if (descriptor.panelInsets() != null) {
            // A composite matrix frame overrides the label margins verbatim so
            // every cell shares an identical panel geometry (alignment across
            // rows and columns), instead of the content-fitted grown margins.
            var fixed = descriptor.panelInsets();
            return completeLayout(plotWidth, plotHeight, inlays, fixed.left(), fixed.top(), fixed.right(), fixed.bottom());
        }
        double ml = Math.max(descriptor.theme().axisLabelZone(), minMarginLeft);
        // An x-axis on the top stacks the main title, the x-axis title, and
        // the top tick labels above the panels, so the base label zone alone
        // is not enough room — for a switched grid or an explicitly
        // repositioned plain-plot x scale.
        boolean xTop = gridAxisXSwitched() || (descriptor.facet() == null && descriptor.scaleSpec().isXAxisTop());
        double mt = descriptor.theme().axisLabelZone() + (xTop ? descriptor.theme().axisTitleOffset() : 0.0);
        double mr = Math.max(descriptor.theme().axisLabelZone(), minMarginRight);
        double mb = descriptor.theme().axisLabelZone();
        return completeLayout(plotWidth, plotHeight, inlays, ml, mt, mr, mb);
    }

    /**
     * The shared second half of {@link #computeLayoutInternal}: reserve and
     * place guide strips.
     */
    private PlotLayout completeLayout(double plotWidth, double plotHeight, List<GuideInlay<?>> inlays,
            double ml, double mt, double mr, double mb) {
        if (inlays.isEmpty()) {
            return PlotLayout.empty(ml, mt, mr, mb);
        }

        double boxPad = descriptor.theme().guideBoxColor() != null ? descriptor.theme().guideBoxMargin() : 0.0;
        double gap = descriptor.theme().panelGuideGap();
        double stripPad = descriptor.theme().guideStripPadding();

        // Group the inlays by the side each guide is placed on.
        var byPosition = new EnumMap<GuidePosition, List<GuideInlay<?>>>(GuidePosition.class);
        for (var inlay : inlays) {
            byPosition.computeIfAbsent(resolvedPosition(inlay.guide()), p -> new ArrayList<>()).add(inlay);
        }

        // Overflow policy: side strips taller than the panel shrink or flow
        // into the bottom strip, so their guides never fall off the canvas.
        var sideFits = applyOverflowPolicy(byPosition, plotHeight, boxPad);

        // First pass: every occupied side claims its margin. The right side
        // keeps no label zone of its own, so its strip replaces the base
        // margin instead of growing it; the bottom strip needs no extra gap
        // since the label zone already separates it from the axis title.
        double mlF = ml;
        double mtF = mt;
        double mrF = mr;
        double mbF = mb;
        for (var entry : byPosition.entrySet()) {
            switch (entry.getKey()) {
                case RIGHT -> mrF = maxPreferredWidth(entry.getValue()) + stripPad + gap + 2 * boxPad;
                case LEFT -> mlF += leftStripInset(boxPad)
                            + maxPreferredWidth(entry.getValue()) + stripPad
                            + boxPad + stripPad;
                case TOP -> mtF += horizontalStripHeight(entry.getValue()) + gap + boxPad;
                case BOTTOM -> mbF += horizontalStripHeight(entry.getValue()) + boxPad + stripPad;
                case INSIDE -> { /* floats over the panel */ }
            }
        }
        // Snap the claimed margins to whole pixels so the panel origin stays on
        // the device-pixel grid (see the second-pass comment below).
        mlF = (double) Math.round(mlF);
        mtF = (double) Math.round(mtF);
        mrF = (double) Math.round(mrF);
        mbF = (double) Math.round(mbF);

        // Second pass: place each strip against the canvas edges, centered
        // over the final panel bounds. The margins themselves are snapped to
        // whole pixels afterwards so the panel origin stays on the device-pixel
        // grid: a fractional right margin (a legend whose width is measured in
        // a proportional font) pushes every panel primitive onto subpixels and
        // costs ~10x in the raster paths. The strip geometry keeps its own
        // fractional resolution — only the space it claims is rounded.
        var strips = new ArrayList<GuideStrip>();
        for (var entry : byPosition.entrySet()) {
            var position = entry.getKey();
            var group = entry.getValue();
            var fit = sideFits.get(position);
            double scale = fit == null ? 1.0 : fit.scale();
            switch (position) {
                case RIGHT -> {
                    // No labels sit on the right, so the margin is just the
                    // strip plus the separating gap. The strip shifts left by
                    // two box margins so a boxed legend keeps whitespace from
                    // the canvas edge instead of ending flush with it.
                    double stripWidth = maxPreferredWidth(group) + stripPad;
                    double stripHeight = fit == null ? stripHeight(group, stripWidth) : fit.height();
                    strips.add(new GuideStrip(position, plotWidth - stripWidth - 2 * boxPad,
                            descriptor.theme().axisLabelZone() + stripPad,
                            stripWidth, stripHeight, scale, group));
                }
                case LEFT -> {
                    // The y-axis labels keep their 50px zone, tightened to a
                    // 10px gap; the strip is sized to its content (no width
                    // floor) with a small inset from the canvas edge.
                    double stripWidth = maxPreferredWidth(group) + stripPad;
                    double stripHeight = fit == null ? stripHeight(group, stripWidth) : fit.height();
                    strips.add(new GuideStrip(position, leftStripInset(boxPad),
                            descriptor.theme().axisLabelZone() + stripPad,
                            stripWidth, stripHeight, scale, group));
                }
                case TOP -> {
                    // The plot title keeps its 50px zone; the strip sits below
                    // it, centered over the panel and stretched to the full
                    // panel width when it holds a continuous bar guide.
                    double avail = plotWidth - mlF - mrF;
                    double w = stretchedStripWidth(group, avail);
                    double x = Math.max(mlF, (mlF + plotWidth - mrF) / 2.0 - w / 2.0);
                    strips.add(new GuideStrip(position, x, mt, w, horizontalStripHeight(group), 1.0, group));
                }
                case BOTTOM -> {
                    // The strip claims the canvas edge below the x-axis
                    // title's 50px label zone, following the order:
                    // panel, tick labels, axis title, legend — with a small
                    // inset of breathing room from the canvas edge.
                    double h = horizontalStripHeight(group);
                    double avail = plotWidth - mlF - mrF;
                    double w = stretchedStripWidth(group, avail);
                    double x = Math.max(mlF, (mlF + plotWidth - mrF) / 2.0 - w / 2.0);
                    strips.add(new GuideStrip(position, x, plotHeight - h - stripPad, w, h, 1.0, group));
                }
                case INSIDE -> {
                    // Floats over the panel, centered on the theme's anchor.
                    double w = Math.max(descriptor.theme().guideMinInsideWidth(), maxPreferredWidth(group) + stripPad);
                    double h = stripHeight(group, w);
                    // The first guide in the strip with an explicit anchor
                    // positions the group; otherwise the theme's default.
                    var anchor = descriptor.theme().legendInsideAnchor();
                    for (var inlay : group) {
                        if (inlay.guide().insideAnchor() != null) {
                            anchor = inlay.guide().insideAnchor();
                            break;
                        }
                    }
                    double x = mlF + anchor.x() * (plotWidth - mlF - mrF) - w / 2.0;
                    double y = mtF + anchor.y() * (plotHeight - mtF - mbF) - h / 2.0;
                    strips.add(new GuideStrip(position, x, y, w, h, 1.0, group));
                }
            }
        }
        return new PlotLayout(mlF, mtF, mrF, mbF, strips);
    }

    /**
     * A side strip that the {@link GuideOverflow} policy had to shrink: its
     * scale factor and the reduced height the strip claims for that scale.
     * Sides that fit naturally have no entry.
     */
    private record SideFit(double scale, double height) {}

    /**
     * Resolves how side strips taller than the panel are handled, per the
     * descriptor.theme()'s {@link Theme#guideOverflow()} policy. {@code groups}
     * is mutated: {@link GuideOverflow#FLOW_TO_BOTTOM} and
     * {@link GuideOverflow#HYBRID} move overflow descriptor.guides() onto the
     * bottom strip (as bottom-aligned copies), while the scale modes keep them
     * on the side and record a shrink factor.
     *
     * @param groups the inlays grouped by position, mutated for relocation
     * @param plotHeight the canvas height in pixels
     * @param boxPad the boxed descriptor.guides()' extra padding, or 0
     * @return the scale and height for each side strip the policy shrank
     */
    private Map<GuidePosition, SideFit> applyOverflowPolicy(
            Map<GuidePosition, List<GuideInlay<?>>> groups, double plotHeight, double boxPad) {
        var sideFits = new EnumMap<GuidePosition, SideFit>(GuidePosition.class);
        for (var position : List.of(GuidePosition.RIGHT, GuidePosition.LEFT)) {
            var group = groups.get(position);
            if (group == null || group.isEmpty()) {
                continue;
            }
            double stripWidth = maxPreferredWidth(group) + descriptor.theme().guideStripPadding();
            double natural = stripHeight(group, stripWidth);
            double available = plotHeight - bottomBase(groups, boxPad)
                    - descriptor.theme().axisLabelZone() - descriptor.theme().guideStripPadding();
            if (natural <= available) {
                continue;
            }
            switch (descriptor.theme().guideOverflow()) {
                case NONE -> {
                    /* keep the natural size; the canvas clips the overflow */ }
                case FLOW_TO_BOTTOM -> {
                    groups.computeIfAbsent(GuidePosition.BOTTOM, p -> new ArrayList<>())
                            .addAll(relocatedToBottom(group));
                    groups.remove(position);
                }
                case HYBRID -> {
                    var kept = new ArrayList<GuideInlay<?>>();
                    var moving = new ArrayList<GuideInlay<?>>();
                    double used = 0.0;
                    for (var inlay : group) {
                        double h = inlay.measure(stripWidth);
                        double addition = kept.isEmpty() ? h : h + descriptor.theme().guideSpacing();
                        if (used + addition <= available) {
                            kept.add(inlay);
                            used += addition;
                        } else {
                            moving.add(inlay);
                        }
                    }
                    if (kept.isEmpty()) {
                        groups.remove(position);
                    } else {
                        groups.put(position, kept);
                    }
                    groups.computeIfAbsent(GuidePosition.BOTTOM, p -> new ArrayList<>())
                            .addAll(relocatedToBottom(moving));
                }
                case SCALE_GEOMETRY, SCALE_UNIFORM -> {
                    double spacingSum = descriptor.theme().guideSpacing() * (group.size() - 1);
                    double scale = Math.clamp((available - spacingSum) / (natural - spacingSum), 0.05, 1.0);
                    sideFits.put(position, new SideFit(scale, available));
                }
            }
        }
        return sideFits;
    }

    /**
     * The top of the bottom label zone plus the bottom strip, for side space.
     */
    private double bottomBase(Map<GuidePosition, List<GuideInlay<?>>> groups, double boxPad) {
        var bottom = groups.get(GuidePosition.BOTTOM);
        return descriptor.theme().axisLabelZone() + (bottom == null || bottom.isEmpty()
                ? 0.0
                : horizontalStripHeight(bottom) + boxPad + descriptor.theme().guideStripPadding());
    }

    /**
     * Bottom-positioned copies of the given inlays, flipping them horizontal.
     */
    private List<GuideInlay<?>> relocatedToBottom(List<GuideInlay<?>> inlays) {
        var result = new ArrayList<GuideInlay<?>>();
        for (var inlay : inlays) {
            result.add(relocateToBottom(inlay));
        }
        return result;
    }

    /**
     * A copy of the inlay whose guide is forced onto the bottom strip.
     */
    private <D extends GuideData> GuideInlay<D> relocateToBottom(GuideInlay<D> inlay) {
        return new GuideInlay<>(inlay.guide().position(GuidePosition.BOTTOM), inlay.data(),
                inlay.labelFont(), inlay.titleFont(), inlay.metrics());
    }

    /**
     * The width of the widest guide in a stack, in its natural size.
     */
    private double maxPreferredWidth(List<GuideInlay<?>> inlays) {
        return inlays.stream()
                .mapToDouble(GuideInlay::preferredWidth)
                .max()
                .orElse(0.0);
    }

    /**
     * The total height of vertically stacked guides at the given slot width.
     */
    private double stripHeight(List<GuideInlay<?>> inlays, double width) {
        return inlays.stream()
                .mapToDouble(inlay -> inlay.measure(width))
                .sum()
                + descriptor.theme().guideSpacing() * (inlays.size() - 1);
    }

    /**
     * The total width of horizontally flowing guides.
     */
    private double stripWidth(List<GuideInlay<?>> inlays) {
        return inlays.stream()
                .mapToDouble(GuideInlay::preferredWidth)
                .sum()
                + descriptor.theme().guideSpacing() * (inlays.size() - 1);
    }

    /**
     * The width a top/bottom strip claims: its natural width, stretched to the
     * full available horizontal span when it holds a continuous bar guide and
     * there is room to spare, so those bars use the whole strip.
     */
    private double stretchedStripWidth(List<GuideInlay<?>> inlays, double availableWidth) {
        double natural = stripWidth(inlays);
        boolean hasStretchy = inlays.stream().anyMatch(i -> i.guide().stretchesToFillStrip());
        return hasStretchy && availableWidth > natural ? availableWidth : natural;
    }

    /**
     * The per-inlay slot widths for a top/bottom strip of the given total
     * width. Discrete legends hold their natural width while continuous bar
     * descriptor.guides() share any leftover space equally among themselves.
     */
    private List<Double> allocateStripWidths(List<GuideInlay<?>> inlays, double stripWidth) {
        int n = inlays.size();
        var widths = new ArrayList<Double>(n);
        double naturalSum = 0.0;
        int stretchyCount = 0;
        for (var inlay : inlays) {
            double w = inlay.preferredWidth();
            widths.add(w);
            naturalSum += w;
            if (inlay.guide().stretchesToFillStrip()) {
                stretchyCount++;
            }
        }
        double available = stripWidth - descriptor.theme().guideSpacing() * (n - 1);
        if (stretchyCount > 0 && available > naturalSum) {
            double extra = (available - naturalSum) / stretchyCount;
            for (int i = 0; i < n; i++) {
                if (inlays.get(i).guide().stretchesToFillStrip()) {
                    widths.set(i, widths.get(i) + extra);
                }
            }
        }
        return widths;
    }

    /**
     * The height of the tallest guide flowing horizontally.
     */
    private double horizontalStripHeight(List<GuideInlay<?>> inlays) {
        return inlays.stream()
                .mapToDouble(inlay -> inlay.measure(inlay.preferredWidth()))
                .max()
                .orElse(0.0);
    }

    /**
     * The guide spacing metrics resolved from the active theme.
     */
    private GuideMetrics guideMetrics() {
        return new GuideMetrics(descriptor.theme().guideTitleHeight(), descriptor.theme().guideKeyTitleHeight(),
                descriptor.theme().guideSizedKeyPad(), descriptor.theme().guideTickLength(),
                descriptor.theme().guideBarRowGap(), descriptor.theme().guideBarTickSpace(),
                descriptor.theme().guideBarTickSpaceHorizontal(), descriptor.theme().guideBarLabelPad(),
                descriptor.theme().guideTitleTickGap(), descriptor.theme().guideBarGap(), descriptor.theme().guideContentPad(),
                descriptor.theme().guideMinNaturalWidth(), descriptor.theme().legendMaxTitleLineWidth(),
                descriptor.theme().legendTitleTextPad(), descriptor.theme().legendTitleSidePad(),
                descriptor.theme().legendTitleOffsetY());
    }

    /**
     * Collects the descriptor.guides() this plot should render: a guide for
     * each mapped colour/fill aesthetic column, plus the continuous
     * reference-line descriptor.guides() carried by the layers (a filled
     * density2d, or a data-driven
     * {@code hline()}/{@code vline()}/{@code abline()} mapping its colour to a
     * column). All descriptor.guides() render stacked in the guide strip,
     * ordered by {@link Guide#order()}.
     */
    private List<GuideInlay<?>> collectGuideInlays(Guides guides) {
        var evt = new GuideCollectionEvent();
        evt.begin();
        try {
            var inlays = collectGuideInlaysInternal(guides);
            evt.guideCount = inlays.size();
            return inlays;
        } finally {
            evt.end();
            evt.commit();
        }
    }

    private List<GuideInlay<?>> collectGuideInlaysInternal(Guides guides) {
        var inlays = new ArrayList<GuideInlay<?>>();
        var seenColumns = new HashSet<String>();
        // Columns whose legend renders as discrete keys; only those fold the
        // linetype dashes into their key glyphs, so only they suppress the
        // standalone linetype legend. A continuous colour bar carries no dashes
        // and must not hide the linetype guide.
        var discreteLegendColumns = new HashSet<String>();

        for (var aesthetic : new Aesthetic[]{Aesthetic.COLOR, Aesthetic.FILL}) {
            var value = aesthetic == Aesthetic.COLOR ? descriptor.aes().colorValue() : descriptor.aes().fillValue();
            var column = value == null ? null : value.column();
            if (column == null || !seenColumns.add(column)) {
                continue;
            }
            collectColourGuide(guides, aesthetic, column, inlays, discreteLegendColumns);
        }

        // A colour/fill mapping living on a layer rather than the plot defines
        // a scale just the same; collect its guide so e.g. an afterStat(...)
        // fill shows the colourbar the default draws for it. Data-less function
        // layers are skipped: their constant colour LABELS draw the synthetic
        // legend handled at the end of this method.
        for (var layer : descriptor.geoms()) {
            if (layer instanceof GeomFunction) {
                continue;
            }
            var layerAes = effectiveAes(layer);
            for (var aesthetic : new Aesthetic[]{Aesthetic.COLOR, Aesthetic.FILL}) {
                var value = aesthetic == Aesthetic.COLOR ? layerAes.colorValue() : layerAes.fillValue();
                var column = value == null ? null : value.column();
                if (column == null || !seenColumns.add(column)) {
                    continue;
                }
                collectColourGuide(guides, aesthetic, column, inlays, discreteLegendColumns);
            }
        }

        // A filled density2d layer maps its bands to a continuous statistic, so
        // it always carries a "density" colourbar guide.
        for (var layer : descriptor.geoms()) {
            if (layer instanceof GeomDensity2d<?> d2d && d2d.isFilled()) {
                @SuppressWarnings("unchecked")
                var max = ((GeomDensity2d<DF>) d2d).maxStatistic(descriptor.data(), descriptor.extractor(), descriptor.aes());
                if (Double.isFinite(max) && max > 0) {
                    var scale = ContinuousColorScale.forRange("density", List.of(0.0, max), detectCmap());
                    var guide = resolveColorbarGuide(guides, Aesthetic.FILL, "density", scale);
                    if (guide != null) {
                        inlays.add(new GuideInlay<>(guide, buildColorbarData(scale), descriptor.theme().guideKeyFont(), descriptor.theme().guideTitleFont(), guideMetrics()));
                    }
                }
            }
        }

        // A data-driven reference line mapping its colour to a column draws a
        // continuous colourbar over that column's range.
        for (var layer : descriptor.geoms()) {
            String colorColumn = null;
            Object data = null;
            if (layer instanceof GeomHline<?> hl) {
                colorColumn = hl.colorColumn();
                data = hl.data();
            } else if (layer instanceof GeomVline<?> vl) {
                colorColumn = vl.colorColumn();
                data = vl.data();
            } else if (layer instanceof GeomAbline<?> al) {
                colorColumn = al.colorColumn();
                data = al.data();
            }
            if (colorColumn != null && data != null && seenColumns.add(colorColumn)) {
                @SuppressWarnings("unchecked")
                var values = descriptor.extractor().getColumn((DF) data, colorColumn);
                var scale = ContinuousColorScale.forRange(colorColumn, values, detectCmap());
                if (scale != null) {
                    var guide = resolveColorbarGuide(guides, Aesthetic.COLOR, colorColumn, scale);
                    if (guide != null) {
                        inlays.add(new GuideInlay<>(guide, buildColorbarData(scale), descriptor.theme().guideKeyFont(), descriptor.theme().guideTitleFont(), guideMetrics()));
                    }
                }
            }
        }

        // A shape mapping whose column no other legend already carries (or is
        // merged into) draws its own shape legend: one neutral key per
        // distinct symbol of the shape column. When an already-collected
        // discrete legend has the identical label sequence — the default merges
        // guides whose keys match — the shapes fold into that legend's keys
        // instead of producing a second legend.
        var shapeColumn = descriptor.aes().shape();
        if (shapeColumn != null && seenColumns.add(shapeColumn)) {
            var categories = Scale.uniqueCategories(descriptor.data(), descriptor.extractor(), shapeColumn);
            var keys = categories.stream().map(category -> {
                var shape = scales().shapeScale(shapeColumn).shapeFor(category);
                // Numeric categories read as ticks ("8"), not doubles ("8.0").
                var rawLabel = category instanceof Number number
                        ? Scale.formatTick(number.doubleValue())
                        : category.toString();
                return new GuideLegend.Key(category, descriptor.labs().map(rawLabel),
                        descriptor.theme().neutralKeyColor(), shape);
            }).toList();
            var guide = resolveLegendGuide(guides, Aesthetic.SHAPE, shapeColumn);
            if (guide != null && !keys.isEmpty() && !mergeShapesIntoMatchingLegend(inlays, keys)) {
                inlays.add(new GuideInlay<>(guide,
                        new GuideLegend.Data(shapeColumn, keys, false, false, null, true, false),
                        descriptor.theme().guideKeyFont(), descriptor.theme().guideTitleFont(), guideMetrics()));
            }
        }

        // A size mapping whose column no other legend already carries draws a
        // size legend: one neutral key per tick of the value range, its dot
        // sized by the same area-proportional mapping the points use.
        var sizeColumn = descriptor.aes().size();
        if (sizeColumn != null && seenColumns.add(sizeColumn)) {
            var range = SizeScale.range(descriptor.data(), descriptor.extractor(), sizeColumn);
            if (range != null) {
                var ticks = new Scale(
                        range.min(), range.max(), 0, 100, descriptor.scaleSpec().getYTransform())
                        .calculateTicks(4, null);
                var keys = ticks.stream()
                        .map(tick -> new GuideLegend.Key(tick, descriptor.labs().map(Scale.formatTick(tick)),
                                descriptor.theme().neutralKeyColor(), PointShape.CIRCLE,
                                SizeScale.radiusFor(tick, range)))
                        .toList();
                var guide = resolveLegendGuide(guides, Aesthetic.SIZE, sizeColumn);
                if (guide != null && !keys.isEmpty()) {
                    inlays.add(new GuideInlay<>(guide,
                            new GuideLegend.Data(sizeColumn, keys, false, false, null, true, false, true),
                            descriptor.theme().guideKeyFont(), descriptor.theme().guideTitleFont(), guideMetrics()));
                }
            }
        }

        // An alpha mapping draws a transparency gradient bar: one bar
        // per colour class when colour is also mapped, each tinted in
        // that class's fill colour; a single red-based bar otherwise.
        var alphaColumn = descriptor.aes().alpha();
        if (alphaColumn != null && seenColumns.add(alphaColumn)) {
            var alphaRange = SizeScale.range(descriptor.data(), descriptor.extractor(), alphaColumn);
            if (alphaRange != null) {
                var alphaBreaks = new Scale(
                        alphaRange.min(), alphaRange.max(), 0, 100, descriptor.scaleSpec().getYTransform())
                        .calculateTicks(4, null);
                var alphaLabels = alphaBreaks.stream().map(Scale::formatTick).toList();
                var colorCol = descriptor.aes().color();
                var fillCol = descriptor.aes().fill();
                var legendCol = colorCol != null ? colorCol : fillCol;
                var colorScale = legendCol != null ? scales().colorScale(legendCol) : null;
                var entries = colorScale != null
                        ? colorScale.categories().stream()
                                .map(cat -> new GuideAlpha.AlphaEntry(colorScale.colorFor(cat), alphaRange))
                                .toList()
                        : List.of(new GuideAlpha.AlphaEntry(descriptor.theme().fallbackColor(), alphaRange));
                var guide = resolveAlphaGuide(guides, Aesthetic.ALPHA, alphaColumn);
                if (guide != null) {
                    inlays.add(new GuideInlay<>(guide,
                            new GuideAlpha.Data(alphaColumn, entries, alphaBreaks, alphaLabels),
                            descriptor.theme().guideKeyFont(), descriptor.theme().guideTitleFont(), guideMetrics()));
                }
            }
        }

        // Data-less function layers carry constant colour labels rather than a
        // data column, so they draw their own synthetic discrete legend: one
        // key per label, in the same palette order the curves were assigned.
        var functionLabels = functionColorLabels();
        if (!functionLabels.isEmpty() && descriptor.aes().color() == null && descriptor.aes().fill() == null) {
            var pseudoColumn = "color:" + String.join("/", functionLabels);
            var scale = DiscreteColorScale.forLabels(pseudoColumn, functionLabels, descriptor.labs(), descriptor.theme().categoricalPalette(), descriptor.theme().fallbackColor());
            var guide = resolveLegendGuide(guides, Aesthetic.COLOR, pseudoColumn, scale);
            if (guide != null) {
                inlays.add(new GuideInlay<>(guide, buildLegendData(scale), descriptor.theme().guideKeyFont(), descriptor.theme().guideTitleFont(), guideMetrics()));
            }
        }

        // A linetype mapping draws its own linetype legend: one neutral dashed key per
        // distinct line-type category. Only a discrete legend on the same column
        // (whose keys already fold these dashes in) suppresses the duplicate; a
        // continuous colour bar on the same column carries no dash glyphs, so the
        // linetype guide still renders.
        var linetypeColumn = descriptor.aes().linetype();
        if (linetypeColumn != null && !discreteLegendColumns.contains(linetypeColumn)) {
            var categories = Scale.uniqueCategories(descriptor.data(), descriptor.extractor(), linetypeColumn);
            var linScale = scales().linetypeScale(linetypeColumn);
            var keys = categories.stream().map(category -> {
                var rawLabel = category instanceof Number number
                        ? Scale.formatTick(number.doubleValue())
                        : category.toString();
                List<Double> dashes = null;
                var arr = linScale.patternFor(category);
                if (arr != null) {
                    var l = new ArrayList<Double>(arr.length);
                    for (double d : arr) {
                        l.add(d);
                    }
                    dashes = List.copyOf(l);
                }
                return new GuideLegend.Key(category, descriptor.labs().map(rawLabel), NEUTRAL_KEY_COLOR, null, null, dashes);
            }).toList();
            var guide = resolveLegendGuide(guides, Aesthetic.LINETYPE, linetypeColumn);
            if (guide != null && !keys.isEmpty()) {
                inlays.add(new GuideInlay<>(guide,
                        new GuideLegend.Data(linetypeColumn, keys, false, false, null, false, true),
                        descriptor.theme().guideKeyFont(), descriptor.theme().guideTitleFont(), guideMetrics()));
            }
        }

        inlays.sort(Comparator.comparingInt(inlay -> inlay.guide().order()));
        return inlays;
    }

    /**
     * Collects the guide (colourbar or discrete legend) for a colour/fill
     * column: continuous numeric or date columns draw a colourbar (binned
     * {@code colorsteps} when a stepped colour scale was set), everything
     * else a discrete legend. Used for both plot-level and layer-level
     * mappings.
     *
     * @param guides the guide registry
     * @param aesthetic the mapped aesthetic (colour or fill)
     * @param column the scale's column key
     * @param inlays the inlay collector
     * @param discreteLegendColumns columns whose legend renders as discrete
     * keys
     */
    private void collectColourGuide(Guides guides, Aesthetic aesthetic, String column,
            List<GuideInlay<?>> inlays, Set<String> discreteLegendColumns) {
        var scale = buildColorScale(column);
        if (scale == null) {
            return;
        }
        if (scale.isContinuous()) {
            var continuous = (ContinuousColorScale) scale;
            if (descriptor.scaleSpec().isBinned()) {
                var guide = resolveColorstepsGuide(guides, aesthetic, column, continuous);
                if (guide != null) {
                    inlays.add(new GuideInlay<>(guide, buildColorstepsData(continuous, descriptor.scaleSpec().getColorSteps()),
                            descriptor.theme().guideKeyFont(), descriptor.theme().guideTitleFont(), guideMetrics()));
                }
            } else {
                var guide = resolveColorbarGuide(guides, aesthetic, column, continuous);
                if (guide != null) {
                    inlays.add(new GuideInlay<>(guide, buildColorbarData(continuous),
                            descriptor.theme().guideKeyFont(), descriptor.theme().guideTitleFont(), guideMetrics()));
                }
            }
        } else {
            var discrete = (DiscreteColorScale) scale;
            var guide = resolveLegendGuide(guides, aesthetic, column, discrete);
            if (guide != null) {
                discreteLegendColumns.add(column);
                inlays.add(new GuideInlay<>(guide, buildLegendData(discrete),
                        descriptor.theme().guideKeyFont(), descriptor.theme().guideTitleFont(), guideMetrics()));
            }
        }
    }

    /**
     * Folds shape glyphs into a previously collected discrete legend whose
     * label sequence is identical to the shape keys' — the rule for
     * merging descriptor.guides(). The first matching legend wins; its keys are
     * rebuilt carrying both the original encoding and the shape, and its point
     * flag is forced so the glyphs render.
     *
     * @return {@code true} when a merge happened and the standalone shape
     * legend is no longer needed
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private boolean mergeShapesIntoMatchingLegend(List<GuideInlay<?>> inlays,
            List<GuideLegend.Key> shapeKeys) {
        for (int idx = 0; idx < inlays.size(); idx++) {
            var inlay = inlays.get(idx);
            if (!(inlay.data() instanceof GuideLegend.Data d)) {
                continue;
            }
            var mergedKeys = foldShapes(d.keys(), shapeKeys);
            if (mergedKeys == null) {
                continue;
            }
            // The checked instanceof above guarantees the inlay carries legend
            // data, so the raw pairing below is safe.
            var legendInlay = (GuideInlay) inlay;
            inlays.set(idx, legendInlay.withData(new GuideLegend.Data(
                    d.columnName(), mergedKeys, d.smooth(), d.drawBand(), d.bandColor(), true, d.lines())));
            return true;
        }
        return false;
    }

    /**
     * Folds shape glyphs into legend keys when both lists share the identical
     * label sequence — {@code null} when they don't match.
     */
    static List<GuideLegend.Key> foldShapes(List<GuideLegend.Key> keys, List<GuideLegend.Key> shapeKeys) {
        if (keys.size() != shapeKeys.size() || !labelsOf(keys).equals(labelsOf(shapeKeys))) {
            return null;
        }
        var merged = new ArrayList<GuideLegend.Key>();
        for (int i = 0; i < keys.size(); i++) {
            var key = keys.get(i);
            var shape = shapeKeys.get(i).shape();
            merged.add(new GuideLegend.Key(key.value(), key.label(), key.color(),
                    shape != null ? shape : key.shape(), key.radius()));
        }
        return merged;
    }

    /**
     * The ordered display labels of a key list, the merge identity.
     */
    private static List<String> labelsOf(List<GuideLegend.Key> keys) {
        return keys.stream().map(GuideLegend.Key::label).toList();
    }

    /**
     * Resolves the colorbar guide for a continuous scale: an explicit override
     * from the {@link Guides} registry, otherwise the default colorbar, with
     * the guide title filled in from {@code labs().legendTitle(...)} or the
     * given fallback. Returns {@code null} when the aesthetic's guide is
     * suppressed.
     */
    private GuideColorbar resolveColorbarGuide(Guides guides, Aesthetic aesthetic, String titleFallback,
            ContinuousColorScale scale) {
        var guide = resolveGuide(guides, aesthetic, titleFallback, scale.isContinuous());
        if (guide != null && guides.isConfigured(aesthetic)) {
            // An explicitly configured guide must accept continuous values,
            // so after this check a non-null result is necessarily a colorbar.
            Guides.validate(guide, scale, aesthetic);
        }
        return guide == null ? null : (GuideColorbar) guide;
    }

    /**
     * Resolves the colorsteps guide for a binned continuous scale: an explicit
     * override from the {@link Guides} registry, otherwise the default
     * colorsteps guide, with the title filled in. Returns {@code null} when the
     * aesthetic's guide is suppressed.
     */
    private GuideColorsteps resolveColorstepsGuide(Guides guides, Aesthetic aesthetic, String titleFallback,
            ContinuousColorScale scale) {
        if (guides.isSuppressed(aesthetic)) {
            return null;
        }
        var guide = guides.isConfigured(aesthetic)
                ? guides.forAesthetic(aesthetic)
                : Guides.guideColorsteps();
        if (guide.title() == null) {
            var perAesthetic = descriptor.labs().legendTitle(aesthetic);
            if (perAesthetic != null) {
                guide = guide.title(perAesthetic);
            } else {
                var title = descriptor.labs().legendTitleIsEmpty() ? titleFallback : descriptor.labs().legendTitle();
                guide = guide.title(title);
            }
        }
        Guides.validate(guide, scale, aesthetic);
        return (GuideColorsteps) guide;
    }

    private GuideLegend resolveLegendGuide(Guides guides, Aesthetic aesthetic, String titleFallback,
            DiscreteColorScale scale) {
        var guide = resolveGuide(guides, aesthetic, titleFallback, scale.isContinuous());
        if (guide != null && guides.isConfigured(aesthetic)) {
            // An explicitly configured guide must accept discrete values, so
            // after this check a non-null result is necessarily a legend.
            Guides.validate(guide, scale, aesthetic);
        }
        return guide == null ? null : (GuideLegend) guide;
    }

    /**
     * Resolves the legend guide for an aesthetic without a colour scale, such
     * as the shape legend. Same resolution rules as the scale-bound variant.
     * Returns {@code null} when the aesthetic's guide is suppressed.
     */
    private GuideLegend resolveLegendGuide(Guides guides, Aesthetic aesthetic, String titleFallback) {
        var guide = resolveGuide(guides, aesthetic, titleFallback, false);
        if (guide instanceof GuideColorbar) {
            throw new IllegalArgumentException(
                    "A colorbar guide requires a mapped colour or fill value, but the '" + aesthetic.key()
                    + "' aesthetic maps shapes");
        }
        // The shape legend is inherently discrete, so any non-null result
        // here is necessarily a legend.
        return guide == null ? null : (GuideLegend) guide;
    }

    /**
     * Resolves the alpha guide: an explicit override from the {@link Guides}
     * registry, otherwise the default alpha guide, with the guide title filled
     * in from labs. Returns {@code null} when suppressed.
     */
    private GuideAlpha resolveAlphaGuide(Guides guides, Aesthetic aesthetic, String titleFallback) {
        if (guides.isSuppressed(aesthetic)) {
            return null;
        }
        var guide = guides.isConfigured(aesthetic)
                ? guides.forAesthetic(aesthetic)
                : Guides.guideAlpha();
        if (guide.title() == null) {
            var perAesthetic = descriptor.labs().legendTitle(aesthetic);
            if (perAesthetic != null) {
                guide = guide.title(perAesthetic);
            } else {
                var title = descriptor.labs().legendTitleIsEmpty() ? titleFallback : descriptor.labs().legendTitle();
                guide = guide.title(title);
            }
        }
        return (GuideAlpha) guide;
    }

    /**
     * Resolves the raw guide for an aesthetic: an explicit override from the
     * {@link Guides} registry, otherwise the value kind's default type. The
     * guide title is filled in from the first of: an explicit guide title, a
     * per-aesthetic {@code labs().legendTitle(aesthetic, ...)}, the global
     * {@code labs().legendTitle(...)}, or the given fallback (the mapped column
     * name). Callers validate explicitly configured descriptor.guides() against
     * their value kind. Returns {@code null} when the aesthetic's guide is
     * suppressed.
     */
    private Guide<?> resolveGuide(Guides guides, Aesthetic aesthetic, String titleFallback, boolean continuous) {
        if (guides.isSuppressed(aesthetic)) {
            return null;
        }
        var guide = guides.isConfigured(aesthetic)
                ? guides.forAesthetic(aesthetic)
                : Guides.defaultFor(continuous);
        if (guide.title() != null) {
            return guide;
        }
        var perAesthetic = descriptor.labs().legendTitle(aesthetic);
        if (perAesthetic != null) {
            return guide.title(perAesthetic);
        }
        var title = descriptor.labs().legendTitleIsEmpty() ? titleFallback : descriptor.labs().legendTitle();
        return guide.title(title);
    }

    /**
     * Builds the colour scale for a column: a continuous scale for numeric and
     * date columns, a discrete scale for categorical columns. An explicit
     * manual palette is always categorical, one key per colour.
     */
    private ColorScale buildColorScale(String columnName) {
        // A stat-computed column lives only in the stat output, so its scale
        // is built from those values rather than the raw frame.
        if (scales().isStatColumn(columnName)) {
            if (scales().isContinuousColor(columnName)) {
                return scales().continuousColorScale(columnName);
            }
            return scales().colorScale(columnName);
        }
        if (descriptor.scaleSpec().getManualColors() != null || descriptor.scaleSpec().isDiscreteColorRamp()) {
            return scales().colorScale(columnName);
        }
        var type = descriptor.extractor().columnType(descriptor.data(), columnName);
        if (type == DataExtractor.ColumnType.NUMBER || type == DataExtractor.ColumnType.DATE) {
            return continuousColorScale(columnName, descriptor.extractor().getColumn(descriptor.data(), columnName));
        }
        return scales().colorScale(columnName);
    }

    /**
     * Builds a continuous colour scale for a column, honouring a plot-level
     * gradient ({@code scaleColorGradient}) or named colour ramp
     * ({@code scaleColorViridisC}, {@code scaleColorBrewer}) before falling
     * back to the per-layer colour ramp.
     *
     * @return the continuous colour scale, or {@code null} when no numeric
     * range
     */
    private ContinuousColorScale continuousColorScale(String columnName, List<?> values) {
        var gradient = descriptor.scaleSpec().getContinuousGradient();
        if (gradient != null && !gradient.isEmpty()) {
            return ContinuousColorScale.fromColors(columnName, values, gradient);
        }
        return ContinuousColorScale.forRange(columnName, values, detectCmap());
    }

    /**
     * Builds the discrete legend data: one key per category from the discrete
     * colour scale with its resolved colour and optional point shape, plus the
     * layer-driven key rendering flags (SE band, midline, point).
     */
    private GuideLegend.Data buildLegendData(DiscreteColorScale scale) {
        var sizeColumn = descriptor.aes().size();
        boolean hasPoints = false;
        boolean hasSmooth = false;
        Color smoothCustomBandColor = null;
        boolean smoothHasSe = true;
        boolean hasLines = false;
        for (var layer : descriptor.geoms()) {
            if (layer instanceof GeomPoint<?>) {
                hasPoints = true;
            }
            if (layer instanceof GeomSmooth<?> smoothLayer) {
                hasSmooth = true;
                smoothCustomBandColor = smoothLayer.getCustomBandColor();
                smoothHasSe = smoothLayer.isSe();
            }
            if (layer instanceof GeomLine<?> || layer instanceof GeomFunction<?>) {
                hasLines = true;
            }
        }

        var categories = scale.categories();
        var labels = scale.labels();
        // When the size aesthetic maps the same column, the legend's keys
        // carry the size encoding too: each key's dot is drawn with the
        // radius its category's average value produces.
        var sizeRange = sizeColumn != null && sizeColumn.equals(scale.columnName())
                ? SizeScale.range(descriptor.data(), descriptor.extractor(), sizeColumn)
                : null;
        var keys = new ArrayList<GuideLegend.Key>();
        for (int i = 0; i < categories.size(); i++) {
            var cat = categories.get(i);
            var color = scale.colorFor(cat);
            var shape = descriptor.aes().shape() != null
                    ? scales().shapeScale(descriptor.aes().shape()).shapeFor(cat)
                    : null;
            // When the line-type aesthetic maps the same column, each key's
            // line is drawn in the category's dash pattern (the linetype fold).
            List<Double> dashes = null;
            if (descriptor.aes().linetype() != null && descriptor.aes().linetype().equals(scale.columnName())) {
                var arr = scales().linetypeScale(descriptor.aes().linetype()).patternFor(cat);
                if (arr != null) {
                    var l = new ArrayList<Double>(arr.length);
                    for (double d : arr) {
                        l.add(d);
                    }
                    dashes = List.copyOf(l);
                }
            }
            Double radius = null;
            if (sizeRange != null) {
                var value = Scale.resolveGlobalSizeValue(descriptor.data(), descriptor.extractor(), sizeColumn, cat);
                radius = value == null ? null : SizeScale.radiusFor(value, sizeRange);
            }
            keys.add(new GuideLegend.Key(cat, labels.get(i), color, shape, radius, dashes));
        }
        return new GuideLegend.Data(scale.columnName(), keys, hasSmooth,
                hasSmooth && smoothHasSe, smoothCustomBandColor, hasPoints, hasLines);
    }

    /**
     * Wraps a continuous colour scale as colourbar data: its domain, breaks,
     * and labels feed the colourbar's tick marks.
     */
    private GuideColorbar.Data buildColorbarData(ContinuousColorScale scale) {
        return new GuideColorbar.Data(scale.columnName(), scale.domain(),
                scale.breaks(), scale.labels(), scale);
    }

    /**
     * Builds the per-plot data a colorsteps guide renders for a binned scale.
     */
    private GuideColorsteps.Data buildColorstepsData(ContinuousColorScale scale, Integer steps) {
        return new GuideColorsteps.Data(scale.columnName(), scale.domain(),
                scale.breaks(), scale.labels(), scale, steps == null ? 10 : steps);
    }

    /**
     * Detects the continuous colour ramp of the plot, preferring an explicit
     * plot-level ramp before any tile or filled density2d layer's own ramp.
     */
    private String detectCmap() {
        var ramp = descriptor.scaleSpec().getContinuousColorMap();
        if (ramp != null) {
            return ramp;
        }
        for (var layer : descriptor.geoms()) {
            if (layer instanceof GeomDensity2d<?> d2d && d2d.isFilled()) {
                return d2d.getCmapName();
            }
            if (layer instanceof GeomTile<?> tile) {
                return tile.getCmapName();
            }
        }
        return descriptor.theme().defaultContinuousRamp();
    }

    /**
     * Layer 4: The mathematical data grid (panels & facet drawing).
     */
    private void drawFacetsGrid(DrawSurface gc, Map<Object, DF> partitions, int numPanels, double plotWidth, double plotHeight, PlotLayout layout) {
        double marginOffsetTop = layout.marginTop();
        double marginOffsetBottom = layout.marginBottom();
        double marginOffsetLeft = layout.marginLeft();
        double marginOffsetRight = layout.marginRight();

        panelScales.clear();

        // Calculate available space for the charts in pixel space
        double availableWidth = plotWidth - marginOffsetLeft - marginOffsetRight;
        double availableHeight = plotHeight - marginOffsetTop - marginOffsetBottom;

        int numCols = (descriptor.facet() != null && descriptor.facet().getCols() > 0) ? descriptor.facet().getCols() : 3;
        if (numCols > numPanels) {
            numCols = numPanels;
        }
        int numRows = (int) Math.ceil((double) numPanels / numCols);

        var hGap = descriptor.theme().facetHGap();
        var vGap = descriptor.theme().facetVGap();
        double panelWidth = Math.floor((availableWidth - (numCols - 1) * hGap) / numCols);

        var xCol = positionColumn(descriptor.aes().xValue());
        var yCol = positionColumn(descriptor.aes().yValue());
        var xTrain = xCol != null ? xCol : positionTrainColumn(descriptor.aes(), true);
        var yTrain = yCol != null ? yCol : positionTrainColumn(descriptor.aes(), false);
        boolean xIsDiscrete = xTrain != null && descriptor.extractor().columnType(descriptor.data(), xTrain) == DataExtractor.ColumnType.TEXT;
        boolean yIsDiscrete = yTrain != null && descriptor.extractor().columnType(descriptor.data(), yTrain) == DataExtractor.ColumnType.TEXT;
        boolean xIsDate = xTrain != null && descriptor.extractor().columnType(descriptor.data(), xTrain) == DataExtractor.ColumnType.DATE;
        boolean yIsDate = yTrain != null && descriptor.extractor().columnType(descriptor.data(), yTrain) == DataExtractor.ColumnType.DATE;

        List<Object> xCategories = null;
        List<Object> yCategories = null;
        if (xIsDiscrete) {
            xCategories = Scale.uniqueCategories(descriptor.data(), descriptor.extractor(), xTrain);
        }
        if (yIsDiscrete) {
            yCategories = Scale.uniqueCategories(descriptor.data(), descriptor.extractor(), yTrain);
        }

        // Layers that sit flush with the panel edges (heatmap tiles) opt out of
        // the default scale expansion, so the folded domain is not widened.
        boolean applyDefaultExpansion = descriptor.geoms().stream().anyMatch(Layer::wantsDefaultExpansion);

        var xAxisTransform = axisTransform(descriptor.coord(), descriptor.scaleSpec().getXTransform(), xIsDiscrete, "x");
        var yAxisTransform = axisTransform(descriptor.coord(), descriptor.scaleSpec().getYTransform(), yIsDiscrete, "y");
        var xBounds = xIsDiscrete ? new MinMax(-0.5, xCategories.size() - 0.5)
                : (xTrain != null ? (applyDefaultExpansion
                                ? expandBounds(positionBounds(descriptor.extractor(), descriptor.data(), descriptor.aes(), true, xTrain,
                                        descriptor.aes().xValue() instanceof AesValue.AfterScale ? yAxisTransform : null),
                                        descriptor.scaleSpec().getXExpand(), xAxisTransform)
                                : positionBounds(descriptor.extractor(), descriptor.data(), descriptor.aes(), true, xTrain,
                                        descriptor.aes().xValue() instanceof AesValue.AfterScale ? yAxisTransform : null))
                        : MinMax.UNIT);
        var yBounds = yIsDiscrete ? new MinMax(-0.5, yCategories.size() - 0.5)
                : (yTrain != null ? (applyDefaultExpansion
                                ? expandBounds(positionBounds(descriptor.extractor(), descriptor.data(), descriptor.aes(), false, yTrain,
                                        descriptor.aes().yValue() instanceof AesValue.AfterScale ? xAxisTransform : null),
                                        descriptor.scaleSpec().getYExpand(), yAxisTransform)
                                : positionBounds(descriptor.extractor(), descriptor.data(), descriptor.aes(), false, yTrain,
                                        descriptor.aes().yValue() instanceof AesValue.AfterScale ? xAxisTransform : null))
                        : MinMax.UNIT);

        double stripHeight = (descriptor.facet() != null) ? descriptor.theme().facetStripHeight() : 0;
        double dataHeight = Math.floor((availableHeight - stripHeight * numRows - (numRows - 1) * vGap) / numRows);

        // Distribute the whole-pixel rounding remainder to the last column and
        // last row, so the panel grid stays flush with the plot edges while
        // every panel origin lands on a whole pixel. Fractional panel origins
        // push each panel's primitives onto subpixel coordinates and cost ~10x
        // in the raster paths (see the margin-snap comment in completeLayout).
        var colWidth = new double[numCols];
        Arrays.fill(colWidth, panelWidth);
        colWidth[numCols - 1] = availableWidth - (numCols - 1) * hGap - panelWidth * (numCols - 1);
        var rowHeight = new double[numRows];
        Arrays.fill(rowHeight, dataHeight);
        rowHeight[numRows - 1] = availableHeight - stripHeight * numRows - (numRows - 1) * vGap
                - dataHeight * (numRows - 1);

        // Record the resolved panel geometry so a JFR report can prove whether
        // the facet division left the panel origins on subpixel coordinates.
        var layoutEvt = new FacetLayoutEvent();
        layoutEvt.numCols = numCols;
        layoutEvt.numRows = numRows;
        layoutEvt.panelWidth = panelWidth;
        layoutEvt.panelHeight = dataHeight;
        layoutEvt.originsIntegral = integralFacetOrigins(marginOffsetLeft, marginOffsetTop,
                numCols, numRows, panelWidth, dataHeight, hGap, vGap, stripHeight);
        layoutEvt.begin();
        layoutEvt.end();
        layoutEvt.commit();

        var panelIdx = 0;
        for (var entry : partitions.entrySet()) {
            int row = panelIdx / numCols;
            int col = panelIdx % numCols;

            // Calculate local pixel anchors based on actual margins
            double xStart = marginOffsetLeft + col * (panelWidth + hGap);
            double yStart = marginOffsetTop + row * (dataHeight + stripHeight + vGap);
            double dataYStart = yStart + stripHeight;

            double innerXMin = xStart;
            double innerXMax = xStart + colWidth[col];
            double innerYMin = dataYStart + rowHeight[row];
            double innerYMax = dataYStart;

            // =========================================================================
            // STEP 3: AUTOMATIC AXIS DOMAIN EXPANSION FOR THE GEOMETRIES
            // Each layer reports the domain it wants; fold the union for the plot.
            // =========================================================================
            var prepEvt = new PanelPrepEvent();
            prepEvt.panelIndex = panelIdx;
            prepEvt.panelCount = numPanels;
            prepEvt.begin();
            var adjustedX = descriptor.coord().adjustXBounds(xBounds);
            var adjustedY = descriptor.coord().adjustYBounds(yBounds);

            var rawBounds = new Layer.Bounds(
                    adjustedX.min(), adjustedX.max(), adjustedY.min(), adjustedY.max());
            double dataMinX = adjustedX.min();
            double dataMaxX = adjustedX.max();
            // When no y column is mapped, the plot has no data seed for its y
            // axis, so the fold starts empty and the first contributing layer
            // (e.g. a standalone Geoms.function) defines the y domain instead of
            // being forced to widen a neutral 0..1 seed. If no layer claims y,
            // the empty range is replaced below by the neutral fallback.
            boolean ySeeded = yTrain != null;
            double dataMinY = ySeeded ? adjustedY.min() : Double.POSITIVE_INFINITY;
            double dataMaxY = ySeeded ? adjustedY.max() : Double.NEGATIVE_INFINITY;

            for (var layer : descriptor.geoms()) {
                var wanted = layer.expandDomain(rawBounds, plotContextFor(layer), xIsDiscrete, yIsDiscrete);
                dataMinX = Math.min(dataMinX, wanted.xMin());
                dataMaxX = Math.max(dataMaxX, wanted.xMax());
                dataMinY = Math.min(dataMinY, wanted.yMin());
                dataMaxY = Math.max(dataMaxY, wanted.yMax());
            }

            if (!ySeeded
                    && (!Double.isFinite(dataMinY) || !Double.isFinite(dataMaxY) || !(dataMinY < dataMaxY))) {
                dataMinY = adjustedY.min();
                dataMaxY = adjustedY.max();
            }

            // Explicit limits (scaleXLimits / scaleYLimits) override the
            // data-driven continuous domain on continuous axes only.
            var xLims = descriptor.scaleSpec().getXLimits();
            if (xLims != null && !xIsDiscrete) {
                dataMinX = xLims.min();
                dataMaxX = xLims.max();
            }
            var yLims = descriptor.scaleSpec().getYLimits();
            if (yLims != null && !yIsDiscrete) {
                dataMinY = yLims.min();
                dataMaxY = yLims.max();
            }

            Scale scaleX;
            Scale scaleY;

            // Coords.coordEqual / Coords.coordEqual: centre-crop the panel window so each
            // data unit spans the same pixels on both continuous axes. Equality
            // is measured in transformed space, honouring log/sqrt axis scales.
            if (descriptor.coord() instanceof CoordFixed coordFixed) {
                if (xIsDiscrete || yIsDiscrete) {
                    throw new IllegalArgumentException(
                            "coordEqual requires continuous scales on both axes");
                }
                var spanX = transformedSpan(dataMinX, dataMaxX, descriptor.scaleSpec().getXTransform());
                var spanY = transformedSpan(dataMinY, dataMaxY, descriptor.scaleSpec().getYTransform());
                var equalized = coordFixed.equalizeWindow(
                        innerXMin, innerXMax, innerYMax, innerYMin,
                        descriptor.coord().isFlipped() ? spanY : spanX,
                        descriptor.coord().isFlipped() ? spanX : spanY);
                innerXMin = equalized[0];
                innerXMax = equalized[1];
                innerYMax = equalized[2];
                innerYMin = equalized[3];
            }

            // Axis transformations follow the data aesthetic, not the screen axis,
            // so they stay attached to the same variable under coordFlip().
            // Coords.coordTrans overrides the scale spec on the axes it transforms.
            var xTransform = axisTransform(descriptor.coord(), descriptor.scaleSpec().getXTransform(), xIsDiscrete, "x");
            var yTransform = axisTransform(descriptor.coord(), descriptor.scaleSpec().getYTransform(), yIsDiscrete, "y");

            if (descriptor.coord().isFlipped()) {
                scaleX = yIsDiscrete ? Scale.createDiscrete(yCategories, innerXMin, innerXMax)
                        : new Scale(dataMinY, dataMaxY, innerXMin, innerXMax, yTransform);
                scaleY = xIsDiscrete ? Scale.createDiscrete(xCategories, innerYMin, innerYMax)
                        : new Scale(dataMinX, dataMaxX, innerYMin, innerYMax, xTransform);
            } else {
                scaleX = xIsDiscrete ? Scale.createDiscrete(xCategories, innerXMin, innerXMax)
                        : new Scale(dataMinX, dataMaxX, innerXMin, innerXMax, xTransform);
                scaleY = yIsDiscrete ? Scale.createDiscrete(yCategories, innerYMin, innerYMax)
                        : new Scale(dataMinY, dataMaxY, innerYMin, innerYMax, yTransform);
            }

            // Date scales remember their column type, so a polar coord's theta
            // axis can label epoch-day positions as years just like a Cartesian
            // axis does. Under coordFlip() each scale maps the other aesthetic.
            if (descriptor.coord().isFlipped()) {
                scaleX.setDateScale(yIsDate);
                scaleY.setDateScale(xIsDate);
            } else {
                scaleX.setDateScale(xIsDate);
                scaleY.setDateScale(yIsDate);
            }

            // Logarithmic y-axis (scaleYLog10): apply to whichever scale maps the y data.
            // For Manhattan raw-p plots, reverse it so the most significant (smallest)
            // p-values sit at the top, matching the -log10(P) orientation. A coordTrans
            // override supplies its own transform, so it wins over the legacy setLog.
            boolean yTransOverridden = descriptor.coord() instanceof Coord2D c2d && c2d.transY() != null;
            if (descriptor.scaleSpec().isYLog() && !yIsDiscrete && !yTransOverridden) {
                var yScale = descriptor.coord().isFlipped() ? scaleX : scaleY;
                yScale.setLog(true);
            }

            // Reversed axes (scaleXReverse / scaleYReverse): invert whichever
            // scale maps the x or y data aesthetic, respecting coordFlip() and
            // skipping discrete axes whose category order is already fixed.
            if (descriptor.scaleSpec().isXReverse() && !(descriptor.coord().isFlipped() ? yIsDiscrete : xIsDiscrete)) {
                (descriptor.coord().isFlipped() ? scaleY : scaleX).setReversed(true);
            }
            if (descriptor.scaleSpec().isYReverse() && !(descriptor.coord().isFlipped() ? xIsDiscrete : yIsDiscrete)) {
                (descriptor.coord().isFlipped() ? scaleX : scaleY).setReversed(true);
            }

            panelScales.add(new PanelScaleRegistry<>(entry.getKey(), entry.getValue(), scaleX, scaleY));
            prepEvt.end();
            prepEvt.commit();

            if (descriptor.coord() instanceof Coord3D coord3d && zAes() != null) {
                setupCoord3d(coord3d, descriptor.data(), dataMinX, dataMaxX, dataMinY, dataMaxY,
                        xIsDiscrete, xIsDiscrete ? xCategories : null,
                        yIsDiscrete, yIsDiscrete ? yCategories : null,
                        xAxisTransform, yAxisTransform,
                        innerXMin, innerXMax, innerYMax, innerYMin, plotWidth, plotHeight, gc);
            } else if (descriptor.coord() instanceof Coord2D coord2d) {
                var resolvedLabels = descriptor.options().resolvedForStandalone();
                var shouldDrawX = (row == numRows - 1) || (descriptor.facet() == null);
                var shouldDrawY = (col == 0) || (descriptor.facet() == null);
                coord2d.setPanelBounds(innerXMin, innerYMax, innerXMax - innerXMin, innerYMin - innerYMax);
                coord2d.prepare(scaleX, scaleY, descriptor.theme(), descriptor.scaleSpec(), descriptor.extractor(), descriptor.data(), descriptor.aes(),
                        shouldDrawX && resolvedLabels.drawXLabels(), shouldDrawY && resolvedLabels.drawYLabels(),
                        descriptor.facet() == null && descriptor.scaleSpec().isXAxisTop(), descriptor.facet() == null && descriptor.scaleSpec().isYAxisRight());
                coord2d.renderBackground(gc);
            } else if (descriptor.coord() instanceof CoordPolar coordPolar) {
                coordPolar.setPanelBounds(innerXMin, innerYMax, innerXMax - innerXMin, innerYMin - innerYMax);
                coordPolar.prepare(scaleX, scaleY, descriptor.theme());
                coordPolar.setRadialAxisTitle(descriptor.labs().yLabelIsEmpty()
                        ? (coordPolar.isThetaX() ? descriptor.aes().y() : descriptor.aes().x())
                        : descriptor.labs().yLabel());
                coordPolar.renderBackground(gc);
            }

            // --- 4. Facet strip header ---
            if (descriptor.facet() != null) {
                gc.setFill(descriptor.theme().stripBackground());
                gc.fillRect(xStart, yStart, colWidth[col], stripHeight);

                gc.setFill(descriptor.theme().stripTextColor());
                gc.setFont(descriptor.theme().stripFont());
                gc.setTextAlign(TextAlignment.CENTER);
                gc.fillText(entry.getKey().toString(), xStart + colWidth[col] / 2.0, yStart + stripHeight - 5);
            }

            // --- 5. Geom rendering ---
            gc.save();
            gc.beginPath();
            boolean clipPanel = true;
            if (descriptor.coord() instanceof CoordPolar coordPolar) {
                // A polar panel is the disc (or fan sector), so geometry is
                // clipped there rather than to the surrounding rectangle.
                coordPolar.clipPanel(gc);
            } else if (descriptor.coord() instanceof Coord3D coord3d && !coord3d.clips()) {
                clipPanel = false;
            } else {
                gc.rect(innerXMin, dataYStart, colWidth[col], rowHeight[row]);
            }
            if (clipPanel) {
                gc.clip();
            }

            var partitionDf = entry.getValue();
            var facetValues = facetValuesForPanel(entry.getKey());
            var panelEvt = new FacetPanelEvent();
            panelEvt.panelIndex = panelIdx;
            panelEvt.panelCount = numPanels;
            panelEvt.facetMode = "wrap";
            panelEvt.begin();
            try {
                var layerIdx = 0;
                for (var layer : descriptor.geoms()) {
                    var panelCtx = new PanelContext<>(plotContextFor(layer), partitionDf, facetValues, scaleX, scaleY,
                            effectiveRenderMode(gc));
                    var layerData = preparedData.getOrDefault(layer, LayerData.NONE);
                    var layerEvt = new LayerRenderEvent();
                    layerEvt.layerType = layer.getClass().getSimpleName();
                    layerEvt.layerIndex = layerIdx;
                    layerEvt.panelIndex = panelIdx;
                    layerEvt.begin();
                    try {
                        // Announce each layer so a backend can pick a strategy for it —
                        // a vector backend diverts very dense layers into a raster image.
                        var target = gc.beginLayer(layer.estimatedPrimitiveCount(panelCtx),
                                innerXMin, dataYStart, colWidth[col], rowHeight[row]);
                        var faceGuard = beginFaceRender(layer);
                        var faceClip = faceGuard.clip(target);
                        try {
                            layer.render(target, panelCtx, layerData);
                        } finally {
                            faceClip.close();
                            faceGuard.close();
                            gc.endLayer();
                        }
                    } finally {
                        layerEvt.end();
                        layerEvt.commit();
                    }
                    layerIdx++;
                }
                if (descriptor.coord() instanceof Coord3D coord3d) {
                    coord3d.renderForeground(gc);
                }
            } finally {
                panelEvt.end();
                panelEvt.commit();
            }

            gc.restore();
            panelIdx++;
        }
    }

    /**
     * The continuous, discrete or date domain of one grid axis group, produced
     * by {@link #finalizeGridDomain}. A discrete domain carries its category
     * list; a continuous one its numeric extent after folding in any layer
     * geometry bounds.
     */
    private record AxisDomain(boolean discrete, List<Object> cats, MinMax num) {
    }

    /**
     * Running accumulator of the raw, folded and categorical extents shared by
     * the panels of one grid scale group.
     */
    private static final class GridCellAccum {

        private static final GridCellAccum EMPTY = new GridCellAccum();
        private double rawMin = Double.POSITIVE_INFINITY;
        private double rawMax = Double.NEGATIVE_INFINITY;
        private double foldMin = Double.POSITIVE_INFINITY;
        private double foldMax = Double.NEGATIVE_INFINITY;
        private final List<Object> cats = new ArrayList<>();
        private boolean hasData = false;

        static GridCellAccum empty() {
            return EMPTY;
        }
    }

    /**
     * Draws the full matrix of grid-facet panels, honouring the free-scale,
     * free-space, margin, strip-switch and outer-axis options of the facet's
     * {@link GridOptions}.
     */
    private void drawGridPanels(DrawSurface gc, FacetGrid<DF> grid, double plotWidth, double plotHeight, PlotLayout layout) {
        var marginOffsetTop = layout.marginTop();
        var marginOffsetBottom = layout.marginBottom();
        var marginOffsetLeft = layout.marginLeft();
        var marginOffsetRight = layout.marginRight();
        var availableWidth = plotWidth - marginOffsetLeft - marginOffsetRight;
        var availableHeight = plotHeight - marginOffsetTop - marginOffsetBottom;

        panelScales.clear();
        gridWidestYLabelWidth = -1.0;

        var opts = descriptor.facet().getOptions();
        var rowVars = descriptor.facet().getRowVars();
        var colVars = descriptor.facet().getColVars();
        var colStripTop = opts.switchPreset() != GridOptions.GridSwitch.X
                && opts.switchPreset() != GridOptions.GridSwitch.BOTH;
        var rowStripLeft = opts.switchPreset() == GridOptions.GridSwitch.Y
                || opts.switchPreset() == GridOptions.GridSwitch.BOTH;
        // Axes follow the strips: X labels/title move to the top when the
        // column strips sit at the bottom, and Y labels/title move to the
        // right when the row strips sit at the left.
        var xAxisSwitched = !colStripTop;
        var yAxisSwitched = rowStripLeft;
        var stripHeight = colVars.isEmpty() ? 0 : 18;
        var stripWidth = rowVars.isEmpty() ? 0 : predictedGridRowStripWidth(grid, rowVars);
        var topInset = colStripTop ? stripHeight : 0;
        var bottomInset = colStripTop ? 0 : stripHeight;
        var leftInset = rowStripLeft ? stripWidth : 0;
        var rightInset = rowStripLeft ? 0 : stripWidth;

        var gridX = marginOffsetLeft + leftInset;
        var gridY = marginOffsetTop + topInset;
        var gridW = availableWidth - leftInset - rightInset;
        var gridH = availableHeight - topInset - bottomInset;

        var rows = new ArrayList<List<Object>>(grid.rows());
        if (!opts.asTable()) {
            Collections.reverse(rows);
        }
        var cols = grid.cols();
        var numRows = rows.size();
        var numCols = cols.size();

        var xTrain = positionTrainColumn(descriptor.aes(), true);
        var yTrain = positionTrainColumn(descriptor.aes(), false);
        var coordColX = xTrain;
        var coordColY = yTrain;
        var xIsDiscrete = xTrain != null && descriptor.extractor().columnType(descriptor.data(), xTrain) == DataExtractor.ColumnType.TEXT;
        var yIsDiscrete = yTrain != null && descriptor.extractor().columnType(descriptor.data(), yTrain) == DataExtractor.ColumnType.TEXT;
        var xIsDate = xTrain != null && descriptor.extractor().columnType(descriptor.data(), xTrain) == DataExtractor.ColumnType.DATE;
        var yIsDate = yTrain != null && descriptor.extractor().columnType(descriptor.data(), yTrain) == DataExtractor.ColumnType.DATE;
        var applyDefaultExpansion = descriptor.geoms().stream().anyMatch(Layer::wantsDefaultExpansion);

        var xAxisTransform = axisTransform(descriptor.coord(), descriptor.scaleSpec().getXTransform(), xIsDiscrete, "x");
        var yAxisTransform = axisTransform(descriptor.coord(), descriptor.scaleSpec().getYTransform(), yIsDiscrete, "y");
        var globalXBounds = xIsDiscrete
                ? new MinMax(-0.5, Math.max(-0.5, (double) Scale.uniqueCategories(descriptor.data(), descriptor.extractor(), xTrain).size() - 0.5))
                : (xTrain != null
                        ? (applyDefaultExpansion ? expandBounds(positionBounds(descriptor.extractor(), descriptor.data(), descriptor.aes(), true, xTrain,
                                        descriptor.aes().xValue() instanceof AesValue.AfterScale ? yAxisTransform : null),
                                        descriptor.scaleSpec().getXExpand(), xAxisTransform)
                                : positionBounds(descriptor.extractor(), descriptor.data(), descriptor.aes(), true, xTrain,
                                        descriptor.aes().xValue() instanceof AesValue.AfterScale ? yAxisTransform : null))
                        : MinMax.UNIT);
        var globalYBounds = yIsDiscrete
                ? new MinMax(-0.5, Math.max(-0.5, (double) Scale.uniqueCategories(descriptor.data(), descriptor.extractor(), yTrain).size() - 0.5))
                : (yTrain != null
                        ? (applyDefaultExpansion ? expandBounds(positionBounds(descriptor.extractor(), descriptor.data(), descriptor.aes(), false, yTrain,
                                        descriptor.aes().yValue() instanceof AesValue.AfterScale ? xAxisTransform : null),
                                        descriptor.scaleSpec().getYExpand(), yAxisTransform)
                                : positionBounds(descriptor.extractor(), descriptor.data(), descriptor.aes(), false, yTrain,
                                        descriptor.aes().yValue() instanceof AesValue.AfterScale ? xAxisTransform : null))
                        : MinMax.UNIT);

        // Per-cell extents, folded (geometry-widened) and raw.
        var n = numRows * numCols;
        var cellRawXMin = new double[n];
        var cellXMin = new double[n];
        var cellRawYMin = new double[n];
        var cellYMin = new double[n];
        var cellRawXMax = new double[n];
        var cellXMax = new double[n];
        var cellRawYMax = new double[n];
        var cellYMax = new double[n];
        var cellHasX = new boolean[n];
        var cellHasY = new boolean[n];
        @SuppressWarnings({"unchecked", "rawtypes"})
        List<Object>[] cellXCats = new List[n];
        @SuppressWarnings({"unchecked", "rawtypes"})
        List<Object>[] cellYCats = new List[n];

        var cellExtentEvt = new CellExtentCollectionEvent();
        cellExtentEvt.cellCount = n;
        cellExtentEvt.rowSpan = numRows;
        cellExtentEvt.colSpan = numCols;
        cellExtentEvt.layerCount = descriptor.geoms().size();
        cellExtentEvt.begin();
        for (var r = 0; r < numRows; r++) {
            for (var c = 0; c < numCols; c++) {
                var idx = r * numCols + c;
                var partDf = grid.panel(rows.get(r), cols.get(c));
                collectCellExtents(partDf, coordColX, coordColY, xIsDiscrete, yIsDiscrete,
                        cellRawXMin, cellXMin, cellRawYMin, cellYMin, cellRawXMax, cellXMax,
                        cellRawYMax, cellYMax, cellHasX, cellHasY, cellXCats, cellYCats, idx);
            }
        }
        cellExtentEvt.end();
        cellExtentEvt.commit();

        var xGlobal = "x:global";
        var yGlobal = "y:global";
        var xGroups = new LinkedHashMap<Object, GridCellAccum>();
        var yGroups = new LinkedHashMap<Object, GridCellAccum>();
        for (var r = 0; r < numRows; r++) {
            for (var c = 0; c < numCols; c++) {
                var idx = r * numCols + c;
                accumulateGrid(xGroups, gridXKey(opts.scale(), xGlobal, r, c),
                        cellHasX[idx], cellRawXMin[idx], cellRawXMax[idx], cellXMin[idx], cellXMax[idx], cellXCats[idx]);
                accumulateGrid(yGroups, gridYKey(opts.scale(), yGlobal, r, c),
                        cellHasY[idx], cellRawYMin[idx], cellRawYMax[idx], cellYMin[idx], cellYMax[idx], cellYCats[idx]);
            }
        }

        var xDomains = new LinkedHashMap<Object, AxisDomain>();
        var yDomains = new LinkedHashMap<Object, AxisDomain>();
        for (var entry : xGroups.entrySet()) {
            xDomains.put(entry.getKey(), finalizeGridDomain(entry.getValue(), xIsDiscrete, coordColX, globalXBounds,
                    applyDefaultExpansion, descriptor.scaleSpec().getXExpand(), entry.getKey(), xGlobal, descriptor.scaleSpec().getXLimits()));
        }
        for (var entry : yGroups.entrySet()) {
            yDomains.put(entry.getKey(), finalizeGridDomain(entry.getValue(), yIsDiscrete, coordColY, globalYBounds,
                    applyDefaultExpansion, descriptor.scaleSpec().getYExpand(), entry.getKey(), yGlobal, descriptor.scaleSpec().getYLimits()));
        }
        xDomains.computeIfAbsent(xGlobal, key -> finalizeGridDomain(GridCellAccum.empty(), xIsDiscrete, coordColX,
                globalXBounds, applyDefaultExpansion, descriptor.scaleSpec().getXExpand(), key, xGlobal, descriptor.scaleSpec().getXLimits()));
        yDomains.computeIfAbsent(yGlobal, key -> finalizeGridDomain(GridCellAccum.empty(), yIsDiscrete, coordColY,
                globalYBounds, applyDefaultExpansion, descriptor.scaleSpec().getYExpand(), key, yGlobal, descriptor.scaleSpec().getYLimits()));

        // Column/row weights: uniform unless space is free, when a panel's
        // share follows the extent of its scale domain.
        var colWeight = new double[numCols];
        var rowWeight = new double[numRows];
        if (opts.space() == GridOptions.Space.FREE) {
            for (var c = 0; c < numCols; c++) {
                var max = 0.0;
                for (var r = 0; r < numRows; r++) {
                    var dom = gridDomain(xDomains, gridXKey(opts.scale(), xGlobal, r, c), xGlobal);
                    if (dom != null) {
                        max = Math.max(max, gridExtentLen(dom));
                    }
                }
                colWeight[c] = max;
            }
            for (var r = 0; r < numRows; r++) {
                var max = 0.0;
                for (var c = 0; c < numCols; c++) {
                    var dom = gridDomain(yDomains, gridYKey(opts.scale(), yGlobal, r, c), yGlobal);
                    if (dom != null) {
                        max = Math.max(max, gridExtentLen(dom));
                    }
                }
                rowWeight[r] = max;
            }
        } else {
            Arrays.fill(colWeight, 1.0);
            Arrays.fill(rowWeight, 1.0);
        }

        var colGap = descriptor.theme().facetHGap();
        var rowGap = descriptor.theme().facetVGap();
        var sumW = Arrays.stream(colWeight).sum();
        var sumH = Arrays.stream(rowWeight).sum();
        var totalPanelW = gridW - colGap * (numCols - 1);
        var totalPanelH = gridH - rowGap * (numRows - 1);

        // Floor every column/row to whole pixels and absorb the sub-pixel
        // remainder into the last column/row, so each panel origin lands on a
        // whole pixel while the grid stays flush with its edges. Fractional
        // panel origins push every panel primitive onto subpixel coordinates
        // and cost ~10x in the raster paths (see drawFacetsGrid).
        var colX = new double[numCols];
        var colWidth = new double[numCols];
        var usedW = 0.0;
        for (var c = 0; c < numCols - 1; c++) {
            colWidth[c] = Math.floor(totalPanelW * colWeight[c] / sumW);
            usedW += colWidth[c] + colGap;
        }
        colWidth[numCols - 1] = gridW - usedW;
        colX[0] = gridX;
        for (var c = 1; c < numCols; c++) {
            colX[c] = colX[c - 1] + colWidth[c - 1] + colGap;
        }
        var rowY = new double[numRows];
        var rowHeight = new double[numRows];
        var usedH = 0.0;
        for (var r = 0; r < numRows - 1; r++) {
            rowHeight[r] = Math.floor(totalPanelH * rowWeight[r] / sumH);
            usedH += rowHeight[r] + rowGap;
        }
        rowHeight[numRows - 1] = gridH - usedH;
        rowY[0] = gridY;
        for (var r = 1; r < numRows; r++) {
            rowY[r] = rowY[r - 1] + rowHeight[r - 1] + rowGap;
        }

        var layoutEvt = new FacetLayoutEvent();
        layoutEvt.numCols = numCols;
        layoutEvt.numRows = numRows;
        layoutEvt.panelWidth = colWidth[0];
        layoutEvt.panelHeight = rowHeight[0];
        layoutEvt.originsIntegral = integralFacetOrigins(gridX, gridY,
                numCols, numRows, colWidth[0], rowHeight[0], colGap, rowGap, 0.0);
        layoutEvt.begin();
        layoutEvt.end();
        layoutEvt.commit();

        var xTransform = descriptor.scaleSpec().getXTransform();
        var yTransform = descriptor.scaleSpec().getYTransform();
        if (descriptor.coord() instanceof Coord2D coord2d) {
            if (coord2d.transX() != null) {
                xTransform = coord2d.transX();
            }
            if (coord2d.transY() != null) {
                yTransform = coord2d.transY();
            }
        }
        var axes = opts.axes();

        for (var r = 0; r < numRows; r++) {
            for (var c = 0; c < numCols; c++) {
                var rowKey = rows.get(r);
                var colKey = cols.get(c);
                var partDf = grid.panel(rowKey, colKey);
                var xStart = colX[c];
                var yStart = rowY[r];
                var panelWidth = colWidth[c];
                var panelHeight = rowHeight[r];
                var dataYStart = yStart;
                var innerXMin = xStart;
                var innerXMax = xStart + panelWidth;
                var innerYMin = dataYStart + panelHeight;
                var innerYMax = dataYStart;

                var xDom = gridDomain(xDomains, gridXKey(opts.scale(), xGlobal, r, c), xGlobal);
                var yDom = gridDomain(yDomains, gridYKey(opts.scale(), yGlobal, r, c), yGlobal);

                var prepEvt = new PanelPrepEvent();
                prepEvt.panelIndex = r * numCols + c;
                prepEvt.panelCount = numRows * numCols;
                prepEvt.begin();
                // Coords.coordEqual / Coords.coordEqual: centre-crop the panel window so
                // each data unit spans the same pixels on both continuous axes.
                if (descriptor.coord() instanceof CoordFixed coordFixed) {
                    if (xDom.discrete() || yDom.discrete()) {
                        throw new IllegalArgumentException("coordEqual requires continuous scales on both axes");
                    }
                    var spanX = transformedSpan(xDom.num().min(), xDom.num().max(), xTransform);
                    var spanY = transformedSpan(yDom.num().min(), yDom.num().max(), yTransform);
                    var equalized = coordFixed.equalizeWindow(
                            innerXMin, innerXMax, innerYMax, innerYMin,
                            descriptor.coord().isFlipped() ? spanY : spanX,
                            descriptor.coord().isFlipped() ? spanX : spanY);
                    innerXMin = equalized[0];
                    innerXMax = equalized[1];
                    innerYMax = equalized[2];
                    innerYMin = equalized[3];
                }
                if (descriptor.coord() instanceof Coord2D c2d && (c2d.transX() != null || c2d.transY() != null)) {
                    if ((c2d.transX() != null && xDom.discrete()) || (c2d.transY() != null && yDom.discrete())) {
                        throw new IllegalArgumentException("coordTrans requires a continuous scale on every transformed axis");
                    }
                }

                Scale scaleX;
                Scale scaleY;
                if (descriptor.coord().isFlipped()) {
                    scaleX = yDom.discrete() ? Scale.createDiscrete(yDom.cats(), innerXMin, innerXMax)
                            : new Scale(yDom.num().min(), yDom.num().max(), innerXMin, innerXMax, yTransform);
                    scaleY = xDom.discrete() ? Scale.createDiscrete(xDom.cats(), innerYMin, innerYMax)
                            : new Scale(xDom.num().min(), xDom.num().max(), innerYMin, innerYMax, xTransform);
                } else {
                    scaleX = xDom.discrete() ? Scale.createDiscrete(xDom.cats(), innerXMin, innerXMax)
                            : new Scale(xDom.num().min(), xDom.num().max(), innerXMin, innerXMax, xTransform);
                    scaleY = yDom.discrete() ? Scale.createDiscrete(yDom.cats(), innerYMin, innerYMax)
                            : new Scale(yDom.num().min(), yDom.num().max(), innerYMin, innerYMax, yTransform);
                }
                if (descriptor.coord().isFlipped()) {
                    scaleX.setDateScale(yIsDate);
                    scaleY.setDateScale(xIsDate);
                } else {
                    scaleX.setDateScale(xIsDate);
                    scaleY.setDateScale(yIsDate);
                }
                if (descriptor.scaleSpec().isYLog() && !yIsDiscrete
                        && !(descriptor.coord() instanceof Coord2D c2d && c2d.transY() != null)) {
                    var yScale = descriptor.coord().isFlipped() ? scaleX : scaleY;
                    yScale.setLog(true);
                }

                // Reversed axes (scaleXReverse / scaleYReverse): invert
                // whichever scale maps the x or y data aesthetic, respecting
                // coordFlip() and skipping discrete axes.
                if (descriptor.scaleSpec().isXReverse() && !(descriptor.coord().isFlipped() ? yDom.discrete() : xDom.discrete())) {
                    (descriptor.coord().isFlipped() ? scaleY : scaleX).setReversed(true);
                }
                if (descriptor.scaleSpec().isYReverse() && !(descriptor.coord().isFlipped() ? xDom.discrete() : yDom.discrete())) {
                    (descriptor.coord().isFlipped() ? scaleX : scaleY).setReversed(true);
                }

                if (partDf != null) {
                    panelScales.add(new PanelScaleRegistry<>(new FacetGrid.Cell(rowKey, colKey), partDf, scaleX, scaleY));
                }
                prepEvt.end();
                prepEvt.commit();

                if (descriptor.coord() instanceof Coord3D coord3d && zAes() != null) {
                    setupCoord3d(coord3d, descriptor.data(), xDom.num().min(), xDom.num().max(),
                            yDom.num().min(), yDom.num().max(),
                            xDom.discrete(), xDom.discrete() ? xDom.cats() : null,
                            yDom.discrete(), yDom.discrete() ? yDom.cats() : null,
                            xAxisTransform, yAxisTransform,
                            innerXMin, innerXMax, innerYMax, innerYMin, plotWidth, plotHeight, gc);
                } else if (descriptor.coord() instanceof Coord2D coord2d) {
                    coord2d.setPanelBounds(innerXMin, innerYMax, innerXMax - innerXMin, innerYMin - innerYMax);
                    var drawY = gridDrawYLabels(c, numCols, axes, yAxisSwitched);
                    coord2d.prepare(scaleX, scaleY, descriptor.theme(), descriptor.scaleSpec(), descriptor.extractor(), descriptor.data(), descriptor.aes(),
                            gridDrawXLabels(r, numRows, axes, xAxisSwitched), drawY,
                            xAxisSwitched, yAxisSwitched);
                    coord2d.renderBackground(gc);
                    if (drawY) {
                        gridWidestYLabelWidth = Math.max(gridWidestYLabelWidth, coord2d.widestYLabelWidth());
                    }
                } else if (descriptor.coord() instanceof CoordPolar coordPolar) {
                    coordPolar.setPanelBounds(innerXMin, innerYMax, innerXMax - innerXMin, innerYMin - innerYMax);
                    coordPolar.prepare(scaleX, scaleY, descriptor.theme());
                    coordPolar.setRadialAxisTitle(descriptor.labs().yLabelIsEmpty()
                            ? (coordPolar.isThetaX() ? descriptor.aes().y() : descriptor.aes().x())
                            : descriptor.labs().yLabel());
                    coordPolar.renderBackground(gc);
                }

                // --- Column strip above (or below) the grid ---
                if (!colVars.isEmpty() && ((colStripTop && r == 0) || (!colStripTop && r == numRows - 1))) {
                    var stripTop = colStripTop ? gridY - stripHeight : gridY + gridH;
                    gc.setFill(descriptor.theme().stripBackground());
                    gc.fillRect(xStart, stripTop, panelWidth, stripHeight);
                    gc.setFill(descriptor.theme().stripTextColor());
                    gc.setFont(descriptor.theme().stripFont());
                    gc.setTextAlign(TextAlignment.CENTER);
                    drawStripColLabels(gc, colKey, xStart + panelWidth / 2.0, stripTop, stripHeight);
                }
                // --- Row strip on the left (or right) of the grid ---
                if (!rowVars.isEmpty() && ((rowStripLeft && c == 0) || (!rowStripLeft && c == numCols - 1))) {
                    var stripLeft = rowStripLeft ? gridX - stripWidth : gridX + gridW;
                    gc.setFill(descriptor.theme().stripBackground());
                    gc.fillRect(stripLeft, yStart, stripWidth, panelHeight);
                    gc.setFill(descriptor.theme().stripTextColor());
                    gc.setFont(descriptor.theme().stripFont());
                    drawStripRowLabels(gc, rowKey, stripLeft + stripWidth / 2.0, yStart, panelHeight);
                }

                // --- Geom rendering ---
                if (partDf != null) {
                    var facetValues = gridFacetValues(rowKey, colKey, rowVars, colVars);
                    gc.save();
                    gc.beginPath();
                    boolean clipPanel = true;
                    if (descriptor.coord() instanceof CoordPolar coordPolar) {
                        coordPolar.clipPanel(gc);
                    } else if (descriptor.coord() instanceof Coord3D coord3d && !coord3d.clips()) {
                        clipPanel = false;
                    } else {
                        gc.rect(innerXMin, dataYStart, panelWidth, panelHeight);
                    }
                    if (clipPanel) {
                        gc.clip();
                    }
                    var gridPanelEvt = new FacetPanelEvent();
                    gridPanelEvt.panelIndex = r * numCols + c;
                    gridPanelEvt.panelCount = numRows * numCols;
                    gridPanelEvt.facetMode = "grid";
                    gridPanelEvt.begin();
                    try {
                        var layerIdx = 0;
                        for (var layer : descriptor.geoms()) {
                            var panelCtx = new PanelContext<>(plotContextFor(layer), partDf, facetValues, scaleX, scaleY,
                                    effectiveRenderMode(gc));
                            var layerData = preparedData.getOrDefault(layer, LayerData.NONE);
                            var layerEvt = new LayerRenderEvent();
                            layerEvt.layerType = layer.getClass().getSimpleName();
                            layerEvt.layerIndex = layerIdx;
                            layerEvt.panelIndex = r * numCols + c;
                            layerEvt.begin();
                            try {
                                var target = gc.beginLayer(layer.estimatedPrimitiveCount(panelCtx),
                                        innerXMin, dataYStart, panelWidth, panelHeight);
                                var faceGuard = beginFaceRender(layer);
                                var faceClip = faceGuard.clip(target);
                                try {
                                    layer.render(target, panelCtx, layerData);
                                } finally {
                                    faceClip.close();
                                    faceGuard.close();
                                    gc.endLayer();
                                }
                            } finally {
                                layerEvt.end();
                                layerEvt.commit();
                            }
                            layerIdx++;
                        }
                        if (descriptor.coord() instanceof Coord3D coord3d) {
                            coord3d.renderForeground(gc);
                        }
                    } finally {
                        gridPanelEvt.end();
                        gridPanelEvt.commit();
                    }
                    gc.restore();
                }
            }
        }
    }

    /**
     * Collects the raw and folded (geometry-widened) extent of one grid cell
     * into the shared arrays, plus the discrete categories when the axis is
     * categorical.
     */
    private void collectCellExtents(DF partDf, String xCol, String yCol, boolean xIsDiscrete, boolean yIsDiscrete,
            double[] rawXMin, double[] xMin, double[] rawYMin, double[] yMin,
            double[] rawXMax, double[] xMax, double[] rawYMax, double[] yMax,
            boolean[] hasX, boolean[] hasY, List<Object>[] xCats, List<Object>[] yCats,
            int idx) {
        double xmn = xCol == null ? MinMax.UNIT.min() : Double.POSITIVE_INFINITY;
        double xmx = xCol == null ? MinMax.UNIT.max() : Double.NEGATIVE_INFINITY;
        double ymn = yCol == null ? MinMax.UNIT.min() : Double.POSITIVE_INFINITY;
        double ymx = yCol == null ? MinMax.UNIT.max() : Double.NEGATIVE_INFINITY;
        var hasData = partDf != null && descriptor.extractor().getRowCount(partDf) > 0;
        if (hasData) {
            if (xCol != null) {
                if (xIsDiscrete) {
                    var set = new LinkedHashSet<Object>();
                    for (var object : descriptor.extractor().getColumn(partDf, xCol)) {
                        if (object != null) {
                            set.add(object);
                        }
                    }
                    if (!set.isEmpty()) {
                        xCats[idx] = new ArrayList<>(set);
                    }
                } else {
                    var mm = descriptor.extractor().getMinMax(partDf, xCol);
                    if (mm.min() <= mm.max()) {
                        xmn = mm.min();
                        xmx = mm.max();
                        rawXMin[idx] = xmn;
                        rawXMax[idx] = xmx;
                        hasX[idx] = true;
                    }
                }
            }
            if (yCol != null) {
                if (yIsDiscrete) {
                    var set = new LinkedHashSet<Object>();
                    for (var object : descriptor.extractor().getColumn(partDf, yCol)) {
                        if (object != null) {
                            set.add(object);
                        }
                    }
                    if (!set.isEmpty()) {
                        yCats[idx] = new ArrayList<>(set);
                    }
                } else {
                    var mm = descriptor.extractor().getMinMax(partDf, yCol);
                    if (mm.min() <= mm.max()) {
                        ymn = mm.min();
                        ymx = mm.max();
                        rawYMin[idx] = ymn;
                        rawYMax[idx] = ymx;
                        hasY[idx] = true;
                    }
                }
            }
            var raw = new Layer.Bounds(xmn, xmx, ymn, ymx);
            for (var layer : descriptor.geoms()) {
                var wanted = layer.expandDomain(raw, plotContextFor(layer), xIsDiscrete, yIsDiscrete);
                xmn = Math.min(xmn, wanted.xMin());
                xmx = Math.max(xmx, wanted.xMax());
                ymn = Math.min(ymn, wanted.yMin());
                ymx = Math.max(ymx, wanted.yMax());
            }
        }
        xMin[idx] = xmn;
        xMax[idx] = xmx;
        yMin[idx] = ymn;
        yMax[idx] = ymx;
    }

    private Object gridXKey(GridOptions.Scale scale, Object global, int r, int c) {
        return switch (scale) {
            case GridOptions.Scale.FIXED, GridOptions.Scale.FREE_Y -> global;
            case GridOptions.Scale.FREE_X -> "x:col" + c;
            case GridOptions.Scale.FREE -> "x:cell" + r + "," + c;
        };
    }

    private Object gridYKey(GridOptions.Scale scale, Object global, int r, int c) {
        return switch (scale) {
            case GridOptions.Scale.FIXED, GridOptions.Scale.FREE_X -> global;
            case GridOptions.Scale.FREE_Y -> "y:row" + r;
            case GridOptions.Scale.FREE -> "y:cell" + r + "," + c;
        };
    }

    private void accumulateGrid(Map<Object, GridCellAccum> groups, Object key, boolean has, double rawMin,
            double rawMax, double foldMin, double foldMax, List<Object> cats) {
        var acc = groups.computeIfAbsent(key, k -> new GridCellAccum());
        if (cats != null) {
            acc.cats.addAll(cats);
        }
        if (has) {
            acc.rawMin = Math.min(acc.rawMin, rawMin);
            acc.rawMax = Math.max(acc.rawMax, rawMax);
            acc.foldMin = Math.min(acc.foldMin, foldMin);
            acc.foldMax = Math.max(acc.foldMax, foldMax);
            acc.hasData = true;
        }
    }

    private AxisDomain finalizeGridDomain(GridCellAccum acc, boolean discrete, String col, MinMax globalBounds,
            boolean applyDefaultExpansion, Expansion expansion, Object key,
            Object globalKey, MinMax limits) {
        if (discrete) {
            var cats = key.equals(globalKey) || acc.cats.isEmpty()
                    ? Scale.uniqueCategories(descriptor.data(), descriptor.extractor(), col)
                    : acc.cats;
            var num = descriptor.coord().adjustXBounds(new MinMax(-0.5, (double) cats.size() - 0.5));
            return new AxisDomain(true, cats, num);
        }
        if (!acc.hasData) {
            return new AxisDomain(false, null, descriptor.coord().adjustXBounds(globalBounds));
        }
        var bMin = applyDefaultExpansion ? expansion.expand(acc.rawMin, acc.rawMax).min() : acc.rawMin;
        var bMax = applyDefaultExpansion ? expansion.expand(acc.rawMin, acc.rawMax).max() : acc.rawMax;
        var adjusted = descriptor.coord().adjustXBounds(new MinMax(bMin, bMax));
        var bounds = new MinMax(Math.min(adjusted.min(), acc.foldMin), Math.max(adjusted.max(), acc.foldMax));
        if (limits != null) {
            bounds = limits;
        }
        return new AxisDomain(false, null, bounds);
    }

    private AxisDomain gridDomain(Map<Object, AxisDomain> domains, Object key, Object global) {
        var dom = domains.get(key);
        return dom != null ? dom : domains.get(global);
    }

    private double gridExtentLen(AxisDomain dom) {
        if (dom.discrete()) {
            return Math.max(dom.cats().size(), 1);
        }
        return Math.max(dom.num().max() - dom.num().min(), 1.0e-9);
    }

    private boolean gridDrawXLabels(int r, int numRows, GridOptions.Axes axes, boolean switched) {
        boolean margin = switched ? r == 0 : r == numRows - 1;
        return margin || axes == GridOptions.Axes.ALL || axes == GridOptions.Axes.ALL_X;
    }

    private boolean gridDrawYLabels(int c, int numCols, GridOptions.Axes axes, boolean switched) {
        boolean margin = switched ? c == numCols - 1 : c == 0;
        return margin || axes == GridOptions.Axes.ALL || axes == GridOptions.Axes.ALL_Y;
    }

    private void drawStripColLabels(DrawSurface gc, List<Object> key, double cx, double top, double height) {
        int k = key.size();
        var line = descriptor.theme().stripFont().getSize() + 1.0;
        var center = top + height / 2.0;
        for (var i = 0; i < k; i++) {
            gc.fillText(gridStripLabel(key.get(i)), cx, center + ((double) i - (double) (k - 1) / 2.0) * line + 3.0);
        }
    }

    private void drawStripRowLabels(DrawSurface gc, List<Object> key, double cx, double top, double height) {
        int k = key.size();
        var line = descriptor.theme().stripFont().getSize() + 1.0;
        gc.save();
        gc.translate(cx, top + height / 2.0);
        gc.rotate(-90.0);
        gc.setTextAlign(TextAlignment.CENTER);
        for (var i = 0; i < k; i++) {
            gc.fillText(gridStripLabel(key.get(i)), 0.0, (double) (k - 1) / 2.0 * line - (double) i * line + 3.0);
        }
        gc.restore();
    }

    private String gridStripLabel(Object value) {
        if (FacetGrid.ALL.equals(value)) {
            return "(all)";
        }
        return descriptor.labs().map(String.valueOf(value));
    }

    private double predictedGridRowStripWidth(FacetGrid<DF> grid, List<String> rowVars) {
        var helper = new Text();
        helper.setFont(descriptor.theme().stripFont());
        double tallest = 0.0;
        for (var row : grid.rows()) {
            for (var i = 0; i < rowVars.size(); i++) {
                helper.setText(gridStripLabel(row.get(i)));
                tallest = Math.max(tallest, helper.getLayoutBounds().getHeight());
            }
        }
        return tallest + 10.0;
    }

    private FacetValues gridFacetValues(List<Object> rowKey, List<Object> colKey, List<String> rowVars,
            List<String> colVars) {
        var values = new LinkedHashMap<String, Object>();
        for (var i = 0; i < rowVars.size(); i++) {
            values.put(rowVars.get(i), rowKey.get(i));
        }
        for (var i = 0; i < colVars.size(); i++) {
            values.put(colVars.get(i), colKey.get(i));
        }
        return FacetValues.of(values);
    }

    private Set<String> resolvedMarginVars(GridOptions options) {
        if (!options.margins()) {
            return Set.of();
        }
        var marginVars = options.marginVars();
        var candidates = new LinkedHashSet<String>();
        candidates.addAll(descriptor.facet().getRowVars());
        candidates.addAll(descriptor.facet().getColVars());
        if (marginVars.isEmpty()) {
            return candidates;
        }
        candidates.retainAll(marginVars);
        return candidates;
    }

    private Map<Object, DF> partitionData() {
        var facetCol = (descriptor.facet() != null) ? descriptor.facet().getColumnName() : null;
        return descriptor.extractor().partition(descriptor.data(), facetCol);
    }

    /**
     * Whether every facet panel origin computed from the given grid geometry
     * falls on a whole pixel. A fractional {@code panelWidth}/{@code panelHeight}
     * (or a fractional margin origin) pushes each panel's primitives onto
     * subpixel coordinates, which costs ~10x in the raster paths.
     */
    private static boolean integralFacetOrigins(double marginOffsetLeft, double marginOffsetTop,
            int numCols, int numRows, double panelWidth, double panelHeight,
            double hGap, double vGap, double stripHeight) {
        for (int row = 0; row < numRows; row++) {
            for (int col = 0; col < numCols; col++) {
                double xStart = marginOffsetLeft + col * (panelWidth + hGap);
                double yStart = marginOffsetTop + row * (panelHeight + stripHeight + vGap);
                if (xStart != Math.floor(xStart) || yStart != Math.floor(yStart)) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Builds the {@link FacetValues} for one grid-facet or wrap-facet panel
     * from its panel key. For a grid panel the key is the {@link FacetGrid.Cell}
     * carrying the row and column levels; for a wrap panel it is the partition
     * value; for a null key (no facet) the values are empty so data-driven
     * layers draw their whole frame in the single panel.
     */
    private FacetValues facetValuesForPanel(Object panelKey) {
        if (panelKey instanceof FacetGrid.Cell cell) {
            return gridFacetValues(cell.row(), cell.col(), descriptor.facet().getRowVars(), descriptor.facet().getColVars());
        }
        var facetCol = (descriptor.facet() != null) ? descriptor.facet().getColumnName() : null;
        if (facetCol == null) {
            return FacetValues.of(Map.of());
        }
        return FacetValues.of(Map.of(facetCol, panelKey));
    }

    /**
     * Applies the plot's label dictionary (from {@code labs().map(...)}) to a
     * tooltip, replacing raw category values with their display names so hover
     * text matches the legend. Only tokens that appear as a whole line, or as
     * the value after a {@code "Key: "} prefix, are remapped — numeric/stat
     * lines such as {@code "n = 25"} are left untouched.
     */
    private String applyLabelMappings(String tooltip) {
        if (tooltip == null) {
            return null;
        }
        @SuppressWarnings("StringSplitter") // no trailing-empty-string case here; avoids adding a Guava dependency
        var lines = Pattern.compile("\n").split(tooltip);
        for (var i = 0; i < lines.length; i++) {
            var line = lines[i];
            var mapped = descriptor.labs().map(line.trim());
            if (!mapped.equals(line.trim())) {
                lines[i] = mapped;
                continue;
            }
            var colon = line.indexOf(": ");
            if (colon > 0) {
                var value = line.substring(colon + 2).trim();
                var mappedValue = descriptor.labs().map(value);
                if (!mappedValue.equals(value)) {
                    lines[i] = line.substring(0, colon + 2) + mappedValue;
                }
            }
        }
        return String.join("\n", lines);
    }

    private Color resolveTooltipColor(String tooltipText) {
        var groupKey = descriptor.aes().color() != null ? descriptor.aes().color() : descriptor.aes().fill();

        if (groupKey != null) {
            var colour = scales().tooltipColor(groupKey, tooltipText);
            if (colour != null) {
                return colour;
            }
        }

        // The plot-global aes may not carry the colour column when the mapping
        // lives on the layer (e.g. afterStat / afterScale).  Try every
        // registered colour scale and return the first match.
        for (var col : scales().colorScaleColumns()) {
            var colour = scales().tooltipColor(col, tooltipText);
            if (colour != null) {
                return colour;
            }
        }

        // Reactive fallback for ungrouped single plots:
        // check layer geometries that carry a constant colour or fill (e.g. a
        // fixed-colour scatter or bar with no mapped aesthetic), and report the
        // very colour the geometry was painted with instead of a default.
        for (var layer : descriptor.geoms()) {
            if (layer instanceof ConstantColorLayer constant) {
                var geomColor = constant.getCustomColor();
                if (geomColor != null) {
                    return geomColor; // Use the exact configured colour
                }
            }
        }
        return descriptor.theme().fallbackColor(); // Absolute default fallback (red)
    }

    private PointShape resolveTooltipShape(String tooltipText) {
        var shapeColumn = descriptor.aes().shape();
        if (shapeColumn != null) {
            var shape = scales().tooltipShape(shapeColumn, tooltipText);
            if (shape != null) {
                return shape;
            }
        }
        // Reactive fallback for ungrouped single plots: a fixed point symbol
        for (var layer : descriptor.geoms()) {
            if (layer instanceof GeomPoint) {
                var custom = ((GeomPoint<?>) layer).getShape();
                if (custom != null) {
                    return custom;
                }
            }
        }
        return PointShape.CIRCLE;
    }

    /**
     * Builds the small {@link PointShape} indicator shown in front of the
     * tooltip text, filled with the hovered point's colour. Plus/cross shapes
     * are stroked only, mirroring how the point geometry draws them.
     */
    private static Node buildTooltipGlyph(Color fill, PointShape shape) {
        Node node = switch (shape) {
            case SQUARE ->
                new Rectangle(-4, -4, 8, 8);
            case TRIANGLE ->
                new Polygon(0, -5, 5, 4, -5, 4);
            case DIAMOND ->
                new Polygon(0, -5, 5, 0, 0, 5, -5, 0);
            case PLUS ->
                new Group(
                new Line(-4, 0, 4, 0),
                new Line(0, -4, 0, 4));
            case CROSS ->
                new Group(
                new Line(-4, -4, 4, 4),
                new Line(-4, 4, 4, -4));
            default ->
                new Circle(4);
        };
        if (node instanceof Group group) {
            for (var child : group.getChildren()) {
                var line = (Line) child;
                line.setStroke(fill);
                line.setStrokeWidth(2.0);
            }
        } else {
            var shapeNode = (Shape) node;
            shapeNode.setFill(fill);
            shapeNode.setStroke(Color.WHITE);
            shapeNode.setStrokeWidth(0.5);
        }
        StackPane glyph = new StackPane();
        glyph.setPrefSize(12, 12);
        glyph.setMinSize(12, 12);
        glyph.setMaxSize(12, 12);
        glyph.getChildren().add(node);
        return glyph;
    }
}
