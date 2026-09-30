///usr/bin/env jbang "$0" "$@" && exit $?
//
// Compares JFR @MeasurePhases benchmark summaries between a PR head and its
// base branch, writing a markdown difference table. Each side may supply
// several runs: either one build/benchmark/<plot>.json per run/label, or the
// aggregate <label>.jsonl with one run per line (emitted by PhaseTimings).
//
// Robustness: per phase we take each run's trimmed mean (mean with the fastest
// and slowest sample removed -- or the plain mean as a fallback). Per side the
// fastest and slowest run are then dropped and the remaining "middle" runs are
// averaged, so a whole run slowed by shared-machine load contributes little.
//
// Verdicts:
//   REGRESSED / improved  - the sides' middle runs do not overlap at the
//                           configured limit: the slowest head middle run stays
//                           above the fastest base middle run by > max% (or
//                           symmetric for improvements). A single fluky run on
//                           either side can no longer trip the gate.
//   noisy                 - the phase's run-to-run coefficient of variation
//                           exceeds --noisy-cv on one side; reported but never
//                           fails the job.
//
// Exits non-zero only for real regressions: base trimmed mean >= --min-gate-us,
// absolute increase >= --min-delta-us, relative increase > --max-regression,
// and the overlap condition above.
//
//USAGE:
//  jbang BenchmarkDiff.java --base-dir <dir> --head-dir <dir> --out <file.md>
//      [--max-regression <pct>] [--min-gate-us <us>] [--min-delta-us <us>]
//      [--noisy-cv <pct>]
//JAVA 26+
//DEPS com.fasterxml.jackson.core:jackson-databind:2.18.3

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;

public class BenchmarkDiff {

    record Timing(long bestUs, long tmeanUs, long avgUs, double cvPct, int n) {}

    public static void main(String[] args) throws Exception {
        Path baseDir = null;
        Path headDir = null;
        Path out = null;
        double maxRegression = 15.0;
        long minGateUs = 1000;
        long minDeltaUs = 1000;
        double noisyCv = 15.0;
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--base-dir" -> baseDir = Path.of(args[++i]);
                case "--head-dir" -> headDir = Path.of(args[++i]);
                case "--out" -> out = Path.of(args[++i]);
                case "--max-regression" -> maxRegression = Double.parseDouble(args[++i]);
                case "--min-gate-us" -> minGateUs = Long.parseLong(args[++i]);
                case "--min-delta-us" -> minDeltaUs = Long.parseLong(args[++i]);
                case "--noisy-cv" -> noisyCv = Double.parseDouble(args[++i]);
                default -> throw new IllegalArgumentException("Unknown argument: " + args[i]);
            }
        }
        if (baseDir == null || headDir == null || out == null) {
            System.err.println("usage: BenchmarkDiff --base-dir <dir> --head-dir <dir>"
                    + " --out <file.md> [--max-regression pct] [--min-gate-us us] [--min-delta-us us]"
                    + " [--noisy-cv pct]");
            System.exit(2);
        }

        var base = load(baseDir);
        var head = load(headDir);
        if (base.isEmpty() || head.isEmpty()) {
            var missing = new ArrayList<String>();
            if (base.isEmpty()) {
                missing.add("base=" + baseDir + " (no summaries; does the base branch emit build/benchmark JSON?)");
            }
            if (head.isEmpty()) {
                missing.add("head=" + headDir);
            }
            var msg = "No JFR benchmark summaries found (" + String.join("; ", missing)
                    + "); skipping the performance comparison.";
            System.out.println(msg);
            Files.writeString(out, msg + System.lineSeparator());
            return;
        }

        var md = buildMarkdown(base, head, baseDir, headDir, maxRegression, minGateUs, minDeltaUs, noisyCv);
        Files.writeString(out, md);
        System.out.print(md);

        double worst = worstRegression(base, head, maxRegression, minGateUs, minDeltaUs, noisyCv);
        if (worst > maxRegression) {
            System.out.printf(Locale.ROOT,
                    "PERFORMANCE REGRESSION: worst gated trimmed-mean regression %.1f%% exceeds max %.1f%%%n",
                    worst, maxRegression);
            System.exit(1);
        }
        System.out.printf(Locale.ROOT,
                "No gated regression: worst gated trimmed-mean change %.1f%% within %.1f%% limit%n",
                worst, maxRegression);
    }

    /** @return per plot a list of per-run phase maps, in file (run) order. */
    private static Map<String, List<Map<String, Timing>>> load(Path dir) throws IOException {
        var byPlot = new LinkedHashMap<String, List<Map<String, Timing>>>();
        if (!Files.isDirectory(dir)) {
            return byPlot;
        }
        var mapper = new ObjectMapper();
        try (var files = Files.list(dir)) {
            for (var file : files.filter(p -> isBenchmarkFile(p.getFileName().toString()))
                    .sorted().toList()) {
                var fileName = file.getFileName().toString();
                var root = mapper.readTree(file.toFile());
                var plot = root.path("plot").asText("<unnamed>");
                List<Map<String, Timing>> runs;
                if (fileName.endsWith(".jsonl")) {
                    runs = new ArrayList<>();
                    for (var line : Files.readAllLines(file)) {
                        if (line.isBlank()) {
                            continue;
                        }
                        runs.add(parseRun(mapper.readTree(line)));
                    }
                } else {
                    runs = List.of(parseRun(root));
                }
                byPlot.put(plot, runs);
            }
        }
        return byPlot;
    }

    private static boolean isBenchmarkFile(String name) {
        return name.endsWith(".jsonl") || name.endsWith(".json");
    }

    private static Map<String, Timing> parseRun(JsonNode root) {
        var phases = new LinkedHashMap<String, Timing>();
        var phaseNodes = root.path("phases");
        var names = new ArrayList<String>();
        phaseNodes.fieldNames().forEachRemaining(names::add);
        names.sort(Comparator.naturalOrder());
        for (var name : names) {
            var t = phaseNodes.path(name);
            phases.put(name, new Timing(t.path("bestUs").asLong(),
                    t.path("tmeanUs").asLong(), t.path("avgUs").asLong(),
                    t.path("cvPct").asDouble(-1.0), t.path("n").asInt(0)));
        }
        return phases;
    }

    private record SidePhase(List<Long> runCentralUs) {}

    /**
     * Per-run values with the fastest and slowest run removed, so a whole run
     * lifted by machine load does not bias the verdict. Two or fewer runs are
     * kept unchanged.
     */
    private static List<Long> middleRuns(List<Long> values) {
        if (values.size() <= 2) {
            return new ArrayList<>(values);
        }
        var sorted = new ArrayList<>(values);
        sorted.sort(Comparator.naturalOrder());
        return new ArrayList<>(sorted.subList(1, sorted.size() - 1));
    }

    /** Robust cross-run central value: average of the middle runs. */
    private static double aggregateUs(List<Long> values) {
        var mid = middleRuns(values);
        if (mid.isEmpty()) {
            return 0;
        }
        return mid.stream().mapToLong(Long::longValue).average().orElse(0);
    }

    /**
     * Run-to-run coefficient of variation (sample sd) of the per-run central
     * values, across all runs. Negative when there are fewer than two runs.
     */
    private static double runCvPct(List<Long> values) {
        if (values.size() < 2) {
            return -1;
        }
        double mean = values.stream().mapToLong(Long::longValue).average().orElse(0);
        if (mean == 0) {
            return 0;
        }
        double squaredDist = 0;
        for (long v : values) {
            double d = v - mean;
            squaredDist += d * d;
        }
        return Math.sqrt(squaredDist / (values.size() - 1)) / mean * 100;
    }

    private static boolean runsTooNoisy(SidePhase b, SidePhase h, double noisyCv) {
        return (runCvPct(b.runCentralUs()) > noisyCv) || (runCvPct(h.runCentralUs()) > noisyCv);
    }

    /**
     * Verdict for one phase. A regression requires the aggregated delta to clear
     * the delta/gate floors AND the slowest head middle run to stay above the
     * fastest base middle run by more than max%, so a single fluky run on either
     * side cannot trip it. Symmetric for improvements.
     */
    private static String verdict(double bUs, double hUs, SidePhase b, SidePhase h,
                                  double maxRegression, long minGateUs, long minDeltaUs,
                                  double noisyCv) {
        if (bUs < minGateUs) {
            return "- ";
        }
        if (runsTooNoisy(b, h, noisyCv)) {
            return "noisy ";
        }
        double deltaUs = hUs - bUs;
        double deltaPct = deltaUs * 100.0 / bUs;
        var bMid = middleRuns(b.runCentralUs());
        var hMid = middleRuns(h.runCentralUs());
        boolean regressed = deltaUs >= minDeltaUs && deltaPct > maxRegression
                && minUs(hMid) > maxUs(bMid) * (1 + maxRegression / 100.0);
        if (regressed) {
            return "REGRESSED ";
        }
        boolean improved = deltaUs <= -minDeltaUs && deltaPct < -maxRegression
                && maxUs(hMid) < minUs(bMid) * (1 - maxRegression / 100.0);
        if (improved) {
            return "improved ";
        }
        return "- ";
    }

    private static long minUs(List<Long> values) {
        return values.stream().mapToLong(Long::longValue).min().orElse(0);
    }

    private static long maxUs(List<Long> values) {
        return values.stream().mapToLong(Long::longValue).max().orElse(0);
    }

    /** Central per-run value: the trimmed mean, or the plain average as fallback. */
    private static long runCentralUs(Timing t) {
        return t.tmeanUs() > 0 ? t.tmeanUs() : t.avgUs();
    }

    /**
     * Flattens each plot's per-run phase maps and reduces them by phase, collecting
     * the per-run central values of every phase into a {@link SidePhase}.
     */
    private static Map<String, Map<String, SidePhase>> collectRuns(
            Map<String, List<Map<String, Timing>>> byPlot) {
        return byPlot.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey,
                plot -> plot.getValue().stream()
                        .flatMap(run -> run.entrySet().stream())
                        .collect(Collectors.groupingBy(Map.Entry::getKey,
                                LinkedHashMap::new,
                                Collectors.mapping(phase -> runCentralUs(phase.getValue()),
                                        Collectors.collectingAndThen(Collectors.toList(), SidePhase::new))))));
    }

    private static String buildMarkdown(Map<String, List<Map<String, Timing>>> baseRuns,
                                        Map<String, List<Map<String, Timing>>> headRuns,
                                        Path baseDir, Path headDir,
                                        double maxRegression, long minGateUs, long minDeltaUs,
                                        double noisyCv) {
        var nl = System.lineSeparator();
        var base = collectRuns(baseRuns);
        var head = collectRuns(headRuns);
        int baseCount = baseRuns.values().stream().mapToInt(List::size).sum();
        int headCount = headRuns.values().stream().mapToInt(List::size).sum();
        var sb = new StringBuilder();
        sb.append("## JFR benchmark: PR head vs base branch").append(nl).append(nl);
        sb.append("Trimmed mean of ").append(baseCount).append(" base / ").append(headCount)
                .append(" head runs ").append("(per-sample best+worst and slowest+fastest run dropped).")
                .append(nl).append(nl);
        sb.append("| plot | phase | base tmean (us) | base run cv% | PR tmean (us) | PR run cv% | d tmean | verdict |").append(nl);
        sb.append("|---|---|---:|---:|---:|---:|---:|---|").append(nl);
        var plots = new ArrayList<>(base.keySet());
        plots.sort(Comparator.naturalOrder());
        for (var plot : plots) {
            var headPhases = head.get(plot);
            if (headPhases == null) {
                continue;
            }
            var basePhases = base.get(plot);
            var phases = new ArrayList<>(basePhases.keySet());
            phases.sort(Comparator.naturalOrder());
            for (var phase : phases) {
                var b = basePhases.get(phase);
                var h = headPhases.get(phase);
                if (b == null || h == null) {
                    continue;
                }
                double bUs = aggregateUs(b.runCentralUs());
                double hUs = aggregateUs(h.runCentralUs());
                if (bUs < minGateUs) {
                    continue;
                }
                double deltaPct = (hUs - bUs) * 100.0 / bUs;
                String verdict = verdict(bUs, hUs, b, h, maxRegression, minGateUs, minDeltaUs, noisyCv);
                sb.append(String.format(Locale.ROOT,
                        "| %s | %s | %d | %s | %d | %s | %+.1f%% | %s|",
                        plot, phase, Math.round(bUs), formatCv(runCvPct(b.runCentralUs())),
                        Math.round(hUs), formatCv(runCvPct(h.runCentralUs())), deltaPct, verdict))
                        .append(nl);
            }
        }
        sb.append(nl)
                .append("_verdict: REGRESSED / improved = head and base middle runs do not overlap at ")
                .append(formatPct(maxRegression))
                .append("; noisy = run-to-run cv% above ").append(formatPct(noisyCv))
                .append(" on one side (reported, not failing)._").append(nl);
        sb.append(nl).append(inliningMarkdown(baseDir, headDir)).append(nl);
        return sb.toString();
    }

    private static double worstRegression(Map<String, List<Map<String, Timing>>> baseRuns,
                                          Map<String, List<Map<String, Timing>>> headRuns,
                                          double maxRegression, long minGateUs, long minDeltaUs,
                                          double noisyCv) {
        var base = collectRuns(baseRuns);
        var head = collectRuns(headRuns);
        double worst = 0.0;
        for (var basePlot : base.entrySet()) {
            var headPhases = head.get(basePlot.getKey());
            if (headPhases == null) {
                continue;
            }
            for (var phaseEntry : basePlot.getValue().entrySet()) {
                var h = headPhases.get(phaseEntry.getKey());
                if (h == null) {
                    continue;
                }
                var b = phaseEntry.getValue();
                double bUs = aggregateUs(b.runCentralUs());
                double hUs = aggregateUs(h.runCentralUs());
                if (!"REGRESSED ".equals(verdict(bUs, hUs, b, h, maxRegression, minGateUs, minDeltaUs, noisyCv))) {
                    continue;
                }
                double deltaPct = (hUs - bUs) * 100.0 / bUs;
                worst = Math.max(worst, deltaPct);
            }
        }
        return worst;
    }

    private static final Set<String> POINT_LEAVES = Set.of("toPixel", "toData", "forward", "draw");

    /** Aggregate C2 inlining record for the constant-style point emission loop. */
    private record InlineSummary(String root, int compilations, int inlineFails,
                                 int leavesInlined, int leavesFailed, Map<String, Integer> reasons) {
        static InlineSummary none() {
            return new InlineSummary("(no hotspot.log)", 0, 0, 0, 0, Map.of());
        }
    }

    /**
     * Reads the C2 compilation log ({@code build/benchmark/hotspot.log}, written
     * by the {@code -XX:+LogCompilation} benchmark JVMs) and reports how the
     * constant-style point emission loop was compiled. The loop lives in
     * {@code PointEmitter.emit} on the head branch; on the base branch it is the
     * long {@code GeomPoint.render} method. Whether its per-point leaves
     * ({@code toPixel}, {@code toData}, {@code forward}, {@code draw}) were
     * inlined or left as runtime calls is deterministic and load-independent,
     * so it surfaces JIT-budget regressions before the phase timings get noisy.
     * Informational only: the phase verdicts stay the gate.
     */
    private static InlineSummary readInlining(Path dir) {
        var log = dir.resolve("hotspot.log");
        if (!Files.isRegularFile(log)) {
            return InlineSummary.none();
        }
        int compilations = 0;
        int inlineFails = 0;
        int leavesInlined = 0;
        int leavesFailed = 0;
        var reasons = new LinkedHashMap<String, Integer>();
        var roots = new LinkedHashSet<String>();
        try (InputStream in = Files.newInputStream(log)) {
            var reader = XMLInputFactory.newFactory().createXMLStreamReader(in);
            boolean matching = false;
            boolean pendingFail = false;
            String lastFail = null;
            int methodDepth = 0;
            int skipUntilDepth = -1;
            var klass = new LinkedHashMap<String, String>();
            while (reader.hasNext()) {
                int type = reader.next();
                if (type == XMLStreamConstants.START_ELEMENT) {
                    String tag = reader.getLocalName();
                    switch (tag) {
                        case "task", "compilation" -> {
                            klass.clear();
                            pendingFail = false;
                            skipUntilDepth = -1;
                            matching = matchesLoopRoot(reader.getAttributeValue(null, "method"));
                            if (matching) {
                                roots.add(loopRootLabel(reader.getAttributeValue(null, "method")));
                                compilations++;
                            }
                        }
                        case "klass" -> {
                            var id = reader.getAttributeValue(null, "id");
                            var name = reader.getAttributeValue(null, "name");
                            if (id != null && name != null) {
                                klass.put(id, name);
                            }
                        }
                        case "inline_fail" -> {
                            if (matching) {
                                inlineFails++;
                                pendingFail = true;
                                lastFail = reader.getAttributeValue(null, "reason");
                            }
                        }
                        case "method" -> {
                            methodDepth++;
                            var holderId = reader.getAttributeValue(null, "holder");
                            var holder = holderId == null ? null : klass.get(holderId);
                            var name = reader.getAttributeValue(null, "name");
                            var leaf = isPointLeaf(holder, name);
                            if (matching) {
                                if (pendingFail) {
                                    if (leaf) {
                                        leavesFailed++;
                                        if (lastFail != null) {
                                            reasons.merge(lastFail, 1, Integer::sum);
                                        }
                                    }
                                    pendingFail = false;
                                    skipUntilDepth = methodDepth;
                                } else if (skipUntilDepth < 0 && leaf) {
                                    leavesInlined++;
                                }
                            }
                        }
                        default -> {
                        }
                    }
                } else if (type == XMLStreamConstants.END_ELEMENT) {
                    var tag = reader.getLocalName();
                    if ("task".equals(tag) || "compilation".equals(tag)) {
                        matching = false;
                        pendingFail = false;
                        skipUntilDepth = -1;
                    } else if ("method".equals(tag)) {
                        if (methodDepth == skipUntilDepth) {
                            skipUntilDepth = -1;
                        }
                        methodDepth--;
                    } else if ("inline_fail".equals(tag)) {
                        pendingFail = true;
                    }
                }
            }
            reader.close();
        } catch (Exception e) {
            return new InlineSummary("(parse error: " + e.getMessage() + ")", 0, 0, 0, 0, Map.of());
        }
        return new InlineSummary(String.join(" + ", roots), compilations, inlineFails, leavesInlined, leavesFailed, reasons);
    }

    /** @return whether the given fully-qualified method string is the point loop root. */
    private static boolean matchesLoopRoot(String method) {
        return loopRootLabel(method) != null;
    }

    /** @return a canonical label for the loop root, or {@code null} if not the point loop. */
    private static String loopRootLabel(String method) {
        if (method == null) {
            return null;
        }
        var sp = method.indexOf(' ');
        if (sp < 0) {
            return null;
        }
        var holder = method.substring(0, sp);
        var name = method.substring(sp + 1);
        // The task attribute uses dotted class names (layer.PointEmitter).
        if (holder.endsWith("layer.PointEmitter") && name.startsWith("emit")) {
            return "PointEmitter.emit";
        }
        if (holder.endsWith("layer.ColorEmitter") && name.startsWith("emit")) {
            return "ColorEmitter.emit";
        }
        if (holder.endsWith("layer.GeomPoint") && name.startsWith("render")) {
            return "GeomPoint.render";
        }
        return null;
    }

    /** @return whether a method holder/name is one of the point loop's per-point leaves. */
    private static boolean isPointLeaf(String holder, String name) {
        return name != null && POINT_LEAVES.contains(name);
    }

    private static String inliningMarkdown(Path baseDir, Path headDir) {
        var nl = System.lineSeparator();
        var b = readInlining(baseDir);
        var h = readInlining(headDir);
        var sb = new StringBuilder();
        sb.append("## JIT inlining: point emission loops").append(nl).append(nl);
        sb.append("Compiled from `-XX:+LogCompilation` (`build/benchmark/hotspot.log`, last rerun). ")
                .append("Per-point leaves (toPixel/toData/forward/draw) that fail to inline are left ")
                .append("as runtime calls: a load-independent early signal for the deep composed/matrix ")
                .append("chain regression. Informational only.").append(nl).append(nl);
        sb.append("| side | loop root | C2 compilations | inline_fail | leaves inlined / failed |").append(nl);
        sb.append("|---|---|---:|---:|---:|").append(nl);
        sb.append("| base | ").append(b.root()).append(" | ").append(b.compilations())
                .append(" | ").append(b.inlineFails())
                .append(" | ").append(b.leavesInlined()).append(" / ").append(b.leavesFailed())
                .append(" |").append(nl);
        sb.append("| head | ").append(h.root()).append(" | ").append(h.compilations())
                .append(" | ").append(h.inlineFails())
                .append(" | ").append(h.leavesInlined()).append(" / ").append(h.leavesFailed())
                .append(" |").append(nl);
        if (!h.reasons().isEmpty()) {
            sb.append(nl).append("Head inline_fail reasons: ")
                    .append(h.reasons().entrySet().stream()
                            .map(e -> e.getKey() + "×" + e.getValue())
                            .sorted().collect(Collectors.joining(", ")))
                    .append(nl);
        }
        return sb.toString();
    }

    private static String formatCv(double cv) {
        if (cv < 0) {
            return "-";
        }
        return String.format(Locale.ROOT, "%.1f%%", cv);
    }

    private static String formatPct(double pct) {
        return String.format(Locale.ROOT, "%.0f%%", pct);
    }
}