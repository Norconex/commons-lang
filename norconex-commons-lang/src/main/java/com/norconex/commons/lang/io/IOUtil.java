/* Copyright 2010-2013 Norconex Inc.
 * 
 * This file is part of Norconex Commons Lang.
 * 
 * Norconex Commons Lang is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Norconex Commons Lang is distributed in the hope that it will be useful, 
 * but WITHOUT ANY WARRANTY; without even the implied warranty of 
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with Norconex Commons Lang. If not, see <http://www.gnu.org/licenses/>.
 */
package com.norconex.commons.lang.io;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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
     * Gets the last lines from an input stream, using UTF-8.  
     * This method is null-safe.
     * If the input stream is null or empty, an empty string array will
     * be returned.  
     * <p/>
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
     * <p/>
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
