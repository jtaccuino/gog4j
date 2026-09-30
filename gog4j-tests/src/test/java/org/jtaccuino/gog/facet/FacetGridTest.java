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
package org.jtaccuino.gog.facet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.dflib.DataFrame;
import org.dflib.Series;
import org.jtaccuino.gog.Facets;
import org.jtaccuino.gog.dflib.DflibDataExtractor;
import org.jtaccuino.gog.facet.GridOptions.Axes;
import org.jtaccuino.gog.facet.GridOptions.GridSwitch;
import org.jtaccuino.gog.facet.GridOptions.Scale;
import org.jtaccuino.gog.facet.GridOptions.Space;
import org.junit.jupiter.api.Test;

class FacetGridTest {

    private final DflibDataExtractor extractor = new DflibDataExtractor();

    @Test
    void wrapKeepsSingleColumnName() {
        FacetSpec spec = Facets.wrap("cyl", 3);
        assertFalse(spec.isGrid());
        assertEquals("cyl", spec.getColumnName());
    }

    @Test
    void gridExposesRowAndColumnVars() {
        FacetSpec spec = Facets.grid("drv", "cyl");
        assertTrue(spec.isGrid());
        assertEquals(List.of("drv"), spec.getRowVars());
        assertEquals(List.of("cyl"), spec.getColVars());
        assertNull(spec.getColumnName());
    }

    @Test
    void singleDimensionGridKeepsColumnNameForLayers() {
        FacetSpec spec = FacetSpec.grid(List.of("cyl"), List.of(), GridOptions.defaults());
        assertEquals("cyl", spec.getColumnName());
        assertEquals("cyl",
                FacetSpec.grid(List.of(), List.of("cyl"), GridOptions.defaults()).getColumnName());
    }

    @Test
    void rowKeysAreNaturallyOrderedNotByFirstAppearance() {
        DataFrame df = DataFrame.byColumn("drv", "cyl").of(
                Series.of("f", "r", "4", "f", "4"),
                Series.of(4.0, 8.0, 6.0, 8.0, 5.0));
        FacetGrid<DataFrame> grid = extractor.partitionGrid(df, List.of("drv"), List.of("cyl"), Set.of());
        assertEquals(List.of(List.of("4"), List.of("f"), List.of("r")), grid.rows());
        assertEquals(List.of(List.of(4.0), List.of(5.0), List.of(6.0), List.of(8.0)), grid.cols());
    }

    @Test
    void nullsSortLastInRowKeys() {
        DataFrame df = DataFrame.byColumn("drv", "cyl").of(
                Series.of("f", null, "4", null),
                Series.of(4.0, 8.0, 6.0, 5.0));
        FacetGrid<DataFrame> grid = extractor.partitionGrid(df, List.of("drv"), List.of("cyl"), Set.of());
        assertEquals(List.of(List.of("4"), List.of("f"), Collections.singletonList(null)), grid.rows());
    }

    @Test
    void defaultsMatchGgplot2() {
        GridOptions options = GridOptions.defaults();
        assertSame(Scale.FIXED, options.scale());
        assertSame(Space.FIXED, options.space());
        assertSame(Axes.MARGINS, options.axes());
        assertSame(GridSwitch.NONE, options.switchPreset());
        assertTrue(options.asTable());
        assertTrue(options.drop());
        assertFalse(options.margins());
    }

    @Test
    void partitionsGridByRowAndColumnVars() {
        DataFrame df = DataFrame.byColumn("drv", "year", "cyl", "hwy").of(
                Series.of("f", "f", "f", "r", "r", "4"),
                Series.of("2008", "2008", "1999", "1999", "2008", "2008"),
                Series.of(8.0, 4.0, 4.0, 8.0, 8.0, 4.0),
                Series.of(26.0, 33.0, 30.0, 17.0, 19.0, 29.0));
        FacetGrid<DataFrame> grid = extractor.partitionGrid(df, List.of("drv", "year"), List.of("cyl"), Set.of());
        assertEquals(2, grid.numCols());
        assertEquals(5, grid.numRows());
        DataFrame panel = grid.panel(List.of("f", "2008"), List.of(8.0));
        assertEquals(1, extractor.getRowCount(panel));
        assertEquals(26.0, extractor.getColumn(panel, "hwy").get(0));
        assertEquals(0, extractor.getRowCount(grid.panel(List.of("r", "1999"), List.of(4.0))));
    }

    @Test
    void marginsAddAggregateRowAndColumnKeys() {
        DataFrame df = mtcarsLike();
        FacetGrid<DataFrame> grid = extractor.partitionGrid(df, List.of("vs", "am"), List.of("gear"), Set.of("vs", "am"));
        List<List<Object>> rowKeys = grid.rows();
        assertTrue(rowKeys.stream().anyMatch(row -> row.contains(FacetGrid.ALL)));
        assertTrue(rowKeys.stream().anyMatch(row -> row.get(0).equals(FacetGrid.ALL) && !row.get(1).equals(FacetGrid.ALL)));
        assertTrue(rowKeys.stream().anyMatch(row -> !row.get(0).equals(FacetGrid.ALL) && row.get(1).equals(FacetGrid.ALL)));
    }

    @Test
    void grandMarginRowAggregatesAcrossTheMarginedVariable() {
        DataFrame df = mtcarsLike();
        FacetGrid<DataFrame> grid = extractor.partitionGrid(df, List.of("vs", "am"), List.of("gear"), Set.of("am"));
        List<Object> grandRow = List.of(FacetGrid.ALL, FacetGrid.ALL);
        assertEquals(2, extractor.getRowCount(grid.panel(grandRow, List.of(4.0))));
        assertEquals(3, extractor.getRowCount(grid.panel(grandRow, List.of(3.0))));
    }

    @Test
    void partialMarginLeavesNonMarginedDimensionUnchanged() {
        DataFrame df = mtcarsLike();
        FacetGrid<DataFrame> grid = extractor.partitionGrid(df, List.of("vs", "am"), List.of("gear"), Set.of("am"));
        assertFalse(grid.cols().stream().anyMatch(FacetGrid::isMarginCol));
        assertTrue(grid.rows().stream().anyMatch(FacetGrid::isMarginRow));
    }

    @Test
    void singleCellSubjectWithoutVariablesHoldsWholeFrame() {
        DataFrame df = mtcarsLike();
        FacetGrid<DataFrame> grid = extractor.partitionGrid(df, List.of(), List.of(), Set.of());
        assertEquals(1, grid.numRows());
        assertEquals(1, grid.numCols());
        assertEquals(extractor.getRowCount(df), extractor.getRowCount(grid.panel(List.of(), List.of())));
    }

    @Test
    void gridOptionsFluentChainRewrites() {
        GridOptions options = GridOptions.defaults()
                .withScale(Scale.FREE)
                .withSpace(Space.FREE)
                .withMargins()
                .withSwitch(GridSwitch.BOTH)
                .withAsTable(false)
                .withAxes(Axes.ALL);
        assertSame(Scale.FREE, options.scale());
        assertSame(Space.FREE, options.space());
        assertSame(GridSwitch.BOTH, options.switchPreset());
        assertSame(Axes.ALL, options.axes());
        assertTrue(options.margins());
        assertFalse(options.asTable());
        assertTrue(options.marginVars().isEmpty());
    }

    @Test
    void namedMarginsListOnlyRequestedVars() {
        GridOptions options = GridOptions.defaults().withMargins("am", "vs");
        assertEquals(Set.of("am", "vs"), options.marginVars());
    }

    private static DataFrame mtcarsLike() {
        return DataFrame.byColumn("vs", "am", "gear", "mpg").of(
                Series.of(0.0, 0.0, 0.0, 1.0, 1.0, 1.0, 0.0, 1.0),
                Series.of(0.0, 1.0, 0.0, 1.0, 0.0, 0.0, 1.0, 0.0),
                Series.of(4.0, 5.0, 3.0, 5.0, 3.0, 4.0, 5.0, 3.0),
                Series.of(21.0, 15.0, 18.0, 30.0, 20.0, 26.0, 16.0, 24.0));
    }
}
