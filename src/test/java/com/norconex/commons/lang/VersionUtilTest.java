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

@Deprecated
class VersionUtilTest {

    @Test
    void testGetVersionClass() {
        assertThat(Integer.parseInt(
                substringBefore(VersionUtil.getVersion(getClass()), ".")))
                        .isGreaterThanOrEqualTo(3);
    }

    @Test
    void testGetVersionClassString() {
        assertThat(VersionUtil.getVersion(
                String.class, "default")).isEqualTo("default");
    }

    @Test
    void testGetDetailedVersionClass() {
        assertThat(VersionUtil.getDetailedVersion(
                getClass())).startsWith("Norconex Commons Lang");
    }

    @Test
    void testGetDetailedVersionClassString() {
        assertThat(VersionUtil.getDetailedVersion(
                String.class, "default")).isEqualTo("default");
    }
}
