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
package com.norconex.commons.lang.xml.flow;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TagTest {
    @Test
    void testTag() {
        assertThat(Tag.IF.isAny(Tag.IFNOT, Tag.CONDITION)).isFalse();
        assertThat(Tag.IF.isAny(Tag.ELSE, Tag.IF)).isTrue();

        assertThat(Tag.isAny("if", Tag.IFNOT, Tag.CONDITION)).isFalse();
        assertThat(Tag.isAny("if", Tag.ELSE, Tag.IF)).isTrue();

        assertThat(Tag.of("then")).isSameAs(Tag.THEN);
        assertThat(Tag.ELSE).hasToString("else");
    }
}
