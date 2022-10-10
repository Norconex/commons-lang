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

import com.norconex.commons.lang.io.IInputStreamListener;

class ExecUtilTest {

    @Test
    void testWatchProcessProcess() throws IOException {
        Process process = Runtime.getRuntime().exec("java --help");
        assertThat(ExecUtil.watchProcess(process)).isZero();
    }

    @Test
    void testWatchProcessProcessIInputStreamListener() throws IOException {
        StringBuilder stdout = new StringBuilder();
        Process process = Runtime.getRuntime().exec("java -help");
        assertThat(ExecUtil.watchProcess(process,
                (t, b, l) -> stdout.append(new String(b)))).isZero();
        assertThat(stdout)
            .containsIgnoringCase("usage")
            .containsIgnoringCase("classpath");
    }

    @Test
    void testWatchProcessProcessIInputStreamListenerArray() throws IOException {
        StringBuilder stdout1 = new StringBuilder();
        StringBuilder stdout2 = new StringBuilder();
        Process process = Runtime.getRuntime().exec("java -help");
        assertThat(ExecUtil.watchProcess(
                process,
                new IInputStreamListener[] {
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
    void testWatchProcessProcessIInputStreamListenerIInputStreamListener()
            throws IOException {
        StringBuilder stdout = new StringBuilder();
        StringBuilder stderr = new StringBuilder();
        Process process = Runtime.getRuntime().exec("java --help");
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
//    void testWatchProcessProcessIInputStreamListenerArrayIInputStreamListenerArray() {
//        throw new RuntimeException("not yet implemented");
//    }
//
//    @Test
//    void testWatchProcessProcessInputStreamIInputStreamListenerArrayIInputStreamListenerArray() {
//        throw new RuntimeException("not yet implemented");
//    }
//
    @Test
    void testWatchProcessAsyncProcessIInputStreamListenerIInputStreamListener()
            throws IOException {
        StringBuilder stdout = new StringBuilder();
        StringBuilder stderr = new StringBuilder();
        Process process = Runtime.getRuntime().exec("java --help");
        assertDoesNotThrow(() -> ExecUtil.watchProcessAsync(
                process,
                (t, b, l) -> stdout.append(new String(b)),
                (t, b, l) -> stderr.append(new String(b))));
    }
//
//    @Test
//    void testWatchProcessAsyncProcessIInputStreamListenerArrayIInputStreamListenerArray() {
//        throw new RuntimeException("not yet implemented");
//    }
//
    @Test
    void testWatchProcessAsyncProcessInputStream() throws IOException {
        StringBuilder stdout = new StringBuilder();
        StringBuilder stderr = new StringBuilder();
        Process process = Runtime.getRuntime().exec("java --help");
        assertDoesNotThrow(() -> ExecUtil.watchProcessAsync(
                process,
                new CharSequenceInputStream("java -h\n", UTF_8),
                new IInputStreamListener[] {
                        (t, b, l) -> stdout.append(new String(b))
                },
                new IInputStreamListener[] {
                        (t, b, l) -> stderr.append(new String(b))
                }));
    }

}
