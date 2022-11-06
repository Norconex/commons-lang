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

import static org.apache.commons.lang3.StringUtils.lowerCase;
import static org.apache.commons.lang3.StringUtils.trimToNull;

import java.util.Comparator;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.apache.commons.collections4.comparators.NullComparator;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.CompareToBuilder;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * <p>
 * Immutable semantic version representation, conforming to
 * <a href="https://semver.org/">https://semver.org/</a> specifications.
 * </p>
 * @see SemanticVersionParser
 * @since 3.0.0
 */
@Getter
@EqualsAndHashCode
@Builder
@AllArgsConstructor(access = AccessLevel.PACKAGE)
public class SemanticVersion implements Comparable<SemanticVersion> {

    /**
     * A special instance that represents a non-versioned entity.
     * This is an alternative to using a <code>null</code>
     * value when there is no version.
     * Sets major, minor, and patch values to -1.
     */
    public static final SemanticVersion UNVERSIONED = new SemanticVersion(
            -1, -1, -1, null, null);

    // evaluated in ordinal order
    // Used against non-null, trimmed, and lower-cased values
    private enum PreRelease{
        SNAPSHOT(0, s -> s.startsWith("snapshot")),
        MILESTONE(1, s -> s.matches("^(m|m[^a-zA-Z].*)$")),
        ALPHA(2, s -> s.startsWith("alpha")),
        BETA(3, s -> s.startsWith("beta")),
        RELEASE_CANDIDATE(4, s -> s.matches("^(rc|rc[^a-zA-Z].*)$")),
        FINAL(5, s -> s.startsWith("final")),
        RELEASE(5, s -> s.startsWith("release")),
        STABLE(5, s -> s.startsWith("stable")),
        UNKNOWN(0, s -> true),
        ;

        private final int weight;
        private final Predicate<String> predicate;
        PreRelease(int weight, Predicate<String> predicate) {
            this.weight = weight;
            this.predicate = predicate;
        }
        static PreRelease of(String preRelease) {
            if (StringUtils.isBlank(preRelease)) {
                return UNKNOWN;
            }
            return Stream.of(PreRelease.values())  //NOSONAR
                .filter(pr -> pr.predicate.test(preRelease))
                .findFirst()
                .get(); // never null
        }
    }

    private static final Comparator<String> nullComparator =
            new NullComparator<>((o1, o2) -> 0);

    private static final Pattern numberPattern = Pattern.compile("\\d+");

    private final int major;
    private final int minor;
    private final int patch;
    private final String preRelease;
    private final String buildMetadata;

    /**
     * Create a new semantic version with the minor and patch numbers set
     * to zero (0).
     * @param major major number
     * @return a semantic version
     */
    public static SemanticVersion of(int major) {
        return of(major, 0, 0, null, null);
    }

    /**
     * Create a new semantic version with the patch number set to zero (0).
     * @param major major number
     * @param minor minor number
     * @return a semantic version
     */
    public static SemanticVersion of(int major, int minor) {
        return of(major, minor, 0, null, null);
    }

    /**
     * Create a new semantic version.
     * @param major major number
     * @param minor minor number
     * @param patch patch number
     * @return a semantic version
     */
    public static SemanticVersion of(int major, int minor, int patch) {
        return of(major, minor, patch, null, null);
    }

    /**
     * Create a new semantic version.
     * @param major major number
     * @param minor minor number
     * @param patch patch number
     * @param preRelease pre-release qualifier (can be <code>null</code>)
     * @return a semantic version
     */
    public static SemanticVersion of(int major, int minor, int patch,
            String preRelease) {
        return of(major, minor, patch, preRelease, null);
    }

    /**
     * Create a new semantic version.
     * @param major major number
     * @param minor minor number
     * @param patch patch number
     * @param preRelease pre-release qualifier (can be <code>null</code>)
     * @param buildMetadata build metadata (can be <code>null</code>)
     * @return a semantic version
     */
    public static SemanticVersion of(int major, int minor, int patch,
            String preRelease, String buildMetadata) {
        return new SemanticVersion(
                major, minor, patch, preRelease, buildMetadata);
    }

    /**
     * Gets whether this semantic version is representing something
     * non-versioned. Creating an instance with any of major, minor, and
     * patch values being less than zero is considered non-versioned.
     * A non-versioned instance will always compare lower than
     * a versioned one.
     * @return <code>true</code> if versioned
     */
    public boolean isVersioned() {
        return major >=0 && minor >= 0 && patch >= 0;
    }

    @Override
    public int compareTo(SemanticVersion o) {
        if (equals(o)) {
            return 0;
        }

        // Compare major.minor.patch
        int result = new CompareToBuilder()
                .append(major, o.major)
                .append(minor, o.minor)
                .append(patch, o.patch)
                .toComparison();
        if (result != 0) {
            return result;
        }

        // Compare pre-release
        String thisPrTxt = lowerCase(trimToNull(preRelease));
        String thatPrTxt = lowerCase(trimToNull(o.preRelease));

        if (Objects.equals(thisPrTxt, thatPrTxt)) {
            return 0;
        }
        result = nullComparator.compare(thisPrTxt, thatPrTxt);
        if (result != 0) {
            return result;
        }

        // both pre-releases can't be null at this point
        PreRelease thisPr = PreRelease.of(thisPrTxt);
        PreRelease thatPr = PreRelease.of(thatPrTxt);
        result = Integer.compare(thisPr.weight, thatPr.weight);
        if (result != 0) {
            return result;
        }

        // extract numerical values (if any) and compare them.
        Matcher thisMatcher = numberPattern.matcher(thisPrTxt);
        Matcher thatMatcher = numberPattern.matcher(thatPrTxt);
        while (thisMatcher.find()) {
            // if this matcher has more numbers, it is higher
            if (!thatMatcher.find()) {
                return 1;
            }
            result = Integer.compare(
                    Integer.parseInt(thisMatcher.group()),
                    Integer.parseInt(thatMatcher.group()));
            if (result != 0) {
                return result;
            }
        }

        // at this point, if there are more matches in that matcher,
        // we assume it is higher
        if (thatMatcher.find()) {
            return -1;
        }
        return 0;
    }

    /**
     * Gets whether this version is semantically equivalent to the other one.
     * @param other other version we are comparing to
     * @return <code>true</code> if this version is equivalent to the other
     */
    public boolean isEquivalentTo(SemanticVersion other) {
        return compareTo(other) == 0;
    }
    /**
     * Gets whether this version is semantically greater than the other one.
     * @param other other version we are comparing to
     * @return <code>true</code> if this version is greater than the other
     */
    public boolean isGreaterThan(SemanticVersion other) {
        return compareTo(other) > 0;
    }
    /**
     * Gets whether this version is semantically greater or equivalent to
     * the other one.
     * @param other other version we are comparing to
     * @return <code>true</code> if this version is greater or equivalent
     *      to the other
     */
    public boolean isGreaterOrEquivalentTo(SemanticVersion other) {
        return compareTo(other) >= 0;
    }
    /**
     * Gets whether this version is semantically lower than the other one.
     * @param other other version we are comparing to
     * @return <code>true</code> if this version is lower than the other
     */
    public boolean isLowerThan(SemanticVersion other) {
        return compareTo(other) < 0;
    }
    /**
     * Gets whether this version is semantically lower or equivalent to
     * the other one.
     * @param other other version we are comparing to
     * @return <code>true</code> if this version is lower or equivalent
     *      to the other
     */
    public boolean isLowerOrEquivalentTo(SemanticVersion other) {
        return compareTo(other) <= 0;
    }

    /**
     * A friendly representation of this semantic version.
     */
    @Override
    public String toString() {
        StringBuilder b = new StringBuilder()
                .append(major).append('.')
                .append(minor).append('.')
                .append(patch);
        if (StringUtils.isNotBlank(preRelease)) {
            b.append("-").append(preRelease);
        }
        if (StringUtils.isNotBlank(buildMetadata)) {
            b.append("+").append(buildMetadata);
        }
        return b.toString();
    }
}
