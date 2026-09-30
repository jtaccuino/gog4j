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
package org.jtaccuino.gog.geometry;

/**
 * The along-axis direction of a regular point grid or a set of ridgelines,
 * matching the {@code direction} argument of the point-grid generation and
 * point-to-ridgeline steps ({@code "x"} or {@code "y"}).
 */
public enum GridDirection {

    /** The grid runs along {@code x} (a ridge per unique x, varying in y). */
    X,

    /** The grid runs along {@code y} (a ridge per unique y, varying in x). */
    Y
}
