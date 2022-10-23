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

class IOUtilTest {

    private final String val = "1234567890";

    @Test
    void testStartsWith() throws IOException {
        assertThat(IOUtil.startsWith(newInputStream(val),
                "12345".getBytes())).isTrue();
        assertThat(IOUtil.startsWith(null, "12345".getBytes())).isFalse();
        assertThat(IOUtil.startsWith(newInputStream(val), null)).isFalse();
    }

    @Test
    void testBorrowBytes() throws IOException {
        // same stream can be re-read from start without impact
        try (InputStream is = newInputStream(val)) {
            assertThat(IOUtil.borrowBytes(is, 4)).isEqualTo("1234".getBytes());
            assertThat(IOUtil.borrowBytes(is, 10)).isEqualTo(val.getBytes());
        }
        assertThrows(IllegalArgumentException.class, //NOSONAR
                () -> IOUtil.borrowBytes(NullInputStream.nullInputStream(), 4));
    }

    @Test
    void testBorrowCharacters() throws IOException {
        // same stream can be re-read from start without impact
        try (Reader r = newReader(val)) {
            assertThat(IOUtil.borrowCharacters(r, 4))
                .isEqualTo("1234".toCharArray());
            assertThat(IOUtil.borrowCharacters(r, 10))
                .isEqualTo(val.toCharArray());
        }
        assertThrows(IllegalArgumentException.class, //NOSONAR
                () -> IOUtil.borrowCharacters(NullReader.nullReader() , 4));
    }

    @Test
    void testIsEmptyInputStream() throws IOException {
        assertThat(IOUtil.isEmpty(newInputStream(""))).isTrue();
        assertThat(IOUtil.isEmpty((InputStream) null)).isTrue();
        assertThrows(IllegalArgumentException.class, //NOSONAR
                () -> IOUtil.isEmpty(NullInputStream.nullInputStream()));
    }

    @Test
    void testIsEmptyReader() throws IOException {
        assertThat(IOUtil.isEmpty(newReader(""))).isTrue();
        assertThat(IOUtil.isEmpty((Reader) null)).isTrue();
        assertThrows(IllegalArgumentException.class, //NOSONAR
                () -> IOUtil.isEmpty(NullReader.nullReader()));
    }

    @Test
    void testToBufferedReader() throws IOException {
        assertThat(IOUtil.toBufferedReader(newReader(val)))
            .isInstanceOf(BufferedReader.class)
            .extracting(this::toString).isEqualTo(val);
        assertThat(IOUtil.toBufferedReader(new BufferedReader(newReader(val))))
            .isInstanceOf(BufferedReader.class)
            .extracting(this::toString).isEqualTo(val);
    }

    @Test
    void testToBufferedInputStream() throws IOException {
        assertThat(IOUtil.toBufferedInputStream(newInputStream(val)))
            .isInstanceOf(BufferedInputStream.class);
        assertThat(IOUtil.toBufferedInputStream(new BufferedInputStream(
                newInputStream(val))))
            .isInstanceOf(BufferedInputStream.class);
    }

    @Test
    void testTail() throws IOException {
        String lines = "a\nb\nc\nd\ne\nf\ng\nh\ni\nj";
        assertThat(IOUtil.tail(newInputStream(lines), 3))
            .containsExactly("h", "i", "j");
        assertThat(IOUtil.tail(null, "UTF-8", 3)).isEmpty();
        assertThat(IOUtil.tail(newInputStream(lines), (Charset) null, 3))
            .containsExactly("h", "i", "j");
    }

    @Test
    void testHead() throws IOException {
        String lines = "a\nb\nc\nd\ne\nf\ng\nh\ni\nj";
        assertThat(IOUtil.head(newInputStream(lines), 3))
            .containsExactly("a", "b", "c");
        assertThat(IOUtil.head(null, "UTF-8", 3)).isEmpty();
        assertThat(IOUtil.head(newInputStream(lines), (Charset) null, 3))
            .containsExactly("a", "b", "c");
    }

    @Test
    void testToNonNullReader() throws IOException {
        assertThat(IOUtil.toNonNullReader(null)).isNotNull();
        Reader r = NullReader.nullReader();
        assertThat(IOUtil.toNonNullReader(r)).isSameAs(r);
    }

    @Test
    void testToNonNullInputStream() throws IOException {
        assertThat(IOUtil.toNonNullInputStream(null)).isNotNull();
        InputStream is = NullInputStream.nullInputStream();
        assertThat(IOUtil.toNonNullInputStream(is)).isSameAs(is);
    }

    @Test
    void testConsumeInputStream() throws IOException {
        assertThat(IOUtil.consume(newInputStream("12345"))).isEqualTo(5);
        assertThat(IOUtil.consume((InputStream) null)).isZero();
    }

    @Test
    void testConsumeReader() throws IOException {
        assertThat(IOUtil.consume(newReader("12345"))).isEqualTo(5);
        assertThat(IOUtil.consume((Reader) null)).isZero();
    }

    @Test
    void testConsumeUntilReaderIntPredicate() throws IOException {
        assertThat(IOUtil.consumeUntil(
                newReader("abcde"), ch -> ch == 'd')).isEqualTo(3);
    }

    @Test
    void testConsumeUntilReaderIntPredicateAppendable() throws IOException {
        StringBuilder b = new StringBuilder();
        assertThat(IOUtil.consumeUntil(
                newReader("abcde"), ch -> ch == 'd', b)).isEqualTo(3);
        assertThat(b).hasToString("abc");
    }

    @Test
    void testConsumeUntilReaderStringAppendable() throws IOException {
        StringBuilder b = new StringBuilder();
        assertThat(IOUtil.consumeUntil(
                newReader("abcde"), "cd", b)).isEqualTo(4);
        assertThat(b).hasToString("abcd");
    }

    @Test
    void testConsumeWhileReaderIntPredicate() throws IOException {
        assertThat(IOUtil.consumeWhile(
                newReader("abcde"), ch -> ch != 'd')).isEqualTo(3);
    }

    @Test
    void testConsumeWhileReaderIntPredicateAppendable() throws IOException {
        StringBuilder b = new StringBuilder();
        assertThat(IOUtil.consumeWhile(
                newReader("abcde"), ch -> ch != 'd', b)).isEqualTo(3);
        assertThat(b).hasToString("abc");

        assertThat(IOUtil.consumeWhile(null, ch -> ch != 'd', b)).isZero();
        assertThrows(IllegalArgumentException.class, //NOSONAR
                () -> IOUtil.consumeWhile(
                        NullReader.nullReader() , ch -> ch != 'd', b));
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
        assertDoesNotThrow(() -> IOUtil.closeQuietly(bais));
        assertDoesNotThrow(() -> IOUtil.closeQuietly((Closeable[]) null));
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
