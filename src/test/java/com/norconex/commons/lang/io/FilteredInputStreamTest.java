/* Copyright 2010-2022 Norconex Inc.
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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.Charset;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;

class FilteredInputStreamTest {

    private static final String LINES = "one\ntwo\n\rthree\r\nfour";

    @Test
    void testRead() throws IOException {
        try (FilteredInputStream is = new FilteredInputStream(
                new ByteArrayInputStream(LINES.getBytes()),
                "one"::equals)) {
            assertThat(IOUtils.toString(is, UTF_8)).isEqualTo("one\n");
        }

        try (FilteredInputStream is = new FilteredInputStream(
                new ByteArrayInputStream(LINES.getBytes()),
                "two"::equals,
                (Charset) null)) {
            assertThat(IOUtils.toString(is, UTF_8)).isEqualTo("two\n");
        }

        try (FilteredInputStream is = new FilteredInputStream(
                new ByteArrayInputStream(LINES.getBytes()),
                "three"::equals,
                (String) null)) {
            assertThat(IOUtils.toString(is, UTF_8)).isEqualTo("three\n");
        }

    }
}
