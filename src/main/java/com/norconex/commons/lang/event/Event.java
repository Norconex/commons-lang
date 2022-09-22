/* Copyright 2018-2020 Norconex Inc.
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

import static org.apache.commons.lang3.StringUtils.isBlank;

import java.util.EventObject;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import com.norconex.commons.lang.EqualsUtil;

import lombok.AccessLevel;
import lombok.Data;
import lombok.NonNull;
import lombok.Setter;

/**
 * An immutable event.
 * @author Pascal Essiembre
 * @since 2.0.0
 * @see IEventListener
 */
@Data
@Setter(value = AccessLevel.NONE)
public class Event extends EventObject {

    private static final long serialVersionUID = 1L;
    /**
     * Gets the event name (never <code>null</code>).
     * @return event name
     */
    @SuppressWarnings("javadoc")
    private final String name;
    /**
     * Gets a message describing the event or giving precision,
     * or <code>null</code> if the event has no message.
     * @return event message
     */
    @SuppressWarnings("javadoc")
    private final String message;
    /**
     * Gets the exception associated with this event, or <code>null</code>.
     * @return event message
     */
    @SuppressWarnings("javadoc")
    private final transient Throwable exception;

    protected Event(Builder<?> b) {
        super(b.source);
        name = b.name;
        message = b.message;
        exception = b.exception;
    }

    public boolean is(Event event) {
        if (event == null) {
            return false;
        }
        return is(event.getName());
    }
    public boolean is(String... eventName) {
        return EqualsUtil.equalsAny(name, (Object[]) eventName);
    }

    /**
     * New event builder. Name and source cannot be <code>null</code>.
     * @param name event name
     * @param source object on which the event initially occurred
     * @return event builder
     * @since 3.0.0
     */
    @SuppressWarnings("rawtypes")
    public static Builder builder(
            @NonNull String name, @NonNull Object source) {
        return new Builder<>(name, source);
    }

    @Override
    public boolean equals(final Object other) {
        if (other == null) { return false; }
        if (other == this) { return true; }
        if (other.getClass() != getClass()) { return false; }
        var event = (Event) other;
        return new EqualsBuilder()
                .append(name, event.name)
                .append(message, event.message)
                .append(exception, event.exception)
                .append(source, event.source)
                .isEquals();
    }
    @Override
    public int hashCode() {
        // Need to build manually as parent does not implement hashCode
        return new HashCodeBuilder()
                .append(name)
                .append(message)
                .append(exception)
                .append(source)
                .build();
    }

    /**
     * A string representation of this event.
     */
    @Override
    public String toString() {
        return new StringBuilder()
            .append(name)
            .append(" - ")
            .append(isBlank(message) ? Objects.toString(source) : message)
            .append(exceptionAsString())
            .toString();
    }
    private String exceptionAsString() {
        if (exception == null) {
            return StringUtils.EMPTY;
        }
        var msg = " - " + exception.getClass().getName();
        if (StringUtils.isNotBlank(exception.getMessage())) {
            msg += ": " + exception.getMessage();
        }
        return msg;
    }

    /**
     * Event builder.
     * @param <B> generic self referencing to allow sub-classing.
     */
    public static class Builder<B extends Builder<B>> {

        private final String name;
        private final Object source;
        private String message;
        private Throwable exception;

        /**
         * New event builder. Name and source cannot be <code>null</code>.
         * @param name event name
         * @param source object on which the event initially occurred
         * @deprecated The visibility of this constructor will be reduced
         *     in a future release. Since 3.0.0, use
         *     {@link Event#builder(String, Object)} instead.
         */
        @Deprecated
        public Builder(String name, Object source) {
            this.name = name;
            this.source = source;
        }
        /**
         * Sets a message describing the event or giving precision.
         * @param message event message
         * @return this builder
         */
        public B message(String message) {
            this.message = message;
            return self();
        }
        /**
         * Sets an exception associated with this event.
         * @param exception the exception
         * @return this builder
         */
        public B exception(Throwable exception) {
            this.exception = exception;
            return self();
        }
        /**
         * Builds the event.
         * @return the event
         */
        public Event build() {
            return new Event(this);
        }

        @SuppressWarnings("unchecked")
        private B self() {
            return (B) this;
        }
    }
}
