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
package com.norconex.commons.lang.exec;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.io.IOException;

import org.apache.commons.io.input.CharSequenceInputStream;
import org.junit.jupiter.api.Test;

import com.norconex.commons.lang.io.InputStreamListener;

class ExecUtilTest {

    @Test
    void testWatchProcessProcess() throws IOException {
        var process = Runtime.getRuntime().exec("java --help");
        assertThat(ExecUtil.watchProcess(process)).isZero();
    }

    @Test
    void testWatchProcessProcessInputStreamListener() throws IOException {
        var stdout = new StringBuilder();
        var process = Runtime.getRuntime().exec("java -help");
        assertThat(ExecUtil.watchProcess(process,
                (t, b, l) -> stdout.append(new String(b)))).isZero();
        assertThat(stdout)
                .containsIgnoringCase("usage")
                .containsIgnoringCase("classpath");
    }

    @Test
    void testWatchProcessProcessInputStreamListenerArray() throws IOException {
        var stdout1 = new StringBuilder();
        var stdout2 = new StringBuilder();
        var process = Runtime.getRuntime().exec("java -help");
        assertThat(ExecUtil.watchProcess(
                process,
                new InputStreamListener[] {
                        (t, b, l) -> stdout1.append(new String(b)),
                        (t, b, l) -> stdout2.append(new String(b))
                }))
                        .isZero();
        assertThat(stdout1.toString())
                .containsIgnoringCase("usage")
                .containsIgnoringCase("classpath")
                .isEqualTo(stdout2.toString());
    }

    @Test
    void testWatchProcessProcessInputStreamListenerInputStreamListener()
            throws IOException {
        var stdout = new StringBuilder();
        var stderr = new StringBuilder();
        var process = Runtime.getRuntime().exec("java --help");
        assertThat(ExecUtil.watchProcess(
                process,
                (t, b, l) -> stdout.append(new String(b)),
                (t, b, l) -> stderr.append(new String(b))))
                        .isZero();
        assertThat(stdout)
                .containsIgnoringCase("usage")
                .containsIgnoringCase("classpath");
        assertThat(stderr).isEmpty();
    }

    //    @Test
    //    void testWatchProcessProcessInputStreamListenerArrayInputStreamListenerArray() {
    //        throw new RuntimeException("not yet implemented");
    //    }
    //
    //    @Test
    //    void testWatchProcessProcessInputStreamInputStreamListenerArrayInputStreamListenerArray() {
    //        throw new RuntimeException("not yet implemented");
    //    }
    //
    @Test
    void testWatchProcessAsyncProcessInputStreamListenerInputStreamListener()
            throws IOException {
        var stdout = new StringBuilder();
        var stderr = new StringBuilder();
        var process = Runtime.getRuntime().exec("java --help");
        assertDoesNotThrow(() -> ExecUtil.watchProcessAsync(
                process,
                (t, b, l) -> stdout.append(new String(b)),
                (t, b, l) -> stderr.append(new String(b))));
    }

    //
    //    @Test
    //    void testWatchProcessAsyncProcessInputStreamListenerArrayInputStreamListenerArray() {
    //        throw new RuntimeException("not yet implemented");
    //    }
    //
    @Test
    void testWatchProcessAsyncProcessInputStream() throws IOException {
        var stdout = new StringBuilder();
        var stderr = new StringBuilder();
        var process = Runtime.getRuntime().exec("java --help");
        assertDoesNotThrow(() -> ExecUtil.watchProcessAsync(
                process,
                CharSequenceInputStream.builder()
                        .setCharSequence("java -h\n")
                        .setCharset(UTF_8)
                        .get(),
                new InputStreamListener[] {
                        (t, b, l) -> stdout.append(new String(b))
                },
                new InputStreamListener[] {
                        (t, b, l) -> stderr.append(new String(b))
                }));
    }

}
