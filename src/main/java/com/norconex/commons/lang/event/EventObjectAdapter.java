/* Copyright 2022 Norconex Inc.
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
package com.norconex.commons.lang.event;

import static java.util.Objects.requireNonNull;

import java.util.EventObject;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import lombok.Generated;

/*
 * An adapter to facilitate building {@link Event} classes
 * extending {@link EventObject}.
 * Not part of public API.
 * @since 3.0.0
 */
abstract class EventObjectAdapter extends EventObject {
    private static final long serialVersionUID = 1L;

    @lombok.Generated
    protected EventObjectAdapter(
            final EventObjectAdapter.EventObjectAdapterBuilder<?, ?> b) {
        super(requireNonNull(b.source, "source must not be null"));
    }

    // This builder allows subclasses to use Lombok @SuperBuilder
    // even if EventObject is not annotated with @SuperBuilder
    @Generated
    public abstract static class EventObjectAdapterBuilder<
            C extends EventObjectAdapter,
            B extends EventObjectAdapter.EventObjectAdapterBuilder<C, B>> {
        @Generated
        private Object source;
        @Generated
        public abstract C build();
        @Generated
        protected abstract B self();
        @Generated
        public B source(final Object source) {
            this.source = requireNonNull(source, "source must not be null");
            return self();
        }
    }

    // Need to implement equals/hashCode manually as parent does not implement
    // them
    @Override
    public boolean equals(final Object other) {
        if (other == null) { return false; }
        if (other == this) { return true; }
        if (other.getClass() != getClass()) { return false; }
        var event = (Event) other;
        return new EqualsBuilder()
                .append(source, event.source)
                .isEquals();
    }
    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(source)
                .build();
    }
}
