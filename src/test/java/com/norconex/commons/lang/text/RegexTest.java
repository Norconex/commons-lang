/* Copyright 2019-2022 Norconex Inc.
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;

import java.text.Collator;
import java.util.Locale;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.norconex.commons.lang.bean.BeanMapper;

/**
 */
class RegexTest {

    @Test
    void testMisc() {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> new Regex().compile(null)); //NOSONAR
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> Regex.compileDotAll(null, false)); //NOSONAR
        assertThat(Regex.escape(null)).isNull();
        assertThat(new Regex("blah").createKeyValueExtractor().getRegex())
                .isEqualTo(new Regex("blah"));
        assertThat(new Regex("blah").createKeyValueExtractor("key").getRegex())
                .isEqualTo(new Regex("blah"));
        assertThat(new Regex("blah").createKeyValueExtractor("key", 1)
                .getRegex()).isEqualTo(new Regex("blah"));
        assertThat(new Regex("blah").createKeyValueExtractor(1, 2).getRegex())
                .isEqualTo(new Regex("blah"));

        assertThat(new Regex("blah")
                .trim()
                .matchEmpty()
                .matcher(null)
                .pattern()).hasToString(".*");
        assertThat(new Regex("blah")
                .trim()
                .setMatchEmpty(false)
                .matcher(null)
                .pattern()).hasToString("(?=x)(?!x)");
    }

    @Test
    void testMarkInsensitive() {
        var pattern = "(èg)";
        String[] tests = {
                "\u00e9gal", // égal
                "e\u0301gal", // e + "Combining acute accent"
                "egal",
                "ègal",
                "êgal",
                "ëgal"
        };

        var regex = new Regex(pattern).ignoreDiacritic();
        for (String test : tests) {
            Assertions.assertTrue(regex.matcher(test).find(),
                    pattern + " pattern did not match " + test);
            var replaced = regex.matcher(test).replaceFirst("[$1]rég");
            var expected = "[" + test.replaceFirst("al", "") + "]régal";
            var collator = Collator.getInstance(Locale.FRENCH);
            assertThat(collator.compare(expected, replaced)).isZero();
        }
    }

    @Test
    void testMarkInsensitiveWithEscape() {
        var t = "one.*(two).?three(?Twô)*öne";
        var p = "one.*(two).?three(?twò)*oNé";
        p = Regex.escape(p);

        var regex = new Regex(p).ignoreDiacritic().ignoreCase();
        Assertions.assertTrue(regex.matcher(t).matches());
        ////\\p{InCombiningDiacriticalMarks}+
    }

    @Test
    void testFluentAndFlags() {
        assertThat(
                new Regex(".*")
                        .canonEq()
                        .comments()
                        .dotAll()
                        .literal()
                        .matchEmpty()
                        .multiline()
                        .trim()
                        .unicodeCase()
                        .unicodeCharacterClass()
                        .unixLines()).isEqualTo(
                                new Regex(".*")
                                        .setCanonEq(true)
                                        .setComments(true)
                                        .setDotAll(true)
                                        .setLiteral(true)
                                        .setMatchEmpty(true)
                                        .setMultiline(true)
                                        .setTrim(true)
                                        .setUnicodeCase(true)
                                        .setUnicodeCharacterClass(true)
                                        .setUnixLines(true));

        assertThat(
                new Regex(".*", Pattern.DOTALL, Pattern.CASE_INSENSITIVE)
                        .setFlags(Pattern.MULTILINE, Pattern.CANON_EQ))
                                .isEqualTo(
                                        new Regex()
                                                .setPattern(".*")
                                                .multiline()
                                                .canonEq());

        assertThat(new Regex("example.com")
                .setFlags(Pattern.DOTALL, Regex.UNICODE_CASE_INSENSTIVE_FLAG)
                .compile().pattern())
                        .isEqualTo(Regex.compileDotAll("example.com", true)
                                .pattern());
    }

    @Test
    void testWriteRead() {
        assertThatNoException().isThrownBy(
                () -> BeanMapper.DEFAULT.assertWriteRead(new Regex("mypattern")
                        .setCanonEq(true)
                        .setMultiline(true)));
    }
}
