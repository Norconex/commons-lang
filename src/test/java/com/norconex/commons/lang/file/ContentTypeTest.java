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

import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ContentTypeTest {

    @Test
    void testGetDisplayName() {
        Assertions.assertEquals("Adobe Portable Document Format",
                ContentType.PDF.getDisplayName(Locale.FRENCH));
        Assertions.assertEquals("Adobe Portable Document Format",
                ContentType.PDF.getDisplayName());
        Assertions.assertEquals("Open eBook Publication Structure",
                ContentType.valueOf(
                        "application/oebps-package+xml").getDisplayName());
        assertThat(ContentType.valueOf("nope").getDisplayName(Locale.ENGLISH))
                .isEqualTo("[nope]");
    }

    @Test
    void testGetExtension() {
        Assertions.assertEquals("pdf", ContentType.PDF.getExtension());
        Assertions.assertEquals("wpd",
                ContentType.valueOf("application/wordperfect").getExtension());
        Assertions.assertArrayEquals(new String[] { "wpd", "wp", "wp5", "wp6" },
                ContentType.valueOf("application/wordperfect").getExtensions());

        assertThat(ContentType.valueOf("imbad").getExtension()).isEmpty();
    }

    @Test
    void testValueOf() {
        assertThat(ContentType.valueOf("  ")).isNull();
        assertThat(ContentType.valueOf(
                "application/pdf")).isEqualTo(ContentType.PDF);

        assertThat(ContentType.valuesOf("application/pdf", "text/html"))
                .containsExactly(ContentType.PDF, ContentType.HTML);
        assertThat(ContentType.valuesOf((String[]) null)).isEmpty();

        assertThat(ContentType.valuesOf(
                Arrays.asList("application/pdf", "text/html")))
                        .containsExactly(ContentType.PDF, ContentType.HTML);
        assertThat(ContentType.valuesOf(Collections.emptyList())).isEmpty();
    }

    @Test
    void testGetContentFamily() {
        assertThat(ContentType.PDF.getContentFamily()).hasToString("pdf");
    }

    @Test
    void testMatches() {
        assertThat(ContentType.PDF.matches("application/pdf")).isTrue();
        assertThat(ContentType.PDF.matches("nope")).isFalse();
    }

    @Test
    void testToString() {
        assertThat(ContentType.PDF).hasToString("application/pdf");
    }

    @Test
    void testToBaseType() {
        assertThat(ContentType.valueOf("text/html;charset=UTF-8").toBaseType())
                .isEqualTo(ContentType.HTML);
        assertThat(ContentType.valueOf("text/html").toBaseType())
                .isEqualTo(ContentType.HTML);
    }
}
