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

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;

/**
 * The shared arithmetic of temporal column values: converting a raw
 * {@link java.time} value into the numeric data position an axis maps, and
 * choosing readable breaks and labels for that axis.
 *
 * <p>Two positions are in play, and they differ because the two temporal column
 * types differ: a {@link LocalDate} is stored as its <em>epoch day</em> and a
 * date-time value as its <em>epoch millisecond</em>. Both sit comfortably
 * inside a {@code double} (epoch millis reach the year 294000), so telling the
 * two apart costs no precision.
 *
 * <p>Date-time values without a zone ({@link LocalDateTime}) are read as UTC
 * rather than in the machine's default zone. A machine-dependent position would
 * make the same data land differently on different machines, which would in
 * turn make the canvas-versus-SVG rendering tests non-deterministic.
 *
 * <p>The Cartesian and polar coordinates both own tick generation and label
 * formatting, so they both resolve their breaks here rather than each growing
 * its own ladder.
 */
public final class Temporals {

    /** Milliseconds in a day, the bridge between the epoch-day and epoch-millisecond positions. */
    public static final double MILLIS_PER_DAY = 86_400_000.0;

    /**
     * One rung of the break ladder: a step size, the calendar unit it advances
     * by, the granularity its labels read at, and how to snap a position onto it.
     *
     * @param unit        the calendar unit to advance by
     * @param count       how many {@code unit}s per break
     * @param granularity the granularity the labels take
     * @param align       snaps a date-time onto the step's grid
     * @param millis      the approximate step length, used only to fit a step to a span
     */
    private record Step(ChronoUnit unit, long count, Granularity granularity,
                        UnaryOperator<LocalDateTime> align, double millis) {
    }

    private static final double MINUTE_MILLIS = 60_000.0;
    private static final double HOUR_MILLIS = 60 * MINUTE_MILLIS;
    private static final double DAY_MILLIS = 24 * HOUR_MILLIS;
    private static final double MONTH_MILLIS = 30 * DAY_MILLIS;
    private static final double YEAR_MILLIS = 365 * DAY_MILLIS;

    /**
     * The break steps a timestamp axis can use, finest first. Ordered so the
     * first entry satisfying the wanted density is also the coarsest one that
     * does, which is what makes the walk in {@link #stepFor} a single pass.
     */
    private static final List<Step> LADDER = List.of(
            second(1, 1000.0), second(5, 5 * 1000.0), second(15, 15 * 1000.0), second(30, 30 * 1000.0),
            minute(1, MINUTE_MILLIS), minute(5, 5 * MINUTE_MILLIS),
            minute(15, 15 * MINUTE_MILLIS), minute(30, 30 * MINUTE_MILLIS),
            hour(1, HOUR_MILLIS), hour(3, 3 * HOUR_MILLIS),
            hour(6, 6 * HOUR_MILLIS), hour(12, 12 * HOUR_MILLIS),
            new Step(ChronoUnit.DAYS, 1, Granularity.DAY, t -> t.truncatedTo(ChronoUnit.DAYS), DAY_MILLIS),
            new Step(ChronoUnit.DAYS, 2, Granularity.DAY, t -> t.truncatedTo(ChronoUnit.DAYS), 2 * DAY_MILLIS),
            new Step(ChronoUnit.DAYS, 7, Granularity.DAY, t -> t.truncatedTo(ChronoUnit.DAYS), 7 * DAY_MILLIS),
            new Step(ChronoUnit.DAYS, 14, Granularity.DAY, t -> t.truncatedTo(ChronoUnit.DAYS), 14 * DAY_MILLIS),
            month(1, MONTH_MILLIS), month(3, 3 * MONTH_MILLIS),
            month(6, 6 * MONTH_MILLIS), month(12, YEAR_MILLIS),
            new Step(ChronoUnit.YEARS, 1, Granularity.YEAR, Temporals::monthStart, YEAR_MILLIS),
            new Step(ChronoUnit.YEARS, 2, Granularity.YEAR, Temporals::monthStart, 2 * YEAR_MILLIS),
            new Step(ChronoUnit.YEARS, 10, Granularity.YEAR, Temporals::monthStart, 10 * YEAR_MILLIS),
            new Step(ChronoUnit.YEARS, 25, Granularity.YEAR, Temporals::monthStart, 25 * YEAR_MILLIS));

    private static Step second(int count, double millis) {
        return new Step(ChronoUnit.SECONDS, count, Granularity.SECOND,
                t -> t.truncatedTo(ChronoUnit.SECONDS), millis);
    }

    private static Step minute(int count, double millis) {
        return new Step(ChronoUnit.MINUTES, count, Granularity.MINUTE,
                t -> t.truncatedTo(ChronoUnit.MINUTES), millis);
    }

    private static Step hour(int count, double millis) {
        return new Step(ChronoUnit.HOURS, count, Granularity.HOUR,
                t -> t.truncatedTo(ChronoUnit.HOURS), millis);
    }

    private static Step month(int count, double millis) {
        return new Step(ChronoUnit.MONTHS, count, Granularity.MONTH, Temporals::monthStart, millis);
    }

    /** {@return the start of the month containing the given date-time} */
    private static LocalDateTime monthStart(LocalDateTime t) {
        return LocalDateTime.of(t.toLocalDate().withDayOfMonth(1), LocalTime.MIDNIGHT);
    }

    /**
     * The step a time axis breaks at, coarsest first. A date axis only ever
     * resolves to {@link #YEAR}, {@link #MONTH} or {@link #DAY}; only a
     * timestamp axis descends to the intraday steps.
     */
    public enum Granularity {
        /** Breaks span years, so a label reads the bare year. */
        YEAR("yyyy"),
        /** Breaks span months, so a label reads the year and month. */
        MONTH("yyyy-MM"),
        /** Breaks span days, so a label reads the date. */
        DAY("yyyy-MM-dd"),
        /** Breaks span hours, so a label reads the date and hour. */
        HOUR("yyyy-MM-dd HH"),
        /** Breaks span minutes, so a label reads the date, hour and minute. */
        MINUTE("yyyy-MM-dd HH:mm"),
        /** Breaks span seconds, so a label reads the full date and time. */
        SECOND("yyyy-MM-dd HH:mm:ss");

        private final DateTimeFormatter labelFormat;

        Granularity(String pattern) {
            this.labelFormat = DateTimeFormatter.ofPattern(pattern);
        }

        /**
         * Whether a label at this granularity carries a time of day.
         *
         * @return {@code true} when the label includes an hour, minute, or second
         */
        public boolean hasTimeOfDay() {
            return this == HOUR || this == MINUTE || this == SECOND;
        }
    }

    private Temporals() {
    }

    /**
     * Whether the value is one of the date-time types the engine reads as a
     * timestamp column.
     *
     * @param value a raw column value, may be {@code null}
     * @return {@code true} when the value carries a time of day
     */
    public static boolean isTimestamp(Object value) {
        return value instanceof Instant || value instanceof OffsetDateTime
                || value instanceof ZonedDateTime || value instanceof LocalDateTime;
    }

    /**
     * The numeric data position of a date-time value, in epoch milliseconds
     * since 1970-01-01T00:00:00Z.
     *
     * @param value the raw column value
     * @return the epoch-millisecond position
     * @throws IllegalArgumentException when the value carries no time of day
     */
    public static double toEpochMillis(Object value) {
        if (value instanceof Instant i) {
            return i.toEpochMilli();
        }
        if (value instanceof OffsetDateTime odt) {
            return odt.toInstant().toEpochMilli();
        }
        if (value instanceof ZonedDateTime zdt) {
            return zdt.toInstant().toEpochMilli();
        }
        if (value instanceof LocalDateTime ldt) {
            return ldt.toInstant(ZoneOffset.UTC).toEpochMilli();
        }
        throw new IllegalArgumentException("not a date-time value: " + value);
    }

    /**
     * The instant at an epoch-millisecond position.
     *
     * @param position the position, in epoch milliseconds
     * @return the instant
     */
    public static Instant instantAt(double position) {
        return Instant.ofEpochMilli(Math.round(position));
    }

    /**
     * The UTC date-time at an epoch-millisecond position.
     *
     * @param position the position, in epoch milliseconds
     * @return the date-time
     */
    public static LocalDateTime dateTimeAt(double position) {
        return LocalDateTime.ofInstant(instantAt(position), ZoneOffset.UTC);
    }

    /**
     * The date at an epoch-day position.
     *
     * @param epochDay the position, in epoch days
     * @return the date
     */
    public static LocalDate dateAt(double epochDay) {
        return LocalDate.ofEpochDay(Math.round(epochDay));
    }

    /**
     * The coarsest granularity that still yields at least {@code target} breaks
     * across the span, so a panel covering an afternoon does not print a tick
     * every second and one covering a decade does not print a tick a minute.
     *
     * @param minMillis the lower bound of the span, in epoch milliseconds
     * @param maxMillis the upper bound of the span, in epoch milliseconds
     * @param target    the wanted number of breaks
     * @return the granularity to break at
     */
    public static Granularity granularityOf(double minMillis, double maxMillis, int target) {
        return stepFor(spanOf(minMillis, maxMillis), target).granularity();
    }

    /**
     * The year-based breaks for a date axis: whole years at a step that adapts
     * to the span (1, 2, 10, 20 or 25 years). Positions are epoch days, and
     * only breaks inside the requested range are returned.
     *
     * @param minData the lower bound of the range, in epoch days
     * @param maxData the upper bound of the range, in epoch days
     * @return the break positions, never empty
     */
    public static List<Double> dateTicks(double minData, double maxData) {
        var ticks = new ArrayList<Double>();
        var startYear = dateAt(minData).getYear();
        var endYear = dateAt(maxData).getYear();

        int yearStep = 10;
        if (endYear - startYear > 100) {
            yearStep = 25;
        } else if (endYear - startYear > 40) {
            yearStep = 20;
        } else if (endYear - startYear > 10) {
            yearStep = 2;
        } else {
            yearStep = 1;
        }

        var currentYear = (startYear / yearStep) * yearStep;
        if (currentYear < startYear) {
            currentYear += yearStep;
        }

        while (currentYear <= endYear) {
            var epochDay = (double) LocalDate.of(currentYear, 1, 1).toEpochDay();
            if (epochDay >= minData && epochDay <= maxData) {
                ticks.add(epochDay);
            }
            currentYear += yearStep;
        }

        if (ticks.isEmpty()) {
            ticks.add(minData);
        }
        return ticks;
    }

    /**
     * The breaks for a timestamp axis: aligned positions at a step the span
     * affords, so an afternoon breaks by quarter hours, a year by days, and a
     * decade by months. Positions are epoch milliseconds.
     *
     * @param minData the lower bound of the range, in epoch milliseconds
     * @param maxData the upper bound of the range, in epoch milliseconds
     * @param target  the wanted number of breaks
     * @return the break positions, never empty
     */
    public static List<Double> timestampTicks(double minData, double maxData, int target) {
        double lo = Math.min(minData, maxData);
        double hi = Math.max(minData, maxData);
        var step = stepFor(spanOf(lo, hi), target);

        List<Double> ticks;
        if (step.granularity() == Granularity.YEAR) {
            // The year ladder already lives in epoch days; reuse it rather than
            // restating the same year-step table here.
            ticks = new ArrayList<>();
            for (var day : dateTicks(lo / MILLIS_PER_DAY, hi / MILLIS_PER_DAY)) {
                ticks.add(day * MILLIS_PER_DAY);
            }
        } else {
            ticks = alignedTicks(dateTimeAt(hi), lo, hi, step);
        }

        if (ticks.isEmpty()) {
            ticks.add(lo);
        }
        return ticks;
    }

    /**
     * The label for a break on a date axis: the bare year, matching how a
     * year-only date axis reads.
     *
     * @param epochDay the break position, in epoch days
     * @return the label text
     */
    public static String dateLabel(double epochDay) {
        return String.valueOf(dateAt(epochDay).getYear());
    }

    /**
     * The label for a break on a timestamp axis, at the granularity the axis
     * breaks at: a bare year when the steps span years, the date from month
     * granularity down, and the date and time from hour granularity down.
     *
     * @param position    the break position, in epoch milliseconds
     * @param granularity the granularity the axis breaks at
     * @return the label text
     */
    public static String timestampLabel(double position, Granularity granularity) {
        return granularity.labelFormat.format(dateTimeAt(position));
    }

    /**
     * The label for a single raw date-time value, for tooltips and legends.
     * Seconds are dropped when they are zero, the common case for data already
     * rounded to the minute or hour.
     *
     * @param value a raw date-time value
     * @return the display string
     * @throws IllegalArgumentException when the value carries no time of day
     */
    public static String label(Object value) {
        var dateTime = dateTimeAt(toEpochMillis(value));
        var pattern = dateTime.getSecond() == 0 && dateTime.getNano() == 0
                ? "yyyy-MM-dd HH:mm"
                : "yyyy-MM-dd HH:mm:ss";
        return DateTimeFormatter.ofPattern(pattern).format(dateTime);
    }

    /** The absolute width of a span. */
    private static double spanOf(double minMillis, double maxMillis) {
        return Math.abs(maxMillis - minMillis);
    }

    /**
     * The break step to use for a span: the coarsest entry of {@link #LADDER}
     * that still affords {@code target} breaks, falling back to the finest when
     * even that is too coarse.
     *
     * <p>The ladder is the single source of truth for both the step and the
     * granularity the labels take. Choosing a granularity by span thresholds and
     * a step count separately is what let a six-hour span end up on one-minute
     * breaks: the thresholds named {@code HOUR} while the count table was free
     * to answer {@code 1}. The walk runs coarsest-first so it settles on the
     * coarsest step that still affords the wanted density.
     *
     * @param spanMillis the width of the span, in epoch milliseconds
     * @param target     the wanted number of breaks
     * @return the step to break at
     */
    private static Step stepFor(double spanMillis, int target) {
        int wanted = Math.max(1, target);
        for (int i = LADDER.size() - 1; i >= 0; i--) {
            var step = LADDER.get(i);
            if (step.millis() * wanted <= spanMillis) {
                return step;
            }
        }
        return LADDER.get(0);
    }

    /**
     * Walks from the step's aligned start to the end of the range, collecting
     * the positions that fall inside the requested bounds.
     *
     * @param to   the end of the range, in date-time form
     * @param lo   the lower bound of the range, in epoch milliseconds
     * @param hi   the upper bound of the range, in epoch milliseconds
     * @param step the step to break at
     * @return the break positions inside the range
     */
    private static List<Double> alignedTicks(LocalDateTime to, double lo, double hi, Step step) {
        var ticks = new ArrayList<Double>();
        var start = step.align().apply(dateTimeAt(lo));
        for (var t = start; !t.isAfter(to); t = t.plus(step.count(), step.unit())) {
            double position = t.toInstant(ZoneOffset.UTC).toEpochMilli();
            if (position >= lo && position <= hi) {
                ticks.add(position);
            }
        }
        return ticks;
    }
}
