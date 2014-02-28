/* Copyright 2010-2013 Norconex Inc.
 * 
 * This file is part of Norconex Commons Lang.
 * 
 * Norconex Commons Lang is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Norconex Commons Lang is distributed in the hope that it will be useful, 
 * but WITHOUT ANY WARRANTY; without even the implied warranty of 
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with Norconex Commons Lang. If not, see <http://www.gnu.org/licenses/>.
 */
package com.norconex.commons.lang.time;

import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;

/**
 * An immutable date interval where both the start and end are inclusive, unless
 * stated otherwise.  Start and end YearMonthDay instances cannot be
 * <code>null</code> and start date must be before or the same date as 
 * the end date.
 * @author Pascal Essiembre
 * @since 1.3.0
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
        this.start = new YearMonthDay(startStr);
        this.end = new YearMonthDay(endStr);
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
        if (start.isAfter(ymd)) {
            return false;
        }
        if (end.isBefore(ymd)) {
            return false;
        }
        return true;
    }
    /**
     * Whether the date falls between this interval 
     * (inclusive endpoints).
     * @param date a date
     * @return <code>true</code> if the date is included in this interval
     */
    public boolean contains(Date date) {
        Date startDate = start.toDate();
        Date endDate = end.toDate();
        if (startDate.after(date)) {
            return false;
        }
        if (endDate.before(date)) {
            return false;
        }
        return true;
    }

    /**
     * Gets the number of years between start and end dates, rounded down.
     * @return number of years
     */
    public int getYears() {
        int years = end.getYear() - start.getYear();
        if (end.getMonth() < start.getMonth()) {
            return years - 1;
        }
        if (end.getMonth() == start.getMonth()
                && end.getDay() < start.getDay()) {
            return years - 1;
        }
        return years;
    }
    /**
     * Gets the number of months between start and end dates, rounded down.
     * @return number of months
     */
    public int getMonths() {
        int months = 0;
        Calendar cal = start.toCalendar();
        Calendar endCal = end.toCalendar();
        cal.add(Calendar.MONTH, 1);
        while (!cal.after(endCal)) {
            months++;
            cal.add(Calendar.MONTH, 1);
        }
        return months;
    }
    /**
     * Gets the number of days between start and end dates, rounded down.
     * @return number of days
     */
    public int getDays() {
        return (int) ((end.toMillis() - start.toMillis()) 
                / DateUtils.MILLIS_PER_DAY);
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
