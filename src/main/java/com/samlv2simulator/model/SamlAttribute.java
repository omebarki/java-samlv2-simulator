package com.samlv2simulator.model;

import jakarta.persistence.*;

@Entity
@Table(name = "saml_attributes")
public class SamlAttribute {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String attributeName;

    @Column(nullable = false, length = 2000)
    private String attributeValue;

    private String nameFormat = "urn:oasis:names:tc:SAML:2.0:attrname-format:basic";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_id")
    private SamlProfile profile;

    public SamlAttribute() {
    }

    public SamlAttribute(String attributeName, String attributeValue) {
        this.attributeName = attributeName;
        this.attributeValue = attributeValue;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAttributeName() {
        return attributeName;
    }

    public void setAttributeName(String attributeName) {
        this.attributeName = attributeName;
    }

    public String getAttributeValue() {
        return attributeValue;
    }

    public void setAttributeValue(String attributeValue) {
        this.attributeValue = attributeValue;
    }

    public String getNameFormat() {
        return nameFormat;
    }

    public void setNameFormat(String nameFormat) {
        this.nameFormat = nameFormat;
    }

    public SamlProfile getProfile() {
        return profile;
    }

    public void setProfile(SamlProfile profile) {
        this.profile = profile;
    }
}
