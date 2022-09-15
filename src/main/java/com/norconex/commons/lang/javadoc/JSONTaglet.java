/* Copyright 2020-2022 Norconex Inc.
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

import org.apache.commons.text.StringEscapeUtils;
import org.json.JSONObject;

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

    public JSONTaglet() {
        super(NAME);
    }

    @Override
    protected String toString(Tag tag) {
        var json = tag.getContent();
        // wrap to ensure properly formed for formatting, then unwrap
        json = "{\"wrap\":" + json + "}";
        json = new JSONObject(json).toString(2);
        json = json.replaceFirst(
                "(?s)^\\s*\\{\\s*\"?wrap\"?\\s*:\\s*(.*)\\}\\s*$", "$1").trim();
        return StringEscapeUtils.escapeXml11(json);
    }
}
