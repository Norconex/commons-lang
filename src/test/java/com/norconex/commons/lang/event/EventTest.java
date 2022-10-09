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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class EventTest {

    @Test
    void testEvent() {
        var name = "MY_EVENT_NAME";

        var event1 = Event.builder(name, "A source")
                .exception(new RuntimeException("Error message."))
                .message("Regular message")
                .build();

        assertEquals(name, event1.getName());
        assertEquals("A source", event1.getSource());
        assertEquals("Regular message", event1.getMessage());
        assertEquals("Error message.", event1.getException().getMessage());
        assertEquals(name + " - Regular message - "
                + "java.lang.RuntimeException: Error message.",
                event1.toString());

        var event2 = Event.builder(name, "Another source").build();

        assertTrue(event1.is(event2));
        assertTrue(event2.is(event1));
        assertTrue(event1.is("different name", name, "another"));
        assertFalse(event1.is((Event) null));

        assertThrows(NullPointerException.class,
                () -> Event.builder(null, "not null"));
        assertThrows(NullPointerException.class,
                () -> Event.builder("not null", null));
        assertEquals(name + " - Another source", event2.toString());

        var event3 = Event.builder(name, "No exception message")
                .exception(new RuntimeException())
                .build();
        assertEquals(name + " - No exception message - "
                + "java.lang.RuntimeException", event3.toString());

    }

    @Test
    void testEquality() {
        Event event1 = Event.builder("blah", "sourceBlah").build();
        Event event2 = Event.builder("blah", "sourceBlah").build();
        Event event3 = Event.builder("nope", "sourceNope").build();
        assertThat(event1).isEqualTo(event2);
        assertThat(event1.hashCode()).isEqualTo(event2.hashCode());
        assertThat(event1).isNotEqualTo(event3);
        assertThat(event1.hashCode()).isNotEqualTo(event3.hashCode());
    }
}
