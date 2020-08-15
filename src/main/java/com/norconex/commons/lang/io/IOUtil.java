/* Copyright 2010-2020 Norconex Inc.
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
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.commons.io.input.NullInputStream;
import org.apache.commons.io.input.NullReader;
import org.apache.commons.lang3.ArrayUtils;

/**
 * I/O related utility methods.
 * @author Pascal Essiembre
 */
public final class IOUtil {

    /** Empty strings. */
    private static final String[] EMPTY_STRINGS = new String[] {};

    /**
     * Constructor.
     */
    private IOUtil() {
        super();
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
        byte[] head = IOUtil.borrowBytes(is, bytes.length);
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
            InputStream is, int qty) throws IOException {
        if (is == null) {
            throw new IllegalArgumentException("Input stream cannot be null.");
        } else if (!is.markSupported()) {
            throw new IllegalArgumentException(
                    "Input stream must support mark.");
        }
        is.mark(qty);
        byte[] bytes = new byte[qty];
        is.read(bytes);
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
            Reader reader, int qty) throws IOException {
        if (reader == null) {
            throw new IllegalArgumentException("Reader stream cannot be null.");
        } else if (!reader.markSupported()) {
            throw new IllegalArgumentException(
                    "Reader stream must support mark.");
        }
        reader.mark(qty);
        char[] chars = new char[qty];
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
        } else if (!is.markSupported()) {
            throw new IllegalArgumentException(
                    "Input stream must support mark.");
        }
        is.mark(1);
        int numRead = is.read();
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
        } else if (!reader.markSupported()) {
            throw new IllegalArgumentException(
                    "Reader stream must support mark.");
        }
        reader.mark(1);
        int numRead = reader.read();
        reader.reset();
        return numRead == -1;
    }

    /**
     * Wraps the reader in a {@link BufferedReader} if not a subclass already.
     * @param reader the reader to wrap if needed
     * @return buffered reader
     * @since 1.6.0
     */
    public static BufferedReader toBufferedReader(Reader reader) {
        if (reader == null) {
            throw new IllegalArgumentException("Reader cannot be null");
        }
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
    public static BufferedInputStream toBufferedInputStream(InputStream in) {
        if (in == null) {
            throw new IllegalArgumentException("InputStream cannot be null");
        }
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
        Charset safeEncoding = encoding;
        if (safeEncoding == null) {
            safeEncoding = StandardCharsets.UTF_8;
        }
        BufferedReader br = new BufferedReader(
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
        Charset safeEncoding = encoding;
        if (safeEncoding == null) {
            safeEncoding = StandardCharsets.UTF_8;
        }
        BufferedReader br = new BufferedReader(
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
        int cnt = 0;
        int read = is.read();
        while(read != -1) {
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
        int cnt = 0;
        int read = reader.read();
        while(read != -1) {
            read = reader.read();
            cnt++;
        }
        return cnt;
    }

    /**
     * Given Apache has deprecated its
     * <code>IOUtils#closeQuietly(java.io.Closeable)</code> method,
     * this one offers an alternate one.
     * @param closeable one or more input streams to close quietly
     * @since 2.0.0
     */
    public static void closeQuietly(Closeable... closeable) {
        if (ArrayUtils.isEmpty(closeable)) {
            return;
        }
        for (Closeable cl : closeable) {
            if (cl != null) {
                try { cl.close(); } catch (IOException e) { /*NOOP*/ }
            }
        }
    }
}
