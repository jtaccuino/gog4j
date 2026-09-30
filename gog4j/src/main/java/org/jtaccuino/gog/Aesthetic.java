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

import java.util.Map;

/**
 * The aesthetics a layer can map: the position aesthetics ({@code x}, {@code y},
 * {@code z}, and the range/endpoint forms) and the visual-aesthetic equivalents
 * of the scale names ({@code colour}, {@code fill}, {@code size},
 * {@code shape}, {@code linetype}, {@code alpha}). Each constant carries the
 * lowercase wire name used by {@link Aes#aesOf(java.util.Map)} keys and by
 * {@code @}-prefixed {@code afterScale(...)} references.
 */
public enum Aesthetic {

    /** The x aesthetic. */
    X("x"),
    /** The y aesthetic. */
    Y("y"),
    /** The z aesthetic. */
    Z("z"),
    /** The xend aesthetic. */
    XEND("xend"),
    /** The yend aesthetic. */
    YEND("yend"),
    /** The zend aesthetic. */
    ZEND("zend"),
    /** The xmin aesthetic. */
    XMIN("xmin"),
    /** The xmax aesthetic. */
    XMAX("xmax"),
    /** The ymin aesthetic. */
    YMIN("ymin"),
    /** The ymax aesthetic. */
    YMAX("ymax"),
    /** The zmin aesthetic. */
    ZMIN("zmin"),
    /** The zmax aesthetic. */
    ZMAX("zmax"),
    /** The weight aesthetic. */
    WEIGHT("weight"),
    /** The color aesthetic. */
    COLOR("color"),
    /** The fill aesthetic. */
    FILL("fill"),
    /** The shape aesthetic. */
    SHAPE("shape"),
    /** The linetype aesthetic. */
    LINETYPE("linetype"),
    /** The size aesthetic. */
    SIZE("size"),
    /** The alpha aesthetic. */
    ALPHA("alpha"),
    /** The group aesthetic. */
    GROUP("group"),
    /** The label aesthetic. */
    LABEL("label");

    private static final Map<String, Aesthetic> BY_KEY = Map.ofEntries(
            Map.entry("x", Aesthetic.X),
            Map.entry("y", Aesthetic.Y),
            Map.entry("z", Aesthetic.Z),
            Map.entry("xend", Aesthetic.XEND),
            Map.entry("yend", Aesthetic.YEND),
            Map.entry("zend", Aesthetic.ZEND),
            Map.entry("xmin", Aesthetic.XMIN),
            Map.entry("xmax", Aesthetic.XMAX),
            Map.entry("ymin", Aesthetic.YMIN),
            Map.entry("ymax", Aesthetic.YMAX),
            Map.entry("zmin", Aesthetic.ZMIN),
            Map.entry("zmax", Aesthetic.ZMAX),
            Map.entry("weight", Aesthetic.WEIGHT),
            Map.entry("color", Aesthetic.COLOR),
            Map.entry("fill", Aesthetic.FILL),
            Map.entry("shape", Aesthetic.SHAPE),
            Map.entry("linetype", Aesthetic.LINETYPE),
            Map.entry("size", Aesthetic.SIZE),
            Map.entry("alpha", Aesthetic.ALPHA),
            Map.entry("group", Aesthetic.GROUP),
            Map.entry("label", Aesthetic.LABEL)
    );
    private final String key;

    Aesthetic(String key) {
        this.key = key;
    }

    /** {@return the lowercase aesthetic name, as used in wire values} */
    public String key() {
        return key;
    }

    /**
     * The aesthetic whose wire name matches, failing fast on unknown names so
     * a typo in an {@code afterScale(...)} reference or {@code aesOf(...)} key
     * surfaces at parse time instead of silently mapping to nothing.
     *
     * @param name the lowercase aesthetic name, e.g. {@code "fill"}
     * @return the matching aesthetic
     * @throws IllegalArgumentException when {@code name} is not a known aesthetic
     */
    public static Aesthetic of(String name) {
        var aesthetic = BY_KEY.get(name);
        if (aesthetic == null) {
            throw new IllegalArgumentException("Unrecognised aesthetic: '" + name + "'");
        }
        return aesthetic;
    }
}
