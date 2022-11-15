/* Copyright 2022 Norconex Inc.
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

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Paths;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;

import com.norconex.commons.lang.security.Credentials;
import com.norconex.commons.lang.url.URLStreamer.HttpHost;

class URLStreamerTest {

    private static final URL URL;
    static {
        try {
            URL = Paths.get("src/test/resources/file/webFile.txt")
                    .toAbsolutePath().toUri().toURL();
        } catch (MalformedURLException e) {
            throw new UncheckedIOException(e);
        }
    }
    private static String URLSTR = URL.toString();
    private static HttpURL HTTPURL = new HttpURL(URL);

    @Test
    void testStreamToInputStream() throws IOException {
        assertThat(toString(URLStreamer.stream(URLSTR))).isEqualTo("SUCCESS");
        assertThat(toString(URLStreamer.stream(URL))).isEqualTo("SUCCESS");
        assertThat(toString(URLStreamer.stream(HTTPURL))).isEqualTo("SUCCESS");

        Credentials creds = new Credentials("joe", "dalton");

        assertThat(toString(URLStreamer.stream(URLSTR, creds)))
            .isEqualTo("SUCCESS");
        assertThat(toString(URLStreamer.stream(URL, creds)))
            .isEqualTo("SUCCESS");
        assertThat(toString(URLStreamer.stream(
                HTTPURL, creds))).isEqualTo("SUCCESS");

        HttpHost proxy = new HttpHost("blah", 0);

        assertThat(toString(URLStreamer.stream(URLSTR, creds, proxy)))
            .isEqualTo("SUCCESS");
        assertThat(toString(URLStreamer.stream(URL, creds, proxy)))
            .isEqualTo("SUCCESS");
        assertThat(toString(URLStreamer.stream(
                HTTPURL, creds, proxy))).isEqualTo("SUCCESS");

        Credentials proxyCreds = new Credentials("jack", "dalton");

        assertThat(toString(URLStreamer.stream(
                URLSTR, creds, proxy, proxyCreds))).isEqualTo("SUCCESS");
        assertThat(toString(URLStreamer.stream(
                URL, creds, proxy, proxyCreds))).isEqualTo("SUCCESS");
        assertThat(toString(URLStreamer.stream(
                HTTPURL, creds, proxy, proxyCreds))).isEqualTo("SUCCESS");

    }

    @Test
    void testStreamToString() throws IOException {
        assertThat(URLStreamer.streamToString(URLSTR)).isEqualTo("SUCCESS");
        assertThat(URLStreamer.streamToString(URL)).isEqualTo("SUCCESS");
        assertThat(URLStreamer.streamToString(HTTPURL)).isEqualTo("SUCCESS");

        Credentials creds = new Credentials("joe", "dalton");

        assertThat(URLStreamer.streamToString(URLSTR, creds))
            .isEqualTo("SUCCESS");
        assertThat(URLStreamer.streamToString(URL, creds))
            .isEqualTo("SUCCESS");
        assertThat(URLStreamer.streamToString(
                HTTPURL, creds)).isEqualTo("SUCCESS");

        HttpHost proxy = new HttpHost("blah", 0);

        assertThat(URLStreamer.streamToString(URLSTR, creds, proxy))
            .isEqualTo("SUCCESS");
        assertThat(URLStreamer.streamToString(URL, creds, proxy))
            .isEqualTo("SUCCESS");
        assertThat(URLStreamer.streamToString(
                HTTPURL, creds, proxy)).isEqualTo("SUCCESS");

        Credentials proxyCreds = new Credentials("jack", "dalton");

        assertThat(URLStreamer.streamToString(
                URLSTR, creds, proxy, proxyCreds)).isEqualTo("SUCCESS");
        assertThat(URLStreamer.streamToString(
                URL, creds, proxy, proxyCreds)).isEqualTo("SUCCESS");
        assertThat(URLStreamer.streamToString(
                HTTPURL, creds, proxy, proxyCreds)).isEqualTo("SUCCESS");
    }

    private String toString(InputStream is) throws IOException {
        return IOUtils.toString(is, UTF_8);
    }
}
