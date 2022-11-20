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
package com.norconex.commons.lang.xml;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;
import javax.xml.stream.util.EventReaderDelegate;

import org.junit.jupiter.api.Test;

class XMLIteratorTest {

    @Test
    void testXMLIterator() {
        List<String> tags = new ArrayList<>();
        List<String> paths = new ArrayList<>();
        for (XMLCursor xmlCursor : new XML(
                "<test>"
              +   "<block>"
              +     "<value>1</value>"
              +     "<value>2</value>"
              +   "</block>"
              +   "<block>"
              +     "<value>3</value>"
              +   "</block>"
              + "</test>")) {
            tags.add(xmlCursor.getLocalName());
            paths.add(xmlCursor.getPath());
        }
        assertThat(tags).containsExactly(
                "test", "block", "value", "value", "block", "value");
        assertThat(paths).containsExactly(
                "/test",
                "/test/block",
                "/test/block/value",
                "/test/block/value",
                "/test/block",
                "/test/block/value");
    }

    @Test
    void testErrors() {
        Iterator<XMLCursor> cur = new XML("<test/>").iterator();
        cur.next();
        assertThatExceptionOfType(NoSuchElementException.class)
            .isThrownBy(() -> cur.next());

        Iterator<XMLCursor> badCur = new XML("<test/>") {
            @Override
            public Iterator<XMLCursor> iterator() {
                return new XMLIterator(new EventReaderDelegate() {
                    @Override
                    public boolean hasNext() {
                        return true;
                    }
                    @Override
                    public XMLEvent peek() throws XMLStreamException {
                        throw new XMLStreamException("Just testing.");
                    }
                });
            }
        }.iterator();
        assertThatExceptionOfType(XMLException.class)
            .isThrownBy(() -> badCur.hasNext());
    }
}
