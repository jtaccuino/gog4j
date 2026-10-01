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

/**
 * Minimal data-frame abstractions used by the core engine.
 *
 * <p>Defines the internal access model a plot is built against and a data-less
 * frame for plots such as function curves, so the core never depends on a
 * concrete DataFrame library. Concrete integrations are provided through the
 * service-provider interface in {@code org.jtaccuino.gog.spi}.
 *
 * <p>{@link org.jtaccuino.gog.data.Values} is the shared coercion of a raw
 * value to its numeric position and display label, and
 * {@link org.jtaccuino.gog.data.Temporals} is the matching temporal
 * conversion, tick, and formatting logic. Keeping both here means a value's
 * position on an axis and the text beside it are derived from the same reading.
 */
package org.jtaccuino.gog.data;
