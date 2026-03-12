package com.samlv2simulator.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "saml_profiles")
public class SamlProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String profileName;

    // IdP Settings
    @Column(nullable = false)
    private String idpEntityId;

    // SP (Federating IdP) Settings
    @Column(nullable = false)
    private String spEntityId;

    @Column(nullable = false)
    private String acsUrl;

    private String audience;

    // User / Subject Settings
    private String nameId;

    private String nameIdFormat = "urn:oasis:names:tc:SAML:1.1:nameid-format:emailAddress";

    private String sessionIndex;

    // Assertion Timing (in minutes)
    private int notBeforeMinutes = 5;
    private int notOnOrAfterMinutes = 5;
    private int sessionNotOnOrAfterHours = 8;

    // Signing Settings
    private boolean signResponse = true;
    private boolean signAssertion = true;

    private String signatureAlgorithm = "RSA_SHA256";
    private String digestAlgorithm = "SHA256";

    // Key material
    @Column(length = 10000)
    private String signingCertificate;

    @Column(length = 10000)
    private String signingPrivateKey;

    private String keyFormat = "PEM";

    @Column(length = 10000)
    private byte[] keystoreFile;

    private String keystorePassword;
    private String keyAlias;
    private String keyPassword;

    // RelayState
    private String relayState;

    // Attributes
    @OneToMany(mappedBy = "profile", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("id ASC")
    private List<SamlAttribute> attributes = new ArrayList<>();

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Getters and setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getProfileName() {
        return profileName;
    }

    public void setProfileName(String profileName) {
        this.profileName = profileName;
    }

    public String getIdpEntityId() {
        return idpEntityId;
    }

    public void setIdpEntityId(String idpEntityId) {
        this.idpEntityId = idpEntityId;
    }

    public String getSpEntityId() {
        return spEntityId;
    }

    public void setSpEntityId(String spEntityId) {
        this.spEntityId = spEntityId;
    }

    public String getAcsUrl() {
        return acsUrl;
    }

    public void setAcsUrl(String acsUrl) {
        this.acsUrl = acsUrl;
    }

    public String getAudience() {
        return audience;
    }

    public void setAudience(String audience) {
        this.audience = audience;
    }

    public String getNameId() {
        return nameId;
    }

    public void setNameId(String nameId) {
        this.nameId = nameId;
    }

    public String getNameIdFormat() {
        return nameIdFormat;
    }

    public void setNameIdFormat(String nameIdFormat) {
        this.nameIdFormat = nameIdFormat;
    }

    public String getSessionIndex() {
        return sessionIndex;
    }

    public void setSessionIndex(String sessionIndex) {
        this.sessionIndex = sessionIndex;
    }

    public int getNotBeforeMinutes() {
        return notBeforeMinutes;
    }

    public void setNotBeforeMinutes(int notBeforeMinutes) {
        this.notBeforeMinutes = notBeforeMinutes;
    }

    public int getNotOnOrAfterMinutes() {
        return notOnOrAfterMinutes;
    }

    public void setNotOnOrAfterMinutes(int notOnOrAfterMinutes) {
        this.notOnOrAfterMinutes = notOnOrAfterMinutes;
    }

    public int getSessionNotOnOrAfterHours() {
        return sessionNotOnOrAfterHours;
    }

    public void setSessionNotOnOrAfterHours(int sessionNotOnOrAfterHours) {
        this.sessionNotOnOrAfterHours = sessionNotOnOrAfterHours;
    }

    public boolean isSignResponse() {
        return signResponse;
    }

    public void setSignResponse(boolean signResponse) {
        this.signResponse = signResponse;
    }

    public boolean isSignAssertion() {
        return signAssertion;
    }

    public void setSignAssertion(boolean signAssertion) {
        this.signAssertion = signAssertion;
    }

    public String getSignatureAlgorithm() {
        return signatureAlgorithm;
    }

    public void setSignatureAlgorithm(String signatureAlgorithm) {
        this.signatureAlgorithm = signatureAlgorithm;
    }

    public String getDigestAlgorithm() {
        return digestAlgorithm;
    }

    public void setDigestAlgorithm(String digestAlgorithm) {
        this.digestAlgorithm = digestAlgorithm;
    }

    public String getSigningCertificate() {
        return signingCertificate;
    }

    public void setSigningCertificate(String signingCertificate) {
        this.signingCertificate = signingCertificate;
    }

    public String getSigningPrivateKey() {
        return signingPrivateKey;
    }

    public void setSigningPrivateKey(String signingPrivateKey) {
        this.signingPrivateKey = signingPrivateKey;
    }

    public String getKeyFormat() {
        return keyFormat;
    }

    public void setKeyFormat(String keyFormat) {
        this.keyFormat = keyFormat;
    }

    public byte[] getKeystoreFile() {
        return keystoreFile;
    }

    public void setKeystoreFile(byte[] keystoreFile) {
        this.keystoreFile = keystoreFile;
    }

    public String getKeystorePassword() {
        return keystorePassword;
    }

    public void setKeystorePassword(String keystorePassword) {
        this.keystorePassword = keystorePassword;
    }

    public String getKeyAlias() {
        return keyAlias;
    }

    public void setKeyAlias(String keyAlias) {
        this.keyAlias = keyAlias;
    }

    public String getKeyPassword() {
        return keyPassword;
    }

    public void setKeyPassword(String keyPassword) {
        this.keyPassword = keyPassword;
    }

    public String getRelayState() {
        return relayState;
    }

    public void setRelayState(String relayState) {
        this.relayState = relayState;
    }

    public List<SamlAttribute> getAttributes() {
        return attributes;
    }

    public void setAttributes(List<SamlAttribute> attributes) {
        this.attributes = attributes;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public void addAttribute(SamlAttribute attribute) {
        attributes.add(attribute);
        attribute.setProfile(this);
    }

    public void removeAttribute(SamlAttribute attribute) {
        attributes.remove(attribute);
        attribute.setProfile(null);
    }
}
