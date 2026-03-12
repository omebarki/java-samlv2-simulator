package com.samlv2simulator.service;

import org.apache.xml.security.signature.XMLSignature;
import org.apache.xml.security.transforms.Transforms;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.security.PrivateKey;
import java.security.cert.X509Certificate;

@Service
public class SigningService {

    public void signElement(Document doc, Element elementToSign, PrivateKey privateKey,
                            X509Certificate certificate, String signatureAlgorithm,
                            String digestAlgorithm) throws Exception {
        String sigAlgoUri = resolveSignatureAlgorithm(signatureAlgorithm);
        String digestAlgoUri = resolveDigestAlgorithm(digestAlgorithm);

        String referenceId = elementToSign.getAttribute("ID");

        // Register the ID attribute so the XML signature URI reference resolver
        // can find the element via Document.getElementById()
        elementToSign.setIdAttributeNS(null, "ID", true);

        XMLSignature sig = new XMLSignature(doc, "", sigAlgoUri,
                org.apache.xml.security.c14n.Canonicalizer.ALGO_ID_C14N_EXCL_OMIT_COMMENTS);

        // Insert signature after the Issuer element if present
        Element issuer = findFirstChildElementNS(elementToSign,
                "urn:oasis:names:tc:SAML:2.0:assertion", "Issuer");
        if (issuer == null) {
            issuer = findFirstChildElementNS(elementToSign,
                    "urn:oasis:names:tc:SAML:2.0:protocol", "Status");
        }

        if (issuer != null && issuer.getNextSibling() != null) {
            elementToSign.insertBefore(sig.getElement(), issuer.getNextSibling());
        } else if (issuer != null) {
            elementToSign.appendChild(sig.getElement());
        } else {
            elementToSign.insertBefore(sig.getElement(), elementToSign.getFirstChild());
        }

        // Create reference
        Transforms transforms = new Transforms(doc);
        transforms.addTransform(Transforms.TRANSFORM_ENVELOPED_SIGNATURE);
        transforms.addTransform(Transforms.TRANSFORM_C14N_EXCL_OMIT_COMMENTS);

        sig.addDocument("#" + referenceId, transforms, digestAlgoUri);

        // Add certificate to KeyInfo
        sig.addKeyInfo(certificate);

        // Sign
        sig.sign(privateKey);
    }

    private Element findFirstChildElementNS(Element parent, String namespaceUri, String localName) {
        Node child = parent.getFirstChild();
        while (child != null) {
            if (child instanceof Element el
                    && namespaceUri.equals(el.getNamespaceURI())
                    && localName.equals(el.getLocalName())) {
                return el;
            }
            child = child.getNextSibling();
        }
        return null;
    }

    public static String resolveSignatureAlgorithm(String algo) {
        if (algo == null) return XMLSignature.ALGO_ID_SIGNATURE_RSA_SHA256;
        return switch (algo.toUpperCase().replace("-", "_")) {
            case "RSA_SHA1" -> XMLSignature.ALGO_ID_SIGNATURE_RSA_SHA1;
            case "RSA_SHA512" -> XMLSignature.ALGO_ID_SIGNATURE_RSA_SHA512;
            default -> XMLSignature.ALGO_ID_SIGNATURE_RSA_SHA256;
        };
    }

    private static final String DIGEST_SHA1 = "http://www.w3.org/2000/09/xmldsig#sha1";
    private static final String DIGEST_SHA256 = "http://www.w3.org/2001/04/xmlenc#sha256";
    private static final String DIGEST_SHA512 = "http://www.w3.org/2001/04/xmlenc#sha512";

    public static String resolveDigestAlgorithm(String algo) {
        if (algo == null) return DIGEST_SHA256;
        return switch (algo.toUpperCase().replace("-", "")) {
            case "SHA1" -> DIGEST_SHA1;
            case "SHA512" -> DIGEST_SHA512;
            default -> DIGEST_SHA256;
        };
    }
}
