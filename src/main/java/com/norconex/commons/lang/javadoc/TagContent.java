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

import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;

import com.sun.source.doctree.DocTree;
import com.sun.source.doctree.UnknownInlineTagTree;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
class TagContent {
    @NonNull
    private final UnknownInlineTagTree unknownTag;
    private String reference;
    private String content;
    private boolean parsed;

    String getName() {
        return unknownTag.getTagName();
    }
    String getReference() {
        if (!parsed) {
            parse();
        }
        return reference;
    }
    String getContent() {
        if (!parsed) {
            parse();
        }
        return content;
    }

    TagContent withReference(String reference) {
        return new TagContent(unknownTag, reference, content, parsed);
    }
    TagContent withContent(String content) {
        return new TagContent(unknownTag, reference, content, parsed);
    }

    // Must have content and be UnknownInlineTag
    static Optional<TagContent> of(List<? extends DocTree> docTrees) {
        if (docTrees.isEmpty()) {
            return Optional.empty();
        }
        return of(docTrees.get(0));
    }
    static Optional<TagContent> of(DocTree docTree) {
        if (docTree.getKind() != UNKNOWN_INLINE_TAG) {
            return Optional.empty();
        }
        var unknownTag = (UnknownInlineTagTree) docTree;
        if (unknownTag.getContent().isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new TagContent(unknownTag));
    }

    private void parse() {
        var text = unknownTag.getContent()
                .toString().trim().replace("\r", "");
        if (text.startsWith("#")) {
            reference = StringUtils.substringBefore(text, "\n");
            content = StringUtils.removeStart(text, reference).trim();
            reference = StringUtils.removeStart(reference, "#");
        } else {
            content = text;
        }
        parsed = true;
    }

    @Override
    public String toString() {
        return "Tag [name=" + unknownTag.getTagName()
                + ", reference=" + reference
                + ", content=" + content
                + ", parsed=" + parsed + "]";
    }
}