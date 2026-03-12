package com.samlv2simulator.service;

import com.samlv2simulator.model.SamlAttribute;
import com.samlv2simulator.model.SamlProfile;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringWriter;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
public class SamlResponseBuilder {

    private static final String SAMLP_NS = "urn:oasis:names:tc:SAML:2.0:protocol";
    private static final String SAML_NS = "urn:oasis:names:tc:SAML:2.0:assertion";
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")
            .withZone(ZoneOffset.UTC);

    private final SigningService signingService;
    private final KeyStoreService keyStoreService;

    public SamlResponseBuilder(SigningService signingService, KeyStoreService keyStoreService) {
        this.signingService = signingService;
        this.keyStoreService = keyStoreService;
    }

    public String buildResponseXml(SamlProfile profile, String inResponseTo) throws Exception {
        Document doc = buildResponseDocument(profile, inResponseTo);

        // Sign if configured
        boolean needsSigning = profile.isSignAssertion() || profile.isSignResponse();
        if (needsSigning && hasSigningCredentials(profile)) {
            PrivateKey privateKey = keyStoreService.loadPrivateKey(profile);
            X509Certificate certificate = keyStoreService.loadCertificate(profile);

            if (profile.isSignAssertion()) {
                Element assertion = (Element) doc.getDocumentElement()
                        .getElementsByTagNameNS(SAML_NS, "Assertion").item(0);
                signingService.signElement(doc, assertion, privateKey, certificate,
                        profile.getSignatureAlgorithm(), profile.getDigestAlgorithm());
            }

            if (profile.isSignResponse()) {
                signingService.signElement(doc, doc.getDocumentElement(), privateKey, certificate,
                        profile.getSignatureAlgorithm(), profile.getDigestAlgorithm());
            }
        }

        return serializeDocument(doc);
    }

    private boolean hasSigningCredentials(SamlProfile profile) {
        if ("PEM".equalsIgnoreCase(profile.getKeyFormat())) {
            return profile.getSigningPrivateKey() != null && !profile.getSigningPrivateKey().isBlank()
                    && profile.getSigningCertificate() != null && !profile.getSigningCertificate().isBlank();
        }
        return profile.getKeystoreFile() != null && profile.getKeystoreFile().length > 0;
    }

    private Document buildResponseDocument(SamlProfile profile, String inResponseTo) throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc = db.newDocument();

        Instant now = Instant.now();
        String responseId = "_" + UUID.randomUUID();

        // <samlp:Response>
        Element response = doc.createElementNS(SAMLP_NS, "samlp:Response");
        response.setAttribute("ID", responseId);
        response.setAttribute("Version", "2.0");
        response.setAttribute("IssueInstant", ISO_FORMATTER.format(now));
        response.setAttribute("Destination", profile.getAcsUrl());
        if (inResponseTo != null && !inResponseTo.isBlank()) {
            response.setAttribute("InResponseTo", inResponseTo);
        }
        doc.appendChild(response);

        // <saml:Issuer>
        Element issuer = doc.createElementNS(SAML_NS, "saml:Issuer");
        issuer.setTextContent(profile.getIdpEntityId());
        response.appendChild(issuer);

        // <samlp:Status>
        Element status = doc.createElementNS(SAMLP_NS, "samlp:Status");
        Element statusCode = doc.createElementNS(SAMLP_NS, "samlp:StatusCode");
        statusCode.setAttribute("Value", "urn:oasis:names:tc:SAML:2.0:status:Success");
        status.appendChild(statusCode);
        response.appendChild(status);

        // <saml:Assertion>
        Element assertion = buildAssertion(doc, profile, now, inResponseTo);
        response.appendChild(assertion);

        return doc;
    }

    private Element buildAssertion(Document doc, SamlProfile profile, Instant now, String inResponseTo) {
        String assertionId = "_" + UUID.randomUUID();

        Element assertion = doc.createElementNS(SAML_NS, "saml:Assertion");
        assertion.setAttribute("ID", assertionId);
        assertion.setAttribute("Version", "2.0");
        assertion.setAttribute("IssueInstant", ISO_FORMATTER.format(now));

        // <saml:Issuer>
        Element issuer = doc.createElementNS(SAML_NS, "saml:Issuer");
        issuer.setTextContent(profile.getIdpEntityId());
        assertion.appendChild(issuer);

        // <saml:Subject>
        assertion.appendChild(buildSubject(doc, profile, now, inResponseTo));

        // <saml:Conditions>
        assertion.appendChild(buildConditions(doc, profile, now));

        // <saml:AuthnStatement>
        assertion.appendChild(buildAuthnStatement(doc, profile, now));

        // <saml:AttributeStatement>
        if (profile.getAttributes() != null && !profile.getAttributes().isEmpty()) {
            assertion.appendChild(buildAttributeStatement(doc, profile));
        }

        return assertion;
    }

    private Element buildSubject(Document doc, SamlProfile profile, Instant now, String inResponseTo) {
        Element subject = doc.createElementNS(SAML_NS, "saml:Subject");

        // <saml:NameID>
        Element nameId = doc.createElementNS(SAML_NS, "saml:NameID");
        nameId.setAttribute("Format", profile.getNameIdFormat());
        nameId.setAttribute("SPNameQualifier", profile.getSpEntityId());
        nameId.setTextContent(profile.getNameId());
        subject.appendChild(nameId);

        // <saml:SubjectConfirmation>
        Element confirmation = doc.createElementNS(SAML_NS, "saml:SubjectConfirmation");
        confirmation.setAttribute("Method", "urn:oasis:names:tc:SAML:2.0:cm:bearer");

        Element confirmData = doc.createElementNS(SAML_NS, "saml:SubjectConfirmationData");
        confirmData.setAttribute("NotOnOrAfter",
                ISO_FORMATTER.format(now.plusSeconds(profile.getNotOnOrAfterMinutes() * 60L)));
        confirmData.setAttribute("Recipient", profile.getAcsUrl());
        if (inResponseTo != null && !inResponseTo.isBlank()) {
            confirmData.setAttribute("InResponseTo", inResponseTo);
        }
        confirmation.appendChild(confirmData);
        subject.appendChild(confirmation);

        return subject;
    }

    private Element buildConditions(Document doc, SamlProfile profile, Instant now) {
        Element conditions = doc.createElementNS(SAML_NS, "saml:Conditions");
        conditions.setAttribute("NotBefore",
                ISO_FORMATTER.format(now.minusSeconds(profile.getNotBeforeMinutes() * 60L)));
        conditions.setAttribute("NotOnOrAfter",
                ISO_FORMATTER.format(now.plusSeconds(profile.getNotOnOrAfterMinutes() * 60L)));

        Element audienceRestriction = doc.createElementNS(SAML_NS, "saml:AudienceRestriction");
        java.util.List<String> audiences = profile.getAudiences();
        if (audiences == null || audiences.isEmpty()) {
            audiences = java.util.List.of(profile.getSpEntityId());
        }
        for (String audienceValue : audiences) {
            Element audience = doc.createElementNS(SAML_NS, "saml:Audience");
            audience.setTextContent(audienceValue);
            audienceRestriction.appendChild(audience);
        }
        conditions.appendChild(audienceRestriction);

        return conditions;
    }

    private Element buildAuthnStatement(Document doc, SamlProfile profile, Instant now) {
        Element statement = doc.createElementNS(SAML_NS, "saml:AuthnStatement");
        statement.setAttribute("AuthnInstant", ISO_FORMATTER.format(now));
        statement.setAttribute("SessionNotOnOrAfter",
                ISO_FORMATTER.format(now.plusSeconds(profile.getSessionNotOnOrAfterHours() * 3600L)));

        String sessionIndex = profile.getSessionIndex();
        if (sessionIndex == null || sessionIndex.isBlank()) {
            sessionIndex = "_session_" + UUID.randomUUID();
        }
        statement.setAttribute("SessionIndex", sessionIndex);

        Element authnContext = doc.createElementNS(SAML_NS, "saml:AuthnContext");
        Element classRef = doc.createElementNS(SAML_NS, "saml:AuthnContextClassRef");
        classRef.setTextContent("urn:oasis:names:tc:SAML:2.0:ac:classes:Password");
        authnContext.appendChild(classRef);
        statement.appendChild(authnContext);

        return statement;
    }

    private Element buildAttributeStatement(Document doc, SamlProfile profile) {
        Element statement = doc.createElementNS(SAML_NS, "saml:AttributeStatement");

        for (SamlAttribute attr : profile.getAttributes()) {
            if (attr.getAttributeName() == null || attr.getAttributeName().isBlank()) continue;

            Element samlAttr = doc.createElementNS(SAML_NS, "saml:Attribute");
            samlAttr.setAttribute("Name", attr.getAttributeName());
            samlAttr.setAttribute("NameFormat", attr.getNameFormat());

            // Support multi-valued attributes (semicolon-separated)
            String[] values = attr.getAttributeValue().split(";");
            for (String val : values) {
                Element attrValue = doc.createElementNS(SAML_NS, "saml:AttributeValue");
                attrValue.setAttributeNS("http://www.w3.org/2001/XMLSchema-instance", "xsi:type", "xs:string");
                attrValue.setTextContent(val.trim());
                samlAttr.appendChild(attrValue);
            }

            statement.appendChild(samlAttr);
        }

        return statement;
    }

    public String serializeDocument(Document doc) throws Exception {
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = tf.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        StringWriter writer = new StringWriter();
        transformer.transform(new DOMSource(doc), new StreamResult(writer));
        return writer.toString();
    }
}
