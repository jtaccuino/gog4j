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
package org.jtaccuino.gog.spi;

import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Resolves {@link DataExtractor} implementations for a given DataFrame type.
 * <p>
 * Providers are discovered on the classpath via {@link ServiceLoader} from the
 * service file {@code META-INF/services/org.jtaccuino.gog.spi.DataExtractor}.
 * Each provider must have a public no-argument constructor. Resolution picks the
 * first registered provider whose {@link DataExtractor#supports(Class)} accepts the
 * DataFrame type, and caches the result per type.
 */
public final class DataExtractorRegistry {

    private static final Map<Class<?>, DataExtractor<?>> CACHE = new ConcurrentHashMap<>();
    private static final List<DataExtractor<?>> PROVIDERS =
            ServiceLoader.load(DataExtractor.class).stream()
                    .<DataExtractor<?>>map(ServiceLoader.Provider::get)
                    .toList();

    private DataExtractorRegistry() {
    }

    /**
     * Returns the extractor that supports the given DataFrame type, or {@code null}
     * if no registered provider accepts it.
     *
     * @param dataFrameType the runtime DataFrame class
     * @return the matching {@link DataExtractor}, or {@code null}
     */
    public static DataExtractor<?> extractorFor(Class<?> dataFrameType) {
        if (dataFrameType == null) return null;
        return CACHE.computeIfAbsent(dataFrameType, DataExtractorRegistry::find);
    }

    private static DataExtractor<?> find(Class<?> dataFrameType) {
        for (var provider : PROVIDERS) {
            if (provider.supports(dataFrameType)) return provider;
        }
        return null;
    }
}
