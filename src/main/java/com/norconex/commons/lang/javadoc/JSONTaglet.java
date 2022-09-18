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

import static org.apache.commons.lang3.StringUtils.removeEnd;
import static org.apache.commons.lang3.StringUtils.removeStart;
import static org.apache.commons.lang3.StringUtils.substringAfter;
import static org.apache.commons.lang3.StringUtils.substringBeforeLast;

import java.util.function.Function;

import org.apache.commons.text.StringEscapeUtils;
import org.json.JSONException;
import org.json.JSONObject;

import lombok.AllArgsConstructor;

/**
 * <p>{&#64;nx.json} JSON beautifier with enhanced functionality making
 * it easy to integrate into JavaDoc.</p>
 * <p>
 * Make sure your curly braces are matching (each opening one has a closing
 * one) to prevent documentation errors.  You can also escape them with
 * <code>&amp;#123;</code> and <code>&amp;#125;</code>.
 * </p>
 *
 *
 * @author Pascal Essiembre
 * @since 2.0.0
 */
public class JSONTaglet extends AbstractInlineTaglet {

    public static final String NAME = "nx.json";

    private static final Wrapper ARRAY_WRAPPER = new Wrapper(
            s -> "{\"wrap\":[" + s + "]}",
            s -> {
                var json =  s;
                json = substringAfter(json, "\"wrap\": [");
                json = substringBeforeLast(json, "]").trim();
                json = removeEnd(json, "null");
                return json.replaceAll("(?m)^ {4}", "").trim();
            });
    private static final Wrapper PROP_WRAPPER = new Wrapper(
            s -> ("{" + s + (s.endsWith(",") ? "\"comma\":\"\"" : "") + "}"),
            s -> {
                var json = removeEnd(removeStart(s, "{"), "}").trim();
                json = removeEnd(json, "\"comma\": \"\"").trim();
                return json.replaceAll("(?m)^ {2}", "").trim();
            });

    private static final Wrapper NO_WRAPPER = new Wrapper(s -> s, s -> s);

    public JSONTaglet() {
        super(NAME);
    }

    @Override
    protected String toString(TagContent tag) {
        var json = tag.getContent();

        Wrapper wrapper = null;
        if (json.startsWith("{") || json.startsWith("[")) {
            wrapper = ARRAY_WRAPPER;
        } else if (json.matches("^(?s)[a-zA-Z0-9\"].*")) {
            wrapper = PROP_WRAPPER;
        } else {
            wrapper = NO_WRAPPER;
        }
        json = wrapper.wrap.apply(json);
        try {
            json = new JSONObject(json).toString(2);
            json = wrapper.unrap.apply(json);

            return TagletUtil.preCodeWrap(
                    TagletUtil.toHtmlIdOrNull(tag, "nx-json-"),
                    "language-json",
                    StringEscapeUtils.escapeXml11(json));
        } catch (JSONException e) {
            return TagletUtil.documentationError("JSONTaglet could not parse "
                    + "JSON content: " + e.getMessage());
        }

    }

    // Wrappers to enable entries without a parent.
    @AllArgsConstructor
    private static class Wrapper {
        final Function<String, String> wrap;
        final Function<String, String> unrap;
    }
}
