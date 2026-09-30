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
 * The domain a fitted 3D surface covers (the {@code domain} parameter of
 * {@code Stats.smooth3d()}).
 */
public enum SmoothDomain {

    /** Restrict the fitted surface to the convex hull of the data. */
    CHULL,

    /** Cover the full rectangular evaluation grid. */
    FULL
}
