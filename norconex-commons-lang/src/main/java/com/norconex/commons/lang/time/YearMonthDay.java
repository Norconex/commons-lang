/* Copyright 2010-2014 Norconex Inc.
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
import java.util.Calendar;
import java.util.Date;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.lang3.time.DateUtils;

/**
 * Immutable class holding a specific date made only of the 
 * year, month, and day.  Unlike Java {@link Calendar}, the months are starting
 * at 1 (January).
 * @author Pascal Essiembre
 * @since 1.3.0
 */
//TODO check if Java LocalDate offers the same now.
public final class YearMonthDay 
        implements Comparable<YearMonthDay>, Serializable {

    private static final long serialVersionUID = -2844519358776099395L;

    private final int year;
    private final int month;
    private final int day;
    
    /**
     * Constructs a YearMonthDay with the current date.
     */
    public YearMonthDay() {
        this(Calendar.getInstance());
    }
    /**
     * Constructs a YearMonthDay with the specified values.
     * @param year the year
     * @param month the month
     * @param day the day
     */
    public YearMonthDay(int year, int month, int day) {
        super();
        this.year = year;
        this.month = month;
        this.day = day;
    }
    /**
     * Constructs a YearMonthDay with the specified year and month.
     * The day is set to 1.
     * @param year the year
     * @param month the month
     */
    public YearMonthDay(int year, int month) {
        this(year, month, 1);
    }
    /**
     * Constructs a YearMonthDay with the specified year.
     * The day and the months are both set to 1.
     * @param year the year
     */
    public YearMonthDay(int year) {
        this(year, 1, 1);
    }
    /**
     * Constructs a YearMonthDay from a {@link Date}.
     * @param date a date
     */
    public YearMonthDay(Date date) {
        this(DateUtils.toCalendar(date));
    }
    /**
     * Constructs a YearMonthDay from a {@link Calendar}.
     * @param calendar a calendar instant
     */
    public YearMonthDay(Calendar calendar) {
        year = calendar.get(Calendar.YEAR);
        month = calendar.get(Calendar.MONTH) + 1;
        day = calendar.get(Calendar.DAY_OF_MONTH);
    }
    /**
     * Constructs a YearMonthDay from a string of this format: 
     * <code>yyyy-MM-dd</code>.
     * @param date a date string
     */
    public YearMonthDay(String date) {
        this(Integer.valueOf(StringUtils.substringBefore(date, "-")),
             Integer.valueOf(StringUtils.substringBetween(date, "-")),
             Integer.valueOf(StringUtils.substringAfterLast(date, "-")));
    }
    
    /**
     * Gets the year.
     * @return the year
     */
    public int getYear() {
        return year;
    }
    /**
     * Gets the month.
     * @return the month
     */
    public int getMonth() {
        return month;
    }
    /**
     * Gets the day.
     * @return the day
     */
    public int getDay() {
        return day;
    }

    /**
     * Whether this YearMonthDay represents a date before the given
     * YearMonthDay.
     * @param ymd the YearMonthDay to evaluate
     * @return <code>true</code> if before
     */
    public boolean isBefore(YearMonthDay ymd) {
        return compareTo(ymd) < 0;
    }
    /**
     * Whether this YearMonthDay represents a date before the given
     * date.
     * @param date the date to evaluate
     * @return <code>true</code> if before
     */
    public boolean isBeforeDate(Date date) {
        return isBefore(new YearMonthDay(date));
    }
    
    /**
     * Whether this YearMonthDay represents a date after the given
     * YearMonthDay.
     * @param ymd the YearMonthDay to evaluate
     * @return <code>true</code> if after
     */
    public boolean isAfter(YearMonthDay ymd) {
        return compareTo(ymd) > 0;
    }
    /**
     * Whether this YearMonthDay represents a date after the given
     * date.
     * @param date the date to evaluate
     * @return <code>true</code> if after
     */
    public boolean isAfterDate(Date date) {
        return isAfter(new YearMonthDay(date));
    }
    
    /**
     * Whether this YearmMonthDay contains the given {@link Date} (i.e. same 
     * year, month, and day).
     * @param date date to evaluate
     * @return <code>true</code> if date is contained
     */
    public boolean contains(Date date) {
        return equals(new YearMonthDay(date));
    }    
    /**
     * Converts this YearMonthDay to a {@link Date} at midnight.
     * @return a date
     */
    public Date toDate() {
        return toCalendar().getTime();
    }
    /**
     * Converts this YearMonthDay to the current time as 
     * UTC milliseconds from the epoch.
     * @return milliseconds
     */
    public long toMillis() {
        return toCalendar().getTimeInMillis();
    }
    /**
     * Gets the date as midnight the day after to represent the end of the 
     * day. This ensures all dates on the
     * same day as this YearMonthDay are smaller than this returned 
     * exclusive date.  Useful for date range comparisons where typically
     * the end date is exclusive.
     * @return midnight past the end date 
     */
    public Date toEndOfDayDate() {
        Calendar cal = toCalendar();
        cal.add(Calendar.DAY_OF_MONTH, 1);
        return cal.getTime();
    }
    
    /**
     * Adds a number of days to a new created YearMonthDay.  For subtractions,
     * user a negative integer.
     * @param numOfDays number of days
     * @return a new YearMonthDay instance
     */
    public YearMonthDay addDays(int numOfDays) {
        Calendar cal = toCalendar();
        cal.add(Calendar.DAY_OF_MONTH, numOfDays);
        return new YearMonthDay(cal.getTime());
    }
    /**
     * Adds a number of months to a new created YearMonthDay.  For subtractions,
     * user a negative integer.
     * @param numOfMonths number of months
     * @return a new YearMonthDay instance
     */
    public YearMonthDay addMonths(int numOfMonths) {
        Calendar cal = toCalendar();
        cal.add(Calendar.MONTH, numOfMonths);
        return new YearMonthDay(cal.getTime());
    }
    /**
     * Adds a number of years to a new created YearMonthDay.  For subtractions,
     * user a negative integer.
     * @param numOfYears number of years
     * @return a new YearMonthDay instance
     */
    public YearMonthDay addYears(int numOfYears) {
        Calendar cal = toCalendar();
        cal.add(Calendar.YEAR, numOfYears);
        return new YearMonthDay(cal.getTime());
    }
    
    /**
     * Converts this YearMonthDay to a {@link Calendar} at midnight.
     * @return a calendar
     */
    public Calendar toCalendar() {
        Calendar cal = Calendar.getInstance();
        cal.set(year, month -1, day);
        cal = DateUtils.truncate(cal, Calendar.DAY_OF_MONTH);
        return cal;
    }

    @Override
    public int compareTo(YearMonthDay ymd) {
        if (ymd == null) {
            return -1;
        }
        int val = Integer.valueOf(year).compareTo(ymd.year);
        if (val == 0) {
            val = Integer.valueOf(month).compareTo(ymd.month);
        }
        if (val == 0) {
            val = Integer.valueOf(day).compareTo(ymd.day);
        }
        return val;
    }
    
    @Override
    public boolean equals(final Object other) {
        return EqualsBuilder.reflectionEquals(this, other);
    }
    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }

    /**
     * Converts the YearMonthDay to a string of this format: 
     * <code>yyyy-MM-dd</code>.
     */
    @Override
    public String toString() {
        return new StringBuilder()
                .append(year)
                .append('-')
                .append(StringUtils.leftPad(Integer.toString(month), 2, '0'))
                .append('-')
                .append(StringUtils.leftPad(Integer.toString(day), 2, '0'))
                .toString();
    }
    public String toString(String pattern) {
        return DateFormatUtils.formatUTC(toDate(), pattern);
    }
}
