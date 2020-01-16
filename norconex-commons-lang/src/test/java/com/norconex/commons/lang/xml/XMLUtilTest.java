/* Copyright 2020 Norconex Inc.
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

/**
 * @author Pascal Essiembre
 */
public class XMLUtilTest {

    @Test
    public void testNoRootFormat() {
        String xml = "<noroot1><child>I am a child</child></noroot1>"
                + "<noroot2>I am a sibling</noroot2>";
        String expected =
                "<noroot1>\n"
              + "    <child>I am a child</child>\n"
              + "</noroot1>\n"
              + "<noroot2>I am a sibling</noroot2>";

        String actual = XMLUtil.format(xml);
        Assertions.assertEquals(expected, actual);
    }

    @Test
    public void testNoTagFormat() {
        String xml = "onSet=\"[append|prepend|replace|optional]\"";
        String actual = XMLUtil.format(xml);
        Assertions.assertEquals(xml, actual);
    }
}
