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

import com.norconex.commons.lang.xml.XMLUtil;
import com.sun.javadoc.Tag;
import com.sun.tools.doclets.Taglet;

/**
 * <p>{&#64;nx.xml} XML beautifier with enhanced functionality.</p>
 *
 * {@nx.xml
 * <test>
 *   <sub attr="xml-taglet-test">This is an XML example.</sub>
 * </test>
 * }
 *
 * {@nx.xml #usage
 * <test>
 *   <sub attr="xml-taglet-test">This is annoter XML example.</sub>
 * </test>
 * }
 *
 * @author Pascal Essiembre
 * @since 2.0.0
 */
public class XMLTaglet extends AbstractInlineTaglet {

    public static final String NAME = "nx.xml";

    //TODO resolve classes in XML to create links to JavaDoc

    /**
     * Register an instance of this taglet.
     *
     * @param tagletMap
     *            registry of taglets
     */
    public static void register(Map<String, Taglet> tagletMap) {
        tagletMap.put(NAME, new XMLTaglet());
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
            b.append("id=\"nx-xml-" + id + "\" ");
        }
        b.append("class=\"language-xml\">\n");
        b.append(StringEscapeUtils.escapeXml11(
                XMLUtil.format(resolveIncludes(text), 2)
//                new XML(resolveIncludes(text)).toString(2)
        ));
        b.append("</code></pre>");
        return b.toString();
    }
}
