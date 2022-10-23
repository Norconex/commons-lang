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
package com.norconex.commons.lang.io;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;

class RegexInputStreamFilterTest {

    @Test
    void testRegexInputStreamFilter() throws IOException {
        try (FilteredInputStream is = new FilteredInputStream(
                IOUtils.toInputStream("ab\ncd\nef", UTF_8),
                new RegexInputStreamFilter("(ab|ef)"))) {
            assertThat(IOUtils.toString(is, UTF_8)).isEqualTo("ab\nef\n");
        }

        try (FilteredInputStream is = new FilteredInputStream(
                IOUtils.toInputStream("ab\ncd\nef", UTF_8),
                new RegexInputStreamFilter(Pattern.compile("cd")))) {
            assertThat(IOUtils.toString(is, UTF_8)).isEqualTo("cd\n");
        }
    }
}
