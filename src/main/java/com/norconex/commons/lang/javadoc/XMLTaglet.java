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

import java.util.function.Function;

import com.norconex.commons.lang.xml.XMLFormatter;
import com.norconex.commons.lang.xml.XMLFormatter.Builder.AttributeWrap;

/**
 * <p>{&#64;nx.xml} XML beautifier with enhanced functionality.</p>
 *
 * <p>
 *   Refer to taglet source code to find out how the following XML
 *   samples were generated.
 * </p>
 *
 * {@nx.xml
 * <test>
 *   <sub attr="xml-taglet-test">This is an XML example.</sub>
 * </test>
 * }
 *
 * {@nx.xml #usage
 * <test>
 *   <sub attr="xml-taglet-test">This is another XML example.</sub>
 * </test>
 * }
 *
 * @author Pascal Essiembre
 * @since 2.0.0
 */
public class XMLTaglet extends AbstractInlineTaglet {

    public static final String NAME = "nx.xml";
    public static final XMLFormatter FORMATTER = XMLFormatter.builder()
            .maxLineLength(80)
            .minTextLength(40)
            .attributeWrapping(AttributeWrap.ALL)
            .elementIndent("  ")
            .attributeIndent("    ")
            .preserveTextIndent()
            .selfCloseEmptyElements()
            .build();

    public XMLTaglet() {
        super(NAME);
    }
    protected XMLTaglet(String name, Function<NxTag, String> headingProvider) {
        super(name, headingProvider);
    }

    @Override
    protected String toString(NxTag tag) {
        return TagletUtil.preCodeWrap(
                TagletUtil.toHtmlIdOrNull(tag, "nx-xml-"),
                "language-xml",
                escapeXml11(FORMATTER.format(tag.getContent())));
    }
}
