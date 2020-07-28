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

import com.sun.javadoc.Tag;
import com.sun.tools.doclets.Taglet;

/**
 * <p>{&#64;nx.block} A wrapper around any JavaDoc content for
 * inclusion in other classes.</p>
 *
 * @author Pascal Essiembre
 * @since 2.0.0
 */
public class BlockTaglet extends AbstractInlineTaglet {

    public static final String NAME = "nx.block";

    /**
     * Register an instance of this taglet.
     * @param tagletMap registry of taglets
     */
    public static void register(Map<String, Taglet> tagletMap) {
        tagletMap.put(NAME, new BlockTaglet());
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    protected String toString(Tag tag, String text, String id) {
        StringBuilder b = new StringBuilder();

        b.append("<span");
        if (StringUtils.isNotBlank(id)) {
            b.append("id=\"nx-block-" + id + "\"");
        }
        b.append(">\n");
        b.append(resolveIncludes(text));
        b.append("</span>");

        return b.toString();
    }
}
