/* Copyright 2017-2020 Norconex Inc.
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
package com.norconex.commons.lang.text;

import static com.norconex.commons.lang.text.StringUtil.countMatchesEnd;
import static com.norconex.commons.lang.text.StringUtil.countMatchesStart;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

/**
 * @since 1.14.0
 */
class StringUtilTest {

    @Test
    void testTruncateWithHash() {
        String text = "I am a string with 33 characters.";

        // Test no truncate needed
        assertEquals(text, StringUtil.truncateWithHash(text, 50));
        // Test no truncate needed equal size
        assertEquals(text, StringUtil.truncateWithHash(text, 33));

        // Test truncate needed with separator
        assertEquals("I am a string with !" + hash("33 characters."),
                StringUtil.truncateWithHash(text, 30, "!"));
        // Test truncate needed with bigger separator
        assertEquals("I am a string wit---" + hash("h 33 characters."),
                StringUtil.truncateWithHash(text, 30, "---"));
        // Test truncate needed no separator
        assertEquals("I am a string with 3" + hash("3 characters."),
                StringUtil.truncateWithHash(text, 30));
    }

    @Test
    void testTruncateBytesWithHash() throws CharacterCodingException {
        Charset utf8 = StandardCharsets.UTF_8;

        //total bytes: 52
        //                               v20   v30
        //numOfBytes:  111111111111111111212121333333223
        String text = "Various 33 chars: é è ï ﮈ₡ὴḚᴙࢤՅǜ™";

//        char[] chars = text.toCharArray();
//        for (char c : chars) {
//            System.out.println(c + " : "
//                    + Character.toString(c).getBytes(utf8).length);
//        }
//        System.out.println(text.getBytes(utf8).length);

        // Test no truncate needed
        assertEquals(text, StringUtil.truncateBytesWithHash(text, utf8, 60));
        // Test no truncate needed equal size
        assertEquals(text, StringUtil.truncateBytesWithHash(text, utf8, 52));

        // Test truncate needed with separator
        assertEquals("Various 33 chars: é è ï ﮈ₡ὴ!" + hash("ḚᴙࢤՅǜ™"),
                StringUtil.truncateBytesWithHash(text, utf8, 48, "!"));
        // Test truncate needed with bigger separator (3-bytes each chars)
        assertEquals("Various 33 chars: é è ï ╠═╣" + hash("ﮈ₡ὴḚᴙࢤՅǜ™"),
                StringUtil.truncateBytesWithHash(text, utf8, 48, "╠═╣"));
        // Test truncate needed no separator
        assertEquals("Various 33 chars: é è ï ﮈ₡ὴ" + hash("ḚᴙࢤՅǜ™"),
                StringUtil.truncateBytesWithHash(text, utf8, 48));
    }

    private String hash(String s) {
        return StringUtil.getHash(s);
    }

    @Test
    void testTrimStart() {
        assertEquals("trimmed", StringUtil.trimStart("  \n  \t trimmed"));
        assertEquals("trimmed", StringUtil.trimStart("trimmed"));
        assertEquals("trimmed \n ", StringUtil.trimStart("trimmed \n "));
        assertEquals("trimmed \n ", StringUtil.trimStart("  trimmed \n "));
    }
    @Test
    void testTrimEnd() {
        assertEquals("trimmed", StringUtil.trimEnd("trimmed  \n  \t "));
        assertEquals("trimmed", StringUtil.trimEnd("trimmed"));
        assertEquals(" \n trimmed", StringUtil.trimEnd(" \n trimmed"));
        assertEquals("  trimmed", StringUtil.trimEnd("  trimmed \n "));
    }
    @Test
    void testCountMatchesStart() {
        assertEquals(2, countMatchesStart("  aa a aa ", " "));
        assertEquals(2, countMatchesStart("aaaa a aa a", "aa"));
        assertEquals(0, countMatchesStart("  aaa a aa a", "a"));
    }
    @Test
    void testCountMatchesEnd() {
        assertEquals(2, countMatchesEnd(" aa a aa  ", " "));
        assertEquals(2, countMatchesEnd("a a aa aaaa", "aa"));
        assertEquals(0, countMatchesEnd("aaa a aa a  ", "a"));
    }
}