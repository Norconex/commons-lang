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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * <p>
 * A list of {@link Predicate} instances, matched as a group.
 * All supplied predicates must return <code>true</code> for this one to return
 * <code>true</code>, unless the constructor is invoked with a
 * <code>true</code> <code>any</code> argument. In such case, any
 * of the predicates must match to return <code>true</code>.
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
                    .collect(Collectors.toList()));
        this.any = any;
    }

    public boolean isAny() {
        return any;
    }
    @Override
    public boolean test(T t) {
        for (Predicate<T> predicate : this) {
            boolean result = predicate.test(t);
            if (!result && !any) {
                return false;
            }
            if (result && any) {
                return true;
            }
        }
        return !any;
    }
}
