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
package org.jtaccuino.gog;

import static org.jtaccuino.gog.Aes.aes;
import static org.jtaccuino.gog.test.JavaFxToolkitExtension.onFxThread;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import javafx.scene.canvas.Canvas;
import javafx.scene.layout.Pane;
import org.dflib.DataFrame;
import org.dflib.Series;
import org.jtaccuino.gog.render.SvgExporter;
import org.jtaccuino.gog.spi.DataExtractor;
import org.jtaccuino.gog.test.JavaFxToolkitExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Verifies {@link PlotMatrix}: cell layout, resolved {@link PlotOptions} per
 * grid position, and end-to-end SVG rendering via {@code renderTo}.
 * Guarded on a live JavaFX toolkit like the other render tests.
 */
@ExtendWith(JavaFxToolkitExtension.class)
class PlotMatrixTest {

    private static final int WIDTH = 1200;
    private static final int HEIGHT = 900;

    private static DataFrame mpg() {
        return DataFrame.byColumn("displ", "hwy", "drv")
                .of(
                        Series.of(1.8, 2.0, 2.8, 3.5, 4.6, 5.3),
                        Series.of(29, 31, 26, 25, 21, 18),
                        Series.of("f", "f", "4", "r", "r", "4"));
    }

    @Test
    void dimensionsAreCorrect() {
        var matrix = new PlotMatrix<DataFrame>(2, 3);
        assertEquals(2, matrix.numRows());
        assertEquals(3, matrix.numCols());
    }

    @Test
    void defaultOptionsAreMargins() {
        var matrix = new PlotMatrix<DataFrame>(2, 3);
        assertSame(PlotOptions.OuterLabels.MARGINS, matrix.options().outerLabels());
    }

    @Test
    void resolvedMarginsSkipsInnerLabels() {
        var matrix = new PlotMatrix<DataFrame>(3, 3);
        var bottomLeft = matrix.resolvedOptions(2, 0);
        assertTrue(bottomLeft.drawXLabels(), "bottom-left should draw x labels");
        assertTrue(bottomLeft.drawYLabels(), "bottom-left should draw y labels");

        var inner = matrix.resolvedOptions(1, 1);
        assertFalse(inner.drawXLabels(), "inner cell must not draw x labels under MARGINS");
        assertFalse(inner.drawYLabels(), "inner cell must not draw y labels under MARGINS");

        var topRight = matrix.resolvedOptions(0, 2);
        assertFalse(topRight.drawXLabels(), "top-right must not draw x labels");
        assertFalse(topRight.drawYLabels(), "top-right must not draw y labels");
    }

    @Test
    void resolvedAllLabelsEveryCell() {
        var matrix = new PlotMatrix<DataFrame>(2, 2);
        matrix.options(PlotOptions.all());
        for (int r = 0; r < 2; r++) {
            for (int c = 0; c < 2; c++) {
                var resolved = matrix.resolvedOptions(r, c);
                assertTrue(resolved.drawXLabels());
                assertTrue(resolved.drawYLabels());
            }
        }
    }

    @Test
    void cellAccessorsAndCellsArray() {
        var df = mpg();
        var d1 = Ggplot.plot(df, aes().x("displ").y("hwy")).geoms(Geoms.point());
        var d2 = Ggplot.plot(df, aes().x("displ").y("hwy")).geoms(Geoms.smooth());
        var d3 = Ggplot.plot(df, aes().x("hwy").y("displ")).geoms(Geoms.point());
        var d4 = Ggplot.plot(df, aes().x("hwy").y("displ")).geoms(Geoms.smooth());

        var matrix = PlotMatrix.<DataFrame>create(2, 2).cells(d1, d2, d3, d4);
        assertSame(d1, matrix.cell(0, 0));
        assertSame(d4, matrix.cell(1, 1));
    }

    @Test
    void baseDescriptorCopiedPerCell() {
        var df = mpg();
        var base = Ggplot.plot(df, aes().x("displ").y("hwy"))
                .geoms(Geoms.point())
                .labs("Base");

        var matrix = PlotMatrix.<DataFrame>create(2, 2).base(base);
        matrix.cells(
                (d, r, c) -> d.labs("Cell " + r + "," + c),
                (d, r, c) -> d.labs("Cell " + r + "," + c),
                (d, r, c) -> d.labs("Cell " + r + "," + c),
                (d, r, c) -> d.labs("Cell " + r + "," + c));

        assertEquals("Cell 0,0", matrix.cell(0, 0).labs().title());
        assertEquals("Cell 1,1", matrix.cell(1, 1).labs().title());
        assertNotNull(matrix.cell(0, 0).data(), "copy retains the dataset reference");
    }

    @Test
    void renderToSvgProducesExpectedPanels() throws Exception {
        var df = mpg();
        var matrix = PlotMatrix.<DataFrame>create(2, 2).base(
                Ggplot.plot(df, aes().x("displ").y("hwy")).geoms(Geoms.point()));
        matrix.cells(
                (d, r, c) -> {},
                (d, r, c) -> {},
                (d, r, c) -> {},
                (d, r, c) -> {});

        var svg = renderSvg(matrix);
        assertNotNull(svg, "SVG must be produced");
        assertTrue(svg.contains("<svg"), "output must be an SVG document");
        assertTrue(svg.length() > 500, "matrix SVG must contain non-trivial geometry");
    }

    @Test
    void ggplotFactoryCreatesMatrix() {
        var df = mpg();
        var matrix = Ggplot.ggplot(df, Aes.aes().x("displ").y("hwy"), 2, 3);
        assertEquals(2, matrix.numRows());
        assertEquals(3, matrix.numCols());
        assertNotNull(matrix.options());
    }

    @Test
    void invalidDimensionsThrow() {
        try {
            new PlotMatrix<>(0, 3);
            throw new AssertionError("expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            // expected
        }
    }

    @Test
    void cellCountMismatchThrows() {
        var matrix = PlotMatrix.<DataFrame>create(2, 2);
        try {
            matrix.cells(Ggplot.plot(mpg(), aes()));
            throw new AssertionError("expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            // expected
        }
    }

    // ─── matrixPlot ───────────────────────────────────────────────────────

    @Test
    void pearsonPerfectPositiveAndNegative() {
        assertEquals(1.0, Ggplot.pearson(List.of(1.0, 2.0, 3.0), List.of(2.0, 4.0, 6.0)), 1e-12);
        assertEquals(-1.0, Ggplot.pearson(List.of(1.0, 2.0, 3.0), List.of(6.0, 4.0, 2.0)), 1e-12);
    }

    @Test
    void pearsonSkipsNulls() {
        assertEquals(1.0, Ggplot.pearson(
                Arrays.asList(1.0, null, 2.0, 3.0, null),
                Arrays.asList(2.0, "junk", 4.0, 6.0, 100.0)), 1e-12);
    }

    @Test
    void matrixBuildsSquareMatrix() {
        var matrix = Ggplot.matrixPlot(mpg(), aes(), "displ", "hwy");
        assertEquals(2, matrix.numRows());
        assertEquals(2, matrix.numCols());
        assertNotNull(matrix.cell(0, 0));
        assertNotNull(matrix.cell(1, 1));
    }

    @Test
    void matrixDiagonalIsDensity() {
        var matrix = Ggplot.matrixPlot(mpg(), aes(), "displ", "hwy");
        var diag = matrix.cell(0, 0);
        assertEquals("displ", diag.aes().x());
        assertTrue(diag.geoms().get(0).getClass().getSimpleName().equals("GeomDensity"),
                "continuous diagonal must carry a density layer");
    }

    @Test
    void matrixLowerTriangleIsPlainScatter() throws Exception {
        var matrix = Ggplot.matrixPlot(mpg(), aes(), "displ", "hwy");
        var lower = matrix.cell(1, 0);
        assertEquals("displ", lower.aes().x(), "row 1, col 0: x is column 0");
        assertEquals("hwy", lower.aes().y(), "row 1, col 0: y is row 1");
        assertNull(lower.aes().label(), "lower triangle carries no correlation label aesthetic");
        assertEquals(1, lower.geoms().size(), "lower triangle is a plain scatter layer");
        assertEquals("GeomPoint", lower.geoms().get(0).getClass().getSimpleName());
    }

    @Test
    void matrixUpperTriangleAnnotatesOnly() {
        var matrix = Ggplot.matrixPlot(mpg(), aes(), "displ", "hwy");
        var upper = matrix.cell(0, 1);
        assertEquals("hwy", upper.aes().x(), "row 0, col 1: x is column 1");
        assertEquals("displ", upper.aes().y(), "row 0, col 1: y is row 0");
        assertEquals(1, upper.geoms().size(), "upper triangle carries the text layer only");
    }

    @Test
    void matrixValueFormReplacesDefaultLayers() {
        var matrix = Ggplot.matrixPlot(mpg(), aes(), "displ", "hwy");
        matrix.diag(Geoms.density());
        matrix.lower(Geoms.point());
        matrix.upper(Geoms.point());
        assertEquals("GeomDensity", matrix.cell(0, 0).geoms().get(0).getClass().getSimpleName(),
                "value-form diag replaces the default density");
        assertEquals(1, matrix.cell(0, 0).geoms().size());
        assertEquals("GeomPoint", matrix.cell(1, 0).geoms().get(0).getClass().getSimpleName(),
                "value-form lower replaces the plain scatter layer");
        assertEquals(1, matrix.cell(0, 1).geoms().size());
        assertEquals("GeomPoint", matrix.cell(0, 1).geoms().get(0).getClass().getSimpleName(),
                "value-form upper replaces the correlation layer");
    }

    @Test
    void matrixNoAesOverloadBuildsMatrix() {
        var matrix = Ggplot.matrixPlot(mpg(), "displ", "hwy");
        assertEquals(2, matrix.numRows());
        assertEquals(2, matrix.numCols());
    }

    @Test
    void matrixRendersCorrelationAnnnotation() throws Exception {
        var matrix = Ggplot.matrixPlot(mpg(), aes(), "displ", "hwy");
        var svg = renderSvg(matrix);
        assertNotNull(svg);
        assertTrue(svg.contains("r = "), "SVG must contain the correlation text annotation");
        assertTrue(svg.length() > 800, "pairs SVG must contain the histograms and scatters");
    }

    @Test
    void corrTextRendersAnnotationOnScatter() throws Exception {
        var plot = Ggplot.ggplot(mpg(),
                aes().x("displ").y("hwy")
                        .label(AesValue.afterStat(AesValue.ComputedVariable.CORR)))
                .geoms(Geoms.corrText());
        var svg = renderSvg(plot);
        assertNotNull(svg);
        assertTrue(svg.contains("r = "),
                "a standalone Geoms.text hosting Stats.cor must expose the afterStat label: " + svg);
    }

    @Test
    void matrixEmptyColumnsThrow() {
        try {
            Ggplot.matrixPlot(mpg(), aes());
            throw new AssertionError("expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            // expected
        }
    }

    @Test
    void pairSugarRewiresByVariableName() {
        var matrix = Ggplot.matrixPlot(mpg(), aes(), "displ", "hwy");
        matrix.diag((d, var) -> d.labs("D: " + var));
        matrix.lower((d, x, y) -> d.labs("L: " + y + "~" + x));
        matrix.upper((d, x, y) -> d.labs("U: " + y + "~" + x));

        assertEquals("D: displ", matrix.cell(0, 0).labs().title());
        assertEquals("D: hwy", matrix.cell(1, 1).labs().title());
        assertEquals("L: hwy~displ", matrix.cell(1, 0).labs().title(),
                "lower (row=1,col=0): y is the row header hwy, x the column header displ");
        assertEquals("U: displ~hwy", matrix.cell(0, 1).labs().title(),
                "upper (row=0,col=1): y is the row header displ, x the column header hwy");
    }

    @Test
    void pairSugarRequiresHeaders() {
        var matrix = PlotMatrix.<DataFrame>create(2, 2)
                .base(Ggplot.plot(mpg(), aes()));
        matrix.cells((d, r, c) -> {}, (d, r, c) -> {}, (d, r, c) -> {}, (d, r, c) -> {});
        try {
            matrix.diag((d, var) -> {});
            throw new AssertionError("expected IllegalStateException");
        } catch (IllegalStateException expected) {
            // expected
        }
    }

    @Test
    void pairSugarRequiresPopulatedCells() {
        var matrix = PlotMatrix.<DataFrame>create(2, 2).headers("a", "b");
        try {
            matrix.diag((d, var) -> {});
            throw new AssertionError("expected IllegalStateException");
        } catch (IllegalStateException expected) {
            // expected
        }
    }

    @Test
    void matrixBaseAesHueReachesDiagonal() {
        var matrix = Ggplot.matrixPlot(mpg(), aes().color("drv"), "displ", "hwy");
        assertEquals("drv", matrix.cell(0, 0).aes().color(), "diagonal density inherits the hue");
        assertEquals("drv", matrix.cell(1, 1).aes().color(), "second diagonal density inherits the hue");
        assertEquals("drv", matrix.cell(1, 0).aes().color(), "lower scatter inherits the hue");
    }

    @Test
    void matrixPlainDiagonalCarriesNoHue() {
        var matrix = Ggplot.matrixPlot(mpg(), aes(), "displ", "hwy");
        assertNull(matrix.cell(0, 0).aes().color(), "no hue mapping on a plain matrix");
        assertNull(matrix.cell(0, 0).aes().fill());
    }

    @Test
    void matrixResolvesCellOptionsByPosition() {
        var matrix = Ggplot.matrixPlot(mpg(), aes(), "displ", "hwy");
        // 2×2 matrix: bottom-left (1,0) draws both x and y labels
        assertTrue(matrix.cell(1, 0).options().drawXLabels(),
                "bottom-left row draws x labels");
        assertTrue(matrix.cell(1, 0).options().drawYLabels(),
                "left column draws y labels");
        // top-right (0,1): inner cell → no labels
        assertFalse(matrix.cell(0, 1).options().drawXLabels());
        assertFalse(matrix.cell(0, 1).options().drawYLabels());
        // top-left (0,0): left column → y labels drawn
        assertTrue(matrix.cell(0, 0).options().drawYLabels(),
                "top-left still draws y labels (left column)");
        assertFalse(matrix.cell(0, 0).options().drawXLabels());
        // bottom-right (1,1): bottom row → x labels drawn
        assertTrue(matrix.cell(1, 1).options().drawXLabels(),
                "bottom-right draws x labels (bottom row)");
        assertFalse(matrix.cell(1, 1).options().drawYLabels());
        // axis titles suppressed: the matrix draws outer axis titles from headers
        assertFalse(matrix.cell(1, 0).options().xTitle(), "no x axis title in a pairs matrix");
        assertFalse(matrix.cell(1, 0).options().yTitle(), "no y axis title in a pairs matrix");
        assertEquals(24, matrix.cell(0, 0).theme().axisLabelZone(), 0.001,
                "cells use a compact label zone so panels fill the grid");
    }

    @Test
    void matrixHueDesignatesDefaultSharedLegendCell() {
        var matrix = Ggplot.matrixPlot(mpg(), aes().color("drv"), "displ", "hwy");
        // the auto-designated shared legend collapses every cell to a single legend
        for (int r = 0; r < 2; r++) {
            for (int c = 0; c < 2; c++) {
                assertTrue(matrix.cell(r, c).guides().isAllSuppressed(),
                        "cell (" + r + "," + c + ") guides must be suppressed");
            }
        }
        assertNotNull(matrix.legendSource(), "auto-legend must set a legend source");
        assertFalse(matrix.legendSource().guides().isAllSuppressed(),
                "the legend source must keep its guides");
        assertEquals("drv", matrix.legendSource().aes().color());
    }

    @Test
    void legendCellSuppressesEveryOtherGuide() {
        var matrix = Ggplot.matrixPlot(mpg(), aes().color("drv"), "displ", "hwy");
        matrix.legendCell(1, 0);
        for (int r = 0; r < 2; r++) {
            for (int c = 0; c < 2; c++) {
                assertTrue(matrix.cell(r, c).guides().isAllSuppressed(),
                        "cell (" + r + "," + c + ") guides must be suppressed");
            }
        }
        assertNotNull(matrix.legendSource(), "legendCell must set the legend source");
        assertFalse(matrix.legendSource().guides().isAllSuppressed(),
                "the legend source keeps its guides");
    }

    @Test
    void matrixRendersAxisTitlesAndSharedLegend() throws Exception {
        var matrix = Ggplot.matrixPlot(mpg(), aes().color("drv"), "displ", "hwy");
        var svg = renderSvg(matrix);
        assertNotNull(svg);
        // the variable names render as outer axis titles (column + row each)
        assertEquals(2, occurrences(svg, ">displ<"), "displ x + y axis titles");
        assertEquals(2, occurrences(svg, ">hwy<"), "hwy x + y axis titles");
        // a single shared legend renders to the right of the matrix
        assertEquals(1, occurrences(svg, ">drv<"), "one legend title");
        assertTrue(svg.contains(">f<"), "legend key f");
        assertTrue(svg.contains(">r<"), "legend key r");
    }

    @Test
    void matrixTitleRendersCentered() throws Exception {
        var matrix = Ggplot.matrixPlot(mpg(), aes(), "displ", "hwy");
        matrix.title("MPG Pairs");
        var svg = renderSvg(matrix);
        assertNotNull(svg);
        assertTrue(svg.contains(">MPG Pairs<"), "matrix title must render");
    }

    // ─── Phase A: widths/heights, axisLabels, columnLabels, xlab/ylab,
    //     subtitle, caption, cell setter, blank-cell skip ────────────────

    @Test
    void widthsHeightsProportional() throws Exception {
        var matrix = Ggplot.matrixPlot(mpg(), aes(), "displ", "hwy");
        matrix.widths(2).heights(3);
        double[] colW = matrix.widths();
        double[] rowH = matrix.heights();
        assertEquals(2, colW.length, "widths must be padded to column count");
        assertEquals(2, rowH.length, "heights must be padded to row count");
        assertEquals(2.0, colW[0], 1e-12, "first explicit column weight preserved");
        assertEquals(1.0, colW[1], 1e-12, "unset column padded to 1.0");
        assertEquals(3.0, rowH[0], 1e-12, "first explicit row weight preserved");
        assertEquals(1.0, rowH[1], 1e-12, "unset row padded to 1.0");
        var svg = renderSvg(matrix);
        assertNotNull(svg, "proportional layout must render without error");
        assertTrue(svg.contains("<svg"), "proportional layout produces valid SVG");
    }

    @Test
    void axisLabelsShowForcesEveryCell() {
        var matrix = Ggplot.matrixPlot(mpg(), aes(), "displ", "hwy");
        matrix.axisLabels(PlotMatrix.AxisLabels.SHOW);
        for (int r = 0; r < 2; r++) {
            for (int c = 0; c < 2; c++) {
                assertTrue(matrix.resolvedOptions(r, c).drawXLabels(),
                        "SHOW forces x labels on cell (" + r + "," + c + ")");
                assertTrue(matrix.resolvedOptions(r, c).drawYLabels(),
                        "SHOW forces y labels on cell (" + r + "," + c + ")");
            }
        }
    }

    @Test
    void axisLabelsNoneSuppressesEveryCell() {
        var matrix = Ggplot.matrixPlot(mpg(), aes(), "displ", "hwy");
        matrix.axisLabels(PlotMatrix.AxisLabels.NONE);
        for (int r = 0; r < 2; r++) {
            for (int c = 0; c < 2; c++) {
                assertFalse(matrix.resolvedOptions(r, c).drawXLabels(),
                        "NONE suppresses x labels on cell (" + r + "," + c + ")");
                assertFalse(matrix.resolvedOptions(r, c).drawYLabels(),
                        "NONE suppresses y labels on cell (" + r + "," + c + ")");
            }
        }
    }

    @Test
    void columnLabelsOverrideHeaders() throws Exception {
        var matrix = Ggplot.matrixPlot(mpg(), aes(), "displ", "hwy");
        matrix.columnLabels("E", "M");
        var svg = renderSvg(matrix);
        assertNotNull(svg);
        assertTrue(svg.contains(">E<"), "column label E must render");
        assertTrue(svg.contains(">M<"), "column label M must render");
        // original headers should NOT appear as outer axis titles
        assertEquals(0, occurrences(svg, ">displ<"),
                "headers must not appear as axis titles when columnLabels set");
        assertEquals(0, occurrences(svg, ">hwy<"),
                "headers must not appear as axis titles when columnLabels set");
    }

    @Test
    void xlabYlabSingleTitles() throws Exception {
        var matrix = Ggplot.matrixPlot(mpg(), aes(), "displ", "hwy");
        matrix.xlab("Engine").ylab("Mileage");
        var svg = renderSvg(matrix);
        assertNotNull(svg);
        assertTrue(svg.contains(">Engine<"), "xlab title must render");
        assertTrue(svg.contains(">Mileage<"), "ylab title must render");
        assertEquals(0, occurrences(svg, ">displ<"),
                "headers must not appear when xlab/ylab set");
        assertEquals(0, occurrences(svg, ">hwy<"),
                "headers must not appear when xlab/ylab set");
        // titles should render once (not duplicated)
        assertEquals(1, occurrences(svg, ">Engine<"), "xlab renders once");
        assertEquals(1, occurrences(svg, ">Mileage<"), "ylab renders once");
    }

    @Test
    void subtitleCaptionRender() throws Exception {
        var matrix = Ggplot.matrixPlot(mpg(), aes(), "displ", "hwy");
        matrix.title("T").subtitle("Sub").caption("Cap");
        var svg = renderSvg(matrix);
        assertNotNull(svg);
        assertTrue(svg.contains(">T<"), "title must render");
        assertTrue(svg.contains(">Sub<"), "subtitle must render");
        assertTrue(svg.contains(">Cap<"), "caption must render");
    }

    @Test
    void cellSetterReplacesDescriptor() {
        var df = mpg();
        var matrix = Ggplot.matrixPlot(df, aes(), "displ", "hwy");
        var replacement = Ggplot.<DataFrame>plot(df, aes().x("displ").y("hwy"))
                .geoms(Geoms.point()).labs("Repl");
        matrix.cell(1, 1, replacement);
        assertSame(replacement, matrix.cell(1, 1),
                "cell(r,c,descriptor) must replace the original descriptor");
        assertEquals("Repl", matrix.cell(1, 1).labs().title());
    }

    @Test
    void blankCellsSkipped() throws Exception {
        var df = mpg();
        var filled = Ggplot.<DataFrame>plot(df, aes().x("displ").y("hwy"))
                .geoms(Geoms.point());
        var blank = Ggplot.<DataFrame>plot(df, aes())
                .geoms(Geoms.none());
        var matrix = PlotMatrix.<DataFrame>create(2, 1)
                .base(Ggplot.plot(df, aes()));
        matrix.cell(0, 0, filled);
        matrix.cell(1, 0, blank);
        var svg = renderSvg(matrix);
        assertNotNull(svg, "blank cell must not prevent rendering");
        assertTrue(svg.contains("<svg"), "must produce valid SVG");
    }

    // ─── Phase B: matrixPlot type dispatch ─────────────────────────────

    @Test
    void cellKindClassifiesByColumnType() {
        assertEquals(CellKind.TT,
                CellKind.of(DataExtractor.ColumnType.NUMBER, DataExtractor.ColumnType.NUMBER),
                "numeric × numeric is TT");
        assertEquals(CellKind.DD,
                CellKind.of(DataExtractor.ColumnType.TEXT, DataExtractor.ColumnType.TEXT),
                "categorical × categorical is DD");
        assertEquals(CellKind.COMBO,
                CellKind.of(DataExtractor.ColumnType.NUMBER, DataExtractor.ColumnType.TEXT),
                "numeric row × categorical column is COMBO");
        assertEquals(CellKind.COMBO,
                CellKind.of(DataExtractor.ColumnType.TEXT, DataExtractor.ColumnType.NUMBER),
                "categorical row × numeric column is COMBO");
        assertEquals(CellKind.TT,
                CellKind.of(DataExtractor.ColumnType.DATE, DataExtractor.ColumnType.NUMBER),
                "temporal × numeric is TT");
        assertEquals(CellKind.TT,
                CellKind.of(DataExtractor.ColumnType.TIMESTAMP, DataExtractor.ColumnType.NUMBER),
                "timestamp × numeric is TT");
        assertEquals(CellKind.TT,
                CellKind.of(DataExtractor.ColumnType.TIMESTAMP, DataExtractor.ColumnType.TIMESTAMP),
                "timestamp × timestamp is TT");
        assertEquals(CellKind.COMBO,
                CellKind.of(DataExtractor.ColumnType.TIMESTAMP, DataExtractor.ColumnType.TEXT),
                "timestamp row × categorical column is COMBO");
    }

    @Test
    void matrixPlotDispatchesContinuousCells() {
        var matrix = Ggplot.matrixPlot(mpg(), aes(), "displ", "hwy");
        assertEquals("GeomDensity", matrix.cell(0, 0).geoms().get(0).getClass().getSimpleName(),
                "type-dispatch diag uses a density for continuous variables");
        assertEquals(1, matrix.cell(1, 0).geoms().size(),
                "type-dispatch lower is a plain scatter, no correlation annotation");
        assertEquals("GeomPoint", matrix.cell(1, 0).geoms().get(0).getClass().getSimpleName());
        assertEquals(1, matrix.cell(0, 1).geoms().size(),
                "type-dispatch upper keeps only the correlation");
        assertEquals("GeomText", matrix.cell(0, 1).geoms().get(0).getClass().getSimpleName(),
                "type-dispatch upper carries the Stats.cor annotation");
    }

    @Test
    void matrixPlotDispatchesMixedCells() {
        var matrix = Ggplot.matrixPlot(mpg(), aes(), "displ", "drv");
        assertEquals("GeomDensity", matrix.cell(0, 0).geoms().get(0).getClass().getSimpleName(),
                "numeric diagonal is a density");
        assertEquals("GeomBar", matrix.cell(1, 1).geoms().get(0).getClass().getSimpleName(),
                "categorical diagonal is a count bar");

        var box = matrix.cell(0, 1);
        assertEquals("GeomBoxplot", box.geoms().get(0).getClass().getSimpleName(),
                "continuous y × categorical x upper cell is a box plot of the continuous variable");
        assertEquals("drv", box.aes().x(), "box x is the categorical (column) variable");
        assertEquals("displ", box.aes().y(), "box y is the continuous (row) variable");

        var hist = matrix.cell(1, 0);
        assertEquals("GeomHistogram", hist.geoms().get(0).getClass().getSimpleName(),
                "categorical y × continuous x lower cell is a faceted histogram");
        assertTrue(hist.facet() != null && hist.facet().isGrid(),
                "facethist must carry a grid facet");
        assertTrue(hist.facet().getRowVars().contains("drv"),
                "facethist must facet by the categorical variable");
        assertEquals("displ", hist.aes().x(), "histogram x is the continuous variable");
    }

    @Test
    void matrixPlotDispatchesDiscreteCells() {
        var df = DataFrame.byColumn("a", "b").of(
                Series.of("f", "f", "4", "r", "r", "4"),
                Series.of("L", "M", "M", "H", "H", "L"));
        var matrix = Ggplot.matrixPlot(df, aes(), "a", "b");
        assertEquals("GeomBar", matrix.cell(0, 0).geoms().get(0).getClass().getSimpleName(),
                "categorical diagonal is a count bar");
        assertEquals("GeomBar", matrix.cell(1, 1).geoms().get(0).getClass().getSimpleName());
        assertEquals("GeomBar", matrix.cell(0, 1).geoms().get(0).getClass().getSimpleName(),
                "discrete upper is a grouped count bar");
        assertEquals("GeomBar", matrix.cell(1, 0).geoms().get(0).getClass().getSimpleName(),
                "discrete lower is a faceted count bar");
    }

    @Test
    void matrixPlotDispatchedMixedRenders() throws Exception {
        var matrix = Ggplot.matrixPlot(mpg(), aes(), "displ", "hwy", "drv");
        var svg = renderSvg(matrix);
        assertNotNull(svg, "mixed-type matrix must render");
        assertTrue(svg.contains("<svg"), "mixed matrix produces valid SVG");
        assertTrue(svg.length() > 800, "mixed matrix must contain non-trivial geometry");
    }

    private static int occurrences(String text, String needle) {
        int count = 0;
        int idx = 0;
        while ((idx = text.indexOf(needle, idx)) >= 0) {
            count++;
            idx += needle.length();
        }
        return count;
    }

    @Test
    void layoutChildrenAttachesRenderedCanvas() throws Exception {
        onFxThread(() -> {
            var matrix = Ggplot.matrixPlot(mpg(), aes(), "displ", "hwy");
            var parent = new Pane(matrix);
            assertNotNull(parent);
            matrix.resize(WIDTH, HEIGHT);
            matrix.layoutChildren();

            var canvases = matrix.getChildren().stream()
                    .filter(Canvas.class::isInstance)
                    .map(Canvas.class::cast)
                    .toList();
            assertEquals(1, canvases.size(), "layoutChildren must attach exactly one canvas");
            assertEquals(WIDTH, canvases.get(0).getWidth(), 0.001);
            assertEquals(HEIGHT, canvases.get(0).getHeight(), 0.001);
            return null;
        });
    }

    private static String renderSvg(GgFigure figure) throws Exception {
        return onFxThread(() -> new SvgExporter()
                .size(WIDTH, HEIGHT).toSvg(figure));
    }
}
