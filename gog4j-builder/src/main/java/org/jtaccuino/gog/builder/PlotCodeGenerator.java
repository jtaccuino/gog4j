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
package org.jtaccuino.gog.builder;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import javafx.scene.paint.Color;
import org.jtaccuino.gog.facet.GridOptions;

/**
 * Renders a {@link BuilderModel} back into the idiomatic gog4j Java that would
 * produce the same plot — the exact inverse of {@link BuilderSpec}. Parameters
 * are omitted when unset so the output stays minimal, and a header comment
 * lists every import the snippet needs. A manual color scale is expanded to the
 * distinct values of the mapped column (shared with {@link BuilderSpec} via
 * {@link BuilderModel#distinctValues}), so the code reproduces the same legend.
 */
public final class PlotCodeGenerator {

    /** Utility class; not meant to be instantiated. */
    private PlotCodeGenerator() {
    }

    /**
     * Emits a complete, copy-pasteable factory method for the given model.
     *
     * @param model the builder state
     * @return the generated Java source
     */
    public static String emit(BuilderModel model) {
        var imports = new LinkedHashSet<String>();
        imports.add("import org.dflib.DataFrame;\n");
        imports.add("import org.jtaccuino.gog.Plot;\n");
        imports.add("import " + model.dataset().loaderImport() + ";\n");
        var body = new StringBuilder();

        body.append("    var df = ").append(model.dataset().loaderExpr()).append(";\n");

        body.append("    return ggplot(df, ").append(aesExpr(model)).append(")\n");
        imports.add("import static org.jtaccuino.gog.Aes.aes;\n");
        imports.add("import static org.jtaccuino.gog.Ggplot.ggplot;\n");

        if (!model.geoms.isEmpty()) {
            var geomExprs = model.geoms.stream().map(g -> geomExpr(g, imports)).toList();
            body.append("            .geoms(\n");
            for (var i = 0; i < geomExprs.size(); i++) {
                body.append("                ").append(geomExprs.get(i));
                body.append(i + 1 < geomExprs.size() ? ",\n" : "\n");
            }
            body.append("            )\n");
        }

        if (model.scaleX != BuilderModel.ScaleKind.AUTO) {
            appendScale(body, imports, "X", model.scaleX, model.scaleXMin, model.scaleXMax);
        }
        if (model.scaleY != BuilderModel.ScaleKind.AUTO) {
            appendScale(body, imports, "Y", model.scaleY, model.scaleYMin, model.scaleYMax);
        }
        appendColorScale(body, imports, model, model.colorScale, model.color, model.colorPalette, true);
        appendColorScale(body, imports, model, model.fillScale, model.fill, model.fillPalette, false);

        if (model.coord != BuilderModel.CoordKind.CARTESIAN
                || model.coordXLimMin != null || model.coordXLimMax != null
                || model.coordYLimMin != null || model.coordYLimMax != null) {
            imports.add("import static org.jtaccuino.gog.Coords." + coordFactory(model.coord) + ";\n");
            body.append("            .coord(").append(coordExpr(model, imports)).append(")\n");
        }

        if (model.theme != BuilderModel.ThemeKind.GRAY) {
            body.append("            .theme(Theme.theme_").append(themeName(model.theme)).append("())\n");
            imports.add("import org.jtaccuino.gog.theme.Theme;\n");
        }
        if (!model.showXGrid) {
            body.append("            .theme(t -> t.showXGrid(false))\n");
        }
        if (!model.showYGrid) {
            body.append("            .theme(t -> t.showYGrid(false))\n");
        }

        if (model.title != null || model.xLabel != null || model.yLabel != null) {
            body.append("            .labs(labs(")
                    .append(lit(model.title)).append(", ")
                    .append(lit(model.xLabel)).append(", ")
                    .append(lit(model.yLabel)).append("))\n");
            imports.add("import static org.jtaccuino.gog.labs.Labs.labs;\n");
        }

        appendFacets(body, imports, model);

        var chain = body.substring(0, body.length() - 1);
        var code = new StringBuilder();
        code.append("/*\n");
        code.append(" * Imports:\n");
        for (var imp : imports) {
            code.append(" *   ").append(imp);
        }
        code.append(" */\n");
        code.append("public static Plot<DataFrame> buildPlot() {\n");
        code.append(chain).append(";\n");
        code.append("}");
        return code.toString();
    }

    private static String aesExpr(BuilderModel model) {
        var sb = new StringBuilder("aes()");
        appendCol(sb, "x", model.x);
        appendCol(sb, "y", model.y);
        appendCol(sb, "z", model.z);
        appendCol(sb, "color", model.color);
        appendCol(sb, "fill", model.fill);
        appendCol(sb, "shape", model.shape);
        appendCol(sb, "size", model.size);
        appendCol(sb, "alpha", model.alpha);
        appendCol(sb, "group", model.group);
        appendCol(sb, "xmin", model.xmin);
        appendCol(sb, "xmax", model.xmax);
        appendCol(sb, "ymin", model.ymin);
        appendCol(sb, "ymax", model.ymax);
        appendCol(sb, "xend", model.xend);
        appendCol(sb, "yend", model.yend);
        appendCol(sb, "zend", model.zend);
        return sb.toString();
    }

    private static void appendCol(StringBuilder sb, String method, String column) {
        if (column != null) {
            sb.append('.').append(method).append("(\"").append(column).append("\")");
        }
    }

    private static String geomExpr(BuilderModel.Geom g, Set<String> imports) {
        return switch (g.kind) {
            case POINT -> {
                var sb = factory("point", imports);
                appendDouble(sb, "size", g, BuilderModel.Params.SIZE);
                appendDouble(sb, "opacity", g, BuilderModel.Params.OPACITY);
                appendColor(sb, "color", g, BuilderModel.Params.COLOR, imports);
                appendEnum(sb, "shape", g, BuilderModel.Params.SHAPE, imports);
                yield sb.toString();
            }
            case JITTER -> {
                var sb = factory("jitter", imports);
                appendDouble(sb, "width", g, BuilderModel.Params.WIDTH);
                appendDouble(sb, "height", g, BuilderModel.Params.HEIGHT);
                appendDouble(sb, "size", g, BuilderModel.Params.SIZE);
                appendDouble(sb, "opacity", g, BuilderModel.Params.OPACITY);
                appendColor(sb, "color", g, BuilderModel.Params.COLOR, imports);
                appendEnum(sb, "shape", g, BuilderModel.Params.SHAPE, imports);
                yield sb.toString();
            }
            case LINE -> factory("line", imports).toString();
            case SMOOTH -> {
                var sb = factory("smooth", imports);
                appendEnum(sb, "method", g, BuilderModel.Params.METHOD, imports);
                appendDouble(sb, "span", g, BuilderModel.Params.SPAN);
                appendFlag(sb, "se", g, BuilderModel.Params.SE);
                appendColor(sb, "color", g, BuilderModel.Params.COLOR, imports);
                yield sb.toString();
            }
            case BAR -> {
                var sb = factory("bar", imports);
                appendEnum(sb, "position", g, BuilderModel.Params.POSITION, imports);
                appendColor(sb, "fill", g, BuilderModel.Params.FILL, imports);
                yield sb.toString();
            }
            case COL -> {
                var sb = factory("col", imports);
                appendEnum(sb, "position", g, BuilderModel.Params.POSITION, imports);
                appendColor(sb, "fill", g, BuilderModel.Params.FILL, imports);
                yield sb.toString();
            }
            case HISTOGRAM -> {
                var sb = factory("histogram", imports);
                appendDouble(sb, "bins", g, BuilderModel.Params.BINS);
                appendColor(sb, "fill", g, BuilderModel.Params.FILL, imports);
                yield sb.toString();
            }
            case FREQPOLY -> {
                var sb = factory("freqpoly", imports);
                appendDouble(sb, "bins", g, BuilderModel.Params.BINS);
                appendDouble(sb, "width", g, BuilderModel.Params.WIDTH);
                appendColor(sb, "color", g, BuilderModel.Params.COLOR, imports);
                yield sb.toString();
            }
            case DENSITY -> {
                var sb = factory("density", imports);
                appendDouble(sb, "adjust", g, BuilderModel.Params.ADJUST);
                appendColor(sb, "fill", g, BuilderModel.Params.FILL, imports);
                yield sb.toString();
            }
            case DENSITY2D -> {
                var sb = factory("density2d", imports);
                appendDouble(sb, "adjust", g, BuilderModel.Params.ADJUST);
                appendDouble(sb, "bins", g, BuilderModel.Params.BINS);
                appendFlag(sb, "filled", g, BuilderModel.Params.FILLED);
                appendDouble(sb, "alpha", g, BuilderModel.Params.ALPHA);
                yield sb.toString();
            }
            case AREA -> {
                var sb = factory("area", imports);
                appendColor(sb, "fill", g, BuilderModel.Params.FILL, imports);
                appendEnum(sb, "position", g, BuilderModel.Params.POSITION, imports);
                yield sb.toString();
            }
            case BOXPLOT -> {
                var sb = factory("boxplot", imports);
                appendColor(sb, "fill", g, BuilderModel.Params.FILL, imports);
                appendColor(sb, "color", g, BuilderModel.Params.COLOR, imports);
                yield sb.toString();
            }
            case VIOLIN -> {
                var sb = factory("violin", imports);
                appendColor(sb, "fill", g, BuilderModel.Params.FILL, imports);
                yield sb.toString();
            }
            case TEXT -> {
                var sb = factory("text", imports);
                appendDouble(sb, "size", g, BuilderModel.Params.SIZE);
                appendColor(sb, "color", g, BuilderModel.Params.COLOR, imports);
                appendFlagCall(sb, "bold", g, BuilderModel.Params.BOLD);
                appendDouble(sb, "angle", g, BuilderModel.Params.ANGLE);
                appendDouble(sb, "hjust", g, BuilderModel.Params.HJUST);
                appendDouble(sb, "vjust", g, BuilderModel.Params.VJUST);
                yield sb.toString();
            }
            case POINT3D -> {
                var sb = factory("point3d", imports);
                appendDouble(sb, "size", g, BuilderModel.Params.SIZE);
                appendDouble(sb, "opacity", g, BuilderModel.Params.OPACITY);
                appendColor(sb, "color", g, BuilderModel.Params.COLOR, imports);
                appendEnum(sb, "shape", g, BuilderModel.Params.SHAPE, imports);
                appendFlag(sb, "rawPoints", g, BuilderModel.Params.RAW_POINTS);
                appendFlag(sb, "refLines", g, BuilderModel.Params.REF_LINES);
                yield sb.toString();
            }
            case TEXT3D -> {
                var sb = factory("text3d", imports);
                appendDouble(sb, "size", g, BuilderModel.Params.SIZE);
                appendColor(sb, "color", g, BuilderModel.Params.COLOR, imports);
                appendFlagCall(sb, "bold", g, BuilderModel.Params.BOLD);
                yield sb.toString();
            }
            case SEGMENT3D -> {
                var sb = factory("segment3d", imports);
                appendColor(sb, "color", g, BuilderModel.Params.COLOR, imports);
                appendDouble(sb, "linewidth", g, BuilderModel.Params.LINE_WIDTH);
                yield sb.toString();
            }
            case PATH3D -> {
                var sb = factory("path3d", imports);
                appendColor(sb, "color", g, BuilderModel.Params.COLOR, imports);
                appendDouble(sb, "linewidth", g, BuilderModel.Params.LINE_WIDTH);
                yield sb.toString();
            }
            case BAR3D -> {
                var sb = factory("bar3d", imports);
                appendColor(sb, "fill", g, BuilderModel.Params.FILL, imports);
                appendColor(sb, "color", g, BuilderModel.Params.COLOR, imports);
                appendDouble(sb, "bins", g, BuilderModel.Params.BINS);
                appendDouble(sb, "width", g, BuilderModel.Params.WIDTH);
                yield sb.toString();
            }
            case COL3D -> {
                var sb = factory("col3d", imports);
                appendColor(sb, "fill", g, BuilderModel.Params.FILL, imports);
                appendColor(sb, "color", g, BuilderModel.Params.COLOR, imports);
                appendDouble(sb, "width", g, BuilderModel.Params.WIDTH);
                appendDouble(sb, "zmin", g, BuilderModel.Params.ZMIN);
                yield sb.toString();
            }
            case SURFACE3D -> {
                var sb = factory("surface3d", imports);
                appendColor(sb, "fill", g, BuilderModel.Params.FILL, imports);
                appendColor(sb, "color", g, BuilderModel.Params.COLOR, imports);
                appendDouble(sb, "linewidth", g, BuilderModel.Params.LINE_WIDTH);
                appendEnum(sb, "method", g, BuilderModel.Params.METHOD, imports);
                appendEnum(sb, "grid", g, BuilderModel.Params.GRID, imports);
                yield sb.toString();
            }
            case POLYGON3D -> {
                var sb = factory("polygon3d", imports);
                appendColor(sb, "fill", g, BuilderModel.Params.FILL, imports);
                appendColor(sb, "color", g, BuilderModel.Params.COLOR, imports);
                appendDouble(sb, "alpha", g, BuilderModel.Params.ALPHA);
                appendDouble(sb, "linewidth", g, BuilderModel.Params.LINE_WIDTH);
                yield sb.toString();
            }
            case VOXEL3D -> {
                var sb = factory("voxel3d", imports);
                appendColor(sb, "fill", g, BuilderModel.Params.FILL, imports);
                appendColor(sb, "color", g, BuilderModel.Params.COLOR, imports);
                appendDouble(sb, "width", g, BuilderModel.Params.WIDTH);
                yield sb.toString();
            }
            case HULL3D -> {
                var sb = factory("hull3d", imports);
                appendEnum(sb, "method", g, BuilderModel.Params.METHOD, imports);
                appendDouble(sb, "radius", g, BuilderModel.Params.RADIUS);
                appendColor(sb, "fill", g, BuilderModel.Params.FILL, imports);
                appendColor(sb, "color", g, BuilderModel.Params.COLOR, imports);
                yield sb.toString();
            }
            case RIDGELINE3D -> {
                var sb = factory("ridgeline3d", imports);
                appendEnum(sb, "direction", g, BuilderModel.Params.DIRECTION, imports);
                appendDouble(sb, "base", g, BuilderModel.Params.BASE);
                appendColor(sb, "fill", g, BuilderModel.Params.FILL, imports);
                appendColor(sb, "color", g, BuilderModel.Params.COLOR, imports);
                yield sb.toString();
            }
            case CONTOUR3D -> {
                var sb = factory("contour3d", imports);
                appendDouble(sb, "bins", g, BuilderModel.Params.BINS);
                appendDouble(sb, "alpha", g, BuilderModel.Params.ALPHA);
                appendColor(sb, "fill", g, BuilderModel.Params.FILL, imports);
                appendColor(sb, "color", g, BuilderModel.Params.COLOR, imports);
                yield sb.toString();
            }
            case SMOOTH3D -> {
                var sb = factory("smooth3d", imports);
                appendEnum(sb, "method", g, BuilderModel.Params.METHOD, imports);
                appendDouble(sb, "span", g, BuilderModel.Params.SPAN);
                appendFlag(sb, "se", g, BuilderModel.Params.SE);
                appendDouble(sb, "level", g, BuilderModel.Params.LEVEL);
                appendColor(sb, "fill", g, BuilderModel.Params.FILL, imports);
                appendColor(sb, "color", g, BuilderModel.Params.COLOR, imports);
                yield sb.toString();
            }
            case DENSITY3D -> {
                var sb = factory("density3d", imports);
                appendDouble(sb, "adjust", g, BuilderModel.Params.ADJUST);
                appendDouble(sb, "n", g, BuilderModel.Params.N);
                appendColor(sb, "fill", g, BuilderModel.Params.FILL, imports);
                appendColor(sb, "color", g, BuilderModel.Params.COLOR, imports);
                yield sb.toString();
            }
            case FUNCTION3D -> {
                var sb = factory("function3d", imports);
                sb.insert(sb.length() - 1, "(x, y) -> { double r = Math.sqrt(x * x + y * y) + 1e-9; return Math.sin(r) / r; }");
                appendLimits(sb, "xlim", g, BuilderModel.Params.XLIM_MIN, BuilderModel.Params.XLIM_MAX);
                appendLimits(sb, "ylim", g, BuilderModel.Params.YLIM_MIN, BuilderModel.Params.YLIM_MAX);
                appendDouble(sb, "n", g, BuilderModel.Params.N);
                appendColor(sb, "fill", g, BuilderModel.Params.FILL, imports);
                appendColor(sb, "color", g, BuilderModel.Params.COLOR, imports);
                yield sb.toString();
            }
            case POLYGON -> {
                var sb = factory("polygon", imports);
                appendColor(sb, "color", g, BuilderModel.Params.COLOR, imports);
                appendDouble(sb, "alpha", g, BuilderModel.Params.ALPHA);
                appendDouble(sb, "lineWidth", g, BuilderModel.Params.LINE_WIDTH);
                appendFlag(sb, "filled", g, BuilderModel.Params.FILLED);
                yield sb.toString();
            }
            case TILE -> factory("tile", imports).toString();
            case PATH -> factory("path", imports).toString();
            case STEP -> factory("step", imports).toString();
            case SEGMENT -> factory("segment", imports).toString();
            case CURVE -> factory("curve", imports).toString();
            case CROSSBAR -> {
                var sb = factory("crossbar", imports);
                appendColor(sb, "fill", g, BuilderModel.Params.FILL, imports);
                appendColor(sb, "color", g, BuilderModel.Params.COLOR, imports);
                appendDouble(sb, "width", g, BuilderModel.Params.WIDTH);
                yield sb.toString();
            }
            case ERRORBAR -> {
                var sb = factory("errorbar", imports);
                appendColor(sb, "color", g, BuilderModel.Params.COLOR, imports);
                appendDouble(sb, "width", g, BuilderModel.Params.WIDTH);
                yield sb.toString();
            }
            case ERRORBARH -> {
                var sb = factory("errorbarh", imports);
                appendColor(sb, "color", g, BuilderModel.Params.COLOR, imports);
                appendDouble(sb, "width", g, BuilderModel.Params.WIDTH);
                yield sb.toString();
            }
            case LINERANGE -> {
                var sb = factory("linerange", imports);
                appendColor(sb, "color", g, BuilderModel.Params.COLOR, imports);
                appendDouble(sb, "width", g, BuilderModel.Params.WIDTH);
                yield sb.toString();
            }
            case POINTRANGE -> {
                var sb = factory("pointrange", imports);
                appendColor(sb, "color", g, BuilderModel.Params.COLOR, imports);
                appendDouble(sb, "width", g, BuilderModel.Params.WIDTH);
                appendDouble(sb, "pointRadius", g, BuilderModel.Params.POINT_RADIUS);
                yield sb.toString();
            }
            case RIBBON -> {
                var sb = factory("ribbon", imports);
                appendColor(sb, "fill", g, BuilderModel.Params.FILL, imports);
                appendDouble(sb, "alpha", g, BuilderModel.Params.ALPHA);
                yield sb.toString();
            }
            case HLINE -> {
                var sb = new StringBuilder("hline(").append(format(required(g, BuilderModel.Params.YINTERCEPT))).append(")");
                imports.add("import static org.jtaccuino.gog.Geoms.hline;\n");
                appendColor(sb, "color", g, BuilderModel.Params.COLOR, imports);
                appendDouble(sb, "width", g, BuilderModel.Params.WIDTH);
                appendFlagCall(sb, "dashed", g, BuilderModel.Params.DASHED);
                yield sb.toString();
            }
            case ABLINE -> {
                var sb = new StringBuilder("abline(").append(format(required(g, BuilderModel.Params.SLOPE)))
                        .append(", ").append(format(required(g, BuilderModel.Params.INTERCEPT))).append(")");
                imports.add("import static org.jtaccuino.gog.Geoms.abline;\n");
                appendColor(sb, "color", g, BuilderModel.Params.COLOR, imports);
                appendDouble(sb, "width", g, BuilderModel.Params.WIDTH);
                appendFlagCall(sb, "dashed", g, BuilderModel.Params.DASHED);
                yield sb.toString();
            }
            case VLINE -> {
                var sb = new StringBuilder("vline(").append(format(required(g, BuilderModel.Params.XINTERCEPT))).append(")");
                imports.add("import static org.jtaccuino.gog.Geoms.vline;\n");
                appendColor(sb, "color", g, BuilderModel.Params.COLOR, imports);
                appendDouble(sb, "width", g, BuilderModel.Params.WIDTH);
                appendFlagCall(sb, "dashed", g, BuilderModel.Params.DASHED);
                yield sb.toString();
            }
        };
    }

    private static StringBuilder factory(String name, Set<String> imports) {
        imports.add("import static org.jtaccuino.gog.Geoms." + name + ";\n");
        return new StringBuilder(name).append("()");
    }

    private static double required(BuilderModel.Geom g, String key) {
        var value = g.doubles.get(key);
        return value == null ? 0.0 : value;
    }

    private static void appendDouble(StringBuilder sb, String method, BuilderModel.Geom g, String key) {
        var value = g.doubles.get(key);
        if (value != null) {
            sb.append('.').append(method).append('(').append(format(value)).append(')');
        }
    }

    private static void appendLimits(StringBuilder sb, String method, BuilderModel.Geom g,
            String minKey, String maxKey) {
        var min = g.doubles.get(minKey);
        var max = g.doubles.get(maxKey);
        if (min != null && max != null) {
            sb.append('.').append(method).append('(')
                    .append(format(min)).append(", ").append(format(max)).append(')');
        }
    }

    private static void appendColor(StringBuilder sb, String method, BuilderModel.Geom g, String key,
            Set<String> imports) {
        var value = g.colors.get(key);
        if (value != null) {
            sb.append('.').append(method).append("(Color.web(\"#").append(hex(value)).append("\"))");
            imports.add("import javafx.scene.paint.Color;\n");
        }
    }

    private static void appendEnum(StringBuilder sb, String method, BuilderModel.Geom g, String key,
            Set<String> imports) {
        var value = g.enums.get(key);
        if (value != null) {
            sb.append('.').append(method).append('(')
                    .append(value.getDeclaringClass().getSimpleName()).append('.').append(value.name())
                    .append(')');
            imports.add("import " + value.getDeclaringClass().getName() + ";\n");
        }
    }

    private static void appendFlag(StringBuilder sb, String method, BuilderModel.Geom g, String key) {
        var value = g.flags.get(key);
        if (value != null) {
            sb.append('.').append(method).append('(').append(value ? "true" : "false").append(')');
        }
    }

    private static void appendFlagCall(StringBuilder sb, String method, BuilderModel.Geom g, String key) {
        if (Boolean.TRUE.equals(g.flags.get(key))) {
            sb.append('.').append(method).append("()");
        }
    }

    // ─── Scales ──────────────────────────────────────────────────────────

    private static void appendScale(StringBuilder body, Set<String> imports, String axis,
            BuilderModel.ScaleKind kind, Double min, Double max) {
        var name = switch (kind) {
            case LOG10 -> "scale" + axis + "Log10";
            case SQRT -> "scale" + axis + "Sqrt";
            case REVERSE -> "scale" + axis + "Reverse";
            case LIMITS -> "scale" + axis + "Limits";
            case AUTO -> throw new IllegalStateException("AUTO has no expression");
        };
        imports.add("import static org.jtaccuino.gog.scale.Scales." + name + ";\n");
        if (kind == BuilderModel.ScaleKind.LIMITS && min != null && max != null) {
            body.append("            .scales(").append(name).append("(")
                    .append(format(min)).append(", ").append(format(max)).append("))\n");
        } else if (kind != BuilderModel.ScaleKind.LIMITS) {
            body.append("            .scales(").append(name).append("())\n");
        }
    }

    private static void appendColorScale(StringBuilder body, Set<String> imports, BuilderModel model,
            BuilderModel.ColorScaleKind kind, String column, String palette, boolean color) {
        switch (kind) {
            case VIRIDIS_C -> appendColorScaleCall(body, imports, color ? "scaleColorViridisC" : "scaleFillViridisC", null);
            case VIRIDIS_D -> appendColorScaleCall(body, imports, color ? "scaleColorViridisD" : "scaleFillViridisD", null);
            case BREWER -> {
                if (palette != null) {
                    appendColorScaleCall(body, imports, color ? "scaleColorBrewer" : "scaleFillBrewer", palette);
                }
            }
            case MANUAL -> {
                if (color && column != null && !model.manualColors.isEmpty()) {
                    var values = model.distinctValues(column);
                    var expr = new StringBuilder("scaleColorManual()");
                    imports.add("import static org.jtaccuino.gog.scale.Scales.scaleColorManual;\n");
                    for (var i = 0; i < values.size(); i++) {
                        expr.append(".color(").append(lit(values.get(i))).append(", Color.web(\"#")
                                .append(hex(model.manualColors.get(i % model.manualColors.size()))).append("\"))");
                        imports.add("import javafx.scene.paint.Color;\n");
                    }
                    body.append("            .scales(").append(expr).append(")\n");
                }
            }
            case AUTO -> { }
        }
    }

    private static void appendColorScaleCall(StringBuilder body, Set<String> imports, String name, String arg) {
        imports.add("import static org.jtaccuino.gog.scale.Scales." + name + ";\n");
        body.append("            .scales(").append(name).append(arg == null ? "()" : "(\"" + arg + "\")").append(")\n");
    }

    // ─── Facets ──────────────────────────────────────────────────────────

    private static void appendFacets(StringBuilder body, Set<String> imports, BuilderModel model) {
        switch (model.facet) {
            case NONE -> { }
            case WRAP -> {
                if (model.facetWrapColumn == null) {
                    return;
                }
                int cols = model.facetWrapCols == null ? -1 : model.facetWrapCols;
                imports.add("import org.jtaccuino.gog.Facets;\n");
                body.append("            .facets(Facets.wrap(\"").append(model.facetWrapColumn)
                        .append("\", ").append(cols).append("))\n");
            }
            case GRID -> {
                if (model.facetGridRow == null && model.facetGridCol == null) {
                    return;
                }
                imports.add("import org.jtaccuino.gog.Facets;\n");
                body.append("            .facets(Facets.grid(")
                        .append(lit(model.facetGridRow)).append(", ")
                        .append(lit(model.facetGridCol));
                if (model.hasNonDefaultGridOptions()) {
                    imports.add("import org.jtaccuino.gog.facet.GridOptions;\n");
                    body.append(", ").append(gridOptionsExpr(model));
                }
                body.append("))\n");
            }
        }
    }

    private static String gridOptionsExpr(BuilderModel model) {
        var sb = new StringBuilder("GridOptions.defaults()");
        if (model.facetScale != GridOptions.Scale.FIXED) {
            sb.append(".withScale(GridOptions.Scale.").append(model.facetScale.name()).append(")");
        }
        if (model.facetSpace != GridOptions.Space.FIXED) {
            sb.append(".withSpace(GridOptions.Space.").append(model.facetSpace.name()).append(")");
        }
        if (model.facetSwitch != GridOptions.GridSwitch.NONE) {
            sb.append(".withSwitch(GridOptions.GridSwitch.").append(model.facetSwitch.name()).append(")");
        }
        if (model.facetAxes != GridOptions.Axes.MARGINS) {
            sb.append(".withAxes(GridOptions.Axes.").append(model.facetAxes.name()).append(")");
        }
        if (model.facetMargins) {
            sb.append(".withMargins()");
        }
        if (!model.facetAsTable) {
            sb.append(".withAsTable(false)");
        }
        if (!model.facetDrop) {
            sb.append(".withDrop(false)");
        }
        return sb.toString();
    }

    // ─── Coords ──────────────────────────────────────────────────────────

    private static String coordFactory(BuilderModel.CoordKind kind) {
        return switch (kind) {
            case CARTESIAN -> "coordCartesian";
            case FLIP -> "coordFlip";
            case POLAR -> "coordPolar";
            case EQUAL -> "coordEqual";
            case COORD3D -> "coord3d";
        };
    }

    private static String coordExpr(BuilderModel model, Set<String> imports) {
        var expr = switch (model.coord) {
            case CARTESIAN -> "coordCartesian()";
            case FLIP -> "coordFlip()";
            case POLAR -> "coordPolar()";
            case EQUAL -> model.equalRatio != null
                    ? "coordEqual(" + format(model.equalRatio) + ")"
                    : "coordEqual()";
            case COORD3D -> coord3dExpr(model, imports);
        };
        if (model.coord != BuilderModel.CoordKind.COORD3D) {
            if (model.coordXLimMin != null && model.coordXLimMax != null) {
                expr += ".xlim(" + format(model.coordXLimMin) + ", " + format(model.coordXLimMax) + ")";
            }
            if (model.coordYLimMin != null && model.coordYLimMax != null) {
                expr += ".ylim(" + format(model.coordYLimMin) + ", " + format(model.coordYLimMax) + ")";
            }
            if (model.coord == BuilderModel.CoordKind.POLAR && model.polarTheta != null) {
                expr += ".theta(" + lit(model.polarTheta) + ")";
            }
            if (model.coord == BuilderModel.CoordKind.POLAR && model.polarStart != null) {
                expr += ".start(" + format(model.polarStart) + ")";
            }
        }
        return expr;
    }

    private static String coord3dExpr(BuilderModel model, Set<String> imports) {
        var sb = new StringBuilder("coord3d()");
        if (model.coord3dPitch != null) {
            sb.append(".pitch(").append(format(model.coord3dPitch)).append(")");
        }
        if (model.coord3dRoll != null) {
            sb.append(".roll(").append(format(model.coord3dRoll)).append(")");
        }
        if (model.coord3dYaw != null) {
            sb.append(".yaw(").append(format(model.coord3dYaw)).append(")");
        }
        if (model.coord3dDist != null) {
            sb.append(".dist(").append(format(model.coord3dDist)).append(")");
        }
        if (model.coord3dZoom != null) {
            sb.append(".zoom(").append(format(model.coord3dZoom)).append(")");
        }
        if (model.coord3dPersp != null) {
            sb.append(".persp(").append(model.coord3dPersp).append(")");
        }
        if (model.coord3dScaleMode != null) {
            sb.append(".scales(ScaleMode.").append(model.coord3dScaleMode.name()).append(")");
            imports.add("import org.jtaccuino.gog.coord.ScaleMode;\n");
        }
        if (model.coord3dPanels != null) {
            sb.append(".panels(CubePanel.").append(model.coord3dPanels.name()).append(")");
            imports.add("import org.jtaccuino.gog.coord.CubePanel;\n");
        }
        if (model.coord3dRatioX != null && model.coord3dRatioY != null && model.coord3dRatioZ != null) {
            sb.append(".ratio(").append(format(model.coord3dRatioX)).append(", ")
                    .append(format(model.coord3dRatioY)).append(", ")
                    .append(format(model.coord3dRatioZ)).append(")");
        }
        if (model.coord3dClip != null) {
            sb.append(".clip(").append(model.coord3dClip).append(")");
        }
        if (model.coord3dExpand != null) {
            sb.append(".expand(").append(model.coord3dExpand).append(")");
        }
        if (Boolean.TRUE.equals(model.coord3dLight)) {
            sb.append(".light(Light3d.defaultLight())");
            imports.add("import org.jtaccuino.gog.coord.Light3d;\n");
        }
        return sb.toString();
    }

    // ─── Formatting ──────────────────────────────────────────────────────

    private static String format(double value) {
        var text = String.format(Locale.ROOT, "%.4f", value);
        while (text.endsWith("0")) {
            text = text.substring(0, text.length() - 1);
        }
        return text.endsWith(".") ? text + "0" : text;
    }

    private static String hex(Color color) {
        return String.format(Locale.ROOT, "%02x%02x%02x",
                Math.round(color.getRed() * 255),
                Math.round(color.getGreen() * 255),
                Math.round(color.getBlue() * 255));
    }

    private static String themeName(BuilderModel.ThemeKind kind) {
        return switch (kind) {
            case GRAY -> "gray";
            case BW -> "bw";
            case DARK -> "dark";
        };
    }

    private static String lit(String value) {
        return value == null ? "null" : "\"" + value + "\"";
    }
}
