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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

/**
 * Verifies the shared value coercion: a value's numeric position must be the
 * same one its axis uses, so a label and a tick can never disagree.
 */
class ValuesTest {

    @Test
    void numbersKeepTheirDoubleValue() {
        assertEquals(3.5, Values.toDouble(3.5));
        assertEquals(3.0, Values.toDouble(3));
    }

    @Test
    void aDateIsItsEpochDayAndATimestampItsEpochMilli() {
        var date = LocalDate.of(2010, 1, 1);
        assertEquals((double) date.toEpochDay(), Values.toDouble(date));

        var dateTime = LocalDateTime.of(2010, 1, 1, 12, 0);
        assertEquals((double) dateTime.toInstant(ZoneOffset.UTC).toEpochMilli(),
                Values.toDouble(dateTime));
        assertEquals(Values.toDouble(dateTime), Values.toDouble(dateTime.atZone(ZoneOffset.UTC).toInstant()));
    }

    @Test
    void aNumericStringIsParsed() {
        assertEquals(42.0, Values.toDouble("42"));
    }

    @Test
    void anUnparseableStringFallsBack() {
        assertEquals(0.0, Values.toDouble("Adelie"));
        assertEquals(-1.0, Values.toDouble("Adelie", -1.0));
        assertNull(Values.toDoubleOrNull("Adelie"));
    }

    @Test
    void aNullValueIsZeroOrNull() {
        assertEquals(0.0, Values.toDouble(null));
        assertNull(Values.toDoubleOrNull(null));
    }

    @Test
    void labelsReadTheSameTemporalValueThePositionEncodes() {
        assertEquals("2010-01-01", Values.label(LocalDate.of(2010, 1, 1)));
        assertEquals("2010-01-01 12:30", Values.label(LocalDateTime.of(2010, 1, 1, 12, 30)));
        assertEquals("2010-01-01 12:30:45",
                Values.label(LocalDateTime.of(2010, 1, 1, 12, 30, 45)));
        assertEquals("2010-01-01 12:30",
                Values.label(Instant.parse("2010-01-01T12:30:00Z")));
    }

    @Test
    void labelsFormatNumbersWithoutSpuriousDecimals() {
        assertEquals("42", Values.label(42.0));
        assertEquals("3.50", Values.label(3.5));
        assertEquals("null", Values.label(null));
        assertEquals("Adelie", Values.label("Adelie"));
    }

    @Test
    void positionAndLabelAgreeForATimestamp() {
        var value = LocalDateTime.of(2010, 6, 15, 9, 30);
        var position = Values.toDouble(value);
        assertTrue(position > 0 && Double.isFinite(position));
        assertEquals("2010-06-15 09:30", Values.label(value));
    }
}
