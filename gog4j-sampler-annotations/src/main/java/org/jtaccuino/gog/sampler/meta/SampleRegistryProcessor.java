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
package org.jtaccuino.gog.sampler.meta;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import javax.tools.StandardLocation;

/**
 * Collects {@link SamplePlot} annotations on example factory methods and writes
 * the sampler catalogue as a structured properties resource
 * ({@code META-INF/sampler/examples.properties}). No class-level marker
 * annotation is required; discovery is driven entirely by methods carrying
 * {@code @SamplePlot}. The processor bakes the method's own source text into
 * the resource, so the sampler needs no runtime file access.
 * <p>
 * Each entry records a comma-separated {@code tags} key whose values are the
 * fully qualified {@link SampleTag#id()}s of the tag enum constants referenced
 * by the annotation, in fixed kind order
 * ({@code dataset,geom,stat,coord,scale,position,theme,guide,feature,facet}).
 * The runtime {@code Tags} facade builds {@code Tag}s from those ids.
 */
public class SampleRegistryProcessor extends AbstractProcessor {

    private static final String PLOT_ANNOTATION = SamplePlot.class.getCanonicalName();

    private static final String PACKAGE = "";
    private static final String FILE = "META-INF/sampler/examples.properties";

    /** Fixed kind order for the tags key (dataset always first). */
    private static final TagKind[] TAG_ORDER = {
            TagKind.DATASET, TagKind.GEOM, TagKind.STAT, TagKind.COORD,
            TagKind.SCALE, TagKind.POSITION, TagKind.THEME, TagKind.GUIDE,
            TagKind.FEATURE, TagKind.FACET
    };

    private final List<PlotEntry> plots = new ArrayList<>();

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Set.of(PLOT_ANNOTATION);
    }

    @Override
    public Set<String> getSupportedOptions() {
        return Set.of("sampler.srcdir");
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    @SuppressWarnings("DoNotClaimAnnotations")
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (roundEnv.processingOver()) {
            if (!plots.isEmpty()) {
                try {
                    writeCatalogue(plots);
                } catch (IOException e) {
                    processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                            "could not write sampler catalogue: " + e);
                }
            }
            return false;
        }
        for (var method : roundEnv.getElementsAnnotatedWith(getType(PLOT_ANNOTATION))) {
            if (method.getKind() != ElementKind.METHOD) {
                continue;
            }
            var plot = method.getAnnotation(SamplePlot.class);
            if (plot == null) {
                continue;
            }
            plots.add(new PlotEntry(
                    (TypeElement) method.getEnclosingElement(),
                    method.getSimpleName().toString(),
                    plot));
        }
        return true;
    }

    private TypeElement getType(String name) {
        return processingEnv.getElementUtils().getTypeElement(name);
    }

    private void writeCatalogue(List<PlotEntry> entries) throws IOException {
        // Group by enclosing type to preserve declared method order within each
        // class; sort classes by qualified name for deterministic catalogue order.
        var byType = entries.stream().collect(Collectors.groupingBy(PlotEntry::type));

        var props = new Properties();
        var index = 0;

        var orderedTypes = byType.keySet().stream()
                .sorted(Comparator.comparing(t -> t.getQualifiedName().toString()))
                .toList();
        for (var type : orderedTypes) {
            // Index the entries by method name to preserve declared order.
            var entryByMethod = new HashMap<String, PlotEntry>();
            for (var e : byType.get(type)) {
                entryByMethod.put(e.methodName, e);
            }
            for (var method : type.getEnclosedElements()) {
                if (method.getKind() != ElementKind.METHOD) {
                    continue;
                }
                var entry = entryByMethod.get(method.getSimpleName().toString());
                if (entry == null) {
                    continue;
                }
                var key = "example." + index + ".";
                props.setProperty(key + "class", type.getQualifiedName().toString());
                props.setProperty(key + "method", entry.methodName);
                props.setProperty(key + "title", entry.plot.title());
                props.setProperty(key + "description", entry.plot.description());
                props.setProperty(key + "tags", entry.tagsString());
                var source = sourceFor(entry);
                props.setProperty(key + "source", Base64.getEncoder()
                        .encodeToString(source.getBytes(StandardCharsets.UTF_8)));
                index++;
            }
        }

        var file = processingEnv.getFiler().createResource(StandardLocation.CLASS_OUTPUT, PACKAGE, FILE);
        try (var out = file.openOutputStream()) {
            props.store(out, "Generated by SampleRegistryProcessor \u2014 do not edit by hand.");
        }
    }

    private String sourceFor(PlotEntry entry) {
        var source = readSource(entry.type);
        if (source == null || source.isBlank()) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.WARNING,
                    "no source found for " + entry.type.getQualifiedName(), entry.type);
            return "";
        }
        return SourceExtractor.extractMethod(source, entry.methodName);
    }

    private String readSource(TypeElement type) {
        var sourceDir = sourceRoot();
        if (sourceDir == null) {
            return null;
        }
        var relative = Path.of(type.getQualifiedName().toString().replace('.', '/') + ".java");
        var file = sourceDir.resolve(relative);
        if (!Files.isReadable(file)) {
            return null;
        }
        try {
            return Files.readString(file);
        } catch (IOException e) {
            return null;
        }
    }

    private Path sourceRoot() {
        var explicit = processingEnv.getOptions().get("sampler.srcdir");
        if (explicit != null && !explicit.isBlank()) {
            return Path.of(explicit);
        }
        var cwd = Path.of(System.getProperty("user.dir", ".")).toAbsolutePath();
        var candidate = cwd.resolve("src/main/java");
        return Files.isDirectory(candidate) ? candidate : null;
    }

    private record PlotEntry(TypeElement type, String methodName, SamplePlot plot) {

        /**
         * Builds the comma-separated tag id string in fixed kind order,
         * starting with the dataset.
         */
        String tagsString() {
            var ids = new ArrayList<String>();
            // Dataset is always first.
            ids.add(plot.dataset().id());
            // Per-kind arrays in the fixed TAG_ORDER order (skip DATASET, already added).
            for (var kind : TAG_ORDER) {
                if (kind == TagKind.DATASET) {
                    continue;
                }
                for (var tag : tagsForKind(kind)) {
                    ids.add(tag.id());
                }
            }
            return String.join(",", ids);
        }

        private SampleTag[] tagsForKind(TagKind kind) {
            return switch (kind) {
                case GEOM -> plot.geoms();
                case STAT -> plot.stats();
                case COORD -> plot.coords();
                case SCALE -> plot.scales();
                case FACET -> plot.facets();
                case POSITION -> plot.positions();
                case THEME -> plot.themes();
                case GUIDE -> plot.guides();
                case FEATURE -> plot.features();
                default -> new SampleTag[0];
            };
        }
    }
}
