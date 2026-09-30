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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import javafx.application.Platform;
import org.jtaccuino.gog.sampler.meta.TagKind;
import org.jtaccuino.gog.sampler.registry.ExampleRegistry;
import org.jtaccuino.gog.sampler.registry.SamplerExample;
import org.jtaccuino.gog.sampler.registry.Tag;
import org.jtaccuino.gog.sampler.registry.Tags;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ExampleRegistryTest {

    @BeforeAll
    static void startToolkit() {
        try {
            var latch = new CountDownLatch(1);
            Platform.startup(latch::countDown);
            assumeTrue(latch.await(30, TimeUnit.SECONDS), "JavaFX toolkit did not start");
        } catch (IllegalStateException alreadyRunning) {
            // Toolkit already up from another test class in this JVM.
        } catch (UnsupportedOperationException | InterruptedException noToolkit) {
            assumeTrue(false, "No JavaFX toolkit available: " + noToolkit.getMessage());
        }
    }

    @Test
    void loadsTheAnnotatedExamples() {
        var examples = ExampleRegistry.all();
        // One factory method is annotated per example across the demo classes.
        assertTrue(examples.size() >= 150, "expected the full example catalogue, got " + examples.size());
    }

    @Test
    void everyExampleCarriesExactlyOneDatasetTag() {
        var examples = ExampleRegistry.all();
        assertFalse(examples.isEmpty());

        for (var example : examples) {
            var datasetCount = count(example, tag -> tag.kind() == TagKind.DATASET);
            assertEquals(1, datasetCount,
                    () -> example + " should have exactly one DATASET tag, had " + datasetCount);
        }
    }

    @Test
    void everyExampleCarriesAtLeastOneNonDatasetTag() {
        var examples = ExampleRegistry.all();
        assertFalse(examples.isEmpty());

        for (var example : examples) {
            var otherCount = count(example, tag -> tag.kind() != TagKind.DATASET);
            assertTrue(otherCount >= 1,
                    () -> example + " should carry at least one geom/stat/coordinate tag, had " + otherCount);
        }
    }

    @Test
    void everyExampleHasNonBlankFactorySourceBakedIn() {
        for (var example : ExampleRegistry.all()) {
            assertFalse(example.source().isBlank(),
                    () -> example + " source must be baked into the registry");
            assertTrue(example.source().contains(example.methodName()),
                    () -> example + " baked source must contain the factory method");
        }
    }

    @Test
    void everyExampleCarriesADescription() {
        for (var example : ExampleRegistry.all()) {
            assertFalse(example.description() == null || example.description().isBlank(),
                    () -> example + " should carry a one-line description");
        }
    }

    @Test
    void tagIdsResolveAgainstTheCatalog() {
        for (var example : ExampleRegistry.all()) {
            for (var tag : example.tags()) {
                assertTrue(tag.kind() != null, () -> example + " tag " + tag + " must have a kind");
                assertEquals(tag, Tags.byName(tag.name()),
                        () -> example + " tag id " + tag.name() + " must round-trip through Tags.byName");
            }
        }
    }

    @Test
    void everyExampleProvidesAResolvableFactory() {
        for (var example : ExampleRegistry.all()) {
            assertNotNull(example.factory(), () -> example + " factory must not be null");
        }
    }

    @Test
    void factoriesInvokeToBuildPlots() {
        var examples = ExampleRegistry.all();
        // Exercise a spread of factories across classes and return types
        // (Plot<DataFrame> and Plot<EmptyDataFrame>) to confirm the recorded
        // class/method names resolve reflectively at runtime.
        var sample = java.util.stream.IntStream.range(0, examples.size())
                .filter(i -> i % 7 == 0)
                .mapToObj(examples::get)
                .toList();
        assertFalse(sample.isEmpty());
        for (var example : sample) {
            assertNotNull(example.factory().get(), () -> example + " factory must build a plot");
        }
    }

    @Test
    void usedTagsExcludeUnusedCatalogConstants() {
        var examples = ExampleRegistry.all();
        var used = Tags.usedBy(examples);
        assertFalse(used.isEmpty(), "used-tag list must not be empty");
        // The filter UI must never suggest a tag no example carries (it would
        // always filter the list down to zero results).
        for (var tag : used) {
            assertTrue(examples.stream().anyMatch(e -> e.tags().contains(tag)),
                    () -> tag.name() + " is suggested but no example carries it");
        }
        // Known-unused catalogue constants must not be offered.
        for (var id : new String[] {"geom:none", "geom:linerange", "geom:errorbarh",
                "theme:gray", "guide:placement-default", "guide:placement-right"}) {
            assertTrue(used.stream().noneMatch(t -> t.name().equals(id)),
                    () -> id + " is unused by every example and must not be offered as a filter");
        }
        // usedBy is a strict subset of the full catalogue.
        var allIds = Tags.all();
        assertTrue(used.size() < allIds.size(),
                "used tags should be a strict subset of the catalogue ("
                        + used.size() + " of " + allIds.size() + ")");
    }

    private static int count(SamplerExample example, Predicate<Tag> test) {
        return (int) example.tags().stream().filter(test).count();
    }
}
