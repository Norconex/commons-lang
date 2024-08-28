/* Copyright 2010-2022 Norconex Inc.
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
package com.norconex.commons.lang.file;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Locale;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ContentFamilyTest {

    @Test
    void testGetDisplayName() {
        Assertions.assertEquals("Word Processor", ContentFamily.valueOf(
                "wordprocessor").getDisplayName(Locale.ENGLISH));
        Assertions.assertEquals("Traitement de texte", ContentFamily.valueOf(
                "wordprocessor").getDisplayName(Locale.FRENCH));

        Assertions.assertEquals("Spreadsheet", ContentFamily.forContentType(
                ContentType.TSV).getDisplayName(Locale.ENGLISH));

        Assertions.assertTrue(ContentFamily.forContentType(
                ContentType.TSV).contains("text/tab-separated-values"));

        Assertions.assertEquals("other", ContentFamily.forContentType(
                "application/octet-stream").getId());

        assertThat(ContentFamily.valueOf(
                "wordprocessor").getDisplayName(null)).isNotNull();
        assertThat(ContentFamily.valueOf("idontexist")
                .getDisplayName()).isEqualTo("[idontexist]");
    }

    @Test
    void testContains() {
        assertThat(ContentFamily.valueOf("spreadsheet")
                .contains(ContentType.TSV)).isTrue();
        assertThat(ContentFamily.valueOf("spreadsheet")
                .contains(ContentType.PNG)).isFalse();
        assertThat(ContentFamily.valueOf("spreadsheet")
                .contains((ContentType) null)).isFalse();
    }

    @Test
    void testNullEqualsToString() {
        assertThat(ContentFamily.forContentType((String) null)).isNull();
        assertThat(ContentFamily.forContentType((String) null)).isNull();

        assertThat(ContentFamily.valueOf("spreadsheet")).isNotEqualTo(
                ContentFamily.valueOf("text"));
        assertThat(ContentFamily.valueOf("spreadsheet")).hasToString(
                "spreadsheet");
    }
}
