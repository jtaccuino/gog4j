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

import java.lang.ref.SoftReference;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * A single-slot cache that keeps its value strongly only while the JVM is not
 * under memory pressure. The value is held through a {@link SoftReference}, so
 * when the collector is close to the heap limit it is dropped and rebuilt by
 * {@link #get(Supplier)} on the next access.
 * <p>
 * The dataset loaders (and their derived frames) use this so browsing the heavy
 * GWAS examples never permanently pins hundreds of megabytes once the plots
 * leave the screen: scrolling past a sample loads its data, and if the heap
 * tightens the collector simply discards it instead of the JVM running out.
 *
 * @param <T> the cached value type
 */
public final class SoftCache<T> {

    /** Creates an empty soft cache. */
    public SoftCache() {
    }

    private final Object lock = new Object();
    private volatile SoftReference<T> reference;

    /**
     * Returns the cached value, building it via {@code loader} if it is absent
     * or has been evicted by the collector. Concurrent callers share one load.
     *
     * @param loader builds the value when the cache is empty or evicted
     * @return the cached (or freshly loaded) value
     */
    public T get(Supplier<T> loader) {
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
            var value = Objects.requireNonNull(loader.get());
            reference = new SoftReference<>(value);
            return value;
        }
    }
}
