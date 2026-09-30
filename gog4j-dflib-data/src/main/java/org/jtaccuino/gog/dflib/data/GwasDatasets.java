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
package org.jtaccuino.gog.dflib.data;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import org.dflib.DataFrame;
import org.dflib.Series;
import org.dflib.csv.Csv;

/**
 * Data loading and genomic layout for the human height GWAS of the GIANT
 * consortium (Yengo et al., <i>A saturated map of common genetic variants
 * associated with human height</i>, Nature 610:704–712, 2022).
 *
 * <h2>Shipped data</h2>
 * Two resources back these examples, both derived from material published under
 * CC BY 4.0 alongside the paper:
 * <ul>
 *   <li>{@code height_gwas_thinned.csv.gz} — the multi-ancestry summary
 *       statistics (GIANT consortium release, GRCh37/hg19). To keep the
 *       repository small, every third SNP is retained, giving
 *       {@value #UNIFORM_SAMPLE_COUNT} of the {@value #TOTAL_SNP_COUNT}
 *       variants. The sample is <em>uniform</em>: thinning by p-value would
 *       leave a visible density step in the plot where the retention rule
 *       changes. On top of that sample the published lead SNPs are always kept,
 *       so the highlight layer is complete; the {@code SAMPLED} column marks
 *       which rows belong to the uniform sample, which is what
 *       {@link #qqPlot(DataFrame)} needs to compute honest quantiles.</li>
 *   <li>{@code height_lead_snps.csv} — the {@value #PUBLISHED_LEAD_SNPS}
 *       independent genome-wide significant lead SNPs of Supplementary Table
 *       10, with the nearest gene per locus.</li>
 * </ul>
 *
 * <h2>Why fewer SNPs are highlighted than the paper reports</h2>
 * The published lead SNPs were called on the full meta-analysis, which includes
 * 23andMe participants whose data is not public. In the public release those
 * same variants carry weaker evidence: of the {@value #PUBLISHED_LEAD_SNPS}
 * lead SNPs, 6,218 still clear {@code P < 5e-8} here, 3,350 fall between
 * {@code 5e-8} and {@code 1e-3}, and 2,543 sit at {@code P >= 1e-3}. Marking
 * all of them would scatter highlights across the noise floor, so only those
 * confirmed genome-wide significant <em>in the plotted data</em> are marked.
 *
 * <h2>Derived columns</h2>
 * {@link #loadManhattan()} returns the plot-ready frame:
 * <ul>
 *   <li>{@code CHR}, {@code BP}, {@code P} — as published</li>
 *   <li>{@code BPCUM} — the cumulative genome position, i.e. {@code BP} shifted
 *       by the summed length of all preceding chromosomes, so that the whole
 *       genome lays out along a single continuous axis</li>
 *   <li>{@code NEGLOG10P} — the plotting height, {@code -log10(P)}, uncapped.
 *       The values span 0 to {@value #UNDERFLOW_NEGLOG10}; the plots fit that
 *       range with a square-root axis rather than by clamping the data</li>
 *   <li>{@code CHRBAND} — {@code "odd"}/{@code "even"} by chromosome parity,
 *       driving the alternating banding</li>
 *   <li>{@code BAND} — as {@code CHRBAND}, but {@code "lead"} for a confirmed
 *       lead SNP</li>
 *   <li>{@code GENE} — sparse: the nearest gene, filled in only for the
 *       strongest loci so that {@code Geoms.text()} names them</li>
 * </ul>
 */
public class GwasDatasets {

    /** Usable SNPs in the published summary statistics before thinning. */
    public static final int TOTAL_SNP_COUNT = 1_377_294;

    /** Rows in the shipped file: the uniform sample plus the retained lead SNPs. */
    public static final int THINNED_SNP_COUNT = 467_159;

    /** Rows belonging to the uniform one-in-three sample. */
    public static final int UNIFORM_SAMPLE_COUNT = 459_098;

    /** Independent lead SNPs reported in Supplementary Table 10. */
    public static final int PUBLISHED_LEAD_SNPS = 12_111;

    /** The conventional genome-wide significance threshold. */
    public static final double GENOME_WIDE_SIGNIFICANCE = 5e-8;

    /** {@code -log10(5e-8)}, the height of the significance line. */
    public static final double GENOME_WIDE_LINE = -Math.log10(GENOME_WIDE_SIGNIFICANCE);

    /**
     * The height assigned to p-values that the published file reports as {@code 0}.
     * <p>
     * Those associations are stronger than IEEE-754 double precision can express:
     * the smallest positive double is {@code 4.94e-324}, so {@code -log10} of it is
     * {@value}. This is not a display choice but the representational floor of the
     * source data — the true p-values are smaller still, and no finite y value can
     * separate them. Every other SNP is plotted at its exact {@code -log10(P)}.
     */
    public static final double UNDERFLOW_NEGLOG10 = 323.3;

    /** How many loci {@link #loadManhattan()} names with their nearest gene. */
    private static final int LABELLED_LOCI = 16;

    /** Minimum distance between two labelled loci on the same chromosome, in base pairs. */
    private static final double LABEL_SEPARATION = 60_000_000.0;

    private static final int CHROMOSOMES = 22;

    private static Layout layout;
    private static final SoftCache<DataFrame> MANHATTAN = new SoftCache<>();
    private static final SoftCache<DataFrame> LEAD_SNPS = new SoftCache<>();
    // The thinned summary statistics are a transient input to buildManhattan()
    // and layout(); they are re-read from the gzipped CSV whenever the manhattan
    // frame is rebuilt and are never retained, so only the single plot-ready
    // frame stays resident. See buildManhattan()/cumulativeOffsets().

    /** Utility class; not meant to be instantiated. */
    private GwasDatasets() {
    }

    /**
     * The genomic axis layout: per-chromosome offsets on the cumulative axis
     * plus the tick positions and labels derived from them.
     *
     * @param offsets      cumulative start position of each chromosome, indexed 1..22
     * @param centers      midpoint of each chromosome on the cumulative axis
     * @param labels       chromosome names, aligned with {@code centers}
     * @param genomeLength total cumulative length of all chromosomes
     */
    @SuppressWarnings("ArrayRecordComponent") // offsets is read-only layout data
    public record Layout(double[] offsets, List<Double> centers, List<String> labels, double genomeLength) {
    }

    /**
     * Loads the thinned summary statistics and augments them with the columns
     * the Manhattan plot maps: cumulative position, {@code -log10(P)},
     * the alternating band, and sparse gene labels for the strongest loci.
     * <p>
     * Rows are ordered so that lead SNPs come last and are therefore painted on
     * top of the surrounding background points.
     *
     * @return the plot-ready DataFrame
     */
    public static DataFrame loadManhattan() {
        return MANHATTAN.get(GwasDatasets::buildManhattan);
    }

    private static DataFrame buildManhattan() {
        var raw = readThinned();

        var chr = intColumn(raw, "CHR");
        var bp = doubleColumn(raw, "BP");
        var p = doubleColumn(raw, "P");
        var sampled = intColumn(raw, "SAMPLED");
        var n = chr.length;

        var leadGenes = leadSnpGenes();
        var offsets = cumulativeOffsets(chr, bp);

        // A published lead SNP is only marked where the public data confirms it;
        // otherwise highlights would litter the noise floor. See the class javadoc.
        var isLead = new boolean[n];
        for (var i = 0; i < n; i++) {
            isLead[i] = p[i] < GENOME_WIDE_SIGNIFICANCE && leadGenes.containsKey(locus(chr[i], bp[i]));
        }

        var labels = selectLabelledLoci(chr, bp, p, isLead, leadGenes);

        // Background points first, lead SNPs last, so highlights survive overplotting.
        var order = new Integer[n];
        for (var i = 0; i < n; i++) order[i] = i;
        Arrays.sort(order, Comparator.comparingInt(i -> isLead[i] ? 1 : 0));

        var outChr = new int[n];
        var outBp = new double[n];
        var outP = new double[n];
        var outSampled = new int[n];
        var bpCum = new double[n];
        var negLog = new double[n];
        var chrBand = new String[n];
        var band = new String[n];
        var gene = new String[n];

        for (var row = 0; row < n; row++) {
            int i = order[row];
            outChr[row] = chr[i];
            outBp[row] = bp[i];
            outP[row] = p[i];
            outSampled[row] = sampled[i];
            bpCum[row] = offsets[chr[i]] + bp[i];
            negLog[row] = negLog10(p[i]);

            var parity = chr[i] % 2 == 1 ? "odd" : "even";
            chrBand[row] = parity;
            band[row] = isLead[i] ? "lead" : parity;
            gene[row] = labels.getOrDefault(locus(chr[i], bp[i]), "");
        }

        return DataFrame.byColumn("CHR", "BP", "P", "SAMPLED", "BPCUM", "NEGLOG10P", "CHRBAND", "BAND", "GENE")
                .of(Series.ofInt(outChr),
                    Series.ofDouble(outBp),
                    Series.ofDouble(outP),
                    Series.ofInt(outSampled),
                    Series.ofDouble(bpCum),
                    Series.ofDouble(negLog),
                    Series.of(chrBand),
                    Series.of(band),
                    Series.of(gene));
    }

    /**
     * Counts the lead SNPs actually highlighted in a Manhattan frame, i.e. those
     * the public release confirms at genome-wide significance.
     *
     * @param manhattan the frame returned by {@link #loadManhattan()}
     * @return the number of highlighted lead SNPs
     */
    public static int highlightedLeadSnps(DataFrame manhattan) {
        var count = 0;
        for (var value : stringColumn(manhattan, "BAND")) {
            if ("lead".equals(value)) count++;
        }
        return count;
    }

    /**
     * Restricts the Manhattan frame to a single chromosome and re-expresses the
     * position in megabases, for a close-up of one locus region.
     *
     * @param manhattan  the frame returned by {@link #loadManhattan()}
     * @param chromosome the chromosome to keep, 1–22
     * @return a frame with an added {@code MB} column, holding only that chromosome
     */
    public static DataFrame chromosome(DataFrame manhattan, int chromosome) {
        var chr = intColumn(manhattan, "CHR");
        var keep = new ArrayList<Integer>();
        for (var i = 0; i < chr.length; i++) {
            if (chr[i] == chromosome) keep.add(i);
        }

        var bp = doubleColumn(manhattan, "BP");
        var p = doubleColumn(manhattan, "P");
        var negLog = doubleColumn(manhattan, "NEGLOG10P");
        var band = stringColumn(manhattan, "BAND");
        var gene = stringColumn(manhattan, "GENE");

        var n = keep.size();
        var outMb = new double[n];
        var outP = new double[n];
        var outNegLog = new double[n];
        var outBand = new String[n];
        var outGene = new String[n];

        for (var row = 0; row < n; row++) {
            int i = keep.get(row);
            outMb[row] = bp[i] / 1_000_000.0;
            outP[row] = p[i];
            outNegLog[row] = negLog[i];
            // Within one chromosome the parity banding carries no information,
            // so the background collapses to a single tone.
            outBand[row] = "lead".equals(band[i]) ? "lead" : "background";
            outGene[row] = gene[i];
        }

        return DataFrame.byColumn("MB", "P", "NEGLOG10P", "BAND", "GENE")
                .of(Series.ofDouble(outMb),
                    Series.ofDouble(outP),
                    Series.ofDouble(outNegLog),
                    Series.of(outBand),
                    Series.of(outGene));
    }

    /**
     * Builds the quantile-quantile frame comparing observed {@code -log10(P)}
     * against the uniform null expectation.
     * <p>
     * Only rows flagged {@code SAMPLED} are used. Those form an unbiased
     * one-in-three sample of the full scan, so plain sample ranks give correct
     * quantiles; the additionally retained lead SNPs are excluded because they
     * would over-weight the extreme tail.
     *
     * @param manhattan the frame returned by {@link #loadManhattan()}
     * @return a frame with {@code EXPECTED}, {@code OBSERVED} and {@code BAND} columns
     */
    public static DataFrame qqPlot(DataFrame manhattan) {
        var allP = doubleColumn(manhattan, "P");
        var sampled = intColumn(manhattan, "SAMPLED");

        var count = 0;
        for (var flag : sampled) {
            if (flag == 1) count++;
        }

        var p = new double[count];
        var next = 0;
        for (var i = 0; i < allP.length; i++) {
            if (sampled[i] == 1) p[next++] = allP[i];
        }
        Arrays.sort(p);

        var expected = new double[count];
        var observed = new double[count];
        var band = new String[count];
        for (var i = 0; i < count; i++) {
            expected[i] = -Math.log10((i + 1.0) / (count + 1.0));
            // Underflowed p-values have no finite log; park them above any view.
            observed[i] = negLog10(p[i]);
            band[i] = p[i] < GENOME_WIDE_SIGNIFICANCE ? "significant" : "null";
        }

        return DataFrame.byColumn("EXPECTED", "OBSERVED", "BAND")
                .of(Series.ofDouble(expected), Series.ofDouble(observed), Series.of(band));
    }

    /**
     * Returns the genomic axis layout, computing it on first use.
     *
     * @return the chromosome offsets, tick centres, and labels
     */
    public static synchronized Layout layout() {
        if (layout == null) {
            layout = computeLayout();
        }
        return layout;
    }

    /**
     * The chromosome tick positions on the cumulative axis.
     *
     * @return the midpoint of each chromosome
     */
    public static List<Double> chromosomeBreaks() {
        return layout().centers();
    }

    /**
     * The chromosome tick labels, aligned with {@link #chromosomeBreaks()}.
     *
     * @return chromosome names 1–22
     */
    public static List<String> chromosomeLabels() {
        return layout().labels();
    }

    /**
     * Converts a p-value to its plotting height, {@code -log10(P)}, without
     * clamping. P-values the source reports as {@code 0} take
     * {@link #UNDERFLOW_NEGLOG10}, the limit of double precision.
     *
     * @param p the p-value
     * @return the exact {@code -log10(P)}, or the precision floor for zeros
     */
    public static double negLog10(double p) {
        if (p <= 0) return UNDERFLOW_NEGLOG10;
        return -Math.log10(p);
    }

    /**
     * Loads the published lead SNPs of Supplementary Table 10.
     *
     * @return the DataFrame with CHR, BP, SNP, P and GENE columns
     */
    public static DataFrame loadLeadSnps() {
        return LEAD_SNPS.get(() -> read("/examples/gwas/height_lead_snps.csv", false));
    }

    /**
     * Picks the loci worth naming: the strongest confirmed lead SNPs, one per
     * gene and spread across the genome, so the labels stay legible.
     */
    private static Map<Long, String> selectLabelledLoci(int[] chr, double[] bp, double[] p,
                                                        boolean[] isLead, Map<Long, String> leadGenes) {
        var candidates = new ArrayList<Integer>();
        for (var i = 0; i < chr.length; i++) {
            if (isLead[i]) candidates.add(i);
        }
        candidates.sort(Comparator.comparingDouble(i -> p[i]));

        var labelled = new HashMap<Long, String>();
        var usedGenes = new LinkedHashSet<String>();
        var placed = new ArrayList<double[]>();

        for (var i : candidates) {
            if (labelled.size() >= LABELLED_LOCI) break;
            var name = leadGenes.get(locus(chr[i], bp[i]));
            if (name == null || name.isBlank() || !usedGenes.add(name)) continue;

            var tooClose = false;
            for (var prior : placed) {
                if (prior[0] == chr[i] && Math.abs(prior[1] - bp[i]) < LABEL_SEPARATION) {
                    tooClose = true;
                    break;
                }
            }
            if (tooClose) continue;

            labelled.put(locus(chr[i], bp[i]), name);
            placed.add(new double[]{chr[i], bp[i]});
        }
        return labelled;
    }

    private static Map<Long, String> leadSnpGenes() {
        var lead = loadLeadSnps();
        var chr = intColumn(lead, "CHR");
        var bp = doubleColumn(lead, "BP");
        var gene = stringColumn(lead, "GENE");

        var genes = new HashMap<Long, String>(chr.length * 2);
        for (var i = 0; i < chr.length; i++) {
            genes.put(locus(chr[i], bp[i]), gene[i]);
        }
        return genes;
    }

    private static Layout computeLayout() {
        var raw = readThinned();
        var chr = intColumn(raw, "CHR");
        var bp = doubleColumn(raw, "BP");
        var offsets = cumulativeOffsets(chr, bp);

        var centers = new ArrayList<Double>(CHROMOSOMES);
        var labels = new ArrayList<String>(CHROMOSOMES);
        for (var c = 1; c <= CHROMOSOMES; c++) {
            centers.add(offsets[c] + (offsets[c + 1] - offsets[c]) / 2.0);
            labels.add(String.valueOf(c));
        }

        return new Layout(offsets, List.copyOf(centers), List.copyOf(labels), offsets[CHROMOSOMES + 1]);
    }

    /**
     * The cumulative start position of each chromosome on the genome axis,
     * computed from the chromosome/position columns of the thinned file. Shared
     * by the Manhattan layout and the plot-ready frame, so the thinned frame is
     * never retained between builds.
     */
    private static double[] cumulativeOffsets(int[] chr, double[] bp) {
        var maxBp = new double[CHROMOSOMES + 1];
        for (var i = 0; i < chr.length; i++) {
            if (bp[i] > maxBp[chr[i]]) maxBp[chr[i]] = bp[i];
        }

        var offsets = new double[CHROMOSOMES + 2];
        var running = 0.0;
        for (var c = 1; c <= CHROMOSOMES; c++) {
            offsets[c] = running;
            running += maxBp[c];
        }
        offsets[CHROMOSOMES + 1] = running;

        return offsets;
    }

    private static DataFrame readThinned() {
        return read("/examples/gwas/height_gwas_thinned.csv.gz", true);
    }

    private static DataFrame read(String resource, boolean gzipped) {
        try (var in = GwasDatasets.class.getResourceAsStream(resource)) {
            if (in == null) {
                throw new IllegalStateException("Missing example resource: " + resource);
            }
            var stream = gzipped ? new GZIPInputStream(in, 1 << 16) : in;
            try (var reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8), 1 << 16)) {
                return Csv.load(reader);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read " + resource, e);
        }
    }

    private static long locus(int chr, double bp) {
        return (long) chr * 1_000_000_000L + (long) bp;
    }

    private static int[] intColumn(DataFrame df, String name) {
        var series = df.getColumn(name);
        var out = new int[series.size()];
        for (var i = 0; i < out.length; i++) {
            var value = series.get(i);
            out[i] = value instanceof Number num ? num.intValue() : Integer.parseInt(value.toString().trim());
        }
        return out;
    }

    private static double[] doubleColumn(DataFrame df, String name) {
        var series = df.getColumn(name);
        var out = new double[series.size()];
        for (var i = 0; i < out.length; i++) {
            var value = series.get(i);
            out[i] = value instanceof Number num ? num.doubleValue() : Double.parseDouble(value.toString().trim());
        }
        return out;
    }

    private static String[] stringColumn(DataFrame df, String name) {
        var series = df.getColumn(name);
        var out = new String[series.size()];
        for (var i = 0; i < out.length; i++) {
            var value = series.get(i);
            out[i] = value == null ? "" : value.toString();
        }
        return out;
    }
}
