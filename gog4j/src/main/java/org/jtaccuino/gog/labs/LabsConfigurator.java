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
package org.jtaccuino.gog.labs;

import org.jtaccuino.gog.Plot;

/**
 * Functional interface for configuring label specifications on an {@link Plot}.
 *
 * @param <DF> the DataFrame type
 */
@FunctionalInterface
public interface LabsConfigurator<DF> {

    /**
     * Applies the label configuration to the given plot.
     *
     * @param plot the target {@link Plot} instance
     */
    void configure(Plot<DF> plot);
}
