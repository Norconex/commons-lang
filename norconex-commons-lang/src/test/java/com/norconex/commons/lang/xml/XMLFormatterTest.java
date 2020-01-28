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
public class XMLFormatterTest {

    @Test
    public void testNoRootFormat() {
        String xml = "<noroot1><child>I am a child</child></noroot1>"
                + "<noroot2>I am a sibling</noroot2>";
        String expected =
                "<noroot1>\n"
              + "    <child>I am a child</child>\n"
              + "</noroot1>\n"
              + "<noroot2>I am a sibling</noroot2>\n";

        Assertions.assertEquals(expected,
                new XMLFormatter()
                    .setIndentSize(4)
                    .setWrapAttributesAt(1)
                    .setWrapContentAt(80)
                    .format(xml));
    }

    @Test
    public void testNoTagFormat() {
        String xml = "onSet=\"[append|prepend|replace|optional]\"";
        Assertions.assertEquals(xml,
                new XMLFormatter()
                    .setIndentSize(4)
                    .setWrapAttributesAt(1)
                    .setWrapContentAt(80)
                    .format(xml));
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
              + "    Content quite long in the body we want to "
              + "break even if it could mess up with content format and space "
              + "preservation.\n\n     \n  "
              + "</pattern>"
              + "<!-- this is a comment -->"
              + "<text>Another content quite\n long in the body we want "
              + "to break even if it could mess up with content format and "
              + "space preservation.</text>"
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
              + "    <pattern\n"
              + "        attr1=\"a short one\"\n"
              + "        attr2=\"another short\">\n"
              + "      Content quite long in the body we want to break even "
              + "if it could\n"
              + "      mess up with content format and space preservation.\n"
              + "    </pattern>\n"
              + "    <text>\n"
              + "      Another content quite\n"
              + "      long in the body we want to break even if it could "
              + "mess up with\n"
              + "      content format and space preservation.\n"
              + "    </text>\n"
              + "  </textMatcher>\n"
              + "</xml>\n";
        XMLFormatter f = new XMLFormatter();
        f.setIndentSize(2);
        f.setWrapAttributesAt(1);
        f.setWrapContentAt(70);
//System.out.println(f.format(xml));
        Assertions.assertEquals(expected, f.format(xml));
    }
}
