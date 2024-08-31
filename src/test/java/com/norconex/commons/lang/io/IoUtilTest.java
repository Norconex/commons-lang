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
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.NullInputStream;
import org.apache.commons.io.input.NullReader;
import org.junit.jupiter.api.Test;

class IoUtilTest {

    private final String val = "1234567890";

    @Test
    void testStartsWith() throws IOException {
        assertThat(IoUtil.startsWith(newInputStream(val),
                "12345".getBytes())).isTrue();
        assertThat(IoUtil.startsWith(null, "12345".getBytes())).isFalse();
        assertThat(IoUtil.startsWith(newInputStream(val), null)).isFalse();
    }

    @Test
    void testBorrowBytes() throws IOException {
        // same stream can be re-read from start without impact
        try (InputStream is = newInputStream(val)) {
            assertThat(IoUtil.borrowBytes(is, 4)).isEqualTo("1234".getBytes());
            assertThat(IoUtil.borrowBytes(is, 10)).isEqualTo(val.getBytes());
        }
        assertThrows(IllegalArgumentException.class, //NOSONAR
                () -> IoUtil.borrowBytes(NullInputStream.nullInputStream(), 4));
    }

    @Test
    void testBorrowCharacters() throws IOException {
        // same stream can be re-read from start without impact
        try (Reader r = newReader(val)) {
            assertThat(IoUtil.borrowCharacters(r, 4))
                    .isEqualTo("1234".toCharArray());
            assertThat(IoUtil.borrowCharacters(r, 10))
                    .isEqualTo(val.toCharArray());
        }
        assertThrows(IllegalArgumentException.class, //NOSONAR
                () -> IoUtil.borrowCharacters(NullReader.nullReader(), 4));
    }

    @Test
    void testIsEmptyInputStream() throws IOException {
        assertThat(IoUtil.isEmpty(newInputStream(""))).isTrue();
        assertThat(IoUtil.isEmpty((InputStream) null)).isTrue();
        assertThrows(IllegalArgumentException.class, //NOSONAR
                () -> IoUtil.isEmpty(NullInputStream.nullInputStream()));
    }

    @Test
    void testIsEmptyReader() throws IOException {
        assertThat(IoUtil.isEmpty(newReader(""))).isTrue();
        assertThat(IoUtil.isEmpty((Reader) null)).isTrue();
        assertThrows(IllegalArgumentException.class, //NOSONAR
                () -> IoUtil.isEmpty(NullReader.nullReader()));
    }

    @Test
    void testToBufferedReader() throws IOException {
        assertThat(IoUtil.toBufferedReader(newReader(val)))
                .isInstanceOf(BufferedReader.class)
                .extracting(this::toString).isEqualTo(val);
        assertThat(IoUtil.toBufferedReader(new BufferedReader(newReader(val))))
                .isInstanceOf(BufferedReader.class)
                .extracting(this::toString).isEqualTo(val);
    }

    @Test
    void testToBufferedInputStream() throws IOException {
        assertThat(IoUtil.toBufferedInputStream(newInputStream(val)))
                .isInstanceOf(BufferedInputStream.class);
        assertThat(IoUtil.toBufferedInputStream(new BufferedInputStream(
                newInputStream(val))))
                        .isInstanceOf(BufferedInputStream.class);
    }

    @Test
    void testTail() throws IOException {
        String lines = "a\nb\nc\nd\ne\nf\ng\nh\ni\nj";
        assertThat(IoUtil.tail(newInputStream(lines), 3))
                .containsExactly("h", "i", "j");
        assertThat(IoUtil.tail(null, "UTF-8", 3)).isEmpty();
        assertThat(IoUtil.tail(newInputStream(lines), (Charset) null, 3))
                .containsExactly("h", "i", "j");
    }

    @Test
    void testHead() throws IOException {
        String lines = "a\nb\nc\nd\ne\nf\ng\nh\ni\nj";
        assertThat(IoUtil.head(newInputStream(lines), 3))
                .containsExactly("a", "b", "c");
        assertThat(IoUtil.head(null, "UTF-8", 3)).isEmpty();
        assertThat(IoUtil.head(newInputStream(lines), (Charset) null, 3))
                .containsExactly("a", "b", "c");
    }

    @Test
    void testToNonNullReader() throws IOException {
        assertThat(IoUtil.toNonNullReader(null)).isNotNull();
        Reader r = NullReader.nullReader();
        assertThat(IoUtil.toNonNullReader(r)).isSameAs(r);
    }

    @Test
    void testToNonNullInputStream() throws IOException {
        assertThat(IoUtil.toNonNullInputStream(null)).isNotNull();
        InputStream is = NullInputStream.nullInputStream();
        assertThat(IoUtil.toNonNullInputStream(is)).isSameAs(is);
    }

    @Test
    void testConsumeInputStream() throws IOException {
        assertThat(IoUtil.consume(newInputStream("12345"))).isEqualTo(5);
        assertThat(IoUtil.consume((InputStream) null)).isZero();
    }

    @Test
    void testConsumeReader() throws IOException {
        assertThat(IoUtil.consume(newReader("12345"))).isEqualTo(5);
        assertThat(IoUtil.consume((Reader) null)).isZero();
    }

    @Test
    void testConsumeUntilReaderIntPredicate() throws IOException {
        assertThat(IoUtil.consumeUntil(
                newReader("abcde"), ch -> ch == 'd')).isEqualTo(3);
    }

    @Test
    void testConsumeUntilReaderIntPredicateAppendable() throws IOException {
        StringBuilder b = new StringBuilder();
        assertThat(IoUtil.consumeUntil(
                newReader("abcde"), ch -> ch == 'd', b)).isEqualTo(3);
        assertThat(b).hasToString("abc");
    }

    @Test
    void testConsumeUntilReaderStringAppendable() throws IOException {
        StringBuilder b = new StringBuilder();
        assertThat(IoUtil.consumeUntil(
                newReader("abcde"), "cd", b)).isEqualTo(4);
        assertThat(b).hasToString("abcd");
    }

    @Test
    void testConsumeWhileReaderIntPredicate() throws IOException {
        assertThat(IoUtil.consumeWhile(
                newReader("abcde"), ch -> ch != 'd')).isEqualTo(3);
    }

    @Test
    void testConsumeWhileReaderIntPredicateAppendable() throws IOException {
        StringBuilder b = new StringBuilder();
        assertThat(IoUtil.consumeWhile(
                newReader("abcde"), ch -> ch != 'd', b)).isEqualTo(3);
        assertThat(b).hasToString("abc");

        assertThat(IoUtil.consumeWhile(null, ch -> ch != 'd', b)).isZero();
        assertThrows(IllegalArgumentException.class, //NOSONAR
                () -> IoUtil.consumeWhile(
                        NullReader.nullReader(), ch -> ch != 'd', b));
    }

    @Test
    void testCloseQuietly() throws IOException {
        ByteArrayInputStream bais = new ByteArrayInputStream(new byte[] {}) {
            @Override
            public void close() throws IOException {
                throw new IOException("Should be silent");
            }
        };
        assertThrows(IOException.class, () -> bais.close());
        assertDoesNotThrow(() -> IoUtil.closeQuietly(bais));
        assertDoesNotThrow(() -> IoUtil.closeQuietly((Closeable[]) null));
    }

    private InputStream newInputStream(String content) {
        return new ByteArrayInputStream(content.getBytes());
    }

    private Reader newReader(String content) {
        return new StringReader(content);
    }

    private String toString(Reader reader) {
        try {
            return IOUtils.toString(reader);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
