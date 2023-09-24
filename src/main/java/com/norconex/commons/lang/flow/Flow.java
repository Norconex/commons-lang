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

import java.util.List;
import java.util.function.Consumer;

import org.apache.commons.lang3.ArrayUtils;

import com.norconex.commons.lang.function.Consumers;

import lombok.Data;

/**
 *
 *
 * @param <T>
 */
@Data
public class Flow<T> implements Consumer<T> {

    private final Consumer<T> consumer;

    @Override
    public void accept(T t) {
        if (consumer != null) {
            consumer.accept(t);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> Flow<T> of(Consumer<T>... consumers) {
        if (ArrayUtils.isEmpty(consumers)) {
            return new Flow<>(null);
        }
        if (consumers.length == 1) {
            return new Flow<>(consumers[0]);
        }
        return new Flow<>(new Consumers<>(List.of(consumers)));
    }
}
