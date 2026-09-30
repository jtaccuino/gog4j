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
package org.jtaccuino.gog.sampler.registry;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.Supplier;

/**
 * Runtime registry of all sampler examples.
 * <p>
 * Each examples module generates its own
 * {@code META-INF/sampler/examples.properties} resource at compile time via
 * {@code SampleRegistryProcessor} from the {@code SamplePlot} annotations. This
 * class merges every such resource on the classpath and builds a
 * {@link SamplerExample} per entry, resolving each factory as a reflective
 * {@code Supplier} of the recorded {@code public static} method's return value.
 */
public final class ExampleRegistry {

    private static final String RESOURCE = "META-INF/sampler/examples.properties";

    private static final List<SamplerExample> EXAMPLES = load();

    private ExampleRegistry() {
    }

    /**
     * Returns all registered examples, ordered by declaring class and then
     * method name so the catalogue is stable regardless of classpath order.
     *
     * @return the examples in catalogue order
     */
    public static List<SamplerExample> all() {
        return EXAMPLES;
    }

    private static List<SamplerExample> load() {
        var examples = new ArrayList<Map.Entry<String, SamplerExample>>();
        try {
            var resources = ExampleRegistry.class.getClassLoader().getResources(RESOURCE);
            while (resources.hasMoreElements()) {
                var url = resources.nextElement();
                try (var in = url.openStream()) {
                    var props = new Properties();
                    props.load(in);
                    int index = 0;
                    while (props.containsKey("example." + index + ".class")) {
                        examples.add(Map.entry(props.getProperty("example." + index + ".class"),
                                readExample(props, index)));
                        index++;
                    }
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException("cannot load sampler catalogue " + RESOURCE, e);
        }
        examples.sort(Map.Entry.<String, SamplerExample>comparingByKey()
                .thenComparing(entry -> entry.getValue().methodName()));
        return examples.stream().map(Map.Entry::getValue).toList();
    }

    private static SamplerExample readExample(Properties props, int index) {
        var key = "example." + index + ".";
        var declaringClass = props.getProperty(key + "class");
        var methodName = props.getProperty(key + "method");
        var title = props.getProperty(key + "title");
        var description = props.getProperty(key + "description");
        var source = new String(Base64.getDecoder().decode(props.getProperty(key + "source")),
                StandardCharsets.UTF_8);

        // The single comma-separated tags list already carries the dataset tag
        // first; every id resolves against the enum-derived catalogue.
        var tags = new ArrayList<Tag>();
        for (var id : props.getProperty(key + "tags").split(",", -1)) {
            if (!id.isBlank()) {
                tags.add(Tags.byName(id));
            }
        }

        return new SamplerExample(title, description, methodName,
                factoryFor(declaringClass, methodName), List.copyOf(tags), source);
    }

    private static Supplier<Object> factoryFor(String declaringClass, String methodName) {
        return () -> {
            try {
                var method = Class.forName(declaringClass).getMethod(methodName);
                return method.invoke(null);
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException(
                        "cannot create plot " + declaringClass + "#" + methodName, e);
            }
        };
    }
}
