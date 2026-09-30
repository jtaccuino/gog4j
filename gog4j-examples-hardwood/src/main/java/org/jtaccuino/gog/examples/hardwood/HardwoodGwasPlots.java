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
package org.jtaccuino.gog.examples.hardwood;

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
import org.jtaccuino.gog.Guides;
import org.jtaccuino.gog.Plot;
import org.jtaccuino.gog.coord.Coord2D;
import org.jtaccuino.gog.hardwood.HardwoodDataFrame;
import org.jtaccuino.gog.hardwood.data.HardwoodGwasDatasets;
import org.jtaccuino.gog.sampler.meta.SampleCoord;
import org.jtaccuino.gog.sampler.meta.SampleDataset;
import org.jtaccuino.gog.sampler.meta.SampleFeature;
import org.jtaccuino.gog.sampler.meta.SampleGeom;
import org.jtaccuino.gog.sampler.meta.SamplePlot;
import org.jtaccuino.gog.sampler.meta.SampleScale;
import org.jtaccuino.gog.sampler.meta.SampleTheme;
import org.jtaccuino.gog.theme.Theme;

/**
 * The human height GWAS as Manhattan and quantile-quantile plots, read from a
 * single ZSTD Parquet file through the Hardwood backend.
 * <p>
 * These mirror the DFLib GWAS examples: the Manhattan plot reads 467,159 SNPs,
 * alternates chromosome tone, highlights the lead SNPs and names the strongest
 * loci; the QQ plot shows the departure from the null expectation characteristic
 * of a polygenic trait.
 */
public class HardwoodGwasPlots {

    private static final Color THRESHOLD = Color.web("#c0392b");
    private static final Color GREY = Color.web("#bdbdbd");
    private static final Color SKYBLUE = Color.web("#56b4e9");
    private static final Color HIGHLIGHT = Color.web("#d62728");
    private static final Color NULL_BAND = Color.web("#7f8c8d");
    private static final Color SIGNIFICANT = Color.web("#c0392b");
    private static final double Y_MAX = 340.0;
    private static final List<Double> Y_BREAKS =
            List.of(0.0, 5.0, 10.0, 25.0, 50.0, 100.0, 200.0, 300.0);

    /** Utility class; not meant to be instantiated. */
    private HardwoodGwasPlots() {
    }

    /**
     * The large, expressive example: a Manhattan plot of 467,159 SNPs read from
     * a single ZSTD Parquet file. Chromosomes alternate in tone, the lead SNPs
     * are highlighted, and the strongest loci are named — all from the Parquet
     * frame, mirroring the DFLib GWAS example.
     *
     * @return the configured plot
     */
    @SamplePlot(description = "Manhattan plot of 467,159 SNPs read from Parquet through Hardwood.",
            title = "Hardwood: Human height GWAS (Parquet)",
            dataset = SampleDataset.GWAS,
            geoms = {SampleGeom.POINT, SampleGeom.HLINE, SampleGeom.TEXT},
            coords = {SampleCoord.CARTESIAN},
            scales = {SampleScale.COLOR_MANUAL, SampleScale.X_CONTINUOUS, SampleScale.Y_SQRT},
            themes = {SampleTheme.B_W},
            features = {SampleFeature.TWO_D, SampleFeature.ANNOTATE, SampleFeature.LAYERS})
    public static Plot<HardwoodDataFrame> createManhattan() {
        var layout = HardwoodGwasDatasets.layout();
        var highlighted = HardwoodGwasDatasets.highlightedLeadSnps(
                HardwoodGwasDatasets.loadManhattan());
        return ggplot(HardwoodGwasDatasets.loadManhattan(),
                      aes().x("BPCUM").y("NEGLOG10P").color("BAND").label("GENE"))
                .geoms(
                    point().size(1.6).opacity(0.8),
                    hline(HardwoodGwasDatasets.GENOME_WIDE_LINE)
                            .color(THRESHOLD).width(1.0).dashed()
                            .annotate("P = 5 × 10⁻⁸"),
                    text().size(9.5).bold().color(Color.web("#1b2631")).nudge(0, -9)
                )
                .scales(scaleColorManual()
                        .color("odd", GREY).color("even", SKYBLUE).color("lead", HIGHLIGHT))
                .scales(scaleXContinuous(HardwoodGwasDatasets.chromosomeBreaks(),
                        HardwoodGwasDatasets.chromosomeLabels()))
                .scales(scaleYSqrt(Y_BREAKS))
                .coord(Coord2D.cartesian()
                        .xlim(0, layout.genomeLength())
                        .ylim(0, Y_MAX))
                .guides(Guides.none())
                .theme(Theme.theme_bw())
                .theme(t -> t.showXGrid(false))
                .labs(labs(String.format("Hardwood GWAS from Parquet — %,d independent lead SNPs highlighted",
                                highlighted),
                        "Chromosome",
                        "−log₁₀(P) — square-root axis"));
    }

    /**
     * The companion quantile-quantile plot from the Parquet frame, showing the
     * departure from the null expectation characteristic of a polygenic trait.
     *
     * @return the configured plot
     */
    @SamplePlot(description = "Quantile-quantile plot read from Parquet through Hardwood.",
            title = "Hardwood: GWAS QQ plot (Parquet)",
            dataset = SampleDataset.GWAS,
            geoms = {SampleGeom.POINT, SampleGeom.ABLINE},
            coords = {SampleCoord.CARTESIAN},
            scales = {SampleScale.COLOR_MANUAL, SampleScale.Y_SQRT},
            themes = {SampleTheme.B_W},
            features = {SampleFeature.TWO_D, SampleFeature.LAYERS})
    public static Plot<HardwoodDataFrame> createQqPlot() {
        var qq = HardwoodGwasDatasets.qqPlot(HardwoodGwasDatasets.loadManhattan());
        return ggplot(qq, aes().x("EXPECTED").y("OBSERVED").color("BAND"))
                .geoms(
                    point().size(1.8).opacity(0.6),
                    abline(1.0, 0.0).color(THRESHOLD).dashed()
                )
                .scales(scaleColorManual().color("null", NULL_BAND)
                        .color("significant", SIGNIFICANT))
                .scales(scaleYSqrt(Y_BREAKS))
                .coord(Coord2D.cartesian().ylim(0, Y_MAX))
                .guides(Guides.none())
                .theme(Theme.theme_bw())
                .labs(labs("Hardwood: GWAS QQ plot from Parquet",
                        "Expected −log₁₀(P)", "Observed −log₁₀(P) — square-root axis"));
    }
}
