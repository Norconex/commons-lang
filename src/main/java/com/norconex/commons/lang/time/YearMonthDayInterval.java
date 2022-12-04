/* Copyright 2010-2022 Norconex Inc.
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

import java.io.Serializable;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.Calendar;
import java.util.Date;

import org.apache.commons.lang3.StringUtils;

/**
 * An immutable date interval where both the start and end are inclusive.
 * The start YearMonthDay instance must be before or the same date as
 * as the end YearMonthDay. Start and end YearMonthDay instances cannot be
 * <code>null</code> and start date must be before or the same date as
 * the end date.
 * @since 1.3.0
 * @see Period
 */
public final class YearMonthDayInterval implements Serializable {

    private static final long serialVersionUID = -5607689446876900272L;

    private final YearMonthDay start;
    private final YearMonthDay end;

    public YearMonthDayInterval(YearMonthDay start, YearMonthDay end) {
        if (start == null || end == null) {
            throw new IllegalArgumentException(
                    "YearMonthday start and YearMonthday end cannot be null.");
        }
        if (start.isAfter(end)) {
            throw new IllegalArgumentException(
                    "YearMonthDay start cannot be after YearMonthDay end.");
        }
        this.start = start;
        this.end = end;
    }

    /**
     * Constructs a YearMonthDayInterval out of a string.  The recommended
     * string format is <code>yyyy-MM-dd - yyyy-MM-dd</code>, but any
     * characters in between the start and end are accepted as long as there
     * is a space after the start YearMontDay and before the end YearMonthDay.
     * @param interval the interval to parse
     */
    public YearMonthDayInterval(String interval) {
        String trimmed = StringUtils.trim(interval);
        String startStr = StringUtils.substringBefore(trimmed, " ");
        String endStr = StringUtils.substringAfterLast(trimmed, " ");
        if (StringUtils.isBlank(startStr) || StringUtils.isBlank(endStr)) {
            throw new IllegalArgumentException(
                    "String YearMonthDay interval cannot be null or empty.");
        }
        start = new YearMonthDay(startStr);
        end = new YearMonthDay(endStr);
    }
    public YearMonthDayInterval(Date start, Date end) {
        if (start == null || end == null) {
            throw new IllegalArgumentException(
                    "Date start and Date end cannot be null.");
        }
        this.start = new YearMonthDay(start);
        this.end = new YearMonthDay(end);
    }
    public YearMonthDayInterval(Calendar start, Calendar end) {
        if (start == null || end == null) {
            throw new IllegalArgumentException(
                    "Calendar start and Calendar end cannot be null.");
        }
        this.start = new YearMonthDay(start);
        this.end = new YearMonthDay(end);
    }
    public YearMonthDay getStart() {
        return start;
    }
    public Date getStartDate() {
        return start.toDate();
    }
    public YearMonthDay getEnd() {
        return end;
    }
    public Date getEndDate() {
        return end.toDate();
    }
    /**
     * Gets the end date as midnight the day after to ensure all dates on the
     * same day as this YearMonthDay are smaller than this returned
     * exclusive date.  Useful for date range comparisons where typically
     * the end date is exclusive.
     * @return midnight past the end date
     */
    public Date getEndDateEndOfDay() {
        return end.toEndOfDayDate();
    }

    /**
     * Whether the YearMonthDay falls between this interval
     * (inclusive endpoints).
     * @param ymd the YearMonthDay
     * @return <code>true</code> if YearMonthDay is included in this interval
     */
    public boolean contains(YearMonthDay ymd) {
        long millis = ymd.toMillis();
        return millis >= start.toMillis() && millis <= end.toMillis();
    }
    /**
     * Whether the date falls between this interval
     * (inclusive endpoints).
     * @param date a date
     * @return <code>true</code> if the date is included in this interval
     */
    public boolean contains(Date date) {
        long millis = date.getTime();
        return millis >= start.toMillis() && millis <= end.toMillis();
    }

    /**
     * Gets the number of years between start and end dates, rounded down.
     * @return number of years
     */
    public int getYears() {
        return toPeriod().getYears();
    }
    /**
     * Gets the number of months between start and end dates, rounded down.
     * @return number of months
     */
    public int getMonths() {
        return (int) toPeriod().toTotalMonths();
    }
    /**
     * Gets the number of days between start and end dates, rounded down.
     * @return number of days
     */
    public int getDays() {
        return (int) ChronoUnit.DAYS.between(
                start.toLocalDate(), end.toLocalDate());
    }

    /**
     * Get a period equivalent to this YearMonthDay.
     * @return period
     * @since 3.0.0
     */
    public Period toPeriod() {
        return Period.between(start.toLocalDate(), end.toLocalDate());
    }

    /**
     * Gets the interval as a string of this format:
     *  <code>yyyy-MM-dd - yyyy-MM-dd</code>;
     *  @return interval as string
     */
    @Override
    public String toString() {
        return start + " - " + end;
    }
}
