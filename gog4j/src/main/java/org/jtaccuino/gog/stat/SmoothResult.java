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

/**
 * The raw result of a smoothing computation: the fitted curve and, when the
 * confidence band is enabled, its lower/upper envelopes over the same X grid.
 * <p>
 * This is an internal transport type shared between the smoothing models
 * ({@link SmoothModels}) and the {@link StatSmooth} stat; callers should read
 * the output through {@link StatData} rather than reaching for these arrays.
 *
 * @param xPoints the x-coordinates of the fitted curve
 * @param yPoints the fitted y-values at each x-coordinate
 * @param yMin    the lower envelope of the confidence interval
 * @param yMax    the upper envelope of the confidence interval
 */
@SuppressWarnings("ArrayRecordComponent") // transient results, never exposed mutably
record SmoothResult(double[] xPoints, double[] yPoints, double[] yMin, double[] yMax) {
}
