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
package org.jtaccuino.gog.data;

/**
 * A library-internal, stateless DataFrame placeholder for plots that carry no
 * data at all, such as a standalone {@code Geoms.function()}.
 * <p>
 * Such a plot has no columns to extract, so the type exposes nothing beyond the
 * object identity that {@link EmptyDataFrameExtractor} keys off. The rendering
 * pipeline already short-circuits every data access when the aesthetic mapping
 * is empty, so this frame is never actually read — it only exists to satisfy
 * the {@code Plot}/{@code DataExtractor} plumbing with a concrete, non-null
 * frame.
 */
public final class EmptyDataFrame {

    /** The single shared instance; the frame is stateless. */
    public static final EmptyDataFrame INSTANCE = new EmptyDataFrame();

    /** Private constructor: use {@link #INSTANCE}. */
    private EmptyDataFrame() {
    }
}
