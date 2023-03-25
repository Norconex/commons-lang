/* Copyright 2023 Norconex Inc.
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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.Data;

class XMLJaxBTest {

    @Test
    void testJaxb() {
        var pojo = new JaxbPojo();

        pojo.setFirstName("John");
        pojo.setLastName("Smith");
        pojo.setLuckyNumber(7);

        var xml = XML.of("test").create();

        xml.addElement("pojo", pojo);

        Assertions.assertEquals(7, xml.getInteger("pojo/@luckyNumber"));
        Assertions.assertEquals("John", xml.getString("pojo/firstName"));
        Assertions.assertEquals("Smith", xml.getString("pojo/lastName"));

        var newPojo = (JaxbPojo) xml.getObject("pojo");
        Assertions.assertEquals(7, (newPojo).getLuckyNumber());
        Assertions.assertEquals("John", newPojo.getFirstName());
        Assertions.assertEquals("Smith", newPojo.getLastName());
    }

    @XmlRootElement
    @Data
    public static class JaxbPojo {

        private String lastName;
        private String firstName;
        private int luckyNumber = 7;

        @XmlAttribute
        public void setLuckyNumber(int luckyNumber) {
            this.luckyNumber = luckyNumber;
        }
    }
}
