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
package org.jtaccuino.gog.sampler.registry;

import java.util.Objects;
import org.jtaccuino.gog.sampler.meta.TagKind;

/**
 * A single, named filterable attribute of an example, such as its dataset
 * group, the geom layers it renders, or the coordinate system it uses.
 *
 * @param name the unique hyphenated identifier, e.g. {@code geom:boxplot}
 * @param label the human-readable label shown in the filter UI
 * @param kind the {@link TagKind} category this tag belongs to
 */
public record Tag(String name, String label, TagKind kind) {

    /** Compact constructor rejecting null components. */
    public Tag {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(label, "label");
        Objects.requireNonNull(kind, "kind");
    }

    @Override
    public String toString() {
        return name;
    }
}
