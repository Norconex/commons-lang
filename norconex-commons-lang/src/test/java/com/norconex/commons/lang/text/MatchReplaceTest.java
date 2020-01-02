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

import java.io.IOException;
import java.util.regex.PatternSyntaxException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;

import com.norconex.commons.lang.text.MatchReplace.Method;
import com.norconex.commons.lang.xml.XML;

/**
 * @author Pascal Essiembre
 * @since 2.0.0
 */
public class MatchReplaceTest {

    @Test
    public void testWriteRead() throws IOException {
        MatchReplace sr = new MatchReplace()
                .setText("mytext")
                .setPattern("mypattern")
                .setReplacement("myreplacement")
                .setMatchWhole(true)
                .setIgnoreAccents(true)
                .setIgnoreCase(true)
                .setReplaceAll(true)
                .setMethod(Method.WILDCARD);
        XML.assertWriteRead(sr, "matchReplace");
    }

    @ParameterizedTest()
    @CsvFileSource(numLinesToSkip = 1, resources = "MatchReplaceMatchTest.csv")
    public void testMatch(
            String text,
            String pattern,
            boolean matchWhole,
            boolean basicAssert,
            boolean wildAssert,
            boolean regexAssert) throws IOException {
        MatchReplace sr = new MatchReplace()
                .setText(text)
                .setPattern(pattern)
                .setMatchWhole(matchWhole);
        testMatch(basicAssert, Method.BASIC, sr);
        testMatch(wildAssert, Method.WILDCARD, sr);
        testMatch(regexAssert, Method.REGEX, sr);
    }
    private void testMatch(boolean expected, Method method, MatchReplace sr) {
        MatchReplace s = new MatchReplace(sr);
        s.setMethod(method);

        Assertions.assertAll(method.toString(),
            () -> {
                s.setIgnoreAccents(true).setIgnoreCase(true);
                try {
                    Assertions.assertEquals(expected,
                            s.matches(), () -> s.toString());
                } catch (PatternSyntaxException e) {
                    Assertions.assertFalse(expected);
                }
            }
        );
    }

    //TODO testReplace
}
