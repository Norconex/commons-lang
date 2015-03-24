/* Copyright 2015 Norconex Inc.
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

import org.apache.commons.io.FileUtils;

/**
 * Reads text form an input stream, splitting it wisely whenever the text
 * is too large.  First tries to split after the last paragraph.  If there
 * are no paragraph, it tries to split after the last sentence.  If no sentence
 * can be detected, it splits on the last word.  If no words are found,
 * it returns all it could read up to the maximum read size.
 * @author Pascal Essiembre
 * @since 1.6.0
 */
public class TextReader extends Reader {

    public static final int DEFAULT_MAX_READ_SIZE = 
            (int) (FileUtils.ONE_KB * 64);
    
    private final BufferedReader reader;
    private final int maxReadSize;
    private final boolean removeTrailingDelimiter;
    private final StringBuilder buffer = new StringBuilder();

    private static final int PATTERN_FLAGS = Pattern.MULTILINE 
            | Pattern.DOTALL | Pattern.UNICODE_CHARACTER_CLASS;
    
    private final Pattern paragraphDelimiterPattern = Pattern.compile(
            "(\\p{javaWhitespace}*[\\n\\r]\\p{javaWhitespace}*"
          + "[\\n\\r]\\p{javaWhitespace}*)", PATTERN_FLAGS);
    private final Pattern sentencePattern = Pattern.compile(
            ".*((^\\p{javaWhitespace}*|[?!\\.]\\p{javaWhitespace}+)"
          + "([^\\p{javaWhitespace}].+?)([?!\\.]\\p{javaWhitespace}+|\\n))", 
          PATTERN_FLAGS);
    private final Pattern wordDelimiterPattern = Pattern.compile(
            "(\\p{javaWhitespace}+)", PATTERN_FLAGS);;

    /**
     * Create a new text reader, reading 64KB at a time with 
     * {@link #readText()} is called.
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
        this.reader = IOUtil.toBufferedReader(reader);
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
        } else {
            reader.reset();
        }
        

        Matcher m = null;
        
        // Try breaking at paragraph:
        m = paragraphDelimiterPattern.matcher(buffer);
        if(m.find()) {
            int mStart = m.start(m.groupCount());
            int mEnd = m.end(m.groupCount());
            int substringEnd = mEnd;
            if (removeTrailingDelimiter) {
                substringEnd = mStart;
            }
            String t = buffer.substring(0, substringEnd);
            buffer.delete(0, substringEnd);
            return t;
        }

        // Try breaking at sentence:
        m = sentencePattern.matcher(buffer);
        if(m.find()) {
            int mStart = m.start(1);
            int mEnd = m.end(1);
            int substringEnd = mEnd;
            if (removeTrailingDelimiter) {
                substringEnd = mStart;
            }
            String t = buffer.substring(0, substringEnd);
            buffer.delete(0, substringEnd);
            return t;
        }

        // Try breaking at word:
        m = wordDelimiterPattern.matcher(buffer);
        if(m.find()) {
            int mStart = m.start(m.groupCount());
            int mEnd = m.end(m.groupCount());
            int substringEnd = mEnd;
            if (removeTrailingDelimiter) {
                substringEnd = mStart;
            }
            String t = buffer.substring(0, substringEnd);
            buffer.delete(0, substringEnd);
            return t;
        }
        
        
        String t = buffer.toString();
        buffer.setLength(0);
        return t;
    }
    
    @Override
    public void close() throws IOException {
        reader.close();
    }

}