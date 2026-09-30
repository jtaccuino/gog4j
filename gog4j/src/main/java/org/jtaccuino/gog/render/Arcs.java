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
package org.jtaccuino.gog.render;

import java.util.Locale;

/**
 * Shared elliptical-arc geometry. Both backends render arcs as SVG path
 * segments — JavaFX via {@code appendSVGPath}, the SVG builder by emitting the
 * same string — so the point and sweep arithmetic lives here, exactly once.
 */
final class Arcs {

    private Arcs() {
    }

    /**
     * Builds the SVG elliptical-arc data describing a sweep from
     * {@code startAngle} to {@code startAngle + arcExtent} degrees, following
     * the canvas convention: 0 at three o'clock, positive clockwise, y down.
     * <p>
     * A full circle cannot be a single SVG arc, so sweeps beyond {@code ±360°}
     * are split into two half arcs.
     *
     * @return the data without a leading "A", e.g. {@code "A 50 50 0 0 1 100 100"}
     */
    static String arcData(double cx, double cy, double rx, double ry,
                          double startAngle, double arcExtent) {
        StringBuilder out = new StringBuilder();
        double remaining = arcExtent;
        double angle = startAngle;
        while (Math.abs(remaining) > 1e-9) {
            double step = Math.copySign(Math.min(Math.abs(remaining), 180.0), remaining);
            double end = angle + step;
            double x2 = cx + rx * Math.cos(Math.toRadians(end));
            double y2 = cy + ry * Math.sin(Math.toRadians(end));
            out.append('A').append(num(rx)).append(' ').append(num(ry))
               .append(" 0 0 ")
               .append(remaining >= 0 ? 1 : 0).append(' ')
               .append(num(x2)).append(' ').append(num(y2)).append(' ');
            remaining -= step;
            angle = end;
        }
        return out.toString();
    }

    /**
     * Formats a number for SVG output, trimming unneeded decimals.
     */
    static String num(double value) {
        if (value == Math.rint(value) && Math.abs(value) < 1e15) {
            return String.valueOf((long) value);
        }
        String s = String.format(Locale.US, "%.2f", value);
        if (s.endsWith(".00")) {
            return s.substring(0, s.length() - 3);
        }
        return s;
    }
}
