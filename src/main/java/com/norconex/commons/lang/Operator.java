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
package com.norconex.commons.lang;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.compare.ComparableUtils;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * <p>
 * Operators used by a few conditions based on comparable values
 * (e.g., numbers, dates, etc.).  Facilitates specifying/parsing such operators
 * as strings
 * </p>
 *
 * {@nx.block #operators
 * <h3>Textual representation:</h3>
 * <p>
 * Operators can be parsed using symbols or
 * text abbreviations. Text abbreviations are useful when you do now want to
 * concern yourself when used in XML. Possible representations are:
 * </p>
 * <table>
 *   <tr><th>Symbol</th><th>Abbreviation</th><th>Meaning</th></tr>
 *   <tr><td>&gt;</td><td>gt</td><td>greater than</td></tr>
 *   <tr><td>&gt;=, =&gt;</td><td>ge</td><td>greater equal</td></tr>
 *   <tr><td>&lt;</td><td>lt</td><td>lower than</td></tr>
 *   <tr><td>&lt;=, =&lt;</td><td>le</td><td>lowe equal</td></tr>
 *   <tr><td>=, ==</td><td>eq,is</td><td>equals</td></tr>
 * </table>
 * }
 *
 * @since 2.0.0
 */
public enum Operator {
    GREATER_THAN("gt", ">") {
        @Override
        public <T extends Comparable<T>> boolean evaluate(T first, T second) {
            return ComparableUtils.is(first).greaterThan(second);
        }
    },
    GREATER_EQUAL("ge", ">=", "=>") {
        @Override
        public <T extends Comparable<T>> boolean evaluate(T first, T second) {
            return ComparableUtils.is(first).greaterThanOrEqualTo(second);
        }
    },
    EQUALS("eq", "is", "=", "==") {
        @Override
        public <T extends Comparable<T>> boolean evaluate(T first, T second) {
            return ComparableUtils.is(first).equalTo(second);
        }
    },
    LOWER_EQUAL("le", "<=", "=<") {
        @Override
        public <T extends Comparable<T>> boolean evaluate(T first, T second) {
            return ComparableUtils.is(first).lessThanOrEqualTo(second);
        }
    },
    LOWER_THAN("lt", "<") {
        @Override
        public <T extends Comparable<T>> boolean evaluate(T first, T second) {
            return ComparableUtils.is(first).lessThan(second);
        }
    };

    String[] abbr;

    Operator(String... abbr) {
        this.abbr = abbr;
    }

    @JsonCreator
    public static Operator of(String op) {
        if (StringUtils.isBlank(op)) {
            return null;
        }
        var cleanOp = StringUtils.remove(op.toLowerCase().trim(), ' ');
        for (Operator c : Operator.values()) {
            if (ArrayUtils.contains(c.abbr, cleanOp)) {
                return c;
            }
        }
        return null;
    }

    @JsonValue
    @Override
    public String toString() {
        return abbr[0];
    }

    public abstract <T extends Comparable<T>> boolean evaluate(
            T first, T second);
}
