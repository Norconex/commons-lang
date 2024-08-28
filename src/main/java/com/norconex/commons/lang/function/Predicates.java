/* Copyright 2021-2023 Norconex Inc.
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.ToString;

/**
 * <p>
 * A list of {@link Predicate} instances, matched as a group. This class
 * is an alternative to chaining multiple {@link Predicate#and(Predicate)} or
 * {@link Predicate#or(Predicate)}.
 * </p>
 * <p>
 * By default predicates are tested in order and testing stops after the first
 * one that returned <code>false</code>. Only if all predicates return
 * <code>true</code> that this predicate list will return <code>true</code>.
 * </p>
 * <p>
 * The behavior is the opposite if the "any" constructor argument is
 * <code>true</code>.  That is, all predicates are tested in order and
 * stop after the first one returning <code>true</code>. Only if all
 * predicates return <code>false</code> that this predicate list will return
 * <code>false</code>.
 * </p>
 * <p>
 * Any <code>null</code> predicates are simply ignored.
 * </p>
 *
 * @param <T> type being put to the test
 * @since 2.0.0
 */
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class Predicates<T> extends ArrayList<Predicate<T>>
        implements Predicate<T> {

    private static final long serialVersionUID = 1L;

    private final boolean any;

    public Predicates() {
        this(false);
    }

    public Predicates(boolean any) {
        this.any = any;
    }

    public Predicates(int initialCapacity) {
        this(initialCapacity, false);
    }

    public Predicates(int initialCapacity, boolean any) {
        super(initialCapacity);
        this.any = any;
    }

    public Predicates(Collection<Predicate<T>> predicates) {
        this(predicates, false);
    }

    public Predicates(Collection<Predicate<T>> predicates, boolean any) {
        super(predicates == null
                ? Collections.emptyList()
                : predicates
                        .stream()
                        .filter(Objects::nonNull)
                        .toList());
        this.any = any;
    }

    public boolean isAny() {
        return any;
    }

    @Override
    public boolean test(T t) {
        for (Predicate<T> predicate : this) {
            var result = predicate.test(t);
            if (!result && !any) {
                return false;
            }
            if (result && any) {
                return true;
            }
        }
        return !any;
    }

    /**
     * Group of predicate that returns <code>true</code> if any of them
     * returns <code>true</code>.
     * @param <T> the type of object tested
     * @param predicates a group of predicates
     * @return a group of predicates
     * @since 3.0.0
     */
    @SafeVarargs
    public static <T> Predicates<T> anyOf(
            @NonNull Predicate<T>... predicates) {
        return new Predicates<>(List.of(predicates), true);
    }

    /**
     * Group of predicate that returns <code>true</code> if all of them
     * returns <code>true</code>.
     * @param <T> the type of object tested
     * @param predicates a group of predicates
     * @return a group of predicates
     * @since 3.0.0
     */
    @SafeVarargs
    public static <T> Predicates<T> allOf(
            @NonNull Predicate<T>... predicates) {
        return new Predicates<>(List.of(predicates), false);
    }
}
