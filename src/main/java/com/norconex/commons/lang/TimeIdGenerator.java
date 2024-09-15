/* Copyright 2015-2020 Norconex Inc.
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

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * Generates a unique ID made out of the current time in milliseconds,
 * combined with a thread-safe atomic sequence value that guarantees
 * order and uniqueness within the same JVM.
 * </p>
 * <p>
 * This class was created for cases where unique sequential IDs are required
 * to be {@code long} primitives,
 * or where {@code long} are desired for things such as faster lookup
 * and generation (over large strings for instance) and you can't use a
 * persistent sequence generator.
 * If you do not have such need, it is best advised to use a
 * universal id generator instead, like java {@link UUID}.
 * </p>
 *
 * <h2>The fine-prints</h2>
 * <p>
 * At a minimum, values generated by this
 * class ensures uniqueness within JVM instances (read
 * <a href="#year2262">Year 2262 and beyond</a> further down).
 * If you need larger or different values to ensure greater
 * uniqueness between many JVMs,
 * you can convert the returned value to a string and simply
 * prefix it with a value of your choice (or convert to a {@code BigInteger}
 * if your can support larger numbers).
 * To obtain universally unique IDs, consider using a UUID implementation,
 * like the Java {@link UUID}.
 * </p>
 * <p>
 * IDs generated within a single JVM are guaranteed to be in order with no
 * duplicates (read
 * <a href="#year2262">Year 2262 and beyond</a> further down).
 * The sequence usually has many gaps.
 * </p>
 * <p>
 * Generated IDs are {@code long} primitives.  Java {@code long} values
 * are 64-bit whereas standard UUID are byte arrays of 128-bit
 * (plus the array reference itself).  When using UUIDs in their common
 * hexadecimal string format, they usually are at least 576-bit (plus the
 * string reference).
 * </p>
 * <p>
 * The reduced byte size of {@code long} values compared to most strings
 * may lead to lookup performance, in
 * addition of some system sorting primitive numbers faster than strings.
 * While not especially optimized for ID generation, creating {@code long}-based
 * IDs is much faster than most string-based approach.  For instance, this class
 * is typically more than 20 times faster at generating {@code long} values
 * than Java UUID at generating strings.
 * An average desktop computer can show it takes less than 50 milliseconds to
 * generate 1 million IDs (single thread).
 * </p>
 * <p>
 * Be advised this implementation does not account for the possibility of
 * a backward UTC time change on the host system clock (as opposed to
 * "local" time which can change without issues as long as UTC time is
 * unaffected).
 * </p>
 * <p>
 * Assuming your application and underlying platform can
 * achieve this feat, a maximum of 1 million unique IDs can be generated every
 * milliseconds (1 billion IDs per seconds). Every time that threshold is
 * reached, the method will wait until the current time has progressed
 * to the next millisecond to prevent ID duplication (waiting 1 nanosecond
 * at a time).
 * </p>
 * <p>
 * Java {@code long} values can hold 19 characters.  Each digit is part
 * of one of two groups of digits with a specific purpose.
 * The exact pattern is the following:
 * </p>
 * <pre>
 * Java long max value:   9223372036854775808
 * ------------------------------------------
 * Current time (ms):     9223372036854          &lt;--- max value
 * Atomic sequence:                    999999    &lt;--- max value
 * </pre>
 * <p>
 * This class is thread-safe.
 * </p>
 *
 * <a id="year2262"></a>
 * <h2>Year 2262 and beyond</h2>
 * <p>
 * Whenever the millisecond EPOCH representation of the current time reaches
 * 1e14, the time representation starts back at 0ms using a modulo between
 * the current time and 1e14.  The first time this will occur is when the
 * system UTC clock time reaches April 11<sup>th</sup>, 2262.  The moment
 * rollback of the current time value occurs, new IDs will be smaller
 * {@code long} values than IDs generated prior to that date.
 * If you only care about uniqueness, your IDs will still be unique unless
 * you have been generating IDs using this class for close to 300 years or more.
 * </p>
 *
 * @since 1.6.0
 */
public final class TimeIdGenerator {

    private static final Logger LOG =
            LoggerFactory.getLogger(TimeIdGenerator.class);

    private static final AtomicInteger DUP_SEQUENCE = new AtomicInteger();
    private static final long MILLIS_ROLLOVER_VALUE =
            BigDecimal.valueOf(1e14).longValueExact();
    private static final int MAX_DUP_SEQUENCE = 999999;
    private static final int TIME_MULTIPLIER = 1000000;

    private static long previousTime = -1;
    private static long previousGeneratedId = -1;
    private static int previousGeneratedDupSequence = 0;

    private TimeIdGenerator() {
    }

    /**
     * Returns the last generated number since the start of this
     * JVM (that value is not persisted anywhere outside the JVM memory).
     * Invoking this method before having called {@link #next()} at least
     * once will return {@code -1}.
     * @return the last id generated
     */
    public static synchronized long last() {
        return previousGeneratedId;
    }

    /**
     * Generates a new number unique within this JVM.
     * @return a long value
     */
    public static synchronized long next() {
        long time = System.currentTimeMillis() % MILLIS_ROLLOVER_VALUE;
        long id = time * TIME_MULTIPLIER;
        id += getDupSequence(time == previousTime);
        previousTime = time;
        previousGeneratedId = id;
        return id;
    }

    private static int getDupSequence(boolean needsIncrement) {
        if (needsIncrement) {
            int dupSeq = DUP_SEQUENCE.incrementAndGet();
            previousGeneratedDupSequence = dupSeq;
            if (dupSeq == MAX_DUP_SEQUENCE) {
                while (System.currentTimeMillis() == previousTime) {
                    Sleeper.sleepNanos(1);
                }
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Reached " + (MAX_DUP_SEQUENCE + 1)
                            + " ID creations in one millisecond. "
                            + "Had to wait for current millisecond to pass.");
                }
            }
            return dupSeq;
        }
        if (previousGeneratedDupSequence != 0) {
            DUP_SEQUENCE.set(0);
            previousGeneratedDupSequence = 0;
        }
        return 0;
    }
}
