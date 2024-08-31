/* Copyright 2015-2020 Norconex Inc.
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
import java.io.IOException;
import java.io.Reader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Reads text form an input stream, splitting it wisely whenever the text
 * is too large.  First tries to split after the last paragraph.  If there
 * are no paragraph, it tries to split after the last sentence.  If no sentence
 * can be detected, it splits on the last word.  If no words are found,
 * it returns all it could read up to the maximum read size in characters.
 * The default maximum number of characters to be read before splitting
 * is 10 millions. Passing <code>-1</code> as the <code>maxReadSize</code>
 * will disable reading in batch and will read the entire text all at once.
 * @since 1.6.0
 */
public class TextReader extends Reader {

    private static final Logger LOG = LoggerFactory.getLogger(TextReader.class);

    public static final int DEFAULT_MAX_READ_SIZE = 10000000;

    private final BufferedReader reader;
    private final int maxReadSize;
    private final boolean removeTrailingDelimiter;
    private final StringBuilder buffer = new StringBuilder();

    private static final int PATTERN_FLAGS =
            Pattern.DOTALL | Pattern.UNICODE_CHARACTER_CLASS;

    private static final Pattern PARAGRAPH_PATTERN = Pattern.compile(
            "^.*(\\p{javaWhitespace}*[\\n\\r]\\p{javaWhitespace}*?"
                    + "[\\n\\r]\\p{javaWhitespace}*)",
            PATTERN_FLAGS);

    private static final Pattern SENTENCE_PATTERN = Pattern.compile(
            "^.*[\\.\\?\\!](\\p{javaWhitespace}+|$)", PATTERN_FLAGS);
    private static final Pattern WORD_PATTERN = Pattern.compile(
            "^.*(\\p{javaWhitespace}+)", PATTERN_FLAGS);

    /**
     * Create a new text reader, reading a maximum of 10 million characters
     * at a time when {@link #readText()} is called.
     * @param reader a Reader
     */
    public TextReader(Reader reader) {
        this(reader, DEFAULT_MAX_READ_SIZE);
    }

    /**
     * Constructor.
     * @param reader a Reader
     * @param maxReadSize maximum to read at once with {@link #readText()}.
     */
    public TextReader(Reader reader, int maxReadSize) {
        this(reader, maxReadSize, false);
    }

    /**
     * Constructor.
     * @param reader a Reader
     * @param maxReadSize maximum to read at once with {@link #readText()}.
     * @param removeTrailingDelimiter whether to remove trailing delimiter
     */
    public TextReader(Reader reader, int maxReadSize,
            boolean removeTrailingDelimiter) {
        super();
        this.maxReadSize = maxReadSize;
        this.reader = IoUtil.toBufferedReader(reader);
        this.removeTrailingDelimiter = removeTrailingDelimiter;
    }

    @Override
    public int read(char[] cbuf, int off, int len) throws IOException {
        return reader.read(cbuf, off, len);
    }

    /**
     * Reads the next chunk of text, up to the maximum read size specified.
     * It tries as much as possible to break long text into paragraph,
     * sentences or words, before returning.  See class documentation.
     * @return text read
     * @throws IOException problem reading text.
     */
    public String readText() throws IOException {
        if (maxReadSize == -1) {
            String txt = IOUtils.toString(reader);
            if (StringUtils.isEmpty(txt)) {
                return null;
            }
            return txt;
        }

        char[] text = new char[maxReadSize - buffer.length()];
        int num = reader.read(text);
        if (num == -1) {
            return null;
        }

        buffer.append(String.valueOf(text, 0, num));

        // Return all if we reached the end.
        reader.mark(1);
        if (reader.read() == -1) {
            String t = buffer.toString();
            buffer.setLength(0);
            reader.reset();
            return t;
        }
        reader.reset();

        Matcher m;

        // Try breaking at paragraph:
        m = PARAGRAPH_PATTERN.matcher(buffer);
        if (m.find()) {
            int mStart = m.start(1);
            int mEnd = m.end(1);
            int substringEnd = mEnd;
            if (removeTrailingDelimiter) {
                substringEnd = mStart;
            }
            String t = buffer.substring(0, substringEnd);
            buffer.delete(0, substringEnd);
            LOG.debug("Reader text split after paragraph.");
            return t;
        }

        // Try breaking at sentence:
        m = SENTENCE_PATTERN.matcher(buffer);
        if (m.find()) {
            int mStart = m.start(1);
            int mEnd = m.end(1);
            int substringEnd = mEnd;
            if (removeTrailingDelimiter) {
                substringEnd = mStart;
            }
            String t = buffer.substring(0, substringEnd);
            buffer.delete(0, substringEnd);
            LOG.debug("Reader text split after sentence.");
            return t;
        }

        // Try breaking at word:
        m = WORD_PATTERN.matcher(buffer);
        if (m.find()) {
            int mStart = m.start(1);
            int mEnd = m.end(1);
            int substringEnd = mEnd;
            if (removeTrailingDelimiter) {
                substringEnd = mStart;
            }
            String t = buffer.substring(0, substringEnd);
            buffer.delete(0, substringEnd);
            LOG.debug("Reader text split after word.");
            return t;
        }

        String t = buffer.toString();
        buffer.setLength(0);
        LOG.debug("Reader text split after maxReadSize.");
        return t;
    }

    @Override
    public void close() throws IOException {
        reader.close();
    }
}