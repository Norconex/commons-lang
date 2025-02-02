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

import java.util.Objects;

/**
 * An abstract {@link FlowConsumerAdapter} that takes care setting/getting
 * the adaptee and leaves only the {@link #accept(Object)} method to implement.
 * @param <A> the adaptee type (the type adapted to a Consumer)
 * @param <T> the type being consumed by this consumer adapter
 */
public abstract class BaseConsumerAdapter<A, T>
        implements FlowConsumerAdapter<T> {

    private A adaptee;

    @Override
    public A getConsumerAdaptee() {
        return adaptee;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void setConsumerAdaptee(Object adaptee) {
        this.adaptee = (A) adaptee;
    }

    @Override
    public String toString() {
        return Objects.toString(adaptee, "");
    }

    @Override
    public boolean equals(Object obj) {
        return Objects.equals(adaptee, obj);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(adaptee);
    }
}