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
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.control.Accordion;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.util.StringConverter;
import org.jtaccuino.gog.RenderMode;
import org.jtaccuino.gog.controls.PlotDataset;
import org.jtaccuino.gog.controls.SourceDrawer;
import org.jtaccuino.gog.coord.CubePanel;
import org.jtaccuino.gog.coord.ScaleMode;
import org.jtaccuino.gog.facet.GridOptions;
import org.jtaccuino.gog.geometry.GridDirection;
import org.jtaccuino.gog.geometry.GridGeometry;
import org.jtaccuino.gog.geometry.HullMethod;
import org.jtaccuino.gog.geometry.SurfaceMethod;
import org.jtaccuino.gog.layer.PointShape;
import org.jtaccuino.gog.layer.Position;
import org.jtaccuino.gog.stat.SmoothMethod;

/**
 * Interactive visual builder for single plots: pick a dataset and the columns
 * to map, add and configure geometries, and choose scales, theme, coordinate
 * system and labels. The plot preview updates live, and the {@link SourceDrawer}
 * beneath it shows the idiomatic gog4j Java that recreates the exact plot seen.
 * <p>
 * The control is data-agnostic: datasets are supplied by the host through
 * {@link #setDatasets(List)}, so the builder carries no DataFrame dependency.
 * {@code ./gradlew runBuilder} launches the standalone sampler application.
 */
public class PlotBuilder extends BorderPane {

    /** Sentinel value rendered as a "(none)"/"(default)" placeholder in combos. */
    private static final Object PLACEHOLDER = new Object();

    private final BuilderModel model = new BuilderModel();
    private List<PlotDataset> datasets = List.of();

    private final StackPane preview = new StackPane();
    private final SourceDrawer sourceDrawer = new SourceDrawer();
    private final VBox geomsBox = new VBox(6);

    private final ComboBox<Object> xCombo = columnCombo(v -> model.x = v);
    private final ComboBox<Object> yCombo = columnCombo(v -> model.y = v);
    private final ComboBox<Object> zCombo = columnCombo(v -> model.z = v);
    private final ComboBox<Object> colorCombo = columnCombo(v -> model.color = v);
    private final ComboBox<Object> fillCombo = columnCombo(v -> model.fill = v);
    private final ComboBox<Object> shapeCombo = columnCombo(v -> model.shape = v);
    private final ComboBox<Object> sizeCombo = columnCombo(v -> model.size = v);
    private final ComboBox<Object> alphaCombo = columnCombo(v -> model.alpha = v);
    private final ComboBox<Object> groupCombo = columnCombo(v -> model.group = v);
    private final ComboBox<Object> xminCombo = columnCombo(v -> model.xmin = v);
    private final ComboBox<Object> xmaxCombo = columnCombo(v -> model.xmax = v);
    private final ComboBox<Object> yminCombo = columnCombo(v -> model.ymin = v);
    private final ComboBox<Object> ymaxCombo = columnCombo(v -> model.ymax = v);
    private final ComboBox<Object> xendCombo = columnCombo(v -> model.xend = v);
    private final ComboBox<Object> yendCombo = columnCombo(v -> model.yend = v);
    private final ComboBox<Object> zendCombo = columnCombo(v -> model.zend = v);
    private final ComboBox<Object> facetWrapCombo = columnCombo(v -> model.facetWrapColumn = v);
    private final ComboBox<Object> facetRowCombo = columnCombo(v -> model.facetGridRow = v);
    private final ComboBox<Object> facetColCombo = columnCombo(v -> model.facetGridCol = v);
    private final ComboBox<Object> polarThetaCombo = columnCombo(v -> {
        model.polarTheta = v;
        scheduleRefresh();
    });

    private boolean refreshing;
    private boolean refreshScheduled;

    /**
     * Builds the builder UI. Datasets are supplied separately via
     * {@link #setDatasets}.
     */
    @SuppressWarnings("this-escape")
    public PlotBuilder() {
        var accordion = new Accordion();
        accordion.setPrefWidth(320);
        accordion.getPanes().addAll(
                pane("Dataset", datasetPane()),
                pane("Mapping", mappingPane()),
                pane("Geometries", geomsPane()),
                pane("Scales", scalesPane()),
                pane("Coordinate system", coordPane()),
                pane("Facets", facetsPane()),
                pane("Labels", labelsPane()),
                pane("Render mode", renderPane()));
        accordion.setExpandedPane(accordion.getPanes().get(0));

        var scroll = new ScrollPane(accordion);
        scroll.setFitToWidth(true);

        preview.setStyle("-fx-background-color: white;");
        preview.setPrefSize(900, 560);
        StackPane.setAlignment(sourceDrawer, Pos.BOTTOM_CENTER);

        var center = new StackPane(preview, sourceDrawer);

        setLeft(scroll);
        setCenter(center);
    }

    /**
     * Sets the datasets the builder offers in its picker, selecting the first
     * and re-populating the column bindings. Call before the control is shown.
     *
     * @param datasets the available datasets
     */
    public void setDatasets(List<PlotDataset> datasets) {
        this.datasets = datasets == null ? List.of() : datasets;
        if (this.datasets.isEmpty()) {
            model.setDataset(null);
        } else {
            model.setDataset(this.datasets.get(0));
        }
        datasetCombo.getItems().setAll(this.datasets);
        datasetCombo.setValue(model.dataset());
        reloadColumns();
        refresh();
    }

    private final ComboBox<PlotDataset> datasetCombo = new ComboBox<>();

    // ─── Sections (accordion panes) ─────────────────────────────────────

    private static TitledPane pane(String title, Node content) {
        var pane = new TitledPane(title, content);
        pane.setAnimated(false);
        return pane;
    }

    private VBox datasetPane() {
        datasetCombo.setMaxWidth(Double.MAX_VALUE);
        datasetCombo.setConverter(new StringConverter<PlotDataset>() {
            @Override
            public String toString(PlotDataset dataset) {
                return dataset == null ? "" : dataset.displayName();
            }

            @Override
            public PlotDataset fromString(String text) {
                return null;
            }
        });
        datasetCombo.valueProperty().addListener((obs, old, now) -> {
            if (now != null && !now.equals(model.dataset())) {
                model.setDataset(now);
                reloadColumns();
                scheduleRefresh();
            }
        });
        return new VBox(4, datasetCombo);
    }

    private GridPane mappingPane() {
        var grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(4);
        int row = 0;
        grid.addRow(row++, new Label("x"), xCombo);
        grid.addRow(row++, new Label("y"), yCombo);
        grid.addRow(row++, new Label("z"), zCombo);
        grid.addRow(row++, new Label("color"), colorCombo);
        grid.addRow(row++, new Label("fill"), fillCombo);
        grid.addRow(row++, new Label("shape"), shapeCombo);
        grid.addRow(row++, new Label("size"), sizeCombo);
        grid.addRow(row++, new Label("alpha"), alphaCombo);
        grid.addRow(row++, new Label("group"), groupCombo);
        grid.addRow(row++, new Label("xmin"), xminCombo);
        grid.addRow(row++, new Label("xmax"), xmaxCombo);
        grid.addRow(row++, new Label("ymin"), yminCombo);
        grid.addRow(row++, new Label("ymax"), ymaxCombo);
        grid.addRow(row++, new Label("xend"), xendCombo);
        grid.addRow(row++, new Label("yend"), yendCombo);
        grid.addRow(row++, new Label("zend"), zendCombo);
        return grid;
    }

    private VBox geomsPane() {
        var add = new Button("+ Add layer");
        var menu = new ContextMenu();
        for (var category : BuilderModel.GeomCategory.values()) {
            var submenu = new Menu(category.label());
            for (var kind : BuilderModel.GeomKind.values()) {
                if (kind.category() != category) {
                    continue;
                }
                var item = new MenuItem(kind.label());
                item.setOnAction(e -> {
                    model.geoms.add(new BuilderModel.Geom(kind));
                    rebuildGeomPanes();
                    scheduleRefresh();
                });
                submenu.getItems().add(item);
            }
            if (!submenu.getItems().isEmpty()) {
                menu.getItems().add(submenu);
            }
        }
        add.setOnMouseClicked(e -> menu.show(add, Side.BOTTOM, 0, 0));
        rebuildGeomPanes();
        return new VBox(8, add, geomsBox);
    }

    private VBox scalesPane() {
        var xScale = nullableCombo("(auto)", List.of(BuilderModel.ScaleKind.values()), model.scaleX,
                v -> {
                    model.scaleX = v == null ? BuilderModel.ScaleKind.AUTO : v;
                    scheduleRefresh();
                });
        var xMin = doubleField(model.scaleXMin, v -> model.scaleXMin = v);
        var xMax = doubleField(model.scaleXMax, v -> model.scaleXMax = v);
        var yScale = nullableCombo("(auto)", List.of(BuilderModel.ScaleKind.values()), model.scaleY,
                v -> {
                    model.scaleY = v == null ? BuilderModel.ScaleKind.AUTO : v;
                    scheduleRefresh();
                });
        var yMin = doubleField(model.scaleYMin, v -> model.scaleYMin = v);
        var yMax = doubleField(model.scaleYMax, v -> model.scaleYMax = v);

        var colorScale = nullableCombo("(auto)", List.of(BuilderModel.ColorScaleKind.values()), model.colorScale,
                v -> {
                    model.colorScale = v == null ? BuilderModel.ColorScaleKind.AUTO : v;
                    scheduleRefresh();
                });
        var colorPalette = textField(model.colorPalette, "e.g. Set2", v -> model.colorPalette = v);
        var manualColors = textField(null, "#4c72b0,#dd8452,#55a868", this::applyManualColors);
        var fillScale = nullableCombo("(auto)", List.of(BuilderModel.ColorScaleKind.values()), model.fillScale,
                v -> {
                    model.fillScale = v == null ? BuilderModel.ColorScaleKind.AUTO : v;
                    scheduleRefresh();
                });
        var fillPalette = textField(model.fillPalette, "e.g. Set2", v -> model.fillPalette = v);

        xScale.valueProperty().addListener((o, old, now) ->
                setEnabled(xMin, xMax, now == BuilderModel.ScaleKind.LIMITS));
        yScale.valueProperty().addListener((o, old, now) ->
                setEnabled(yMin, yMax, now == BuilderModel.ScaleKind.LIMITS));
        colorScale.valueProperty().addListener((o, old, now) -> {
            colorPalette.setDisable(now != BuilderModel.ColorScaleKind.BREWER);
            manualColors.setDisable(now != BuilderModel.ColorScaleKind.MANUAL);
        });
        fillScale.valueProperty().addListener((o, old, now) ->
                fillPalette.setDisable(now != BuilderModel.ColorScaleKind.BREWER));
        setEnabled(xMin, xMax, model.scaleX == BuilderModel.ScaleKind.LIMITS);
        setEnabled(yMin, yMax, model.scaleY == BuilderModel.ScaleKind.LIMITS);
        colorPalette.setDisable(model.colorScale != BuilderModel.ColorScaleKind.BREWER);
        manualColors.setDisable(model.colorScale != BuilderModel.ColorScaleKind.MANUAL);
        fillPalette.setDisable(model.fillScale != BuilderModel.ColorScaleKind.BREWER);

        return new VBox(8,
                labeled("x scale", xScale), new HBox(4, xMin, xMax),
                labeled("y scale", yScale), new HBox(4, yMin, yMax),
                labeled("color scale", colorScale),
                labeled("palette", colorPalette),
                labeled("manual colors", manualColors),
                labeled("fill scale", fillScale),
                labeled("fill palette", fillPalette));
    }

    private VBox coordPane() {
        var coord = nullableCombo("(default)", List.of(BuilderModel.CoordKind.values()), model.coord,
                v -> {
                    model.coord = v == null ? BuilderModel.CoordKind.CARTESIAN : v;
                    scheduleRefresh();
                });
        var xLimMin = doubleField(model.coordXLimMin, v -> model.coordXLimMin = v);
        var xLimMax = doubleField(model.coordXLimMax, v -> model.coordXLimMax = v);
        var yLimMin = doubleField(model.coordYLimMin, v -> model.coordYLimMin = v);
        var yLimMax = doubleField(model.coordYLimMax, v -> model.coordYLimMax = v);
        var ratio = doubleField(model.equalRatio, v -> model.equalRatio = v);
        var theta = polarThetaCombo;
        var start = doubleField(model.polarStart, v -> model.polarStart = v);

        // coord3d() view controls.
        var pitch = doubleField(model.coord3dPitch, v -> model.coord3dPitch = v);
        var roll = doubleField(model.coord3dRoll, v -> model.coord3dRoll = v);
        var yaw = doubleField(model.coord3dYaw, v -> model.coord3dYaw = v);
        var dist = doubleField(model.coord3dDist, v -> model.coord3dDist = v);
        var zoom = doubleField(model.coord3dZoom, v -> model.coord3dZoom = v);
        var ratio3d = new HBox(4,
                doubleField(model.coord3dRatioX, v -> model.coord3dRatioX = v),
                doubleField(model.coord3dRatioY, v -> model.coord3dRatioY = v),
                doubleField(model.coord3dRatioZ, v -> model.coord3dRatioZ = v));
        var panels = nullableCombo("(default)", List.of(CubePanel.values()), model.coord3dPanels,
                v -> {
                    model.coord3dPanels = v;
                    scheduleRefresh();
                });
        var scaleMode = nullableCombo("(default)", List.of(ScaleMode.values()), model.coord3dScaleMode,
                v -> {
                    model.coord3dScaleMode = v;
                    scheduleRefresh();
                });
        var persp = booleanCombo(model.coord3dPersp, v -> model.coord3dPersp = v);
        var clip = booleanCombo(model.coord3dClip, v -> model.coord3dClip = v);
        var expand = booleanCombo(model.coord3dExpand, v -> model.coord3dExpand = v);
        var light = booleanCombo(model.coord3dLight, v -> model.coord3dLight = v);

        var threeD = new VBox(4,
                labeled("pitch", pitch), labeled("roll", roll), labeled("yaw", yaw),
                labeled("distance", dist), labeled("zoom", zoom),
                labeled("aspect (x,y,z)", ratio3d),
                labeled("panels", panels), labeled("scales", scaleMode),
                labeled("perspective", persp), labeled("clip", clip),
                labeled("expand", expand), labeled("default light", light));

        coord.valueProperty().addListener((o, old, now) -> {
            boolean limits = now == BuilderModel.CoordKind.CARTESIAN || now == BuilderModel.CoordKind.FLIP;
            boolean polar = now == BuilderModel.CoordKind.POLAR;
            boolean equal = now == BuilderModel.CoordKind.EQUAL;
            boolean is3d = now == BuilderModel.CoordKind.COORD3D;
            setEnabled(xLimMin, xLimMax, yLimMin, yLimMax, limits);
            theta.setDisable(!polar);
            start.setDisable(!polar);
            ratio.setDisable(!equal);
            threeD.setDisable(!is3d);
        });
        setEnabled(xLimMin, xLimMax, yLimMin, yLimMax,
                model.coord == BuilderModel.CoordKind.CARTESIAN || model.coord == BuilderModel.CoordKind.FLIP);
        theta.setDisable(model.coord != BuilderModel.CoordKind.POLAR);
        start.setDisable(model.coord != BuilderModel.CoordKind.POLAR);
        ratio.setDisable(model.coord != BuilderModel.CoordKind.EQUAL);
        threeD.setDisable(model.coord != BuilderModel.CoordKind.COORD3D);

        var limits = new VBox(4, labeled("x limits", new HBox(4, xLimMin, xLimMax)),
                labeled("y limits", new HBox(4, yLimMin, yLimMax)));
        return new VBox(8, labeled("Coordinate system", coord),
                limits, labeled("polar theta (column)", theta), labeled("polar start (°)", start),
                labeled("aspect ratio", ratio), threeD);
    }

    private VBox facetsPane() {
        var kind = nullableCombo("(none)",
                List.of(BuilderModel.FacetKind.WRAP, BuilderModel.FacetKind.GRID),
                model.facet == BuilderModel.FacetKind.NONE ? null : model.facet,
                v -> {
                    model.facet = v == null ? BuilderModel.FacetKind.NONE : v;
                    scheduleRefresh();
                });

        var wrapCols = intField(model.facetWrapCols, v -> model.facetWrapCols = v);
        var wrapBox = new VBox(4,
                labeled("wrap column", facetWrapCombo),
                labeled("columns (-1 auto)", wrapCols));

        var scale = nullableCombo("(default)", List.of(GridOptions.Scale.values()), model.facetScale,
                v -> {
                    model.facetScale = v == null ? GridOptions.Scale.FIXED : v;
                    scheduleRefresh();
                });
        var space = nullableCombo("(default)", List.of(GridOptions.Space.values()), model.facetSpace,
                v -> {
                    model.facetSpace = v == null ? GridOptions.Space.FIXED : v;
                    scheduleRefresh();
                });
        var stripSwitch = nullableCombo("(default)", List.of(GridOptions.GridSwitch.values()),
                model.facetSwitch, v -> {
                    model.facetSwitch = v == null ? GridOptions.GridSwitch.NONE : v;
                    scheduleRefresh();
                });
        var axes = nullableCombo("(default)", List.of(GridOptions.Axes.values()), model.facetAxes,
                v -> {
                    model.facetAxes = v == null ? GridOptions.Axes.MARGINS : v;
                    scheduleRefresh();
                });
        var margins = booleanCombo(model.facetMargins, v -> model.facetMargins = v != null && v);
        var asTable = booleanCombo(model.facetAsTable, v -> model.facetAsTable = v == null || v);
        var drop = booleanCombo(model.facetDrop, v -> model.facetDrop = v == null || v);
        var gridBox = new VBox(4,
                labeled("row", facetRowCombo), labeled("column", facetColCombo),
                labeled("scales", scale), labeled("space", space),
                labeled("switch", stripSwitch), labeled("axes", axes),
                labeled("margins", margins), labeled("as table", asTable),
                labeled("drop empty", drop));

        kind.valueProperty().addListener((o, old, now) -> {
            wrapBox.setDisable(now != BuilderModel.FacetKind.WRAP);
            gridBox.setDisable(now != BuilderModel.FacetKind.GRID);
        });
        wrapBox.setDisable(model.facet != BuilderModel.FacetKind.WRAP);
        gridBox.setDisable(model.facet != BuilderModel.FacetKind.GRID);

        return new VBox(8, labeled("Facet", kind), wrapBox, gridBox);
    }

    private VBox labelsPane() {
        return new VBox(4,
                labeled("Title", textField(model.title, "(none)", v -> model.title = v)),
                labeled("x label", textField(model.xLabel, "(none)", v -> model.xLabel = v)),
                labeled("y label", textField(model.yLabel, "(none)", v -> model.yLabel = v)));
    }

    private VBox renderPane() {
        var mode = new ComboBox<RenderMode>();
        mode.setMaxWidth(Double.MAX_VALUE);
        mode.getItems().addAll(RenderMode.FAST, RenderMode.FULL);
        mode.setValue(model.renderMode);
        mode.valueProperty().addListener((o, old, now) -> {
            model.renderMode = now == null ? RenderMode.FAST : now;
            scheduleRefresh();
        });
        return new VBox(4, mode);
    }

    // ─── Geom layers ─────────────────────────────────────────────────────

    private void rebuildGeomPanes() {
        geomsBox.getChildren().clear();
        for (var geom : model.geoms) {
            geomsBox.getChildren().add(geomPane(geom));
        }
    }

    private TitledPane geomPane(BuilderModel.Geom geom) {
        var remove = new Button("✕");
        remove.setStyle("-fx-padding: 0 6 0 6;");
        remove.setOnAction(e -> {
            model.geoms.remove(geom);
            rebuildGeomPanes();
            scheduleRefresh();
        });
        var pane = new TitledPane(geom.kind.label(), geomParams(geom));
        pane.setAnimated(false);
        pane.setGraphic(remove);
        return pane;
    }

    private VBox geomParams(BuilderModel.Geom g) {
        var box = new VBox(4);
        switch (g.kind) {
            case POINT -> {
                box.getChildren().addAll(
                        optionalDouble("size", g, BuilderModel.Params.SIZE, 0.1, 12, 2.0),
                        optionalDouble("opacity", g, BuilderModel.Params.OPACITY, 0.0, 1.0, 0.9),
                        optionalColor("color", g, BuilderModel.Params.COLOR),
                        enumRow("shape", g, BuilderModel.Params.SHAPE, PointShape.values()));
            }
            case JITTER -> box.getChildren().addAll(
                    optionalDouble("width", g, BuilderModel.Params.WIDTH, 0.0, 1.0, 0.2),
                    optionalDouble("height", g, BuilderModel.Params.HEIGHT, 0.0, 1.0, 0.2),
                    optionalDouble("size", g, BuilderModel.Params.SIZE, 0.1, 12, 2.0),
                    optionalDouble("opacity", g, BuilderModel.Params.OPACITY, 0.0, 1.0, 0.9),
                    optionalColor("color", g, BuilderModel.Params.COLOR),
                    enumRow("shape", g, BuilderModel.Params.SHAPE, PointShape.values()));
            case SMOOTH -> box.getChildren().addAll(
                    enumRow("method", g, BuilderModel.Params.METHOD, SmoothMethod.values()),
                    optionalDouble("span", g, BuilderModel.Params.SPAN, 0.1, 2.0, 0.8),
                    flagRow("se", g, BuilderModel.Params.SE),
                    optionalColor("color", g, BuilderModel.Params.COLOR));
            case BAR, COL -> box.getChildren().addAll(
                    enumRow("position", g, BuilderModel.Params.POSITION, Position.values()),
                    optionalColor("fill", g, BuilderModel.Params.FILL));
            case HISTOGRAM -> box.getChildren().addAll(
                    optionalDouble("bins", g, BuilderModel.Params.BINS, 1, 200, 30),
                    optionalColor("fill", g, BuilderModel.Params.FILL));
            case FREQPOLY -> box.getChildren().addAll(
                    optionalDouble("bins", g, BuilderModel.Params.BINS, 1, 200, 30),
                    optionalDouble("width", g, BuilderModel.Params.WIDTH, 0.0, 5.0, 1.0),
                    optionalColor("color", g, BuilderModel.Params.COLOR));
            case DENSITY -> box.getChildren().addAll(
                    optionalDouble("adjust", g, BuilderModel.Params.ADJUST, 0.1, 3.0, 1.0),
                    optionalColor("fill", g, BuilderModel.Params.FILL));
            case DENSITY2D -> box.getChildren().addAll(
                    optionalDouble("adjust", g, BuilderModel.Params.ADJUST, 0.1, 3.0, 1.0),
                    optionalDouble("bins", g, BuilderModel.Params.BINS, 1, 100, 30),
                    flagRow("filled", g, BuilderModel.Params.FILLED),
                    optionalDouble("alpha", g, BuilderModel.Params.ALPHA, 0.0, 1.0, 0.7));
            case AREA -> box.getChildren().addAll(
                    optionalColor("fill", g, BuilderModel.Params.FILL),
                    enumRow("position", g, BuilderModel.Params.POSITION, Position.values()));
            case BOXPLOT -> box.getChildren().addAll(
                    optionalColor("fill", g, BuilderModel.Params.FILL),
                    optionalColor("color", g, BuilderModel.Params.COLOR));
            case VIOLIN -> box.getChildren().add(
                    optionalColor("fill", g, BuilderModel.Params.FILL));
            case TEXT -> box.getChildren().addAll(
                    optionalDouble("size", g, BuilderModel.Params.SIZE, 1, 40, 12),
                    optionalColor("color", g, BuilderModel.Params.COLOR),
                    flagRow("bold", g, BuilderModel.Params.BOLD),
                    optionalDouble("angle", g, BuilderModel.Params.ANGLE, -90, 90, 0),
                    optionalDouble("hjust", g, BuilderModel.Params.HJUST, -2, 2, 0),
                    optionalDouble("vjust", g, BuilderModel.Params.VJUST, -2, 2, 0));
            case POINT3D -> box.getChildren().addAll(
                    optionalDouble("size", g, BuilderModel.Params.SIZE, 0.1, 12, 2.0),
                    optionalDouble("opacity", g, BuilderModel.Params.OPACITY, 0.0, 1.0, 0.9),
                    optionalColor("color", g, BuilderModel.Params.COLOR),
                    enumRow("shape", g, BuilderModel.Params.SHAPE, PointShape.values()));
            case TEXT3D -> box.getChildren().addAll(
                    optionalDouble("size", g, BuilderModel.Params.SIZE, 1, 40, 12),
                    optionalColor("color", g, BuilderModel.Params.COLOR),
                    flagRow("bold", g, BuilderModel.Params.BOLD));
            case SEGMENT3D, PATH3D -> box.getChildren().addAll(
                    optionalColor("color", g, BuilderModel.Params.COLOR),
                    optionalDouble("linewidth", g, BuilderModel.Params.LINE_WIDTH, 0.0, 5.0, 1.0));
            case BAR3D -> box.getChildren().addAll(
                    optionalColor("fill", g, BuilderModel.Params.FILL),
                    optionalColor("color", g, BuilderModel.Params.COLOR),
                    optionalDouble("bins", g, BuilderModel.Params.BINS, 1, 50, 10),
                    optionalDouble("width", g, BuilderModel.Params.WIDTH, 0.0, 1.0, 0.9));
            case COL3D -> box.getChildren().addAll(
                    optionalColor("fill", g, BuilderModel.Params.FILL),
                    optionalColor("color", g, BuilderModel.Params.COLOR),
                    optionalDouble("width", g, BuilderModel.Params.WIDTH, 0.0, 1.0, 0.9),
                    optionalDouble("zmin", g, BuilderModel.Params.ZMIN, -1000, 1000, 0.0));
            case SURFACE3D -> box.getChildren().addAll(
                    optionalColor("fill", g, BuilderModel.Params.FILL),
                    optionalColor("color", g, BuilderModel.Params.COLOR),
                    optionalDouble("linewidth", g, BuilderModel.Params.LINE_WIDTH, 0.0, 5.0, 0.5),
                    enumRow("method", g, BuilderModel.Params.METHOD, SurfaceMethod.values()),
                    enumRow("grid", g, BuilderModel.Params.GRID, GridGeometry.values()));
            case POLYGON3D -> box.getChildren().addAll(
                    optionalColor("fill", g, BuilderModel.Params.FILL),
                    optionalColor("color", g, BuilderModel.Params.COLOR),
                    optionalDouble("alpha", g, BuilderModel.Params.ALPHA, 0.0, 1.0, 0.7),
                    optionalDouble("linewidth", g, BuilderModel.Params.LINE_WIDTH, 0.0, 5.0, 1.0));
            case VOXEL3D -> box.getChildren().addAll(
                    optionalColor("fill", g, BuilderModel.Params.FILL),
                    optionalColor("color", g, BuilderModel.Params.COLOR),
                    optionalDouble("width", g, BuilderModel.Params.WIDTH, 0.0, 1.0, 0.9));
            case HULL3D -> box.getChildren().addAll(
                    enumRow("method", g, BuilderModel.Params.METHOD, HullMethod.values()),
                    optionalDouble("radius", g, BuilderModel.Params.RADIUS, 0.0, 10.0, 0.6),
                    optionalColor("fill", g, BuilderModel.Params.FILL),
                    optionalColor("color", g, BuilderModel.Params.COLOR));
            case RIDGELINE3D -> box.getChildren().addAll(
                    enumRow("direction", g, BuilderModel.Params.DIRECTION, GridDirection.values()),
                    optionalDouble("base", g, BuilderModel.Params.BASE, -1000, 1000, 0.0),
                    optionalColor("fill", g, BuilderModel.Params.FILL),
                    optionalColor("color", g, BuilderModel.Params.COLOR));
            case CONTOUR3D -> box.getChildren().addAll(
                    optionalDouble("bins", g, BuilderModel.Params.BINS, 1, 50, 10),
                    optionalDouble("alpha", g, BuilderModel.Params.ALPHA, 0.0, 1.0, 0.85),
                    optionalColor("fill", g, BuilderModel.Params.FILL),
                    optionalColor("color", g, BuilderModel.Params.COLOR));
            case SMOOTH3D -> box.getChildren().addAll(
                    enumRow("method", g, BuilderModel.Params.METHOD, SmoothMethod.values()),
                    optionalDouble("span", g, BuilderModel.Params.SPAN, 0.1, 2.0, 0.8),
                    optionalDouble("level", g, BuilderModel.Params.LEVEL, 0.0, 1.0, 0.95),
                    flagRow("se", g, BuilderModel.Params.SE),
                    optionalColor("fill", g, BuilderModel.Params.FILL),
                    optionalColor("color", g, BuilderModel.Params.COLOR));
            case DENSITY3D -> box.getChildren().addAll(
                    optionalDouble("adjust", g, BuilderModel.Params.ADJUST, 0.1, 3.0, 1.0),
                    optionalDouble("n", g, BuilderModel.Params.N, 5, 200, 30),
                    optionalColor("fill", g, BuilderModel.Params.FILL),
                    optionalColor("color", g, BuilderModel.Params.COLOR));
            case FUNCTION3D -> box.getChildren().addAll(
                    optionalDouble("xlim min", g, BuilderModel.Params.XLIM_MIN, -1000, 1000, -3),
                    optionalDouble("xlim max", g, BuilderModel.Params.XLIM_MAX, -1000, 1000, 3),
                    optionalDouble("ylim min", g, BuilderModel.Params.YLIM_MIN, -1000, 1000, -3),
                    optionalDouble("ylim max", g, BuilderModel.Params.YLIM_MAX, -1000, 1000, 3),
                    optionalDouble("n", g, BuilderModel.Params.N, 5, 200, 30),
                    optionalColor("fill", g, BuilderModel.Params.FILL),
                    optionalColor("color", g, BuilderModel.Params.COLOR));
            case POLYGON -> box.getChildren().addAll(
                    optionalColor("color", g, BuilderModel.Params.COLOR),
                    optionalDouble("alpha", g, BuilderModel.Params.ALPHA, 0.0, 1.0, 0.7),
                    optionalDouble("lineWidth", g, BuilderModel.Params.LINE_WIDTH, 0.0, 5.0, 1.0),
                    flagRow("filled", g, BuilderModel.Params.FILLED));
            case CROSSBAR -> box.getChildren().addAll(
                    optionalColor("fill", g, BuilderModel.Params.FILL),
                    optionalColor("color", g, BuilderModel.Params.COLOR),
                    optionalDouble("width", g, BuilderModel.Params.WIDTH, 0.0, 5.0, 1.0));
            case ERRORBAR, ERRORBARH, LINERANGE -> box.getChildren().addAll(
                    optionalColor("color", g, BuilderModel.Params.COLOR),
                    optionalDouble("width", g, BuilderModel.Params.WIDTH, 0.0, 5.0, 1.0));
            case POINTRANGE -> box.getChildren().addAll(
                    optionalColor("color", g, BuilderModel.Params.COLOR),
                    optionalDouble("width", g, BuilderModel.Params.WIDTH, 0.0, 5.0, 1.0),
                    optionalDouble("pointRadius", g, BuilderModel.Params.POINT_RADIUS, 0.1, 5.0, 1.0));
            case RIBBON -> box.getChildren().addAll(
                    optionalColor("fill", g, BuilderModel.Params.FILL),
                    optionalDouble("alpha", g, BuilderModel.Params.ALPHA, 0.0, 1.0, 0.7));
            case HLINE -> box.getChildren().addAll(
                    requiredDouble("yintercept", g, BuilderModel.Params.YINTERCEPT, 0.0),
                    optionalColor("color", g, BuilderModel.Params.COLOR),
                    optionalDouble("width", g, BuilderModel.Params.WIDTH, 0.0, 5.0, 1.0),
                    flagRow("dashed", g, BuilderModel.Params.DASHED));
            case VLINE -> box.getChildren().addAll(
                    requiredDouble("xintercept", g, BuilderModel.Params.XINTERCEPT, 0.0),
                    optionalColor("color", g, BuilderModel.Params.COLOR),
                    optionalDouble("width", g, BuilderModel.Params.WIDTH, 0.0, 5.0, 1.0),
                    flagRow("dashed", g, BuilderModel.Params.DASHED));
            case ABLINE -> box.getChildren().addAll(
                    requiredDouble("slope", g, BuilderModel.Params.SLOPE, 0.0),
                    requiredDouble("intercept", g, BuilderModel.Params.INTERCEPT, 0.0),
                    optionalColor("color", g, BuilderModel.Params.COLOR),
                    optionalDouble("width", g, BuilderModel.Params.WIDTH, 0.0, 5.0, 1.0),
                    flagRow("dashed", g, BuilderModel.Params.DASHED));
            case TILE, PATH, STEP, SEGMENT, CURVE, LINE -> {
                box.getChildren().add(new Label("No parameters"));
            }
        }
        return box;
    }

    // ─── Small controls ──────────────────────────────────────────────────

    /** A combo whose items include a selectable placeholder mapped to {@code null}. */
    @SuppressWarnings({"unchecked", "ReferenceEquality"}) // sentinel identity + Object->T cast
    private static <T> ComboBox<Object> nullableCombo(String placeholder, List<T> values, T current,
            Consumer<T> onChange) {
        var box = new ComboBox<Object>();
        box.setMaxWidth(Double.MAX_VALUE);
        box.getItems().add(PLACEHOLDER);
        box.getItems().addAll(values);
        box.setValue(current == null ? PLACEHOLDER : current);
        box.setCellFactory(ignore -> placeholderCell(placeholder));
        box.setButtonCell(placeholderCell(placeholder));
        box.valueProperty().addListener((o, old, now) -> {
            if (now == PLACEHOLDER) {
                onChange.accept(null);
            } else {
                onChange.accept((T) now);
            }
        });
        return box;
    }

    @SuppressWarnings("ReferenceEquality") // sentinel identity by design
    private static ListCell<Object> placeholderCell(String placeholder) {
        return new ListCell<>() {
            @Override
            protected void updateItem(Object item, boolean empty) {
                super.updateItem(item, empty);
                setText(item == PLACEHOLDER ? placeholder : item == null ? "" : item.toString());
            }
        };
    }

    private static HBox labeled(String text, Node control) {
        var label = new Label(text);
        label.setMinWidth(110);
        var box = new HBox(8, label, control);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    private static void setEnabled(Node a, Node b, boolean enabled) {
        a.setDisable(!enabled);
        b.setDisable(!enabled);
    }

    private static void setEnabled(Node a, Node b, Node c,
            Node d, boolean enabled) {
        a.setDisable(!enabled);
        b.setDisable(!enabled);
        c.setDisable(!enabled);
        d.setDisable(!enabled);
    }

    private ComboBox<Object> columnCombo(Consumer<String> apply) {
        var box = nullableCombo("(none)", List.of(), null, (String v) -> {
            apply.accept(v);
            scheduleRefresh();
        });
        return box;
    }

    private HBox optionalDouble(String label, BuilderModel.Geom g, String key,
            double min, double max, double def) {
        var check = new CheckBox(label);
        var spinner = new Spinner<Double>(min, max, def, (max - min) / 50.0);
        spinner.setPrefWidth(90);
        spinner.setDisable(true);
        var current = g.doubles.get(key);
        if (current != null) {
            check.setSelected(true);
            spinner.setDisable(false);
            spinner.getValueFactory().setValue(current);
        }
        check.selectedProperty().addListener((o, old, on) -> {
            spinner.setDisable(!on);
            if (on) {
                g.doubles.put(key, spinner.getValue());
            } else {
                g.doubles.remove(key);
            }
            scheduleRefresh();
        });
        spinner.valueProperty().addListener((o, old, now) -> {
            if (check.isSelected()) {
                g.doubles.put(key, now.doubleValue());
                scheduleRefresh();
            }
        });
        var row = new HBox(8, check, spinner);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private HBox requiredDouble(String label, BuilderModel.Geom g, String key, double def) {
        g.doubles.putIfAbsent(key, def);
        var spinner = new Spinner<Double>(-1_000_000.0, 1_000_000.0, g.doubles.get(key), 0.5);
        spinner.setPrefWidth(110);
        spinner.valueProperty().addListener((o, old, now) -> {
            g.doubles.put(key, now.doubleValue());
            scheduleRefresh();
        });
        return labeled(label, spinner);
    }

    private HBox optionalColor(String label, BuilderModel.Geom g, String key) {
        var check = new CheckBox(label);
        var picker = new ColorPicker(Color.web("#4c72b0"));
        picker.setDisable(true);
        var current = g.colors.get(key);
        if (current != null) {
            check.setSelected(true);
            picker.setDisable(false);
            picker.setValue(current);
        }
        check.selectedProperty().addListener((o, old, on) -> {
            picker.setDisable(!on);
            if (on) {
                g.colors.put(key, picker.getValue());
            } else {
                g.colors.remove(key);
            }
            scheduleRefresh();
        });
        picker.valueProperty().addListener((o, old, now) -> {
            if (check.isSelected()) {
                g.colors.put(key, now);
                scheduleRefresh();
            }
        });
        var row = new HBox(8, check, picker);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private <T extends Enum<T>> HBox enumRow(String label, BuilderModel.Geom g, String key, T[] values) {
        var combo = nullableCombo("(default)", List.of(values), g.enums.get(key),
                v -> {
                    if (v == null) {
                        g.enums.remove(key);
                    } else {
                        g.enums.put(key, v);
                    }
                    scheduleRefresh();
                });
        combo.setPrefWidth(160);
        return labeled(label, combo);
    }

    private ComboBox<Object> booleanCombo(Boolean current, Consumer<Boolean> apply) {
        return nullableCombo("(default)", List.of(Boolean.TRUE, Boolean.FALSE), current, v -> {
            apply.accept(v);
            scheduleRefresh();
        });
    }

    private HBox flagRow(String label, BuilderModel.Geom g, String key) {
        var combo = nullableCombo("(default)", List.of(Boolean.TRUE, Boolean.FALSE), g.flags.get(key),
                v -> {
                    if (v == null) {
                        g.flags.remove(key);
                    } else {
                        g.flags.put(key, v);
                    }
                    scheduleRefresh();
                });
        combo.setPrefWidth(160);
        return labeled(label, combo);
    }

    private TextField textField(String current, String prompt, Consumer<String> apply) {
        var field = new TextField(current == null ? "" : current);
        field.setPromptText(prompt);
        field.textProperty().addListener((o, old, now) -> {
            apply.accept(now.isBlank() ? null : now);
            scheduleRefresh();
        });
        return field;
    }

    private TextField intField(Integer current, Consumer<Integer> apply) {
        var field = new TextField(current == null ? "" : String.valueOf(current));
        field.setPrefWidth(70);
        field.textProperty().addListener((o, old, now) -> {
            if (now.isBlank()) {
                apply.accept(null);
            } else {
                try {
                    apply.accept(Integer.parseInt(now.trim()));
                } catch (NumberFormatException ignored) {
                    // keep the previous value while typing an incomplete number
                }
            }
            scheduleRefresh();
        });
        return field;
    }

    private TextField doubleField(Double current, Consumer<Double> apply) {
        var field = new TextField(current == null ? "" : format(current));
        field.setPrefWidth(70);
        field.textProperty().addListener((o, old, now) -> {
            if (now.isBlank()) {
                apply.accept(null);
            } else {
                try {
                    apply.accept(Double.parseDouble(now));
                } catch (NumberFormatException ignored) {
                    // keep the previous value while typing an incomplete number
                }
            }
            scheduleRefresh();
        });
        return field;
    }

    @SuppressWarnings("StringSplitter") // comma-separated hex tokens; Pattern.split() still trips the checker
    private void applyManualColors(String text) {
        model.manualColors.clear();
        if (text != null) {
            for (var token : text.split(",")) {
                var trimmed = token.trim();
                if (!trimmed.isEmpty()) {
                    try {
                        model.manualColors.add(Color.web(trimmed));
                    } catch (IllegalArgumentException ignored) {
                        // skip malformed tokens
                    }
                }
            }
        }
    }

    private static String format(double value) {
        return String.format(Locale.ROOT, "%g", value);
    }

    // ─── Columns + preview refresh ───────────────────────────────────────

    private void reloadColumns() {
        refreshing = true;
        try {
            var columns = new ArrayList<String>(model.dataset().columns());
            columns.sort(String::compareTo);
            reloadColumn(xCombo, model.x, columns);
            reloadColumn(yCombo, model.y, columns);
            reloadColumn(zCombo, model.z, columns);
            reloadColumn(colorCombo, model.color, columns);
            reloadColumn(fillCombo, model.fill, columns);
            reloadColumn(shapeCombo, model.shape, columns);
            reloadColumn(sizeCombo, model.size, columns);
            reloadColumn(alphaCombo, model.alpha, columns);
            reloadColumn(groupCombo, model.group, columns);
            reloadColumn(xminCombo, model.xmin, columns);
            reloadColumn(xmaxCombo, model.xmax, columns);
            reloadColumn(yminCombo, model.ymin, columns);
            reloadColumn(ymaxCombo, model.ymax, columns);
            reloadColumn(xendCombo, model.xend, columns);
            reloadColumn(yendCombo, model.yend, columns);
            reloadColumn(zendCombo, model.zend, columns);
            reloadColumn(facetWrapCombo, model.facetWrapColumn, columns);
            reloadColumn(facetRowCombo, model.facetGridRow, columns);
            reloadColumn(facetColCombo, model.facetGridCol, columns);
            reloadColumn(polarThetaCombo, model.polarTheta, columns);
        } finally {
            refreshing = false;
        }
    }

    private void reloadColumn(ComboBox<Object> box, String current, List<String> columns) {
        box.getItems().setAll(columns);
        box.getItems().add(0, PLACEHOLDER);
        box.setValue(columns.contains(current) ? current : PLACEHOLDER);
    }

    private void scheduleRefresh() {
        if (refreshScheduled) {
            return;
        }
        refreshScheduled = true;
        Platform.runLater(() -> {
            refreshScheduled = false;
            refresh();
        });
    }

    private void refresh() {
        if (refreshing) {
            return;
        }
        preview.getChildren().clear();
        try {
            var plot = BuilderSpec.build(model);
            plot.setPrefWidth(preview.getWidth() > 1 ? preview.getWidth() : 880);
            plot.setPrefHeight(preview.getHeight() > 1 ? preview.getHeight() : 540);
            preview.getChildren().add(plot);
        } catch (RuntimeException ex) {
            var error = new Label("Could not build plot: " + ex.getMessage());
            error.setWrapText(true);
            preview.getChildren().add(error);
        }
        sourceDrawer.setCode(PlotCodeGenerator.emit(model));
    }
}
