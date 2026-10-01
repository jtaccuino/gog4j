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
package org.jtaccuino.gog.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Tests the temporal arithmetic: value positions, the granularity ladder, and
 * the breaks and labels derived from them.
 */
class TemporalsTest {

    @Test
    void isTimestampCoversTheDateTimeTypes() {
        assertTrue(Temporals.isTimestamp(Instant.EPOCH));
        assertTrue(Temporals.isTimestamp(LocalDateTime.of(2010, 1, 1, 1, 0)));
        assertTrue(Temporals.isTimestamp(OffsetDateTime.of(2010, 1, 1, 1, 0, 0, 0, ZoneOffset.UTC)));
        assertTrue(Temporals.isTimestamp(ZonedDateTime.of(2010, 1, 1, 1, 0, 0, 0, ZoneOffset.UTC)));
    }

    @Test
    void isTimestampRejectsDatesAndNumbers() {
        assertFalse(Temporals.isTimestamp(LocalDate.of(2010, 1, 1)));
        assertFalse(Temporals.isTimestamp(1.0));
        assertFalse(Temporals.isTimestamp("2010-01-01T01:00:00"));
        assertFalse(Temporals.isTimestamp(null));
    }

    @Test
    void toEpochMillisReadsZonedValuesAsInstants() {
        var instant = Instant.parse("2010-01-01T01:00:00Z");
        assertEquals((double) instant.toEpochMilli(), Temporals.toEpochMillis(instant));
        assertEquals((double) instant.toEpochMilli(),
                Temporals.toEpochMillis(OffsetDateTime.of(2010, 1, 1, 1, 0, 0, 0, ZoneOffset.UTC)));
        assertEquals((double) instant.toEpochMilli(),
                Temporals.toEpochMillis(ZonedDateTime.of(2010, 1, 1, 1, 0, 0, 0, ZoneOffset.UTC)));
    }

    @Test
    void toEpochMillisReadsZoneLessValuesAsUtcRatherThanTheMachineZone() {
        // A machine-dependent reading would make the same data render differently
        // per machine and would break the canvas/SVG parity tests.
        var local = LocalDateTime.of(2010, 1, 1, 1, 0);
        assertEquals((double) local.toInstant(ZoneOffset.UTC).toEpochMilli(), Temporals.toEpochMillis(local));
    }

    @Test
    void toEpochMillisRejectsValuesWithoutATimeOfDay() {
        var thrown = assertThrows(IllegalArgumentException.class,
                () -> Temporals.toEpochMillis(LocalDate.of(2010, 1, 1)));
        assertTrue(thrown.getMessage().contains("not a date-time"));
    }

    @Test
    void positionsRoundTripThroughTheDateTimeConversions() {
        var instant = Instant.parse("2010-06-15T12:30:45Z");
        assertEquals(instant, Temporals.instantAt(Temporals.toEpochMillis(instant)));
        assertEquals(LocalDateTime.of(2010, 6, 15, 12, 30, 45), Temporals.dateTimeAt((double) instant.toEpochMilli()));
    }

    @Test
    void dateAtReadsAnEpochDayPosition() {
        assertEquals(LocalDate.of(2010, 1, 1), Temporals.dateAt((double) LocalDate.of(2010, 1, 1).toEpochDay()));
    }

    @Test
    void granularityFollowsTheSpan() {
        var year = LocalDateTime.of(2010, 1, 1, 0, 0);
        assertEquals(Temporals.Granularity.YEAR, Temporals.granularityOf(millis(year), millis(year.plusYears(10)), 5));
        assertEquals(Temporals.Granularity.MONTH, Temporals.granularityOf(millis(year), millis(year.plusYears(2)), 5));
        assertEquals(Temporals.Granularity.DAY, Temporals.granularityOf(millis(year), millis(year.plusMonths(3)), 5));
        assertEquals(Temporals.Granularity.HOUR, Temporals.granularityOf(millis(year), millis(year.plusDays(2)), 5));
        assertEquals(Temporals.Granularity.MINUTE, Temporals.granularityOf(millis(year), millis(year.plusHours(3)), 5));
        // A five-minute span affords one break a minute, so it stops at MINUTE
        // rather than dropping to seconds; only a shorter span reaches SECOND.
        assertEquals(Temporals.Granularity.MINUTE, Temporals.granularityOf(millis(year), millis(year.plusMinutes(5)), 5));
        assertEquals(Temporals.Granularity.SECOND, Temporals.granularityOf(millis(year), millis(year.plusMinutes(2)), 5));
    }

    @Test
    void hasTimeOfDayOnlyBelowDayGranularity() {
        assertFalse(Temporals.Granularity.YEAR.hasTimeOfDay());
        assertFalse(Temporals.Granularity.MONTH.hasTimeOfDay());
        assertFalse(Temporals.Granularity.DAY.hasTimeOfDay());
        assertTrue(Temporals.Granularity.HOUR.hasTimeOfDay());
        assertTrue(Temporals.Granularity.MINUTE.hasTimeOfDay());
        assertTrue(Temporals.Granularity.SECOND.hasTimeOfDay());
    }

    @Test
    void dateTicksBreakOnWholeYearsInsideTheRange() {
        var min = (double) LocalDate.of(2000, 6, 1).toEpochDay();
        var max = (double) LocalDate.of(2010, 6, 1).toEpochDay();
        var ticks = Temporals.dateTicks(min, max);
        // Positions are epoch days, so compare the years they decode to.
        var years = ticks.stream().map(Temporals::dateAt).map(LocalDate::getYear).toList();
        assertEquals(List.of(2001, 2002, 2003, 2004, 2005, 2006, 2007, 2008, 2009, 2010), years);
    }

    @Test
    void dateTicksWidenTheYearStepForLongSpans() {
        var min = (double) LocalDate.of(1900, 1, 1).toEpochDay();
        var max = (double) LocalDate.of(2010, 1, 1).toEpochDay();
        var ticks = Temporals.dateTicks(min, max);
        // A 110-year span steps 25 years, so the breaks are quarter-century marks.
        var years = ticks.stream().map(Temporals::dateAt).map(LocalDate::getYear).toList();
        assertEquals(List.of(1900, 1925, 1950, 1975, 2000), years);
        assertEquals("1925", Temporals.dateLabel(ticks.get(1)));
    }

    @Test
    void dateTicksFallBackToTheBoundWhenNoYearFits() {
        // A span narrower than one year between two mid-year dates has no
        // January 1 inside it.
        var min = (double) LocalDate.of(2010, 6, 1).toEpochDay();
        var max = (double) LocalDate.of(2010, 9, 1).toEpochDay();
        assertEquals(List.of(min), Temporals.dateTicks(min, max));
    }

    @Test
    void timestampTicksLandOnTheLadderStepForTheSpan() {
        // A five-hour span with five wanted breaks affords exactly one break an
        // hour, so the step and the label granularity agree. An offset start
        // would trim the span below that and drop the step to thirty minutes.
        var from = millis(LocalDateTime.of(2010, 1, 1, 9, 0));
        var to = millis(LocalDateTime.of(2010, 1, 1, 14, 0));
        assertEquals(Temporals.Granularity.HOUR, Temporals.granularityOf(from, to, 5));
        var ticks = Temporals.timestampTicks(from, to, 5);
        assertTrue(ticks.size() >= 2, "expected several breaks, got " + ticks);
        for (var tick : ticks) {
            var at = Temporals.dateTimeAt(tick);
            assertEquals(0, at.getMinute(), "expected a whole-hour break, got " + at);
            assertEquals(0, at.getSecond());
        }
        assertTrue(ticks.get(0) >= from && ticks.get(ticks.size() - 1) <= to);
    }

    @Test
    void timestampTicksLandOnWholeMinutesForAShortSpan() {
        // A sub-hour span cannot afford hourly steps, so it drops to minutes.
        var from = millis(LocalDateTime.of(2010, 1, 1, 9, 1));
        var to = millis(LocalDateTime.of(2010, 1, 1, 9, 59));
        assertEquals(Temporals.Granularity.MINUTE, Temporals.granularityOf(from, to, 5));
        var ticks = Temporals.timestampTicks(from, to, 5);
        assertTrue(ticks.size() >= 2, "expected several breaks, got " + ticks);
        for (var tick : ticks) {
            var at = Temporals.dateTimeAt(tick);
            assertEquals(0, at.getSecond(), "expected a whole-minute break, got " + at);
        }
    }

    @Test
    void timestampTicksBreakByMonthAcrossAYear() {
        var from = millis(LocalDateTime.of(2010, 1, 1, 0, 0));
        var to = millis(LocalDateTime.of(2010, 12, 31, 23, 0));
        var ticks = Temporals.timestampTicks(from, to, 5);
        assertTrue(ticks.size() >= 2);
        for (var tick : ticks) {
            var at = Temporals.dateTimeAt(tick);
            assertEquals(1, at.getDayOfMonth(), "expected a month start, got " + at);
            assertEquals(0, at.getHour());
        }
    }

    @Test
    void timestampTicksHandleAReversedRange() {
        var from = millis(LocalDateTime.of(2010, 1, 1, 0, 0));
        var to = millis(LocalDateTime.of(2010, 1, 1, 6, 0));
        assertEquals(Temporals.timestampTicks(from, to, 5), Temporals.timestampTicks(to, from, 5));
    }

    @Test
    void timestampTicksFallBackToTheLowerBoundForADegenerateSpan() {
        var at = millis(LocalDateTime.of(2010, 1, 1, 9, 7));
        var ticks = Temporals.timestampTicks(at, at, 5);
        assertFalse(ticks.isEmpty());
        assertEquals(at, ticks.get(0));
    }

    @Test
    void timestampLabelReadsAtTheAxisGranularity() {
        var position = millis(LocalDateTime.of(2010, 3, 4, 5, 6, 7));
        assertEquals("2010", Temporals.timestampLabel(position, Temporals.Granularity.YEAR));
        assertEquals("2010-03", Temporals.timestampLabel(position, Temporals.Granularity.MONTH));
        assertEquals("2010-03-04", Temporals.timestampLabel(position, Temporals.Granularity.DAY));
        assertEquals("2010-03-04 05", Temporals.timestampLabel(position, Temporals.Granularity.HOUR));
        assertEquals("2010-03-04 05:06", Temporals.timestampLabel(position, Temporals.Granularity.MINUTE));
        assertEquals("2010-03-04 05:06:07", Temporals.timestampLabel(position, Temporals.Granularity.SECOND));
    }

    @Test
    void labelDropsZeroSecondsButKeepsRealOnes() {
        assertEquals("2010-01-01 01:00", Temporals.label(LocalDateTime.of(2010, 1, 1, 1, 0)));
        assertEquals("2010-01-01 01:00:30", Temporals.label(LocalDateTime.of(2010, 1, 1, 1, 0, 30)));
        assertEquals("2010-01-01 01:00", Temporals.label(Instant.parse("2010-01-01T01:00:00Z")));
    }

    @Test
    void millisPerDayBridgesTheTwoTemporalPositions() {
        var local = LocalDateTime.of(2010, 1, 1, 0, 0);
        assertEquals((double) local.toInstant(ZoneOffset.UTC).toEpochMilli(),
                LocalDate.of(2010, 1, 1).toEpochDay() * Temporals.MILLIS_PER_DAY);
    }

    private static double millis(LocalDateTime dateTime) {
        return dateTime.toInstant(ZoneOffset.UTC).toEpochMilli();
    }
}
