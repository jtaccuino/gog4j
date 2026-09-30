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
package org.jtaccuino.gog.scale;

import org.jtaccuino.gog.ConfigTarget;
import org.jtaccuino.gog.Plot;

/**
 * Functional interface for configuring scale (axis break/limit) specifications on a {@link ConfigTarget}.
 */
@FunctionalInterface
public interface ScaleConfigurator {

    /**
     * Applies the scale configuration to the given target.
     *
     * @param target the target {@link ConfigTarget} instance
     */
    void configure(ConfigTarget<?> target);
}
