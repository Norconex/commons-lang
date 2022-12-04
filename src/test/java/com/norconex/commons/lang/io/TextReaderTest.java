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

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 */
class TextReaderTest {

    @Test
    void testSentenceBreaks() throws IOException {

        TextReader reader = getTextReader("funkyParagraphBreaks.txt", 60);
        int count = 0;
        while ((reader.readText()) != null) {
            count++;
        }
        reader.close();
        Assertions.assertEquals(10, count, "Wrong number of sentences");
    }

    @Test
    void testParagraphBreaks() throws IOException {

        TextReader reader = getTextReader("funkyParagraphBreaks.txt", 100);
        int count = 0;
        while ((reader.readText()) != null) {
            count++;
        }
        reader.close();
        Assertions.assertEquals(
                5, count, "Wrong number of text chunks returned");
    }

    @Test
    void testNoBreak() throws IOException {
        TextReader reader = getTextReader("funkyParagraphBreaks.txt", 1000);
        String allContent = reader.readText();
        reader.close();
        Assertions.assertEquals(400, allContent.length(),
                "Wrong number of characters returned.");
    }

    @Test
    void testUnlimited() throws IOException {
        TextReader reader = getTextReader("funkyParagraphBreaks.txt", -1);
        String allContent = reader.readText();
        reader.close();
        Assertions.assertEquals(400, allContent.length(),
                "Wrong number of characters returned.");
    }

    private TextReader getTextReader(String file, int readSize) {
        return new TextReader(new InputStreamReader(
                getClass().getResourceAsStream(file), StandardCharsets.UTF_8),
                readSize);
    }
}
