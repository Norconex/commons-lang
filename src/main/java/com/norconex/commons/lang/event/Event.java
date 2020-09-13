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

import java.util.EventObject;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import com.norconex.commons.lang.EqualsUtil;

/**
 * An immutable event.
 * @author Pascal Essiembre
 * @since 2.0.0
 * @see IEventListener
 */
public class Event extends EventObject {

    private static final long serialVersionUID = 1L;
    private final String name;
    private final String message;
    private final transient Throwable exception;


    public static class Builder<B extends Builder<B>> {

        private final String name;
        private final Object source;
        private String message;
        private Throwable exception;

        /**
         * New event builder. Name and source cannot be <code>null</code>.
         * @param name event name
         * @param source object responsible for triggering the event
         */
        public Builder(String name, Object source) {
            this.name = name;
            this.source = source;
        }

        public B message(String message) {
            this.message = message;
            return self();
        }
        public B exception(Throwable exception) {
            this.exception = exception;
            return self();
        }

        public Event build() {
            return new Event(this);
        }

        @SuppressWarnings("unchecked")
        private B self() {
            return (B) this;
        }
    }

    protected Event(Builder<?> b) {
        super(b.source);
        this.name = Objects.requireNonNull(b.name, "'name' must not be null.");
        this.message = b.message;
        this.exception = b.exception;
    }

    /**
     * Gets the object representing the source of this event.
     * @return the subject
     */
    @Override
    public Object getSource() {
        return source;
    }

    /**
     * Gets the event name.
     * @return the event name
     */
    public String getName() {
        return name;
    }

    /**
     * Gets a message describing the event or giving precision.
     * @return message the messsage
     */
    public String getMessage() {
        return message;
    }

    /**
     * Gets the exception, if any.
     * @return the exception or <code>null</code>
     */
    public Throwable getException() {
        return exception;
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


    @Override
    public boolean equals(final Object other) {
        return EqualsBuilder.reflectionEquals(this, other);
    }
    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }
    /**
     * Returns the event message, if set, or the return value of
     * <code>toString()</code> on source.
     */
    @Override
    public String toString() {
        if (StringUtils.isBlank(message)) {
            return Objects.toString(source);
        }
        return message;
    }
}
