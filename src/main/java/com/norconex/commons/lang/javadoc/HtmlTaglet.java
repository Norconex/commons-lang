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

import static org.apache.commons.text.StringEscapeUtils.escapeXml11;

import com.norconex.commons.lang.xml.XmlFormatter;
import com.norconex.commons.lang.xml.XmlFormatter.Builder.AttributeWrap;

/**
 * <p>{&#64;nx.html} HTML beautifier with enhanced functionality.
 * HTML has to follow basic XML syntax.</p>
 *
 * @since 2.0.0
 * @deprecated Will be removed
 */
@Deprecated(since = "3.0.0", forRemoval = true)
public class HtmlTaglet extends AbstractInlineTaglet {

    public static final String NAME = "nx.html";
    public static final XmlFormatter FORMATTER = XmlFormatter.builder()
            .maxLineLength(80)
            .minTextLength(40)
            .attributeWrapping(AttributeWrap.ALL)
            .elementIndent("  ")
            .preserveTextIndent()
            .selfCloseEmptyElements()
            .closeWrappingTagOnOwnLine()
            .build();

    public HtmlTaglet() {
        super(NAME);
    }

    @Override
    protected String toString(TagContent tag) {
        return TagletUtil.preCodeWrap(
                TagletUtil.toHtmlIdOrNull(tag, "nx-html-"),
                "language-html",
                escapeXml11(FORMATTER.format(tag.getContent())));
    }
}
