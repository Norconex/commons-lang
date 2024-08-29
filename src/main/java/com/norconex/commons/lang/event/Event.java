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

import java.util.Objects;

import org.apache.commons.lang3.StringUtils;

import com.norconex.commons.lang.EqualsUtil;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.experimental.SuperBuilder;

/**
 * An immutable event.
 * @since 2.0.0
 * @see EventListener
 */
@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class Event extends EventObjectAdapter {

    private static final long serialVersionUID = 1L;
    /**
     * The event name (never <code>null</code>).
     */
    @NonNull
    private final String name;
    /**
     * A message describing the event or giving precision,
     * or <code>null</code> if the event has no message.
     */
    private final String message;
    /**
     * The exception associated with this event, or <code>null</code>.
     */
    private final transient Throwable exception;

    /**
     * Gets whether the supplied event has the same name as this event.
     * Equivalent to invoking <code>thisEvent.is(thatEvent.getName())</code>.
     * @param event event to compare
     * @return <code>true</code> if events share the same name.
     */
    public boolean is(Event event) {
        if (event == null) {
            return false;
        }
        return is(event.getName());
    }

    /**
     * Gets whether this event has the same name as any of the supplied names.
     * @param eventName event names
     * @return <code>true</code> if this event name matches one of supplied ones
     */
    public boolean is(String... eventName) {
        return EqualsUtil.equalsAny(name, (Object[]) eventName);
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
}
