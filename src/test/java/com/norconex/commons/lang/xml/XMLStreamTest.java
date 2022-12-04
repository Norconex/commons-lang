/* Copyright 2021 Norconex Inc.
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

import static com.norconex.commons.lang.ResourceLoader.getXmlString;

import java.io.IOException;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.norconex.commons.lang.ResourceLoader;

/**
 */
class XMLStreamTest {

    @Test
    void testStream() throws IOException {
        // 62 elements
        Assertions.assertEquals(62, stream().count());

        // 51 nodes have text
        Assertions.assertEquals(51, stream()
                .map(XMLCursor::readText)
                .filter(StringUtils::isNotBlank)
                .count());

        // 3 elements have one or more attributes
        Assertions.assertEquals(3, stream()
                .map(XMLCursor::getAttributes)
                .filter(p -> !p.isEmpty())
                .count());

        // 1 book ISBN is 0000-0000
        Assertions.assertTrue("0000-0000".equals(stream()
                .filter(c -> c.getLocalName().equals("book"))
                .findFirst()
                .map(c -> c.getAttributes().getString("isbn"))
                .orElse(null)));

        // 4 plants elements, from which:
        //     2 plants have "botanic:" prefix
        //     1 plant has "agriculture:" prefix
        Assertions.assertEquals(4, stream()
                .filter(c -> c.getLocalName().equals("plant"))
                .count());
        Assertions.assertEquals(2, stream()
                .filter(c -> c.getName().equals("botanic:plant"))
                .count());
        Assertions.assertEquals(1, stream()
                .filter(c -> c.getName().equals("agriculture:plant"))
                .count());

        // Same as previous test, but using path
        Assertions.assertEquals(4, stream()
                .filter(c -> c.getLocalPath().equals("/catalog/plant"))
                .count());
        Assertions.assertEquals(2, stream()
                .filter(c -> c.getPath().equals("/catalog/botanic:plant"))
                .count());
        Assertions.assertEquals(1, stream()
                .filter(c -> c.getPath().equals("/catalog/agriculture:plant"))
                .count());
    }

    @Test
    void testIterateReadAsXML() throws IOException {
        int totalPlantPrice = 0;
        int totalBookPages = 0;
        for (XMLCursor c : new XML(getXmlString(XMLStreamTest.class))) {
            if (c.getLocalName().equals("plant")) {
                totalPlantPrice += c.readAsXML().getInteger("price");
            } else if (c.getLocalName().equals("book")) {
                totalBookPages += c.readAsXML().getInteger("@pageCount");
            }
        }

        // total plant price: $1208
        Assertions.assertEquals(1208, totalPlantPrice);
        // total book pages: 303
        Assertions.assertEquals(303, totalBookPages);
    }

    private Stream<XMLCursor> stream() {
        return XML.stream(ResourceLoader.getXmlString(XMLStreamTest.class));
    }
}
