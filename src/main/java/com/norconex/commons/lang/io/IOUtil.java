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

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.IntPredicate;

import org.apache.commons.io.input.NullInputStream;
import org.apache.commons.io.input.NullReader;
import org.apache.commons.io.output.NullOutputStream;
import org.apache.commons.io.output.NullWriter;
import org.apache.commons.lang3.ArrayUtils;

import lombok.NonNull;

/**
 * I/O related utility methods.
 */
public final class IOUtil {

    /** Empty strings. */
    private static final String[] EMPTY_STRINGS = {};
    private static final String ERR_READER_MUST_SUPPORT_MARK =
            "Reader must support mark.";
    private static final String ERR_STREAM_MUST_SUPPORT_MARK =
            "Input stream must support mark.";

    /**
     * Constructor.
     */
    private IOUtil() {
    }

    /**
     * Whether the given input stream starts the specified bytes array
     * or not.  The input stream must support marking. If the byte array
     * or the input stream is null, then <code>false</code> is returned.
     * @param is input stream
     * @param bytes byte array to compare
     * @return <code>true</code> if input stream starts with byte array
     * @throws IOException if {@link InputStream#markSupported()} returns false
     */
    public static boolean startsWith(
            InputStream is, byte[] bytes) throws IOException {
        if (is == null || bytes == null) {
            return false;
        }
        var head = IOUtil.borrowBytes(is, bytes.length);
        return Arrays.equals(bytes, head);
    }

    /**
     * Gets and resets the specified number of bytes from the input stream.
     * Must support marks.
     * @param is input stream
     * @param qty number of bytes to read
     * @return byte array of length matching the quantity requested
     * @throws IOException if {@link InputStream#markSupported()} returns false
     * @since 2.0.0
     */
    public static byte[] borrowBytes(
            @NonNull InputStream is, int qty) throws IOException {
        if (!is.markSupported()) {
            throw new IllegalArgumentException(
                    ERR_STREAM_MUST_SUPPORT_MARK);
        }
        is.mark(qty);
        var bytes = new byte[qty];
        is.read(bytes); //NOSONAR
        is.reset();
        return bytes;
    }

    /**
     * Gets and resets the specified number of characters from the reader.
     * Must support marks.
     * @param reader reader
     * @param qty number of characters to read
     * @return char array of length matching the quantity requested
     * @throws IOException if {@link Reader#markSupported()} returns false
     * @since 2.0.0
     */
    public static char[] borrowCharacters(
            @NonNull Reader reader, int qty) throws IOException {
        if (!reader.markSupported()) {
            throw new IllegalArgumentException(ERR_READER_MUST_SUPPORT_MARK);
        }
        reader.mark(qty);
        var chars = new char[qty];
        reader.read(chars);
        reader.reset();
        return chars;
    }

    /**
     * Gets whether the given input stream is <code>null</code> or empty.
     * Must support marks.
     * @param is input stream
     * @return <code>true</code> if <code>null</code> or empty
     * @throws IOException if {@link InputStream#markSupported()} returns false
     * @since 2.0.0
     */
    public static boolean isEmpty(InputStream is) throws IOException {
        if (is == null) {
            return true;
        }
        if (!is.markSupported()) {
            throw new IllegalArgumentException(
                    ERR_STREAM_MUST_SUPPORT_MARK);
        }
        is.mark(1);
        var numRead = is.read();
        is.reset();
        return numRead == -1;
    }

    /**
     * Gets whether the given Reader is <code>null</code> or empty.
     * Must support marks.
     * @param reader reader
     * @return <code>true</code> if <code>null</code> or empty
     * @throws IOException if {@link Reader#markSupported()} returns false
     * @since 2.0.0
     */
    public static boolean isEmpty(Reader reader) throws IOException {
        if (reader == null) {
            return true;
        }
        if (!reader.markSupported()) {
            throw new IllegalArgumentException(ERR_READER_MUST_SUPPORT_MARK);
        }
        reader.mark(1);
        var numRead = reader.read();
        reader.reset();
        return numRead == -1;
    }

    /**
     * Wraps the reader in a {@link BufferedReader} if not a subclass already.
     * @param reader the reader to wrap if needed
     * @return buffered reader
     * @since 1.6.0
     */
    public static BufferedReader toBufferedReader(@NonNull Reader reader) {
        if (BufferedReader.class.isAssignableFrom(reader.getClass())) {
            return (BufferedReader) reader;
        }
        return new BufferedReader(reader);
    }

    /**
     * Wraps the input stream in a {@link BufferedInputStream} if not a subclass
     * already.
     * @param in the input stream to wrap if needed
     * @return buffered input stream
     * @since 1.6.0
     */
    public static BufferedInputStream toBufferedInputStream(
            @NonNull InputStream in) {
        if (BufferedInputStream.class.isAssignableFrom(in.getClass())) {
            return (BufferedInputStream) in;
        }
        return new BufferedInputStream(in);
    }

    /**
     * Gets the last lines from an input stream, using UTF-8.
     * This method is null-safe.
     * If the input stream is null or empty, an empty string array will
     * be returned.
     * <br><br>
     * Use of this method can often be a bad idea (especially on large streams)
     * since it needs to read the entire stream to return the last lines.
     * If you are dealing with files, use
     * {@link com.norconex.commons.lang.file.FileUtil#tail(
     * java.io.File, int)} instead, which can read
     * a file starting from the end.
     * @param is input stream
     * @param lineQty maximum number of lines to return
     * @return lines as a string array
     * @throws IOException problem reading lines
     */
    public static String[] tail(final InputStream is, final int lineQty)
            throws IOException {
        return tail(is, StandardCharsets.UTF_8, lineQty);
    }

    /**
     * Gets the last lines from an input stream, using the specified encoding.
     * This method is null-safe.
     * If the input stream is null or empty, an empty string array will
     * be returned.
     * <br><br>
     * Use of this method can often be a bad idea (especially on large streams)
     * since it needs to read the entire stream to return the last lines.
     * If you are dealing with files, use
     * {@link com.norconex.commons.lang.file.FileUtil#tail(
     * File, String, int)} instead, which can read
     * a file starting from the end.
     * @param is input stream
     * @param encoding character encoding
     * @param lineQty maximum number of lines to return
     * @return lines as a string array
     * @throws IOException problem reading lines
     * @since 1.5.0
     */
    public static String[] tail(
            final InputStream is, String encoding, final int lineQty)
            throws IOException {
        return tail(is, Charset.forName(encoding), lineQty);
    }

    /**
     * Gets the last lines from an input stream, using the specified encoding.
     * This method is null-safe.
     * If the input stream is null or empty, an empty string array will
     * be returned.
     * <br><br>
     * Use of this method can often be a bad idea (especially on large streams)
     * since it needs to read the entire stream to return the last lines.
     * If you are dealing with files, use
     * {@link com.norconex.commons.lang.file.FileUtil#tail(
     * File, String, int)} instead, which can read
     * a file starting from the end.
     * @param is input stream
     * @param encoding character encoding
     * @param lineQty maximum number of lines to return
     * @return lines as a string array
     * @throws IOException problem reading lines
     * @since 1.14.0
     */
    public static String[] tail(
            final InputStream is, Charset encoding, final int lineQty)
            throws IOException {
        if (is == null) {
            return EMPTY_STRINGS;
        }
        var safeEncoding = encoding;
        if (safeEncoding == null) {
            safeEncoding = StandardCharsets.UTF_8;
        }
        var br = new BufferedReader(
                new InputStreamReader(is, safeEncoding));
        List<String> lines = new ArrayList<>(lineQty);
        String line;
        while ((line = br.readLine()) != null) {
            lines.add(0, line);
            if (lines.size() > lineQty) {
                lines.remove(lineQty);
            }
        }
        br.close();
        Collections.reverse(lines);
        return lines.toArray(EMPTY_STRINGS);
    }

    /**
     * Gets the first lines from an input stream, using UTF-8.
     * This method is null-safe.
     * If the input stream is null or empty, an empty string array will
     * be returned.
     * @param is input stream
     * @param lineQty maximum number of lines to return
     * @return lines as a string array
     * @throws IOException problem reading lines
     */
    public static String[] head(
            final InputStream is, final int lineQty)
            throws IOException {
        return head(is, StandardCharsets.UTF_8, lineQty);
    }

    /**
     * Gets the first lines from an input stream, using the specified encoding.
     * This method is null-safe.
     * If the input stream is null or empty, an empty string array will
     * be returned.
     * @param is input stream
     * @param encoding character encoding
     * @param lineQty maximum number of lines to return
     * @return lines as a string array
     * @throws IOException problem reading lines
     */
    public static String[] head(
            final InputStream is, String encoding, final int lineQty)
            throws IOException {
        return head(is, Charset.forName(encoding), lineQty);
    }

    /**
     * Gets the first lines from an input stream, using the specified encoding.
     * This method is null-safe.
     * If the input stream is null or empty, an empty string array will
     * be returned.
     * @param is input stream
     * @param encoding character encoding
     * @param lineQty maximum number of lines to return
     * @return lines as a string array
     * @throws IOException problem reading lines
     * @since 1.14.0
     */
    public static String[] head(
            final InputStream is, Charset encoding, final int lineQty)
            throws IOException {
        if (is == null) {
            return EMPTY_STRINGS;
        }
        var safeEncoding = encoding;
        if (safeEncoding == null) {
            safeEncoding = StandardCharsets.UTF_8;
        }
        var br = new BufferedReader(
                new InputStreamReader(is, safeEncoding));
        List<String> lines = new ArrayList<>(lineQty);
        String line;
        while ((line = br.readLine()) != null) {
            lines.add(line);
            if (lines.size() == lineQty) {
                break;
            }
        }
        br.close();
        return lines.toArray(EMPTY_STRINGS);
    }

    /**
     * Gets an "empty" reader (zero size) when the supplied reader
     * is <code>null</code>.
     * Else, return the supplied reader.
     * @param reader original reader
     * @return the supplied reader, or an empty reader
     * @since 2.0.0
     */
    public static Reader toNonNullReader(Reader reader) {
        return reader != null ? reader : new NullReader(0);
    }

    /**
     * Gets an "empty" input stream (zero size) when the supplied input
     * stream is <code>null</code>.
     * Else, return the supplied input stream.
     * @param is original input stream
     * @return the supplied input stream, or an empty input stream
     * @since 2.0.0
     */
    public static InputStream toNonNullInputStream(InputStream is) {
        return is != null ? is : new NullInputStream(0);
    }

    /**
     * Gets a non-effective writer when the supplied writer
     * is <code>null</code> (writing to it has no effect).
     * Else, return the supplied writer.
     * @param writer original writer
     * @return the supplied writer, or an empty writer
     * @since 3.0.0
     */
    public static Writer toNonNullWriter(Writer writer) {
        return writer != null ? writer : NullWriter.INSTANCE;
    }

    /**
     * Gets a non-effective output stream when the supplied output
     * stream is <code>null</code> (writing to it has no effect).
     * Else, return the supplied output stream.
     * @param os original output stream
     * @return the supplied output stream, or an empty output stream
     * @since 3.0.0
     */
    public static OutputStream toNonNullOutputStream(OutputStream os) {
        return os != null ? os : NullOutputStream.INSTANCE;
    }

    /**
     * Fully consumes an input stream.
     * @param is input stream
     * @return number of bytes consumed
     * @throws IOException could not consume stream
     * @since 2.0.0
     */
    public static int consume(InputStream is) throws IOException {
        if (is == null) {
            return 0;
        }
        var cnt = 0;
        var read = is.read();
        while (read != -1) {
            read = is.read();
            cnt++;
        }
        return cnt;
    }

    /**
     * Fully consumes an input stream.
     * @param reader reader
     * @return number of characters consumed
     * @throws IOException could not consume reader
     * @since 2.0.0
     */
    public static int consume(Reader reader) throws IOException {
        if (reader == null) {
            return 0;
        }
        var cnt = 0;
        var read = reader.read();
        while (read != -1) {
            read = reader.read();
            cnt++;
        }
        return cnt;
    }

    /**
     * Consumes markable reader characters until the predicate returns
     * <code>true</code> for a character or the end of stream is reached.
     * The character ending the consumption is not consumed (i.e.,
     * the reader cursor is reset just before that character).
     * @param reader the reader to consume
     * @param predicate the character evaluation condition
     * @return number of characters consumed (0 if reader is <code>null</code>)
     * @throws IOException could not consume stream
     * @throws IllegalArgumentException if reader does not support mark
     * @since 2.0.0
     */
    public static int consumeUntil(Reader reader, IntPredicate predicate)
            throws IOException {
        return consumeWhile(reader, ch -> !predicate.test(ch), null);
    }

    /**
     * Consumes markable reader characters until the predicate returns
     * <code>true</code> for a character or the end of stream is reached.
     * Optionally append the consumed characters to an {@link Appendable}
     * (e.g. StringBuilder).
     * The character ending the consumption is not consumed (i.e.,
     * the reader cursor is reset just before that character).
     * @param reader the reader to consume
     * @param predicate the character evaluation condition
     * @param appendable optional, to append consumed characters
     * @return number of characters consumed (0 if reader is <code>null</code>)
     * @throws IOException could not consume stream
     * @throws IllegalArgumentException if reader does not support mark
     * @since 2.0.0
     */
    public static int consumeUntil(
            Reader reader, IntPredicate predicate, Appendable appendable)
            throws IOException {
        return consumeWhile(reader, ch -> !predicate.test(ch), appendable);
    }

    /**
     * Consumes reader characters until after encountering the supplied string
     * (the matching string is also consumed) or the end of stream is reached.
     * Optionally append the consumed characters to an {@link Appendable}
     * (e.g. StringBuilder).
     * The matching string is also consumed.
     * @param reader the reader to consume
     * @param str the string to match
     * @param appendable optional, to append consumed characters
     * @return number of characters consumed (0 if reader is <code>null</code>)
     * @throws IOException could not consume stream
     * @since 2.0.0
     */
    public static int consumeUntil(
            Reader reader, String str, Appendable appendable)
            throws IOException {

        var chars = str.toCharArray();
        var qtyRead = 0;
        var qtyMatched = 0;

        int intch;
        while ((intch = reader.read()) != -1) {
            var ch = (char) intch;
            if (chars.length > 0 && chars[qtyMatched] == ch) {
                qtyMatched++;
            } else {
                qtyMatched = 0;
            }

            appendable.append(ch);
            qtyRead++;
            if (qtyMatched == chars.length) {
                break;
            }
        }
        return qtyRead;
    }

    /**
     * Consumes markable reader characters while the predicate returns
     * <code>true</code> for a character or the end of stream is reached.
     * The character ending the consumption is not consumed (i.e.,
     * the reader cursor is reset just before that character).
     * @param reader the reader to consume
     * @param predicate the character evaluation condition
     * @return number of characters consumed (0 if reader is <code>null</code>)
     * @throws IOException could not consume stream
     * @throws IllegalArgumentException if reader does not support mark
     * @since 2.0.0
     */
    public static int consumeWhile(Reader reader, IntPredicate predicate)
            throws IOException {
        return consumeWhile(reader, predicate, null);
    }

    /**
     * Consumes markable reader characters while the predicate returns
     * <code>true</code> for a character or the end of stream is reached.
     * Optionally append the consumed characters to an {@link Appendable}
     * (e.g. StringBuilder).
     * The character ending the consumption is not consumed (i.e.,
     * the reader cursor is reset just before that character).
     * @param reader the reader to consume
     * @param predicate the character evaluation condition
     * @param appendable optional, to append consumed characters
     * @return number of characters consumed (0 if reader is <code>null</code>)
     * @throws IOException could not consume stream
     * @throws IllegalArgumentException if reader does not support mark
     * @since 2.0.0
     */
    public static int consumeWhile(
            Reader reader, IntPredicate predicate, Appendable appendable)
            throws IOException {

        if (reader == null) {
            return 0;
        }
        if (!reader.markSupported()) {
            throw new IllegalArgumentException(ERR_READER_MUST_SUPPORT_MARK);
        }

        var qtyRead = 0;
        int ch;
        reader.mark(1);
        while ((ch = reader.read()) != -1) {
            if (!predicate.test(ch)) {
                reader.reset();
                break;
            }
            if (appendable != null) {
                appendable.append((char) ch);
            }
            reader.mark(1);
            qtyRead++;
        }
        return qtyRead;
    }

    /**
     * Given Apache has deprecated its
     * <code>IOUtils#closeQuietly(java.io.Closeable)</code> method,
     * this one offers an alternative.
     * @param closeable one or more input streams to close quietly
     * @since 2.0.0
     */
    public static void closeQuietly(Closeable... closeable) {
        if (ArrayUtils.isEmpty(closeable)) {
            return;
        }
        for (Closeable cl : closeable) {
            if (cl != null) {
                try {
                    cl.close();
                } catch (IOException e) {
                    /*NOOP*/
                }
            }
        }
    }
}
