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
package org.jtaccuino.gog.builder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Verifies the {@link BuilderModel.GeomKind} dimension classification used to
 * group the builder's add-layer menu.
 */
class BuilderModelTest {

    @Test
    void everyGeomHasACategory() {
        for (var kind : BuilderModel.GeomKind.values()) {
            assertNotNull(kind.category(), () -> kind + " must carry a category");
        }
    }

    @Test
    void the3dGeomsAreGroupedUnderThreeD() {
        var threeD = EnumSet.of(
                BuilderModel.GeomKind.POINT3D,
                BuilderModel.GeomKind.TEXT3D,
                BuilderModel.GeomKind.SEGMENT3D,
                BuilderModel.GeomKind.PATH3D,
                BuilderModel.GeomKind.BAR3D,
                BuilderModel.GeomKind.COL3D,
                BuilderModel.GeomKind.SURFACE3D,
                BuilderModel.GeomKind.POLYGON3D,
                BuilderModel.GeomKind.VOXEL3D,
                BuilderModel.GeomKind.HULL3D,
                BuilderModel.GeomKind.RIDGELINE3D,
                BuilderModel.GeomKind.CONTOUR3D,
                BuilderModel.GeomKind.SMOOTH3D,
                BuilderModel.GeomKind.DENSITY3D,
                BuilderModel.GeomKind.FUNCTION3D);

        for (var kind : BuilderModel.GeomKind.values()) {
            var expected = threeD.contains(kind)
                    ? BuilderModel.GeomCategory.THREE_D
                    : BuilderModel.GeomCategory.TWO_D;
            assertEquals(expected, kind.category(), () -> kind + " category");
        }
        assertEquals(Set.of(BuilderModel.GeomCategory.TWO_D, BuilderModel.GeomCategory.THREE_D),
                EnumSet.allOf(BuilderModel.GeomCategory.class));
    }

    @Test
    void categoriesAreLabelledAndBothPopulated() {
        assertEquals("2D", BuilderModel.GeomCategory.TWO_D.label());
        assertEquals("3D", BuilderModel.GeomCategory.THREE_D.label());
        for (var category : BuilderModel.GeomCategory.values()) {
            assertTrue(Arrays.stream(BuilderModel.GeomKind.values())
                            .anyMatch(k -> k.category() == category),
                    () -> category + " must contain at least one geom");
        }
    }
}
