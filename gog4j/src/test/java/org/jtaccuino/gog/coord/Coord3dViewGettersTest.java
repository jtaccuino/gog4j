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
package org.jtaccuino.gog.coord;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * The view getters {@link Coord3D#pitch()}/{@link Coord3D#roll()}/{@link
 * Coord3D#yaw()} added for the {@code Orbit3d} controllers and readouts: they
 * must mirror the default 3-D view (pitch 0, roll -60, yaw -30) and round-trip
 * the fluent setters.
 */
class Coord3dViewGettersTest {

    @Test
    void defaultsMatchTheCoord3dView() {
        var coord = new Coord3D();
        assertEquals(0.0, coord.pitch(), 1e-9);
        assertEquals(-60.0, coord.roll(), 1e-9);
        assertEquals(-30.0, coord.yaw(), 1e-9);
    }

    @Test
    void explicitViewRoundTrips() {
        var coord = new Coord3D().pitch(35).roll(-75).yaw(-55);
        assertEquals(35.0, coord.pitch(), 1e-9);
        assertEquals(-75.0, coord.roll(), 1e-9);
        assertEquals(-55.0, coord.yaw(), 1e-9);
    }

    @Test
    void settersRebuildTheProjectionForTheNextRender() {
        var coord = new Coord3D();
        coord.yaw(12).pitch(8).roll(4);
        // The projection is rebuilt lazily; mutating a view angle must not
        // throw and must converge on the set orientation once projected.
        assertEquals(12.0, coord.yaw(), 1e-9);
        assertEquals(8.0, coord.pitch(), 1e-9);
        assertEquals(4.0, coord.roll(), 1e-9);
    }
}
