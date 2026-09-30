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

/**
 * An explicit three-way switch, the typed replacement for a nullable
 * {@link Boolean} whose {@code null} stood for "follow the default".
 * {@link #AUTO} lets a coordinate system pick its own behaviour, while
 * {@link #YES} and {@link #NO} force it on or off.
 */
public enum Mode {

    /** Follow the coordinate system's default for the option. */
    AUTO,

    /** Force the option on. */
    YES,

    /** Force the option off. */
    NO
}
