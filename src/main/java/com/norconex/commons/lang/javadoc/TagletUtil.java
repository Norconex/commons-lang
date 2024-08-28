/* Copyright 2022 Norconex Inc.
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
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import javax.lang.model.element.TypeElement;

import org.apache.commons.lang3.StringUtils;

import com.sun.source.doctree.DocTree;
import com.sun.source.doctree.UnknownInlineTagTree;

final class TagletUtil {

    private TagletUtil() {
    }

    static boolean isDeclaredType(TypeElement el) {
        switch (el.getKind()) {
            case CLASS:
            case INTERFACE:
            case ENUM:
            case ANNOTATION_TYPE:
                return true;
            default:
                return false;
        }
    }

    static UnknownInlineTagTree toUnknownInlineTagTreeOrFail(DocTree docTree) {
        return toUnknownInlineTagTreeOrFail(docTree, null);
    }

    static UnknownInlineTagTree toUnknownInlineTagTreeOrFail(
            DocTree docTree, String expectedTagName) {
        if (docTree.getKind() != UNKNOWN_INLINE_TAG) {
            throw new IllegalArgumentException(
                    String.format("Wrong type: %s.", docTree.getKind()));
        }
        var tag = (UnknownInlineTagTree) docTree;
        var tagName = tag.getTagName();

        if (expectedTagName != null && !expectedTagName.equals(tagName)) {
            throw new IllegalArgumentException(
                    String.format("Wrong tag name: %s.", tagName));
        }
        return tag;
    }

    static String documentationError(String error, Object... arguments) {
        return "!!! Documentation error: "
                + String.format(error, arguments)
                + " Please report to documentation author. !!!";
    }

    static String preCodeWrap(String id, String className, String text) {
        var html = "<pre><code ";
        if (StringUtils.isNotBlank(id)) {
            html += "id=\"" + id + "\" ";
        }
        html += "class=\"" + className + "\">\n";
        html += text;
        html += "</code></pre>";
        return html;
    }

    static String toHtmlIdOrNull(TagContent tag, String prefix) {
        return isNotBlank(tag.getReference())
                ? prefix + tag.getReference()
                : null;
    }
}
