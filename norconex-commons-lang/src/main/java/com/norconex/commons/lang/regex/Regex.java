package com.norconex.commons.lang.regex;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.ArrayUtils;

/**
 * Builder and utility methods making it easier to construct and use
 * regular expressions.
 * @author Pascal Essiembre
 * @since 2.0.0 (part of it moved from Norconex Importer RegexUtil)
 */
public class Regex {

    public static final int UNICODE_CASE_INSENSTIVE_FLAG =
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE;
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
        if (dotAll) {
            flags.add(Pattern.DOTALL);
        } else {
            flags.remove(Pattern.DOTALL);
        }
        return this;
    }
    public boolean isDotAll() {
        return flags.contains(Pattern.DOTALL);
    }

    public Regex caseInsensitive() {
        return setCaseInsensitive(true);
    }
    public Regex setCaseInsensitive(boolean caseInsensitive) {
        if (caseInsensitive) {
            flags.add(UNICODE_CASE_INSENSTIVE_FLAG);
        } else {
            flags.remove(UNICODE_CASE_INSENSTIVE_FLAG);
        }
        return this;
    }
    public boolean isCaseInsensitive() {
        return flags.contains(UNICODE_CASE_INSENSTIVE_FLAG);
    }

    public Regex unixLines() {
        return setUnixLines(true);
    }
    public Regex setUnixLines(boolean unixLines) {
        if (unixLines) {
            flags.add(Pattern.UNIX_LINES);
        } else {
            flags.remove(Pattern.UNIX_LINES);
        }
        return this;
    }
    public boolean isUnixLines() {
        return flags.contains(Pattern.UNIX_LINES);
    }

    public Regex literal() {
        return setLiteral(true);
    }
    public Regex setLiteral(boolean literal) {
        if (literal) {
            flags.add(Pattern.LITERAL);
        } else {
            flags.remove(Pattern.LITERAL);
        }
        return this;
    }
    public boolean isLiteral() {
        return flags.contains(Pattern.LITERAL);
    }

    public Regex comments() {
        return setComments(true);
    }
    public Regex setComments(boolean comments) {
        if (comments) {
            flags.add(Pattern.COMMENTS);
        } else {
            flags.remove(Pattern.COMMENTS);
        }
        return this;
    }
    public boolean isComments() {
        return flags.contains(Pattern.COMMENTS);
    }

    public Regex multiline() {
        return setMultiline(true);
    }
    public Regex setMultiline(boolean multiline) {
        if (multiline) {
            flags.add(Pattern.MULTILINE);
        } else {
            flags.remove(Pattern.MULTILINE);
        }
        return this;
    }
    public boolean isMultiline() {
        return flags.contains(Pattern.MULTILINE);
    }

    public Regex canonEq() {
        return setCanonEq(true);
    }
    public Regex setCanonEq(boolean canonEq) {
        if (canonEq) {
            flags.add(Pattern.CANON_EQ);
        } else {
            flags.remove(Pattern.CANON_EQ);
        }
        return this;
    }
    public boolean isCanonEq() {
        return flags.contains(Pattern.CANON_EQ);
    }

    public Regex unicode() {
        return setUnicode(true);
    }
    public Regex setUnicode(boolean unicode) {
        if (unicode) {
            flags.add(Pattern.UNICODE_CHARACTER_CLASS);
        } else {
            flags.remove(Pattern.UNICODE_CHARACTER_CLASS);
        }
        return this;
    }
    public boolean isUnicode() {
        return flags.contains(Pattern.UNICODE_CHARACTER_CLASS);
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
     * Compiles a previously set pattern.
     * @return compiled pattern
     */
    public Pattern compile() {
        return compile(pattern);
    }
    /**
     * Compiles the given pattern without assigning it to this object.
     * @param pattern the pattern to compile
     * @return compiled pattern
     */
    public Pattern compile(String pattern) {
        if (pattern == null) {
            throw new IllegalArgumentException("Pattern cannot be null.");
        }
        int f = 0;
        for (int i : flags) {
            f |= i;
        }
        return Pattern.compile(pattern, f);
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
        return new Regex(regex).dotAll().setCaseInsensitive(
                caseInsensitive).compile();
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
        return compile(pattern).matcher(text);
    }
}
