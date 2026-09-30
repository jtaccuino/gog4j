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
package org.jtaccuino.gog.stat;

import java.util.List;
import org.jtaccuino.gog.Aes;
import org.jtaccuino.gog.AesValue;
import org.jtaccuino.gog.spi.DataExtractor;

/**
 * A statistical transformation — the {@code transform} half of a layer.
 * <p>
 * A stat turns raw input data into the values a geometry actually draws:
 * binning for histograms, count/probability summaries, density estimation,
 * smoothing fits, and so on. It is purely a data transformation and knows
 * nothing about drawing.
 * <p>
 * gog4j stats are computed once per render pass over the global DataFrame (the
 * {@code computeLayer} entry point) and the result is stashed in a layer's
 * {@link org.jtaccuino.gog.layer.LayerData} through the existing
 * {@code prepare(PlotContext)} seam, so every facet panel reuses the same fit.
 *
 * @param <DF> the DataFrame type representing the underlying dataset
 */
public interface Stat<DF> {

    /**
     * Computes the transformed data over the global DataFrame. gog4j models
     * the whole of the {@code layer computation} → {@code panel computation} →
     * {@code compute_group} tier here, but because partitioning by facet and
     * group is already handled by the plot pipeline, a stat may operate on
     * whatever slice of rows it needs: panelled layers read the partition via
     * {@code ctx}, while stat-first layers compute over the whole frame.
     *
     * @param df     the DataFrame to transform (global, or a panel partition
     *               when the host layer computes per panel)
     * @param ext    the data extraction strategy for {@code df}
     * @param aes    the effective aesthetic mapping for the layer
     * @param params the stat parameters bag
     * @return the transformed data, never {@code null}
     */
    StatData computeLayer(DF df, DataExtractor<DF> ext, Aes aes, StatParams params);

    /**
     * The aesthetics this stat consumes from the mapping. A stat that reads
     * {@code x} and {@code y}, for instance, returns
     * {@code new Aes().x(...).y(...)}. Used by hosts to validate or to know
     * which input columns to extract.
     *
     * @return the aesthetics this stat reads
     */
    default Aes requiredAes() {
        return Aes.aes();
    }

    /**
     * The names of the columns this stat produces in its {@link StatData} for a
     * given {@link #requiredAes()}. These are the variables that may be
     * referenced from an aesthetic mapping via {@link AesValue#afterStat(String)} —
     * e.g. {@code count}, {@code density} or {@code ncount} for a histogram.
     * <p>
     * The default returns the empty list, which is correct for stats that only
     * pass through their inputs. Stats that derive new values should override
     * this so the scale pipeline can resolve computed aesthetics against the
     * stat output.
     *
     * @return the stat output column names
     */
    default List<String> outputColumns() {
        return List.of();
    }
}
