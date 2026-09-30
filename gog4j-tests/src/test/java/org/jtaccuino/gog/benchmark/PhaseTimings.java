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
package org.jtaccuino.gog.benchmark;

import static java.nio.file.StandardOpenOption.APPEND;
import static java.nio.file.StandardOpenOption.CREATE;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import jdk.jfr.Recording;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordingFile;

/**
 * Test-facing handle for a single JFR benchmark run. One {@code PhaseTimings} is created
 * per {@link MeasurePhases} test method; the test body calls
 * {@link #measure(ThrowingRunnable)} with a single-pass workload, and the enclosing
 * {@link JfrBenchmarkExtension} prints the per-phase best/avg/p95 report on completion.
 * The JFR recording is started only for the measured passes, so the warmup passes are
 * excluded from the reported numbers.
 */
public final class PhaseTimings {

    private static final String[] EVENT_NAMES = {
        "org.jtaccuino.gog.jfr.PlotRender",
        "org.jtaccuino.gog.jfr.MatrixRender",
        "org.jtaccuino.gog.jfr.ComposedRender",
        "org.jtaccuino.gog.jfr.PreparePhase",
        "org.jtaccuino.gog.jfr.LayerPrepare",
        "org.jtaccuino.gog.jfr.CellExtentCollection",
        "org.jtaccuino.gog.jfr.GuideCollection",
        "org.jtaccuino.gog.jfr.Layout",
        "org.jtaccuino.gog.jfr.FacetLayout",
        "org.jtaccuino.gog.jfr.FacetPartition",
        "org.jtaccuino.gog.jfr.FacetPanel",
        "org.jtaccuino.gog.jfr.PanelPrep",
        "org.jtaccuino.gog.jfr.LayerRender",
        "org.jtaccuino.gog.jfr.PointRender",
        "org.jtaccuino.gog.jfr.SmoothFit",
    };

    private final Recording recording;
    private final String label;
    private final int warmup;
    private final int measured;
    private final Map<String, List<Long>> byPhase = new LinkedHashMap<>();
    private boolean started;

    PhaseTimings(Recording recording, String label, int warmup, int measured) {
        this.recording = recording;
        this.label = label;
        this.warmup = warmup;
        this.measured = measured;
        for (var name : EVENT_NAMES) {
            byPhase.put(name.substring(name.lastIndexOf('.') + 1), new ArrayList<>());
        }
    }

    /**
     * Runs {@code warmup} passes (unrecorded), starts the recording, then runs
     * {@code measured} passes (recorded).
     */
    public void measure(ThrowingRunnable onePass) throws Exception {
        for (var name : EVENT_NAMES) {
            recording.enable(name);
        }
        for (int i = 0; i < warmup; i++) {
            onePass.run();
        }
        recording.start();
        started = true;
        for (int i = 0; i < measured; i++) {
            onePass.run();
        }
    }

    void report() throws IOException {
        if (!started) {
            return;
        }
        recording.stop();
        Path recordingFile = null;
        try {
            recordingFile = Files.createTempFile("gog-bench-" + label, ".jfr");
            recording.dump(recordingFile);
            try (var file = new RecordingFile(recordingFile)) {
                while (file.hasMoreEvents()) {
                    RecordedEvent evt = file.readEvent();
                    var type = evt.getEventType().getName();
                    for (var name : EVENT_NAMES) {
                        if (type.equals(name)) {
                            byPhase.get(name.substring(name.lastIndexOf('.') + 1)).add(evt.getDuration().toNanos());
                            break;
                        }
                    }
                }
            }
        } finally {
            if (recordingFile != null) {
                Files.deleteIfExists(recordingFile);
            }
        }
        System.out.printf("%n=== %s ===%n", label);
        System.out.printf("  %-20s %12s %12s %8s%n", "phase", "best(us)", "avg(us)", "cv%");
        // Report the phases slowest-first so the bottleneck is the first line.
        var sorted = byPhase.entrySet().stream()
                .sorted(Comparator.comparingDouble((Map.Entry<String, List<Long>> e) -> avgNs(e.getValue())).reversed())
                .toList();
        for (var entry : sorted) {
            var ns = entry.getValue();
            System.out.printf("  %-20s %12.0f %12.0f %8.1f%n", entry.getKey(),
                    bestNs(ns) / 1e3, avgNs(ns) / 1e3, cvPct(ns));
        }
        writeSummaryJson();
    }

    private void writeSummaryJson() throws IOException {
        var dir = Path.of("build", "benchmark");
        Files.createDirectories(dir);
        var body = new StringBuilder();
        body.append("{\"plot\":\"").append(label).append("\",\"phases\":{");
        boolean first = true;
        for (var entry : byPhase.entrySet()) {
            if (!first) {
                body.append(',');
            }
            first = false;
            appendPhase(body, entry.getKey(), entry.getValue());
        }
        body.append("}}");
        var fileName = label.replaceAll("[^A-Za-z0-9._-]", "_") + ".json";
        Files.writeString(dir.resolve(fileName), body);
        Files.writeString(dir.resolve(fileName + "l"), body.append('\n'), CREATE, APPEND);
    }

    private static void appendPhase(StringBuilder sb, String phase, List<Long> ns) {
        int n = ns.size();
        double avgNs = avgNs(ns);
        sb.append('"').append(phase).append("\":{\"bestUs\":")
                .append(Math.round(bestNs(ns) / 1e3))
                .append(",\"tmeanUs\":").append(Math.round(tmeanNs(ns, 1) / 1e3))
                .append(",\"avgUs\":").append(Math.round(avgNs / 1e3))
                .append(",\"sdUs\":").append(Math.round(sdNs(ns) / 1e3))
                .append(",\"cvPct\":").append(cvPct(ns))
                .append(",\"p50Us\":").append(Math.round(percentileNs(ns, 0.50) / 1e3))
                .append(",\"p95Us\":").append(Math.round(percentileNs(ns, 0.95) / 1e3))
                .append(",\"n\":").append(n)
                .append('}');
    }

    private static long bestNs(List<Long> ns) {
        return ns.stream().mapToLong(Long::longValue).min().orElse(0);
    }

    /**
     * Mean of the samples with the {@code k} smallest and {@code k} largest removed,
     * so one-sided slow-sample spikes (GC pauses, scheduling) do not skew the central
     * value. Falls back to the plain mean when there are fewer than {@code 2k+1} samples.
     */
    private static double tmeanNs(List<Long> ns, int k) {
        int n = ns.size();
        if (n <= 2 * k) {
            return avgNs(ns);
        }
        var sorted = new ArrayList<>(ns);
        sorted.sort(Comparator.naturalOrder());
        long sum = 0;
        for (int i = k; i < n - k; i++) {
            sum += sorted.get(i);
        }
        return (double) sum / (n - 2 * k);
    }

    private static double avgNs(List<Long> ns) {
        return ns.stream().mapToLong(Long::longValue).average().orElse(0);
    }

    private static double sdNs(List<Long> ns) {
        int n = ns.size();
        if (n < 2) {
            return 0;
        }
        double avg = avgNs(ns);
        double sumSquares = 0;
        for (long v : ns) {
            double d = v - avg;
            sumSquares += d * d;
        }
        return Math.sqrt(sumSquares / (n - 1));
    }

    private static double cvPct(List<Long> ns) {
        double avg = avgNs(ns);
        if (avg == 0) {
            return 0;
        }
        return Math.round(sdNs(ns) / avg * 1000.0) / 10.0;
    }

    private static long percentileNs(List<Long> ns, double q) {
        if (ns.isEmpty()) {
            return 0;
        }
        var sorted = new ArrayList<>(ns);
        sorted.sort(Comparator.naturalOrder());
        int idx = (int) Math.ceil(q * (sorted.size() - 1));
        return sorted.get(idx);
    }

    /**
     * {@link Runnable} that may throw checked exceptions, mirroring the render helpers.
     */
    @FunctionalInterface
    public interface ThrowingRunnable {
        void run() throws Exception;
    }
}
