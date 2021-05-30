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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

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
 * @author Pascal Essiembre
 * @since 2.0.0
 */
public class Consumers<T> extends ArrayList<Consumer<T>>
        implements Consumer<T> {

    private static final long serialVersionUID = 1L;

    public Consumers() {
        super();
    }
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
