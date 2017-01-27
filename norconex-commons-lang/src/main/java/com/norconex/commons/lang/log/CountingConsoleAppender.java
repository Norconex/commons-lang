/* Copyright 2017 Norconex Inc.
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
package com.norconex.commons.lang.log;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Layout;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.spi.LoggingEvent;

/**
 * A console appender that keeps track of how many events of each matching level 
 * were logged.
 * Contrary to {@link ConsoleAppender}, invoking the empty constructor
 * on this class will set a default layout ({@link #DEFAULT_LAYOUT}).
 * @author Pascal Essiembre
 * @since 1.13.0
 */
public class CountingConsoleAppender extends ConsoleAppender {

    public static final Layout DEFAULT_LAYOUT = new PatternLayout("%-5p %m%n");
    private final Map<Level, AtomicInteger> counters = new HashMap<>();
    private final Map<Class<?>, Logger> loggers = new HashMap<>();
    
    public CountingConsoleAppender() {
        this(DEFAULT_LAYOUT);
    }

    public CountingConsoleAppender(Layout layout, String target) {
        super(layout, target);
    }

    public CountingConsoleAppender(Layout layout) {
        super(layout);
    }
    @Override
    public void append(LoggingEvent event) {
        super.append(event);
        getCounter(event.getLevel()).incrementAndGet();
    }

    /**
     * Whether this appender counted any log events.
     * @return <code>true</code> if an event was logged
     */
    public boolean isEmpty() {
        return counters.isEmpty();
    }
    
    /**
     * Gets the number of events logged for the given log level.
     * @param level log level
     * @return number of events logged
     */
    public int getCount(Level level) {
        return getCounter(level).get();
    }

    /**
     * Gets the number of events logged for all log levels.
     * @return number of events logged
     */
    public int getCount() {
        int count = 0;
        for (AtomicInteger i : counters.values()) {
            count += i.get();
        }
        return count;
    }

    /**
     * Resets all counts to zero.
     */
    @Override
    public synchronized void reset() {
        super.reset();
        if (counters != null) {
            counters.clear();
        }
    }

    /**
     * Starts counting log events for a class by creating a logger for that
     * class and appending itself to it.  If a logger was already created 
     * for the class, it will be reused (but the passed log level will be set
     * on it).
     * @param clazz class to count log events for
     * @param logLevel minimum log level to track
     */
    public synchronized final void startCountingFor(
            Class<?> clazz, Level logLevel) {
        Logger logger = loggers.get(clazz);
        if (logger == null) {
            logger = LogManager.getLogger(clazz);
            loggers.put(clazz, logger);
        }
        if (!logger.isAttached(this)) {
            logger.addAppender(this);
        }
        logger.setLevel(logLevel);
        logger.setAdditivity(false);
    }
    /**
     * Stops counting log events for a class by removing this appender
     * from the logger previously created for the supplied class.
     * This method has no effect if {@link #startCountingFor(Class, Level)} 
     * was not previously invoked with the same class.
     * @param clazz class to stop counting log events for
     */
    public synchronized final void stopCountingFor(Class<?> clazz) {
        Logger logger = loggers.get(clazz);
        if (logger != null) {
            logger.removeAppender(this);
        }
    }
    
    private synchronized AtomicInteger getCounter(Level level) {
        AtomicInteger i = counters.get(level);
        if (i == null) {
            i = new AtomicInteger();
            counters.put(level, i);
        }
        return i;
    }
    
    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
                .append(counters)
                .toString();
    }
}
