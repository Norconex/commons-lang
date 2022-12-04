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

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;

import com.norconex.commons.lang.Sleeper;

class InputStreamConsumerTest {

    @Test
    void testInputStreamConsumer() throws IOException {
        try (InputStream is =
                new ByteArrayInputStream("abcdefghij".getBytes())) {
            InputStreamConsumer isc = new InputStreamConsumer(is);
            is.mark(3);
            assertThat(is.read()).isSameAs((int) 'a');
            is.reset();
            isc.run();
            assertThat(is.read()).isEqualTo(-1);
        }

        try (InputStream is =
                new ByteArrayInputStream("abcdefghij".getBytes())) {
            is.mark(3);
            assertThat(is.read()).isSameAs((int) 'a');
            is.reset();
            InputStreamConsumer.consume(is);
            Sleeper.sleepMillis(250);
            assertThat(is.read()).isEqualTo(-1);
        }

        try (InputStream is =
                new ByteArrayInputStream("abcdefghij".getBytes())) {
            is.mark(3);
            assertThat(is.read()).isSameAs((int) 'a');
            is.reset();
            InputStreamConsumer.consumeAndWait(is);
            assertThat(is.read()).isEqualTo(-1);
        }
    }

    @Test
    void testInputStreamConsumerWithListener() throws IOException {
        StringBuilder b = new StringBuilder();
        InputStreamListener listener = (type, bytes, len) -> {
            String str =
                    new String(ArrayUtils.subarray(bytes, 0, len));
            if (StringUtils.isNotBlank(str)) {
                b.append(type + " - " + new String(
                        ArrayUtils.subarray(bytes, 0, len)));
            }
        };
        try (InputStream is =
                new ByteArrayInputStream("abcdefghij".getBytes())) {
            InputStreamConsumer isc =
                    new InputStreamConsumer(is, "test", listener);
            isc.run();
            assertThat(b).hasToString("test - abcdefghij");
            assertThat(isc.getStreamListeners()).contains(listener);
            assertThat(isc.getType()).isEqualTo("test");

        }
    }
}
