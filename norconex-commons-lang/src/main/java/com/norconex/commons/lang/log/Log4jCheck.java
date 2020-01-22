/* Copyright 2020 Norconex Inc.
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

/**
 * Utility class to facilitate migration to SLF4J
 * (logging mechanism used starting with version 2.0.0). It offers checks
 * for the presence or log4j or slf4j for conditional invocation in the code.
 * Will be removed in version 2.0.0.
 * @author Pascal Essiembre
 * @since 1.15.2
 */
public final class Log4jCheck {

    private static final boolean LOG4J_PRESENT;
    static {
        boolean present;
        try {
            // This class is only present in Log4j and not SLF4J so we use it
            // to find out if log4j implementation is present.
            Class.forName("org.apache.log4j.AsyncAppender");
            present = true;
        } catch (ClassNotFoundException e) {
            present = false;
        }
        LOG4J_PRESENT = present;
    }

    private Log4jCheck() {
        super();
    }

    public static boolean present() {
        return LOG4J_PRESENT;
    }
}
