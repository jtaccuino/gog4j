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
 * The Hardwood backend for gog4j.
 *
 * <p>{@link org.jtaccuino.gog.hardwood.HardwoodDataFrame} is the frame handle the
 * plotting engine accepts: a reference to one or more Apache Parquet files that
 * Hardwood reads lazily, plus an in-memory form used when a plot's statistical
 * transformation produces a frame. {@link org.jtaccuino.gog.hardwood.HardwoodDataExtractor}
 * adapts it to the engine's {@code DataExtractor} SPI and is registered as a
 * {@link java.util.ServiceLoader} provider.
 */
package org.jtaccuino.gog.hardwood;
