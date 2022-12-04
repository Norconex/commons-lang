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
import java.util.function.Consumer;
import java.util.stream.Collectors;

import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * <p>
 * A list of {@link Consumer} instances, each triggered
 * sequentially when this one is.
 * </p>
 * <p>
 * Any <code>null</code> consumers are simply ignored.
 * </p>
 *
 * @param <T> type being consumed by all consumers
 * @since 2.0.0
 */
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class Consumers<T> extends ArrayList<Consumer<T>>
        implements Consumer<T> {

    private static final long serialVersionUID = 1L;

    public Consumers() {}
    public Consumers(int initialCapacity) {
        super(initialCapacity);
    }
    public Consumers(Collection<? extends Consumer<T>> consumers) {
        super(consumers == null
                ? Collections.emptyList()
                : consumers
                    .stream()
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList()));
    }

    @Override
    public final void accept(T t) {
        forEach(c -> c.accept(t));
    }
}
