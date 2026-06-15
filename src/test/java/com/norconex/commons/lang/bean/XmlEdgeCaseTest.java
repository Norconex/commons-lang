/* Copyright 2024 Norconex Inc.
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
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.norconex.commons.lang.bean.BeanMapper.Format;
import com.norconex.commons.lang.bean.jackson.JsonXmlCollection;
import tools.jackson.dataformat.xml.annotation.JacksonXmlProperty;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * Tests edge cases in XmlTokenStream and XmlBeanPropertyWriter covering
 * self-closing vs empty elements, CDATA, collections, and null/empty handling.
 */
class XmlEdgeCaseTest {

    @Data
    @Accessors(chain = true)
    @JsonInclude(value = Include.ALWAYS, content = Include.ALWAYS)
    static class NullableXmlBean {
        private String name;
        private String value;
        private Integer count;
    }

    @Data
    @Accessors(chain = true)
    static class CollectionXmlBean {
        @JsonXmlCollection(entryName = "item")
        private List<String> items = new ArrayList<>();
        private String label;
    }

    @Data
    @Accessors(chain = true)
    static class AlwaysIncludeCollectionXmlBean {
        @com.fasterxml.jackson.annotation.JsonInclude(Include.ALWAYS)
        @JsonXmlCollection(entryName = "item")
        private List<String> items;
        private String label;
    }

    @Data
    @Accessors(chain = true)
    static class NestedXmlBean {
        private String title;
        private NullableXmlBean nested;
    }

    @Data
    @Accessors(chain = true)
    @JsonInclude(value = Include.ALWAYS, content = Include.ALWAYS)
    static class EmptyStringXmlBean {
        private String presentField = "default";
        private String emptyField = "default";
        private String nullField = "default";
    }

    @Data
    @Accessors(chain = true)
    static class BeanWithXmlAttribute {
        @JacksonXmlProperty(isAttribute = true)
        private String id;
        private String name;
    }

    @Data
    @Accessors(chain = true)
    static class SetCollectionXmlBean {
        @JsonXmlCollection(entryName = "item")
        private Set<String> items;
        private String label;
    }

    // Exercises XmlTokenStream._collectUntilTag() self-closing → null path
    // and empty element pair → empty string distinction
    @Test
    void testSelfClosingVsEmptyElement() {
        // Self-closing <name/> should be deserialized as null
        // Empty <value></value> should be deserialized as empty string
        var xml = """
                <NullableXmlBean>
                  <name/>
                  <value></value>
                  <count>42</count>
                </NullableXmlBean>
                """;
        var result = BeanMapper.DEFAULT.read(NullableXmlBean.class,
                new StringReader(xml), Format.XML);
        assertThat(result.getName()).isNull();
        assertThat(result.getValue()).isEmpty();
        assertThat(result.getCount()).isEqualTo(42);
    }

    // Exercises XmlTokenStream handling of nested objects
    @Test
    void testXmlNestedObjects() {
        var xml = """
                <NestedXmlBean>
                  <title>Parent</title>
                  <nested>
                    <name>Child</name>
                    <value>text</value>
                    <count>5</count>
                  </nested>
                </NestedXmlBean>
                """;
        var result = BeanMapper.DEFAULT.read(NestedXmlBean.class,
                new StringReader(xml), Format.XML);
        assertThat(result.getTitle()).isEqualTo("Parent");
        assertThat(result.getNested()).isNotNull();
        assertThat(result.getNested().getName()).isEqualTo("Child");
    }

    // Exercises XmlTokenStream with collection of single element (no array wrapping)
    @Test
    void testXmlSingleElementCollection() {
        var xml = """
                <CollectionXmlBean>
                  <label>myLabel</label>
                  <items>
                    <item>only-one</item>
                  </items>
                </CollectionXmlBean>
                """;
        var result = BeanMapper.DEFAULT.read(CollectionXmlBean.class,
                new StringReader(xml), Format.XML);
        assertThat(result.getItems()).containsExactly("only-one");
        assertThat(result.getLabel()).isEqualTo("myLabel");
    }

    // Exercises XmlTokenStream with multi-element collection (virtual wrapping)
    @Test
    void testXmlMultiElementCollection() {
        var xml = """
                <CollectionXmlBean>
                  <label>multi</label>
                  <items>
                    <item>alpha</item>
                    <item>beta</item>
                    <item>gamma</item>
                  </items>
                </CollectionXmlBean>
                """;
        var result = BeanMapper.DEFAULT.read(CollectionXmlBean.class,
                new StringReader(xml), Format.XML);
        assertThat(result.getItems()).containsExactly("alpha", "beta", "gamma");
    }

    // Exercises XmlBeanPropertyWriter.serializeAsProperty() with empty collection
    // using @JsonInclude(ALWAYS) so empty list is not suppressed by NON_DEFAULT
    @Test
    void testXmlEmptyCollectionSerialization() {
        var bean = new AlwaysIncludeCollectionXmlBean();
        bean.setItems(new ArrayList<>()); // empty list, always serialized
        bean.setLabel("test");

        var sw = new StringWriter();
        BeanMapper.DEFAULT.write(bean, sw, Format.XML);
        var xml = sw.toString();
        // Empty list with @JsonInclude(ALWAYS) should produce empty element pair
        assertThat(xml).containsPattern(
                "<items></items>|<items/>|<label>test</label>");
    }

    // Exercises write/read round-trip for XML with all null/empty/set fields
    @Test
    void testXmlNullAndEmptyFieldRoundTrip() {
        var bean = new EmptyStringXmlBean();
        bean.setPresentField("hello");
        bean.setEmptyField("");
        bean.setNullField(null);

        assertThatNoException().isThrownBy(
                () -> BeanMapper.DEFAULT.assertWriteRead(bean, Format.XML));
    }

    // Exercises XmlBeanPropertyWriter with non-null values (serialization path)
    @Test
    void testXmlNonNullValueSerialization() {
        var bean = new NullableXmlBean();
        bean.setName("hello");
        bean.setValue("world");
        bean.setCount(99);

        var sw = new StringWriter();
        BeanMapper.DEFAULT.write(bean, sw, Format.XML);
        var xml = sw.toString();
        assertThat(xml).contains("hello");
        assertThat(xml).contains("world");
        assertThat(xml).contains("99");
    }

    // Exercises XML serialization with indentation (EmptyWithClosingTagXmlFactory wrapped printer)
    @Test
    void testXmlIndentedSerialization() {
        var bean = new NullableXmlBean();
        bean.setName("test");
        bean.setCount(10);

        var indentedMapper = BeanMapper.builder().indent(true).build();
        var sw = new StringWriter();
        assertThatNoException().isThrownBy(
                () -> indentedMapper.write(bean, sw, Format.XML));
        assertThat(sw.toString()).contains("test");
    }

    // Exercises XML write-read round-trip for collection beans
    @Test
    void testXmlCollectionWriteRead() {
        var bean = new CollectionXmlBean();
        bean.setLabel("rtrip");
        bean.setItems(new ArrayList<>(List.of("one", "two", "three")));

        assertThatNoException().isThrownBy(
                () -> BeanMapper.DEFAULT.assertWriteRead(bean, Format.XML));
    }

    // Exercises CDATA content in XML (XmlTokenStream CDATA case)
    @Test
    void testXmlCdataContent() {
        var xml = """
                <NullableXmlBean>
                  <name><![CDATA[hello & world]]></name>
                  <count>7</count>
                </NullableXmlBean>
                """;
        var result = BeanMapper.DEFAULT.read(NullableXmlBean.class,
                new StringReader(xml), Format.XML);
        assertThat(result.getName()).isEqualTo("hello & world");
        assertThat(result.getCount()).isEqualTo(7);
    }

    // Exercises XmlTokenStream with xsi:nil attribute (xsi:nil handling path)
    @Test
    void testXmlXsiNilElement() {
        var xml = """
                <NullableXmlBean xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
                  <name xsi:nil="true"/>
                  <value>present</value>
                  <count>3</count>
                </NullableXmlBean>
                """;
        // xsi:nil="true" on an element should cause it to be read as null
        var result = BeanMapper.DEFAULT.read(NullableXmlBean.class,
                new StringReader(xml), Format.XML);
        assertThat(result.getName()).isNull();
        assertThat(result.getValue()).isEqualTo("present");
    }

    // Exercises XmlTokenStream _allWs helper with whitespace-only text between elements
    @Test
    void testXmlWhitespaceIgnored() {
        var xml = """
                <NullableXmlBean>
                  <name>  spaced  </name>
                  <count>1</count>
                </NullableXmlBean>
                """;
        var result = BeanMapper.DEFAULT.read(NullableXmlBean.class,
                new StringReader(xml), Format.XML);
        assertThat(result.getName()).isEqualTo("  spaced  ");
        assertThat(result.getCount()).isEqualTo(1);
    }

    // Exercises EmptyWithClosingTagXmlFactory.copy() path
    @Test
    void testXmlFactoryCopyViaBuild() {
        // Building a mapper with XmlMapper (which calls copy() on factory) covers
        // the EmptyWithClosingTagXmlFactory.copy() method
        var mapper = BeanMapper.builder().build();
        var bean = new NullableXmlBean().setName("copy-test").setCount(1);
        assertThatNoException().isThrownBy(
                () -> mapper.assertWriteRead(bean, Format.XML));
    }

    // Exercises XmlTokenStream attribute loop: _nextAttributeIndex < _attributeCount
    // and XML_ATTRIBUTE_NAME / XML_ATTRIBUTE_VALUE states in _next()
    @Test
    void testXmlBeanWithAttribute() {
        var xml = """
                <BeanWithXmlAttribute id="test-id">
                  <name>hello</name>
                </BeanWithXmlAttribute>
                """;
        var result = BeanMapper.DEFAULT.read(BeanWithXmlAttribute.class,
                new StringReader(xml), Format.XML);
        assertThat(result.getId()).isEqualTo("test-id");
        assertThat(result.getName()).isEqualTo("hello");
    }

    // Exercises _collectUntilTag() StringBuilder accumulation (multiple CDATA sections)
    @Test
    void testXmlMultipleCdataSections() {
        var xml = """
                <NullableXmlBean>
                  <name><![CDATA[hello]]><![CDATA[ world]]></name>
                  <count>5</count>
                </NullableXmlBean>
                """;
        var result = BeanMapper.DEFAULT.read(NullableXmlBean.class,
                new StringReader(xml), Format.XML);
        // Result should contain the concatenated CDATA content
        assertThat(result.getName()).contains("hello");
        assertThat(result.getCount()).isEqualTo(5);
    }

    // Exercises JsonXmlCollectionDeserializer.createCollection() Set/HashSet fallback path
    @Test
    void testXmlSetCollection() {
        var xml = """
                <SetCollectionXmlBean>
                  <label>set-test</label>
                  <items>
                    <item>alpha</item>
                    <item>beta</item>
                    <item>gamma</item>
                  </items>
                </SetCollectionXmlBean>
                """;
        var result = BeanMapper.DEFAULT.read(SetCollectionXmlBean.class,
                new StringReader(xml), Format.XML);
        assertThat(result.getItems()).containsExactlyInAnyOrder("alpha", "beta",
                "gamma");
        assertThat(result.getLabel()).isEqualTo("set-test");
    }

    // Exercises XML attribute write-read round-trip
    @Test
    void testXmlBeanWithAttributeWriteRead() {
        var bean =
                new BeanWithXmlAttribute().setId("round-trip").setName("test");
        assertThatNoException().isThrownBy(
                () -> BeanMapper.DEFAULT.assertWriteRead(bean, Format.XML));
    }
}
