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
package org.jtaccuino.gog.stat;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * An immutable bag of named parameters configured on a {@link Stat} when the
 * layer was built — the analogue of the {@code params} list threaded
 * through {@code setup_*} and {@code compute_*}.
 * <p>
 * Params are supplied as a string→value map. Host layers that need typed,
 * validated access wrap their known keys in typed accessor methods.
 */
public final class StatParams {

    private final Map<String, Object> params;

    private StatParams(Map<String, Object> params) {
        this.params = Collections.unmodifiableMap(new LinkedHashMap<>(params));
    }

    /**
     * Creates a parameter bag from the given name→value pairs.
     *
     * @param params the parameters, may be empty; copied defensively
     * @return a new immutable {@link StatParams}
     */
    public static StatParams of(Map<String, Object> params) {
        return new StatParams(params);
    }

    /**
     * Creates an empty parameter bag.
     *
     * @return an immutable {@link StatParams} with no entries
     */
    public static StatParams empty() {
        return new StatParams(Map.of());
    }

    /**
     * Reads a parameter, or a default when it was not supplied.
     *
     * @param name  the parameter name
     * @param fallback the value returned when the parameter is absent
     * @return the parameter value, or {@code fallback}
     */
    public Object get(String name, Object fallback) {
        return params.getOrDefault(name, fallback);
    }

    /**
     * Reads a parameter as a {@code double} if it was supplied as a
     * {@link Number}, otherwise returns {@code fallback}.
     *
     * @param name  the parameter name
     * @param fallback the value returned when the parameter is absent or not numeric
     * @return the numeric value, or {@code fallback}
     */
    public double getDouble(String name, double fallback) {
        var v = params.get(name);
        return v instanceof Number n ? n.doubleValue() : fallback;
    }

    /**
     * Reads a parameter as an {@code int} if it was supplied as a
     * {@link Number}, otherwise returns {@code fallback}.
     *
     * @param name  the parameter name
     * @param fallback the value returned when the parameter is absent or not numeric
     * @return the integer value, or {@code fallback}
     */
    public int getInt(String name, int fallback) {
        var v = params.get(name);
        return v instanceof Number n ? n.intValue() : fallback;
    }

    /**
     * Reads a parameter as a {@code boolean}.
     *
     * @param name  the parameter name
     * @param fallback the value returned when the parameter is absent or not a Boolean
     * @return the boolean value, or {@code fallback}
     */
    public boolean getBoolean(String name, boolean fallback) {
        var v = params.get(name);
        return v instanceof Boolean b ? b : fallback;
    }

    /**
     * Whether the given parameter was supplied.
     *
     * @param name the parameter name
     * @return {@code true} if the parameter is present
     */
    public boolean contains(String name) {
        return params.containsKey(name);
    }

    /**
     * {@return the number of parameters}
     */
    public int size() {
        return params.size();
    }
}
