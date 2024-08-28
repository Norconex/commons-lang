/* Copyright 2018-2022 Norconex Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.norconex.commons.lang.time;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Date;

/**
 * Date-related utility methods.
 * @since 2.0.0
 * @deprecated Use more flexible {@link DateModel} instead.
 */
@Deprecated(since = "3.0.0")
public final class DateUtil { //NOSONAR

    private DateUtil() {
    }

    /**
     * Converts a {@link Date} to a {@link LocalDate}, local to the system
     * default {@link ZoneId}.
     * @param date to convert
     * @return converted date
     */
    public static LocalDate toLocalDate(Date date) {
        return toLocalDate(date, ZoneId.systemDefault());
    }

    /**
     * Converts a {@link Date} to a {@link LocalDate}, local to the specified
     * {@link ZoneId}.
     * @param date to convert
     * @param zoneId zone id
     * @return converted date
     */
    public static LocalDate toLocalDate(Date date, ZoneId zoneId) {
        return date.toInstant().atZone(zoneId).toLocalDate();
    }

    /**
     * Converts a {@link Date} to a {@link LocalDate}, local to
     * {@link ZoneOffset#UTC} (Greenwich).
     * @param date to convert
     * @return converted date
     * @since 3.0.0
     */
    public static LocalDate toLocalDateUTC(Date date) {
        return date.toInstant().atZone(ZoneOffset.UTC).toLocalDate();
    }

    /**
     * Converts a millisecond EPOCH date to a {@link LocalDate}, local to the
     * system default {@link ZoneId}.
     * @param date to convert
     * @return converted date
     */
    public static LocalDate toLocalDate(long date) {
        return toLocalDate(date, ZoneId.systemDefault());
    }

    /**
     * Converts a millisecond EPOCH date to a {@link LocalDate}, local to the
     * specified {@link ZoneId}.
     * @param date to convert
     * @param zoneId zone id
     * @return converted date
     */
    public static LocalDate toLocalDate(long date, ZoneId zoneId) {
        return toLocalDateTime(date, zoneId).toLocalDate();
    }

    /**
     * Converts a millisecond EPOCH date to a {@link LocalDate}, local to
     * {@link ZoneOffset#UTC} (Greenwich).
     * @param date to convert
     * @return converted date
     * @since 3.0.0
     */
    public static LocalDate toLocalDateUTC(long date) {
        return toLocalDate(date, ZoneOffset.UTC);
    }

    /**
     * Converts a {@link Date} to a {@link LocalDateTime}, local to the
     * system default {@link ZoneId}.
     * @param date to convert
     * @return converted date
     */
    public static LocalDateTime toLocalDateTime(Date date) {
        return toLocalDateTime(date, ZoneId.systemDefault());
    }

    /**
     * Converts a {@link Date} to a {@link LocalDateTime}, local to the
     * specified {@link ZoneId}.
     * @param date to convert
     * @param zoneId zone id
     * @return converted date
     */
    public static LocalDateTime toLocalDateTime(Date date, ZoneId zoneId) {
        return date.toInstant().atZone(zoneId).toLocalDateTime();
    }

    /**
     * Converts a {@link Date} to a {@link LocalDateTime}, local to
     * {@link ZoneOffset#UTC} (Greenwich).
     * @param date to convert
     * @return converted date
     * @since 3.0.0
     */
    public static LocalDateTime toLocalDateTimeUTC(Date date) {
        return toLocalDateTime(date, ZoneOffset.UTC);
    }

    /**
     * Converts a millisecond EPOCH date to a {@link LocalDateTime}, local to
     * the system default {@link ZoneId}.
     * @param date to convert
     * @return converted date
     */
    public static LocalDateTime toLocalDateTime(long date) {
        return toLocalDateTime(date, ZoneId.systemDefault());
    }

    /**
     * Converts a millisecond EPOCH date to a {@link LocalDateTime}, local to
     * the specified {@link ZoneId}.
     * @param date to convert
     * @param zoneId zone id
     * @return converted date
     */
    public static LocalDateTime toLocalDateTime(long date, ZoneId zoneId) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(date), zoneId);
    }

    /**
     * Converts a millisecond EPOCH date to a {@link LocalDateTime}, local to
     * {@link ZoneOffset#UTC} (Greenwich).
     * @param date to convert
     * @return converted date
     * @since 3.0.0
     */
    public static LocalDateTime toLocalDateTimeUTC(long date) {
        return toLocalDateTime(date, ZoneOffset.UTC);
    }

    /**
     * Converts a {@link LocalDate} to a {@link Date} using the system
     * default {@link ZoneId}.
     * @param date to convert
     * @return converted date
     */
    public static Date toDate(LocalDate date) {
        return toDate(date, ZoneId.systemDefault());
    }

    /**
     * Converts a {@link LocalDate} to a {@link Date} using the specified
     * {@link ZoneId}.
     * @param date date to convert
     * @param zoneId zone id
     * @return converted date
     */
    public static Date toDate(LocalDate date, ZoneId zoneId) {
        return Date.from(date.atStartOfDay().atZone(zoneId).toInstant());
    }

    /**
     * Converts a {@link LocalDate} to a {@link Date}
     * using {@link ZoneOffset#UTC} (Greenwich).
     * @param date to convert
     * @return converted date
     * @since 3.0.0
     */
    public static Date toDateUTC(LocalDate date) {
        return toDate(date, ZoneOffset.UTC);
    }

    /**
     * Converts a {@link LocalDateTime} to a {@link Date} using the system
     * default {@link ZoneId}.
     * @param date to convert
     * @return converted date
     */
    public static Date toDate(LocalDateTime date) {
        return toDate(date, ZoneId.systemDefault());
    }

    /**
     * Converts a {@link LocalDateTime} to a {@link Date} using the specified
     * {@link ZoneId}.
     * @param date date to convert
     * @param zoneId zone id
     * @return converted date
     */
    public static Date toDate(LocalDateTime date, ZoneId zoneId) {
        return Date.from(date.atZone(zoneId).toInstant());
    }

    /**
     * Converts a {@link LocalDateTime} to a {@link Date}
     * using {@link ZoneOffset#UTC} (Greenwich).
     * @param date to convert
     * @return converted date
     * @since 3.0.0
     */
    public static Date toDateUTC(LocalDateTime date) {
        return toDate(date, ZoneOffset.UTC);
    }

    /**
     * Converts an {@link Instant} to a {@link Date} using the system
     * default {@link ZoneId}.
     * @param instant to convert
     * @return converted date
     */
    public static Date toDate(Instant instant) {
        return toDate(instant, ZoneId.systemDefault());
    }

    /**
     * Converts an {@link Instant} to a {@link Date} using the specified
     * {@link ZoneId}.
     * @param instant date to convert
     * @param zoneId zone id
     * @return converted date
     */
    public static Date toDate(Instant instant, ZoneId zoneId) {
        return Date.from(instant
                .atZone(ZoneOffset.UTC)
                .withZoneSameLocal(zoneId)
                .toInstant());
    }

    /**
     * Converts an {@link Instant} to a {@link Date}
     * using {@link ZoneOffset#UTC} (Greenwich).
     * @param instant to convert
     * @return converted date
     * @since 3.0.0
     */
    public static Date toDateUTC(Instant instant) {
        return toDate(instant, ZoneOffset.UTC);
    }
}
