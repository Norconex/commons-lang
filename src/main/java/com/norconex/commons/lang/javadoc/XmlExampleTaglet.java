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

import org.apache.commons.lang3.StringUtils;

/**
 * <p>{&#64;nx.xml.example} XML beautifier with enhanced functionality.</p>
 *
 * @since 2.0.0
 * @deprecated Will be removed
 */
@Deprecated(since = "3.0.0", forRemoval = true)
public class XmlExampleTaglet extends XmlTaglet {

    public static final String NAME = "nx.xml.example";

    public XmlExampleTaglet() {
        super(NAME, tag -> "<h4 id=\"nx-xml-" + orDefaultId(tag.getReference())
                + "-heading\">XML usage example:</h4>\n");
    }

    private static String orDefaultId(String id) {
        return StringUtils.isNotBlank(id) ? id : "example";
    }

    @Override
    protected String toString(TagContent tag) {
        return super.toString(
                tag.withReference(orDefaultId(tag.getReference())));
    }
}
