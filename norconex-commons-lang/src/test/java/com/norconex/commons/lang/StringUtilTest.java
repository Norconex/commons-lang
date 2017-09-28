/* Copyright 2017 Norconex Inc.
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
package com.norconex.commons.lang;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * @author Pascal Essiembre
 * @since 1.14.0
 */
public class StringUtilTest {

    @Test
    public void testTruncateWithHash() {
        String text = "I am a string with 33 characters.";

        // Test no truncate needed
        assertEquals(text, StringUtil.truncateWithHash(text, 50));
        // Test no truncate needed equal size
        assertEquals(text, StringUtil.truncateWithHash(text, 33));

        // Test truncate needed with separator
        assertEquals("I am a!" + " string with 33 characters.".hashCode(), 
                StringUtil.truncateWithHash(text, 30, '!'));
        // Test truncate needed no separator
        assertEquals("I am a" + " string with 33 characters.".hashCode(), 
                StringUtil.truncateWithHash(text, 30));
    }
}
