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

import static org.jtaccuino.gog.Aes.aes;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

class AesTest {

    @Test
    void overrideWithIsFieldWise() {
        var merged = aes().x("displ").y("hwy")
                .overrideWith(aes().color("drv").shape("drv"));

        assertEquals("displ", merged.x());
        assertEquals("hwy", merged.y());
        assertEquals("drv", merged.color());
        assertEquals("drv", merged.shape());
        // Aesthetics not set on the local mapping fall back to the base one.
        assertNull(merged.size());
        assertNull(merged.alpha());
        assertNull(merged.group());
    }

    @Test
    void overrideWithNullLocalReturnsSameInstance() {
        var base = aes().x("displ").y("hwy");
        assertSame(base, base.overrideWith(null));
    }

    @Test
    void overrideWithDoesNotMutateOperands() {
        var base = aes().x("displ").color("drv");
        var local = aes().color("cyl");
        base.overrideWith(local);

        assertEquals("displ", base.x());
        assertEquals("drv", base.color());
        assertEquals("cyl", local.color());
    }

    @Test
    void overrideWithLocalWinsEveryAesthetic() {
        var merged = aes().x("a").y("b").color("c").fill("d").size("e").shape("f")
                .group("g").alpha("h").z("i").label("j").linetype("k")
                .overrideWith(aes().color("x").fill("y").size("z").linetype("lt"));

        assertEquals("a", merged.x());
        assertEquals("b", merged.y());
        assertEquals("x", merged.color());
        assertEquals("y", merged.fill());
        assertEquals("z", merged.size());
        assertEquals("f", merged.shape());
        assertEquals("g", merged.group());
        assertEquals("h", merged.alpha());
        assertEquals("i", merged.z());
        assertEquals("j", merged.label());
        assertEquals("lt", merged.linetype());
    }

    @Test
    void overrideWithKeepsLinetypeWhenLocalUnset() {
        var merged = aes().x("a").y("b").linetype("drv")
                .overrideWith(aes().color("cyl"));
        assertEquals("a", merged.x());
        assertEquals("drv", merged.linetype());
        assertEquals("cyl", merged.color());
    }
}
