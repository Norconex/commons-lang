/* Copyright 2010-2018 Norconex Inc.
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
package com.norconex.commons.lang;

import java.util.concurrent.TimeUnit;

/**
 * Convenience class to put to sleep the currently running thread.
 * If sleeping fails, it throws a runtime exception of type
 * {@link SleeperException}
 * 
 * @author Pascal Essiembre
 */
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
