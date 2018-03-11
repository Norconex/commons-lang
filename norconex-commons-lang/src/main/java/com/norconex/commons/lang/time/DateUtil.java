/* Copyright 2018 Norconex Inc.
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

/**
 * Date-related utility methods.
 * @author Pascal Essiembre
 * @since 2.0.0
 */
public final class DateUtil {

    private DateUtil() {
        super();
    }
    
    /**
     * Converts a {@link Date} to a {@link LocalDate} using the system
     * default {@link ZoneId}.
     * @param date to convert
     * @return converted date
     */
    public static LocalDate toLocalDate(Date date) {
        return toLocalDate(date, ZoneId.systemDefault());
    }
    /**
     * Converts a {@link Date} to a {@link LocalDate} using the specified
     * {@link ZoneId}.
     * @param date to convert
     * @param zoneId zone id
     * @return converted date
     */
    public static LocalDate toLocalDate(Date date, ZoneId zoneId) {
        return date.toInstant().atZone(zoneId).toLocalDate();
    }
    
    /**
     * Converts a {@link Date} to a {@link LocalDateTime} using the system
     * default {@link ZoneId}.
     * @param date to convert
     * @return converted date
     */
    public static LocalDateTime toLocalDateTime(Date date) {
        return toLocalDateTime(date, ZoneId.systemDefault());
    }
    /**
     * Converts a {@link Date} to a {@link LocalDateTime} using the specified
     * {@link ZoneId}.
     * @param date to convert
     * @param zoneId zone id
     * @return converted date
     */
    public static LocalDateTime toLocalDateTime(Date date, ZoneId zoneId) {
        return date.toInstant().atZone(zoneId).toLocalDateTime();        
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
}
