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

import static com.norconex.commons.lang.version.SemanticVersionParser.LENIENT;
import static com.norconex.commons.lang.version.SemanticVersionParser.STRICT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;

class SemanticVersionParserTest {

    @Test
    void testStrictParser() {
        assertStrict("0.0.1", 0, 0, 1);
        assertStrict("2.0.0-RC", 2, 0, 0, "RC", null);
        assertStrict("1.2.3-M1", 1, 2, 3, "M1", null);
        assertStrict("1.2.3-M1+build456", 1, 2, 3, "M1", "build456");
        assertStrict("1.2.3-M1a2b3c", 1, 2, 3, "M1a2b3c", null);
        assertStrict("1.2.3+build456", 1, 2, 3, null, "build456");

        assertStrictFail("1.2");
        assertStrictFail("2");
        assertStrictFail("1.2.3_M1");
        assertStrictFail("1.2.3M1");
        assertStrictFail("1.2.3_M1+build456");
        assertStrictFail("1.2.3M1+build456");
        assertStrictFail("1.2.3.Final");
        assertStrictFail("v1.2.3");
        assertStrictFail("v1.2.3.jar");
        assertStrictFail("v1.2.3M1-54.jar");
        assertStrictFail("c:\\dir-1.2.3\\v1.2.3M1-54+b456.jar");
        assertStrictFail(StringUtils.repeat('v', 300) + "1.2.3");
        assertStrictFail("1.2.3+" + StringUtils.repeat('b', 300));
        assertStrictFail("not-a-version");
    }

    @Test
    void testLenientParser() {
        assertLenient("0.0.1", 0, 0, 1);
        assertLenient("2.0.0-RC", 2, 0, 0, "RC", null);
        assertLenient("1.2.3-M1", 1, 2, 3, "M1", null);
        assertLenient("1.2.3-M1+build456", 1, 2, 3, "M1", "build456");
        assertLenient("1.2.3-M1a2b3c", 1, 2, 3, "M1a2b3c", null);
        assertLenient("1.2.3+build456", 1, 2, 3, null, "build456");

        assertLenient("1.2", 1, 2, 0);
        assertLenient("2", 2, 0, 0);
        assertLenient("1.2.3_M1", 1, 2, 3, "M1", null);
        assertLenient("1.2.3M1", 1, 2, 3, "M1", null);
        assertLenient("1.2.3_M1+build456", 1, 2, 3, "M1", "build456");
        assertLenient("1.2.3M1+build456", 1, 2, 3, "M1", "build456");
        assertLenient("1.2.3.Final", 1, 2, 3, "Final", null);
        assertLenient("v1.2.3", 1, 2, 3);
        assertLenient("v1.2.3.jar", 1, 2, 3);
        assertLenient("v1.2.3M1-54.jar", 1, 2, 3, "M1-54", null);
        assertLenient("c:\\dir-1.2.3\\v1.2.3M1-54+b456.jar",
                1, 2, 3, "M1-54", "b456");
        assertLenient(StringUtils.repeat('v', 300) + "1.2.3", 1, 2, 3);

        assertLenientFail("1.2.3+" + StringUtils.repeat('b', 300));
        assertLenientFail("not-a-version");
    }

    @Test
    void testNull() {
        assertThrows(SemanticVersionParserException.class, () -> {
           SemanticVersionParser.STRICT.parse(null);
        });
        assertThrows(SemanticVersionParserException.class, () -> {
            SemanticVersionParser.STRICT.parse(null);
        });
    }

    private void assertStrict(
            String version, int major, int minor, int patch) {
        assertThat(STRICT.parse(version)).isEqualTo(
                semver(major, minor, patch));
    }
    private void assertStrict(
            String version, int major, int minor, int patch,
            String preRelease, String meta) {
        assertThat(STRICT.parse(version)).isEqualTo(
                semver(major, minor, patch, preRelease, meta));
    }
    private void assertStrictFail(String version) {
        assertThrows(SemanticVersionParserException.class,
                () -> STRICT.parse(version));
    }
    private void assertLenient(
            String version, int major, int minor, int patch) {
        assertThat(LENIENT.parse(version)).isEqualTo(
                semver(major, minor, patch));
    }
    private void assertLenient(
            String version, int major, int minor, int patch,
            String preRelease, String meta) {
        assertThat(LENIENT.parse(version)).isEqualTo(
                semver(major, minor, patch, preRelease, meta));
    }
    private void assertLenientFail(String version) {
        assertThrows(SemanticVersionParserException.class,
                () -> LENIENT.parse(version));
    }

    private SemanticVersion semver(int major, int minor, int patch) {
        return semver(major, minor, patch, null, null);
    }
    private SemanticVersion semver(
            int major, int minor, int patch, String preRelease, String meta) {
        return SemanticVersion.of(
                major,
                minor,
                patch,
                preRelease,
                meta);
    }
}
