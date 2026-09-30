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
package org.jtaccuino.gog.sampler;

import java.util.List;
import java.util.Objects;
import java.util.prefs.Preferences;
import org.jtaccuino.gog.RenderMode;

/**
 * Persists the sampler's browsing state (search query, active filter tags, and
 * the A/B render mode) between launches using the platform preferences store.
 * A {@link Preferences} node is injected so tests can point the sampler at a
 * throwaway node instead of the real user preferences.
 */
public final class SamplerPreferences {

    private static final String KEY_QUERY = "filter.query";
    private static final String KEY_TAGS = "filter.tags";
    private static final String KEY_RENDER_MODE = "render.mode";
    private static final String KEY_DARK_THEME = "theme.dark";
    private static final String MODE_FULL = "full";
    private static final String MODE_FAST = "fast";

    private final Preferences node;

    /**
     * Wraps the given preference node used to read and write sampler state.
     *
     * @param node the backing preference node
     */
    public SamplerPreferences(Preferences node) {
        this.node = Objects.requireNonNull(node, "node");
    }

    /**
     * The user-level preference node under which the sampler stores its state.
     *
     * @return the default node
     */
    public static Preferences defaultNode() {
        return Preferences.userNodeForPackage(SamplerApp.class);
    }

    /**
     * Reads the persisted full-text search query.
     *
     * @return the query string, or empty when nothing was saved
     */
    public String query() {
        return node.get(KEY_QUERY, "");
    }

    /**
     * Persists the full-text search query.
     *
     * @param query the query string to save
     */
    public void saveQuery(String query) {
        node.put(KEY_QUERY, query == null ? "" : query);
    }

    /**
     * Reads the persisted active filter tag ids (e.g. {@code geom:boxplot}).
     * Tags no longer present in the catalogue are simply skipped by the caller.
     *
     * @return the saved tag ids, possibly empty
     */
    public List<String> tagIds() {
        var raw = node.get(KEY_TAGS, "");
        if (raw.isBlank()) {
            return List.of();
        }
        return List.of(raw.split(","));
    }

    /**
     * Persists the active filter tag ids as a comma-separated string.
     *
     * @param ids the tag ids to save
     */
    public void saveTagIds(List<String> ids) {
        node.put(KEY_TAGS, String.join(",", ids));
    }

    /**
     * Reads the persisted A/B render mode.
     *
     * @return the saved mode, or {@link RenderMode#FAST} when nothing was saved
     */
    public RenderMode renderMode() {
        return MODE_FULL.equalsIgnoreCase(node.get(KEY_RENDER_MODE, MODE_FAST))
                ? RenderMode.FULL
                : RenderMode.FAST;
    }

    /**
     * Persists the A/B render mode.
     *
     * @param mode the mode to save
     */
    public void saveRenderMode(RenderMode mode) {
        node.put(KEY_RENDER_MODE, Objects.equals(mode, RenderMode.FULL)
                ? MODE_FULL
                : MODE_FAST);
    }

    /**
     * Reads whether the source view should use the dark theme.
     *
     * @return {@code true} when the dark theme was saved, {@code false} for light
     */
    public boolean darkTheme() {
        return node.getBoolean(KEY_DARK_THEME, false);
    }

    /**
     * Persists whether the source view should use the dark theme.
     *
     * @param dark {@code true} for the dark theme, {@code false} for light
     */
    public void saveDarkTheme(boolean dark) {
        node.putBoolean(KEY_DARK_THEME, dark);
    }

    /**
     * Removes every key this class manages, restoring the state to defaults.
     */
    public void clear() {
        node.remove(KEY_QUERY);
        node.remove(KEY_TAGS);
        node.remove(KEY_RENDER_MODE);
        node.remove(KEY_DARK_THEME);
    }
}
