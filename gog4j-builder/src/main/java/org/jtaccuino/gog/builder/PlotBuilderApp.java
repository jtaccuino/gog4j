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
package org.jtaccuino.gog.builder;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Standalone launcher for the {@link PlotBuilder} control, wiring in the
 * built-in dflib dataset catalogue.
 * <p>
 * Run via {@code ./gradlew runBuilder}.
 */
public class PlotBuilderApp extends Application {

    @Override
    public void start(Stage stage) {
        var builder = new PlotBuilder();
        builder.setDatasets(DflibDatasets.all());
        var scene = new Scene(builder, 1240, 720);
        stage.setTitle("Gog4j Plot Builder");
        stage.setScene(scene);
        stage.show();
    }

    /** Entry point for {@code ./gradlew runBuilder}. */
    public static void main(String[] args) {
        Application.launch(args);
    }
}
