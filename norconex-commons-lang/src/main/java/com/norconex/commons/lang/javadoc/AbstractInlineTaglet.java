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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.sun.javadoc.Tag;
import com.sun.tools.doclets.Taglet;

/**
 * <p>
 * Base inline taglet class. Subclasses are by default
 * usable everywhere.
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
    //     https://docs.oracle.com/javase/8/docs/jdk/api/javadoc/
    //     doclet/com/sun/javadoc/package-summary.html
    //     https://docs.oracle.com/javase/8/docs/jdk/api/javadoc/
    //     taglet/com/sun/tools/doclets/Taglet.html


    @Override
    public boolean isInlineTag() {
        return true;
    }

    @Override
    public String toString(Tag[] tags) {
        return null;
    }

    @Override
    public boolean inField() {
        return true;
    }

    @Override
    public boolean inConstructor() {
        return true;
    }

    @Override
    public boolean inMethod() {
        return true;
    }

    @Override
    public boolean inOverview() {
        return true;
    }

    @Override
    public boolean inPackage() {
        return true;
    }

    @Override
    public boolean inType() {
        return true;
    }

    private static final Pattern ID_PATTERN =
            Pattern.compile("(?s)^\\s*\\#(.*?)[\\r\\n]+.*$");
    private static final Pattern TEXT_PATTERN =
            Pattern.compile("(?s)^\\s*\\#.*?[\\r\\n]+(.*)$");
    @Override
    public String toString(Tag tag) {
        String id = null;
        Matcher m = ID_PATTERN.matcher(tag.text());
        if (m.matches()) {
            id = m.replaceFirst("$1");
        }

        String text = TEXT_PATTERN.matcher(tag.text()).replaceFirst("$1");
        String out = toString(tag, text, id);
        if (out == null) {
            return text;
        }
        return out;
    }
    protected String toString(Tag tag, String text, String id) {
        //NOOP
        return null;
    };

    protected String resolveIncludes(Tag tag, String text) {
        Matcher m = Pattern.compile(
                "\\{\\@nx\\.include(.*?)\\}", Pattern.DOTALL).matcher(text);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String ref = m.group(1);
            m.appendReplacement(sb, IncludeTaglet.include(tag, ref));
        }
        m.appendTail(sb);
        return sb.toString();
    }
}
