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
import java.io.Reader;
import java.util.function.Consumer;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.norconex.commons.lang.ResourceLoader;
import com.norconex.commons.lang.map.Properties;
import com.norconex.commons.lang.xml.XML;

import lombok.extern.slf4j.Slf4j;

/**
 */
@Slf4j
class XMLFlowTest {

    @Test
    void testEmptyNullError() throws IOException {
        assertThatNoException().isThrownBy(() -> {
            new XMLFlow<>();
        });
    }

    @Test
    void testCondition() {
        XMLCondition<Integer> cnd1 = new XMLCondition<>(new XMLFlow<>());
        assertThat(cnd1.test(null)).isFalse(); // nothing to test, false
        assertThat(cnd1.test(123)).isTrue(); // no predicate, always true
    }

    @Test
    void testMisc() {
        XMLFlow<Properties> flow = createXMLFlow();
        assertThat(flow.getConsumerAdapter()).isEqualTo(
                MockXMLFlowConsumerAdapter.class);
        assertThat(flow.getPredicateAdapter()).isEqualTo(
                MockXMLFlowPredicateAdapter.class);
        assertThat(flow.parse(new XML("test"))).isNull();

        XMLFlow<Properties> flow2 = new XMLFlow<>();
        assertThatExceptionOfType(XMLFlowException.class)
                .isThrownBy(() -> flow2.parse( //NOSONAR
                        new XML("<test><value>1</value></test>")))
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
        XMLFlow<Properties> flow = createXMLFlow();
        XML sourceXML = sampleXML();
        Consumer<Properties> c = flow.parse(sourceXML);
        XML writtenXML = new XML("<xml/>");
        flow.write(writtenXML, c);
        LOG.debug("{}", writtenXML.toString(2));
        testFlow(writtenXML);
    }

    private void testFlow(XML xml) throws IOException {
        XMLFlow<Properties> flow = createXMLFlow();
        Consumer<Properties> c;
        try (Reader r = ResourceLoader.getXmlReader(getClass())) {
            c = flow.parse(xml);
        }

        Properties data1 = new Properties();
        data1.add("firstName", "John");
        data1.add("lastName", "Smith");
        data1.set("car", "volvo");
        c.accept(data1);

        // first name uppercase
        Assertions.assertEquals("JOHN", data1.getString("firstName"));
        // last name unchanged
        Assertions.assertEquals("Smith", data1.getString("lastName"));

        Properties data2 = new Properties();
        data2.add("firstName", "John");
        data2.add("lastName", "Smith");
        data2.set("car", "toyota");
        c.accept(data2);

        // first name lowercase
        Assertions.assertEquals("john", data2.getString("firstName"));
        // last name uppercase
        Assertions.assertEquals("SMITH", data2.getString("lastName"));
    }

    private XML sampleXML() throws IOException {
        try (Reader r = ResourceLoader.getXmlReader(getClass())) {
            return new XML(r);
        }
    }

    static XMLFlow<Properties> createXMLFlow() {
        return new XMLFlow<>(
                MockXMLFlowConsumerAdapter.class,
                MockXMLFlowPredicateAdapter.class);
    }
}
