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
package org.jtaccuino.gog.examples.dflib;

import static org.jtaccuino.gog.Aes.aes;
import static org.jtaccuino.gog.Aesthetic.COLOR;
import static org.jtaccuino.gog.Aesthetic.FILL;
import static org.jtaccuino.gog.Geoms.boxplot;
import static org.jtaccuino.gog.Geoms.jitter;
import static org.jtaccuino.gog.Geoms.point;
import static org.jtaccuino.gog.Geoms.tile;
import static org.jtaccuino.gog.Ggplot.ggplot;
import static org.jtaccuino.gog.Guides.guide;
import static org.jtaccuino.gog.Guides.guideColorbar;
import static org.jtaccuino.gog.Guides.guideLegend;
import static org.jtaccuino.gog.labs.Labs.labs;

import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import org.dflib.DataFrame;
import org.dflib.Series;
import org.jtaccuino.gog.Aesthetic;
import org.jtaccuino.gog.Plot;
import org.jtaccuino.gog.dflib.data.DiamondsDatasets;
import org.jtaccuino.gog.dflib.data.FaithfulDatasets;
import org.jtaccuino.gog.dflib.data.MpgDatasets;
import org.jtaccuino.gog.guide.Guide.Direction;
import org.jtaccuino.gog.guide.GuideTheme;
import org.jtaccuino.gog.guide.SizeLegendStyle;
import org.jtaccuino.gog.layer.Position;
import org.jtaccuino.gog.sampler.meta.SampleDataset;
import org.jtaccuino.gog.sampler.meta.SampleFeature;
import org.jtaccuino.gog.sampler.meta.SampleGeom;
import org.jtaccuino.gog.sampler.meta.SampleGuide;
import org.jtaccuino.gog.sampler.meta.SamplePlot;
import org.jtaccuino.gog.sampler.meta.SamplePosition;
import org.jtaccuino.gog.theme.GuideOverflow;
import org.jtaccuino.gog.theme.GuidePosition;

/**
 * Example plots demonstrating the guide subsystem: legend key grids
 * ({@code nrow}/{@code ncol}/{@code byrow}), horizontal and reversed legends,
 * per-side placement ({@code theme(GuidePosition = ...)}), the optional
 * legend box, and multi-guide stacks.
 */
public class GuidePlots {

    /** Utility class; not meant to be instantiated. */
    private GuidePlots() {
    }

    /** {@return an mpg jitter plot with a two-column legend} */
    @SamplePlot(description = "Jittered points colored by class with the legend arranged in two columns.",
            title = "Two-Column Legend (ncol = 2)",
            dataset = SampleDataset.MPG,
            geoms = {SampleGeom.JITTER},
            guides = {SampleGuide.TWO_COLUMN},
            features = {SampleFeature.TWO_D})
    public static Plot<DataFrame> createTwoColumnLegend() {
        var df = MpgDatasets.loadMpg();
        return ggplot(df, aes().x("displ").y("hwy").color("class"))
                .geoms(jitter().size(4.0).opacity(0.7))
                .guides(guide(COLOR, guideLegend().ncol(2)))
                .labs(labs("1. Two-Column Legend (ncol = 2)", "engine displacement", "highway mpg")
                        .legendTitle("Vehicle class"));
    }

    /** {@return an mpg jitter plot with a two-column legend filled row by row} */
    @SamplePlot(description = "Jittered points with the two-column legend filled row by row.",
            title = "Two-Column Legend (ncol = 2, byrow = TRUE)",
            dataset = SampleDataset.MPG,
            geoms = {SampleGeom.JITTER},
            guides = {SampleGuide.TWO_COLUMN, SampleGuide.BY_ROWS},
            features = {SampleFeature.TWO_D})
    public static Plot<DataFrame> createByRowLegend() {
        var df = MpgDatasets.loadMpg();
        return ggplot(df, aes().x("displ").y("hwy").color("class"))
                .geoms(jitter().size(4.0).opacity(0.7))
                .guides(guide(COLOR, guideLegend().ncol(2).byrow(true)))
                .labs(labs("2. Two-Column Legend (ncol = 2, byrow = TRUE)", "engine displacement", "highway mpg")
                        .legendTitle("Vehicle class"));
    }

    /** {@return an mpg jitter plot with a reversed legend} */
    @SamplePlot(description = "Jittered points with the legend keys ordered in reverse.",
            title = "Reversed Legend (reverse = TRUE)",
            dataset = SampleDataset.MPG,
            geoms = {SampleGeom.JITTER},
            guides = {SampleGuide.REVERSED},
            features = {SampleFeature.TWO_D})
    public static Plot<DataFrame> createReversedLegend() {
        var df = MpgDatasets.loadMpg();
        return ggplot(df, aes().x("displ").y("hwy").color("class"))
                .geoms(jitter().size(4.0).opacity(0.7))
                .guides(guide(COLOR, guideLegend().reverse(true)))
                .labs(labs("3. Reversed Legend (reverse = TRUE)", "engine displacement", "highway mpg")
                        .legendTitle("Vehicle class"));
    }

    /** {@return a diamonds scatter plot with the legend on top} */
    @SamplePlot(description = "Diamonds scatter with the horizontal legend placed above the panel.",
            title = "Legend on Top (GuidePosition = 'top')",
            dataset = SampleDataset.DIAMONDS,
            geoms = {SampleGeom.POINT},
            guides = {SampleGuide.TOP},
            features = {SampleFeature.TWO_D})
    public static Plot<DataFrame> createTopLegend() {
        var df = DiamondsDatasets.loadDiamonds();
        return ggplot(df, aes().x("carat").y("price").color("cut"))
                .geoms(point().size(2.0))
                .guides(guide(COLOR, guideLegend().direction(Direction.HORIZONTAL)))
                .theme(t -> t.guidePosition(GuidePosition.TOP))
                .labs(labs("4. Legend on Top (GuidePosition = 'top')", "carat", "price")
                        .legendTitle("Cut"));
    }

    /** {@return a diamonds scatter plot with the legend on the bottom} */
    @SamplePlot(description = "Diamonds scatter with the horizontal legend placed below the panel.",
            title = "Legend on Bottom (GuidePosition = 'bottom')",
            dataset = SampleDataset.DIAMONDS,
            geoms = {SampleGeom.POINT},
            guides = {SampleGuide.BOTTOM},
            features = {SampleFeature.TWO_D})
    public static Plot<DataFrame> createBottomLegend() {
        var df = DiamondsDatasets.loadDiamonds();
        return ggplot(df, aes().x("carat").y("price").color("cut"))
                .geoms(point().size(2.0))
                .guides(guide(COLOR, guideLegend().direction(Direction.HORIZONTAL)))
                .theme(t -> t.guidePosition(GuidePosition.BOTTOM))
                .labs(labs("5. Legend on Bottom (GuidePosition = 'bottom')", "carat", "price")
                        .legendTitle("Cut"));
    }

    /** {@return a diamonds scatter plot with the legend on the left} */
    @SamplePlot(description = "Diamonds scatter with the legend placed to the left of the panel.",
            title = "Legend on the Left (GuidePosition = 'left')",
            dataset = SampleDataset.DIAMONDS,
            geoms = {SampleGeom.POINT},
            guides = {SampleGuide.LEFT},
            features = {SampleFeature.TWO_D})
    public static Plot<DataFrame> createLeftLegend() {
        var df = DiamondsDatasets.loadDiamonds();
        return ggplot(df, aes().x("carat").y("price").color("cut"))
                .geoms(point().size(2.0))
                .theme(t -> t.guidePosition(GuidePosition.LEFT))
                .labs(labs("6. Legend on the Left (GuidePosition = 'left')", "carat", "price")
                        .legendTitle("Cut"));
    }

    /** {@return an mpg jitter plot with a boxed legend} */
    @SamplePlot(description = "Jittered points with the legend drawn inside a boxed background.",
            title = "Legend Box (legend.background)",
            dataset = SampleDataset.MPG,
            geoms = {SampleGeom.JITTER},
            guides = {SampleGuide.TOP, SampleGuide.BOXED},
            features = {SampleFeature.TWO_D})
    public static Plot<DataFrame> createLegendBox() {
        var df = MpgDatasets.loadMpg();
        return ggplot(df, aes().x("displ").y("hwy").color("class"))
                .geoms(jitter().size(4.0).opacity(0.7))
                .guides(guide(COLOR, guideLegend().direction(Direction.HORIZONTAL)))
                .theme(t -> t.guidePosition(GuidePosition.TOP)
                        .guideBoxColor(Color.web("#f5f5f5"))
                        .guideBoxMargin(10.0))
                .labs(labs("7. Legend Box (legend.background)", "engine displacement", "highway mpg")
                        .legendTitle("Vehicle class"));
    }

    /** {@return an mpg boxplot-and-jitter plot with two stacked guides} */
    @SamplePlot(description = "Dodged boxplots and jittered points with stacked colour and fill guides.",
            title = "Two Guides Stacked (fill + colour)",
            dataset = SampleDataset.MPG,
            geoms = {SampleGeom.BOXPLOT, SampleGeom.JITTER},
            positions = {SamplePosition.DODGE},
            guides = {SampleGuide.STACKED},
            features = {SampleFeature.TWO_D})
    public static Plot<DataFrame> createMultiGuide() {
        var df = MpgDatasets.loadMpg();
        return ggplot(df, aes().x("class").y("hwy").color("class").fill("drv"))
                .geoms(
                        boxplot().position(Position.DODGE).dodgeWidth(0.9).widthFactor(0.6),
                        jitter().size(3.0).opacity(0.5)
                )
                .theme(t -> t.guideBoxColor(Color.WHITE).guideBoxMargin(8.0))
                .labs(labs("8. Two Guides Stacked (fill + colour)", "vehicle class", "highway mpg")
                        .legendTitle("Drive type"));
    }

    /** {@return a faithful heatmap with a horizontal colourbar} */
    @SamplePlot(description = "Density tile plot with a horizontal colourbar above the panel.",
            title = "Horizontal Colourbar on Top",
            dataset = SampleDataset.FAITHFUL,
            geoms = {SampleGeom.TILE},
            guides = {SampleGuide.COLORBAR, SampleGuide.TOP},
            features = {SampleFeature.TWO_D})
    public static Plot<DataFrame> createHorizontalColorbar() {
        var df = FaithfulDatasets.loadFaithfuld();
        return ggplot(df, aes().x("waiting").y("eruptions").fill("density"))
                .geoms(tile("plasma"))
                .guides(guide(FILL, guideColorbar().direction(Direction.HORIZONTAL)))
                .theme(t -> t.guidePosition(GuidePosition.TOP))
                .labs(labs("9. Horizontal Colourbar on Top", "waiting time (minutes)", "eruption duration (minutes)")
                        .legendTitle("Density"));
    }

    /** {@return an mpg jitter plot with a shape-only legend} */
    @SamplePlot(description = "Jittered points mapped to shapes with a shape-only legend.",
            title = "Shape-Only Legend",
            dataset = SampleDataset.MPG,
            geoms = {SampleGeom.POINT},
            guides = {SampleGuide.KEY_SHAPE},
            features = {SampleFeature.TWO_D})
    public static Plot<DataFrame> createShapeOnlyLegend() {
        var df = MpgDatasets.loadMpg();
        return ggplot(df, aes().x("displ").y("hwy").shape("drv"))
                .geoms(point().size(4.0).opacity(0.7))
                .labs(labs("10. Shape-Only Legend", "engine displacement", "highway mpg")
                        .legendTitle(Aesthetic.SHAPE, "Drive type"));
    }

    /** {@return an mpg jitter plot with a merged colour-and-shape legend} */
    @SamplePlot(description = "Jittered points with colour and shape merged into one legend.",
            title = "Merged Colour + Shape Legend",
            dataset = SampleDataset.MPG,
            geoms = {SampleGeom.POINT},
            guides = {SampleGuide.MERGED},
            features = {SampleFeature.TWO_D})
    public static Plot<DataFrame> createMergedColorShapeLegend() {
        var df = MpgDatasets.loadMpg();
        return ggplot(df, aes().x("displ").y("hwy").color("drv").shape("drv"))
                .geoms(point().size(4.0).opacity(0.7))
                .labs(labs("11. Merged Colour + Shape Legend", "engine displacement", "highway mpg")
                        .legendTitle("Drive type"));
    }

    /** {@return an mpg plot splitting the fill and colour guides across sides} */
    @SamplePlot(description = "Dodged boxplots and jitter with colour and fill guides split across sides.",
            title = "Split Placement (fill right, colour bottom)",
            dataset = SampleDataset.MPG,
            geoms = {SampleGeom.BOXPLOT, SampleGeom.JITTER},
            positions = {SamplePosition.DODGE},
            guides = {SampleGuide.BOTTOM, SampleGuide.SEPARATE},
            features = {SampleFeature.TWO_D})
    public static Plot<DataFrame> createSplitPlacement() {
        var df = MpgDatasets.loadMpg();
        return ggplot(df, aes().x("class").y("hwy").color("class").fill("drv"))
                .geoms(
                        boxplot().position(Position.DODGE).dodgeWidth(0.9).widthFactor(0.6),
                        jitter().size(3.0).opacity(0.5)
                )
                .guides(guide(COLOR, guideLegend().position(GuidePosition.BOTTOM)))
                .labs(labs("12. Split Placement (fill right, colour bottom)", "vehicle class", "highway mpg")
                        .legendTitle(Aesthetic.FILL, "Fill: drive type"));
    }

    /** {@return an mpg jitter plot with the legend inside the panel} */
    @SamplePlot(description = "Jittered points with the boxed legend floating inside the panel.",
            title = "Legend Inside the Panel",
            dataset = SampleDataset.MPG,
            geoms = {SampleGeom.JITTER},
            guides = {SampleGuide.INSIDE},
            features = {SampleFeature.TWO_D})
    public static Plot<DataFrame> createInsidePlacement() {
        var df = MpgDatasets.loadMpg();
        return ggplot(df, aes().x("displ").y("hwy").color("class"))
                .geoms(jitter().size(4.0).opacity(0.7))
                .theme(t -> t.guidePosition(GuidePosition.INSIDE)
                        .guideBoxColor(Color.web("#ffffff"))
                        .guideBoxMargin(8.0))
                .labs(labs("13. Legend Inside the Panel", "engine displacement", "highway mpg")
                        .legendTitle("Vehicle class"));
    }

    /** {@return an mpg jitter plot with per-aesthetic guide titles} */
    @SamplePlot(description = "Jittered points with a distinct title for each aesthetic's guide.",
            title = "Per-Aesthetic Guide Titles",
            dataset = SampleDataset.MPG,
            geoms = {SampleGeom.POINT},
            guides = {SampleGuide.PER_AESTHETIC},
            features = {SampleFeature.TWO_D})
    public static Plot<DataFrame> createPerAestheticTitles() {
        var df = MpgDatasets.loadMpg();
        return ggplot(df, aes().x("displ").y("hwy").color("drv").shape("cyl"))
                .geoms(point().size(4.0).opacity(0.7))
                .labs(labs("14. Per-Aesthetic Guide Titles", "engine displacement", "highway mpg")
                        .legendTitle("Aesthetic")
                        .legendTitle(Aesthetic.COLOR, "Drive type")
                        .legendTitle(Aesthetic.SHAPE, "Cylinders"));
    }

    /** {@return an mpg jitter plot with a size-only legend} */
    @SamplePlot(description = "Jittered points mapped to size with a size-only legend.",
            title = "Size-Only Legend",
            dataset = SampleDataset.MPG,
            geoms = {SampleGeom.POINT},
            guides = {SampleGuide.KEY_SIZE},
            features = {SampleFeature.TWO_D})
    public static Plot<DataFrame> createSizeOnlyLegend() {
        var df = MpgDatasets.loadMpg();
        return ggplot(df, aes().x("displ").y("hwy").size("cyl"))
                .geoms(point().opacity(0.7))
                .labs(labs("15. Size-Only Legend", "engine displacement", "highway mpg")
                        .legendTitle(Aesthetic.SIZE, "Cylinders"));
    }

    /** {@return an mpg jitter plot with colour and size legends} */
    @SamplePlot(description = "Jittered points with separate colour and size legends.",
            title = "Colour + Size Legends",
            dataset = SampleDataset.MPG,
            geoms = {SampleGeom.POINT},
            guides = {SampleGuide.KEY_COLOUR, SampleGuide.KEY_SIZE},
            features = {SampleFeature.TWO_D})
    public static Plot<DataFrame> createColorAndSize() {
        var df = MpgDatasets.loadMpg();
        return ggplot(df, aes().x("displ").y("hwy").color("class").size("cyl"))
                .geoms(point().opacity(0.7))
                .labs(labs("16. Colour + Size Legends", "engine displacement", "highway mpg")
                        .legendTitle(Aesthetic.COLOR, "Class")
                        .legendTitle(Aesthetic.SIZE, "Cylinders"));
    }

    /**
     * 17. A themed legend — {@code GuideTheme} overrides the title and key
     * styling of one guide without touching the theme for anything else.
     *
     * @return the rendered themed-legend example plot
     */
    @SamplePlot(description = "Jittered points with a legend restyled through per-guide theme overrides.",
            title = "Themed Legend (per-guide overrides)",
            dataset = SampleDataset.MPG,
            geoms = {SampleGeom.JITTER},
            guides = {SampleGuide.THEMED},
            features = {SampleFeature.TWO_D})
    public static Plot<DataFrame> createThemedLegend() {
        var df = MpgDatasets.loadMpg();
        var titleFont = javafx.scene.text.Font.font("System", javafx.scene.text.FontWeight.BOLD, 13);
        return ggplot(df, aes().x("displ").y("hwy").color("class"))
                .geoms(jitter().size(4.0).opacity(0.7))
                .guides(guide(COLOR, guideLegend()
                        .theme(GuideTheme.title(Color.web("#8b0000"), titleFont)
                                .withKeys(Color.web("#003366"), null))))
                .labs(labs("17. Themed Legend (per-guide overrides)", "engine displacement", "highway mpg")
                        .legendTitle("Vehicle class"));
    }

    /**
     * 18. A merged legend — colour maps the quality rating while shape maps
     * the reliability rating; both columns share the same category labels, so
     * the merge rule folds them into one legend of coloured shapes.
     *
     * @return the rendered merged-legend example plot
     */
    @SamplePlot(description = "Synthetic scatter merging colour and shape legends with matching labels.",
            title = "Merged Legend (matching labels)",
            dataset = SampleDataset.SYNTHETIC,
            geoms = {SampleGeom.POINT},
            guides = {SampleGuide.MERGED},
            features = {SampleFeature.TWO_D})
    public static Plot<DataFrame> createMergedRatingsLegend() {
        var index = org.dflib.Series.ofDouble(2.0, 3.5, 5.0, 2.8, 4.1, 5.5, 3.2, 4.6, 6.0);
        var score = org.dflib.Series.ofDouble(4.0, 1.5, 3.0, 5.2, 2.4, 4.8, 1.9, 3.7, 5.6);
        var quality = org.dflib.Series.of("High", "Low", "Medium", "Medium", "High", "Low",
                "Low", "Medium", "High");
        var reliability = org.dflib.Series.of("Low", "High", "Medium", "High", "Low", "Medium",
                "Medium", "High", "Low");
        var df = DataFrame.byColumn("index", "score", "quality", "reliability")
                .of(index, score, quality, reliability);
        return ggplot(df, aes().x("index").y("score").color("quality").shape("reliability"))
                .geoms(point().size(5.0))
                .labs(labs("18. Merged Legend (matching labels)", "index score", "score")
                        .legendTitle(Aesthetic.COLOR, "Quality = Reliability"));
    }

    /**
     * 19. An anchored inside legend — {@code inside(x, y)} pins the floating
     * legend to the panel's top-left corner instead of the centered default.
     *
     * @return the rendered anchored-inside-legend example plot
     */
    @SamplePlot(description = "Jittered points with the inside legend anchored to the top-left corner.",
            title = "Inside Legend, Anchored Top-Left",
            dataset = SampleDataset.MPG,
            geoms = {SampleGeom.JITTER},
            guides = {SampleGuide.INSIDE, SampleGuide.ANCHORED},
            features = {SampleFeature.TWO_D})
    public static Plot<DataFrame> createAnchoredInsideLegend() {
        var df = MpgDatasets.loadMpg();
        return ggplot(df, aes().x("displ").y("hwy").color("class"))
                .geoms(jitter().size(4.0).opacity(0.7))
                .theme(t -> t.guidePosition(GuidePosition.INSIDE)
                        .guideBoxColor(Color.WHITE)
                        .guideBoxMargin(8.0))
                .guides(guide(COLOR, guideLegend().inside(0.10, 0.20)))
                .labs(labs("19. Inside Legend, Anchored Top-Left", "engine displacement", "highway mpg")
                        .legendTitle("Vehicle class"));
    }

    /**
     * 20. A continuous alpha mapping — carat maps to transparency, producing
     * a gradient bar that ramps from transparent to opaque red.
     *
     * @return the rendered alpha-guide example plot
     */
    @SamplePlot(description = "Diamonds scatter with point transparency mapped to carat.",
            title = "Alpha Guide (carat → opacity)",
            dataset = SampleDataset.DIAMONDS,
            geoms = {SampleGeom.POINT},
            guides = {SampleGuide.KEY_ALPHA},
            features = {SampleFeature.TWO_D, SampleFeature.ALPHA})
    public static Plot<DataFrame> createAlphaGuide() {
        var df = DiamondsDatasets.loadDiamonds();
        return ggplot(df, aes().x("carat").y("price").alpha("carat"))
                .geoms(point().opacity(1.0))
                .labs(labs("20. Alpha Guide (carat → opacity)", "carat", "price"));
    }

    /**
     * 21. Alpha + colour — both cut and carat are mapped; the alpha guide
     * shows one gradient bar per cut class, each tinted in that class's
     * fill colour (conventional merge semantics).
     *
     * @return the rendered alpha-plus-colour example plot
     */
    @SamplePlot(description = "Diamonds scatter with per-cut alpha bars tinted by the colour guide.",
            title = "Alpha + Colour Guide",
            dataset = SampleDataset.DIAMONDS,
            geoms = {SampleGeom.POINT},
            guides = {SampleGuide.KEY_COLOUR, SampleGuide.KEY_ALPHA},
            features = {SampleFeature.TWO_D, SampleFeature.ALPHA})
    public static Plot<DataFrame> createAlphaWithColourGuide() {
        var df = DiamondsDatasets.loadDiamonds();
        return ggplot(df, aes().x("carat").y("price").color("cut").alpha("carat"))
                .geoms(point().opacity(1.0))
                .labs(labs("21. Alpha + Colour Guide", "carat", "price")
                        .legendTitle(Aesthetic.COLOR, "Cut"));
    }

    /**
     * 22. Overflow to the bottom — five per-cut alpha bars (with the colour
     * legend) would overshoot the 560px canvas on the right; the default
     * {@link GuideOverflow#FLOW_TO_BOTTOM} policy folds the whole strip onto
     * the bottom, shrinking the panel to make room.
     *
     * @return the rendered overflow-to-bottom example plot
     */
    @SamplePlot(description = "Colour and alpha guides overflowing to the bottom of the plot.",
            title = "Overflow → Bottom",
            dataset = SampleDataset.DIAMONDS,
            geoms = {SampleGeom.POINT},
            guides = {SampleGuide.OVERFLOW_BOTTOM},
            features = {SampleFeature.TWO_D, SampleFeature.ALPHA})
    public static Plot<DataFrame> createOverflowToBottom() {
        var df = DiamondsDatasets.loadDiamonds();
        return ggplot(df, aes().x("carat").y("price").color("cut").alpha("carat"))
                .geoms(point().opacity(1.0))
                .theme(t -> t.guideOverflow(GuideOverflow.FLOW_TO_BOTTOM))
                .labs(labs("22. Overflow → Bottom", "carat", "price")
                        .legendTitle(Aesthetic.COLOR, "Cut"));
    }

    /**
     * 23. Hybrid overflow — the guides that individually fit stay on the
     * right; the rest of the strip folds onto the bottom.
     *
     * @return the rendered hybrid-overflow example plot
     */
    @SamplePlot(description = "Fitting guides stay right while the rest of the strip folds to the bottom.",
            title = "Overflow → Hybrid",
            dataset = SampleDataset.DIAMONDS,
            geoms = {SampleGeom.POINT},
            guides = {SampleGuide.OVERFLOW_HYBRID},
            features = {SampleFeature.TWO_D, SampleFeature.ALPHA})
    public static Plot<DataFrame> createHybridOverflow() {
        var df = DiamondsDatasets.loadDiamonds();
        return ggplot(df, aes().x("carat").y("price").color("cut").alpha("carat"))
                .geoms(point().opacity(1.0))
                .theme(t -> t.guideOverflow(GuideOverflow.HYBRID))
                .labs(labs("23. Overflow → Hybrid", "carat", "price")
                        .legendTitle(Aesthetic.COLOR, "Cut"));
    }

    /**
     * 24. Uniform scale — the strip that cannot fit is shrunk as a whole,
     * labels and all, so everything (tiny) still renders on the right.
     *
     * @return the rendered uniform-scaled-overflow example plot
     */
    @SamplePlot(description = "The whole guide strip shrinks uniformly to fit on the right.",
            title = "Overflow → Uniform Scaled",
            dataset = SampleDataset.DIAMONDS,
            geoms = {SampleGeom.POINT},
            guides = {SampleGuide.OVERFLOW_UNIFORM},
            features = {SampleFeature.TWO_D, SampleFeature.ALPHA})
    public static Plot<DataFrame> createUniformScaledOverflow() {
        var df = DiamondsDatasets.loadDiamonds();
        return ggplot(df, aes().x("carat").y("price").color("cut").alpha("carat"))
                .geoms(point().opacity(1.0))
                .theme(t -> t.guideOverflow(GuideOverflow.SCALE_UNIFORM))
                .labs(labs("24. Overflow → Uniform Scaled", "carat", "price")
                        .legendTitle(Aesthetic.COLOR, "Cut"));
    }

    /**
     * 25. Geometry scale — geometry shrinks to fit while the labels keep
     * their full size, so text stays legible and the bars compress.
     *
     * @return the rendered geometry-scaled-overflow example plot
     */
    @SamplePlot(description = "Guide keys shrink while labels keep full size on the right.",
            title = "Overflow → Geometry Scaled",
            dataset = SampleDataset.DIAMONDS,
            geoms = {SampleGeom.POINT},
            guides = {SampleGuide.OVERFLOW_GEOMETRY},
            features = {SampleFeature.TWO_D, SampleFeature.ALPHA})
    public static Plot<DataFrame> createGeometryScaledOverflow() {
        var df = DiamondsDatasets.loadDiamonds();
        return ggplot(df, aes().x("carat").y("price").color("cut").alpha("carat"))
                .geoms(point().opacity(1.0))
                .theme(t -> t.guideOverflow(GuideOverflow.SCALE_GEOMETRY))
                .labs(labs("25. Overflow → Geometry Scaled", "carat", "price")
                        .legendTitle(Aesthetic.COLOR, "Cut"));
    }

    /**
     * 26. Separated size legend — the default size legend nests its circles
     * concentrically (as in guide 15); {@code SizeLegendStyle.SEPARATED}
     * instead gives every key its own row sized to its own diameter, so the
     * circles never overlap and grow down the column.
     *
     * @return the rendered separated-size-legend example plot
     */
    @SamplePlot(description = "Jittered points with each size key placed on its own row.",
            title = "Separated Size Legend",
            dataset = SampleDataset.MPG,
            geoms = {SampleGeom.POINT},
            guides = {SampleGuide.KEY_SIZE},
            features = {SampleFeature.TWO_D})
    public static Plot<DataFrame> createSeparatedSizeLegend() {
        var df = MpgDatasets.loadMpg();
        return ggplot(df, aes().x("displ").y("hwy").size("cyl"))
                .geoms(point().opacity(0.7))
                .guides(guide(Aesthetic.SIZE, guideLegend().sizeStyle(SizeLegendStyle.SEPARATED)))
                .labs(labs("26. Separated Size Legend", "engine displacement", "highway mpg")
                        .legendTitle(Aesthetic.SIZE, "Cylinders"));
    }
}
