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
package com.norconex.commons.lang.flow;

import java.util.function.Consumer;
import java.util.function.Predicate;

import lombok.Builder;
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;


@Data
@RequiredArgsConstructor
@Builder
public class If<T> implements Consumer<T> {

    private final @NonNull Predicate<T> condition;
    private final @NonNull Consumer<T> thenConsumer;
    private final Consumer<T> elseConsumer;
    private final boolean negate;

    @Override
    public void accept(T t) {
        if (condition.test(t) && !negate) {
            if (thenConsumer != null) {
                thenConsumer.accept(t);
            }
        } else if (elseConsumer != null) {
            elseConsumer.accept(t);
        }
    }

    public static <T> If<T> isTrue(
            @NonNull Predicate<T> condition,
            @NonNull Consumer<T> thenConsumer) {
        return isTrue(condition, thenConsumer, null);
    }
    public static <T> If<T> isTrue(
            @NonNull Predicate<T> condition,
            @NonNull Consumer<T> thenConsumer,
            final Consumer<T> elseConsumer) {
        return new If<>(condition, thenConsumer, elseConsumer, false);
    }

    public static <T> If<T> isFalse(
            @NonNull Predicate<T> condition,
            @NonNull Consumer<T> thenConsumer) {
        return isFalse(condition, thenConsumer, null);
    }
    public static <T> If<T> isFalse(
            @NonNull Predicate<T> condition,
            @NonNull Consumer<T> thenConsumer,
            final Consumer<T> elseConsumer) {
        return new If<>(condition, thenConsumer, elseConsumer, true);
    }
}
