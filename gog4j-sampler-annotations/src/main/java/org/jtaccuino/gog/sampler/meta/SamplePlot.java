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
package org.jtaccuino.gog.sampler.meta;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks one example factory method (a {@code public static Plot<?>} method) so
 * the annotation processor can register it in the generated sampler registry.
 * <p>
 * Every method carries a {@link #title} and a required {@link #dataset}; the
 * remaining tag members are per-kind enum arrays whose non-default elements
 * document the API constructs the example demonstrates. The processor builds
 * the full tag list from these enums; no class-level marker or per-method
 * number is needed.
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
public @interface SamplePlot {

    /** The short display title, e.g. {@code "Notched Boxplot"}. */
    String title();

    /** An optional one-line description; empty means no description. */
    String description() default "";

    /** The {@link SampleDataset} constant for the data source used. */
    SampleDataset dataset();

    /** The geom layers the example renders. */
    SampleGeom[] geoms() default {};

    /** The statistical transformations the example applies. */
    SampleStat[] stats() default {};

    /** The coordinate systems the example uses. */
    SampleCoord[] coords() default {};

    /** The scale configurations the example uses. */
    SampleScale[] scales() default {};

    /** The faceting strategies the example uses. */
    SampleFacet[] facets() default {};

    /** The position adjustments the example uses. */
    SamplePosition[] positions() default {};

    /** The plot themes the example uses. */
    SampleTheme[] themes() default {};

    /** The guide/legend placement or layout strategies. */
    SampleGuide[] guides() default {};

    /** Cross-cutting capabilities the example exercises. */
    SampleFeature[] features() default {};
}
