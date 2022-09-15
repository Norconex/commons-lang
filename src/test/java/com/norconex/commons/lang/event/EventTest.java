package com.norconex.commons.lang.event;

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
}
