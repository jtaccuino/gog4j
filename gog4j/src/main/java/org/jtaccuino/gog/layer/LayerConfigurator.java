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
package org.jtaccuino.gog.layer;

import org.jtaccuino.gog.ConfigTarget;
import org.jtaccuino.gog.Plot;

/**
 * Functional interface for registering and configuring geometry layers within a {@link ConfigTarget}.
 * <p>
 * Enables fluid method chaining inside {@code Plot.geoms()} and
 * {@code PlotDescriptor.geoms()} — the receiver surface is shared so the same
 * declarative builders configure either the plot node or its lightweight spec.
 *
 * @param <DF> the DataFrame type representing the underlying dataset
 */
@FunctionalInterface
public interface LayerConfigurator<DF> {
    /**
     * Configures and registers geometry layers onto the specified target.
     *
     * @param target the target {@link ConfigTarget} instance
     */
    void configure(ConfigTarget<DF> target);
}
