/* Copyright 2020-2022 Norconex Inc.
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

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.norconex.commons.lang.url.HttpURL;

import jakarta.xml.bind.DatatypeConverter;
import lombok.NonNull;

/**
 * Certificate-related (e.g., SSL) utility methods.
 * @since 2.0.0
 */
public final class CertificateUtil {

    //MAYBE: make those configurable: protocol, provider, algorithm
    //MAYBE: maybe provide option to specify which host to trust
    // with a matcher (.* for all).

    private static final Logger LOG =
            LoggerFactory.getLogger(CertificateUtil.class);

    private CertificateUtil() {
    }

    /**
     * Fetches certificates associated with the URL host.
     * @param url url from which to fetch certificates
     * @return certificates
     * @throws GeneralSecurityException certificate exception
     * @throws IOException I/O exception
     */
    public static List<X509Certificate> fetchCertificates(@NonNull String url)
            throws GeneralSecurityException, IOException {
        var u = new HttpURL(url);
        return fetchCertificates(u.getHost(), u.getPort());
    }

    /**
     * Fetches certificates associated with the given host and port.
     * @param host from which to fetch certificates
     * @param port host port
     * @return certificates
     * @throws GeneralSecurityException certificate exception
     * @throws IOException I/O exception
     */
    public static List<X509Certificate> fetchCertificates(
            @NonNull String host, int port)
            throws GeneralSecurityException, IOException {
        List<X509Certificate> certs = new ArrayList<>();
        fetchCertificates(certs, host, port, null);
        return certs;
    }

    /**
     * Gets whether a host is trusted by the given key store.
     * @param host host to verify for trust
     * @param port host port
     * @param keyStore key store used to establish trust
     * @return <code>true</code> if trusted
     * @throws GeneralSecurityException certificate exception
     * @throws IOException I/O exception
     */
    public static boolean isTrusted(
            @NonNull String host, int port, KeyStore keyStore)
            throws GeneralSecurityException, IOException {
        return fetchCertificates(new ArrayList<>(), host, port, keyStore);
    }

    /**
     * Trusts the URL host.  Adds the host certificates to the supplied
     * key store if not already trusted.
     * @param url url from which to get the host
     * @param keyStore key store used for trust verification and to store
     *                 new certificates
     * @return the number of new certificates added to the key store
     *         (0 if already trusted)
     * @throws GeneralSecurityException certificate exception
     * @throws IOException I/O exception
     */
    public static int trustHost(@NonNull String url, KeyStore keyStore)
            throws GeneralSecurityException, IOException {
        var u = new HttpURL(url);
        return trustHost(u.getHost(), u.getPort(), keyStore);
    }

    /**
     * Trusts a host.  Adds the host certificates to the supplied
     * key store if not already trusted.
     * @param host the host to trust
     * @param port the host port
     * @param keyStore key store used for trust verification and to store
     *                 new certificates
     * @return the number of new certificates added to the key store
     *         (0 if already trusted)
     * @throws GeneralSecurityException certificate exception
     * @throws IOException I/O exception
     */
    public static int trustHost(
            @NonNull String host, int port, KeyStore keyStore)
            throws GeneralSecurityException, IOException {
        List<X509Certificate> certs = new ArrayList<>();
        var trusted = fetchCertificates(certs, host, port, keyStore);
        if (LOG.isDebugEnabled()) {
            LOG.debug("Fetched {} certificates: \n{}",
                    certs.size(), toString(certs));
        }
        if (!trusted) {
            var idx = 0;
            for (X509Certificate cert : certs) {
                var alias = host + "-" + ++idx;
                keyStore.setCertificateEntry(alias, cert);
            }
            return certs.size();
        }
        return 0;
    }

    /**
     * Returns a friendly string display of certificates.
     * @param certificates certificates to convert to string
     * @return string display of certificates
     * @throws GeneralSecurityException certificate exception
     */
    public static String toString(@NonNull List<X509Certificate> certificates)
            throws GeneralSecurityException {
        var sha1 = MessageDigest.getInstance("SHA1"); //NOSONAR informational
        var md5 = MessageDigest.getInstance("MD5"); //NOSONAR informational
        var b = new StringBuilder();
        for (var i = 0; i < certificates.size(); i++) {
            var cert = certificates.get(i);
            b.append(" " + (i + 1) + " Subject "
                    + cert.getSubjectX500Principal() + "\n");
            b.append("   Issuer  " + cert.getIssuerX500Principal() + "\n");
            sha1.update(cert.getEncoded());
            b.append("   sha1    "
                    + DatatypeConverter.printHexBinary(sha1.digest()) + "\n");
            md5.update(cert.getEncoded());
            b.append("   md5     "
                    + DatatypeConverter.printHexBinary(md5.digest()) + "\n");
        }
        return b.toString();
    }

    // return true if trusted
    private static boolean fetchCertificates(List<X509Certificate> certificates,
            String host, int port, KeyStore keyStore)
            throws GeneralSecurityException, IOException {
        var context = SSLContext.getInstance("TLS");
        var tmf = TrustManagerFactory.getInstance(
                TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(keyStore);
        var defaultTrustManager =
                (X509TrustManager) tmf.getTrustManagers()[0];
        var certInterceptor =
                new CertificateInterceptor(defaultTrustManager);
        context.init(null, new TrustManager[] { certInterceptor }, null);
        var factory = context.getSocketFactory();

        LOG.debug("Connecting to {}:{}...", host, port);
        var trusted = false;
        try (var socket = (SSLSocket) factory.createSocket(host, port)) {
            socket.setSoTimeout(10000);
            LOG.debug("Starting SSL handshake...");
            socket.startHandshake();
            LOG.debug("No errors, certificate is already trusted.");
            trusted = true;
        } catch (SSLException e) {
            LOG.debug("Errors. Certificate not trusted.", e);
        }

        var chain = certInterceptor.getCerts();
        if (chain != null) {
            certificates.addAll(Arrays.asList(chain));
        } else {
            LOG.error("Could not obtain host certificate chain for {}:{}.",
                    host, port);
        }
        return trusted;
    }

    private static class CertificateInterceptor implements X509TrustManager {
        private final X509TrustManager tm;
        private X509Certificate[] chain;

        private CertificateInterceptor(X509TrustManager tm) {
            this.tm = tm;
        }

        public X509Certificate[] getCerts() {
            return chain;
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() { // NOSONAR
            return chain;
        }

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType)
                throws CertificateException {
            tm.checkClientTrusted(chain, authType);
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType)
                throws CertificateException {
            this.chain = chain;
            tm.checkServerTrusted(chain, authType);
        }
    }
}
