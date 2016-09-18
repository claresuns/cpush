package cn.claresun.cpush.util;

import io.netty.handler.codec.http2.Http2SecurityUtil;
import io.netty.handler.ssl.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLException;
import java.io.*;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Enumeration;
import java.util.Objects;

/**
 * Created by claresun on 16-9-1.
 */
public class SslUtil {
    private static final Logger log = LoggerFactory.getLogger(SslUtil.class);

    public static SslContext getSslContextWithP12File(final File p12File, final String password) throws FileNotFoundException, SSLException, IOException {
        try (final InputStream p12InputStream = new FileInputStream(p12File)) {
            return getSslContextWithP12InputStream(p12InputStream, password);
        }
    }

    public static SslContext getSslContextWithP12InputStream(final InputStream p12InputStream, final String password) throws SSLException {
        final X509Certificate x509Certificate;
        final PrivateKey privateKey;

        try {
            final KeyStore.PrivateKeyEntry privateKeyEntry = getFirstPrivateKeyEntryFromP12InputStream(p12InputStream, password);

            final Certificate certificate = privateKeyEntry.getCertificate();

            if (!(certificate instanceof X509Certificate)) {
                throw new KeyStoreException("Found a certificate in the provided PKCS#12 file, but it was not an X.509 certificate.");
            }

            x509Certificate = (X509Certificate) certificate;
            privateKey = privateKeyEntry.getPrivateKey();
        } catch (final KeyStoreException | IOException e) {
            throw new SSLException(e);
        }

        return getSslContextWithCertificateAndPrivateKey(x509Certificate, privateKey, password);
    }

    public static KeyStore.PrivateKeyEntry getFirstPrivateKeyEntryFromP12InputStream(final InputStream p12InputStream, final String password) throws KeyStoreException, IOException {
        Objects.requireNonNull(password, "Password may be blank, but must not be null.");

        final KeyStore keyStore = KeyStore.getInstance("PKCS12");

        try {
            keyStore.load(p12InputStream, password.toCharArray());
        } catch (NoSuchAlgorithmException | CertificateException e) {
            throw new KeyStoreException(e);
        }

        final Enumeration<String> aliases = keyStore.aliases();
        final KeyStore.PasswordProtection passwordProtection = new KeyStore.PasswordProtection(password.toCharArray());

        while (aliases.hasMoreElements()) {
            final String alias = aliases.nextElement();

            KeyStore.Entry entry;

            try {
                try {
                    entry = keyStore.getEntry(alias, passwordProtection);
                } catch (final UnsupportedOperationException e) {
                    entry = keyStore.getEntry(alias, null);
                }
            } catch (final UnrecoverableEntryException | NoSuchAlgorithmException e) {
                throw new KeyStoreException(e);
            }

            if (entry instanceof KeyStore.PrivateKeyEntry) {
                return (KeyStore.PrivateKeyEntry) entry;
            }
        }

        throw new KeyStoreException("Key store did not contain any private key entries.");
    }

    public static SslContext getSslContextWithCertificateAndPrivateKey(final X509Certificate certificate, final PrivateKey privateKey, final String privateKeyPassword) throws SSLException {
        return getBaseSslContextBuilder()
                .keyManager(privateKey, privateKeyPassword, certificate)
                .build();
    }

    private static SslContextBuilder getBaseSslContextBuilder() {
        final SslProvider sslProvider;

        if (OpenSsl.isAvailable()) {
            if (OpenSsl.isAlpnSupported()) {
                log.info("OpenSSL (via netty-tcnative) is available and supports ALPN; will use OpenSSL.");
                sslProvider = SslProvider.OPENSSL;
            } else {
                log.info("OpenSSL (via netty-tcnative) is available, but does not support ALPN; will use JDK SSL provider.");
                sslProvider = SslProvider.JDK;
            }
        } else {
            log.info("OpenSSL (via netty-tcnative) not available; will use JDK SSL provider.");
            sslProvider = SslProvider.JDK;
        }

        return SslContextBuilder.forClient()
                .sslProvider(sslProvider)
                .ciphers(Http2SecurityUtil.CIPHERS, SupportedCipherSuiteFilter.INSTANCE)
                .applicationProtocolConfig(
                        new ApplicationProtocolConfig(ApplicationProtocolConfig.Protocol.ALPN,
                                ApplicationProtocolConfig.SelectorFailureBehavior.NO_ADVERTISE,
                                ApplicationProtocolConfig.SelectedListenerFailureBehavior.ACCEPT,
                                ApplicationProtocolNames.HTTP_2));
    }

    public static String sanitizeTokenString(final String tokenString) {
        return tokenString.replaceAll("[^a-fA-F0-9]", "");
    }
}
