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
package org.jtaccuino.gog.benchmark;

import java.util.concurrent.CountDownLatch;
import javafx.application.Platform;
import jdk.jfr.Recording;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolver;

/**
 * JUnit 5 extension backing {@link MeasurePhases}: brings up the headless JavaFX toolkit once,
 * creates a fresh {@link Recording} and {@link PhaseTimings} around each annotated test method,
 * injects the {@link PhaseTimings} parameter, and prints the phase report after the method runs.
 */
public class JfrBenchmarkExtension implements BeforeAllCallback, BeforeTestExecutionCallback,
        AfterTestExecutionCallback, ParameterResolver {

    private static final String TIMINGS_KEY = "phaseTimings";

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        var latch = new CountDownLatch(1);
        try {
            Platform.startup(latch::countDown);
            latch.await();
        } catch (IllegalStateException alreadyRunning) {
            // Toolkit already brought up by an earlier test in this JVM.
        }
    }

    @Override
    public void beforeTestExecution(ExtensionContext context) throws Exception {
        var method = context.getRequiredTestMethod();
        var anno = method.getAnnotation(MeasurePhases.class);
        if (anno == null) {
            return;
        }
        var label = anno.label().isEmpty() ? method.getName() : anno.label();
        var timings = new PhaseTimings(new Recording(), label, anno.warmup(), anno.measured());
        context.getStore(ExtensionContext.Namespace.create(JfrBenchmarkExtension.class, method))
                .put(TIMINGS_KEY, timings);
    }

    @Override
    public void afterTestExecution(ExtensionContext context) throws Exception {
        var timings = (PhaseTimings) context.getStore(
                ExtensionContext.Namespace.create(JfrBenchmarkExtension.class, context.getRequiredTestMethod()))
                .get(TIMINGS_KEY);
        if (timings != null) {
            timings.report();
        }
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext context) {
        return context.getRequiredTestMethod().isAnnotationPresent(MeasurePhases.class)
                && parameterContext.getParameter().getType() == PhaseTimings.class;
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext context) {
        return context.getStore(
                ExtensionContext.Namespace.create(JfrBenchmarkExtension.class, context.getRequiredTestMethod()))
                .get(TIMINGS_KEY);
    }
}
