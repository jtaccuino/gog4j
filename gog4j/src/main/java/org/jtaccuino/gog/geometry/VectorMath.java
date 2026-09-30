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
package org.jtaccuino.gog.geometry;

/** Small 3-D vector and rotation helpers used by the 3-D shading pipeline. */
public final class VectorMath {

    private VectorMath() {
    }

    /**
     * The rotation matrix mapping the unit vector {@code a} onto the unit
     * vector {@code b} (Rodrigues' formula). Degenerate inputs (parallel or
     * anti-parallel axes) fall back to the identity or a 180° rotation about a
     * perpendicular axis.
     *
     * @param a the source unit vector
     * @param b the target unit vector
     * @return the 3×3 rotation matrix {@code R} with {@code R·a = b}
     */
    public static double[][] rotationAligning(double[] a, double[] b) {
        double d = a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
        double cx = a[1] * b[2] - a[2] * b[1];
        double cy = a[2] * b[0] - a[0] * b[2];
        double cz = a[0] * b[1] - a[1] * b[0];
        double cl = Math.sqrt(cx * cx + cy * cy + cz * cz);
        if (cl < 1e-9) {
            if (d > 0) {
                return new double[][] {{1, 0, 0}, {0, 1, 0}, {0, 0, 1}};
            }
            double ux = -a[1], uy = a[0], uz = 0; // a × (0, 0, 1), always perpendicular
            double ul = Math.sqrt(ux * ux + uy * uy + uz * uz);
            if (ul < 1e-9) {
                ux = -a[2];
                uz = a[0];
                ul = Math.sqrt(ux * ux + uz * uz);
            }
            ux /= ul;
            uy /= ul;
            uz /= ul;
            // 180° about the axis: 2·u·uᵀ − I.
            return new double[][] {
                {2 * ux * ux - 1, 2 * ux * uy, 2 * ux * uz},
                {2 * uy * ux, 2 * uy * uy - 1, 2 * uy * uz},
                {2 * uz * ux, 2 * uz * uy, 2 * uz * uz - 1}
            };
        }
        double m = (1 - d) / (cl * cl);
        double ux = cx / cl, uy = cy / cl, uz = cz / cl;
        return new double[][] {
            {m * ux * ux + 1 - m, m * ux * uy - uz, m * ux * uz + uy},
            {m * uy * ux + uz, m * uy * uy + 1 - m, m * uy * uz - ux},
            {m * uz * ux - uy, m * uz * uy + ux, m * uz * uz + 1 - m}
        };
    }
}
