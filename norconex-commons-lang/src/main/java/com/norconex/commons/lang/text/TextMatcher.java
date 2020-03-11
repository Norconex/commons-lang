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

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.ObjectUtils;
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
 *     partial="[false|true]"
 * }
 *
 * <p>
 * The above are configurable attributes consuming classes can expect.
 * The actual expression is expected to be the tag content.
 * </p>
 *
 * <h3>Null handling</h3>
 * <p>
 * Unless otherwise stated by consuming classes,
 * a <code>null</code> expressions will match everything, but replace nothing.
 * when invoking {@link #matches(CharSequence)} or
 * {@link #replace(String, String)}.
 * </p>
 * <p>
 * When simply matching (no replacements) the attributes are the same,
 * minus the "replaceAll" (which is simply ignored):
 * </p>
 * {@nx.xml #matchAttributes
 *     method="[basic|wildcard|regex]"
 *     ignoreCase="[false|true]"
 *     ignoreDiacritic="[false|true]"
 *     partial="[false|true]"
 * }
 *
 * {@nx.xml.example
 * <sampleConfig method="wildcard" ignoreCase="true" partial="true">
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

    public enum Method {
        BASIC(new MethodStrategy() {
            @Override
            public boolean matches(TextMatcher tm, CharSequence text) {
                if (tm.getPattern() == null) {
                    return true;
                }
                Matcher m = regexMatcher(tm, text);
                return tm.partial ? m.find() : m.matches();
            }
            @Override
            public String replace(
                    TextMatcher tm, String text, String replacement) {
                if (tm.getPattern() == null || replacement == null) {
                    return text;
                }
                String quotedRepl = Matcher.quoteReplacement(replacement);
                Matcher m = regexMatcher(tm, text);
                return doReplace(tm, m, text, quotedRepl);
            }
            @Override
            public Pattern regexPattern(TextMatcher tm) {
                return doRegexPattern(tm, strPattern(tm));
            }
            @Override
            public Matcher regexMatcher(TextMatcher tm, CharSequence text) {
                return doRegexMatcher(tm, strPattern(tm), text);
            }
            private String strPattern(TextMatcher tm) {
                return Regex.escape(Objects.toString(tm.pattern, ""));
            }
        }),
        WILDCARD(new MethodStrategy() {
            @Override
            public boolean matches(TextMatcher tm, CharSequence text) {
                if (tm.getPattern() == null) {
                    return true;
                }
                Matcher m = regexMatcher(tm, text);
                return tm.partial ? m.find() : m.matches();
            }
            @Override
            public String replace(
                    TextMatcher tm, String text, String replacement) {
                if (tm.getPattern() == null || replacement == null) {
                    return text;
                }
                String quotedRepl = Matcher.quoteReplacement(replacement);
                Matcher m = regexMatcher(tm, text);
                return doReplace(tm, m, text, quotedRepl);
            }
            @Override
            public Pattern regexPattern(TextMatcher tm) {
                return doRegexPattern(tm, strPattern(tm));
            }
            @Override
            public Matcher regexMatcher(TextMatcher tm, CharSequence text) {
                return doRegexMatcher(tm, strPattern(tm), text);
            }
            private String strPattern(TextMatcher tm) {
                Pattern p = Pattern.compile("[^*?]+|(\\*)|(\\?)");
                Matcher m = p.matcher(Objects.toString(tm.pattern, ""));
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
                return b.toString();
            }
        }),
        REGEX(new MethodStrategy() {
            @Override
            public boolean matches(TextMatcher tm, CharSequence text) {
                if (tm.getPattern() == null) {
                    return true;
                }
                Matcher m = regexMatcher(tm, text);
                return tm.partial ? m.find() : m.matches();
            }
            @Override
            public String replace(
                    TextMatcher tm, String text, String replacement) {
                if (tm.getPattern() == null || replacement == null) {
                    return text;
                }
                Matcher m = regexMatcher(tm, text);
                return doReplace(tm, m, text, replacement);
            }
            @Override
            public Pattern regexPattern(TextMatcher tm) {
                return doRegexPattern(tm, strPattern(tm));
            }
            @Override
            public Matcher regexMatcher(
                    TextMatcher tm, CharSequence text) {
                return doRegexMatcher(tm, strPattern(tm), text);

            }
            private String strPattern(TextMatcher tm) {
                return Objects.toString(tm.pattern, "");
            }
        });

        private final MethodStrategy ms;

        private Method(MethodStrategy ms) {
            this.ms = ms;
        }

        private static Pattern doRegexPattern(TextMatcher tm, String pattern) {
            return new Regex(pattern)
                    .dotAll()
                    .setIgnoreCase(tm.ignoreCase)
                    .setIgnoreDiacritic(tm.ignoreDiacritic)
                    .compile();
        }
        private static Matcher doRegexMatcher(
                TextMatcher tm, String pattern, CharSequence text) {
            return new Regex(pattern)
                    .dotAll()
                    .setIgnoreCase(tm.ignoreCase)
                    .setIgnoreDiacritic(tm.ignoreDiacritic)
                    .matcher(text);
        }

        private static String doReplace(
                TextMatcher tm, Matcher m, String text, String replacement) {
            if (!tm.partial && m.matches()) {
                return m.replaceFirst(replacement);
            }
            if (tm.partial) {
                if (tm.replaceAll) {
                    return m.replaceAll(replacement);
                }
                return m.replaceFirst(replacement);
            }
            return text;
        }
    }

    private interface MethodStrategy {
        boolean matches(TextMatcher tm, CharSequence text);
        String replace(TextMatcher tm, String text, String replacement);
        Pattern regexPattern(TextMatcher tm);
        Matcher regexMatcher(TextMatcher tm, CharSequence text);
    }

    private Method method = Method.BASIC;

    private String pattern;
    private boolean ignoreCase;
    private boolean ignoreDiacritic;
    private boolean replaceAll;
    private boolean partial;

    /**
     * Creates a basic matcher.
     */
    public TextMatcher() {
        super();
    }
    /**
     * Creates a basic matcher with the given pattern.
     * Default behavior will match the pattern exactly.
     * @param pattern expression used for matching
     */
    public TextMatcher(String pattern) {
        super();
        this.pattern = pattern;
    }
    /**
     * Creates a matcher with the specified method.
     * @param method matching method
     */
    public TextMatcher(Method method) {
        super();
        this.method = method;
    }
    /**
     * Creates a basic matcher with the given pattern.
     * @param pattern expression used for matching
     * @param method matching method
     */
    public TextMatcher(String pattern, Method method) {
        super();
        this.pattern = pattern;
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
            this.partial = textMatcher.partial;
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

    public boolean isPartial() {
        return partial;
    }
    public TextMatcher setPartial(boolean partial) {
        this.partial = partial;
        return this;
    }
    public TextMatcher partial() {
        return setPartial(true);
    }
    public TextMatcher withPartial(boolean partial) {
        return copy().setPartial(partial);
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
    public TextMatcher ignoreCase() {
        return setIgnoreCase(true);
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
    public TextMatcher ignoreDiacritic() {
        return setIgnoreDiacritic(true);
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
    public TextMatcher replaceAll() {
        return setReplaceAll(true);
    }
    public TextMatcher withReplaceAll(boolean replaceAll) {
        return copy().setReplaceAll(replaceAll);
    }

    public void copyTo(TextMatcher tm) {
        BeanUtil.copyProperties(tm, this);
    }
    public void copyFrom(TextMatcher tm) {
        BeanUtil.copyProperties(this, tm);
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
    public boolean matches(CharSequence text) {
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

    /**
     * Converts this text matcher to a pattern {@link Matcher}.
     * @param text text to match using this text matcher method
     * @return matcher
     */
    public Matcher toRegexMatcher(CharSequence text) {
        return safeMethod().ms.regexMatcher(this, text);
    }
    /**
     * Compiles this text matcher to create a regular expression
     * {@link Pattern}.
     * @return pattern
     */
    public Pattern toRegexPattern() {
        return safeMethod().ms.regexPattern(this);
    }

    private Method safeMethod() {
        return ObjectUtils.defaultIfNull(method, Method.BASIC);
    }

    @Override
    public void loadFromXML(XML xml) {
        if (xml == null) {
            return;
        }
        setMethod(xml.getEnum("@method", Method.class, method));
        setIgnoreCase(xml.getBoolean("@ignoreCase", ignoreCase));
        setIgnoreDiacritic(xml.getBoolean("@ignoreDiacritic", ignoreDiacritic));
        setReplaceAll(xml.getBoolean("@replaceAll", replaceAll));
        setPartial(xml.getBoolean("@partial", partial));
        setPattern(xml.getString("."));
    }
    @Override
    public void saveToXML(XML xml) {
        if (xml == null) {
            return;
        }
        xml.setAttribute("method", method);
        xml.setAttribute("ignoreCase", ignoreCase);
        xml.setAttribute("ignoreDiacritic", ignoreDiacritic);
        xml.setAttribute("replaceAll", replaceAll);
        xml.setAttribute("partial", partial);
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
