/* Copyright 2021-2022 Norconex Inc.
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

import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * <p>
 * A {@link Consumer} that only gets triggered if a given predicate is
 * <code>true</code>.
 * </p>
 * <p>
 * A <code>null</code> predicate evaluates to <code>true</code> (before
 * applying negation if "negate" is <code>true</code>).
 * </p>
 * <p>
 * A <code>null</code> consumer is simply ignored.
 * </p>
 * <p>
 * Setting the "negate" constructor argument to <code>true</code> will
 * perform the same predicate evaluation, but will only trigger the
 * consumers if it returns <code>false</code>.
 * </p>
 *
 * @param <T> type being predicated and consumed
 * @since 2.0.0
 */
@EqualsAndHashCode
@ToString
public class PredicatedConsumer<T> implements Consumer<T> {

    private final Predicate<T> predicate;
    private final Consumer<T> thenConsumer;
    private final Consumer<T> elseConsumer;
    private final boolean negate;

    /**
     * Constructor.
     * @param predicate predicate
     * @param consumer consumer
     * @deprecated Since 3.0.0, use
     *     {@link PredicatedConsumer#ifTrue(Predicate, Consumer)}
     *     instead.
     */
    @Deprecated(since = "3.0.0")
    public PredicatedConsumer( //NOSONAR
            Predicate<T> predicate, Consumer<T> consumer) {
        this(predicate, consumer, false);
    }
    /**
     * Constructor.
     * @param predicate predicate
     * @param consumer consumer
     * @param negate whether to negate the condition
     * @deprecated Since 3.0.0, use {@link PredicatedConsumer#ifTrue(
     *         Predicate, Consumer, Consumer)} instead.
     */
    @Deprecated(since = "3.0.0")
    public PredicatedConsumer( //NOSONAR
            Predicate<T> predicate, Consumer<T> consumer, boolean negate) {
        this(predicate, consumer, null, negate);
    }

    /**
     * Constructor.
     * @param predicate predicate condition
     * @param thenConsumer consumer invoked when condition is <code>true</code>
     * @param elseConsumer consumer invoked when condition is <code>false</code>
     * @param negate whether to negate the condition
     */
    public PredicatedConsumer(
            Predicate<T> predicate,
            Consumer<T> thenConsumer,
            Consumer<T> elseConsumer,
            boolean negate) {
        this.predicate = predicate;
        this.thenConsumer = thenConsumer;
        this.elseConsumer = elseConsumer;
        this.negate = negate;
    }

    public Predicate<T> getPredicate() {
        return predicate;
    }
    public Consumer<T> getConsumer() {
        return thenConsumer;
    }

    @Override
    public final void accept(T t) {
        var result = predicate == null || predicate.test(t);
        if (negate) {
            result = !result;
        }
        if (result) {
            if (thenConsumer != null) {
                thenConsumer.accept(t);
            }
        } else if (elseConsumer != null) {
            elseConsumer.accept(t);
        }
    }

    /**
     * If condition is <code>true</code>, then execute consumer.
     * @param <T> type of object consumed
     * @param condition the condition deciding whether to consume
     * @param thenConsumer the object consumer
     * @return a predicated consumer
     * @since 3.0.0
     */
    public static <T> PredicatedConsumer<T> ifTrue(
            Predicate<T> condition,
            Consumer<T> thenConsumer) {
        return ifTrue(condition, thenConsumer, null);
    }
    /**
     * If condition is <code>true</code>, then execute the "then" consumer,
     * else,execute the "else" consumer.
     * @param <T> type of object consumed
     * @param condition the condition deciding which consumer to use
     * @param thenConsumer the object consumer when condition is
     *     <code>true</code>
     * @param elseConsumer the object consumer when condition is
     *     <code>false</code>
     * @return a predicated consumer
     * @since 3.0.0
     */
    public static <T> PredicatedConsumer<T> ifTrue(
            Predicate<T> condition,
            Consumer<T> thenConsumer,
            final Consumer<T> elseConsumer) {
        return new PredicatedConsumer<>(
                condition, thenConsumer, elseConsumer, false);
    }

    /**
     * If condition is <code>false</code>, then execute consumer.
     * @param <T> type of object consumed
     * @param condition the condition deciding whether to consume
     * @param thenConsumer the object consumer
     * @return a predicated consumer
     * @since 3.0.0
     */
    public static <T> PredicatedConsumer<T> ifFalse(
            Predicate<T> condition,
            Consumer<T> thenConsumer) {
        return ifFalse(condition, thenConsumer, null);
    }
    /**
     * If condition is <code>false</code>, then execute the "then" consumer,
     * else, execute the "else" consumer.
     * @param <T> type of object consumed
     * @param condition the condition deciding which consumer to use
     * @param thenConsumer the object consumer when condition is
     *     <code>true</code>
     * @param elseConsumer the object consumer when condition is
     *     <code>false</code>
     * @return a predicated consumer
     * @since 3.0.0
     */
    public static <T> PredicatedConsumer<T> ifFalse(
            Predicate<T> condition,
            Consumer<T> thenConsumer,
            final Consumer<T> elseConsumer) {
        return new PredicatedConsumer<>(
                condition, thenConsumer, elseConsumer, true);
    }
}
