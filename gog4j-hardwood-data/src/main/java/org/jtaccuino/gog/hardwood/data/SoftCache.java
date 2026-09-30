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
package org.jtaccuino.gog.hardwood.data;

import java.lang.ref.SoftReference;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * A single-slot cache that keeps its value strongly only while the JVM is not
 * under memory pressure, so the heavy GWAS frame is not permanently pinned once
 * its plots leave the screen.
 *
 * @param <T> the cached value type
 */
final class SoftCache<T> {

    private final Object lock = new Object();
    private volatile SoftReference<T> reference;

    /** Creates an empty soft cache. */
    SoftCache() {
    }

    /**
     * Returns the cached value, building it via {@code loader} if absent or
     * evicted. Concurrent callers share one load.
     *
     * @param loader builds the value when the cache is empty or evicted
     * @return the cached (or freshly loaded) value
     */
    T get(Supplier<T> loader) {
        Objects.requireNonNull(loader, "loader");
        var ref = reference;
        if (ref != null) {
            var value = ref.get();
            if (value != null) {
                return value;
            }
        }
        synchronized (lock) {
            ref = reference;
            if (ref != null) {
                var value = ref.get();
                if (value != null) {
                    return value;
                }
            }
            var value = loader.get();
            reference = new SoftReference<>(value);
            return value;
        }
    }
}
