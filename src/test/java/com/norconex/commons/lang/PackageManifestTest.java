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
package com.norconex.commons.lang;

import static org.apache.commons.lang3.StringUtils.substringBefore;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.norconex.commons.lang.version.SemanticVersion;

class PackageManifestTest {

    @Test
    void testPackageManifest() {
        PackageManifest pm = PackageManifest.of(getClass());

        assertThat(pm.isEmpty()).isFalse();

        assertThat(Integer.parseInt(substringBefore(pm.getVersion(), ".")))
                .isGreaterThanOrEqualTo(3);
        assertThat(pm.getVendor()).isEqualTo("Norconex Inc.");
        assertThat(pm.getTitle()).isEqualTo("Norconex Commons Lang");

        assertThat(pm.toString()).matches(
                "^Norconex Commons Lang \\d+.\\d+.\\d+.* "
                        + "\\(Norconex Inc\\.\\)$");

        assertThat(pm.getSemanticVersion().isGreaterOrEquivalentTo(
                SemanticVersion.builder()
                        .major(3)
                        .preRelease("SNAPSHOT")
                        .build())).isTrue();
    }
}
