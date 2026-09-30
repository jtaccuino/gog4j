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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.jtaccuino.gog.AesValue.AfterStat;
import org.jtaccuino.gog.AesValue.AfterStatExpr;
import org.jtaccuino.gog.AesValue.Raw;
import org.jtaccuino.gog.AesValue.Stage;
import org.jtaccuino.gog.stat.StatData;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the {@link AesValue} sealed hierarchy, its factory methods,
 * wire-round-trip semantics, and per-row resolution logic. Larger integration
 * tests for colour/scale resolution live in {@code BarColStatsPlotsTest}.
 */
class AesValueTest {

    @Test
    void rawPreservesPlainColumnNames() {
        var v = AesValue.raw("displ");
        assertEquals("displ", v.render());
        assertEquals("displ", v.column());
        assertTrue(v.isRaw());
        assertFalse(v.referencesStat());
        assertFalse(v.referencesScale());
    }

    @Test
    void rawParsesStatPrefixIntoAfterStat() {
        var v = AesValue.raw("::count");
        assertTrue(v instanceof AfterStat, "::count parses into AfterStat");
        assertEquals("::count", v.render());
        assertEquals("count", ((AfterStat) v).column());
        assertTrue(v.referencesStat());
    }

    @Test
    void rawParsesScalePrefixIntoAfterScale() {
        var v = AesValue.raw("@color");
        assertTrue(v instanceof AesValue.AfterScale, "@color parses into AfterScale");
        assertEquals("@color", v.render());
        assertTrue(v.referencesScale());
    }

    @Test
    void afterStatFactoryRendersWire() {
        var v = AesValue.afterStat("density");
        assertEquals("::density", v.render());
        assertEquals("density", v.column());
        assertTrue(v.referencesStat());
    }

    @Test
    void afterStatComputedVariableRendersWire() {
        var v = AesValue.afterStat(AesValue.ComputedVariable.NCOUNT);
        assertEquals("::ncount", v.render());
        assertEquals("ncount", v.column());
    }

    @Test
    void afterStatExprRendersAsExpr() {
        var v = AesValue.afterStat(sd -> List.of(1.0, 2.0), "count");
        assertEquals("::expr", v.render());
        assertTrue(v.referencesStat());
        assertFalse(v.referencesScale());
        assertTrue(((AfterStatExpr) v).requires().contains("count"));
    }

    @Test
    void afterStatExprColumnExposesRequirement() {
        var v = AesValue.afterStat(sd -> List.of(1.0, 2.0), "count", "prop");
        assertEquals("count", v.column());
        assertNull(AesValue.afterStat(sd -> List.of(1.0, 2.0)).column());
    }

    @Test
    void afterStatExprEvaluateValidatesRequiredColumns() {
        var v = (AfterStatExpr) AesValue.afterStat(sd -> List.of(1.0), "count");
        var sd = statData("x", List.of("a"));
        assertThrows(IllegalArgumentException.class,
                () -> v.evaluate(sd),
                "missing required column must throw");
    }

    @Test
    void afterStatExprEvaluateComputesValueColumn() {
        var v = (AfterStatExpr) AesValue.afterStat(sd -> sd.columnAsDoubles("count"),
                "count");
        var sd = statData("count", List.of(3.0, 5.0));
        assertEquals(List.of(3.0, 5.0), v.evaluate(sd));
    }

    @Test
    void afterScaleRendersReference() {
        var v = AesValue.afterScale("color");
        assertEquals("@color", v.render());
        assertTrue(v.referencesScale());
        assertFalse(v.referencesStat());
        assertFalse(v.isRaw());
    }

    @Test
    void unknownAfterScaleAestheticFailsFast() {
        assertThrows(IllegalArgumentException.class, () -> AesValue.afterScale("bogus"));
        assertThrows(IllegalArgumentException.class, () -> AesValue.raw("@bogus"));
        assertThrows(IllegalArgumentException.class, () -> AesValue.stage("count").afterScale("bogus"));
        assertThrows(IllegalArgumentException.class, () -> Aesthetic.of("bogus"));
    }

    @Test
    void stageAfterStatPreservesAfterStatRender() {
        var v = AesValue.stage("x")
                .afterStat(AesValue.ComputedVariable.COUNT)
                .build();
        assertTrue(v instanceof Stage);
        assertEquals("::count", v.render(), "stage render follows the after-stat stage");
        assertTrue(v.referencesStat());
        assertFalse(v.referencesScale());
    }

    @Test
    void stageAfterScalePreservesAfterScaleRender() {
        var v = AesValue.stage("count")
                .afterScale("fill")
                .build();
        assertEquals("@fill", v.render(), "stage render follows the after-scale stage");
        assertTrue(v.referencesScale());
    }

    @Test
    void stageCannotSetAfterStatTwice() {
        assertThrows(IllegalStateException.class,
                () -> AesValue.stage("x").afterStat("count").afterStat("density"),
                "double-pinning afterStat must fail");
    }

    @Test
    void stageCannotSetAfterScaleTwice() {
        assertThrows(IllegalStateException.class,
                () -> AesValue.stage("x").afterScale("color").afterScale("fill"),
                "double-pinning afterScale must fail");
    }

    @Test
    void aesStringSetterRoundTripsWireValues() {
        var a = Aes.aes()
                .fill("::count")
                .color("@color")
                .x("displ");
        assertEquals("::count", a.fill());
        assertEquals("count", ((AfterStat) a.fillValue()).column());
        assertEquals("@color", a.color());
        assertEquals("displ", a.x());
        assertTrue(a.xValue() instanceof Raw);
    }

    @Test
    void aesTypedSetterOverridesStringSetter() {
        var a = Aes.aes()
                .fill("::count")
                .fill(AesValue.afterScale("color"));
        assertEquals("@color", a.fill());
        assertTrue(a.fillValue() instanceof AesValue.AfterScale);
    }

    @Test
    void aesCopyAndOverridePreserveAesValues() {
        var base = Aes.aes().x("displ").y("hwy");
        var local = Aes.aes().y(AesValue.afterStat(AesValue.ComputedVariable.COUNT));
        var merged = base.overrideWith(local);
        assertEquals("displ", merged.x());
        assertEquals("::count", merged.y());
        assertTrue(merged.yValue() instanceof AfterStat);
    }

    @Test
    void aesValueResolveRawReadsRawValues() {
        var raw = AesValue.raw("col");
        var ctx = new AesValue.AesResolution<>(null, Aes.aes(),
                null, List.of(10.0, 20.0), new ArrayList<>());
        assertEquals(10.0, raw.resolve(ctx, 0));
        assertEquals(20.0, raw.resolve(ctx, 1));
    }

    @Test
    void aesValueResolveAfterStatReadsStatData() {
        var stat = AesValue.afterStat("density");
        var sd = statData("density", List.of(0.1, 0.25));
        var ctx = new AesValue.AesResolution<>(null, Aes.aes(),
                sd, null, new ArrayList<>());
        assertEquals(0.1, stat.resolve(ctx, 0));
        assertEquals(0.25, stat.resolve(ctx, 1));
    }

    @Test
    void aesValueResolveAfterStatExprComputes() {
        var expr = AesValue.afterStat(
                sd -> {
                    var c = sd.columnAsDoubles("count");
                    var out = new ArrayList<Double>();
                    for (var v : c) out.add(v * 2.0);
                    return List.copyOf(out);
                },
                "count");
        var sd = statData("count", List.of(3.0, 5.0));
        var ctx = new AesValue.AesResolution<>(null, Aes.aes(),
                sd, null, new ArrayList<>());
        assertEquals(6.0, expr.resolve(ctx, 0));
        assertEquals(10.0, expr.resolve(ctx, 1));
    }

    @Test
    void aesValueResolveCycleDetection() {
        var aes = Aes.aes()
                .fill(AesValue.afterScale("color"))
                .color(AesValue.afterScale("fill"));
        var ctx = new AesValue.AesResolution<>(null, aes,
                null, null, new ArrayList<>());
        assertThrows(IllegalArgumentException.class,
                () -> aes.fillValue().resolve(ctx, 0),
                "mutual afterScale cycle must throw");
    }

    private static StatData statData(String name, List<?> values) {
        var b = StatData.builder();
        for (var v : values) {
            b.add(name, v);
        }
        return b.build();
    }
}
