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
import static org.assertj.core.api.Assertions.assertThatNoException;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;

import com.norconex.commons.lang.bean.BeanMapper;
import com.norconex.commons.lang.text.TextMatcher.Method;

/**
 * @since 2.0.0
 */
class TextMatcherTest {

    @Test
    void testMisc() {
        var tm = TextMatcher
                .basic("blah")
                .trim()
                .replaceAll()
                .ignoreCase();
        var tmCopy = new TextMatcher();
        tm.copyTo(tmCopy);
        assertThat(tmCopy).isEqualTo(tm);

        tmCopy = new TextMatcher();
        tmCopy.copyFrom(tm);
        assertThat(tmCopy).isEqualTo(tm);

        assertThatNoException().isThrownBy(() -> tm.copyFrom(null));
        assertThatNoException().isThrownBy(() -> tm.copyTo(null));

        assertThat(TextMatcher.basic(null).test("something")).isTrue();
    }

    @Test
    void testReplace() {
        var text = "un gros-gros éléphant";

        var tm = new TextMatcher("gros");

        assertThat(tm
                .replace(text, "super"))
                        .isEqualTo(text);

        assertThat(tm
                .withPartial(true)
                .replace(text, "super"))
                        .isEqualTo("un super-gros éléphant");
        assertThat(tm
                .withPartial(true)
                .withReplaceAll(true)
                .apply(text, "super"))
                        .isEqualTo("un super-super éléphant");

        assertThat(TextMatcher.csv("GROS")
                .withPartial(true)
                .withIgnoreCase(true)
                .replace(text, "super"))
                        .isEqualTo("un super-gros éléphant");

        assertThat(TextMatcher.wildcard("un * elephant")
                .withIgnoreDiacritic(true)
                .replace(text, "un petit ours"))
                        .isEqualTo("un petit ours");

        tm.partial();
        tm.setMethod(Method.REGEX);
        tm.setReplaceAll(true);

        assertThat(tm
                .withPattern("(gros.)+")
                .replace(text, "super "))
                        .isEqualTo("un super éléphant");
        assertThat(tm
                .withPattern("ele")
                .replace(text, "oli"))
                        .isEqualTo("un gros-gros éléphant");
        assertThat(tm
                .withPattern("ele")
                .ignoreDiacritic()
                .replace(text, "oli"))
                        .isEqualTo("un gros-gros oliphant");

        assertThat(tm
                .withPattern(null)
                .replace("aaa", "bbb"))
                        .isEqualTo("aaa");
    }

    @Test
    void testTrimAndEmpty() {
        assertThat(TextMatcher.basic("blah").matches(" blah ")).isFalse();
        assertThat(TextMatcher.csv("blah").trim().matches(" blah ")).isTrue();
        assertThat(new TextMatcher()
                .withMethod(Method.WILDCARD)
                .withPattern("aaa")
                .matches(" "))
                        .isFalse();
        assertThat(TextMatcher.regex("bbb").trim().matches(" ")).isFalse();
        assertThat(new TextMatcher("ccc", Method.BASIC)
                .withTrim(true)
                .withMatchEmpty(true)
                .matches(" "))
                        .isTrue();
        assertThat(TextMatcher.basic("ddd")
                .matchEmpty()
                .matches(null))
                        .isTrue();
    }

    @Test
    void testRegexPattern() {
        assertThat(
                TextMatcher
                        .wildcard("ab*ef?h")
                        .toRegexPattern()
                        .pattern()).isEqualTo(
                                Pattern.compile("ab.*ef.h").pattern());
    }

    @Test
    void testWriteRead() {
        var tm = new TextMatcher()
                .setPattern("mypattern")
                .partial()
                .ignoreDiacritic()
                .ignoreCase()
                .replaceAll()
                .setMethod(Method.WILDCARD);
        assertThatNoException().isThrownBy(
                () -> BeanMapper.DEFAULT.assertWriteRead(tm));
    }

    @ParameterizedTest()
    @CsvFileSource(numLinesToSkip = 1, resources = "TextMatcherTest.csv")
    void testMatch(
            String text,
            String pattern,
            boolean matchWhole,
            boolean basicAssert,
            boolean wildAssert,
            boolean regexAssert,
            boolean csvAssert) {
        var sr = new TextMatcher()
                .setPattern(pattern)
                .setPartial(!matchWhole);
        testMatch(basicAssert, Method.BASIC, sr, text);
        testMatch(wildAssert, Method.WILDCARD, sr, text);
        testMatch(regexAssert, Method.REGEX, sr, text);
        testMatch(csvAssert, Method.CSV, sr, text);
    }

    private void testMatch(
            boolean expected, Method method, TextMatcher sr, String text) {
        var s = new TextMatcher(sr);
        s.setMethod(method);

        Assertions.assertAll(method.toString(),
                () -> {
                    s.setIgnoreDiacritic(true).setIgnoreCase(true);
                    try {
                        Assertions.assertEquals(expected,
                                s.matches(text), () -> s.toString());
                    } catch (PatternSyntaxException e) {
                        Assertions.assertFalse(expected);
                    }
                });
    }
}
