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

import java.util.List;
import org.jtaccuino.gog.spi.DataExtractor;

/**
 * The {@link DataExtractor} for {@link EmptyDataFrame}, the placeholder frame
 * behind data-less plots such as a standalone {@code Geoms.function()}.
 * <p>
 * Every accessor reports an empty result, which is exactly what the rendering
 * pipeline expects: with no aesthetic mapped to a column, no code path ever
 * reads a column from this frame. The interface defaults supply the remaining
 * behaviour — partitioning yields the single {@code "GLOBAL"} group and column
 * type/min-max fall back to benign defaults.
 */
public class EmptyDataFrameExtractor implements DataExtractor<EmptyDataFrame> {

    /** Creates the extractor, normally picked up through the service registry. */
    public EmptyDataFrameExtractor() {
    }

    @Override
    public boolean supports(Class<?> dataFrameType) {
        return EmptyDataFrame.class.isAssignableFrom(dataFrameType);
    }

    @Override
    public List<?> getColumn(EmptyDataFrame df, String columnName) {
        return List.of();
    }

    @Override
    public int getRowCount(EmptyDataFrame df) {
        return 0;
    }
}
