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

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

import org.junit.jupiter.api.Test;

class PredicatesTest {

    @Test
    void testPredicates() {
        Predicates<String> predicates;

        // all
        predicates = new Predicates<>();
        predicates.add(s -> true);
        predicates.add(s -> true);
        assertThat(predicates.test("")).isTrue();

        predicates = new Predicates<>(2);
        predicates.add(s -> true);
        predicates.add(s -> false);
        assertThat(predicates.test("")).isFalse();

        predicates = new Predicates<>(2, false);
        predicates.add(s -> false);
        predicates.add(s -> false);
        assertThat(predicates.test("")).isFalse();

        predicates = new Predicates<>(Arrays.asList(
                s -> false,
                s -> false));
        assertThat(predicates.test("")).isFalse();

        assertThat((List<Predicate<Object>>) new Predicates<Object>(
                (Collection<Predicate<Object>>) null)).isEmpty();

        // any
        predicates = new Predicates<>(true);
        predicates.add(s -> true);
        predicates.add(s -> true);
        assertThat(predicates.test("")).isTrue();

        predicates = new Predicates<>(2, true);
        predicates.add(s -> true);
        predicates.add(s -> false);
        assertThat(predicates.test("")).isTrue();

        predicates = new Predicates<>(Arrays.asList(
                s -> false, s -> false), true);
        assertThat(predicates.test("")).isFalse();
        assertThat(predicates.isAny()).isTrue();
    }
}
