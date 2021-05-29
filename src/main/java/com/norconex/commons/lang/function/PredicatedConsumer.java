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
package com.norconex.commons.lang.function;

import java.util.function.Consumer;
import java.util.function.Predicate;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * <p>
 * A {@link Consumer} that only gets triggered if a given predicate is
 * <code>true</code>.
 * </p>
 * <p>
 * A <code>null</code> predicate evaluates to <code>true</code> and is the
 * same as using a regular {@link Consumer}.
 * </p>
 * <p>
 * A <code>null</code> consumer has no effect and renders this class useless.
 * </p>
 * <p>
 * Setting the "negate" constructor argument to <code>true</code> will
 * perform the same predicate evaluation, but will only trigger the
 * consumers if it returns <code>false</code>.
 * </p>
 *
 * @param <T> type being predicated and consumed
 * @author Pascal Essiembre
 * @since 2.0.0
 */
public class PredicatedConsumer<T> implements Consumer<T> {

    private final Predicate<T> predicate;
    private final Consumer<T> consumer;
    private final boolean negate;

    public PredicatedConsumer(Predicate<T> predicate, Consumer<T> consumer) {
        this(predicate, consumer, false);
    }
    public PredicatedConsumer(
            Predicate<T> predicate, Consumer<T> consumer, boolean negate) {
        super();
        this.predicate = predicate;
        this.consumer = consumer;
        this.negate = negate;
    }

    public Predicate<T> getPredicate() {
        return predicate;
    }
    public Consumer<T> getConsumer() {
        return consumer;
    }

    @Override
    public final void accept(T t) {
        if (consumer == null) {
            return;
        }
        boolean passed = predicate == null || predicate.test(t);
        if (negate) {
            passed = !passed;
        }
        if (passed) {
            consumer.accept(t);
        }
    }

    @Override
    public boolean equals(final Object other) {
        return EqualsBuilder.reflectionEquals(this, other);
    }
    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }
    @Override
    public String toString() {
        return new ReflectionToStringBuilder(
                this, ToStringStyle.SHORT_PREFIX_STYLE).toString();
    }
}
