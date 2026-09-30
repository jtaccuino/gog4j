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
package org.jtaccuino.gog.controls;

import java.util.List;
import java.util.function.Supplier;

/**
 * A declarative description of one plot for a {@link PlotCard}: the chrome
 * (title, description, tags, source) and a factory that produces the figure.
 * Data-agnostic — the factory may return any object the library's
 * {@code DataExtractorRegistry} knows how to render — so the control package
 * stays free of any concrete DataFrame type.
 *
 * @param title       the display title
 * @param description the optional description line
 * @param methodName  the factory method name shown on the source drawer
 * @param factory     builds the figure to render
 * @param tags        display tags (labels only)
 * @param source      the Java source shown in the source drawer
 */
public record PlotSpec(String title, String description, String methodName,
        Supplier<Object> factory, List<String> tags, String source) {
}
