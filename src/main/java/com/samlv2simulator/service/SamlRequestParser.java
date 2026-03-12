package com.samlv2simulator.service;

import com.samlv2simulator.util.SamlEncodingUtils;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class SamlRequestParser {

    private static final String SAMLP_NS = "urn:oasis:names:tc:SAML:2.0:protocol";
    private static final String SAML_NS = "urn:oasis:names:tc:SAML:2.0:assertion";

    public ParsedAuthnRequest parseRedirectBinding(String samlRequest) throws Exception {
        String xml = SamlEncodingUtils.base64DecodeAndInflate(samlRequest);
        return parseXml(xml);
    }

    public ParsedAuthnRequest parsePostBinding(String samlRequest) throws Exception {
        String xml = SamlEncodingUtils.base64Decode(samlRequest);
        return parseXml(xml);
    }

    private ParsedAuthnRequest parseXml(String xml) throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        // Prevent XXE
        dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        dbf.setFeature("http://xml.org/sax/features/external-general-entities", false);
        dbf.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document document = db.parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        Element root = document.getDocumentElement();

        ParsedAuthnRequest result = new ParsedAuthnRequest();
        result.setRawXml(prettyPrintXml(root));
        result.setId(root.getAttribute("ID"));
        result.setIssueInstant(root.getAttribute("IssueInstant"));
        result.setDestination(root.getAttribute("Destination"));
        result.setAssertionConsumerServiceURL(root.getAttribute("AssertionConsumerServiceURL"));
        result.setProtocolBinding(root.getAttribute("ProtocolBinding"));
        result.setForceAuthn("true".equalsIgnoreCase(root.getAttribute("ForceAuthn")));
        result.setIsPassive("true".equalsIgnoreCase(root.getAttribute("IsPassive")));

        // Extract Issuer
        NodeList issuerNodes = root.getElementsByTagNameNS(SAML_NS, "Issuer");
        if (issuerNodes.getLength() > 0) {
            result.setIssuer(issuerNodes.item(0).getTextContent());
        }

        return result;
    }

    private String prettyPrintXml(Element element) throws Exception {
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = tf.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        StringWriter writer = new StringWriter();
        transformer.transform(new DOMSource(element), new StreamResult(writer));
        return writer.toString();
    }

    public static class ParsedAuthnRequest {
        private String rawXml;
        private String id;
        private String issueInstant;
        private String issuer;
        private String destination;
        private String assertionConsumerServiceURL;
        private String protocolBinding;
        private Boolean forceAuthn;
        private Boolean isPassive;

        public String getRawXml() { return rawXml; }
        public void setRawXml(String rawXml) { this.rawXml = rawXml; }
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getIssueInstant() { return issueInstant; }
        public void setIssueInstant(String issueInstant) { this.issueInstant = issueInstant; }
        public String getIssuer() { return issuer; }
        public void setIssuer(String issuer) { this.issuer = issuer; }
        public String getDestination() { return destination; }
        public void setDestination(String destination) { this.destination = destination; }
        public String getAssertionConsumerServiceURL() { return assertionConsumerServiceURL; }
        public void setAssertionConsumerServiceURL(String url) { this.assertionConsumerServiceURL = url; }
        public String getProtocolBinding() { return protocolBinding; }
        public void setProtocolBinding(String protocolBinding) { this.protocolBinding = protocolBinding; }
        public Boolean getForceAuthn() { return forceAuthn; }
        public void setForceAuthn(Boolean forceAuthn) { this.forceAuthn = forceAuthn; }
        public Boolean getIsPassive() { return isPassive; }
        public void setIsPassive(Boolean isPassive) { this.isPassive = isPassive; }

        public Map<String, String> toMap() {
            Map<String, String> map = new LinkedHashMap<>();
            map.put("ID", id);
            map.put("IssueInstant", issueInstant);
            map.put("Issuer", issuer);
            map.put("Destination", destination);
            map.put("AssertionConsumerServiceURL", assertionConsumerServiceURL);
            map.put("ProtocolBinding", protocolBinding);
            map.put("ForceAuthn", forceAuthn != null ? forceAuthn.toString() : "false");
            map.put("IsPassive", isPassive != null ? isPassive.toString() : "false");
            return map;
        }
    }
}
