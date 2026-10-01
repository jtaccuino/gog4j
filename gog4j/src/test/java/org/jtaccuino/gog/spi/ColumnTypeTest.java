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
package org.jtaccuino.gog.spi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import org.junit.jupiter.api.Test;

/**
 * Verifies the column taxonomy: the timestamp family, the continuous flags the
 * axes and the matrix dispatch read, and the single-value classifier the
 * raw-value path shares.
 */
class ColumnTypeTest {

    @Test
    void timestampsCoverEveryJavaDateTimeType() {
        var when = LocalDateTime.of(2010, 1, 1, 12, 0);
        assertEquals(DataExtractor.ColumnType.TIMESTAMP,
                DataExtractor.ColumnType.ofValue(when.toInstant(ZoneOffset.UTC)));
        assertEquals(DataExtractor.ColumnType.TIMESTAMP,
                DataExtractor.ColumnType.ofValue(when));
        assertEquals(DataExtractor.ColumnType.TIMESTAMP,
                DataExtractor.ColumnType.ofValue(when.atOffset(ZoneOffset.UTC)));
        assertEquals(DataExtractor.ColumnType.TIMESTAMP,
                DataExtractor.ColumnType.ofValue(when.atZone(ZoneOffset.UTC)));
    }

    @Test
    void aLocalDateIsADateNotATimestamp() {
        assertEquals(DataExtractor.ColumnType.DATE,
                DataExtractor.ColumnType.ofValue(LocalDate.of(2010, 1, 1)));
    }

    @Test
    void numbersAndNumericStringsAreNumbers() {
        assertEquals(DataExtractor.ColumnType.NUMBER, DataExtractor.ColumnType.ofValue(1.5));
        assertEquals(DataExtractor.ColumnType.NUMBER, DataExtractor.ColumnType.ofValue(3));
        assertEquals(DataExtractor.ColumnType.NUMBER, DataExtractor.ColumnType.ofValue("42.5"));
    }

    @Test
    void aNonNumericStringIsText() {
        assertEquals(DataExtractor.ColumnType.TEXT, DataExtractor.ColumnType.ofValue("Adelie"));
    }

    @Test
    void aNullValueIsText() {
        assertEquals(DataExtractor.ColumnType.TEXT, DataExtractor.ColumnType.ofValue(null));
    }

    @Test
    void temporalTypesAreContinuousSoTheyPlotOnAxes() {
        assertTrue(DataExtractor.ColumnType.isContinuous(DataExtractor.ColumnType.NUMBER));
        assertTrue(DataExtractor.ColumnType.isContinuous(DataExtractor.ColumnType.DATE));
        assertTrue(DataExtractor.ColumnType.isContinuous(DataExtractor.ColumnType.TIMESTAMP));
        assertFalse(DataExtractor.ColumnType.isContinuous(DataExtractor.ColumnType.TEXT));
    }

    @Test
    void onlyDateAndTimestampAreTemporal() {
        assertTrue(DataExtractor.ColumnType.isTemporal(DataExtractor.ColumnType.DATE));
        assertTrue(DataExtractor.ColumnType.isTemporal(DataExtractor.ColumnType.TIMESTAMP));
        assertFalse(DataExtractor.ColumnType.isTemporal(DataExtractor.ColumnType.NUMBER));
        assertFalse(DataExtractor.ColumnType.isTemporal(DataExtractor.ColumnType.TEXT));
    }

    @Test
    void aSchemaTypeMapsByNameAndFallsBackToNumberOrText() {
        assertEquals(DataExtractor.ColumnType.TEXT, DataExtractor.ColumnType.of(String.class));
        assertEquals(DataExtractor.ColumnType.NUMBER, DataExtractor.ColumnType.of(Integer.class));
        assertEquals(DataExtractor.ColumnType.TIMESTAMP, DataExtractor.ColumnType.of(Instant.class));
        assertEquals(DataExtractor.ColumnType.DATE, DataExtractor.ColumnType.of(LocalDate.class));
        // An unlisted class that is not a Number has no better answer than text.
        assertEquals(DataExtractor.ColumnType.TEXT, DataExtractor.ColumnType.of(Object.class));
        assertNull(DataExtractor.ColumnType.of(null));
    }
}
