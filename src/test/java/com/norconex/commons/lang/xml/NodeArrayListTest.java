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

import org.junit.jupiter.api.Test;

class NodeArrayListTest {

    @Test
    void testNodeArrayList() {
        Xml xml = new Xml("<test><value>a</value><value>b</value></test>");

        NodeArrayList node1 = new NodeArrayList(xml.getNode());
        assertThat(node1.getLength()).isEqualTo(2);

        NodeArrayList node2 = new NodeArrayList(node1);
        assertThat(node2.getLength()).isEqualTo(2);
    }
}
