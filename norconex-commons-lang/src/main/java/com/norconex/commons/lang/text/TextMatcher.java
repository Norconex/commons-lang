/* Copyright 2019-2020 Norconex Inc.
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
package com.norconex.commons.lang.text;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.norconex.commons.lang.bean.BeanUtil;
import com.norconex.commons.lang.xml.IXMLConfigurable;
import com.norconex.commons.lang.xml.XML;

/**
 * <p>
 * A configurable class offering a few different ways to perform text matching
 * and replacing.  Supported methods are:
 * </p>
 * <ul>
 *  <li><b>BASIC:</b>
 *      Default. Text is matched as specified.</li>
 *  <li><b>WILDCARD:</b>
 *      An asterisk (*) matches a series made of any characters.
 *      A question mark (?) matches any single character.
 *      If you need to escape wildcards, use REGEX instead.</li>
 *  <li><b>REGEX:</b>
 *      Matching/replacing is done using Java-style regular expressions.</li>
 * </ul>
 * <p>
 * Match/replace methods are case-sensitive by default.
 * </p>
 * <p>
 * This class is not thread-safe.
 * </p>
 *
 * {@nx.xml.usage #attributes
 *     method="[basic|wildcard|regex]"
 *     ignoreCase="[false|true]"
 *     ignoreDiacritic="[false|true]"
 *     replaceAll="[false|true]"
 *     matchWhole="[false|true]"
 * }
 *
 * <p>
 * The above are configurable attributes consuming classes can expect.
 * The actual regular expression is expected to be the tag content.
 * </p>
 *
 * {@nx.xml.example
 * <sampleConfig method="wildcard" ignoreCase="true">
 *     paul*mar?
 * </sampleConfig>
 * }
 * <p>
 * Given a text of "It seems Paul and Marc are friends" and a replacement of
 * "they", the above will result in "It seems they are friends.".
 * </p>
 *
 * @author Pascal Essiembre
 * @since 2.0.0
 */
public class TextMatcher implements IXMLConfigurable {



    //TODO rename TextPattern? (more neutral since it is form match AND replace.

    //TODO rename just Matcher?  or StringMatcher or TextMatcher to avoid conflict?

    public enum Method {
        BASIC(new MethodStrategy() {
            @Override
            public boolean matches(TextMatcher sr, String text) {
                Matcher m = createMatcher(sr, text);
                return sr.matchWhole ? m.matches() : m.find();
            }
            @Override
            public String replace(
                    TextMatcher sr, String text, String replacement) {
                String quotedRepl = Matcher.quoteReplacement(replacement);
                //TODO move this generic code to a method shared by all
                // replace Methods.
                Matcher m = createMatcher(sr, text);
                if (sr.matchWhole && m.matches()) {
                    return m.replaceFirst(quotedRepl);
                }
                if (!sr.matchWhole) {
                    if (sr.replaceAll) {
                        return m.replaceAll(quotedRepl);
                    } else {
                        return m.replaceFirst(quotedRepl);
                    }
                }
                return text;
            }
            private Matcher createMatcher(TextMatcher sr, String text) {
                return new Regex(Regex.escape(sr.pattern))
                        .dotAll()
                        .setIgnoreCase(sr.ignoreCase)
                        .setIgnoreDiacritic(sr.ignoreDiacritic)
                        .matcher(text);
            }
        }),
        WILDCARD(new MethodStrategy() {
            @Override
            public boolean matches(TextMatcher sr, String text) {
                Matcher m = createMatcher(sr, text);
                return sr.matchWhole ? m.matches() : m.find();
            }
            @Override
            public String replace(
                    TextMatcher sr, String text, String replacement) {
                String quotedRepl = Matcher.quoteReplacement(replacement);
                Matcher m = createMatcher(sr, text);
                if (sr.matchWhole && m.matches()) {
                    return m.replaceFirst(quotedRepl);
                }
                if (!sr.matchWhole) {
                    if (sr.replaceAll) {
                        return m.replaceAll(quotedRepl);
                    } else {
                        return m.replaceFirst(quotedRepl);
                    }
                }
                return text;
            }
            private Matcher createMatcher(TextMatcher sr, String text) {
                Pattern p = Pattern.compile("[^*?]+|(\\*)|(\\?)");
                Matcher m = p.matcher(sr.pattern);
                StringBuilder b = new StringBuilder();
                while (m.find()) {
                    if(m.group(1) != null) {
                        b.append(".*");
                    } else if(m.group(2) != null) {
                        b.append(".");
                    } else {
                        b.append(Regex.escape(m.group()));
                    }
                }
                return new Regex(b.toString())
                        .dotAll()
                        .setIgnoreCase(sr.ignoreCase)
                        .setIgnoreDiacritic(sr.ignoreDiacritic)
                        .matcher(text);
            }
        }),
        REGEX(new MethodStrategy() {
            @Override
            public boolean matches(TextMatcher sr, String text) {
                Matcher m = createMatcher(sr, text);
                return sr.matchWhole ? m.matches() : m.find();
            }
            @Override
            public String replace(
                    TextMatcher sr, String text, String replacement) {
                Matcher m = createMatcher(sr, text);
                if (sr.matchWhole && m.matches()) {
                    return m.replaceFirst(replacement);
                }
                if (!sr.matchWhole) {
                    if (sr.replaceAll) {
                        return m.replaceAll(replacement);
                    } else {
                        return m.replaceFirst(replacement);
                    }
                }
                return text;
            }
            private Matcher createMatcher(TextMatcher sr, String text) {
                return new Regex(sr.pattern)
                        .dotAll()
                        .setIgnoreCase(sr.ignoreCase)
                        .setIgnoreDiacritic(sr.ignoreDiacritic)
                        .matcher(text);
            }
        });

        private final MethodStrategy ms;

        private Method(MethodStrategy ms) {
            this.ms = ms;
        }
    }

    interface MethodStrategy {
        boolean matches(TextMatcher sr, String text);
        String replace(TextMatcher sr, String text, String replacement);
    }

    private Method method = Method.BASIC;

    private String pattern;
    private boolean ignoreCase;
    private boolean ignoreDiacritic;
    private boolean replaceAll;
    private boolean matchWhole;

    public TextMatcher() {
        super();
    }
    public TextMatcher(Method method) {
        super();
        this.method = method;
    }
    /**
     * Copy constructor. Supplying <code>null</code> is the same
     * as evoking the empty constructor.
     * @param textMatcher instance to copy
     */
    public TextMatcher(TextMatcher textMatcher) {
        super();
        if (textMatcher != null) {
            this.method = textMatcher.method;
            this.pattern = textMatcher.pattern;
            this.ignoreCase = textMatcher.ignoreCase;
            this.ignoreDiacritic = textMatcher.ignoreDiacritic;
            this.replaceAll = textMatcher.replaceAll;
            this.matchWhole = textMatcher.matchWhole;
        }
    }

    public Method getMethod() {
        return method;
    }
    public TextMatcher setMethod(Method method) {
        this.method = method;
        return this;
    }
    public TextMatcher withMethod(Method method) {
        return copy().setMethod(method);
    }

    public boolean isMatchWhole() {
        return matchWhole;
    }
    public TextMatcher setMatchWhole(boolean matchWhole) {
        this.matchWhole = matchWhole;
        return this;
    }
    public TextMatcher withMatchWhole(boolean matchWhole) {
        return copy().setMatchWhole(matchWhole);
    }

    public String getPattern() {
        return pattern;
    }
    public TextMatcher setPattern(String pattern) {
        this.pattern = pattern;
        return this;
    }
    public TextMatcher withPattern(String pattern) {
        return copy().setPattern(pattern);
    }

    public boolean isIgnoreCase() {
        return ignoreCase;
    }
    public TextMatcher setIgnoreCase(boolean ignoreCase) {
        this.ignoreCase = ignoreCase;
        return this;
    }
    public TextMatcher withIgnoreCase(boolean ignoreCase) {
        return copy().setIgnoreCase(ignoreCase);
    }

    public boolean isIgnoreDiacritic() {
        return ignoreDiacritic;
    }
    public TextMatcher setIgnoreDiacritic(boolean ignoreDiacritic) {
        this.ignoreDiacritic = ignoreDiacritic;
        return this;
    }
    public TextMatcher withIgnoreDiacritic(boolean ignoreDiacritic) {
        return copy().setIgnoreDiacritic(ignoreDiacritic);
    }

    public boolean isReplaceAll() {
        return replaceAll;
    }
    public TextMatcher setReplaceAll(boolean replaceAll) {
        this.replaceAll = replaceAll;
        return this;
    }
    public TextMatcher withReplaceAll(boolean replaceAll) {
        return copy().setReplaceAll(replaceAll);
    }

    public boolean hasPattern() {
        return StringUtils.isNotBlank(pattern);
    }

    //TODO Really have this since we have copy constructor?
    public void copyTo(TextMatcher sr) {
        BeanUtil.copyProperties(sr, this);
    }
    public void copyFrom(TextMatcher sr) {
        BeanUtil.copyProperties(this, sr);
    }

    private TextMatcher copy() {
        return new TextMatcher(this);
    }

    /**
     * <p>Creates a new text matcher initialized with basic matching.
     * Same as invoking
     * <code>new TextMatcher(Method.BASIC).setPattern(pattern)</code>.</p>
     * @param pattern expression to match against values
     * @return basic text matcher
     */
    public static TextMatcher basic(String pattern) {
        return new TextMatcher(Method.BASIC).setPattern(pattern);
    }
    /**
     * <p>Creates a new text matcher initialized with wildcard matching.
     * Same as invoking
     * <code>new TextMatcher(Method.WILDCARD).setPattern(pattern)</code>.</p>
     * @param pattern expression to match against values
     * @return wildcard text matcher
     */
    public static TextMatcher wildcard(String pattern) {
        return new TextMatcher(Method.WILDCARD).setPattern(pattern);
    }
    /**
     * <p>Creates a new text matcher initialized with regular expression
     * matching. Same as invoking
     * <code>new TextMatcher(Method.REGEX).setPattern(pattern)</code>.</p>
     * @param pattern expression to match against values
     * @return regex text matcher
     */
    public static TextMatcher regex(String pattern) {
        return new TextMatcher(Method.REGEX).setPattern(pattern);
    }

    //--- Match/replace methods ------------------------------------------------
    /**
     * Matches this class pattern against its text.
     * @param text text to match
     * @return <code>true</code> if matching
     */
    public boolean matches(String text) {
        return safeMethod().ms.matches(this, text);
    }
    /**
     * Replaces this class matching text with replacement value.
     * @param text text to match
     * @param replacement text replacement
     * @return replaced text
     */
    public String replace(String text, String replacement) {
        return safeMethod().ms.replace(this, text, replacement);
    }

    private Method safeMethod() {
        return ObjectUtils.defaultIfNull(method, Method.BASIC);
    }

    @Override
    public void loadFromXML(XML xml) {
        setMethod(xml.getEnum("@method", Method.class, method));
        setIgnoreCase(xml.getBoolean("@ignoreCase", ignoreCase));
        setIgnoreDiacritic(xml.getBoolean("@ignoreDiacritic", ignoreDiacritic));
        setReplaceAll(xml.getBoolean("@replaceAll", replaceAll));
        setMatchWhole(xml.getBoolean("@matchWhole", matchWhole));
        setPattern(xml.getString("."));
    }
    @Override
    public void saveToXML(XML xml) {
        xml.setAttribute("method", method);
        xml.setAttribute("ignoreCase", ignoreCase);
        xml.setAttribute("ignoreDiacritic", ignoreDiacritic);
        xml.setAttribute("replaceAll", replaceAll);
        xml.setAttribute("matchWhole", matchWhole);
        xml.setTextContent(pattern);
    }

    @Override
    public boolean equals(final Object other) {
        return EqualsBuilder.reflectionEquals(this, other);
    }
    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }
    @Override
    public String toString() {
        return new ReflectionToStringBuilder(
                this, ToStringStyle.SHORT_PREFIX_STYLE).toString();
    }
}
