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
package com.norconex.commons.lang.security;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HttpsURLConnection;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.tls.HandshakeCertificates;
import okhttp3.tls.HeldCertificate;

class CertificateUtilTest {

    private static MockWebServer server;
    private static HandshakeCertificates clientCertificates;

    @BeforeAll
    static void setUp() throws
            IOException,
            NoSuchAlgorithmException,
            KeyManagementException {
        //Tips:
        // https://adambennett.dev/2021/09/mockwebserver--https/
        // https://github.com/square/okhttp/blob/master/okhttp-tls/README.md

        // Server:
        HeldCertificate rootCertificate = new HeldCertificate.Builder()
                .certificateAuthority(1)
                .build();

        HeldCertificate intermediateCertificate = new HeldCertificate.Builder()
                .certificateAuthority(0)
                .signedBy(rootCertificate)
                .build();

        String localhost = InetAddress.getByName("localhost")
                .getCanonicalHostName();
        HeldCertificate localhostCertificate = new HeldCertificate.Builder()
                .addSubjectAlternativeName(localhost)
                .signedBy(intermediateCertificate)
                .duration(10 * 365, TimeUnit.DAYS)
                .build();

        HandshakeCertificates serverCertificates =
                new HandshakeCertificates.Builder()
                    .heldCertificate(localhostCertificate)
                    .build();

        server = new MockWebServer();
        server.useHttps(serverCertificates.sslSocketFactory(), false);
        server.enqueue(new MockResponse().setBody("Having trust issues?"));
        server.start();


        // Tell the clients to trust:
        clientCertificates = new HandshakeCertificates
              .Builder()
              .addTrustedCertificate(localhostCertificate.certificate())
              .build();

        HttpsURLConnection.setDefaultSSLSocketFactory(
                clientCertificates.sslSocketFactory());
        HttpsURLConnection.setDefaultHostnameVerifier((h, s) -> true);
    }

    @AfterAll
    static void tearDown() throws IOException {
      server.shutdown();
    }

    @Test
    void testFetchCertificates()
            throws GeneralSecurityException, IOException {

        String host = "localhost";
        int port = server.getPort();
        URL url = new URL("https", host, port, "/");
        KeyStore keyStore = KeyStoreBuilder.empty().create();

        // not trusted at first
        assertThat(CertificateUtil.isTrusted(host, port, keyStore)).isFalse();

        // fetch the certificate
        List<X509Certificate> certs =
                CertificateUtil.fetchCertificates(url.toString());
        assertThat(certs).hasSize(1);
        assertThat(CertificateUtil.toString(certs)).contains(
                "Subject", "Issuer", "sha1", "md5");

        // add the cert in our made-up keystore
        CertificateUtil.trustHost(
                "https://" + host + ":" + port, keyStore);
        assertThat(CertificateUtil.isTrusted(host, port, keyStore)).isTrue();

        try (InputStream is = url.openConnection().getInputStream()) {
            assertThat(IOUtils.toString(is, UTF_8)).isEqualTo(
                    "Having trust issues?");
        }
    }

}