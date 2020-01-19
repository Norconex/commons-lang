/* Copyright 2019 Norconex Inc.
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

import java.text.Collator;
import java.util.Locale;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author Pascal Essiembre
 */
public class RegexTest {

    @Test
    public void testMarkInsensitive() {
        String pattern = "(èg)";
        String[] tests = {
                "\u00e9gal",  // égal
                "e\u0301gal", // e + "Combining acute accent"
                "egal",
                "ègal",
                "êgal",
                "ëgal"
        };

        Regex regex = new Regex(pattern).ignoreDiacritic();
        for (String test : tests) {
            Assertions.assertTrue(regex.matcher(test).find(),
                    pattern + " pattern did not match " + test);
            String replaced = regex.matcher(test).replaceFirst("[$1]rég");
            String expected = "[" + test.replaceFirst("al", "") + "]régal";
            Collator collator = Collator.getInstance(Locale.FRENCH);
            Assertions.assertTrue(collator.compare(
                    expected, replaced) == 0,
                            expected + "not matching" + replaced);
        }
    }

    @Test
    public void testMarkInsensitiveWithEscape() {
        String t = "one.*(two).?three(?Twô)*öne";
        String p = "one.*(two).?three(?twò)*oNé";
        p = Regex.escape(p);

        Regex regex = new Regex(p).ignoreDiacritic().ignoreCase();
        Assertions.assertTrue(regex.matcher(t).matches());
        ////\\p{InCombiningDiacriticalMarks}+
    }
}
