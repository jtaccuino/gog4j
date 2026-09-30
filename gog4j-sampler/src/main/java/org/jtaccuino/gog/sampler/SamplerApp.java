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
import javafx.application.Application;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import org.jtaccuino.gog.RenderMode;
import org.jtaccuino.gog.controls.SourceTheme;
import org.jtaccuino.gog.sampler.registry.ExampleRegistry;
import org.jtaccuino.gog.sampler.registry.SamplerExample;
import org.jtaccuino.gog.sampler.registry.Tag;
import org.jtaccuino.gog.sampler.registry.Tags;
import org.jtaccuino.gog.sampler.ui.FilterBar;
import org.jtaccuino.gog.sampler.ui.NoSelectionModel;
import org.jtaccuino.gog.sampler.ui.SampleListCell;

/**
 * Interactive sampler application that presents all registered example plots
 * in a single vertical flow: each filtered example shows its title, tags, and
 * description followed by its rendered plot. A full-text search plus active
 * tag chips filter the examples.
 */
public class SamplerApp extends Application {

    private final SamplerPreferences preferences;
    private FilterBar filterBar;
    private BorderPane root;
    @SuppressWarnings("UnusedVariable") // the strong end of the weak subscription
    private Runnable themeSubscription;

    /**
     * Creates the application with the default user preference node; JavaFX
     * instantiates it via the no-arg constructor.
     */
    public SamplerApp() {
        this(new SamplerPreferences(SamplerPreferences.defaultNode()));
    }

    /**
     * Creates the application bound to an explicit preference node (tests pass
     * a throwaway node so the real user preferences stay untouched).
     *
     * @param preferences the preference store to read and save browsing state
     */
    public SamplerApp(SamplerPreferences preferences) {
        this.preferences = preferences;
    }

    /** The filter bar this application wired, for tests. */
    FilterBar filterBar() {
        return filterBar;
    }

    @Override
    public void start(Stage primaryStage) {
        var all = FXCollections.observableArrayList(ExampleRegistry.all());
        var usedTags = Tags.usedBy(all);
        filterBar = new FilterBar(usedTags);
        restorePreferences(filterBar, usedTags);
        // Persist any change back to the preferences store; the app restores
        // them on the next launch, so a session's browsing state carries over.
        filterBar.queryProperty().addListener((obs, oldQ, query) ->
                preferences.saveQuery(query));
        filterBar.activeTagsProperty().addListener(
                (javafx.collections.ListChangeListener<Tag>) change ->
                        preferences.saveTagIds(filterBar.activeTagsProperty()
                                .stream().map(Tag::name).toList()));
        filterBar.renderModeProperty().addListener((obs, oldMode, mode) ->
                preferences.saveRenderMode(mode));
        // A dark theme selection applies to every source drawer at once through
        // the shared SourceTheme.CURRENT, and is remembered for next launch.
        filterBar.themeProperty().addListener((obs, oldTheme, theme) ->
                preferences.saveDarkTheme(theme == SourceTheme.DARK));

        var filtered = new FilteredList<SamplerExample>(all);
        filtered.predicateProperty().bind(Bindings.createObjectBinding(
                () -> example -> SamplerQuery.matches(example,
                        filterBar.queryProperty().get(),
                        filterBar.activeTagsProperty()),
                filterBar.queryProperty(),
                filterBar.activeTagsProperty()));
        var sorted = new SortedList<>(filtered);
        filterBar.bindResultCount(Bindings.size(filtered), Bindings.size(all));

        // A virtualized ListView only creates cells for the visible rows, so
        // only a handful of plots are ever built at once regardless of how many
        // examples match the filters.
        var list = new ListView<SamplerExample>(sorted);
        // Each card embeds its own source drawer behind a nudge handle at the
        // bottom of the card, so no window-level drawer is needed.
        list.setCellFactory(view -> new SampleListCell(
                tag -> filterBar.activeTagsProperty().setAll(List.of(tag)),
                filterBar.renderModeProperty()));
        var placeholder = new Label("No examples match the current filters.");
        placeholder.getStyleClass().add("sampler-placeholder");
        list.setPlaceholder(placeholder);
        // No master/detail selection here; a no-op selection model keeps clicks
        // from driving the selection machinery against a changing items list.
        list.setSelectionModel(new NoSelectionModel<>());

        var main = new BorderPane();
        main.getStyleClass().add("sampler-root");
        main.setTop(filterBar);
        main.setCenter(list);

        var scene = new Scene(main, 1280, 900);
        // The base sampler.css ships with the cards/filter bar; the dark sheet
        // is enabled by dropping the sampler-dark class onto the root.
        scene.getStylesheets().addAll(
                getClass().getResource("/org/jtaccuino/gog/controls/sampler.css").toExternalForm(),
                getClass().getResource("/org/jtaccuino/gog/controls/sampler-dark.css").toExternalForm());
        root = main;
        // The subscription is held by a field so the weak end inside the
        // static theme property can never pin a closed sampler window.
        themeSubscription = SourceTheme.subscribe(theme -> applyThemeClassToRoot());
        applyThemeClassToRoot();
        primaryStage.setTitle("Gog4jSampler \u2014 Grammar of Graphics Showcase");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void applyThemeClassToRoot() {
        if (SourceTheme.CURRENT.get().isDark()) {
            root.getStyleClass().add("sampler-dark");
        } else {
            root.getStyleClass().remove("sampler-dark");
        }
    }

    /**
     * Re-applies the persisted browsing state to a freshly built filter bar:
     * search query, active filter tags (skipping any whose catalogue entry no
     * longer exists), the A/B render mode, and the light/dark source theme.
     * Restoring the mode here happens before the first cards are created, so
     * cells pick the saved mode up on their initial install rather than
     * re-rendering everything once more.
     *
     * @param bar the filter bar to populate
     * @param usedTags the tags present on at least one example
     */
    private void restorePreferences(FilterBar bar, List<Tag> usedTags) {
        var query = preferences.query();
        if (!query.isBlank()) {
            bar.queryProperty().set(query);
        }
        var usedById = new java.util.HashMap<String, Tag>();
        for (var tag : usedTags) {
            usedById.put(tag.name(), tag);
        }
        var restored = preferences.tagIds().stream()
                .map(usedById::get)
                .filter(Objects::nonNull)
                .toList();
        if (!restored.isEmpty()) {
            bar.activeTagsProperty().setAll(restored);
        }
        bar.renderModeProperty().set(preferences.renderMode());
        bar.themeProperty().set(preferences.darkTheme()
                ? SourceTheme.DARK
                : SourceTheme.LIGHT);
    }

    /**
     * Launches the sampler JavaFX application.
     *
     * @param args command-line arguments passed to {@link Application#launch}
     */
    public static void main(String[] args) {
        launch(args);
    }
}
