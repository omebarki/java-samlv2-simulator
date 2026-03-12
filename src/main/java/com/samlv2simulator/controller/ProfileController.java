package com.samlv2simulator.controller;

import com.samlv2simulator.model.SamlAttribute;
import com.samlv2simulator.model.SamlProfile;
import com.samlv2simulator.repository.SamlProfileRepository;
import com.samlv2simulator.service.KeyStoreService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/profiles")
public class ProfileController {

    private final SamlProfileRepository profileRepository;
    private final KeyStoreService keyStoreService;

    public ProfileController(SamlProfileRepository profileRepository, KeyStoreService keyStoreService) {
        this.profileRepository = profileRepository;
        this.keyStoreService = keyStoreService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("profiles", profileRepository.findAllByOrderByUpdatedAtDesc());
        return "profile-list";
    }

    @GetMapping("/new")
    public String newProfile(Model model) {
        SamlProfile profile = new SamlProfile();
        profile.setIdpEntityId("https://simulator.example.com/idp");
        profile.setNameIdFormat("urn:oasis:names:tc:SAML:1.1:nameid-format:emailAddress");
        profile.setSignatureAlgorithm("RSA_SHA256");
        profile.setDigestAlgorithm("SHA256");
        model.addAttribute("profile", profile);
        model.addAttribute("isNew", true);
        return "profile-form";
    }

    @PostMapping
    public String create(@ModelAttribute SamlProfile profile,
                         @RequestParam(value = "keystoreUpload", required = false) MultipartFile keystoreFile,
                         @RequestParam(value = "audiences", required = false) String[] audiences,
                         @RequestParam(value = "attrNames", required = false) String[] attrNames,
                         @RequestParam(value = "attrValues", required = false) String[] attrValues,
                         @RequestParam(value = "attrFormats", required = false) String[] attrFormats,
                         RedirectAttributes redirectAttributes) {
        try {
            handleKeystoreUpload(profile, keystoreFile);
            handleAudiences(profile, audiences);
            handleAttributes(profile, attrNames, attrValues, attrFormats);
            profileRepository.save(profile);
            redirectAttributes.addFlashAttribute("success", "Profile created successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error creating profile: " + e.getMessage());
        }
        return "redirect:/profiles";
    }

    @GetMapping("/{id}/edit")
    public String edit(@PathVariable Long id, Model model) {
        SamlProfile profile = profileRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Profile not found: " + id));
        model.addAttribute("profile", profile);
        model.addAttribute("isNew", false);
        return "profile-form";
    }

    @PutMapping("/{id}")
    public String update(@PathVariable Long id,
                         @ModelAttribute SamlProfile profile,
                         @RequestParam(value = "keystoreUpload", required = false) MultipartFile keystoreFile,
                         @RequestParam(value = "audiences", required = false) String[] audiences,
                         @RequestParam(value = "attrNames", required = false) String[] attrNames,
                         @RequestParam(value = "attrValues", required = false) String[] attrValues,
                         @RequestParam(value = "attrFormats", required = false) String[] attrFormats,
                         RedirectAttributes redirectAttributes) {
        try {
            SamlProfile existing = profileRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Profile not found: " + id));

            copyProfileFields(profile, existing);
            handleKeystoreUpload(existing, keystoreFile);
            existing.getAudiences().clear();
            handleAudiences(existing, audiences);
            existing.getAttributes().clear();
            handleAttributes(existing, attrNames, attrValues, attrFormats);
            profileRepository.save(existing);
            redirectAttributes.addFlashAttribute("success", "Profile updated successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error updating profile: " + e.getMessage());
        }
        return "redirect:/profiles";
    }

    @DeleteMapping("/{id}")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        profileRepository.deleteById(id);
        redirectAttributes.addFlashAttribute("success", "Profile deleted");
        return "redirect:/profiles";
    }

    @GetMapping("/{id}/clone")
    public String cloneProfile(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        SamlProfile original = profileRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Profile not found: " + id));

        SamlProfile clone = new SamlProfile();
        copyProfileFields(original, clone);
        clone.setProfileName(original.getProfileName() + " (copy)");
        clone.setKeystoreFile(original.getKeystoreFile());

        for (SamlAttribute attr : original.getAttributes()) {
            SamlAttribute clonedAttr = new SamlAttribute(attr.getAttributeName(), attr.getAttributeValue());
            clonedAttr.setNameFormat(attr.getNameFormat());
            clone.addAttribute(clonedAttr);
        }

        profileRepository.save(clone);
        redirectAttributes.addFlashAttribute("success", "Profile cloned successfully");
        return "redirect:/profiles";
    }

    @PostMapping("/generate-keypair")
    @ResponseBody
    public java.util.Map<String, String> generateKeyPair() throws Exception {
        KeyStoreService.KeyPairAndCert kp = keyStoreService.generateSelfSignedKeyPair();
        java.util.Map<String, String> result = new java.util.HashMap<>();
        result.put("privateKey", keyStoreService.privateKeyToPem(kp.privateKey()));
        result.put("certificate", keyStoreService.certificateToPem(kp.certificate()));
        return result;
    }

    private void handleKeystoreUpload(SamlProfile profile, MultipartFile file) throws Exception {
        if (file != null && !file.isEmpty()) {
            profile.setKeystoreFile(file.getBytes());
        }
    }

    private void handleAudiences(SamlProfile profile, String[] audiences) {
        if (audiences == null) return;
        for (String aud : audiences) {
            if (aud != null && !aud.isBlank()) {
                profile.getAudiences().add(aud.trim());
            }
        }
    }

    private void handleAttributes(SamlProfile profile, String[] names, String[] values, String[] formats) {
        if (names == null) return;
        for (int i = 0; i < names.length; i++) {
            if (names[i] != null && !names[i].isBlank()) {
                SamlAttribute attr = new SamlAttribute();
                attr.setAttributeName(names[i]);
                attr.setAttributeValue(values != null && i < values.length ? values[i] : "");
                attr.setNameFormat(formats != null && i < formats.length && formats[i] != null && !formats[i].isBlank()
                        ? formats[i] : "urn:oasis:names:tc:SAML:2.0:attrname-format:basic");
                profile.addAttribute(attr);
            }
        }
    }

    private void copyProfileFields(SamlProfile from, SamlProfile to) {
        to.setProfileName(from.getProfileName());
        to.setIdpEntityId(from.getIdpEntityId());
        to.setSpEntityId(from.getSpEntityId());
        to.setAcsUrl(from.getAcsUrl());
        to.setAudiences(new ArrayList<>(from.getAudiences()));
        to.setNameId(from.getNameId());
        to.setNameIdFormat(from.getNameIdFormat());
        to.setSessionIndex(from.getSessionIndex());
        to.setNotBeforeMinutes(from.getNotBeforeMinutes());
        to.setNotOnOrAfterMinutes(from.getNotOnOrAfterMinutes());
        to.setSessionNotOnOrAfterHours(from.getSessionNotOnOrAfterHours());
        to.setSignResponse(from.isSignResponse());
        to.setSignAssertion(from.isSignAssertion());
        to.setSignatureAlgorithm(from.getSignatureAlgorithm());
        to.setDigestAlgorithm(from.getDigestAlgorithm());
        to.setSigningCertificate(from.getSigningCertificate());
        to.setSigningPrivateKey(from.getSigningPrivateKey());
        to.setKeyFormat(from.getKeyFormat());
        to.setKeystorePassword(from.getKeystorePassword());
        to.setKeyAlias(from.getKeyAlias());
        to.setKeyPassword(from.getKeyPassword());
        to.setRelayState(from.getRelayState());
    }
}
