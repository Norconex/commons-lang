/* Copyright 2016-2022 Norconex Inc.
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
package com.norconex.commons.lang.url;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

import java.net.MalformedURLException;
import java.net.URL;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class QueryStringTest {

    // This tests issue: https://github.com/Norconex/collector-http/issues/304
    @Test
    void testKeepProtocolUpperCase() {
        QueryString qs = new QueryString(
                "http://site.com/page?NoEquals&WithEquals=EqualsValue");

        Assertions.assertTrue(qs.toString().contains("NoEquals"),
                "Argument without equal sign was not found.");
    }

    @Test
    void testEmptyConstructor() {
        QueryString qs = new QueryString();
        Assertions.assertEquals("", qs.toString());
    }

    @Test
    void testURLConstructor() throws MalformedURLException {
        QueryString qs = new QueryString();
        qs.set("param1", "value1");
        qs.set("param2", "value2a", "value2b");

        assertThat(new QueryString(new URL(
                "http://example.com/blah"
                        + "?param1=value1"
                        + "&param2=value2a"
                        + "&param2=value2b"
                        + "#asdf")))
                                .isEqualTo(qs);
        assertThat(new QueryString(new URL(
                "http://example.com/blah"
                        + "?param1=value1"
                        + "&param2=value2a"
                        + "&param2=value2b"
                        + "#asdf"),
                UTF_8.toString()))
                        .isEqualTo(qs)
                        .returns("UTF-8", q -> ((QueryString) q).getEncoding());
    }

    @Test
    void testApplyOnURL() throws MalformedURLException {
        QueryString qs = new QueryString();
        qs.set("param1", "value1");
        qs.set("param2", "value2a", "value2b");

        assertThat(qs.applyOnURL("http://example.com/blah?param3=value3#hash"))
                .isEqualTo(
                        "http://example.com/blah"
                                + "?param1=value1"
                                + "&param2=value2a"
                                + "&param2=value2b");

        assertThat(qs.applyOnURL(
                new URL("http://example.com/blah?param3=value3#hash"))
                .toString())
                        .isEqualTo(
                                "http://example.com/blah"
                                        + "?param1=value1"
                                        + "&param2=value2a"
                                        + "&param2=value2b");

        assertThat(qs.applyOnURL((String) null)).isNull();
        assertThat(qs.applyOnURL((URL) null)).isNull();
    }
}
