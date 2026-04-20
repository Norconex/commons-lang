/* Copyright 2026 Norconex Inc.
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
package com.norconex.commons.lang.flow;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.assertj.core.api.Assertions.assertThat;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JacksonAnnotationsInside;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize.Typing;

class FlowSupportTest {

    @Test
    void testBaseConsumerAdapter() {
        var adapter = new TestConsumerAdapter();

        adapter.setConsumerAdaptee("wrapped");
        adapter.accept("payload");

        assertThat(adapter.getConsumerAdaptee()).isEqualTo("wrapped");
        assertThat(adapter.getLastAccepted()).isEqualTo("payload");
        assertThat(adapter).hasToString("wrapped");
        assertThat(adapter.equals("wrapped")).isTrue();
        assertThat(adapter.hashCode()).isEqualTo("wrapped".hashCode());
    }

    @Test
    void testBasePredicateAdapter() {
        var adapter = new TestPredicateAdapter();

        adapter.setPredicateAdaptee(3);

        assertThat(adapter.getPredicateAdaptee()).isEqualTo(3);
        assertThat(adapter.test("abcd")).isTrue();
        assertThat(adapter.test("ab")).isFalse();
        assertThat(adapter).hasToString("3");
        assertThat(adapter.equals(3)).isTrue();
        assertThat(adapter.hashCode()).isEqualTo(Integer.hashCode(3));
    }

    @Test
    void testJsonFlowAnnotationDefaults() throws NoSuchFieldException {
        var field = FlowHolder.class.getDeclaredField("consumer");
        var annotation = field.getAnnotation(JsonFlow.class);

        assertThat(annotation).isNotNull();
        assertThat(annotation.builder()).isEqualTo(JsonFlow.NoBuilder.class);
        assertThat(JsonFlow.class.getAnnotation(Retention.class).value())
                .isEqualTo(RUNTIME);
        assertThat(JsonFlow.class.getAnnotation(Target.class).value())
                .containsExactly(FIELD);
        assertThat(JsonFlow.class.isAnnotationPresent(
                JacksonAnnotationsInside.class)).isTrue();
        assertThat(JsonFlow.class.getAnnotation(JsonSerialize.class).typing())
                .isEqualTo(Typing.STATIC);
    }

    @Test
    void testJsonFlowNoBuilder() {
        assertThat(new JsonFlow.NoBuilder().get()).isNull();
    }

    private static final class TestConsumerAdapter
            extends BaseConsumerAdapter<String, String> {
        private String lastAccepted;

        @Override
        public void accept(String value) {
            lastAccepted = value;
        }

        private String getLastAccepted() {
            return lastAccepted;
        }
    }

    private static final class TestPredicateAdapter
            extends BasePredicateAdapter<Integer, String> {
        @Override
        public boolean test(String value) {
            return value.length() >= getPredicateAdaptee();
        }
    }

    private static final class FlowHolder {
        @JsonFlow
        private Consumer<String> consumer;
    }
}