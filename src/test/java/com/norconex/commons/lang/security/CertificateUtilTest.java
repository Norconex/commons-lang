package com.norconex.commons.lang.security;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

@Disabled
public class CertificateUtilTest {

    @Test
    public void testFetchCertificates()
            throws GeneralSecurityException, IOException {
        List<X509Certificate> certs = CertificateUtil
                .fetchCertificates("https://untrusted-root.badssl.com/");
        System.out.println("CERTS:\n" + CertificateUtil.toString(certs));
    }

    @Test
    public void testFetchPage()
            throws GeneralSecurityException, IOException {

//      String untrustedURL = "https://example.com/";
        String untrustedURL = "https://untrusted-root.badssl.com/";
        URL url = new URL(untrustedURL);
        doit(url, null);
    }

    private void doit(URL url, KeyStore ks)
            throws GeneralSecurityException, IOException {

        KeyStore keyStore = ks;
        if (keyStore == null) {
            keyStore = KeyStoreBuilder.empty().create();
                                   // .fromJavaHome().create();
        }

        TrustManagerFactory tmf = TrustManagerFactory.getInstance(
                TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(keyStore);
        X509TrustManager tm = (X509TrustManager) tmf.getTrustManagers()[0];
        SSLContext context = SSLContext.getInstance("TLS");
        context.init(null, new TrustManager[] { tm }, null);
        SSLSocketFactory factory = context.getSocketFactory();

        HttpsURLConnection con = (HttpsURLConnection) url.openConnection();
        con.setSSLSocketFactory(factory);

        try (InputStream is = con.getInputStream()) {
            System.err.println("Connection obtained!");
            System.out.println(IOUtils.toString(is, StandardCharsets.UTF_8));
        } catch (SSLHandshakeException e) {
            System.err.println("handshake exception.");
            if (ks != null) {
                System.err.println("abort.");
                throw e;
            }

            CertificateUtil.trustHost(url.toString(), keyStore);
            doit(url, keyStore);
        }
    }

}