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
import org.json.JSONObject;

import com.sun.javadoc.Tag;
import com.sun.tools.doclets.Taglet;

/**
 * <p>{&#64;nx.json} JSON beautifier with enhanced functionality making
 * it easy to integrate into JavaDoc.</p>
 * <p>
 * Make sure your curly braces are matching (each opening one has a closing
 * one) or it will fail.  You can also escape them with
 * <code>&amp;#123;</code> and <code>&amp;#125;</code>.
 * </p>
 *
 *
 * @author Pascal Essiembre
 * @since 2.0.0
 */
public class JSONTaglet extends AbstractInlineTaglet {

    public static final String NAME = "nx.json";

    /**
     * Register an instance of this taglet.
     * @param tagletMap registry of taglets
     */
    public static void register(Map<String, Taglet> tagletMap) {
        tagletMap.put(NAME, new JSONTaglet());
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
            b.append("id=\"nx-json-" + id + "\" ");
        }
        b.append("class=\"language-json\">\n");

        String json = resolveIncludes(text);
        json = "{\"wrap\":" + json + "}";
        json = new JSONObject(json).toString(2);
        json = json.replaceFirst(
                "(?s)^\\s*\\{\\s*\"?wrap\"?\\s*:\\s*(.*)\\}\\s*$", "$1").trim();
        json = StringEscapeUtils.escapeXml11(json);
        b.append(json);

        b.append("</code></pre>");
        return b.toString();
    }
}
