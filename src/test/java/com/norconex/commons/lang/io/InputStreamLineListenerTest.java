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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;

class InputStreamLineListenerTest {

    @Test
    void testInputStreamLineListener() throws IOException {
        StringBuilder b = new StringBuilder();
        InputStreamLineListener listener = new InputStreamLineListener() {
            @Override
            protected void lineStreamed(String type, String line) {
                if (StringUtils.isNotBlank(line)) {
                    if (StringUtils.isNotBlank(type)) {
                        b.append(type + " - ");
                    }
                    b.append(line);
                }
            }
        };

        try (InputStream is =
                new ByteArrayInputStream("abcdefghij".getBytes())) {
            InputStreamConsumer.consumeAndWait(is, "test", listener);
            assertThat(b).hasToString("test - abcdefghij");
        }

        b.setLength(0);
        try (InputStream is =
                new ByteArrayInputStream("abcdefghij".getBytes())) {
            InputStreamConsumer.consumeAndWait(is, null, listener);
            assertThat(b).hasToString("abcdefghij");
        }

        b.setLength(0);
        try (InputStream is =
                new ByteArrayInputStream("abcde\nfghij".getBytes())) {
            InputStreamConsumer.consumeAndWait(is, "X", listener);
            assertThat(b).hasToString("X - abcdeX - fghij");
        }
    }

    @Test
    void testMisc() throws IOException {
        assertThat(new InputStreamLineListener(UTF_8) {
            @Override
            protected void lineStreamed(String type, String line) {/*NOOP*/}
        }.getCharset()).isEqualTo(UTF_8);
        assertThat(new InputStreamLineListener((Charset) null) {
            @Override
            protected void lineStreamed(String type, String line) {/*NOOP*/}
        }.getCharset()).isEqualTo(UTF_8);

        assertThat(new InputStreamLineListener("UTF-8") {
            @Override
            protected void lineStreamed(String type, String line) {/*NOOP*/}
        }.getCharset()).isEqualTo(UTF_8);
        assertThat(new InputStreamLineListener((String) null) {
            @Override
            protected void lineStreamed(String type, String line) {/*NOOP*/}
        }.getCharset()).isEqualTo(UTF_8);
    }

}
