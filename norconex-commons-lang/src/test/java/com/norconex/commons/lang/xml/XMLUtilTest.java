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

    @Test
    public void testLongLinesFormat() {
        String xml =
                "<xml>"
              + "<textMatcher "
              + "class=\"com.norconex.commons.lang.text.TextMatcher\" "
              + "ignoreAccents=\"[false|true]\" ignoreCase=\"[false|true]\" "
              + "matchWhole=\"[false|true]\" "
              + "method=\"[basic|wildcard|regex]\" "
              + "replaceAll=\"[false|true]\">"
              + "<pattern attr1=\"a short one\" attr2=\"another short\">\n"
              + "    Content quite long in the body we do not want to "
              + "break since it could mess up with content format and space "
              + "preservation.\n"
              + "</pattern>"
              + "<text>Another content quite long in the body we do not want "
              + "to break since it could mess up with content format and space "
              + "preservation.</text>"
              + "</textMatcher>"
              + "</xml>";

        String expected =
                "<xml>\n"
              + "  <textMatcher\n"
              + "      class=\"com.norconex.commons.lang.text.TextMatcher\"\n"
              + "      ignoreAccents=\"[false|true]\"\n"
              + "      ignoreCase=\"[false|true]\"\n"
              + "      matchWhole=\"[false|true]\"\n"
              + "      method=\"[basic|wildcard|regex]\"\n"
              + "      replaceAll=\"[false|true]\">\n"
              + "    <pattern attr1=\"a short one\" attr2=\"another short\">\n"
              + "  Content quite long in the body we do not want to break "
              + "since it could mess up with content format and space "
              + "preservation.\n"
              + "</pattern>\n"
              + "    <text>Another content quite long in the body we do not "
              + "want to break since it could mess up with content format "
              + "and space preservation.</text>\n"
              + "  </textMatcher>\n"
              + "</xml>";
        Assertions.assertEquals(expected, XMLUtil.format(xml, 2, 80));
    }
}
