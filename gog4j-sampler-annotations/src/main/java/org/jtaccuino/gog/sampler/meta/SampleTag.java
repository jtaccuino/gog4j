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
package org.jtaccuino.gog.sampler.meta;

import java.util.Locale;

/**
 * Common contract of the per-kind tag catalog enums ({@code SampleDataset},
 * {@code SampleGeom}, ...). Each constant is one filterable attribute of an
 * example; the annotation processor and the runtime {@code Tags} facade build
 * {@code Tag}s from these values without any stringly-typed catalogue drift.
 */
public interface SampleTag {

    /**
     * Returns the category this catalog belongs to.
     *
     * @return the {@link TagKind} shared by every constant of the enum
     */
    TagKind kind();

    /**
     * Returns the feature identifier, e.g. {@code "boxplot"}.
     *
     * @return the per-kind feature suffix of the tag id
     */
    String feature();

    /**
     * Returns the human-readable display label, e.g. {@code "Notched Boxplot"}.
     *
     * @return the tag label shown in the filter UI
     */
    String label();

    /**
     * Returns the fully qualified tag id, e.g. {@code "geom:boxplot"}.
     *
     * @return {@code kind:feature}
     */
    default String id() {
        return kind().name().toLowerCase(Locale.ROOT) + ":" + feature();
    }
}
