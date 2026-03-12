package com.samlv2simulator.service;

import com.samlv2simulator.model.SamlProfile;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Base64;

@Service
public class KeyStoreService {

    public PrivateKey loadPrivateKey(SamlProfile profile) throws Exception {
        String format = profile.getKeyFormat();
        if ("PEM".equalsIgnoreCase(format)) {
            return loadPrivateKeyFromPem(profile.getSigningPrivateKey());
        } else {
            return loadPrivateKeyFromKeystore(profile);
        }
    }

    public X509Certificate loadCertificate(SamlProfile profile) throws Exception {
        String format = profile.getKeyFormat();
        if ("PEM".equalsIgnoreCase(format)) {
            return loadCertificateFromPem(profile.getSigningCertificate());
        } else {
            return loadCertificateFromKeystore(profile);
        }
    }

    private PrivateKey loadPrivateKeyFromPem(String pem) throws Exception {
        if (pem == null || pem.isBlank()) {
            throw new IllegalArgumentException("No private key PEM provided");
        }
        try (PEMParser parser = new PEMParser(new StringReader(pem))) {
            Object object = parser.readObject();
            JcaPEMKeyConverter converter = new JcaPEMKeyConverter();
            if (object instanceof PEMKeyPair keyPair) {
                return converter.getPrivateKey(keyPair.getPrivateKeyInfo());
            } else if (object instanceof PrivateKeyInfo privateKeyInfo) {
                return converter.getPrivateKey(privateKeyInfo);
            }
            throw new IllegalArgumentException("Unsupported PEM object: " + object.getClass().getName());
        }
    }

    private X509Certificate loadCertificateFromPem(String pem) throws Exception {
        if (pem == null || pem.isBlank()) {
            throw new IllegalArgumentException("No certificate PEM provided");
        }
        String cleaned = pem.trim();
        if (!cleaned.startsWith("-----BEGIN")) {
            cleaned = "-----BEGIN CERTIFICATE-----\n" + cleaned + "\n-----END CERTIFICATE-----";
        }
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        return (X509Certificate) cf.generateCertificate(
                new ByteArrayInputStream(cleaned.getBytes()));
    }

    private PrivateKey loadPrivateKeyFromKeystore(SamlProfile profile) throws Exception {
        KeyStore ks = loadKeyStore(profile);
        String alias = profile.getKeyAlias();
        if (alias == null || alias.isBlank()) {
            alias = ks.aliases().nextElement();
        }
        String keyPass = profile.getKeyPassword();
        if (keyPass == null || keyPass.isBlank()) {
            keyPass = profile.getKeystorePassword();
        }
        return (PrivateKey) ks.getKey(alias, keyPass.toCharArray());
    }

    private X509Certificate loadCertificateFromKeystore(SamlProfile profile) throws Exception {
        KeyStore ks = loadKeyStore(profile);
        String alias = profile.getKeyAlias();
        if (alias == null || alias.isBlank()) {
            alias = ks.aliases().nextElement();
        }
        return (X509Certificate) ks.getCertificate(alias);
    }

    private KeyStore loadKeyStore(SamlProfile profile) throws Exception {
        byte[] data = profile.getKeystoreFile();
        if (data == null || data.length == 0) {
            throw new IllegalArgumentException("No keystore file provided");
        }
        String type = "PKCS12".equalsIgnoreCase(profile.getKeyFormat()) ? "PKCS12" : "JKS";
        KeyStore ks = KeyStore.getInstance(type);
        ks.load(new ByteArrayInputStream(data),
                profile.getKeystorePassword() != null ? profile.getKeystorePassword().toCharArray() : null);
        return ks;
    }

    public KeyPairAndCert generateSelfSignedKeyPair() throws Exception {
        var keyGen = java.security.KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        var keyPair = keyGen.generateKeyPair();

        var startDate = new java.util.Date();
        var endDate = new java.util.Date(startDate.getTime() + 365L * 24 * 60 * 60 * 1000);

        var certBuilder = new org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder(
                new org.bouncycastle.asn1.x500.X500Name("CN=SAML Simulator, O=Test"),
                java.math.BigInteger.valueOf(System.currentTimeMillis()),
                startDate, endDate,
                new org.bouncycastle.asn1.x500.X500Name("CN=SAML Simulator, O=Test"),
                keyPair.getPublic()
        );

        var signer = new org.bouncycastle.operator.jcajce.JcaContentSignerBuilder("SHA256WithRSA")
                .build(keyPair.getPrivate());
        var certHolder = certBuilder.build(signer);
        var cert = new org.bouncycastle.cert.jcajce.JcaX509CertificateConverter()
                .getCertificate(certHolder);

        return new KeyPairAndCert(keyPair.getPrivate(), cert);
    }

    public String privateKeyToPem(PrivateKey key) {
        StringBuilder sb = new StringBuilder();
        sb.append("-----BEGIN PRIVATE KEY-----\n");
        sb.append(Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(key.getEncoded()));
        sb.append("\n-----END PRIVATE KEY-----\n");
        return sb.toString();
    }

    public String certificateToPem(X509Certificate cert) throws Exception {
        StringBuilder sb = new StringBuilder();
        sb.append("-----BEGIN CERTIFICATE-----\n");
        sb.append(Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(cert.getEncoded()));
        sb.append("\n-----END CERTIFICATE-----\n");
        return sb.toString();
    }

    public record KeyPairAndCert(PrivateKey privateKey, X509Certificate certificate) {
    }
}
