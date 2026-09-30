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
package org.jtaccuino.gog.hardwood.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import org.jtaccuino.gog.hardwood.HardwoodDataFrame;
import org.jtaccuino.gog.hardwood.HardwoodDataFrame.Cache;

/**
 * Data loading and genomic layout for the human height GWAS of the GIANT
 * consortium (Yengo et al., <i>A saturated map of common genetic variants
 * associated with human height</i>, Nature 610:704–712, 2022), backed by the
 * Parquet files the build derives from the same source CSVs as the DFLib
 * backend.
 *
 * <p>The derived columns and the layout rules mirror
 * {@code org.jtaccuino.gog.dflib.data.GwasDatasets} exactly, so the Hardwood
 * examples render identically; only the frame type differs.
 *
 * <h2>Derived columns</h2>
 * {@link #loadManhattan()} returns the plot-ready table:
 * <ul>
 *   <li>{@code CHR}, {@code BP}, {@code P} — as published</li>
 *   <li>{@code BPCUM} — cumulative genome position, so the whole genome lays out
 *       along one continuous axis</li>
 *   <li>{@code NEGLOG10P} — the plotting height, {@code -log10(P)}, uncapped</li>
 *   <li>{@code CHRBAND} — {@code "odd"}/{@code "even"} by chromosome parity</li>
 *   <li>{@code BAND} — as {@code CHRBAND}, but {@code "lead"} for a confirmed
 *       lead SNP</li>
 *   <li>{@code GENE} — sparse: the nearest gene for the strongest loci</li>
 * </ul>
 */
public final class HardwoodGwasDatasets {

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
     * The height assigned to p-values the published file reports as {@code 0} —
     * the representational floor of IEEE-754 double precision.
     */
    public static final double UNDERFLOW_NEGLOG10 = 323.3;

    private static final int LABELLED_LOCI = 16;
    private static final double LABEL_SEPARATION = 60_000_000.0;
    private static final int CHROMOSOMES = 22;

    private static final Map<Cache, SoftCache<HardwoodDataFrame>> MANHATTAN = cachesByMode();
    private static final Map<Cache, SoftCache<HardwoodDataFrame>> LEAD_SNPS = cachesByMode();
    private static volatile Layout layout;

    private HardwoodGwasDatasets() {
    }

    /** {@return one soft cache per {@link Cache} mode, so each mode caches its own frame} */
    private static Map<Cache, SoftCache<HardwoodDataFrame>> cachesByMode() {
        var caches = new EnumMap<Cache, SoftCache<HardwoodDataFrame>>(Cache.class);
        for (var cache : Cache.values()) {
            caches.put(cache, new SoftCache<>());
        }
        return caches;
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
     * Loads the thinned summary statistics as the plot-ready Manhattan frame,
     * materializing the source Parquet in memory.
     *
     * @return the plot-ready frame
     */
    public static HardwoodDataFrame loadManhattan() {
        return loadManhattan(Cache.IN_MEMORY);
    }

    /**
     * Loads the thinned summary statistics as the plot-ready Manhattan frame,
     * materializing the source Parquet with {@code cache}. The returned frame is
     * the in-memory derived table either way, so {@code cache} only affects the
     * transient read of the raw Parquet.
     *
     * @param cache how to materialize the source Parquet before deriving
     * @return the plot-ready frame
     */
    public static HardwoodDataFrame loadManhattan(Cache cache) {
        return MANHATTAN.get(cache).get(() -> buildManhattan(cache));
    }

    private static HardwoodDataFrame buildManhattan(Cache cache) {
        var raw = rawThinned(cache);
        var chr = raw.intColumn("CHR");
        var bp = raw.doubleColumn("BP");
        var p = raw.doubleColumn("P");
        var sampled = raw.intColumn("SAMPLED");
        var n = chr.length;

        var leadGenes = leadSnpGenes();
        var offsets = cumulativeOffsets(chr, bp);

        var isLead = new boolean[n];
        for (var i = 0; i < n; i++) {
            isLead[i] = p[i] < GENOME_WIDE_SIGNIFICANCE && leadGenes.containsKey(locus(chr[i], bp[i]));
        }

        var labels = selectLabelledLoci(chr, bp, p, isLead, leadGenes);

        // Background points first, lead SNPs last, so highlights survive overplotting.
        var order = new Integer[n];
        for (var i = 0; i < n; i++) {
            order[i] = i;
        }
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

        var columns = new LinkedHashMap<String, List<?>>();
        columns.put("CHR", box(outChr));
        columns.put("BP", box(outBp));
        columns.put("P", box(outP));
        columns.put("SAMPLED", box(outSampled));
        columns.put("BPCUM", box(bpCum));
        columns.put("NEGLOG10P", box(negLog));
        columns.put("CHRBAND", List.of(chrBand));
        columns.put("BAND", List.of(band));
        columns.put("GENE", List.of(gene));
        return HardwoodDataFrame.ofColumns(columns);
    }

    /**
     * Counts the lead SNPs actually highlighted in a Manhattan table.
     *
     * @param manhattan the table returned by {@link #loadManhattan()}
     * @return the number of highlighted lead SNPs
     */
    public static int highlightedLeadSnps(HardwoodDataFrame manhattan) {
        var count = 0;
        for (var value : manhattan.stringColumn("BAND")) {
            if ("lead".equals(value)) {
                count++;
            }
        }
        return count;
    }

    /**
     * Restricts the Manhattan table to a single chromosome, re-expressing the
     * position in megabases for a close-up.
     *
     * @param manhattan  the table returned by {@link #loadManhattan()}
     * @param chromosome the chromosome to keep, 1–22
     * @return a table with an added {@code MB} column, holding only that chromosome
     */
    public static HardwoodDataFrame chromosome(HardwoodDataFrame manhattan, int chromosome) {
        var chr = manhattan.intColumn("CHR");
        var keep = new ArrayList<Integer>();
        for (var i = 0; i < chr.length; i++) {
            if (chr[i] == chromosome) {
                keep.add(i);
            }
        }
        var indices = keep.stream().mapToInt(Integer::intValue).toArray();

        var bp = manhattan.doubleColumn("BP");
        var p = manhattan.doubleColumn("P");
        var negLog = manhattan.doubleColumn("NEGLOG10P");
        var band = manhattan.stringColumn("BAND");
        var gene = manhattan.stringColumn("GENE");

        var n = indices.length;
        var outMb = new double[n];
        var outP = new double[n];
        var outNegLog = new double[n];
        var outBand = new String[n];
        var outGene = new String[n];
        for (var row = 0; row < n; row++) {
            int i = indices[row];
            outMb[row] = bp[i] / 1_000_000.0;
            outP[row] = p[i];
            outNegLog[row] = negLog[i];
            outBand[row] = "lead".equals(band[i]) ? "lead" : "background";
            outGene[row] = gene[i];
        }

        var columns = new LinkedHashMap<String, List<?>>();
        columns.put("MB", box(outMb));
        columns.put("P", box(outP));
        columns.put("NEGLOG10P", box(outNegLog));
        columns.put("BAND", List.of(outBand));
        columns.put("GENE", List.of(outGene));
        return HardwoodDataFrame.ofColumns(columns);
    }

    /**
     * Builds the quantile-quantile table comparing observed {@code -log10(P)}
     * against the uniform null expectation, using only the {@code SAMPLED} rows.
     *
     * @param manhattan the table returned by {@link #loadManhattan()}
     * @return a table with {@code EXPECTED}, {@code OBSERVED} and {@code BAND} columns
     */
    public static HardwoodDataFrame qqPlot(HardwoodDataFrame manhattan) {
        var allP = manhattan.doubleColumn("P");
        var sampled = manhattan.intColumn("SAMPLED");

        var count = 0;
        for (var flag : sampled) {
            if (flag == 1) {
                count++;
            }
        }
        var p = new double[count];
        var next = 0;
        for (var i = 0; i < allP.length; i++) {
            if (sampled[i] == 1) {
                p[next++] = allP[i];
            }
        }
        Arrays.sort(p);

        var expected = new double[count];
        var observed = new double[count];
        var band = new String[count];
        for (var i = 0; i < count; i++) {
            expected[i] = -Math.log10((i + 1.0) / (count + 1.0));
            observed[i] = negLog10(p[i]);
            band[i] = p[i] < GENOME_WIDE_SIGNIFICANCE ? "significant" : "null";
        }

        var columns = new LinkedHashMap<String, List<?>>();
        columns.put("EXPECTED", box(expected));
        columns.put("OBSERVED", box(observed));
        columns.put("BAND", List.of(band));
        return HardwoodDataFrame.ofColumns(columns);
    }

    /**
     * Returns the genomic axis layout, computing it on first use.
     *
     * @return the chromosome offsets, tick centres, and labels
     */
    public static Layout layout() {
        var resolved = layout;
        if (resolved == null) {
            synchronized (HardwoodGwasDatasets.class) {
                resolved = layout;
                if (resolved == null) {
                    resolved = computeLayout();
                    layout = resolved;
                }
            }
        }
        return resolved;
    }

    /** {@return the chromosome tick positions on the cumulative axis} */
    public static List<Double> chromosomeBreaks() {
        return layout().centers();
    }

    /** {@return the chromosome tick labels, aligned with {@link #chromosomeBreaks()}} */
    public static List<String> chromosomeLabels() {
        return layout().labels();
    }

    /**
     * Converts a p-value to its plotting height, {@code -log10(P)}, without
     * clamping.
     *
     * @param p the p-value
     * @return the exact {@code -log10(P)}, or the precision floor for zeros
     */
    public static double negLog10(double p) {
        if (p <= 0) {
            return UNDERFLOW_NEGLOG10;
        }
        return -Math.log10(p);
    }

    /**
     * Loads the published lead SNPs of Supplementary Table 10, materializing the
     * source Parquet in memory.
     *
     * @return the table with CHR, BP, SNP, P and GENE columns
     */
    public static HardwoodDataFrame loadLeadSnps() {
        return loadLeadSnps(Cache.IN_MEMORY);
    }

    /**
     * Loads the published lead SNPs of Supplementary Table 10, materializing the
     * source Parquet with {@code cache}. The returned frame stays Parquet-backed,
     * so {@code cache} decides how its storage is held.
     *
     * @param cache how to materialize the source Parquet
     * @return the table with CHR, BP, SNP, P and GENE columns
     */
    public static HardwoodDataFrame loadLeadSnps(Cache cache) {
        return LEAD_SNPS.get(cache).get(() -> HardwoodDataFrame.ofResource(
                "/examples/gwas/gwas-lead.parquet", cache));
    }

    private static HardwoodDataFrame rawThinned(Cache cache) {
        return HardwoodDataFrame.ofResource("/examples/gwas/gwas.parquet", cache);
    }

    private static Map<Long, String> selectLabelledLoci(int[] chr, double[] bp, double[] p,
                                                        boolean[] isLead, Map<Long, String> leadGenes) {
        var candidates = new ArrayList<Integer>();
        for (var i = 0; i < chr.length; i++) {
            if (isLead[i]) {
                candidates.add(i);
            }
        }
        candidates.sort(Comparator.comparingDouble(i -> p[i]));

        var labelled = new HashMap<Long, String>();
        var usedGenes = new LinkedHashSet<String>();
        var placed = new ArrayList<double[]>();

        for (var i : candidates) {
            if (labelled.size() >= LABELLED_LOCI) {
                break;
            }
            var name = leadGenes.get(locus(chr[i], bp[i]));
            if (name == null || name.isBlank() || !usedGenes.add(name)) {
                continue;
            }
            var tooClose = false;
            for (var prior : placed) {
                if (prior[0] == chr[i] && Math.abs(prior[1] - bp[i]) < LABEL_SEPARATION) {
                    tooClose = true;
                    break;
                }
            }
            if (tooClose) {
                continue;
            }
            labelled.put(locus(chr[i], bp[i]), name);
            placed.add(new double[]{chr[i], bp[i]});
        }
        return labelled;
    }

    private static Map<Long, String> leadSnpGenes() {
        var lead = loadLeadSnps();
        var chr = lead.intColumn("CHR");
        var bp = lead.doubleColumn("BP");
        var gene = lead.stringColumn("GENE");

        var genes = new HashMap<Long, String>(chr.length * 2);
        for (var i = 0; i < chr.length; i++) {
            genes.put(locus(chr[i], bp[i]), gene[i]);
        }
        return genes;
    }

    private static Layout computeLayout() {
        var raw = rawThinned(Cache.IN_MEMORY);
        var chr = raw.intColumn("CHR");
        var bp = raw.doubleColumn("BP");
        var offsets = cumulativeOffsets(chr, bp);

        var centers = new ArrayList<Double>(CHROMOSOMES);
        var labels = new ArrayList<String>(CHROMOSOMES);
        for (var c = 1; c <= CHROMOSOMES; c++) {
            centers.add(offsets[c] + (offsets[c + 1] - offsets[c]) / 2.0);
            labels.add(String.valueOf(c));
        }
        return new Layout(offsets, List.copyOf(centers), List.copyOf(labels), offsets[CHROMOSOMES + 1]);
    }

    private static double[] cumulativeOffsets(int[] chr, double[] bp) {
        var maxBp = new double[CHROMOSOMES + 1];
        for (var i = 0; i < chr.length; i++) {
            if (bp[i] > maxBp[chr[i]]) {
                maxBp[chr[i]] = bp[i];
            }
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

    private static long locus(int chr, double bp) {
        return (long) chr * 1_000_000_000L + (long) bp;
    }

    private static List<Integer> box(int[] values) {
        var out = new ArrayList<Integer>(values.length);
        for (var value : values) {
            out.add(value);
        }
        return out;
    }

    private static List<Double> box(double[] values) {
        var out = new ArrayList<Double>(values.length);
        for (var value : values) {
            out.add(value);
        }
        return out;
    }
}
