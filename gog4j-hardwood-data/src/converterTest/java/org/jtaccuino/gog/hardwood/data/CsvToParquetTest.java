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
package org.jtaccuino.gog.hardwood.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

/**
 * Verifies the converter's date-time reading rules: what counts as a calendar
 * date against a date-time, and that a cell naming an instant keeps that
 * instant rather than being flattened to a wall-clock reading.
 */
class CsvToParquetTest {

    @Test
    void aBareCalendarDateIsADateNotADateTime() {
        assertTrue(CsvToParquet.parsesAsDate("2010-01-01"));
        assertFalse(CsvToParquet.parsesAsDate("2010-01-01T01:00:00"));
        assertFalse(CsvToParquet.parsesAsDate("10"));
        assertFalse(CsvToParquet.parsesAsDate("Adelie"));
    }

    @Test
    void aTimeOfDayMakesItADateTime() {
        assertTrue(CsvToParquet.parsesAsDateTime("2010-01-01T01:00:00"));
        assertTrue(CsvToParquet.parsesAsDateTime("2010-01-01T01:00:00Z"));
        assertTrue(CsvToParquet.parsesAsDateTime("2010-01-01T01:00:00+02:00"));
        assertFalse(CsvToParquet.parsesAsDateTime("2010-01-01"));
        assertFalse(CsvToParquet.parsesAsDateTime("42.5"));
    }

    @Test
    void aZoneLessReadingIsTakenAsUtc() {
        assertEquals((double) LocalDateTime.of(2010, 1, 1, 0, 0)
                .toInstant(ZoneOffset.UTC).toEpochMilli(),
                (double) CsvToParquet.toEpochMillis("2010-01-01T00:00:00"));
    }

    @Test
    void aReadingWithAnOffsetKeepsItsInstant() {
        // 01:00+02:00 is 23:00 the previous day in UTC, so the offset must not
        // simply be dropped: that would shift the instant by two hours.
        var utc = LocalDateTime.of(2009, 12, 31, 23, 0)
                .toInstant(ZoneOffset.UTC).toEpochMilli();
        assertEquals((double) utc, (double) CsvToParquet.toEpochMillis("2010-01-01T01:00:00+02:00"));
        assertEquals((double) LocalDateTime.of(2010, 1, 1, 1, 0)
                .toInstant(ZoneOffset.UTC).toEpochMilli(),
                (double) CsvToParquet.toEpochMillis("2010-01-01T01:00:00Z"));
    }
}
