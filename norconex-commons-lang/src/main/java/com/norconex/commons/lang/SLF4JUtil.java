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

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.event.Level;

/**
 * Provides convenience methods complementing the SLF4J offerings.
 * @author Pascal Essiembre
 * @since 2.0.0
 */
public final class SLF4JUtil {

    private static final Map<Level, LevelLogger> LL_MAP =
            new EnumMap<>(Level.class);
    static {
        LL_MAP.put(Level.TRACE, (l, f, a) -> l.trace(f, a));
        LL_MAP.put(Level.DEBUG, (l, f, a) -> l.debug(f, a));
        LL_MAP.put(Level.INFO,  (l, f, a) -> l.info(f, a));
        LL_MAP.put(Level.WARN,  (l, f, a) -> l.warn(f, a));
        LL_MAP.put(Level.ERROR, (l, f, a) -> l.error(f, a));
    }


    private SLF4JUtil() {
        super();
    }

    //TODO have log version without the Logger argument.
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

    @FunctionalInterface
    private interface LevelLogger {
        public void log(Logger logger, String format, Object... args);
    }

}
