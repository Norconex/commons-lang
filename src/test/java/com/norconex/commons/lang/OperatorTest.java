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

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

class OperatorTest {

    @Test
    void testOf() {
        assertThat(of("gt", "GT", ">"))
            .isNotEmpty()
            .allMatch(op -> Operator.GREATER_THAN == op);
        assertThat(of("ge", "GE", ">=", "=>"))
            .isNotEmpty()
            .allMatch(op -> Operator.GREATER_EQUAL == op);
        assertThat(of("eq", "EQ", "is", "IS", "=", "=="))
            .isNotEmpty()
            .allMatch(op -> Operator.EQUALS == op);
        assertThat(of("le", "LE", "<=", "=<"))
            .isNotEmpty()
            .allMatch(op -> Operator.LOWER_EQUAL == op);
        assertThat(of("lt", "LT", "<"))
            .isNotEmpty()
            .allMatch(op -> Operator.LOWER_THAN == op);
        assertThat(of((String) null))
            .isNotEmpty()
            .allMatch(Objects::isNull);
        assertThat(of("badOne"))
            .isNotEmpty()
            .allMatch(Objects::isNull);
    }

    @Test
    void testToString() {
        assertThat(Operator.GREATER_THAN).hasToString("gt");
        assertThat(Operator.GREATER_EQUAL).hasToString("ge");
        assertThat(Operator.EQUALS).hasToString("eq");
        assertThat(Operator.LOWER_EQUAL).hasToString("le");
        assertThat(Operator.LOWER_THAN).hasToString("lt");
    }

    @Test
    void testEvaluate() {
        assertThat(Operator.GREATER_THAN.evaluate(1, 2)).isFalse();
        assertThat(Operator.GREATER_THAN.evaluate(1, 1)).isFalse();
        assertThat(Operator.GREATER_THAN.evaluate(2, 1)).isTrue();

        assertThat(Operator.GREATER_EQUAL.evaluate(1, 2)).isFalse();
        assertThat(Operator.GREATER_EQUAL.evaluate(1, 1)).isTrue();
        assertThat(Operator.GREATER_EQUAL.evaluate(2, 1)).isTrue();

        assertThat(Operator.EQUALS.evaluate(1, 2)).isFalse();
        assertThat(Operator.EQUALS.evaluate(1, 1)).isTrue();
        assertThat(Operator.EQUALS.evaluate(2, 1)).isFalse();

        assertThat(Operator.LOWER_EQUAL.evaluate(1, 2)).isTrue();
        assertThat(Operator.LOWER_EQUAL.evaluate(1, 1)).isTrue();
        assertThat(Operator.LOWER_EQUAL.evaluate(2, 1)).isFalse();

        assertThat(Operator.LOWER_THAN.evaluate(1, 2)).isTrue();
        assertThat(Operator.LOWER_THAN.evaluate(1, 1)).isFalse();
        assertThat(Operator.LOWER_THAN.evaluate(2, 1)).isFalse();
    }

    private List<Operator> of(String... ops) {
        return Stream.of(ops)
                .map(Operator::of)
                .collect(Collectors.toList());
    }
}
