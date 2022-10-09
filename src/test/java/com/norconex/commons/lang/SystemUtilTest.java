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

import org.apache.commons.lang3.mutable.MutableObject;
import org.junit.jupiter.api.Test;

class SystemUtilTest {

    @Test
    void testRunWithProperty() {
        MutableObject<String> prop = new MutableObject<>();
        SystemUtil.runWithProperty("fruit", "apple", () -> {
            prop.setValue(System.getProperty("fruit"));
        });
        assertThat(prop.getValue()).isEqualTo("apple");
        assertThat(System.getProperty("fruit")).isNull();

        assertDoesNotThrow(() -> SystemUtil.runWithProperty("x", "y", null));
        assertDoesNotThrow(
                () -> SystemUtil.runWithProperty(null, "y", () -> {}));
        assertDoesNotThrow(
                () -> SystemUtil.runWithProperty("x", null, () -> {}));
    }

    @Test
    void testCallWithProperty() throws Exception {
        String prop = SystemUtil.callWithProperty("fruit", "apple",
                () -> System.getProperty("fruit"));
        assertThat(prop).isEqualTo("apple");
        assertThat(System.getProperty("fruit")).isNull();

        assertDoesNotThrow(() -> SystemUtil.callWithProperty("x", "y", null));
        assertDoesNotThrow(
                () -> SystemUtil.callWithProperty(null, "y", () -> null));
        assertDoesNotThrow(
                () -> SystemUtil.callWithProperty("x", null, () -> null));
    }

    @Test
    void testGetEnvironmentOrProperty() throws Exception {
        assertThat(SystemUtil.callWithProperty("some.fruit", "apple",
                () -> SystemUtil.getPropertyOrEnvironment("SOME_FRUIT")))
            .isEqualTo("apple");
        assertThat(SystemUtil.getPropertyOrEnvironment("Not/Exist")).isNull();
    }
}
