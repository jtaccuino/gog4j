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
package org.jtaccuino.gog.dflib.data;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

/**
 * Opens a bundled dataset resource as a {@link Reader}. Reads the bytes through
 * {@link Class#getResourceAsStream(String)} and closes the stream immediately,
 * so the loaders work whether {@code gog4j-data} is an exploded directory (a
 * development build) or a jar (the published artifact) — a file {@code Path}
 * derived from a jar URL would otherwise be unusable.
 */
final class CsvResources {

    private CsvResources() {
    }

    /**
     * Reads the given classpath resource into a reader.
     *
     * @param resource the absolute classpath resource path
     * @return a reader over the resource contents
     */
    static Reader reader(String resource) {
        try (var in = CsvResources.class.getResourceAsStream(resource)) {
            if (in == null) {
                throw new IllegalStateException("missing dataset resource " + resource);
            }
            return new StringReader(new String(in.readAllBytes(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new UncheckedIOException("cannot read dataset resource " + resource, e);
        }
    }
}
