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
package org.jtaccuino.gog.layer;

import org.jtaccuino.gog.Aes;
import org.jtaccuino.gog.RenderMode;
import org.jtaccuino.gog.coord.Coord;
import org.jtaccuino.gog.coord.Light3d;
import org.jtaccuino.gog.labs.LabsSpec;
import org.jtaccuino.gog.scale.ResolvedScales;
import org.jtaccuino.gog.scale.ScaleSpec;
import org.jtaccuino.gog.spi.DataExtractor;
import org.jtaccuino.gog.theme.Theme;

/**
 * The plot-scoped context handed to a layer's {@link Layer#prepare} phase. It
 * carries the unpartitioned master DataFrame, the data extraction strategy,
 * the effective {@link Aes}, the scale and label specifications, the
 * coordinate system, the active theme, the eagerly resolved
 * {@link ResolvedScales} shared by all layers, the legend, and the tooltip.
 *
 * @param globalDf  the unpartitioned master DataFrame
 * @param extractor the data extraction strategy
 * @param aes       the effective aesthetic mapping for the layer
 * @param scaleSpec the plot's scale specification
 * @param labs      the plot's label dictionary
 * @param coord     the coordinate system transformer
 * @param theme     the active plot theme
* @param scales    the eagerly resolved scales
 * @param renderMode the configured quality-vs-speed tradeoff
 * @param light     the plot-level 3D light, or {@code null}
 * @param <DF>      the DataFrame type representing the underlying dataset
 */
public record PlotContext<DF>(DF globalDf, DataExtractor<DF> extractor, Aes aes,
                              ScaleSpec scaleSpec, LabsSpec labs, Coord coord, Theme theme,
                              ResolvedScales<DF> scales, RenderMode renderMode, Light3d light) {
}
