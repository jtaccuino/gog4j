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

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import org.jtaccuino.gog.Aes;
import org.jtaccuino.gog.AesValue;
import org.jtaccuino.gog.data.Values;
import org.jtaccuino.gog.spi.DataExtractor;

/**
 * {@code Stats.count()} statistical transformation — the inherited statistic of
 * {@code Geoms.bar()} and {@code Geoms.col()}: counts the observations for each
 * distinct value of {@code aes(x)} and emits one row per category with the
 * count and the proportion of the total.
 * <p>
 * The output ({@code x}/{@code count}/{@code prop}) mirrors the reference exactly, so
 * the computed variables can be referenced from an aesthetic mapping via
 * {@link AesValue#afterStat(AesValue.ComputedVariable)} (e.g.
 * {@code aes().fill(AesValue.afterStat(AesValue.ComputedVariable.COUNT))}) to colour bars by
 * their count, independent of any group/fill column.
 *
 * @param <DF> the DataFrame type representing the underlying dataset
 */
public class StatCount<DF> implements Stat<DF> {

    /**
     * Constructs a Stats.count transformation.
     */
    public StatCount() {
    }

    @Override
    public StatData computeLayer(DF df, DataExtractor<DF> ext, Aes aes, StatParams params) {
        var xCol = aes.x() == null ? null : ext.getColumn(df, aes.x());
        if (xCol == null) {
            return StatData.builder().build();
        }
        var categories = new LinkedHashSet<>();
        for (var v : xCol) {
            if (v != null) {
                categories.add(v);
            }
        }
        var order = new ArrayList<>(categories);
        order.sort((a, b) -> Values.label(a).compareTo(Values.label(b)));

        int total = 0;
        for (var cat : order) {
            total += countOf(cat, xCol);
        }

        var builder = StatData.builder();
        for (var cat : order) {
            int count = countOf(cat, xCol);
            builder.add("x", cat)
                    .add("count", (double) count)
                    .add("prop", total > 0 ? count / (double) total : 0.0);
        }
        return builder.build();
    }

    private static int countOf(Object category, List<?> values) {
        int count = 0;
        for (var v : values) {
            if (category.equals(v)) {
                count++;
            }
        }
        return count;
    }

    @Override
    public Aes requiredAes() {
        return Aes.aes();
    }

    @Override
    public List<String> outputColumns() {
        return List.of("x", "count", "prop");
    }
}
