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
package com.norconex.commons.lang.xml;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import org.junit.jupiter.api.Test;

class XpathUtilTest {

    @Test
    void testAttr() {
        assertThat(XpathUtil.attr("test")).isEqualTo("@test");
        assertThat(XpathUtil.attr("   ")).isEmpty();
        assertThat(XpathUtil.attr(null)).isEmpty();
    }

    @Test
    void testNewXPath() {
        assertThat(XpathUtil.newXPath()).isNotNull();
    }

    @Test
    void testNewXPathExpression() {
        assertThat(XpathUtil.newXPathExpression("my/path")).isNotNull();
        assertThatExceptionOfType(XmlException.class).isThrownBy(() -> {
            XpathUtil.newXPathExpression("$#$%^&");
        });
    }
}
