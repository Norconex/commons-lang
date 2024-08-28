/* Copyright 2022 Norconex Inc.
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

class SLF4JUtilTest {

    @Test
    void testSLF4JUtil() {
        Logger logger = LoggerFactory.getLogger(SLF4JUtilTest.class);

        assertDoesNotThrow(() -> {
            SLF4JUtil.log(logger, "debug", "log message.");
            SLF4JUtil.log(logger, Level.DEBUG, "log message.");
            SLF4JUtil.getLevel(logger);
        });

        assertThat(SLF4JUtil.isEnabled(logger, Level.DEBUG))
                .isEqualTo(logger.isDebugEnabled());

        assertThat(SLF4JUtil.fromJavaLevel(java.util.logging.Level.WARNING))
                .isEqualTo(Level.WARN);

        assertThat(SLF4JUtil.toJavaLevel(Level.WARN))
                .isEqualTo(java.util.logging.Level.WARNING);
    }
}
