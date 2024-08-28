package com.norconex.commons.lang.xml.flow;

import java.util.Arrays;

/**
 * An XML tag used in flow resolution.
 * @since 2.0.0
 */
enum Tag {
    IF("if"),
    IFNOT("ifNot"),
    CONDITIONS("conditions"),
    CONDITION("condition"),
    THEN("then"),
    ELSE("else");
    ;

    private final String name;

    Tag(String name) {
        this.name = name;
    }

    public boolean is(String tagName) {
        return name.equals(tagName);
    }

    @Override
    public String toString() {
        return name;
    }

    public boolean isAny(Tag... tags) {
        for (Tag tag : tags) {
            if (tag == this) {
                return true;
            }
        }
        return false;
    }

    public static boolean isAny(String tagName, Tag... tags) {
        for (Tag tag : tags) {
            if (tag.is(tagName)) {
                return true;
            }
        }
        return false;
    }

    public static Tag of(String tagName) {
        return Arrays.stream(values())
                .filter(t -> t.is(tagName))
                .findFirst()
                .orElse(null);
    }
}