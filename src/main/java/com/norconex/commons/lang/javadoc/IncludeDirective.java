/* Copyright 2024 Norconex Inc.
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

import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import com.sun.source.doctree.DocTree;

class IncludeDirective {

    private final String reference;
    private final String tagName;
    private final String className;
    private final String parseError;

    IncludeDirective(String reference, String tagName, String className,
            String parseError) {
        this.reference = reference;
        this.tagName = tagName;
        this.className = className;
        this.parseError = parseError;
    }

    public String getReference() {
        return reference;
    }
    public String getTagName() {
        return tagName;
    }
    public String getClassName() {
        return className;
    }
    public String getParseError() {
        return parseError;
    }

    static IncludeDirective of(DocTree directive) {
        var tag = TagletUtil.toUnknownInlineTagTreeOrFail(
                directive, IncludeTaglet.NAME);

        // at this point, There shall be only one TEXT entry in content.
        return of(tag.getContent().get(0).toString());
    }

    static IncludeDirective of(String directive) {
        String className;
        String parseError = null;
        String tagName = null;
        String reference = null;

        // className
        className = extractGroup1(directive, "^(.*?)(\\s|\\@|\\#|$)");
        if (StringUtils.isBlank(className)) {
            parseError =
                    "Include directive missing fully qualified class name.";
        } else {
            // tagName
            tagName = extractGroup1(directive, "@(.*?)(\\s|\\#|$)");

            // reference
            reference = extractGroup1(directive, "#(.*?)(\\s|\\@|$)");

            if (StringUtils.isAllBlank(tagName, reference)) {
                parseError = "Include directive missing both tag name "
                        + "and reference.";
            }
        }
        return new IncludeDirective(reference, tagName, className, parseError);

    }

    boolean matches(TagContent tag) {
        return !hasParseError() && nameOK(tag) && refOK(tag);
    }

    boolean hasParseError() {
        return parseError != null;
    }

    private boolean nameOK(TagContent tag) {
        return tagName == null || tagName.equals(tag.getName());
    }

    private boolean refOK(TagContent tag) {
        return reference == null || reference.equals(tag.getReference());
    }

    private static String extractGroup1(String txt, String regex) {
        var m = Pattern.compile(regex).matcher(txt);
        if (m.find()) {
            return m.group(1);
        }
        return null;
    }
}