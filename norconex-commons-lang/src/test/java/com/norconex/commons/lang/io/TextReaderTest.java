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

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;

import org.apache.commons.lang3.CharEncoding;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Pascal Essiembre
 */
public class TextReaderTest {

    
    @Test
    public void testSentenceBreaks() throws IOException {
        
        TextReader reader = getTextReader("funkyParagraphBreaks.txt", 60);
        String text = null;
        int count = 0;
        while ((text = reader.readText()) != null) {
            count++;
            System.out.println("CHUNK #" + count + " = " + text);
        }
        reader.close();
        Assert.assertEquals("Wrong number of sentences", 10, count);
    }

    
    
    @Test
    public void testParagraphBreaks() throws IOException {
        
        TextReader reader = getTextReader("funkyParagraphBreaks.txt", 100);
        String text = null;
        int count = 0;
        while ((text = reader.readText()) != null) {
            count++;
            System.out.println("CHUNK #" + count + " = " + text);
        }
        reader.close();
        Assert.assertEquals("Wrong number of text chunks returned", 5, count);
    }

    @Test
    public void testNoBreak() throws IOException {
        TextReader reader = getTextReader("funkyParagraphBreaks.txt", 1000);
        String allContent = reader.readText();
        reader.close();
        Assert.assertEquals("Wrong number of characters returned.", 
                400, allContent.length());
    }
    
    private TextReader getTextReader(String file, int readSize) 
            throws UnsupportedEncodingException {
        return new TextReader(new InputStreamReader(
                getClass().getResourceAsStream(file), CharEncoding.UTF_8),
                readSize);
    }
}
