/* Copyright 2023 Norconex Inc.
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
package com.norconex.commons.lang.flow.module;

import java.util.Arrays;

/**
 * A control flow statement.
 * @since 3.0.0
 */
enum Statement {
    IF("if", new IfHandler<>(false)),
    IFNOT("ifNot", new IfHandler<>(true)),
    ALLOF("allOf", new ConditionGroupHandler<>(false)),
    ANYOF("anyOf", new ConditionGroupHandler<>(true)),
    CONDITION("condition", new ConditionHandler<>()),
    THEN("then", new RootHandler<>()),
    ELSE("else", new RootHandler<>());
    ;

    private final String name;
    private final StatementHandler<?> handler;

    Statement(String name, StatementHandler<?> handler) {
        this.name = name;
        this.handler = handler;
    }

    public boolean is(String tagName) {
        return name.equals(tagName);
    }

    public StatementHandler<?> handler() {
        return handler;
    }

    @Override
    public String toString() {
        return name;
    }

    public boolean isAnyOf(Statement... tags) {
        for (Statement statement : tags) {
            if (statement == this) {
                return true;
            }
        }
        return false;
    }

    public static boolean isAnyOf(String tagName, Statement... tags) {
        for (Statement statement : tags) {
            if (statement.is(tagName)) {
                return true;
            }
        }
        return false;
    }

    public static Statement of(String tagName) {
        return Arrays.stream(values())
                .filter(t -> t.is(tagName))
                .findFirst()
                .orElse(null);
    }
}