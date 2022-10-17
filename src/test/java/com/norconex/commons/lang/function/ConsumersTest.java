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
package com.norconex.commons.lang.function;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;

class ConsumersTest {

    @Test
    void testConsumers() {
        List<String> output = new ArrayList<>();

        // #1
        Consumers<String> consumers1 = new Consumers<>();
        consumers1.add(s -> output.add("a"));
        consumers1.add(s -> output.add("b"));
        consumers1.accept("");

        // #2
        Consumers<String> consumers2 = new Consumers<>(2);
        consumers2.add(s -> output.add("a"));
        consumers2.add(s -> output.add("b"));
        consumers2.accept("");

        // #3
        Consumers<String> consumers3 = new Consumers<>(Arrays.asList(
                s -> output.add("a"),
                s -> output.add("b")));
        consumers3.accept("");

        assertThat(output).containsExactly("a", "b", "a", "b", "a", "b");
        assertThat(new Consumers<Object>((List<Consumer<Object>>) null))
                .isEmpty();
    }
}
