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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javafx.scene.paint.Color;
import org.jtaccuino.gog.Aes;
import org.jtaccuino.gog.RenderMode;
import org.jtaccuino.gog.controls.PlotDataset;
import org.jtaccuino.gog.coord.CubePanel;
import org.jtaccuino.gog.coord.ScaleMode;
import org.jtaccuino.gog.facet.GridOptions;

/**
 * Mutable state of the visual plot builder: the dataset, aesthetic column
 * bindings, an ordered list of geometries with their parameters, and the
 * scale/theme/coord/lab presentation options. Both the live preview
 * ({@link BuilderSpec}) and the generated source ({@link PlotCodeGenerator})
 * are derived from one model, so what you see is exactly what the code builds.
 * <p>
 * The data source is a host-provided {@link PlotDataset} (the dflib adapter
 * lives outside this package), so the builder itself carries no DataFrame
 * dependency. Parameters are absent (missing keys) when unset: the emitted code
 * (and the assembled plot) then fall back to the library defaults instead of
 * spelling a value out. This keeps generated code minimal and honest.
 */
public final class BuilderModel {

    /** X/Y scale transformations offered by the builder. */
    public enum ScaleKind { AUTO, LOG10, SQRT, REVERSE, LIMITS }

    /** Color/fill scale strategies offered by the builder. */
    public enum ColorScaleKind { AUTO, VIRIDIS_C, VIRIDIS_D, BREWER, MANUAL }

    /** The library's three named themes. */
    public enum ThemeKind { GRAY, BW, DARK }

    /** Coordinate systems offered by the builder. */
    public enum CoordKind { CARTESIAN, FLIP, POLAR, EQUAL, COORD3D }

    /** Facet layouts offered by the builder. */
    public enum FacetKind { NONE, WRAP, GRID }

    /**
     * A grouping of {@link GeomKind}s by dimension/domain, used to fan the
     * builder's add-layer menu into labelled submenus. Today only {@link #TWO_D}
     * and {@link #THREE_D} exist; map- and graph-oriented geoms can be added as
     * further categories without touching the menu logic.
     */
    public enum GeomCategory {
        TWO_D("2D"), THREE_D("3D");

        private final String label;

        GeomCategory(String label) {
            this.label = label;
        }

        /** Human-readable label for the add-layer submenu. */
        public String label() {
            return label;
        }
    }

    /** Every geometry the library offers, with a short UI label. */
    public enum GeomKind {
        POINT("Point", GeomCategory.TWO_D),
        JITTER("Jitter", GeomCategory.TWO_D),
        LINE("Line", GeomCategory.TWO_D),
        SMOOTH("Smooth", GeomCategory.TWO_D),
        BAR("Bar", GeomCategory.TWO_D),
        COL("Column", GeomCategory.TWO_D),
        HISTOGRAM("Histogram", GeomCategory.TWO_D),
        FREQPOLY("Freqpoly", GeomCategory.TWO_D),
        DENSITY("Density", GeomCategory.TWO_D),
        DENSITY2D("Density 2D", GeomCategory.TWO_D),
        AREA("Area", GeomCategory.TWO_D),
        BOXPLOT("Boxplot", GeomCategory.TWO_D),
        VIOLIN("Violin", GeomCategory.TWO_D),
        TEXT("Text", GeomCategory.TWO_D),
        POLYGON("Polygon", GeomCategory.TWO_D),
        TILE("Tile", GeomCategory.TWO_D),
        PATH("Path", GeomCategory.TWO_D),
        STEP("Step", GeomCategory.TWO_D),
        SEGMENT("Segment", GeomCategory.TWO_D),
        CURVE("Curve", GeomCategory.TWO_D),
        CROSSBAR("Crossbar", GeomCategory.TWO_D),
        ERRORBAR("Errorbar", GeomCategory.TWO_D),
        ERRORBARH("Errorbar H", GeomCategory.TWO_D),
        LINERANGE("Line range", GeomCategory.TWO_D),
        POINTRANGE("Point range", GeomCategory.TWO_D),
        RIBBON("Ribbon", GeomCategory.TWO_D),
        HLINE("Horizontal line", GeomCategory.TWO_D),
        ABLINE("Ab line", GeomCategory.TWO_D),
        VLINE("Vertical line", GeomCategory.TWO_D),
        POINT3D("Point", GeomCategory.THREE_D),
        TEXT3D("Text", GeomCategory.THREE_D),
        SEGMENT3D("Segment", GeomCategory.THREE_D),
        PATH3D("Path", GeomCategory.THREE_D),
        BAR3D("Bar", GeomCategory.THREE_D),
        COL3D("Column", GeomCategory.THREE_D),
        SURFACE3D("Surface", GeomCategory.THREE_D),
        POLYGON3D("Polygon", GeomCategory.THREE_D),
        VOXEL3D("Voxel", GeomCategory.THREE_D),
        HULL3D("Hull", GeomCategory.THREE_D),
        RIDGELINE3D("Ridgeline", GeomCategory.THREE_D),
        CONTOUR3D("Contour", GeomCategory.THREE_D),
        SMOOTH3D("Smooth", GeomCategory.THREE_D),
        DENSITY3D("Density", GeomCategory.THREE_D),
        FUNCTION3D("Function", GeomCategory.THREE_D);

        private final String label;
        private final GeomCategory category;

        GeomKind(String label, GeomCategory category) {
            this.label = label;
            this.category = category;
        }

        /** Human-readable label for UI buttons and pane titles. */
        public String label() {
            return label;
        }

        /** The dimension/domain grouping this geometry belongs to. */
        public GeomCategory category() {
            return category;
        }
    }

    /** Parameter keys used by every geometry; absent key = library default. */
    public static final class Params {
        public static final String SIZE = "size";
        public static final String OPACITY = "opacity";
        public static final String WIDTH = "width";
        public static final String HEIGHT = "height";
        public static final String SPAN = "span";
        public static final String ADJUST = "adjust";
        public static final String BINS = "bins";
        public static final String ALPHA = "alpha";
        public static final String ANGLE = "angle";
        public static final String HJUST = "hjust";
        public static final String VJUST = "vjust";
        public static final String LINE_WIDTH = "lineWidth";
        public static final String POINT_RADIUS = "pointRadius";
        public static final String YINTERCEPT = "yintercept";
        public static final String XINTERCEPT = "xintercept";
        public static final String SLOPE = "slope";
        public static final String INTERCEPT = "intercept";
        public static final String COLOR = "color";
        public static final String FILL = "fill";
        public static final String METHOD = "method";
        public static final String SHAPE = "shape";
        public static final String POSITION = "position";
        public static final String SE = "se";
        public static final String BOLD = "bold";
        public static final String FILLED = "filled";
        public static final String DASHED = "dashed";

        // 3D primitives and solids.
        public static final String RAW_POINTS = "rawPoints";
        public static final String REF_LINES = "refLines";
        public static final String REF_POINTS = "refPoints";
        public static final String REF_FACES = "refFaces";
        public static final String REF_CIRCLE_RADIUS = "refCircleRadius";
        public static final String FACES = "faces";
        public static final String ZMIN = "zmin";
        public static final String BANDWIDTH = "bandwidth";
        public static final String N = "n";
        public static final String LEVEL = "level";
        public static final String RADIUS = "radius";
        public static final String BASE = "base";
        public static final String DIRECTION = "direction";
        public static final String GRID = "grid";
        public static final String DOMAIN = "domain";
        public static final String SORT_METHOD = "sortMethod";
        public static final String XLIM_MIN = "xlimMin";
        public static final String XLIM_MAX = "xlimMax";
        public static final String YLIM_MIN = "ylimMin";
        public static final String YLIM_MAX = "ylimMax";

        // coord3d view controls.
        public static final String PITCH = "pitch";
        public static final String ROLL = "roll";
        public static final String YAW = "yaw";
        public static final String DIST = "dist";
        public static final String ZOOM = "zoom";
        public static final String PERSP = "persp";
        public static final String SCALE_MODE = "scaleMode";
        public static final String PANELS = "panels";
        public static final String RATIO_X = "ratioX";
        public static final String RATIO_Y = "ratioY";
        public static final String RATIO_Z = "ratioZ";
        public static final String CLIP = "clip";
        public static final String EXPAND = "expand";
        public static final String LIGHT = "light";

        private Params() {
        }
    }

    /**
     * One geometry in the builder's layer stack. Parameters are stored in typed
     * maps keyed by {@link Params}; an absent key means "use the library
     * default", so the generated code only spells out what the user changed.
     */
    public static final class Geom {
        public final GeomKind kind;
        public final Map<String, Double> doubles = new HashMap<>();
        public final Map<String, Color> colors = new HashMap<>();
        public final Map<String, Enum<?>> enums = new HashMap<>();
        public final Map<String, Boolean> flags = new HashMap<>();

        public Geom(GeomKind kind) {
            this.kind = kind;
        }
    }

    private PlotDataset dataset;

    /** The dataset currently loaded in the builder, or {@code null} if none. */
    public PlotDataset dataset() {
        return dataset;
    }

    /**
     * Sets the dataset the builder assembles plots against. Selecting a new
     * dataset should clear the column bindings and geom layers, since columns
     * rarely transfer.
     *
     * @param dataset the new dataset, or {@code null} to clear
     */
    public void setDataset(PlotDataset dataset) {
        this.dataset = dataset;
    }

    public String x;
    public String y;
    public String z;
    public String color;
    public String fill;
    public String shape;
    public String size;
    public String alpha;
    public String group;
    public String xmin;
    public String xmax;
    public String ymin;
    public String ymax;
    public String xend;
    public String yend;
    public String zend;

    public final List<Geom> geoms = new ArrayList<>();

    public ScaleKind scaleX = ScaleKind.AUTO;
    public ScaleKind scaleY = ScaleKind.AUTO;
    public Double scaleXMin;
    public Double scaleXMax;
    public Double scaleYMin;
    public Double scaleYMax;

    public ColorScaleKind colorScale = ColorScaleKind.AUTO;
    public ColorScaleKind fillScale = ColorScaleKind.AUTO;
    public String colorPalette;
    public String fillPalette;
    public final List<Color> manualColors = new ArrayList<>();

    public ThemeKind theme = ThemeKind.GRAY;
    public boolean showXGrid = true;
    public boolean showYGrid = true;

    public CoordKind coord = CoordKind.CARTESIAN;
    public Double coordXLimMin;
    public Double coordXLimMax;
    public Double coordYLimMin;
    public Double coordYLimMax;
    public String polarTheta;
    public Double polarStart;
    public Double equalRatio;

    // coord3d() view controls.
    public Double coord3dPitch;
    public Double coord3dRoll;
    public Double coord3dYaw;
    public Double coord3dDist;
    public Double coord3dZoom;
    public Boolean coord3dPersp;
    public ScaleMode coord3dScaleMode;
    public CubePanel coord3dPanels;
    public Double coord3dRatioX;
    public Double coord3dRatioY;
    public Double coord3dRatioZ;
    public Boolean coord3dClip;
    public Boolean coord3dExpand;
    public Boolean coord3dLight;

    // Facets.wrap()/Facets.grid() layout.
    public FacetKind facet = FacetKind.NONE;
    public String facetWrapColumn;
    public Integer facetWrapCols;
    public String facetGridRow;
    public String facetGridCol;
    public GridOptions.Scale facetScale =
            GridOptions.Scale.FIXED;
    public GridOptions.Space facetSpace =
            GridOptions.Space.FIXED;
    public GridOptions.GridSwitch facetSwitch =
            GridOptions.GridSwitch.NONE;
    public GridOptions.Axes facetAxes =
            GridOptions.Axes.MARGINS;
    public boolean facetMargins;
    public boolean facetAsTable = true;
    public boolean facetDrop = true;

    public String title;
    public String xLabel;
    public String yLabel;

    public RenderMode renderMode = RenderMode.FAST;

    /** Builds the aesthetic mapping from the column bindings that are set. */
    public Aes toAes() {
        var aes = Aes.aes();
        if (x != null) aes.x(x);
        if (y != null) aes.y(y);
        if (z != null) aes.z(z);
        if (color != null) aes.color(color);
        if (fill != null) aes.fill(fill);
        if (shape != null) aes.shape(shape);
        if (size != null) aes.size(size);
        if (alpha != null) aes.alpha(alpha);
        if (group != null) aes.group(group);
        if (xmin != null) aes.xmin(xmin);
        if (xmax != null) aes.xmax(xmax);
        if (ymin != null) aes.ymin(ymin);
        if (ymax != null) aes.ymax(ymax);
        if (xend != null) aes.xend(xend);
        if (yend != null) aes.yend(yend);
        if (zend != null) aes.zend(zend);
        return aes;
    }

    /**
     * The distinct values of a column in first-appearance order — what a manual
     * color scale maps, shared by the preview and the generated code so they
     * agree. Delegates to the current {@link PlotDataset}.
     *
     * @param column the mapped column
     * @return the distinct cell values, in first-appearance order
     */
    public List<String> distinctValues(String column) {
        return dataset == null ? List.of() : dataset.distinctValues(column);
    }

    /**
     * Builds the {@code GridOptions} for a grid facet from the current model,
     * applying only the options that differ from the library defaults so the
     * preview and the generated code agree.
     *
     * @return the grid options described by the model
     */
    public GridOptions toGridOptions() {
        var options = GridOptions.defaults();
        if (facetScale != GridOptions.Scale.FIXED) {
            options = options.withScale(facetScale);
        }
        if (facetSpace != GridOptions.Space.FIXED) {
            options = options.withSpace(facetSpace);
        }
        if (facetSwitch != GridOptions.GridSwitch.NONE) {
            options = options.withSwitch(facetSwitch);
        }
        if (facetAxes != GridOptions.Axes.MARGINS) {
            options = options.withAxes(facetAxes);
        }
        if (facetMargins) {
            options = options.withMargins();
        }
        if (!facetAsTable) {
            options = options.withAsTable(false);
        }
        if (!facetDrop) {
            options = options.withDrop(false);
        }
        return options;
    }

    /** {@return whether any grid facet option differs from the defaults} */
    public boolean hasNonDefaultGridOptions() {
        return facetScale != GridOptions.Scale.FIXED
                || facetSpace != GridOptions.Space.FIXED
                || facetSwitch != GridOptions.GridSwitch.NONE
                || facetAxes != GridOptions.Axes.MARGINS
                || facetMargins
                || !facetAsTable
                || !facetDrop;
    }
}
