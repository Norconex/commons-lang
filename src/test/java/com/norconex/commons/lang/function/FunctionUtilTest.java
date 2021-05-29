/* Copyright 2021 Norconex Inc.
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
package com.norconex.commons.lang.function;

import java.util.function.Consumer;

import org.apache.commons.lang3.mutable.MutableObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class FunctionUtilTest {

    @Test
    void testPredicatedConsumer() {
        MutableObject<String> result = new MutableObject<>();
        Consumer<String> c = FunctionUtil.predicatedConsumer(
                v -> "potato".equals(v),
                v -> result.setValue("fries"));

        c.accept("tomato");
        Assertions.assertNull(result.getValue());

        c.accept("potato");
        Assertions.assertEquals("fries", result.getValue());
    }
}
