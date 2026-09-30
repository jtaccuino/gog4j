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

import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import org.jtaccuino.gog.controls.PlotSpec;

/**
 * Metadata for one example plot in the sampler.
 * <p>
 * A {@link SamplerExample} does not hold the rendered plot itself; the
 * {@link #factory()} supplier builds a fresh plot on demand so the
 * sampler can discard and recreate the chart whenever the selection changes.
 *
 * @param title the short display title, e.g. {@code "Notched Boxplot"}
 * @param description an optional one-line description, may be {@code null}
 * @param methodName the name of the {@code public static} factory method
 *     in the declaring class
 * @param factory supplier that builds a fresh plot for this example
 * @param tags the tags attached to this example, the dataset tag first
 * @param source the Java source of the factory method, or an empty string if
 *     the source file could not be resolved
 */
public record SamplerExample(
        String title,
        String description,
        String methodName,
        Supplier<Object> factory,
        List<Tag> tags,
        String source) {

    /** Compact constructor rejecting null components. */
    public SamplerExample {
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(methodName, "methodName");
        Objects.requireNonNull(factory, "factory");
        tags = List.copyOf(tags);
        source = source == null ? "" : source;
    }

    /**
     * Returns the short display title.
     *
     * @return the title
     */
    public String displayTitle() {
        return title;
    }

    /**
     * Adapts this example to the data-agnostic {@link PlotSpec} the reusable
     * {@code PlotCard} control consumes, mapping the typed tags to plain labels.
     *
     * @return a {@link PlotSpec} sharing this example's factory and source
     */
    public PlotSpec toSpec() {
        return new PlotSpec(title, description, methodName, factory,
                tags.stream().map(Tag::label).toList(), source);
    }
}
