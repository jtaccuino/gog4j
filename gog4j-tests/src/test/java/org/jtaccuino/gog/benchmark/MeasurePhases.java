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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a JUnit 5 test method as a JFR benchmark. A {@link PhaseTimings} parameter is
 * injected into the test body and {@link PhaseTimings#measure(PhaseTimings.ThrowingRunnable)}
 * runs the warmup passes (unrecorded) followed by the measured passes (recorded); the
 * extension prints a best/avg/p95 report per phase after the method returns.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface MeasurePhases {

    int warmup() default 5;

    int measured() default 20;

    String label() default "";
}
