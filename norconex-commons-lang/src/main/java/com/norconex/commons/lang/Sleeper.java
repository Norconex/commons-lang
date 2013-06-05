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
package com.norconex.commons.lang;

import java.util.concurrent.TimeUnit;

/**
 * Convenience class to put to sleep the currently running thread.
 * If sleeping fails, it throws a runtime exception of type
 * {@link SleeperException}
 * 
 * @author Pascal Essiembre
 */
@SuppressWarnings("nls")
public final class Sleeper {

    /** Number of milliseconds representing 1 second. */
    public static final long ONE_SECOND = 1000;
    /** Number of milliseconds representing 1 minute. */
    public static final long ONE_MINUTE = 60 * ONE_SECOND;
    /** Number of milliseconds representing 1 hour. */
    public static final long ONE_HOUR = 60 * ONE_MINUTE;
    
    private Sleeper() {
        super();
    }

    /**
     * Sleeps for the number of milliseconds specified.
     * @param milliseconds milliseconds
     */
    public static void sleepMillis(long milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            throw new SleeperException("Cannot sleep.", e);
        }
    }
    /**
     * Sleeps for the number of nanoseconds specified.
     * @param nanoSeconds nanoseconds
     */
    public static void sleepNanos(long nanoSeconds) {
        long milis = TimeUnit.NANOSECONDS.toMillis(nanoSeconds);
        int nanoRemains = (int) (nanoSeconds 
                - TimeUnit.MILLISECONDS.toNanos(milis));
        try {
            Thread.sleep(milis, nanoRemains);
        } catch (InterruptedException e) {
            throw new SleeperException("Cannot sleep.", e);
        }
    }
    /**
     * Sleeps for the number of seconds specified.
     * @param seconds seconds
     */
    public static void sleepSeconds(int seconds) {
        sleepMillis(seconds * ONE_SECOND);
    }
    /**
     * Sleeps for the number of minutes specified.
     * @param minutes minutes
     */
    public static void sleepMinutes(int minutes) {
        sleepMillis(minutes * ONE_MINUTE);
    }
    /**
     * Sleeps for the number of hours specified.
     * @param hours hours
     */
    public static void sleepHours(int hours) {
        sleepMillis(hours * ONE_HOUR);
    }
}
