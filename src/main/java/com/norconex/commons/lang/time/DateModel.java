/* Copyright 2023 Norconex Inc.
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

import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.With;
import lombok.experimental.Accessors;

/**
 * <p>
 * Class abstracting several date and time implementations
 * ({@link Date}, {@link LocalDate}, {@link ZonedDateTime}, etc.).
 * The
 * </p>
 * <h3>Default values</h3>
 * When unspecified, the following are the default values:
 * <ul>
 *   <li>Time units: zero</li>
 *   <li>Zone ID: UTC</li>
 * </ul>
 *
 * <p>
 * When resolving a date with the specified time zone, it is set as the
 * new time zone for the same time units. For example, setting PST
 * as the time zone will change the time portion of a UTC date-time
 * from "13:34 UTC" to "13:34 PST").
 * </p>
 * <p>
 * Modifying a time unit by calling "set" or "with" methods on it does not
 * change any of the other time units. This includes the time zone. For
 * instance, setting "America/Vancouver" time zone on a UTC date will result
 * in the same clock time, but in a different time zone. E.g.,:
 * <code>2022-11-10T04:49:51.000Z</code> will become
 * <code>2022-11-10T04:49:51.000-08:00[America/Vancouver]</code>
 * </p>
 */
@Data
@Accessors(fluent = true)
@AllArgsConstructor(access = AccessLevel.PACKAGE)
@NoArgsConstructor(access = AccessLevel.PACKAGE)
@With
public final class DateModel {

    private int year;
    private int month;
    private int day;
    private int hour;
    private int minute;
    private int second;
    private int nano; // of seconds
    @NonNull
    private ZoneId zoneId = ZoneOffset.UTC;

    private boolean startOfDay;

    //--- Static constructors --------------------------------------------------

    public static DateModel of(
            int year, int month, int day, int hour, int minute, int second) {
        var db = new DateModel();
        db.year = year;
        db.month = month;
        db.day = day;
        db.hour = hour;
        db.minute = minute;
        db.second = second;
        return db;
    }

    public static DateModel of(int year, int month, int day) {
        return of(year, month, day, 0, 0, 0);
    }

    public static DateModel of(@NonNull ZonedDateTime zdt) {
        var db = new DateModel();
        db.year = zdt.getYear();
        db.month = zdt.getMonthValue();
        db.day = zdt.getDayOfMonth();
        db.hour = zdt.getHour();
        db.minute = zdt.getMinute();
        db.second = zdt.getSecond();
        db.nano = zdt.getNano();
        db.zoneId = zdt.getZone();
        return db;
    }

    public static DateModel of(@NonNull LocalDateTime ldt) {
        var db = new DateModel();
        db.year = ldt.getYear();
        db.month = ldt.getMonthValue();
        db.day = ldt.getDayOfMonth();
        db.hour = ldt.getHour();
        db.minute = ldt.getMinute();
        db.second = ldt.getSecond();
        db.nano = ldt.getNano();
        return db;
    }

    public static DateModel of(@NonNull LocalDate ld) {
        return of(ld.getYear(), ld.getMonthValue(), ld.getDayOfMonth());
    }

    public static DateModel of(@NonNull Instant instant) {
        return of(ZonedDateTime.ofInstant(instant, ZoneOffset.UTC));
    }

    public static DateModel of(long epoch) {
        return of(LocalDateTime.ofInstant(
                Instant.ofEpochMilli(epoch), ZoneOffset.UTC));
    }

    public static DateModel of(Date date) {
        return of(date.getTime());
    }

    /**
     * Create a date model out of a date string representation, in the format
     * defined by either {@link DateTimeFormatter#ISO_DATE_TIME} for date-time
     * or {@link DateTimeFormatter#ISO_DATE} for date-only. If the time zone
     * is not defined in the date string, UTC is assumed.
     * @param dateStr date string
     * @return date model
     * @throws DateTimeException if the date string could not be parsed
     */
    public static DateModel of(@NonNull String dateStr) {
        ZonedDateTime zdt = null;
        try {
            zdt = ZonedDateTimeParser.ofFormat(
                    dateStr, DateTimeFormatter.ISO_DATE_TIME, null);
        } catch (DateTimeException e) {
            // swallow
        }
        if (zdt == null) {
            zdt = ZonedDateTimeParser.ofFormat(
                    dateStr, DateTimeFormatter.ISO_DATE, null);
        }
        if (zdt == null) {
            throw new DateTimeException(
                    "Could not create %s out of date string: %s".formatted(
                            DateModel.class.getSimpleName(), dateStr));
        }
        return of(zdt);
    }
    /**
     * Create a date model out of a date string representation, based on
     * settings defined in the supplied {@link ZonedDateTimeParser}.
     * @param dateStr date string
     * @param parser the zoned date time parser
     * @return date model
     * @throws DateTimeException if the date string could not be parsed
     */
    public static DateModel of(
            @NonNull String dateStr, @NonNull ZonedDateTimeParser parser) {
        return of(parser.parse(dateStr));
    }

    //--- Accessors ------------------------------------------------------------

    // nano remainder is lost
    public DateModel withMillisecond(int millisecond) {
        return withNano(millisecond * 1_000_000);
    }

    public DateModel withDate(int year, int month, int day) {
        return withYear(year).withMonth(month).withDay(day);
    }

    public DateModel withTime(int hour, int minute, int second) {
        return withHour(hour).withMinute(minute).withSecond(second);
    }

    //--- Exporters ------------------------------------------------------------

    public Date toDate() {
        return new Date(toEpoch());
    }

    public long toEpoch() {
        return toZonedDateTime().toInstant().toEpochMilli();
    }

    public Instant toInstant() {
        return toZonedDateTime().toInstant();
    }

    public LocalDate toLocalDate() {
        return toLocalDateTime().toLocalDate();
    }

    public LocalDateTime toLocalDateTime() {
        return ZonedDateTime.ofInstant(toZonedDateTime().toInstant(), zoneId)
                .toLocalDateTime();
    }

    public ZonedDateTime toZonedDateTime() {
//        if (startOfDay) {
//            return ZonedDateTime.of(
//                    year, month, day, 0, 0, 0, 0, zoneId);
//        }
//        return ZonedDateTime.of(
//                year, month, day, hour, minute, second, nano, zoneId);
        var zdt = ZonedDateTime.of(
                year, month, day, hour, minute, second, nano, zoneId);
        if (startOfDay) {
            return zdt.truncatedTo(ChronoUnit.DAYS);
        }
        return zdt;
    }

    /**
     * Return the date as a string representation in the format
     * defined by {@link DateTimeFormatter#ISO_DATE_TIME}. If the time zone
     * is not defined, UTC is assumed.
     * @return date string
     * @throws DateTimeException if the date string could not be rendered as
     *     a string
     */
    @Override
    public String toString() {
        return DateTimeFormatter.ISO_DATE_TIME.format(toZonedDateTime());
    }
}
