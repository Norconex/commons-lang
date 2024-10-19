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

import com.sun.source.doctree.DocTree;
import com.sun.source.doctree.UnknownInlineTagTree;

import jdk.javadoc.doclet.Doclet;
import jdk.javadoc.doclet.DocletEnvironment;
import jdk.javadoc.doclet.Taglet;

/**
 * <p>
 * {&#64;nx.include} Include text from other JavaDoc documentation found in
 * source files.
 * </p>
 *
 * <h2>Making comments eligible for inclusion</h2>
 * <p>
 * The text to be included is taken from the declared type JavaDoc comment of
 * the referenced class source file. The given text needs to be wrapped by
 * a block tag such as {@link BlockTaglet},
 * {@link HtmlTaglet}, {@link JsonTaglet}, {@link XmlTaglet}, etc.
 * </p>
 * <p>
 * If you have multiple blocks of text to include from a JavaDoc comment,
 * you can add a reference anchor to it (a recommended practice).
 * In the following example, the JavaDoc comment has two HTML blocks
 * ready to be included by any other JavaDoc comments.
 * </p>
 *
 * <pre>
 *  package com.somepackage;
 *
 *  &#x2F;**
 *   * &lt;h1&gt;How to create a list&lt;/h1&gt;
 *   * &lt;p&gt;Two examples:&lt;/p&gt;
 *   *
 *   * {&#x40;nx.html #ordered
 *   *   &lt;ol&gt;
 *   *     &lt;li&gt;An item.&lt;/li&gt;
 *   *     &lt;li&gt;Another item.&lt;/li&gt;
 *   *   &lt;/ol&gt;
 *   * }
 *   *
 *   * {&#x40;nx.html #unordered
 *   *   &lt;ul&gt;
 *   *     &lt;li&gt;An item.&lt;/li&gt;
 *   *     &lt;li&gt;Another item.&lt;/li&gt;
 *   *   &lt;/ul&gt;
 *   * }
 *   *&#x2F;
 *  public class MyClassWithReusableComments {
 *    //...
 *  }
 * </pre>
 *
 * <h2>Including comments</h2>
 * <p>
 * The <code>{&#x40;nx.include ...}</code> directive is used
 * to reference other classes along with which comment block to include.
 * The complete syntax is:
 * </p>
 * <pre>
 * {&#x40;nx.include [class][@tagName][#reference]}
 * </pre>
 * <p>
 * Where each elements in square brackets are:
 * </p>
 * <table style="padding: 2px;">
 *   <caption style="display: none;">Include tag elements</caption>
 *   <tr>
 *     <td style="vertical-align: top;"><b>class</b></td>
 *     <td>
 *       The fully qualified class name containing the comment to include.
 *       Required.
 *     </td>
 *   </tr>
 *   <tr>
 *     <td style="vertical-align: top;"><b>@tagName</b></td>
 *     <td>
 *       The name of the block tag wrapping the comment. If there are multiple
 *       block tags with the same name in the class comment, the first one
 *       will be picked. Use <code>#reference</code> for more precision.
 *       At least one of <code>@tagName</code> and <code>#reference</code>
 *       need to be specified.
 *     </td>
 *   </tr>
 *   <tr>
 *     <td style="vertical-align: top;"><b>#reference</b></td>
 *     <td>
 *       Matches the reference anchor defined in the included class comment.
 *       Those should be unique within a class comment so should be favored
 *       over <code>@tagName</code> when present.
 *       At least one of <code>@tagName</code> and <code>#reference</code>
 *       need to be specified.
 *     </td>
 *   </tr>
 * </table>
 *
 * <p>
 * The following example will insert the two blocks defined in the previous
 * example along with additional HTML.
 * </p>
 *
 * <pre>
 *  &#x2F;**
 *   * &lt;p&gt;
 *   *   Here are two types of HTML lists:
 *   * &lt;/p&gt;
 *   * {&#x40;nx.html
 *   *
 *   *   &lt;h1&gt;Ordered List&lt;/h1&gt;
 *   *   {&#x40;nx.include com.somepackage.MyClassWithReusableComments#ordered}
 *   *
 *   *   &lt;h1&gt;Unordered List&lt;/h1&gt;
 *   *   {&#x40;nx.include com.somepackage.MyClassWithReusableComments#unordered}
 *   * }
 *   *&#x2F;
 *  public class MyClassIncludingComments {
 *    //...
 *  }
 * </pre>
 *
 * <p>
 * One thing to keep in mind is the included text is taken as is.
 * If you want to format it, you would need to wrap it yourself like above.
 * Includes can be nested in any other <code>@nx.*</code> taglets.
 * </p>
 *
 * @since 2.0.0
 * @deprecated Will be removed
 */
@Deprecated(since = "3.0.0", forRemoval = true)
public class IncludeTaglet implements Taglet {

    public static final String NAME = "nx.include";

    private DocletEnvironment env;

    private final EnumSet<Location> allowedSet = EnumSet.allOf(Location.class);

    @Override
    public void init(DocletEnvironment env, Doclet doclet) {
        this.env = env;
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

        var directive = IncludeDirective.of(includeTag);

        if (directive.hasParseError()) {
            return TagletUtil.documentationError(directive.getParseError());
        }

        return resolveIncludeDirective(directive, env);
    }

    // When a resolved comment has one or more includes in it, resolve them.
    static String resolveContentIncludes(
            String content, DocletEnvironment env) {
        var m = Pattern.compile("\\{\\@nx\\.include +([^\\n]+?)\\}",
                Pattern.DOTALL).matcher(content);
        return m.replaceAll(mr -> resolveIncludeDirective(
                IncludeDirective.of(mr.group(1)), env));
    }

    // directive derived from javadoc tool picking up {@nx.include ...}
    static String resolveIncludeDirective(
            IncludeDirective directive, DocletEnvironment env) {

        // Technically, since we only support declared types, we assume to be
        // dealing with one here, else we throw an error. If one day we support
        // referencing method and other elements, we can user a visitor
        // or loop through TypeElement#getEnclosedElements()
        var typeEl = env.getElementUtils().getTypeElement(
                directive.getClassName());
        if (typeEl == null) {
            return TagletUtil.documentationError(
                    "Include directive failed as type element could not be "
                            + "resolved: %s (maybe a typo?).",
                    directive.getClassName());
        }

        if (!TagletUtil.isDeclaredType(typeEl)) {
            return TagletUtil.documentationError(
                    "Include directive failed as referenced element is not a "
                            + "declared type: %s.",
                    directive.getClassName());
        }

        var typeCommentTree = env.getDocTrees().getDocCommentTree(typeEl);
        var content = "";
        if (typeCommentTree != null) {
            for (DocTree bodyPart : typeCommentTree.getFullBody()) {
                if (bodyPart.getKind() == UNKNOWN_INLINE_TAG) {
                    var tag = TagContent.of(
                            TagletUtil.toUnknownInlineTagTreeOrFail(bodyPart));
                    if (tag.isPresent() && directive.matches(tag.get())) {
                        content = tag.get().getContent();
                        break;
                    }
                }
            }
        }
        return resolveContentIncludes(content, env);
    }
}
