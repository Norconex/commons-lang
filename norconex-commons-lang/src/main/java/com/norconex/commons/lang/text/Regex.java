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

import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * <P>
 * Builder and utility methods making it easier to construct and use
 * regular expressions.
 * </p>
 * @author Pascal Essiembre
 * @since 2.0.0
 */
public class Regex {

    /**
     * Flag that ignores diacritical marks when matching or replacing
     * (e.g. accents).
     * This flag is not supported by Java {@link Pattern} and only
     * works when used with this class.
     */
    public static final int UNICODE_MARK_INSENSTIVE_FLAG = 0x10000;
    /**
     * Convenience flag that combines {@link Pattern#UNICODE_CASE}
     * and {@link Pattern#CASE_INSENSITIVE}
     */
    public static final int UNICODE_CASE_INSENSTIVE_FLAG =
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE;

    private static final Pattern ESCAPE_PATTERN = Pattern.compile(
            "[" + ("<([{\\^-=$!|]})?*+.>".replaceAll(".", "\\\\$0")) + "]");

    private String pattern;
    private final Set<Integer> flags = new HashSet<>();

    public Regex() {
        super();
    }
    public Regex(String pattern) {
        super();
        this.pattern = pattern;
    }
    public Regex(String pattern, int... flags) {
        super();
        this.pattern = pattern;
        this.flags.addAll(Arrays.asList(ArrayUtils.toObject(flags)));
    }

    public Regex dotAll() {
        return setDotAll(true);
    }
    public Regex setDotAll(boolean dotAll) {
        return setFlag(Pattern.DOTALL, dotAll);
    }
    public boolean isDotAll() {
        return flags.contains(Pattern.DOTALL);
    }

    public Regex ignoreCase() {
        return setIgnoreCase(true);
    }
    public Regex setIgnoreCase(boolean ignoreCase) {
        return setFlag(UNICODE_CASE_INSENSTIVE_FLAG, ignoreCase);
    }
    public boolean isIgnoreCase() {
        return flags.contains(UNICODE_CASE_INSENSTIVE_FLAG);
    }

    public Regex unixLines() {
        return setUnixLines(true);
    }
    public Regex setUnixLines(boolean unixLines) {
        return setFlag(Pattern.UNIX_LINES, unixLines);
    }
    public boolean isUnixLines() {
        return flags.contains(Pattern.UNIX_LINES);
    }

    public Regex literal() {
        return setLiteral(true);
    }
    public Regex setLiteral(boolean literal) {
        return setFlag(Pattern.LITERAL, literal);
    }
    public boolean isLiteral() {
        return flags.contains(Pattern.LITERAL);
    }

    public Regex comments() {
        return setComments(true);
    }
    public Regex setComments(boolean comments) {
        return setFlag(Pattern.COMMENTS, comments);
    }
    public boolean isComments() {
        return flags.contains(Pattern.COMMENTS);
    }

    public Regex multiline() {
        return setMultiline(true);
    }
    public Regex setMultiline(boolean multiline) {
        return setFlag(Pattern.MULTILINE, multiline);
    }
    public boolean isMultiline() {
        return flags.contains(Pattern.MULTILINE);
    }

    public Regex canonEq() {
        return setCanonEq(true);
    }
    public Regex setCanonEq(boolean canonEq) {
        return setFlag(Pattern.CANON_EQ, canonEq);
    }
    public boolean isCanonEq() {
        return flags.contains(Pattern.CANON_EQ);
    }

    public Regex unicode() {
        return setUnicode(true);
    }
    public Regex setUnicode(boolean unicode) {
        return setFlag(Pattern.UNICODE_CHARACTER_CLASS, unicode);
    }
    public boolean isUnicode() {
        return flags.contains(Pattern.UNICODE_CHARACTER_CLASS);
    }

    /**
     * Ignores diacritical marks when matching or replacing
     * (e.g. accents).
     * @return this instance
     */
    public Regex ignoreDiacritic() {
        return setIgnoreDiacritic(true);
    }
    public Regex setIgnoreDiacritic(boolean ignoreDiacritic) {
        return setFlag(UNICODE_MARK_INSENSTIVE_FLAG, ignoreDiacritic);
    }
    public boolean isIgnoreDiacritic() {
        return flags.contains(UNICODE_MARK_INSENSTIVE_FLAG);
    }

    public void setFlags(int... flags) {
        this.flags.clear();
        if (flags != null) {
            this.flags.addAll(Arrays.asList(ArrayUtils.toObject(flags)));
        }
    }
    public Set<Integer> getFlags() {
        return Collections.unmodifiableSet(flags);
    }

    public Regex setPattern(String pattern) {
        this.pattern = pattern;
        return this;
    }
    public String getPattern() {
        return pattern;
    }

    /**
     * <p>
     * Compiles a previously set pattern.
     * </p>
     * <p>
     * For text-matching with diacritical mark insensitivity support enabled,
     * use {@link #matcher(CharSequence)} instead.
     * </p>
     * @return compiled pattern
     */
    public Pattern compile() {
        return compile(pattern);
    }
    /**
     * <p>
     * Compiles the given pattern without assigning it to this object.
     * </p>
     * <p>
     * For text-matching with diacritical mark insensitivity support enabled,
     * use {@link #matcher(String, CharSequence)} instead.
     * </p>
     * @param pattern the pattern to compile
     * @return compiled pattern
     */
    public Pattern compile(String pattern) {
        if (pattern == null) {
            throw new IllegalArgumentException("Pattern cannot be null.");
        }
        boolean ignoreMarks = false;
        int f = 0;
        for (int i : flags) {
            if (i == UNICODE_MARK_INSENSTIVE_FLAG) {
                ignoreMarks = true;
            } else {
                f |= i;
            }
        }

        String p = pattern;
        if (ignoreMarks) {
            p = Normalizer.normalize(p, Form.NFD)
                    .replaceAll("(\\w)(\\p{M}*)", "$1\\\\p{M}*");
        }
        return Pattern.compile(p, f);
    }

    /**
     * Compiles a "dotall" pattern (dots match all, including new lines)
     * with optional case sensitivity.
     *
     * @param regex regular expression
     * @param caseInsensitive <code>true</code> to ignore character case.
     * @return compiled pattern
     */
    public static Pattern compileDotAll(String regex, boolean caseInsensitive) {
        // we allow empty regex here, but not null ones
        if (regex == null) {
            throw new IllegalArgumentException(
                    "Supplied regular expression cannot be null");
        }
        return new Regex(regex).dotAll().setIgnoreCase(
                caseInsensitive).compile();
    }

    /**
     * Escape special characters with a backslash (\) in a regular expression.
     * This is an alternative
     * to {@link Pattern#quote(String)} for when you do not want the string
     * to be treated as a literal.
     * @param pattern the pattern to escape
     * @return escaped pattern
     */
    public static String escape(String pattern) {
        return ESCAPE_PATTERN.matcher(pattern).replaceAll("\\\\$0");
    }

    /**
     * Matches the previously set pattern against the given text.
     * @param text the text to match
     * @return matcher
     */
    public Matcher matcher(CharSequence text) {
        return matcher(pattern, text);
    }
    /**
     * Matches the the given pattern against the given text without assigning
     * the pattern to this object.
     * @param pattern the pattern to match
     * @param text the text to match
     * @return matcher
     */
    public Matcher matcher(String pattern, CharSequence text) {
        if (text == null) {
            throw new IllegalArgumentException("Text cannot be null.");
        }
        CharSequence t = text;
        if (flags.contains(UNICODE_MARK_INSENSTIVE_FLAG)) {
            t = Normalizer.normalize(t, Form.NFD);
        }
        return compile(pattern).matcher(t);
    }

    public RegexKeyValueExtractor createKeyValueExtractor() {
        return new RegexKeyValueExtractor(this);
    }
    public RegexKeyValueExtractor createKeyValueExtractor(String key) {
        return new RegexKeyValueExtractor(this, key);
    }
    public RegexKeyValueExtractor createKeyValueExtractor(
            String key, int valueGroup) {
        return new RegexKeyValueExtractor(this, key, valueGroup);
    }
    public RegexKeyValueExtractor createKeyValueExtractor(
            int keyGroup, int valueGroup) {
        return new RegexKeyValueExtractor(this, keyGroup, valueGroup);
    }

    private Regex setFlag(int flag, boolean bool) {
        if (bool) {
            flags.add(flag);
        } else {
            flags.remove(flag);
        }
        return this;
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
