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

/**
 * A data source offered by the plot builder application. The builder is data-agnostic:
 * {@link #data()} returns whatever object the library's
 * {@code DataExtractorRegistry} knows how to render (a DataFrame in the built-in
 * dflib adapter), while the column and value access serve the mapping controls
 * and manual scales. {@link #loaderExpr()} and {@link #loaderImport()} are the
 * Java text the code generator emits so the produced snippet loads the same
 * data.
 */
public interface PlotDataset {

    /** The human-readable name shown in the dataset picker. */
    String displayName();

    /** The column names available for aesthetic mappings. */
    List<String> columns();

    /** The distinct values of a column in first-appearance order. */
    List<String> distinctValues(String column);

    /** The data object to plot (any type with a registered extractor). */
    Object data();

    /** A Java expression that loads the data, e.g. {@code MpgDatasets.loadMpg()}. */
    String loaderExpr();

    /** The import the loader expression needs, or an empty string. */
    String loaderImport();
}
