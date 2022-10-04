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
package com.norconex.commons.lang.bean;

import static org.apache.commons.lang3.reflect.MethodUtils.getAccessibleMethod;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;

import org.junit.jupiter.api.Test;

import com.norconex.commons.lang.bean.MiscAccessorsBean.Fields;

class FluentPropertyDescriptorTest {

    @Test
    void testConstructors() throws IntrospectionException {
        var expected = new FluentPropertyDescriptor(
                "normal", MiscAccessorsBean.class);

        assertThat(new FluentPropertyDescriptor(new PropertyDescriptor(
                "normal", MiscAccessorsBean.class))).isEqualTo(expected);
        assertThat(new FluentPropertyDescriptor(new PropertyDescriptor(
                "normal",
                null,
                getAccessibleMethod(
                        MiscAccessorsBean.class, "setNormal", String.class))))
            .isEqualTo(expected);
        assertThat(new FluentPropertyDescriptor(new PropertyDescriptor(
                "normal",
                getAccessibleMethod(MiscAccessorsBean.class, "getNormal"),
                null)))
            .isEqualTo(expected);



        assertThat(new FluentPropertyDescriptor(
                "normal",
                getAccessibleMethod(MiscAccessorsBean.class, "getNormal"),
                getAccessibleMethod(
                        MiscAccessorsBean.class, "setNormal", String.class)))
            .isEqualTo(expected);
        assertThat(new FluentPropertyDescriptor(
                "normal",
                null,
                getAccessibleMethod(
                        MiscAccessorsBean.class, "setNormal", String.class)))
            .isEqualTo(expected);
        assertThat(new FluentPropertyDescriptor(
                "normal",
                getAccessibleMethod(MiscAccessorsBean.class, "getNormal"),
                null))
            .isEqualTo(expected);
        assertThat(new FluentPropertyDescriptor(
                "normal", MiscAccessorsBean.class, "getNormal", "setNormal"))
            .isEqualTo(expected);
    }

    @Test
    void testIsReadable() throws IntrospectionException {
        assertReadable(Fields.normal, true);
        assertReadable(Fields.normalSetterNoGetter, false);
        assertReadable(Fields.normalGetterNoSetter, true);
        assertReadable(Fields.fluent, true);
        assertReadable(Fields.fluentSetterNoGetter, false);
        assertReadable(Fields.boolNormal, true);
        assertReadable(Fields.boolSetterNoGetter, false);
        assertReadable(Fields.boolGetterNoSetter, true);
        assertReadable(Fields.boolFluent, true);
        assertReadable(Fields.boolFluentSetterNoGetter, false);
        assertReadable(Fields.compactNormal, true);
        assertReadable(Fields.compactSetterNoGetter, false);
        assertReadable(Fields.compactGetterNoSetter, true);
        assertReadable(Fields.compactFluent, true);
        assertReadable(Fields.compactFluentSetterNoGetter, false);
    }

    @Test
    void testIsWritable() throws IntrospectionException {
        assertWritable(Fields.normal, true);
        assertWritable(Fields.normalSetterNoGetter, true);
        assertWritable(Fields.normalGetterNoSetter, false);
        assertWritable(Fields.fluent, true);
        assertWritable(Fields.fluentSetterNoGetter, true);
        assertWritable(Fields.boolNormal, true);
        assertWritable(Fields.boolSetterNoGetter, true);
        assertWritable(Fields.boolGetterNoSetter, false);
        assertWritable(Fields.boolFluent, true);
        assertWritable(Fields.boolFluentSetterNoGetter, true);
        assertWritable(Fields.compactNormal, true);
        assertWritable(Fields.compactSetterNoGetter, true);
        assertWritable(Fields.compactGetterNoSetter, false);
        assertWritable(Fields.compactFluent, true);
        assertWritable(Fields.compactFluentSetterNoGetter, true);
    }

    @Test
    void testReadValue() throws IntrospectionException {
        var b = new MiscAccessorsBean();
        b.setNormal("a");
        b.setFluent("b");
        b.setBoolFluent(true);
        b.setBoolNormal(true);
        b.compactNormal("c");
        b.compactFluent("d");

        assertReadValue(b, Fields.normal, "a");
        assertReadValue(b, Fields.fluent, "b");
        assertReadValue(b, Fields.boolFluent, true);
        assertReadValue(b, Fields.boolNormal, true);
        assertReadValue(b, Fields.compactNormal, "c");
        assertReadValue(b, Fields.compactFluent, "d");

        // bad property returns null
        assertReadValue(b, "badOne", null);
    }

    @Test
    void testWriteValue() throws IntrospectionException {
        var expected = new MiscAccessorsBean();
        expected.setNormal("a");
        expected.setFluent("b");
        expected.setBoolFluent(true);
        expected.setBoolNormal(true);
        expected.compactNormal("c");
        expected.compactFluent("d");

        var actual = new MiscAccessorsBean();
        writeValue(actual, Fields.normal, "a");
        writeValue(actual, Fields.fluent, "b");
        writeValue(actual, Fields.boolFluent, true);
        writeValue(actual, Fields.boolNormal, true);
        writeValue(actual, Fields.compactNormal, "c");
        writeValue(actual, Fields.compactFluent, "d");

        assertThat(actual).isEqualTo(expected);
    }

    private void assertReadable(String property, boolean readable)
            throws IntrospectionException {
        assertThat(new FluentPropertyDescriptor(
                property, MiscAccessorsBean.class)
            .isReadable())
            .isEqualTo(readable);
    }
    private void assertWritable(String property, boolean writable)
            throws IntrospectionException {
        assertThat(new FluentPropertyDescriptor(
                property, MiscAccessorsBean.class)
            .isWritable())
            .isEqualTo(writable);
    }
    private void assertReadValue(
            MiscAccessorsBean mab, String property, Object expectedValue)
                    throws IntrospectionException {
        assertEquals(expectedValue, new FluentPropertyDescriptor(
                property, MiscAccessorsBean.class)
            .readValue(mab));
    }
    private void writeValue(
            MiscAccessorsBean mab, String property, Object value)
                    throws IntrospectionException {
        new FluentPropertyDescriptor(
                property, MiscAccessorsBean.class).writeValue(mab, value);
    }
}
