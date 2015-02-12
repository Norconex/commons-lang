/* Copyright 2010-2015 Norconex Inc.
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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.CharEncoding;
import org.apache.commons.lang3.StringUtils;

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
        return tail(is, null, lineQty);
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
        if (is == null) {
            return EMPTY_STRINGS;
        }
        String safeEncoding = encoding;
        if (StringUtils.isBlank(safeEncoding)) {
            safeEncoding = CharEncoding.UTF_8;
        }
        BufferedReader br = new BufferedReader(
                new InputStreamReader(is, safeEncoding));
        List<String> lines = new ArrayList<String>(lineQty);
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
        return head(is, null, lineQty);
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
        if (is == null) {
            return EMPTY_STRINGS;
        }
        String safeEncoding = encoding;
        if (StringUtils.isBlank(safeEncoding)) {
            safeEncoding = CharEncoding.UTF_8;
        }
        BufferedReader br = new BufferedReader(
                new InputStreamReader(is, safeEncoding));
        List<String> lines = new ArrayList<String>(lineQty);
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
}
