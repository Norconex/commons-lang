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

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import javax.lang.model.element.Element;

import org.apache.commons.lang3.StringUtils;

import com.sun.source.doctree.DocTree;  //NOSONAR this com.sun lib is public

import jdk.javadoc.doclet.Doclet;
import jdk.javadoc.doclet.DocletEnvironment;
import jdk.javadoc.doclet.Taglet;

/**
 * <p>
 * Base inline taglet class. Subclasses are by default
 * usable everywhere.  Can optionally supply a heading that will appear
 * before the tag content.
 * </p>
 * <pre>
 * {&#64;mytag #optionaIdForInclusion
 *   &lt;xml&gt;an example&lt;/xml&gt;
 * }
 * </pre>
 *
 * @author Pascal Essiembre
 * @since 2.0.0
 */
public abstract class AbstractInlineTaglet implements Taglet {

    // References:
    //   https://docs.oracle.com/en/java/javase/11/docs/api/jdk.javadoc/
    //   https://docs.oracle.com/en/java/javase/11/docs/api/jdk.javadoc/
    //        jdk/javadoc/doclet/package-summary.html
    //   https://openjdk.org/groups/compiler/using-new-doclet.html

    private final EnumSet<Location> allowedSet = EnumSet.allOf(Location.class);

    private final String name;
    private final Function<TagContent, String> headingProvider;
    private DocletEnvironment env;

    protected AbstractInlineTaglet(String name) {
        this(name, null);
    }
    protected AbstractInlineTaglet(
            String name, Function<TagContent, String> headingProvider) {
        this.name = name;
        this.headingProvider = headingProvider;
    }

    @Override
    public void init(DocletEnvironment env, Doclet doclet) {
        this.env = env;
    }

    @Override
    public String getName() {
        return name;
    }

    public Function<TagContent, String> getHeadingProvider() {
        return headingProvider;
    }

    @Override
    public boolean isInlineTag() {
        return true;
    }

    @Override
    public Set<Location> getAllowedLocations() {
        return allowedSet;
    }

    @Override
    public String toString(List<? extends DocTree> tagTrees, Element element) {
        var tag = TagContent.of(tagTrees).orElse(null);
        if (tag == null) {
            return "";
        }

        // resolve nested includes
        tag = tag.withContent(
                IncludeTaglet.resolveContentIncludes(tag.getContent(), env));

        var text = toString(tag);
        if (text == null) {
            return "";
        }

        if (headingProvider != null) {
            var heading = headingProvider.apply(tag);
            if (StringUtils.isNotBlank(heading)) {
                text = heading + "\n" + text;
            }
        }

        return text;
    }

    protected abstract String toString(TagContent tag);
}
