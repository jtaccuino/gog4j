# gog4j — Grammar of Graphics for Java

**gog4j** is a pure Java and JavaFX plotting library built around the *Grammar of Graphics* — the declarative, layer-based model in which a plot is composed from data, aesthetic mappings, geometries, statistics, scales, coordinates, facets, and themes. It brings that framework to the Java ecosystem, designed for scientific computing, data visualization, and data science applications integrated with Java DataFrame libraries such as [DFLib](https://github.com/bootique/dflib).

The project targets feature parity with established Grammar-of-Graphics implementations in a pure Java, type-safe form: the whole pipeline is statically typed and renders off-screen on the JVM, with no runtime interpreter and no dependency on a non-Java plotting runtime.

![Build](https://img.shields.io/github/actions/workflow/status/jtaccuino/gog4j/gradleBuild.yml?branch=main) ![License](https://img.shields.io/github/license/jtaccuino/gog4j) ![Java](https://img.shields.io/badge/java-26-blue) ![Lines of Code](https://img.shields.io/badge/lines%20of%20code-37k-blue) ![Stars](https://img.shields.io/github/stars/jtaccuino/gog4j) ![Last Commit](https://img.shields.io/github/last-commit/jtaccuino/gog4j) ![Commits Since Release](https://img.shields.io/github/commits-since/jtaccuino/gog4j/latest)

---

## Key Features

- 🎨 **Grammar of Graphics Paradigm**: Declarative, chainable fluent API (`ggplot`, `aes`, `geoms`, `facets`, `scales`, `labs`, `guides`, `theme`).
- 📊 **Rich Geometry Support (`Geoms`)**:
  - `point()` / `jitter()` — Scatter plots with aesthetic `color`, `shape` and `size` mappings; `size` scales each point's area by a data column (area-proportional semantics).
  - `line()` — Line charts supporting time-series data with configurable averaging/smoothing filters.
  - `bar()` / `col()` — Vertical and horizontal bar charts with support for `dodge` (clustered) and `stack` layouts.
  - `area()` — Filled area charts for continuous time-series and volume visualizations.
  - `boxplot()` / `violin()` — Distribution summaries with notches, variable width, quantile display and jitter companions.
  - `density()` / `density2d()` — Kernel density estimates, stacked/filled count densities, filled 2-D bands with contour lines.
  - `tile()` — Heatmap tiles driven by a continuous fill scale with configurable colour ramps.
  - `smooth()` — Statistical smoothing & trendlines powered by Apache Commons Math:
    - **LOESS** (Locally Estimated Scatterplot Smoothing) with custom span control.
    - **Linear Regression (LM)** models with optional confidence interval ribbons (`se(boolean)`).
    - Local aesthetic overrides (e.g., global points with per-group smoothed trendlines).
  - `hline()` / `vline()` / `abline()` — Reference lines at a constant value or a slope/intercept, solid or dashed, with an optional caption.
  - `text()` — Text annotations driven by a `label` aesthetic, skipping blank rows and staggering labels that would collide.
- 🔄 **Coordinate Transformations (`Coords`)**:
  - Cartesian coordinates with `coordFlip()` for horizontal charts and inverted axes.
  - `coordPolar()` — Pies, coxcombs, donuts, stacked roses and wind roses.
  - `coordRadial()` — Partial radial fans (`start`/`end`) that fill the panel with hollow centres (`innerRadius`).
  - `coord3d()` — A perspective 3-D cube coordinate system: `plot3d()`/`ggplot3d()` layers for surfaces, volumes, lighting, orbit controls, and an interactive 3-D gallery.
- 🔲 **Multi-Panel Faceting (`Facets.wrap`, `Facets.grid`)**:
  - Grid-based subplots automatically faceted by categorical DataFrame columns with dynamic row/column layout calculation.
  - `Facets.grid()` — `rows ~ cols` grids with `WRAP`/`GRID` modes, per-panel scale control (`scales = free`), margin panel inclusion, strip reordering via `asTable`, and transposed strip placement (`switch`).
- 🗂️ **Multi-Plot Composition (`matrixPlot`, `composedPlot`)**:
  - `matrixPlot()` — an *n*×*n* scatter-plot matrix (histogram diagonals, Pearson-correlation annotations in the triangles) with automatic per-column-type dispatch: numeric pairs get densities/scatters/correlations, mixed pairs box plots and faceted histograms, categorical pairs count bars; a single legend shared by every cell.
  - `composedPlot()` — a composition grid arranging several plots, matrices, or compositions into one aligned figure with bottom-left panel alignment, per-panel tag letters, a shared collected legend, and a composed title/subtitle/caption.
  - Both render into a single SVG or JavaFX `Canvas` via the shared `GgFigure` contract.
- 🎨 **Theming System (`Theme`)**:
  - Customizable plot appearance with built-in themes including `GrayTheme` (classic grey panels), `BwTheme` (`theme_bw()`, white panel with a thin border), `DarkTheme`, and `DerivedTheme`.
  - Per-axis grid line control (`showXGrid`, `showYGrid`) for plots where one axis carries no metric meaning.
- 📐 **Scales & Annotations (`Scales`, `Labs`)**:
  - Explicit tick mark (`breaks`) configuration for continuous axes, optionally with **custom tick labels** placed at arbitrary positions (`Scales.scaleXContinuous(breaks, labels)`).
  - **Axis transformations** — `Scales.scaleYLog10()`, `Scales.scaleYSqrt()` and their x-axis counterparts. Only the spacing changes, so long-tailed data fits the panel without any value being clamped; tick labels stay in the original units.
  - `Scales.scaleColorManual()` for an explicit category-to-colour palette, overriding the automatic one.
  - Custom color maps, titles, subtitles, axis labels, and captions.
- 🧭 **Guides (`Guides`, `Aesthetic`)**:
  - Per-aesthetic guide registry — `guideLegend()`, `guideColorbar()`, suppression, and a typed `guide(Aesthetic, Guide)` mapping (`COLOR`, `FILL`, `SIZE`, `SHAPE`).
  - Legend key grids (`nrow`/`ncol`/`byrow`), reversed keys, horizontal/vertical flow, and the optional legend box.
  - **Per-guide placement** — each guide can sit on its own side (`TOP`/`RIGHT`/`BOTTOM`/`LEFT`) or float inside the panel (`INSIDE`), independent of the theme's default position.
  - Automatic legends for every mapped aesthetic: discrete colour/fill legends, continuous colourbars, shape legends with real point glyphs, and size legends whose reference dots grow with the data (area-proportional).
  - Per-aesthetic titles via `labs(colour = "Drive type")`, title placement that follows the legend's direction (above vertical legends, beside horizontal ones), and tooltips that report every mapped aesthetic using its display name.
- 🖼️ **Reproducible Export, Raster and Vector**:
  - **SVG output** via `SvgExporter` for publication-quality, editable figures.
  - PNG snapshots via `org.jtaccuino.gog.dflib.apps.PlotExporter`.
  - Both render off-screen — no window required — so documentation figures can be regenerated from source.
- 🧱 **Pluggable Rendering Backends (`org.jtaccuino.gog.render`)**:
  - Geometry layers draw onto a backend-neutral `DrawSurface`, so screen, PNG, and SVG all come from one rendering pipeline instead of parallel implementations.
- ⚡ **Hardware-Accelerated JavaFX Rendering**:
  - Canvas-based rendering (`Layer` onto a `DrawSurface`) with dynamic re-layout on window resize and automated legends.

---

## Technology Stack & Requirements

- **Java**: JDK 26 (Java 26)
- **Build Tool**: Gradle (`build.gradle`, wrapper `9.7.0`)
- **UI Framework**: OpenJFX / JavaFX 26
- **DataFrame Integration**: DFLib `2.0.0-M7`; Apache Parquet via Hardwood `1.1.0.Beta1`
- **Numerical Computations**: Apache Commons Math 4 (`4.0-beta1`)

---

## Repository Layout

- `gog4j` — the core Grammar-of-Graphics engine and rendering pipeline (JavaFX + Commons Math).
- `gog4j-data` — the bundled demo datasets.
- `gog4j-dflib` — the DFLib `DataExtractor` integration.
- `gog4j-dflib-data` — DFLib-backed demo datasets.
- `gog4j-hardwood` — the Hardwood `DataExtractor` integration, rendering Apache Parquet files.
- `gog4j-hardwood-data` — Parquet demo datasets (the build converts the shared CSVs to ZSTD Parquet).
- `gog4j-controls` — reusable JavaFX controls.
- `gog4j-builder` — the interactive plot builder application.
- `gog4j-sampler` — the interactive example gallery application.
- `gog4j-sampler-annotations` — the `@SamplePlot` annotation, its tag vocabulary, and the compile-time registry processor.
- `gog4j-examples-dflib` — the DFLib example gallery sources and the documentation exporter.
- `gog4j-examples-hardwood` — the Hardwood (Parquet) example gallery sources.
- `gog4j-tests` — cross-module integration tests and benchmarks.

Dependencies flow inward: the core depends only on JavaFX and Apache Commons Math, and the integrations, applications, and tests depend on the core — never the other way round.

---

## Building and Running from Source

### Prerequisites
Ensure JDK 26 is installed. Use the Gradle wrapper (`./gradlew`) — it downloads Gradle 9.7.0 automatically.

### 1. Compile the Project
```bash
./gradlew build
```

### 2. Run the Sampler Gallery
Launch the interactive gallery browser (`SamplerApp`):

```bash
./gradlew :gog4j-sampler:runSampler
```

> **Note**: If your system has multiple JDK versions, ensure `JAVA_HOME` points to Java 26:
> ```bash
> JAVA_HOME=/path/to/jdk-26 ./gradlew :gog4j-sampler:runSampler
> ```

---

## Quickstart Example

Here is how to create a scatter plot with LOESS trendlines using `gog4j` and `DFLib`:

```java
import static org.jtaccuino.gog.Ggplot.ggplot;
import static org.jtaccuino.gog.Aes.aes;
import static org.jtaccuino.gog.Geoms.*;
import static org.jtaccuino.gog.labs.Labs.labs;
import static org.jtaccuino.gog.stat.SmoothMethod.LOESS;
import org.jtaccuino.gog.Plot;
import org.jtaccuino.gog.theme.DarkTheme;
import org.dflib.DataFrame;

// 1. Initialize DataFrame — the DataExtractor is resolved via ServiceLoader
DataFrame df = ...; 

// 2. Build declarative plot
Plot<DataFrame> plot = ggplot(df, aes().x("displ").y("hwy").color("drv"))
        .geoms(
            point().size(6.0),         // Scatter points mapped by drive type
            smooth(LOESS, true)        // LOESS trendline with confidence interval ribbon
        )
        .labs(labs("Fuel Efficiency Showcase", "Displacement (L)", "Highway MPG"))
        .theme(new DarkTheme());

// 3. Mount to JavaFX scene graph
// plot can be embedded into any JavaFX Scene or layout container
```

---

## Featured Example: Genome-Wide Association Study

A Manhattan plot of the largest genome-wide association study of human height to
date — 5.4 million participants, 1.4 million variants.
Data from Yengo *et al.*, [*A saturated map of common genetic variants associated
with human height*](https://doi.org/10.1038/s41586-022-05275-y), Nature **610**,
704–712 (2022), CC BY 4.0.

![Manhattan plot of the human height GWAS](docs/images/manhattan-height.png)

The whole genome is laid out along one continuous axis, chromosomes told apart by
alternating tone, with the genome-wide significance threshold marked:

```java
ggplot(df, aes().x("BPCUM").y("NEGLOG10P").color("CHRBAND"))
    .geoms(
        point().size(1.6).opacity(0.8),
        hline(GwasDatasets.GENOME_WIDE_LINE).color(THRESHOLD).dashed()
                .annotate("genome-wide significance, P = 5 × 10⁻⁸")
    )
    .scales(Scales.scaleColorManual(Map.of("odd", GREY, "even", SKYBLUE)))
    .scales(Scales.scaleXContinuous(GwasDatasets.chromosomeBreaks(), GwasDatasets.chromosomeLabels()))
    .scales(Scales.scaleYSqrt(Y_BREAKS))
    .guides(Guides.none())
    .theme(Theme.theme_bw())
    .theme(t -> t.showXGrid(false));
```

`-log10(P)` runs from 0 to 323 here — the strongest associations are stronger
than double precision can represent. The **square-root axis** fits that range
while keeping the null band at the bottom legible, so every SNP is drawn at its
published value and nothing is clamped to make it fit.

Adding a `label` aesthetic and a `text()` layer names the strongest loci, and the
published independent lead SNPs are highlighted by mapping colour to a computed
role column:

![Manhattan plot with lead SNPs highlighted and loci named](docs/images/manhattan-height-annotated.png)

Zooming to a single chromosome resolves the individual peaks — the gap near
90 Mb is the centromere, where no variants are called:

![Close-up of chromosome 3](docs/images/manhattan-chromosome-3.png)

The companion quantile-quantile plot shows how far the observed p-values depart
from the null expectation, the signature of a highly polygenic trait:

<img src="docs/images/gwas-qq-plot.png" alt="Quantile-quantile plot" width="460">

### Regenerating the figures

```bash
./gradlew :gog4j-examples-dflib:exportDocs
```

This writes each figure as both PNG and SVG.

### Regenerating the example data

The published source datasets (~50 MB) are not committed, but the script that
derives the demo files from them is, along with the exact selection rules:

```bash
cd gog4j-data/src/main/resources/examples/gwas
./derive-demo-dataset.sh            # downloads the published data, then derives
./derive-demo-dataset.sh --verify   # checks the committed files still match
```

Provenance, the derivation step by step, and the column definitions are
documented in
[`gog4j-data/src/main/resources/examples/gwas/README.md`](gog4j-data/src/main/resources/examples/gwas/README.md).
No p-value is altered anywhere in that pipeline — the derivation only selects
rows.

---

## Exporting Figures

```java
Platform.startup(() -> {
    new SvgExporter().size(1400, 620).write(plot, Path.of("figure.svg"));
});
```

SVG export runs the same `Plot.renderTo(DrawSurface, …)` pipeline that paints the
screen, so the vector figure matches the on-screen one by construction rather
than by maintenance. Layout and text measurement use JavaFX, so an initialised
toolkit is required — no window is shown.

| Option | Effect |
|---|---|
| `size(w, h)` | the layout the plot is rendered at |
| `background(Color)` | backdrop colour, or `null` for transparency |
| `rasterizeAbove(n)` | layers with more than *n* shapes are embedded as a raster image; pass `Integer.MAX_VALUE` for pure vector |
| `rasterScale(s)` | resolution of those embedded rasters |
| `batchPoints(b)` | group same-styled points so their paint is written once |
| `compress(b)` | write gzip-compressed `.svgz` |

**Why the hybrid mode exists.** A genome-wide scatter of 467,000 points becomes
54 MB of SVG if every point is its own element — unopenable in most vector
editors. Batching same-styled points into one group brings that to about 18 MB;
diverting the dense layer into an embedded PNG while keeping axes, grid, labels,
and legend as vector brings it to under 4 MB and stays editable where editing
matters. This is the standard trade-off between vector fidelity and file size,
equivalent to rasterizing a dense layer. Plots below the threshold are pure
vector throughout.

Correctness is checked against [Apache Batik](https://xmlgraphics.apache.org/batik/):
the test suite renders a plot through both backends, rasterises the SVG with
Batik, and compares the two images region by region.

---

## Example Gallery

**[→ Browse the example figures, grouped by geometry](docs/GALLERY.md)**

Every example ships as both PNG and SVG. In the gallery each figure links to its
vector version, which opens straight in the browser — scalable and editable.
The page is generated together with the images, so it cannot drift out of sync
with what the examples actually render.

---

## Grammar of Graphics & References

gog4j is an independent implementation of the *Grammar of Graphics*. It treats
the grammar — data, aesthetics, geometries, statistics, scales, coordinates,
facets, and themes — as the baseline vocabulary of every plot, and implements it
from scratch on its own `GgFigure`/`PlotMatrix`/descriptor infrastructure rather
than porting or wrapping another library. The project's goal is feature parity
in a pure Java, type-safe form.

The Grammar of Graphics concept originates with Leland Wilkinson's *The Grammar
of Graphics*, and the following pre-existing software projects are the
conceptual references whose behaviour and feature set inspired parts of gog4j.
They are acknowledged here, together with their licences:

- **[ggplot2](https://ggplot2.tidyverse.org/)** and **[plotnine](https://plotnine.org/)** — the layer-based grammar (`aes`, `geoms`, `facets`, `scales`, `labs`, `guides`, `theme`) and the guiding model for feature parity. ggplot2 is licensed under the MIT licence; plotnine under the MIT licence.
- **[GGally](https://ggobi.github.io/ggally/)** — the generalized pairs plot that inspired `matrixPlot()` (histogram diagonal, correlation annotations, per-column-type dispatch, shared legend). Licensed under the GPL-2.0 licence.
- **[patchwork](https://patchwork.data-imaginist.com/)** — the composition semantics behind `composedPlot()` (aligned panels, `nrow`/`ncol`/`byrow` layout, lettered tags, collected legend). Licensed under the MIT licence.
- **[ggcube](https://github.com/matthewkling/ggcube)** — the perspective 3-D cube presentation that inspired `coord3d()`. Licensed under the MIT licence.

These projects are not bundled, linked, or required at runtime; gog4j contains
no code from them.

---

## License & 3rd Party Libraries

### Project License
Licensed under the [Apache License, Version 2.0](LICENSE).

### Third-Party Software & Components
- **JavaFX / OpenJFX**: [GPLv2 with Classpath Exception](https://openjfx.io/)
- **DFLib**: [Apache License 2.0](https://dflib.org/)
- **Apache Commons Math**: [Apache License 2.0](https://commons.apache.org/proper/commons-math/)

### Example Datasets
- **Human height GWAS** (`examples/gwas`): derived from Yengo *et al.*, *Nature* **610**, 704–712 (2022), [CC BY 4.0](https://creativecommons.org/licenses/by/4.0/). Summary statistics courtesy of the [GIANT consortium](https://portals.broadinstitute.org/collaboration/giant/index.php/GIANT_consortium_data_files). See [the dataset README](gog4j-data/src/main/resources/examples/gwas/README.md) for provenance and preprocessing.
