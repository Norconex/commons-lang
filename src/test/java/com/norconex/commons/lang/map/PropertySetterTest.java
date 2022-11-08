package com.norconex.commons.lang.map;

import static com.norconex.commons.lang.map.PropertySetter.APPEND;
import static com.norconex.commons.lang.map.PropertySetter.OPTIONAL;
import static com.norconex.commons.lang.map.PropertySetter.PREPEND;
import static com.norconex.commons.lang.map.PropertySetter.REPLACE;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.api.Test;

import com.norconex.commons.lang.xml.XML;

class PropertySetterTest {

    @Test
    void testApply() {
        Properties sampleProps = new Properties(MapUtil.toMap(
            "a", asList("1", "2", "3"),
            "b", asList("4", "5", "6"),
            "abc", asList("7", "8", "9")
        ));

        APPEND.apply(sampleProps, "a", new String[] {"10", "11"});
        assertThat(sampleProps.get("a")).containsExactly(
                "1", "2", "3", "10", "11");
        APPEND.apply(sampleProps, "d", 12);
        assertThat(sampleProps.get("d")).containsExactly("12");
        APPEND.apply(sampleProps, "e", null);
        assertThat(sampleProps.containsKey("e")).isFalse();
        APPEND.apply(sampleProps, null, "13");
        assertThat(sampleProps.get(null)).containsExactly("13");
        assertDoesNotThrow(() -> APPEND.apply(null, "f", "13"));

        PREPEND.apply(sampleProps, "b", "14");
        assertThat(sampleProps.get("b")).containsExactly("14", "4", "5", "6");

        REPLACE.apply(sampleProps, "abc", "0");
        assertThat(sampleProps.get("abc")).containsExactly("0");

        OPTIONAL.apply(sampleProps, "abc", "15");
        assertThat(sampleProps.get("abc")).containsExactly("0");
        OPTIONAL.apply(sampleProps, "g", "16");
        assertThat(sampleProps.get("g")).containsExactly("16");
    }

    @Test
    void testFrom() {
        assertThat(PropertySetter.from("optional")).isSameAs(OPTIONAL);
        assertThat(PropertySetter.from(null)).isNull();
        assertThat(PropertySetter.from("badOne", PREPEND)).isSameAs(PREPEND);
        assertThat(PropertySetter.from(null, PREPEND)).isSameAs(PREPEND);
        assertThat(PropertySetter.from("badOne", null)).isNull();
    }

    @Test
    void testOrPropertySetter() {
        assertThat(PropertySetter.orAppend(OPTIONAL)).isSameAs(OPTIONAL);
        assertThat(PropertySetter.orAppend(null)).isSameAs(APPEND);
        assertThat(PropertySetter.orOptional(APPEND)).isSameAs(APPEND);
        assertThat(PropertySetter.orOptional(null)).isSameAs(OPTIONAL);
        assertThat(PropertySetter.orPrepend(APPEND)).isSameAs(APPEND);
        assertThat(PropertySetter.orPrepend(null)).isSameAs(PREPEND);
        assertThat(PropertySetter.orReplace(APPEND)).isSameAs(APPEND);
        assertThat(PropertySetter.orReplace(null)).isSameAs(REPLACE);
    }

    @Test
    void testSaveLoadXML() {
        XML xml = new XML("test");
        PropertySetter.toXML(xml, REPLACE);

        assertThat(PropertySetter.fromXML(xml, null)).isSameAs(REPLACE);
        assertThat(PropertySetter.fromXML(null, OPTIONAL)).isSameAs(OPTIONAL);
    }
}
