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

import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class If<T> implements Consumer<T> {

    private final @NonNull FlowCondition<T> condition;
    private final @NonNull Consumer<T> thenConsumer;
    private final Consumer<T> elseConsumer;

    @Override
    public void accept(T t) {
        if (conditionPasses(t)) {
            if (thenConsumer != null) {
                thenConsumer.accept(t);
            }
        } else if (elseConsumer != null) {
            elseConsumer.accept(t);
        }
    }

    protected boolean conditionPasses(T t) {
        return condition.test(t);
    }
}
