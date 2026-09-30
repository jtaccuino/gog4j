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
 * Example datasets read with Hardwood.
 *
 * <p>The demo CSV fixtures are packaged as ZSTD Parquet files by the
 * {@code convertParquet} build task; this package's loaders open them as
 * {@code HardwoodDataFrame}s and (for the GWAS dataset) derive the plot-ready
 * columns. The classes mirror the DFLib backend's loaders so the same examples
 * can be rendered from either frame type.
 */
package org.jtaccuino.gog.hardwood.data;
