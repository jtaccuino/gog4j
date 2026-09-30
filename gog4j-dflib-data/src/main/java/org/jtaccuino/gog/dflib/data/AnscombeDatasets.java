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

import org.dflib.DataFrame;
import org.dflib.Series;

/**
 * Provides Anscombe's quartet as a long-format DFLib DataFrame
 * with raw bounds; the fixed axis limits live on the example plots.
 */
public class AnscombeDatasets {

    /** Utility class; not meant to be instantiated. */
    private AnscombeDatasets() {
    }

    /**
     * Returns Anscombe's quartet in long format with columns {@code x}, {@code y}, and {@code quartet}.
     *
     * @return a long-format {@link DataFrame} of Anscombe's quartet
     */
    public static DataFrame getQuartetData() {
        return DataFrame.byColumn("x", "y", "quartet").of(
            Series.ofDouble(
                10.0, 8.0, 13.0, 9.0, 11.0, 14.0, 6.0, 4.0, 12.0, 7.0, 5.0,  // I
                10.0, 8.0, 13.0, 9.0, 11.0, 14.0, 6.0, 4.0, 12.0, 7.0, 5.0,  // II
                10.0, 8.0, 13.0, 9.0, 11.0, 14.0, 6.0, 4.0, 12.0, 7.0, 5.0,  // III
                8.0,  8.0, 8.0,  8.0, 8.0,  8.0,  8.0, 19.0, 8.0,  7.0, 8.0   // IV
            ),
            Series.ofDouble(
                8.04, 6.95, 7.58, 8.81, 8.33, 9.96, 7.24, 4.26, 10.84, 4.82, 5.68,
                9.14, 8.14, 8.74, 8.77, 9.26, 8.10, 6.13, 3.10, 9.13,  7.26, 4.74,
                7.46, 6.77, 12.74, 7.11, 7.81, 8.84, 6.08, 5.39, 8.15,  6.42, 5.73,
                6.58, 5.76, 7.71,  8.84, 8.47, 7.04, 5.25, 12.50, 5.56, 7.91, 6.89
            ),
            Series.of(
                "I", "I", "I", "I", "I", "I", "I", "I", "I", "I", "I",
                "II", "II", "II", "II", "II", "II", "II", "II", "II", "II", "II",
                "III", "III", "III", "III", "III", "III", "III", "III", "III", "III", "III",
                "IV", "IV", "IV", "IV", "IV", "IV", "IV", "IV", "IV", "IV", "IV"
            )
        );
    }
}
