/* Copyright 2021-2022 Norconex Inc.
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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

import org.apache.commons.lang3.mutable.MutableObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@Deprecated(since = "3.0.0")
class FunctionUtilTest {

    @Test
    void testPredicatedConsumer() {
        var result = new MutableObject<String>();
        Consumer<String> c = FunctionUtil.predicatedConsumer(
                "potato"::equals,
                v -> result.setValue("fries"));

        c.accept("tomato");
        Assertions.assertNull(result.getValue());

        c.accept("potato");
        Assertions.assertEquals("fries", result.getValue());
    }

    @Test
    void testAllConsumers() {
        List<String> output = new ArrayList<>();
        Consumers<String> consumers = FunctionUtil.allConsumers(
                s -> output.add(s + "b"),
                s -> output.add(s + "bc"));
        consumers.accept("a");
        assertThat(output).containsExactly("ab", "abc");

        assertThat(FunctionUtil.allConsumers(
                (Consumer<Object>[]) null)).isEmpty();
    }

    @Test
    void testAllPredicates() {
        assertThat(FunctionUtil.allPredicates(
                s -> true, s -> false).test("")).isFalse();
        assertThat(FunctionUtil.allPredicates(
                s -> false, s -> false).test("")).isFalse();
        assertThat(FunctionUtil.allPredicates(
                s -> true, s -> true).test("")).isTrue();

        assertThat((List<Predicate<Object>>) FunctionUtil.allPredicates(
                (Predicate<Object>[]) null)).isEmpty();
    }

    @Test
    void testAnyPredicates() {
        assertThat(FunctionUtil.anyPredicates(
                s -> true, s -> false).test("")).isTrue();
        assertThat(FunctionUtil.anyPredicates(
                s -> false, s -> false).test("")).isFalse();
        assertThat(FunctionUtil.anyPredicates(
                s -> true, s -> true).test("")).isTrue();

        assertThat((List<Predicate<Object>>) FunctionUtil.anyPredicates(
                (Predicate<Object>[]) null)).isEmpty();
    }

}
