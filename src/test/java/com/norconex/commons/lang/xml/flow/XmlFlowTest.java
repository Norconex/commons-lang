/* Copyright 2021-2022 Norconex Inc.
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
package com.norconex.commons.lang.xml.flow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;

import java.io.IOException;
import java.util.function.Consumer;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.norconex.commons.lang.ResourceLoader;
import com.norconex.commons.lang.map.Properties;
import com.norconex.commons.lang.xml.Xml;

import lombok.extern.slf4j.Slf4j;

/**
 */
@Slf4j
class XmlFlowTest {

    @Test
    void testEmptyNullError() throws IOException {
        assertThatNoException().isThrownBy(() -> {
            new XmlFlow<>();
        });
    }

    @Test
    void testCondition() {
        var cnd1 = new XmlCondition<Integer>(new XmlFlow<>());
        assertThat(cnd1.test(null)).isFalse(); // nothing to test, false
        assertThat(cnd1.test(123)).isTrue(); // no predicate, always true
    }

    @Test
    void testMisc() {
        var flow = createXMLFlow();
        assertThat(flow.getConsumerAdapter()).isEqualTo(
                MockXmlFlowConsumerAdapter.class);
        assertThat(flow.getPredicateAdapter()).isEqualTo(
                MockXmlFlowPredicateAdapter.class);
        assertThat(flow.parse(new Xml("test"))).isNull();

        var flow2 = new XmlFlow<Properties>();
        assertThatExceptionOfType(XmlFlowException.class)
                .isThrownBy(() -> flow2.parse( //NOSONAR
                        new Xml("<test><value>1</value></test>")))
                .withStackTraceContaining("does not resolve to ");
    }

    @Test
    void testFlow() throws IOException {
        testFlow(sampleXML());
    }

    @Test
    void testWriteRead() throws IOException {
        // to test and it was written properly, we load it back and execute
        // the same test again and it should still work just fine.
        var flow = createXMLFlow();
        var sourceXML = sampleXML();
        var c = flow.parse(sourceXML);
        var writtenXML = new Xml("<xml/>");
        flow.write(writtenXML, c);
        LOG.debug("{}", writtenXML.toString(2));
        testFlow(writtenXML);
    }

    private void testFlow(Xml xml) throws IOException {
        var flow = createXMLFlow();
        Consumer<Properties> c;
        try (var r = ResourceLoader.getXmlReader(getClass())) {
            c = flow.parse(xml);
        }

        var data1 = new Properties();
        data1.add("firstName", "John");
        data1.add("lastName", "Smith");
        data1.set("car", "volvo");
        c.accept(data1);

        // first name uppercase
        Assertions.assertEquals("JOHN", data1.getString("firstName"));
        // last name unchanged
        Assertions.assertEquals("Smith", data1.getString("lastName"));

        var data2 = new Properties();
        data2.add("firstName", "John");
        data2.add("lastName", "Smith");
        data2.set("car", "toyota");
        c.accept(data2);

        // first name lowercase
        Assertions.assertEquals("john", data2.getString("firstName"));
        // last name uppercase
        Assertions.assertEquals("SMITH", data2.getString("lastName"));
    }

    private Xml sampleXML() throws IOException {
        try (var r = ResourceLoader.getXmlReader(getClass())) {
            return new Xml(r);
        }
    }

    static XmlFlow<Properties> createXMLFlow() {
        return new XmlFlow<>(
                MockXmlFlowConsumerAdapter.class,
                MockXmlFlowPredicateAdapter.class);
    }
}
