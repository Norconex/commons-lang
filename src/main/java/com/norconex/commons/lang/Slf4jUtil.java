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
package com.norconex.commons.lang;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Predicate;

import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.event.Level;

/**
 * Provides convenience methods complementing the SLF4J offerings.
 * @since 2.0.0
 */
public final class Slf4jUtil {

    private static final Map<Level, LevelLogger> LL_MAP =
            new EnumMap<>(Level.class);
    static {
        LL_MAP.put(Level.TRACE, Logger::trace);
        LL_MAP.put(Level.DEBUG, Logger::debug);
        LL_MAP.put(Level.INFO, Logger::info);
        LL_MAP.put(Level.WARN, Logger::warn);
        LL_MAP.put(Level.ERROR, Logger::error);
    }

    private static final Map<Level, Predicate<Logger>> ENABLED_MAP =
            new EnumMap<>(Level.class);
    static {
        ENABLED_MAP.put(Level.TRACE, Logger::isTraceEnabled);
        ENABLED_MAP.put(Level.DEBUG, Logger::isDebugEnabled);
        ENABLED_MAP.put(Level.INFO, Logger::isInfoEnabled);
        ENABLED_MAP.put(Level.WARN, Logger::isWarnEnabled);
        ENABLED_MAP.put(Level.ERROR, Logger::isErrorEnabled);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static final BidiMap<java.util.logging.Level,
            Level> JAVA_LEVEL_MAP = new DualHashBidiMap();
    static {
        JAVA_LEVEL_MAP.put(java.util.logging.Level.FINEST, Level.TRACE);
        JAVA_LEVEL_MAP.put(java.util.logging.Level.FINER, Level.DEBUG);
        JAVA_LEVEL_MAP.put(java.util.logging.Level.FINE, Level.DEBUG);
        JAVA_LEVEL_MAP.put(java.util.logging.Level.INFO, Level.INFO);
        JAVA_LEVEL_MAP.put(java.util.logging.Level.WARNING, Level.WARN);
        JAVA_LEVEL_MAP.put(java.util.logging.Level.SEVERE, Level.ERROR);
    }

    private Slf4jUtil() {
    }

    /**
     * Logs a message with a dynamically set SLF4J log {@link Level}.  As
     * of this writing, SLF4J did not offer such ability.
     * @param logger the original logger
     * @param level the log level
     * @param format formatted log message
     * @param args log arguments
     */
    public static void log(
            Logger logger, String level, String format, Object... args) {
        log(logger, Level.valueOf(StringUtils.upperCase(level)), format, args);
    }

    /**
     * Logs a message with a dynamically set SLF4J log {@link Level}.  As
     * of this writing, SLF4J did not offer such ability.
     * @param logger the original logger
     * @param level the log level
     * @param format formatted log message
     * @param args log arguments
     */
    public static void log(
            Logger logger, Level level, String format, Object... args) {
        LL_MAP.get(level).log(logger, format, args);
    }

    /**
     * Gets whether the supplied log level is enabled for the given logger.
     * @param logger logger
     * @param level level
     * @return <code>true</code> if enabled
     */
    public static boolean isEnabled(Logger logger, Level level) {
        return ENABLED_MAP.get(level).test(logger);
    }

    /**
     * Gets the finest log level supported by the supplied logger.
     * @param logger the logger to get the finest level from
     * @return finest log level
     */
    public static Level getLevel(Logger logger) {
        Level l = null;
        for (Level level : LL_MAP.keySet()) {
            if (isEnabled(logger, level)) {
                l = level;
            }
        }
        return l;
    }

    /**
     * Converts a Java {@link java.util.logging.Level} to a SLF4J {@link Level}.
     * @param javaLevel java level
     * @return SLF4J Level
     */
    public static Level fromJavaLevel(java.util.logging.Level javaLevel) {
        return JAVA_LEVEL_MAP.get(javaLevel);
    }

    /**
     * Converts a SLF4J {@link Level} to a Java {@link java.util.logging.Level}.
     * @param level SLF4J Level
     * @return java level
     */
    public static java.util.logging.Level toJavaLevel(Level level) {
        return JAVA_LEVEL_MAP.getKey(level);
    }

    @FunctionalInterface
    private interface LevelLogger {
        void log(Logger logger, String format, Object... args);
    }
}
