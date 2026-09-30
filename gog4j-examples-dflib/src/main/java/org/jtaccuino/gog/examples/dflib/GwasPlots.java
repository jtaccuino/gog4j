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
import static org.jtaccuino.gog.Geoms.abline;
import static org.jtaccuino.gog.Geoms.hline;
import static org.jtaccuino.gog.Geoms.point;
import static org.jtaccuino.gog.Geoms.text;
import static org.jtaccuino.gog.Ggplot.ggplot;
import static org.jtaccuino.gog.labs.Labs.labs;
import static org.jtaccuino.gog.scale.Scales.scaleColorManual;
import static org.jtaccuino.gog.scale.Scales.scaleXContinuous;
import static org.jtaccuino.gog.scale.Scales.scaleYSqrt;

import java.util.List;
import javafx.scene.paint.Color;
import org.dflib.DataFrame;
import org.jtaccuino.gog.Guides;
import org.jtaccuino.gog.Plot;
import org.jtaccuino.gog.coord.Coord2D;
import org.jtaccuino.gog.dflib.data.GwasDatasets;
import org.jtaccuino.gog.sampler.meta.SampleCoord;
import org.jtaccuino.gog.sampler.meta.SampleDataset;
import org.jtaccuino.gog.sampler.meta.SampleFeature;
import org.jtaccuino.gog.sampler.meta.SampleGeom;
import org.jtaccuino.gog.sampler.meta.SamplePlot;
import org.jtaccuino.gog.sampler.meta.SampleScale;
import org.jtaccuino.gog.sampler.meta.SampleTheme;
import org.jtaccuino.gog.scale.ColorManualScale;
import org.jtaccuino.gog.scale.Scales;
import org.jtaccuino.gog.theme.Theme;

/**
 * Manhattan plots of the GIANT human height GWAS (Yengo et al., Nature 610,
 * 2022), following the recipe of the standard reference's Manhattan plot: the
 * genome laid out along one continuous axis, chromosomes distinguished by
 * alternating tone, and the strongest loci called out by name.
 *
 * @see GwasDatasets
 */
public class GwasPlots {

    /** The two tones the standard reference alternates between: {@code grey} and {@code skyblue}. */
    private static final Color GREY = Color.web("#bebebe");
    private static final Color SKYBLUE = Color.web("#87ceeb");
    private static final Color HIGHLIGHT = Color.web("#f39c12");
    private static final Color THRESHOLD = Color.web("#c0392b");

    private static final ColorManualScale CHROMOSOME_BANDS =
        Scales.scaleColorManual().color("odd", GREY).color("even", SKYBLUE);

    private static final ColorManualScale HIGHLIGHTED_BANDS =
        Scales.scaleColorManual().color("odd", GREY).color("even", SKYBLUE).color("lead", HIGHLIGHT);

    /**
     * Tick positions in {@code -log10(P)} units. They are spaced for a
     * square-root axis: close together near zero, further apart towards the
     * extreme tail, so the null band and the strongest loci are both readable.
     */
    private static final List<Double> Y_BREAKS =
            List.of(0.0, 5.0, 10.0, 25.0, 50.0, 100.0, 200.0, 300.0);

    /** Top of the y range, just above the strongest attainable value. */
    private static final double Y_MAX = 340.0;

    /** Utility class; not meant to be instantiated. */
    private GwasPlots() {
    }

    private static DataFrame manhattan() {
        // Single owner: the soft-cached frame lives on GwasDatasets only.
        return GwasDatasets.loadManhattan();
    }

    /**
     * 1. The classic Manhattan plot: every retained SNP, chromosomes in
     * alternating grey and sky blue, with the genome-wide significance
     * threshold marked.
     *
     * @return the configured plot
     */
    @SamplePlot(description = "Manhattan plot with chromosomes in alternating tones and a square-root y-axis.",
            title = "Human height GWAS — 5.4 million individuals (Yengo et al., Nature 2022)",
            dataset = SampleDataset.GWAS,
            geoms = {SampleGeom.POINT, SampleGeom.HLINE},
            coords = {SampleCoord.CARTESIAN},
            scales = {SampleScale.COLOR_MANUAL, SampleScale.X_CONTINUOUS, SampleScale.Y_SQRT},
            themes = {SampleTheme.B_W},
            features = {SampleFeature.TWO_D, SampleFeature.ANNOTATE, SampleFeature.LAYERS})
    public static Plot<DataFrame> createManhattan() {
        var layout = GwasDatasets.layout();
        return ggplot(manhattan(),
                      aes().x("BPCUM").y("NEGLOG10P").color("CHRBAND"))
                .geoms(
                    point().size(1.6).opacity(0.8),
                    hline(GwasDatasets.GENOME_WIDE_LINE)
                            .color(THRESHOLD).width(1.0).dashed()
                            .annotate("genome-wide significance, P = 5 × 10⁻⁸")
                )
                .scales(CHROMOSOME_BANDS)
                .scales(scaleXContinuous(GwasDatasets.chromosomeBreaks(), GwasDatasets.chromosomeLabels()))
                .scales(scaleYSqrt(Y_BREAKS))
                .coord(Coord2D.cartesian()
                        .xlim(0, layout.genomeLength())
                        .ylim(0, Y_MAX))
                .guides(Guides.none())
                .theme(Theme.theme_bw())
                .theme(t -> t.showXGrid(false))
                .labs(labs("Human height GWAS — 5.4 million individuals (Yengo et al., Nature 2022)",
                           "Chromosome",
                           "−log₁₀(P) — square-root axis"));
    }

    /**
     * 2. The same genome-wide view, with the published independent lead SNPs
     * highlighted and the strongest loci labelled by their nearest gene — the
     * gallery's {@code label-repulsion} step.
     *
     * @return the configured plot
     */
    @SamplePlot(description = "Manhattan plot with lead SNPs highlighted and strongest loci labelled by gene.",
            title = "Annotated Manhattan",
            dataset = SampleDataset.GWAS,
            geoms = {SampleGeom.POINT, SampleGeom.HLINE, SampleGeom.TEXT},
            coords = {SampleCoord.CARTESIAN},
            scales = {SampleScale.COLOR_MANUAL, SampleScale.X_CONTINUOUS, SampleScale.Y_SQRT},
            themes = {SampleTheme.B_W},
            features = {SampleFeature.TWO_D, SampleFeature.ANNOTATE, SampleFeature.LAYERS})
    public static Plot<DataFrame> createAnnotatedManhattan() {
        var layout = GwasDatasets.layout();
        var highlighted = GwasDatasets.highlightedLeadSnps(manhattan());
        return ggplot(manhattan(),
                      aes().x("BPCUM").y("NEGLOG10P").color("BAND").label("GENE"))
                .geoms(
                    point().size(1.6).opacity(0.8),
                    hline(GwasDatasets.GENOME_WIDE_LINE)
                            .color(THRESHOLD).width(1.0).dashed()
                            .annotate("P = 5 × 10⁻⁸"),
                    text().size(9.5).bold().color(Color.web("#1b2631")).nudge(0, -9)
                )
                .scales(HIGHLIGHTED_BANDS)
                .scales(scaleXContinuous(GwasDatasets.chromosomeBreaks(), GwasDatasets.chromosomeLabels()))
                .scales(scaleYSqrt(Y_BREAKS))
                .coord(Coord2D.cartesian()
                        .xlim(0, layout.genomeLength())
                        .ylim(0, Y_MAX))
                .guides(Guides.none())
                .theme(Theme.theme_bw())
                .theme(t -> t.showXGrid(false))
                .labs(labs(String.format("Height-associated loci — %,d independent lead SNPs highlighted,"
                                        + " strongest loci named", highlighted),
                           "Chromosome",
                           "−log₁₀(P) — square-root axis"));
    }

    /**
     * 3. A close-up of chromosome 3, where the position becomes readable in
     * megabases and the peak structure of individual loci emerges.
     *
     * @return the configured plot
     */
    @SamplePlot(description = "Close-up of chromosome 3 in megabases with lead loci highlighted and named.",
            title = "Chromosome 3 in close-up — position in megabases",
            dataset = SampleDataset.GWAS,
            geoms = {SampleGeom.POINT, SampleGeom.HLINE, SampleGeom.TEXT},
            coords = {SampleCoord.CARTESIAN},
            scales = {SampleScale.COLOR_MANUAL, SampleScale.Y_SQRT},
            themes = {SampleTheme.B_W},
            features = {SampleFeature.TWO_D, SampleFeature.ANNOTATE, SampleFeature.LAYERS})
    public static Plot<DataFrame> createChromosomeZoom() {
        var chr3 = GwasDatasets.chromosome(manhattan(), 3);
        return ggplot(chr3,
                      aes().x("MB").y("NEGLOG10P").color("BAND").label("GENE"))
                .geoms(
                    point().size(2.2).opacity(0.75),
                    hline(GwasDatasets.GENOME_WIDE_LINE)
                            .color(THRESHOLD).width(1.0).dashed()
                            .annotate("P = 5 × 10⁻⁸"),
                    text().size(9.5).bold().color(Color.web("#1b2631")).nudge(0, -9)
                )
                .scales(scaleColorManual().color("background", SKYBLUE).color("lead", HIGHLIGHT))
                .scales(scaleYSqrt(Y_BREAKS))
                .coord(Coord2D.cartesian().ylim(0, Y_MAX))
                .guides(Guides.none())
                .theme(Theme.theme_bw())
                .labs(labs("Chromosome 3 in close-up — position in megabases",
                           "Position on chromosome 3 (Mb)",
                           "−log₁₀(P) — square-root axis"));
    }

    /**
     * 4. The companion quantile-quantile plot. The early departure from the
     * diagonal is the signature of a highly polygenic trait: height draws
     * signal from thousands of loci across the genome.
     *
     * @return the configured plot
     */
    @SamplePlot(description = "Quantile-quantile plot of observed versus expected -log10(P) values with a reference diagonal.",
            title = "Quantile-quantile plot — observed versus expected under the null",
            dataset = SampleDataset.GWAS,
            geoms = {SampleGeom.POINT, SampleGeom.ABLINE},
            coords = {SampleCoord.CARTESIAN},
            scales = {SampleScale.COLOR_MANUAL, SampleScale.Y_SQRT},
            themes = {SampleTheme.B_W},
            features = {SampleFeature.TWO_D, SampleFeature.LAYERS})
    public static Plot<DataFrame> createQqPlot() {
        var qq = GwasDatasets.qqPlot(manhattan());
        return ggplot(qq,
                      aes().x("EXPECTED").y("OBSERVED").color("BAND"))
                .geoms(
                    point().size(1.8).opacity(0.7),
                    abline(1.0, 0.0).color(Color.web("#7f8c8d")).dashed()
                )
                .scales(scaleColorManual().color("null", Color.web("#95a5a6")).color("significant", SKYBLUE))
                .scales(scaleYSqrt(Y_BREAKS))
                .coord(Coord2D.cartesian().ylim(0, Y_MAX))
                .guides(Guides.none())
                .theme(Theme.theme_bw())
                .labs(labs("Quantile-quantile plot — observed versus expected under the null",
                           "Expected −log₁₀(P)",
                           "Observed −log₁₀(P), square-root axis — the plateau is the double-precision floor"));
    }
}
