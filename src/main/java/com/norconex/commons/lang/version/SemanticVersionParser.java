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
package com.norconex.commons.lang.version;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Singular;

/**
 * <p>
 * Semantic version string parser. Default parser settings conform to
 * <a href="https://semver.org/">https://semver.org/</a> specifications.
 * Use available builder options for more lenient parsing.
 * </p>
 * <h3>Max version length</h3>
 * <p>
 * The maximum string length supported by this parser is 255 characters.
 * Longer strings will throw a {@link SemanticVersionParserException}
 * except when {@link #ignoreLeadingCharacters} is <code>true</code>. In such
 * case, it will try parsing longer strings, but will only
 * consider the last 255 characters.
 * </p>
 * @since 3.0.0
 */
@EqualsAndHashCode
@Builder
public class SemanticVersionParser {

    /**
     * Default parser settings, using strict adherence to
     * <a href="https://semver.org/">SemVer</a> specifications.
     */
    public static final SemanticVersionParser STRICT = builder().build();

    /**
     * A relaxed parser with support for optional minor and patch numbers,
     * ignoring leading characters, and ignoring the following suffix
     * strings:
     * <code>.jar</code>, <code>.zip</code>, <code>.exe</code>,
     * <code>.gz</code>, <code>.tgz</code>, <code>.tar</code>,
     * and <code>.tar.gz</code>,
     */
    public static final SemanticVersionParser LENIENT = builder()
            .ignoreLeadingCharacters(true)
            .optionalMinorAndPatch(true)
            .optionalPreReleasePrefix(true)
            .ignoreSuffixes(Arrays.asList(
                    ".jar", ".zip", ".exe", ".tgz", ".gz", ".tgz",
                    ".tar", ".tar.gz"))
            .build();

    /**
     * Same as {@link #STRICT} except for ignoring non-numeric leading
     * characters before the version (e.g., <code>v1.2.3</code>).
     */
    public static final SemanticVersionParser TAG = builder()
            .ignoreLeadingCharacters(true)
            .build();

    // We use Semver-provided regular expression, modified to support
    // optional minor, patch, and pre-release prefix, as well, as support
    // for ignoring leading characters. We ignore sonar warning as we
    // make sure the string is within reasonable length.
    private static final Pattern semverPattern = Pattern.compile(
            "^(?<leadingchars>.*?)"
                    + "(?<major>0|[1-9]\\d*)"
                    + "(?:\\.(?<minor>0|[1-9]\\d*))?"
                    + "(?:\\.(?<patch>0|[1-9]\\d*))?"
                    + "(?:"
                    + "(?<prereleaseprefix>[^0-9a-zA-Z\\+])?"
                    + "(?<prerelease>"
                    + "(?:0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*)"
                    + "(?:\\.(?:0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*))*"//NOSONAR
                    + ")"
                    + ")?"
                    + "(?:\\+(?<buildmetadata>[0-9a-zA-Z-]+(?:\\.[0-9a-zA-Z-]+)*))?"
                    + "$",
            Pattern.MULTILINE);

    /**
     * <p>
     * Enables supports for version strings not having the minor or patch
     * segment. They are interpreted as if they were zero. Examples:
     * </p>
     * <ul>
     *   <li><code>1.2</code> is interpreted as <code>1.2.0</code></li>
     *   <li><code>3</code> is interpreted as <code>3.0.0</code></li>
     * </ul>
     * @param optionalMinorAndPatch <code>true</code> if minor and patch
     *     numbers are optional
     * @return {@code this}.
     */
    @SuppressWarnings("javadoc")
    private final boolean optionalMinorAndPatch;
    /**
     * <p>
     * Enables supports for pre-release prefix separator string to be any
     * number of non alpha-numeric characters, including none
     * (i.e., having the pre-release string next to version). Examples:
     * </p>
     * <ul>
     *   <li><code>1.2.3RC1</code> is interpreted as <code>1.2.3-RC1</code></li>
     *   <li><code>1.2.3_M3</code> is interpreted as <code>1.2.3-M3</code></li>
     * </ul>
     * @param optionalPreReleasePrefix <code>true</code> if pre-release prefix
     *     presence is optional
     * @return {@code this}.
     */
    @SuppressWarnings("javadoc")
    private final boolean optionalPreReleasePrefix;
    /**
     * <p>
     * Performs a best attempt to ignore any non-numeric characters before the
     * version. In the event that more than one valid variation of a version
     * is be detected, the outcome is unpredictable. Examples:
     * </p>
     * <ul>
     *   <li><code>v1.2.3</code> is interpreted as <code>1.2.3</code></li>
     *   <li><code>some-library-1.2.3</code> is interpreted as <code>1.2.3</code></li>
     * </ul>
     * @param ignoreLeadingCharacters <code>true</code> if ignoring leading
     *     characters
     * @return {@code this}.
     */
    @SuppressWarnings("javadoc")
    private final boolean ignoreLeadingCharacters;
    /**
     * <p>
     * Ignores matching trailing text after the version. If you specify
     * more than one suffix, only the first one matching (in order supplied)
     * will be ignored.
     * Example ignoring the string <code>.jar</code>:
     * </p>
     * <ul>
     *   <li><code>1.2.3.jar</code> is interpreted as <code>1.2.3</code></li>
     * </ul>
     * @param ignoreSuffixes suffixes to be ignored when extracting version
     * @return {@code this}.
     */
    @SuppressWarnings("javadoc")
    @Singular(ignoreNullCollections = true, value = "ignoreSuffix")
    private List<String> ignoreSuffixes;

    /**
     * Parses a semantic version string.
     * @param version the string to parse
     * @return a semantic version instance
     * @throws SemanticVersionParserException if the version can't be parsed.
     */
    public SemanticVersion parse(String version) {
        String v = StringUtils.trimToNull(version);

        if (v == null) {
            throw new SemanticVersionParserException(
                    "Version must neither be null or blank.");
        }

        v = handleLongVersionString(v);

        if (ignoreLeadingCharacters) {
            // we strip everything we know are before invalid characters
            v = v.replaceFirst("^.*[^\\w\\.\\+-](.*)$", "$1");
        }
        for (String suffix : ignoreSuffixes) {
            String before = v;
            v = StringUtils.removeEnd(v, suffix);
            if (Objects.equals(before, v)) {
                break;
            }
        }

        Matcher m = semverPattern.matcher(v);
        if (!m.matches()) {
            throw new SemanticVersionParserException(
                    "Could not parse: " + version);
        }

        String leadingChars = m.group("leadingchars");
        String major = m.group("major");
        String minor = m.group("minor");
        String patch = m.group("patch");
        String preReleasePrefix = m.group("prereleaseprefix");
        String preRelease = m.group("prerelease");
        String metadata = m.group("buildmetadata");

        if (!ignoreLeadingCharacters && StringUtils.isNotBlank(leadingChars)) {
            throw new SemanticVersionParserException(
                    "Version does not start with a numeric value: " + version);
        }
        if (!optionalMinorAndPatch && (minor == null || patch == null)) {
            throw new SemanticVersionParserException(
                    "Version is missing minor and/or patch numbers: "
                            + version);
        }
        if (!optionalPreReleasePrefix
                && preRelease != null
                && !"-".equals(preReleasePrefix)) {
            throw new SemanticVersionParserException(
                    "Version pre-release prefix '-' is missing: " + version);
        }
        return SemanticVersion.of(
                NumberUtils.toInt(major),
                NumberUtils.toInt(minor),
                NumberUtils.toInt(patch),
                preRelease,
                metadata);
    }

    private String handleLongVersionString(String v) {
        if (v.length() > 255 && !ignoreLeadingCharacters) {
            throw new SemanticVersionParserException(
                    "Version is too long (over 255 characters): " + v);
        }
        return StringUtils.substring(v, -256);
    }
}
