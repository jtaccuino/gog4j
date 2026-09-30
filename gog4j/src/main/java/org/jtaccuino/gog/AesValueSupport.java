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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.jtaccuino.gog.layer.PanelContext;
import org.jtaccuino.gog.scale.ResolvedScales;
import org.jtaccuino.gog.scale.Scale;
import org.jtaccuino.gog.scale.ScaleTransform;
import org.jtaccuino.gog.stat.StatData;

/**
 * Resolution engine behind {@link AesValue#resolve}. Kept out of the sealed
 * hierarchy so the reference unwinding machinery can depend on {@link
 * PanelContext}, {@link ResolvedScales} and the {@link Aes} mapping without
 * widening the public record surface. The public {@link
 * #statColourInfo(AesValue, StatData, int)} helper also serves the layer
 * package's stat-paint and tooltip colour resolution.
 */
public final class AesValueSupport {

    private AesValueSupport() {
    }

    /**
     * Resolves an {@code @aesthetic} reference: reads the referenced aesthetic's
     * own value (through its full pipeline) and maps that value through the
     * referenced scale. Guards against reference cycles by tracking the
     * in-flight chain.
     */
    static Object resolveReference(AesValue.AesResolution<?> ctx, Aesthetic aesthetic,
                                   Object injectedValue, int row) {
        var inFlight = ctx.inFlight();
        if (inFlight.contains(aesthetic)) {
            var chain = new ArrayList<>(inFlight);
            chain.add(aesthetic);
            throw new IllegalArgumentException("afterScale reference cycle detected: "
                    + chain.stream().map(Aesthetic::key).collect(Collectors.joining(" -> ")));
        }
        if (injectedValue != null) {
            return mapThroughScale(ctx, aesthetic, injectedValue, null);
        }
        var target = targetValue(ctx, aesthetic);
        if (target == null) {
            return null;
        }
        inFlight.add(aesthetic);
        Object value;
        try {
            value = target.resolve(ctx, row);
        } finally {
            inFlight.remove(inFlight.size() - 1);
        }
        return mapThroughScale(ctx, aesthetic, value, target);
    }

    /**
     * Maps a value through the scale applied to the {@code source} aesthetic
     * (or through the referenced aesthetic when {@code source} is {@code null}).
     * Colour, fill, shape, and linetype references return the scale-mapped
     * visual; x and y return the value in the referenced axis's transformed
     * space.
     */
    static Object mapThroughScale(AesValue.AesResolution<?> ctx, Aesthetic aesthetic,
                                  Object value, AesValue source) {
        if (value == null) {
            return null;
        }
        var scales = ctx.scales();
        var key = keyFor(ctx, aesthetic, source);
        return switch (aesthetic) {
            case COLOR, FILL -> key == null ? value : scales.resolvedColorFor(key, value);
            case SHAPE -> {
                var scale = key == null ? null : scales.shapeScale(key);
                yield scale == null ? value : scale.shapeFor(value);
            }
            case LINETYPE -> {
                var scale = key == null ? null : scales.linetypeScale(key);
                yield scale == null ? value : scale.patternFor(value);
            }
            case X -> transformAs(ctx.panel().scaleX().transform(), ctx.panel().scaleX(), value);
            case Y -> transformAs(ctx.panel().scaleY().transform(), ctx.panel().scaleY(), value);
            default -> value;
        };
    }

    private static Object transformAs(ScaleTransform transform, Scale scale,
                                      Object value) {
        if (value instanceof Number n) {
            return transform.forward(n.doubleValue());
        }
        return scale.toData(value);
    }

    /**
     * The {@link AesValue} the referenced aesthetic maps to in the effective
     * layer mapping.
     */
    private static AesValue targetValue(AesValue.AesResolution<?> ctx, Aesthetic aesthetic) {
        var aes = ctx.aes();
        return aes == null ? null : aes.value(aesthetic);
    }

    /**
     * The column key used to select the referenced aesthetic's scale: the bare
     * column for raw/stat values, or the wire form when the value was computed
     * inline.
     */
    private static String keyFor(AesValue.AesResolution<?> ctx, Aesthetic aesthetic,
                                 AesValue sourceValue) {
        var target = sourceValue != null ? sourceValue : targetValue(ctx, aesthetic);
        if (target == null) {
            return null;
        }
        return switch (target) {
            case AesValue.Raw r -> r.value();
            case AesValue.AfterStat s -> s.column();
            case AesValue.AfterScale s -> resolveKey(ctx, s);
            default -> target.render();
        };
    }

    /**
     * The key of an after-scale reference is its own referenced aesthetic's key,
     * unwound one level so that {@code fill = afterScale("color")} selects the
     * colour column that drives the mapping.
     */
    private static String resolveKey(AesValue.AesResolution<?> ctx, AesValue.AfterScale ref) {
        var target = targetValue(ctx, ref.aesthetic());
        return target == null ? null : keyFor(ctx, ref.aesthetic(), target);
    }

    /**
     * The stat column feeding a {@code afterStat} fill/color reference, its
     * values, and the cell the given row maps to.
     *
     * @param column   the driving stat column name
     * @param values   the column values
     * @param rowValue the cell the row maps to
     */
    public record StatColourInfo(String column, List<?> values, Object rowValue) {
    }

    /**
     * Reads the stat column driving an {@code afterStat} fill/color reference
     * and the cell the given row maps to, so paint resolution and the tooltip
     * colour hint read the column exactly once. Inline expressions report
     * their evaluated column; a missing requirement, an unknown column, or an
     * out-of-range row yields {@code null}.
     *
     * @param value    the fill/color {@link AesValue}, or {@code null}
     * @param statData layer's stat output
     * @param row      the zero-based stat row
     * @return the {@link StatColourInfo}, or {@code null}
     */
    public static StatColourInfo statColourInfo(AesValue value, StatData statData, int row) {
        if (value instanceof AesValue.AfterStat ref) {
            return columnCell(ref.column(), statData.column(ref.column()), row);
        }
        if (value instanceof AesValue.AfterStatExpr ex) {
            if (ex.requires().isEmpty()
                    || !statData.columnNames().containsAll(ex.requires())) {
                return null;
            }
            return columnCell(ex.requires().get(0), ex.evaluate(statData), row);
        }
        return null;
    }

    private static StatColourInfo columnCell(String column, List<?> values, int row) {
        if (column == null || values == null || row < 0 || row >= values.size()) {
            return null;
        }
        return new StatColourInfo(column, values, values.get(row));
    }
}
