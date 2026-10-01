# 🏛️ System Architecture Overview (`ARCHITECTURE.md`)

**Last Updated:** 2026-09-28
**Owners:** [Contributing Engineers]

## 🎯 Project Mission & Core Goals

This system is designed to be a comprehensive, extensible statistical plotting library following the principles of the Grammar of Graphics. Its core goal is to allow users to construct complex visualizations by composing independent layers built upon standardized aesthetic and coordinate mappings.

*   **Primary Goal:** To provide a highly flexible framework for generating static, publication-quality 2D/3D statistical plots using object composition.
*   **Non-Functional Requirements (NFRs):**
    *   **Extensibility:** Adding new geometry types (`Geoms`) or variable mappings (`Aes`) should require minimal changes to core plotting logic.
    *   **Modularity:** Components must be separated by function: Data preparation, mapping, transformation, and rendering.
    *   **Consistency:** All output components (axes, legends, themes) must adhere to a unified visual specification.

## 🧩 Architectural Pattern: Grammar of Graphics Composition

The architecture follows the pattern where a plot object is built through the composition of multiple independent 'Components' which modify a central `Plot` context object.

The general flow for constructing a plot is:
$$\text{Plot} = \text{Base Layer} + (\sum_{i=1}^{N} \text{Stat}_i) + (\sum_{j=1}^{M} \text{Layer}_j)$$

### 🧱 Key Architectural Modules (gog package):

#### 1. **Data & Utilities (`data/`, `dflib/`, `hardwood/`, `spi/`)**
*   **`spi/DataExtractor`**: The pluggable abstraction that normalizes any tabular source into the internal access model (column by name, row counts, column types). Resolved via `ServiceLoader`, so a plot is built against `DataExtractor` and never against a concrete DataFrame library. Its `ColumnType` taxonomy is the single classification point — `NUMBER`, `DATE`, `TIMESTAMP`, and `TEXT` — with `isContinuous`/`isTemporal` shaping axis and `CellKind` dispatch. A `TIMESTAMP` is any type carrying a time of day (`Instant`, `OffsetDateTime`, `ZonedDateTime`, `LocalDateTime`) and is positioned in epoch milliseconds; `LocalDate` stays a `DATE` in epoch days.
*   **`data/Temporals`, `data/Values`**: the shared temporal utilities. `Temporals` converts, labels, and generates axis breaks from one ordered step ladder (second → minute → hour → day → month → year), so a value's numeric position, its tick positions, and its label text can never disagree. `Values.toDouble`/`label` are what the geoms, tooltips, and legends call. Zone-less `LocalDateTime` values are read as UTC for determinism.
*   **`dflib/*`**: The DFLib integration — a `DataExtractor` implementation for DFLib `DataFrame`s plus the dataset loaders (`DiamondsDatasets`, `MpgDatasets`, …) and the example/export applications built on them.
*   **`hardwood/*`**: The Hardwood integration — a `DataExtractor` implementation over Apache Parquet files. `HardwoodDataFrame` is the frame handle: `of(Path)` opens a Parquet file on disk, `ofResource(resource, Cache)` opens a classpath resource (materialized in a heap buffer or extracted to a temporary file, per `Cache`), and `ofColumns` builds the write path's in-memory output. The dataset loaders read the Parquet files the build derives from the shared CSVs. This shows the engine rendering identically from a second, columnar frame type.

#### 2. **Data Transformation & Abstraction (`stat/`, `coord/`)**
*   **`stat/`**: Implements statistical transformations (e.g., binning for histograms, calculating means, LOESS/LM smoothing via Apache Commons Math). This module is crucial as it transforms the raw input data frame into *plot-ready* data before rendering geometry. The 3-D stats mirror the geom families: `Stats.col3d()`/`bar3d()`/`voxel3d()`/`hull3d()`/`smooth3d()`/`density3d()`/`identity3d()`, with surface/font geometry handled in `geometry/` (below).
*   **`coord/*`**: Coordinate systems — Cartesian 2-D/3-D with flip support, and polar coordinates. `CoordPolar` serves both full-turn plots (pies, coxcombs, donuts) and partial fans (`coord_radial`-style `start`/`end`), sizing each fan's bounding box into the panel so partial arcs use the available space. Coordinates own their panel bounds, tick generation, label measurement, and clipping.
*   **`coord3d()`** (`Coords.coord3d()` → `Coord3D`): the perspective-axonometric cube projection that powers every 3-D plot. It fits a cube around the `x`/`y`/`z` data range, projects each point through `M = Rx(pitch)·Ry(roll)·Rz(yaw)` style rotations plus a z-flip and perspective divide at a configurable camera distance, and renders the six cube faces as panels. See §11 for the full 3-D architecture.

#### 3. **The Core Plotting Elements (`Geoms.java`, `layer/`)**
*   **`Geoms.java`** and the **`layer/*Configurator.java`** classes are the fluent entry points. A `LayerConfigurator` (a *Spec) binds a concrete `Layer` implementation to an aesthetic mapping — e.g., `PointSpec` plus a colour mapping means "draw circles at data point $(x, y)$, coloured by variable $Z$".
*   **3-D geoms** extend the same `Layer` lifecycle with `z`: `Geoms.point3d()` (scatter), `surface3d()`/`function3d()`/`density3d()`/`ridgeline3d()`/`contour3d()`/`smooth3d()` (surfaces), `col3d()`/`bar3d()`/`voxel3d()`/`hull3d()` (volumes), `segment3d()`/`path3d()`/`text3d()` (primitives), and the data-free `Annotations3d.point()/.text()/.segment()`. Position adjustments such as `Positions.positionOnFace()` place 2-D layers onto a specific cube face.
*   **The two-phase `Layer` lifecycle** — every geometry runs *prepare once, draw many*, so per-panel rendering is stateless and correct by construction:
    1.  **`prepare(PlotContext)`** runs once per render pass over the unpartitioned global data and returns an opaque **`LayerData`** blob holding the layer's model state (e.g., `StatSmooth` fits, binned counts, smoothed densities). Expensive statistical work is shared across all facet panels instead of being recomputed per panel.
    2.  **`render(DrawSurface, PanelContext, LayerData)`** draws one panel against the plot's backend-neutral `DrawSurface`.
    3.  **`locate(PanelContext, LayerData, x, y)`** does hover hit-testing against the *same* prepared model data, so tooltips and drawn geometry can never disagree.
    4.  **`expandDomain(Bounds, PlotContext, …)`** lets a layer widen the axis domain (density curves, smoothed trendlines) before the scales are fixed.
*   **`PlotContext` / `PanelContext` / `LayerData`**: `PlotContext` is the immutable per-render bundle — the global master `DataFrame`, its `DataExtractor`, the `Aes` mapping, the `ScaleSpec`, the `LabsSpec` label dictionary, the `Coord`, and the eagerly resolved `ResolvedScales`. `PanelContext` narrows that to a single facet frame together with its pixel `Scale`s and `FacetValues`. `LayerData` is the output of `prepare`, giving geoms a clean seam between "fit once" and "draw many".

#### 4. **Aesthetics & Mappings (`Aes.java`)**
*   **`Aes.java`**: Defines aesthetic mappings—the linkage between a raw data field and a visual property (e.g., `mapping(color = "group", size = "count")`). This is the central concept linking data to appearance. Supported aesthetics: `x`, `y`, `z`, `color`, `fill`, `shape`, `size`, `group`, `label`.

#### 5. **Composition & Metadata (`facet/`, `labs/`)**
*   **`facet/*`**: Controls *data segmentation*, enabling the visualization to be split into multiple, smaller plots (small multiples) based on discrete categorical variables (e.g., separate charts for each year). `Facets.wrap` facets on a single column; `Facets.grid` builds a `rows ~ cols` grid via `FacetGrid`, with `GridOptions` for per-panel scale freedom (`FIXED`/`FREE_X`/`FREE_Y`/`FREE` cells), margin rows/columns, strip ordering (`asTable`), and strip placement (`switch`). The data extractor partitions each cell through `DataExtractor.partitionGrid`. Wrap and grid panels share the same per-panel `PanelContext` lifecycle for rendering, hover, and domain expansion.
*   **`labs/*`**: Manages **Plot Metadata**. This includes titles, axis labels, legend titles (global and per-aesthetic, e.g. `labs(colour = "Drive type")`), and a label dictionary mapping raw category values to display names. The spec is announced to layers so interactive tooltips can present mapped display names.

#### 6. **Visual Styling & Theming (`theme/`)**
*   **`theme/*`**: Controls non-data appearance settings. This module manages global visual overrides (e.g., setting the background color or removing major grid lines) without affecting the core data drawing logic. It defines the overall aesthetic 'skin' of the plot — including the default guide placement (`GuidePosition`).

#### 7. **Guides & Legends (`guide/`, `Guides`, `Aesthetic`)**
*   **`guide/Guide`** is the abstract base of `GuideLegend` and `GuideColorbar`; `Guide.NONE` suppresses rendering. Guides are immutable; every setter returns a copy.
*   **`Guides`** (in the root package) is the per-aesthetic registry: an immutable `Map<Aesthetic, Guide>` built through typed `guide(Aesthetic.COLOR, guideLegend())` mappings. Unregistered aesthetics fall back to their scale's default guide type; suppressed ones render nothing.
*   **`Aesthetic`** is the enum of guide-addressable aesthetics (`COLOR`, `FILL`, `SIZE`, `SHAPE`) — replacing stringly-typed keys everywhere.
*   **Placement**: each guide carries an optional `position()` override over the theme's `GuidePosition` (`TOP`/`RIGHT`/`BOTTOM`/`LEFT`/`INSIDE`). The plot partitions guides by resolved side, grows the matching panel margin per occupied strip, and floats `INSIDE` strips above the data layers.
*   **Collection** (`Plot.collectGuideInlays`): one guide per mapped aesthetic — colour/fill legends or colourbars, shape legends with real point glyphs, size legends with area-proportional reference dots, plus stat-driven colourbars (filled density2d) and reference-line colour scales. Aesthetic mappings that share a column merge into one set of keys. Guides are built from the same `ResolvedScales` the layers draw with, so a legend key's colour, shape, and size always match the plotted geometry.
*   **Titles**: resolution order is explicit guide title → per-aesthetic labs title → global legend title → column name; direction-derived layout puts horizontal legends' titles beside their keys.

#### 8. **Scales (`scale/`)**
*   Continuous/discrete colour scales with automatic palettes, manual palettes (`Scales.scaleColorManual`), axis transforms (log10, sqrt), tick generation, and expansion control. Temporal axes (`Scale.timestampScale`) break on the `Temporals` ladder and format labels at the matching granularity, in normal, faceted, and flipped coordinates alike.
*   **`SizeScale`**: area-proportional point sizing (area-proportional semantics) — radius ∝ √value across the column's global range; shared by geometry rendering and size legends so sizes stay comparable.
*   **`ResolvedScales`**: the per-render registry of discrete colour (`DiscreteColorScale`), shape (`DiscreteShapeScale`), and numeric-range scales, built over the *global* data so that every layer, legend, and tooltip resolves a column's palette (and any `scale_color_manual` override) identically — geometry, guides, and hover text can never diverge on a colour.

#### 9. **Rendering & Export (`render/`)**
*   **`DrawSurface`** is the backend-neutral drawing interface; geometry layers never touch JavaFX or SVG directly.
*   **`FxDrawSurface`** paints onto a JavaFX `Canvas` for the live application; **`SvgDrawSurface`** writes vector SVG, with same-styled point batching and an opt-in raster path that embeds dense layers as PNG while keeping everything else vector.
*   **`SvgExporter` / `PlotExporter`** drive the exact same `Plot.renderTo(DrawSurface, …)` pipeline off-screen, which is how the documentation figures are regenerated deterministically.

#### 10. **Multi-Plot Composition (`GgFigure`, `PlotMatrix`, `ComposedPlot`)**
*   **`GgFigure`** is the shared render contract — `renderTo(DrawSurface, width, height)` — sealed to a single `Plot`, a `PlotMatrix`, and a `ComposedPlot`, so exporters treat every figure uniformly.
*   **`PlotDescriptor`** is the mutable spec a `Plot` delegates to and the unit a composite clones per cell (`copy()`), stamping per-position `PanelInsets` and resolved `PlotOptions` for aligned panel geometry.
*   **`PlotMatrix`** tiles an *n*×*n* grid of plot specs (generalized-pairs scatter-plot matrix): each cell is dispatched by `DataExtractor.ColumnType` (`CellKind` TT/COMBO/DD), the outer edges share one x/y label frame. Per-matrix knobs (`widths`/`heights` proportions, `axisLabels`, `columnLabels`, `xlab`/`ylab`, `subtitle`/`caption`) are theme- and descriptor-driven rather than hard-coded, and an optional shared legend band hugs the right edge.
*   **`ComposedPlot`** arranges arbitrary figures into a grid (`rows`/`cols`, `nrow`/`ncol`/`byrow`, `widths`/`heights`, `beside()`/`above()`). `Plot.insetsFor(w, h)` measures each leaf's natural panel insets, which the composition re-stamps with the shared left/bottom inset so panels align at the bottom-left corner while every leaf keeps its own axes and guides; optional lettered panel tags and a collected shared legend round it out.
*   Composite layout constants (title band, outer label bands, y-title pivot, panel-tag offset, right margin, compact cell label zone, legend band width) live on `Theme`/`DerivedTheme` so composites reuse the single-plot theme defaults and stay configurable.

#### 11. **The 3-D Cube Pipeline (`coord3d()` & friends)**
The 3-D feature set extends the 2-D grammar rather than forking it: a `Coord3D` coordinate system, the shared `Layer` lifecycle, the stat/scale/guide machinery, and a scene-light model.

*   **Entry points** — `Ggplot.plot3d(DF, Aes)` and `Ggplot.ggplot3d(DF, Aes)` return a `PlotDescriptor3D<DF>` (the cube-first DSL: default view, rotation, hidden panels, plot-level light); `Coords.coord3d()` is the bare coordinate system for classic `gg.plot(...).coord(...)` composition. `Plot.light(...)` / `coord3d().light(...)` set a scene light; supplying both at coord-apply time throws.
*   **`Coord3D` projection** — fits a unit cube around the data extents (`x`, `y`, `z`), applies the rotation matrix `M = Rz(yaw)·Ry(pitch)·Rx(roll)` (row-vector convention, `Mᵀ·v` application), flips z to screen space, then projects with a perspective divide at camera distance `dist` (default 2). Orthographic (`persp=false`) skips the divide; `zoom` magnifies the fitted cube. Continuous x/y get the same scale headroom as z so outer ticks/gridlines stay inside the cube corners, while discrete banded axes stay flush at `[-0.5, N-0.5]`.
*   **Extents, labels, clip** — `coord3d().expand(false)` disables the default headroom, `clip(true)` confines overflow to the panel, `ratio(x, y, z)` rescales per-axis, and `scales(ScaleMode...)` freezes/relaxes individual axes. Per-axis label edges are auto-selected peripheral faces or explicit `xlabels(CubeFace...)`/`ylabels(...)`/`zlabels(...)` pairs; titles keep their axis-edge angle (the horizontal-label centered case uses title-position centering). Face selection is done by projecting cube corners and scoring per-face perpendicularity/area.
*   **Cube faces as panels** — the six faces are `CubePanel.BACKGROUND`/`FOREGROUND`/`ALL`/`NONE`/`NEAR`/`FAR` and face-specific `CubeFace.XMIN..ZMAX`s; the `positionOnFace()` adjustment (`Positions`) flattens 2-D layers (density2d, tile, point clouds) onto a chosen face. Face mixing lets 2-D `Geoms.density2d()`/`Geoms.tile()` marks live on the cube alongside true 3-D layers.
*   **3-D geometry (`geometry/`)** — `GridGeometry` (RECTANGLE/RIGHT1/RIGHT2/EQUILATERAL lattice tessellation), `MeshUtil` (row-major triangulation of regular grids), `Delaunay2D` (Delaunay triangulation of scattered points into surface tiles), `PolygonMath` (point-in-polygon, area, winding/orientation), `GridMetrics` (per-row/col resolution, used for the alpha radius defaults), `Hull3d`/`HullMethod` (CONVEX/ALPHA hulls via tetrahedralization), `Pairwise3dSorter`/`DepthSorter` (painter's-algorithm depth ordering), and `SurfaceMethod` (AUTO/GRID tessellation selection).
*   **3-D stats (`stat/`)** — `StatCol3d`/`StatBar3d` (columns and counted/binned bars), `StatVoxel3d` (sparse voxels), `StatHull3d` (convex/alpha), `StatSmooth3d` (LOESS/LM surfaces with confidence panels), `StatDensity3d` (2-D Gaussian KDE over a grid), `StatIdentity3d` (pass-through). All run in the layer `prepare` phase once per render pass.
*   **Lighting (`Light3d`)** — a record (`method`, `mode`, `fill`, `color`, `contrast`, `direction`, `position`, `distanceFalloff`, `anchor`, `backfaceScale`, `backfaceOffset`). `Method.DIFFUSE`/`DIRECT`/`RGB`/`NONE`, blended in `Mode.HSV` or `Mode.HSL` with a `contrast` (0.5 neutral); `Anchor.SCENE`/`CAMERA` for the direction; positional lights with distance falloff; and `backfaceScale`/`backfaceOffset` to shade inside-out polygons. Default direction `(−0.5, 0, 1)` (above-front), default contrast 1.0, HSV. `Polygon3dSpec.SOLID_LIGHT` (contrast 0.4) is the default per-face lighting for `col3d()`/`bar3d()`/`voxel3d()`. `GuideShading` reuses the same shade math for `Guides.guideColorbar3d()`/`Guides.guideLegend3d()`.
*   **Interaction (`interaction/`)** — `Orbit3d`/`Orbit3dOptions` wrap a drag-to-rotate handle over a `Coord3D` plot (yaw/pitch via mouse, readout overlay, bounded zoom) and `CubeOrbitControl` renders a mini-globe widget that rotates the same cube; both repaint via the figure's `markDirty()` contract.
*   **Composition** — `Coord3D` cells compose inside `PlotMatrix`/`ComposedPlot` like any other coordinate system; `ComposedPlot` re-stamps shared panel insets so square 3-D cells align edge-to-edge.

#### Design Inspirations
The multi-plot features are original implementations of two Grammar-of-Graphics composition ideas: the generalized pairs plot (histogram diagonal, correlation annotations, per-column-type dispatch, shared legend) informs `PlotMatrix`'s scatter-plot matrix gestures, and a composition API (aligned grid, `nrow`/`ncol`/`byrow` layout, panel tags, collected legend) informs `ComposedPlot`. gog4j implements both ideas on its own `GgFigure`/descriptor infrastructure rather than wrapping another library.

## 🚀 Data Flow Example: Creating a Binned Histogram

1.  **Data $\rightarrow$ Stat:** Raw data is passed to `stat/` module, which applies binning and aggregation formulas to create counts (Stat transformation).
2.  **Transformed Data $\rightarrow$ Layer:** The binned count data is passed to the specific geometry layer (`BarSpec`).
3.  **Aesthetics & Mapping:** The `Aes` mapping dictates that the height of the bars should correspond to the calculated 'count' and the center position corresponds to the binned bin (the X-coordinate).
4.  **Composition $\rightarrow$ Faceting/Labeling:** If faceting is applied, the process repeats for each factor group. The `labs` module ensures the subplot titles reflect the grouping variable.
5.  **Theming & Utilities:** The `theme` system applies global rules (e.g., grid line visibility), while `dflib` provides helper methods used by multiple components to finalize coordinates or process strings before drawing occurs in `coord/`.

***