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

import static com.sun.source.doctree.DocTree.Kind.UNKNOWN_INLINE_TAG;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import javax.lang.model.element.Element;
import javax.lang.model.util.Elements;

import org.apache.commons.lang3.StringUtils;

import com.sun.source.doctree.DocTree;
import com.sun.source.doctree.UnknownInlineTagTree;
import com.sun.source.util.DocTrees;

import jdk.javadoc.doclet.Doclet;
import jdk.javadoc.doclet.DocletEnvironment;
import jdk.javadoc.doclet.Taglet;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/**
 * <p>{&#64;nx.include} Include raw content from another
 * taglet in a different source file.</p>
 * <p>Example that includes the {@link XMLUsageTaglet} usage example:</p>
 * <pre>
 * &lt;xml&gt;
 *   &lt;sample attr="whatever"&gt;Next line should be replaced.&lt;/sample&gt;
 *   {&#64;nx.include com.norconex.commons.lang.javadoc.XMLUsageTaglet@nx.xml.usage}
 * &lt;/xml&gt;
 * </pre>
 *
 * <p>Results in:</p>
 * {@nx.xml
 * <xml>
 *   <sub attr="whatever">Next line should be replaced.</sub>
 *   {@nx.include com.norconex.commons.lang.javadoc.XMLExampleTaglet@nx.xml.example}
 * </xml>
 * }
 *
 * <p>Can be nested in nx.xml.* taglets.</p>
 *
 * <p>Can also use # if the target defines such as id.</p>
 *
 * @author Pascal Essiembre
 * @since 2.0.0
 */

//TODO document supported use cases:

//TODO document include can be "used" anywhere, but it will only look in
// class/type documentation, not method, not anything else.
// In other words, it is useless to define an #id for a block if not
// in class documentation

//TODO add {@nx.block.hide
//   not in javadoc, but eligible for includes in other classes
//}

/*
   {@include com.mypackage.MyClass}    <-- Entire Type javadoc, or.. NOT supported?

   {@include com.mypackage.MyClass@nx.xml.usage} <-- References by block tag name
   E.g.:
     {@nx.xml.usage
       <myxml>
         An example
       </myxml>
     }

   {@include com.mypackage.MyClass#someDoc} <-- References by block custom id
   E.g.:
     {@nx.xml.usage #someDoc   <-- with a space separating or not
       <myxml>
         An example
       </myxml>
     }

   {@include com.mypackage.MyClass@nx.xml.usage#someDoc} <-- Supported, but a bit useless
   E.g.:
     {@nx.xml.usage #someDoc   <-- with a space separating or not
       <myxml>
         An example
       </myxml>
     }


*/
//TODO document: can use @ (to include taglet block) or
// # to include taglet id (prefixed with @ or not)

//SEE:


@SuppressWarnings("javadoc")
public class IncludeTaglet implements Taglet {


    public static final String NAME = "nx.include";

    private DocTrees treeUtil;
    private Elements elementUtil;

    private final EnumSet<Location> allowedSet = EnumSet.allOf(Location.class);

    @Override
    public void init(DocletEnvironment env, Doclet doclet) {
        treeUtil = env.getDocTrees();
        elementUtil = env.getElementUtils();
    }

    @Override
    public Set<Location> getAllowedLocations() {
        return allowedSet;
    }
    @Override
    public boolean isInlineTag() {
        return true;
    }
    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String toString(List<? extends DocTree> tags, Element element) {
        if (tags.isEmpty()) {
            return "";
        }

        DocTree tag = tags.get(0);
        if (tag.getKind() != UNKNOWN_INLINE_TAG) {
            return "";
        }

        var includeTag = (UnknownInlineTagTree) tag;
        if (includeTag.getContent().isEmpty()) {
            return "";
        }

        var directive = new IncludeDirective(includeTag);

        if (!directive.isParseValid()) {
            return TagletUtil.documentationError(directive.parseError);
        }


        return doInclude(directive);

        //TODO maybe: allows {@link } blocks to support strong typing
        // and avoid bad links. When not using taglets, it would remain
        // an actual link.
    }

    public String doInclude(IncludeDirective directive) {

        // Technically, since we only support declared types, we assume to be
        // dealing with one here, else we throw an error. If one day we support
        // referencing method and other elements, we can user a visitor
        // or loop through TypeElement#getEnclosedElements()
        var typeEl = elementUtil.getTypeElement(directive.className);
        if (!TagletUtil.isDeclaredType(typeEl)) {
            return TagletUtil.documentationError(
                    "Include directive failed as referenced element is not a "
                    + "declared type: %s.",
                    directive.className);
        }

        var typeCommentTree = treeUtil.getDocCommentTree(typeEl);

        var content = "";
        for (DocTree bodyPart  : typeCommentTree.getFullBody()) {
            if (bodyPart.getKind() == UNKNOWN_INLINE_TAG) {
                var tag = new NxTag(
                        TagletUtil.toUnknownInlineTagTreeOrFail(bodyPart));
                if (directive.matches(tag)) {
                    content = tag.getContent();
                    break;
                }
            }
        }

        return content;
    }

    @Getter
    @ToString
    @EqualsAndHashCode
    private static class IncludeDirective {
        private String reference;
        private String tagName;
        private String className;
        private String parseError;
        private IncludeDirective(DocTree docTree) {

            var tag = TagletUtil.toUnknownInlineTagTreeOrFail(
                    docTree, IncludeTaglet.NAME);

            // There shall be only one TEXT entry in content.
            var content = tag.getContent().get(0).toString();

            // className
            var cls = extractGroup1(content, "^(.*?)(\\s|\\@|\\#|$)");
            if (StringUtils.isBlank(cls)) {
                parseError = "Include directive missing fully qualified "
                        + "class name.";
                return;
            }
            className = cls;

            // tagName
            tagName = extractGroup1(content, "@(.*?)(\\s|\\#|$)");

            // reference
            reference = extractGroup1(content, "#(.*?)(\\s|\\@|$)");

            if (StringUtils.isAllBlank(tagName, reference)) {
                parseError = "Include directive missing both tag name "
                        + "and reference.";
            }
        }

        boolean matches(NxTag tag) {
            return isParseValid() && nameOK(tag) && refOK(tag);
        }

        private boolean nameOK(NxTag tag) {
            return tagName == null || tagName.equals(tag.getName());
        }
        private boolean refOK(NxTag tag) {
            return reference == null || reference.equals(tag.getReference());
        }
        private static String extractGroup1(String txt, String regex) {
            var m = Pattern.compile(regex).matcher(txt);
            if (m.find()) {
                return m.group(1);
            }
            return null;
        }
        private boolean isParseValid() {
            return parseError == null;
        }
    }
}
