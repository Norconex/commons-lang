/* Copyright 2026 Norconex Inc.
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import com.norconex.commons.lang.bean.BeanMapper.Format;
import com.norconex.commons.lang.bean.stubs.AutomobileConfig;
import com.norconex.commons.lang.bean.stubs.ClearableDefaults;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * What is written for a property, depending on what it holds and what its
 * default is. The rule, which a configuration depends on to switch a default
 * off:
 * <ul>
 *   <li>left at its non-null default: not written (the reader uses the
 *       default);</li>
 *   <li>cleared to null while its default is non-null: written, as null, so
 *       reading it back clears the default too;</li>
 *   <li>null while its default is null: not written;</li>
 *   <li>anything else: written when it differs from the default.</li>
 * </ul>
 */
class ClearedDefaultTest {

    private static JsonNode written(Object bean) {
        var writer = new StringWriter();
        BeanMapper.DEFAULT.write(bean, writer, Format.JSON);
        return JsonMapper.builder().build().readTree(writer.toString());
    }

    private static ClearableDefaults roundTrip(
            ClearableDefaults bean, Format format) {
        var writer = new StringWriter();
        BeanMapper.DEFAULT.write(bean, writer, format);
        return BeanMapper.DEFAULT.read(ClearableDefaults.class,
                new StringReader(writer.toString()), format);
    }

    //--- The use case: null where the default is non-null ---

    @Test
    void clearingANonNullDefaultWithNullIsWrittenAsNull() {
        var bean = new ClearableDefaults().setText(null);

        var json = written(bean);

        assertThat(json.has("text")).isTrue();
        assertThat(json.get("text").isNull()).isTrue();
    }

    @ParameterizedTest
    @EnumSource(Format.class)
    void aClearedDefaultStaysClearedWhenReadBack(Format format) {
        var bean = new ClearableDefaults().setText(null);

        var back = roundTrip(bean, format);

        // not "default-text" again: the null made it through
        assertThat(back.getText()).isNull();
        assertThatNoException().isThrownBy(
                () -> BeanMapper.DEFAULT.assertWriteRead(bean, format));
    }

    @Test
    void clearingADefaultObjectWithNullIsWrittenAsNull() {
        var bean = new ClearableDefaults().setObject(null);

        var json = written(bean);

        assertThat(json.has("object")).isTrue();
        assertThat(json.get("object").isNull()).isTrue();
    }

    @Test
    void clearingABoxedDefaultWithNullIsWrittenAsNull() {
        var bean = new ClearableDefaults().setBoxed(null);

        var json = written(bean);

        assertThat(json.has("boxed")).isTrue();
        assertThat(json.get("boxed").isNull()).isTrue();
    }

    @Test
    void clearingADefaultListWithNullIsWrittenAsNull() {
        var bean = new ClearableDefaults().setList(null);

        var json = written(bean);

        assertThat(json.has("list")).isTrue();
        assertThat(json.get("list").isNull()).isTrue();
    }

    @ParameterizedTest
    @EnumSource(Format.class)
    void clearingADefaultListWithAnEmptyOneIsWrittenAndRoundTrips(
            Format format) {
        var bean = new ClearableDefaults().setList(new java.util.ArrayList<>());

        assertThat(written(bean).get("list").isArray()).isTrue();
        assertThat(written(bean).get("list")).isEmpty();
        assertThat(roundTrip(bean, format).getList()).isEmpty();
    }

    @Test
    void clearingADefaultArrayWithNullIsWrittenAsNull() {
        var bean = new ClearableDefaults().setArray(null);

        var json = written(bean);

        assertThat(json.has("array")).isTrue();
        assertThat(json.get("array").isNull()).isTrue();
    }

    @ParameterizedTest
    @EnumSource(Format.class)
    void everythingClearedAtOnceRoundTrips(Format format) {
        var bean = new ClearableDefaults()
                .setText(null)
                .setBoxed(null)
                .setList(null)
                .setArray(null)
                .setObject(null);

        var back = roundTrip(bean, format);

        assertThat(back.getText()).isNull();
        assertThat(back.getBoxed()).isNull();
        assertThat(back.getList()).isNull();
        assertThat(back.getArray()).isNull();
        assertThat(back.getObject()).isNull();
        assertThatNoException().isThrownBy(
                () -> BeanMapper.DEFAULT.assertWriteRead(bean, format));
    }

    //--- Not written: the reader will use the default ---

    @Test
    void aPropertyLeftAtItsNonNullDefaultIsNotWritten() {
        var json = written(new ClearableDefaults());

        assertThat(json.has("text")).isFalse();
        assertThat(json.has("primitive")).isFalse();
        assertThat(json.has("boxed")).isFalse();
        assertThat(json.has("list")).isFalse();
        assertThat(json.has("array")).isFalse();
        assertThat(json.has("object")).isFalse();
    }

    @Test
    void aNullWhoseDefaultIsNullIsNotWritten() {
        var json = written(new ClearableDefaults());

        assertThat(json.has("noDefault")).isFalse();
    }

    @Test
    void aNullWhoseDefaultIsNullStaysNotWrittenWhateverElseChanges() {
        var json =
                written(new ClearableDefaults().setText(null).setPrimitive(1));

        assertThat(json.has("noDefault")).isFalse();
    }

    //--- Written: a real change ---

    @Test
    void aPropertyChangedToAnotherValueIsWritten() {
        var bean = new ClearableDefaults()
                .setText("other")
                .setPrimitive(9)
                .setBoxed(8)
                .setList(new java.util.ArrayList<>(List.of("c")))
                .setArray(new String[] { "z" })
                .setObject(new AutomobileConfig().setModel("Camry"))
                .setNoDefault("now set");

        var json = written(bean);

        assertThat(json.get("text").asString()).isEqualTo("other");
        assertThat(json.get("primitive").asInt()).isEqualTo(9);
        assertThat(json.get("boxed").asInt()).isEqualTo(8);
        assertThat(json.get("list")).hasSize(1);
        assertThat(json.get("array")).hasSize(1);
        assertThat(json.get("object").get("model").asString())
                .isEqualTo("Camry");
        assertThat(json.get("noDefault").asString()).isEqualTo("now set");
    }

    @Test
    void aPrimitiveSetToZeroWhenItsDefaultIsNotIsWritten() {
        var json = written(new ClearableDefaults().setPrimitive(0));

        assertThat(json.has("primitive")).isTrue();
        assertThat(json.get("primitive").asInt()).isZero();
    }

    //--- Explicit inclusion rules still win ---

    @Test
    void anExplicitNonNullInclusionNeverWritesNull() {
        var json = written(new ClearableDefaults().setNeverNull(null));

        assertThat(json.has("neverNull")).isFalse();
    }

    @Test
    void anExplicitAlwaysInclusionAlwaysWrites() {
        assertThat(written(new ClearableDefaults()).has("always")).isTrue();

        var json = written(new ClearableDefaults().setAlways(null));
        assertThat(json.has("always")).isTrue();
        assertThat(json.get("always").isNull()).isTrue();
    }

    @Test
    void aPropertyWrittenOnPurposeIsNotLostToTheDefaultTheNextTime() {
        // the end result for a configuration: write, read, and write again,
        // and what was cleared is still cleared
        var first = new ClearableDefaults().setText(null);

        var second = roundTrip(first, Format.JSON);
        var third = roundTrip(second, Format.YAML);

        assertThat(third.getText()).isNull();
        assertThat(third).isEqualTo(roundTrip(third, Format.XML));
    }
}
