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

import org.jtaccuino.gog.RenderMode;
import org.jtaccuino.gog.facet.FacetValues;
import org.jtaccuino.gog.scale.Scale;

/**
 * The panel-scoped context handed to a layer's {@link Layer#render} and
 * {@link Layer#locate} phases. It carries the shared {@link PlotContext}, the
 * current panel's partition DataFrame, the panel's facet levels, the pixel
 * scales for the two axes, and the effective {@link RenderMode} — the
 * configured quality-vs-speed tradeoff already resolved against the rendering
 * backend (a vector surface always renders {@link RenderMode#FULL}).
 *
 * @param plot        the shared plot context
 * @param partitionDf the current panel partition DataFrame
 * @param facetValues the panel's facet levels, or an empty {@link FacetValues}
 * @param scaleX      the horizontal pixel scale
 * @param scaleY      the vertical pixel scale
 * @param renderMode  the effective quality-vs-speed tradeoff
 * @param <DF>        the DataFrame type representing the underlying dataset
 */
public record PanelContext<DF>(PlotContext<DF> plot, DF partitionDf,
                               FacetValues facetValues, Scale scaleX, Scale scaleY,
                               RenderMode renderMode) {
}
