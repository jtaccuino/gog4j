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
package org.jtaccuino.gog.stat;
import java.util.List;

/**
 * Functional interface for custom smoothing/prediction strategies.
 * Implementations predict a Y value for a given target X based on observed
 * data points.
 */
@FunctionalInterface
public interface SmoothingFunction {

    /**
     * Predicts the Y value at the target X coordinate.
     *
     * @param tx the target X value
     * @param x  the observed X data points
     * @param y  the observed Y data points
     * @return the predicted Y value
     */
    double predict(double tx, List<Double> x, List<Double> y);
}
