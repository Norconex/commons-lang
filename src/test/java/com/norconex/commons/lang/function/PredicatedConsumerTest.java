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
import java.util.List;

import org.junit.jupiter.api.Test;

class PredicatedConsumerTest {

    @Test
    void testPredicates() {
        List<String> output = new ArrayList<>();
        PredicatedConsumer<String> pc;

        pc = new PredicatedConsumer<>("a"::equals, s -> output.add(s));
        assertThat(pc.getPredicate()).isNotNull();
        assertThat(pc.getConsumer()).isNotNull();

        pc.accept("a");
        pc.accept("b");
        assertThat(output).containsExactly("a");

        // negate
        output.clear();
        pc = new PredicatedConsumer<>("a"::equals, s -> output.add(s), true);
        pc.accept("a");
        pc.accept("b");
        assertThat(output).containsExactly("b");

        // nulls
        output.clear();
        pc = new PredicatedConsumer<>(null, s -> output.add(s));
        pc.accept("a");
        assertThat(output).containsExactly("a");

        output.clear();
        pc = new PredicatedConsumer<>("a"::equals, null);
        pc.accept("a");
        assertThat(output).isEmpty();
    }
}
