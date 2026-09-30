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
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import org.jtaccuino.gog.LayerParams;
import org.jtaccuino.gog.coord.Coord;
import org.jtaccuino.gog.coord.Coord2D;
import org.jtaccuino.gog.coord.Light3d;
import org.jtaccuino.gog.facet.FacetSpec;
import org.jtaccuino.gog.labs.LabsSpec;
import org.jtaccuino.gog.layer.Layer;
import org.jtaccuino.gog.layer.LayerConfigurator;
import org.jtaccuino.gog.layer.PositionAdjust;
import org.jtaccuino.gog.layer.StackableGeom;
import org.jtaccuino.gog.layer.StatHost;
import org.jtaccuino.gog.render.TextMeasurer;
import org.jtaccuino.gog.scale.ScaleConfigurator;
import org.jtaccuino.gog.scale.ScaleSpec;
import org.jtaccuino.gog.spi.DataExtractor;
import org.jtaccuino.gog.stat.Stat;
import org.jtaccuino.gog.theme.Theme;
import org.jtaccuino.gog.theme.ThemeConfigurator;

/**
 * Mutable, fluid specification of a single plot's declarative state: the
 * dataset, the global aesthetic mapping, the registered geometry layers, the
 * coordinate system, faceting, labels, theme, guides and scales.
 * <p>
 * A descriptor is the *spec* half of the gog4j plotting model: heavier, fully
 * configurable, and embeddable. {@link Plot} is a node that renders a
 * descriptor to a JavaFX {@code Canvas} or SVG surface — the two are loosely
 * decoupled, so the same descriptor can be shown standalone, or cloned via
 * {@link #copy()} into the cells of a composite {@code PlotMatrix}.
 * <p>
 * Descriptors are created through {@link Ggplot#plot(Object, Aes)} and take
 * the same configuration surface as {@link Plot} (geoms, scales, facets,
 * labs, theme, coord, guides), plus the presentation {@link PlotOptions}.
 * <p>
 * All mutators return {@code this} for fluid chaining, matching the fluent
 * grammar of the rest of the library.
 *
 * @param <DF> the DataFrame type representing the underlying dataset
 */
public class PlotDescriptor<DF> implements ConfigTarget<DF> {

    private final DF df;
    private final DataExtractor<DF> extractor;
    private Aes aes;
    private final List<Layer<DF>> internalLayers = new ArrayList<>();
    private final Map<Layer<DF>, Aes> localAesByLayer = new HashMap<>();
    private final ScaleSpec scaleSpec = new ScaleSpec();
    private Guides guides = Guides.empty();
    private FacetSpec facetSpec = null;
    private LabsSpec labsSpec = new LabsSpec();
    private Theme theme = Theme.theme_gray();
    private Coord coord = new Coord2D();
    private Light3d light;
    private PlotOptions options = PlotOptions.defaults();
    private RenderMode renderMode = RenderMode.FULL;
    private boolean guidesOnly;
    private PanelInsets panelInsets;

    /**
     * Constructs a new descriptor holding the dataset, its extractor and the
     * global aesthetic mapping.
     *
     * @param df        the dataset (e.g., DFLib {@code DataFrame})
     * @param extractor the extractor strategy for the dataset type
     * @param aes       the global aesthetic mappings (x, y, color, fill, shape, group)
     */
    PlotDescriptor(DF df, DataExtractor<DF> extractor, Aes aes) {
        this.df = df;
        this.extractor = extractor;
        this.aes = aes;
    }

    /**
     * The dataset.
     *
     * @return the dataset element
     */
    public DF data() {
        return df;
    }

    /**
     * The extractor strategy bound to this descriptor's dataset type.
     *
     * @return the {@link DataExtractor}
     */
    public DataExtractor<DF> extractor() {
        return extractor;
    }

    /**
     * The global aesthetic mapping.
     *
     * @return the running {@link Aes}
     */
    public Aes aes() {
        return aes;
    }

    /**
     * Replaces the global aesthetic mapping wholesale.
     *
     * @param aes the new global mapping
     * @return this descriptor for fluid chaining
     */
    public PlotDescriptor<DF> aes(Aes aes) {
        this.aes = aes;
        return this;
    }

    /**
     * The registered geometry layers, in draw order.
     * <p>
     * The returned list is this descriptor's live backing list: mutate
     * callbacks handed to {@code PlotMatrix} read and write it in place.
     *
     * @return the live list of registered {@link Layer} instances
     */
    public List<Layer<DF>> geoms() {
        return internalLayers;
    }

    /**
     * The layer-local aesthetic overrides, keyed by layer.
     *
     * @return the live map of {@code Layer} to local {@link Aes}
     */
    public Map<Layer<DF>, Aes> localAesByLayer() {
        return localAesByLayer;
    }

    /**
     * The coordinate system.
     *
     * @return the active {@link Coord}
     */
    public Coord coord() {
        return coord;
    }

    /**
     * The plot-level 3D light source, filled in by {@code Plot.light(Light3d)}.
     *
     * @return the {@link Light3d}, or {@code null} when unset
     */
    public Light3d light() {
        return light;
    }

    /**
     * Sets the plot-level 3D light source, combined with (but exclusive of) a
     * coord-level {@code coord3d(light=…)}.
     *
     * @param light the {@link Light3d}, or {@code null} for none
     * @return this descriptor for fluid chaining
     */
    public PlotDescriptor<DF> light(Light3d light) {
        this.light = light;
        return this;
    }

    /**
     * The faceting specification, if configured.
     *
     * @return the {@link FacetSpec}, or {@code null} for an unfaceted plot
     */
    public FacetSpec facet() {
        return facetSpec;
    }

    /**
     * The declarative labels (title, subtitle, axis labels, caption).
     *
     * @return the {@link LabsSpec}
     */
    public LabsSpec labs() {
        return labsSpec;
    }

    /**
     * The applied theme.
     *
     * @return the active {@link Theme}
     */
    public Theme theme() {
        return theme;
    }

    /**
     * The per-scale guide registry.
     *
     * @return the {@link Guides} registry
     */
    public Guides guides() {
        return guides;
    }

    /**
     * The axis scale specification.
     *
     * @return the {@link ScaleSpec}
     */
    public ScaleSpec scaleSpec() {
        return scaleSpec;
    }

    /**
     * The presentation options controlling the axis labels and titles.
     *
     * @return the {@link PlotOptions}
     */
    public PlotOptions options() {
        return options;
    }

    /**
     * Returns a deep structural clone of this descriptor: an independent
     * geometry list, layer-local map and {@link ScaleSpec}, sharing the
     * immutable spec pieces (data, mapping, theme, labels, guides, coord,
     * options). Mutating the copy never disturbs the original — the basis of
     * the composite {@code PlotMatrix} cells.
     *
     * @return an independent copy of this descriptor
     */
    public PlotDescriptor<DF> copy() {
        var copy = new PlotDescriptor<>(df, extractor, aes);
        copy.internalLayers.addAll(internalLayers);
        copy.localAesByLayer.putAll(localAesByLayer);
        copy.scaleSpec.copyFrom(scaleSpec);
        copy.guides = guides;
        copy.facetSpec = facetSpec;
        copy.labsSpec = labsSpec;
        copy.theme = theme;
        copy.coord = coord;
        copy.options = options;
        copy.renderMode = renderMode;
        copy.guidesOnly = guidesOnly;
        copy.panelInsets = panelInsets;
        return copy;
    }

    /**
     * Sets the presentation options.
     *
     * @param options the {@link PlotOptions}
     * @return this descriptor for fluid chaining
     */
    public PlotDescriptor<DF> options(PlotOptions options) {
        this.options = options;
        return this;
    }

    /**
     * Marks this descriptor to render <em>only</em> its guides, omitting the
     * plot background, title, panels and axis titles. Used to extract a
     * standalone shared legend (e.g. the external legend band of a
     * {@code PlotMatrix}).
     *
     * @return this descriptor for fluid chaining
     */
    public PlotDescriptor<DF> guidesOnly() {
        this.guidesOnly = true;
        return this;
    }

    /**
     * Whether this descriptor renders only its guides.
     *
     * @return {@code true} when the panels and titles are omitted
     */
    public boolean isGuidesOnly() {
        return guidesOnly;
    }

    /**
     * The natural width of the band a shared legend for {@code source}'s
     * guides needs, using the same defaults as a standalone plot's legend: the
     * key swatch and key-label gap come from the default
     * {@link Guides#guideLegend() legend}, the title and key labels are
     * measured in the theme's guide fonts, and the band adds the theme's strip
     * padding, panel-to-guide gap, and optional guide-box margins — mirroring
     * the single-plot right-guide margin formula. Composite nodes
     * ({@code PlotMatrix}, {@code ComposedPlot}) use this so the guide hugs the
     * panels instead of leaving a fixed-width empty strip, without hard-coding
     * layout constants.
     *
     * @param source the descriptor carrying the shared guides
     * @return the band width in device-independent pixels
     */
    static <DF> double legendBandWidth(PlotDescriptor<DF> source) {
        var theme = source.theme();
        var legend = Guides.guideLegend();
        String column = source.aes().color() != null ? source.aes().color()
                : source.aes().fill() != null ? source.aes().fill()
                : source.aes().shape() != null ? source.aes().shape()
                : source.aes().size() != null ? source.aes().size()
                : source.aes().alpha() != null ? source.aes().alpha()
                : source.aes().linetype() != null ? source.aes().linetype()
                : null;
        double maxLabel = 0.0;
        double titleWidth = 0.0;
        if (column != null) {
            var seen = new LinkedHashSet<String>();
            for (var value : source.extractor().getColumn(source.data(), column)) {
                if (value == null || !seen.add(String.valueOf(value))) {
                    continue;
                }
                maxLabel = Math.max(maxLabel,
                        TextMeasurer.width(String.valueOf(value), theme.guideKeyFont()));
            }
            titleWidth = TextMeasurer.width(column, theme.guideTitleFont());
        }
        double contentWidth = Math.max(titleWidth, legend.keyWidth() + legend.keySpacingX() + maxLabel);
        double boxPad = theme.guideBoxColor() != null ? theme.guideBoxMargin() : 0.0;
        return contentWidth + theme.guideStripPadding() + theme.panelGuideGap() + 2 * boxPad;
    }

    /**
     * Fixed panel insets ({@code left}, {@code right}, {@code top},
     * {@code bottom}) that replace the plot's grown label margins verbatim.
     * Applied by a composite {@code PlotMatrix} under the uniform "outer edges
     * only" label frame, so every cell's panel shares an identical geometry and
     * the ticks align across rows and columns. {@code null} keeps the plot's
     * natural content-fitted margins.
     *
     * @param left the left inset, or 0
     * @param right the right inset, or 0
     * @param top the top inset, or 0
     * @param bottom the bottom inset, or 0
     * @return this descriptor for fluid chaining
     */
    public PlotDescriptor<DF> panelInsets(double left, double right, double top, double bottom) {
        this.panelInsets = new PanelInsets(left, right, top, bottom);
        return this;
    }

    /**
     * The fixed panel insets overriding this plot's label margins, or
     * {@code null} for the natural content-fitted margins.
     *
     * @return the fixed insets, or {@code null}
     */
    public PanelInsets panelInsets() {
        return panelInsets;
    }

    /**
     * The configured quality-vs-speed tradeoff for interactive rendering.
     * <p>
     * When the plot renders to a vector backend ({@code SvgDrawSurface}) the
     * effective mode is always {@link RenderMode#FULL}, so exported or zoomed
     * vector output never loses fidelity regardless of this setting.
     *
     * @return the configured {@link RenderMode}
     */
    public RenderMode renderMode() {
        return renderMode;
    }

    /**
     * Sets the quality-vs-speed tradeoff for interactive rendering.
     *
     * @param renderMode the {@link RenderMode} to apply
     * @return this descriptor for fluid chaining
     */
    public PlotDescriptor<DF> renderMode(RenderMode renderMode) {
        this.renderMode = renderMode;
        return this;
    }

    /**
     * A fixed rectangle of label-space reserved around a cell's panel. The
     * four values are applied verbatim, unlike the content-fitted grown
     * margins of a standalone plot.
     *
     * @param left the left inset
     * @param right the right inset
     * @param top the top inset
     * @param bottom the bottom inset
     */
    public record PanelInsets(double left, double right, double top, double bottom) {}

    /**
     * Replaces the geometry pipeline with the given layers, discarding any
     * previously configured geometry. Convenient when overriding the default
     * layers a matrix factory installed: the default
     * pairplot cells already carry a histogram or a correlation annotation, and
     * a {@code pairs()} variant with a different look has to swap them out
     * rather than stack on top.
     *
     * @param configurators the replacement geometry configurators
     * @return this descriptor for fluid chaining
     */
    @SafeVarargs
    @SuppressWarnings("unchecked")
    public final PlotDescriptor<DF> replaceGeoms(LayerConfigurator<? super DF>... configurators) {
        geoms().clear();
        return geoms(configurators);
    }

    /**
     * Adds one or more geometry layer configurators to the pipeline.
     *
     * @param configurators one or more geometry configurators (e.g.
     * {@code point()}, {@code line()}, {@code bar()})
     * @return this descriptor for fluid chaining
     */
    @SafeVarargs
    @SuppressWarnings("unchecked")
    public final PlotDescriptor<DF> geoms(LayerConfigurator<? super DF>... configurators) {
        for (var configurator : configurators) {
            // Allows safe configuration passthrough
            ((LayerConfigurator<DF>) configurator).configure((ConfigTarget<DF>) this);
        }
        return this;
    }

    /**
     * Adds one or more geometry layer configurators supplied as a collection,
     * for callers that assemble configurators dynamically.
     *
     * @param configurators the geometry configurators (e.g. {@code point()},
     * {@code line()}, {@code bar()})
     * @return this descriptor for fluid chaining
     */
    @SuppressWarnings("unchecked")
    public final PlotDescriptor<DF> geoms(Collection<? extends LayerConfigurator<? super DF>> configurators) {
        for (var configurator : configurators) {
            // Allows safe configuration passthrough
            ((LayerConfigurator<DF>) configurator).configure((ConfigTarget<DF>) this);
        }
        return this;
    }

    /**
     * Configures multi-panel grid faceting.
     *
     * @param facetSpec the facet layout specification (e.g.,
     * {@code Facets.wrap("col", cols)})
     * @return this descriptor for fluid chaining
     */
    public PlotDescriptor<DF> facets(FacetSpec facetSpec) {
        this.facetSpec = facetSpec;
        return this;
    }

    /**
     * Sets declarative plot labels (title, subtitle, X/Y axis labels, caption).
     *
     * @param labsSpec the label specification
     * @return this descriptor for fluid chaining
     */
    public PlotDescriptor<DF> labs(LabsSpec labsSpec) {
        this.labsSpec = labsSpec;
        return this;
    }

    /**
     * Sets the plot title using a concise label spec.
     *
     * @param title the main plot title
     * @return this descriptor for fluid chaining
     */
    public PlotDescriptor<DF> labs(String title) {
        this.labsSpec = new LabsSpec(title);
        return this;
    }

    /**
     * Sets the plot title and axis labels using a concise label spec.
     *
     * @param title  the main plot title
     * @param xLabel the X-axis label
     * @param yLabel the Y-axis label
     * @return this descriptor for fluid chaining
     */
    public PlotDescriptor<DF> labs(String title, String xLabel, String yLabel) {
        this.labsSpec = new LabsSpec(title, xLabel, yLabel);
        return this;
    }

    /**
     * Applies a predefined theme to customize the appearance.
     *
     * @param customTheme the theme instance (e.g.,
     * {@code Theme.theme_gray()}, {@code new DarkTheme()})
     * @return this descriptor for fluid chaining
     */
    public PlotDescriptor<DF> theme(Theme customTheme) {
        this.theme = customTheme;
        return this;
    }

    /**
     * Applies an inline theme modification via a lambda configurator.
     *
     * @param config a lambda configurator (e.g.,
     * {@code t -> t.plotBackground(Color.WHITE)})
     * @return this descriptor for fluid chaining
     */
    public PlotDescriptor<DF> theme(ThemeConfigurator config) {
        // 1. Create a safe, isolated derivation from the current plot theme
        var derived = Theme.derive(this.theme);

        // 2. Apply user lambda to the copy
        config.configure(derived);

        // 3. Store the mutated but completely isolated DerivedTheme back
        this.theme = derived;

        return this;
    }

    /**
     * Returns the currently applied theme.
     *
     * @return the active {@link Theme}
     */
    public Theme getTheme() {
        return theme;
    }

    /**
     * Configures axis scale breaks and limits.
     *
     * @param configurator the scale configurator
     * @return this descriptor for fluid chaining
     */
    public PlotDescriptor<DF> scales(ScaleConfigurator configurator) {
        configurator.configure(this);
        return this;
    }

    /**
     * Configures the coordinate system transformation.
     *
     * @param coord the coordinate system (e.g.,
     * {@code CoordCartesian.cartesian()} or {@code Coords.coordFlip()})
     * @return this descriptor for fluid chaining
     */
    public PlotDescriptor<DF> coord(Coord coord) {
        this.coord = coord;
        return this;
    }

    /**
     * Configures the per-scale guide registry, following the
     * {@code guides()} function.
     *
     * @param guides the registry of guide overrides and suppressions
     * @return this descriptor for fluid chaining
     */
    public PlotDescriptor<DF> guides(Guides guides) {
        this.guides = guides;
        return this;
    }

    /**
     * Merges aesthetic-to-guide mappings into the guide registry.
     *
     * @param mappings the aesthetic/guide pairs to merge into the registry
     * @return this descriptor for fluid chaining
     */
    public PlotDescriptor<DF> guides(Guides.Mapping... mappings) {
        return guides(guides.set(mappings));
    }

    /**
     * Registers a geometry layer into the internal rendering pipeline.
     *
     * @param layer the geometry layer instance to register
     * @return this descriptor for fluid chaining
     */
    @Override
    public PlotDescriptor<DF> registerInternalLayer(Layer<DF> layer) {
        this.internalLayers.add(layer);
        return this;
    }

    /**
     * Registers a geometry layer with a local aesthetic override.
     * <p>
     * The local mapping shadows the {@linkplain #aes() global mapping} on a
     * per-geom basis: aesthetics it sets override the global ones for this
     * layer only (see {@link Aes#overrideWith(Aes)}).
     *
     * @param layer    the geometry layer instance to register
     * @param localAes local aesthetic mappings overriding global mappings
     * @return this descriptor for fluid chaining
     */
    @Override
    public PlotDescriptor<DF> registerInternalLayer(Layer<DF> layer, Aes localAes) {
        this.internalLayers.add(layer);
        if (localAes != null) {
            this.localAesByLayer.put(layer, localAes);
        }
        return this;
    }

    /**
     * Registers one or more data-free annotation layers into the descriptor,
     * the gog4j counterpart of the {@code annotate()}.
     *
     * @param layers the annotation layers to register
     * @return this descriptor for fluid chaining
     */
    @SuppressWarnings({"unchecked", "varargs", "rawtypes"})
    public PlotDescriptor<DF> annotate(Layer... layers) {
        for (var layer : layers) {
            registerInternalLayer((Layer<DF>) layer);
        }
        return this;
    }

    /**
     * Assembles an all-in-one layer from a stat-consuming geometry, a stat, a
     * position adjustment, a layer-local mapping, and a params bag — the
     * gog4j counterpart of the {@code Plot#layer(...)}.
     *
     * @param geom     the stat-consuming geometry layer
     * @param stat     the statistic to run
     * @param position the position adjustment
     * @param mapping  the layer-local aesthetic mapping, merged over the global one
     * @param params   the stat config and geom aesthetic defaults
     * @return this descriptor for fluid chaining
     */
    @Override
    public PlotDescriptor<DF> layer(Layer<DF> geom, Stat<DF> stat, PositionAdjust position,
                                    Aes mapping, LayerParams params) {
        if (geom instanceof StatHost) {
            @SuppressWarnings("unchecked")
            var host = (StatHost<DF>) geom;
            host.attach(stat, position, params);
        }
        registerInternalLayer(geom, mapping);
        return this;
    }

    /**
     * Convenience overload of {@link #layer(Layer, Stat, PositionAdjust, Aes, LayerParams)}
     * with the identity position and an empty params bag.
     *
     * @param geom    the stat-consuming geometry layer
     * @param stat    the statistic to run
     * @param mapping the layer-local aesthetic mapping, merged over the global one
     * @return this descriptor for fluid chaining
     */
    public PlotDescriptor<DF> layer(Layer<DF> geom, Stat<DF> stat, Aes mapping) {
        return layer(geom, stat, new PositionAdjust.Identity(), mapping, LayerParams.empty());
    }

    /**
     * Binds a geometry with its {@linkplain Layer#defaultStat() default stat}
     * and position adjustment — the ergonomic form of
     * {@link #layer(Layer, Stat, PositionAdjust, Aes, LayerParams)}.
     *
     * @param geom    the stat-consuming geometry layer
     * @param mapping the layer-local aesthetic mapping, merged over the global one
     * @return this descriptor for fluid chaining
     */
    public PlotDescriptor<DF> layer(Layer<DF> geom, Aes mapping) {
        var stat = geom.defaultStat();
        if (geom instanceof StackableGeom) {
            return layer(geom, stat, ((StackableGeom<DF>) geom).positionAdjust(), mapping, LayerParams.empty());
        }
        return layer(geom, stat, new PositionAdjust.Identity(), mapping, LayerParams.empty());
    }

    /**
     * Binds a geometry with its {@linkplain Layer#defaultStat() default stat}
     * and no layer-local mapping (the global {@linkplain #aes() mapping} applies).
     *
     * @param geom the stat-consuming geometry layer
     * @return this descriptor for fluid chaining
     */
    public PlotDescriptor<DF> layer(Layer<DF> geom) {
        return layer(geom, (Aes) null);
    }

    /**
     * The scale specification, which scale configurators mutate in place.
     *
     * @return this descriptor's {@link ScaleSpec}
     */
    @Override
    public ScaleSpec getScaleSpec() {
        return scaleSpec;
    }
}
