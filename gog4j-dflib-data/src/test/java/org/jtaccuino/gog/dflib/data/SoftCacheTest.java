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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;

/**
 * Verifies the {@link SoftCache} single-slot semantics: a hot cache returns the
 * same instance (the value is only weakly pinned once the collector evicts it),
 * and an evicted cache rebuilds through the loader instead of serving stale
 * state. Eviction is simulated by clearing the reference so the test is
 * deterministic regardless of JVM soft-reference timing.
 */
class SoftCacheTest {

    @Test
    void hotCacheReturnsTheSameInstance() {
        var cache = new SoftCache<Object>();
        var calls = new AtomicInteger();
        Supplier<Object> loader = () -> {
            calls.incrementAndGet();
            return new Object();
        };

        var first = cache.get(loader);
        assertSame(first, cache.get(loader),
                "a hot cache must return the same instance on repeat access");
        assertSame(first, cache.get(loader));
        assertEquals(1, calls.get(), "the loader must run exactly once while hot");
    }

    @Test
    void evictedCacheRebuildsThroughTheLoader() throws Exception {
        var cache = new SoftCache<Object>();
        var calls = new AtomicInteger();
        Supplier<Object> loader = () -> {
            calls.incrementAndGet();
            return new Object();
        };

        var first = cache.get(loader);
        assertEquals(1, calls.get());

        // Simulate the collector evicting the soft reference under pressure.
        var reference = SoftCache.class.getDeclaredField("reference");
        reference.setAccessible(true);
        reference.set(cache, null);

        var second = cache.get(loader);
        assertNotSame(first, second,
                "an evicted cache must rebuild a fresh value through the loader");
        assertEquals(2, calls.get(), "the loader must run again after eviction");
    }
}
