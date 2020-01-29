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
package com.norconex.commons.lang.javadoc;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;

import com.norconex.commons.lang.xml.XMLFormatter;
import com.sun.javadoc.Tag;
import com.sun.tools.doclets.Taglet;

/**
 * <p>{&#64;nx.html} HTML beautifier with enhanced functionality.
 * HTML has to follow basic XML syntax.</p>
 *
 * @author Pascal Essiembre
 * @since 2.0.0
 * @see XMLTaglet
 */
public class HTMLTaglet extends AbstractInlineTaglet {

    public static final String NAME = "nx.html";

    /**
     * Register an instance of this taglet.
     * @param tagletMap registry of taglets
     */
    public static void register(Map<String, Taglet> tagletMap) {
        tagletMap.put(NAME, new HTMLTaglet());
    }

    @Override
    public String getName() {
        return NAME;
    }

    protected String getHeading(String text, String id) {
        return null;
    }

    @Override
    protected String toString(Tag tag, String text, String id) {
        StringBuilder b = new StringBuilder();

        String heading = getHeading(text, id);
        if (StringUtils.isNotBlank(heading)) {
            b.append(heading);
        }

        b.append("<pre><code ");
        if (StringUtils.isNotBlank(id)) {
            b.append("id=\"nx-html-" + id + "\" ");
        }
        b.append("class=\"language-html\">\n");
        b.append(StringEscapeUtils.escapeXml11(
                new XMLFormatter()
                        .setIndentSize(2)
                        .setWrapAttributesAt(1)
                        .setWrapContentAt(80)
                        .setBlankLineBeforeComment(true)
                        .format(resolveIncludes(text))
        ));
        b.append("</code></pre>");
        return b.toString();
    }
}
