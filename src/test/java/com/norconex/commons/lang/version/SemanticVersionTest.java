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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

class SemanticVersionTest {

    @Test
    void testVersionComparison() {
        List<String> expected = Arrays.asList(
            "0.0.6",
            "0.5.0",
            "1.0.0",
            "1.1.1",
            "1.2.0",
            "2.0.0-RC",
            "3.0.0-SNAPSHOT",
            "3.0.0-M1",
            "3.0.0-alpha",
            "3.0.0-alpha1",
            "3.0.0-beta",
            "3.0.0-RC1",
            "3.0.0-RC3a2b",
            "3.0.0-RC3a2b3c",
            "3.0.0-RC12a2b",
            "3.0.0-RELEASE",
            "3.0.0",
            "3.1.0-snapshot+build66",
            "3.1.0-Stable+build33",
            "3.1.0+build33",
            "3.10.10"
        );

        // Source is the same, shuffled, with a few entries modified
        // to test versions without minor and patch segments.
        List<String> source = new ArrayList<>(expected);
        Collections.shuffle(source);

        Set<SemanticVersion> set = new TreeSet<>();
        source.forEach(v -> set.add(SemanticVersionParser.STRICT.parse(v)));
        List<String> actual = set.stream()
                .map(SemanticVersion::toString)
                .collect(Collectors.toList());
        assertThat(actual).containsExactlyElementsOf(expected);
    }

    @Test
    void testMisc() {

        assertThat(parse("1.0.0-M1+asdf").isEquivalentTo(
                parse("1.0.0-M1a+qwert"))).isTrue();
        assertThat(parse("1.0.0-M1+asdf").isLowerThan(
                parse("1.0.0-M2+asdf"))).isTrue();

        assertThat(parse("1.0.0-snapshot")
                .isLowerThan(parse("1.0.0"))).isTrue();
        assertThat(parse("1.0.0-snapshot")
                .isLowerOrEquivalentTo(parse("1.0.0"))).isTrue();
        assertThat(parse("1.0.0-snapshot")
                .isEquivalentTo(parse("1.0.0"))).isFalse();
        assertThat(parse("1.0.0-snapshot")
                .isGreaterThan(parse("1.0.0"))).isFalse();
        assertThat(parse("1.0.0-snapshot")
                .isGreaterOrEquivalentTo(parse("1.0.0"))).isFalse();

        assertThat(SemanticVersion.builder()
                .major(1).minor(2).patch(-1).build().isVersioned()).isFalse();

        assertThat(parse("2.0.0")).hasToString("2.0.0");
        assertThat(parse("2.1.0-M2ab+b345")).hasToString("2.1.0-M2ab+b345");

        assertThat(SemanticVersion.builder()
                .major(1).minor(2).patch(3).preRelease("").build()
                .isVersioned()).isTrue();
    }

    private static SemanticVersion parse(String v) {
        return SemanticVersionParser.STRICT.parse(v);
    }
}
