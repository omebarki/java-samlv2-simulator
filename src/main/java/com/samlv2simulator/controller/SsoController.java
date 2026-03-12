package com.samlv2simulator.controller;

import com.samlv2simulator.model.SamlExchangeLog;
import com.samlv2simulator.model.SamlProfile;
import com.samlv2simulator.repository.SamlExchangeLogRepository;
import com.samlv2simulator.repository.SamlProfileRepository;
import com.samlv2simulator.service.SamlRequestParser;
import com.samlv2simulator.service.SamlResponseBuilder;
import com.samlv2simulator.util.SamlEncodingUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/sso")
public class SsoController {

    private final SamlProfileRepository profileRepository;
    private final SamlExchangeLogRepository logRepository;
    private final SamlResponseBuilder responseBuilder;
    private final SamlRequestParser requestParser;

    public SsoController(SamlProfileRepository profileRepository,
                         SamlExchangeLogRepository logRepository,
                         SamlResponseBuilder responseBuilder,
                         SamlRequestParser requestParser) {
        this.profileRepository = profileRepository;
        this.logRepository = logRepository;
        this.responseBuilder = responseBuilder;
        this.requestParser = requestParser;
    }

    @GetMapping("/idp-init/{profileId}")
    public String prepareIdpInitiated(@PathVariable Long profileId, Model model) {
        try {
            SamlProfile profile = profileRepository.findById(profileId)
                    .orElseThrow(() -> new IllegalArgumentException("Profile not found: " + profileId));

            String xml = responseBuilder.buildResponseXml(profile, null);
            String base64 = SamlEncodingUtils.base64Encode(xml);

            model.addAttribute("profile", profile);
            model.addAttribute("samlResponseXml", xml);
            model.addAttribute("samlResponseBase64", base64);
            model.addAttribute("acsUrl", profile.getAcsUrl());
            model.addAttribute("relayState", profile.getRelayState());
            model.addAttribute("flowType", "IdP-Initiated");
            model.addAttribute("inResponseTo", "");
            return "send-response";
        } catch (Exception e) {
            model.addAttribute("error", "Error generating SAML Response: " + e.getMessage());
            return "send-response";
        }
    }

    @PostMapping("/send")
    public String sendResponse(@RequestParam String samlResponseXml,
                               @RequestParam String acsUrl,
                               @RequestParam(required = false) String relayState,
                               @RequestParam(required = false) Long profileId,
                               @RequestParam(required = false) String flowType,
                               @RequestParam(required = false) String inResponseTo,
                               Model model) {
        String base64 = SamlEncodingUtils.base64Encode(samlResponseXml);

        // Log the exchange
        SamlExchangeLog log = new SamlExchangeLog();
        log.setFlowType(flowType != null ? flowType : "IdP-Initiated");
        log.setSamlResponse(samlResponseXml);
        log.setAcsUrl(acsUrl);
        log.setRelayState(relayState);
        if (profileId != null) {
            profileRepository.findById(profileId).ifPresent(p -> {
                log.setProfileName(p.getProfileName());
                log.setProfileId(p.getId());
                log.setSpEntityId(p.getSpEntityId());
                log.setNameId(p.getNameId());
            });
        }
        logRepository.save(log);

        model.addAttribute("acsUrl", acsUrl);
        model.addAttribute("samlResponseBase64", base64);
        model.addAttribute("relayState", relayState);
        return "post-form";
    }

    @PostMapping("/regenerate")
    public String regenerateXml(@RequestParam Long profileId,
                                @RequestParam(required = false) String inResponseTo,
                                @RequestParam(required = false) String flowType,
                                Model model) {
        try {
            SamlProfile profile = profileRepository.findById(profileId)
                    .orElseThrow(() -> new IllegalArgumentException("Profile not found"));

            String xml = responseBuilder.buildResponseXml(profile, inResponseTo);
            String base64 = SamlEncodingUtils.base64Encode(xml);

            model.addAttribute("profile", profile);
            model.addAttribute("samlResponseXml", xml);
            model.addAttribute("samlResponseBase64", base64);
            model.addAttribute("acsUrl", profile.getAcsUrl());
            model.addAttribute("relayState", profile.getRelayState());
            model.addAttribute("flowType", flowType != null ? flowType : "IdP-Initiated");
            model.addAttribute("inResponseTo", inResponseTo != null ? inResponseTo : "");
            return "send-response";
        } catch (Exception e) {
            model.addAttribute("error", "Error regenerating: " + e.getMessage());
            return "send-response";
        }
    }

    // SP-Initiated: Receive AuthnRequest via HTTP-Redirect
    @GetMapping("/sso")
    public String receiveAuthnRequestRedirect(@RequestParam("SAMLRequest") String samlRequest,
                                              @RequestParam(value = "RelayState", required = false) String relayState,
                                              Model model) {
        try {
            SamlRequestParser.ParsedAuthnRequest parsed = requestParser.parseRedirectBinding(samlRequest);
            populateAuthnRequestModel(model, parsed, relayState);
            return "authn-request-view";
        } catch (Exception e) {
            model.addAttribute("error", "Error parsing AuthnRequest: " + e.getMessage());
            return "authn-request-view";
        }
    }

    // SP-Initiated: Receive AuthnRequest via HTTP-POST
    @PostMapping("/sso")
    public String receiveAuthnRequestPost(@RequestParam("SAMLRequest") String samlRequest,
                                          @RequestParam(value = "RelayState", required = false) String relayState,
                                          Model model) {
        try {
            SamlRequestParser.ParsedAuthnRequest parsed = requestParser.parsePostBinding(samlRequest);
            populateAuthnRequestModel(model, parsed, relayState);
            return "authn-request-view";
        } catch (Exception e) {
            model.addAttribute("error", "Error parsing AuthnRequest: " + e.getMessage());
            return "authn-request-view";
        }
    }

    @PostMapping("/respond-to-authn")
    public String respondToAuthnRequest(@RequestParam Long profileId,
                                        @RequestParam String inResponseTo,
                                        @RequestParam(required = false) String relayState,
                                        Model model) {
        try {
            SamlProfile profile = profileRepository.findById(profileId)
                    .orElseThrow(() -> new IllegalArgumentException("Profile not found"));

            String xml = responseBuilder.buildResponseXml(profile, inResponseTo);
            String base64 = SamlEncodingUtils.base64Encode(xml);

            model.addAttribute("profile", profile);
            model.addAttribute("samlResponseXml", xml);
            model.addAttribute("samlResponseBase64", base64);
            model.addAttribute("acsUrl", profile.getAcsUrl());
            model.addAttribute("relayState", relayState);
            model.addAttribute("flowType", "SP-Initiated");
            model.addAttribute("inResponseTo", inResponseTo);
            return "send-response";
        } catch (Exception e) {
            model.addAttribute("error", "Error building response: " + e.getMessage());
            return "send-response";
        }
    }

    private void populateAuthnRequestModel(Model model, SamlRequestParser.ParsedAuthnRequest parsed, String relayState) {
        model.addAttribute("parsedRequest", parsed);
        model.addAttribute("requestDetails", parsed.toMap());
        model.addAttribute("rawXml", parsed.getRawXml());
        model.addAttribute("relayState", relayState);
        model.addAttribute("profiles", profileRepository.findAllByOrderByUpdatedAtDesc());

        // Log the incoming request
        SamlExchangeLog log = new SamlExchangeLog();
        log.setFlowType("SP-Initiated (AuthnRequest received)");
        log.setSamlRequest(parsed.getRawXml());
        log.setSpEntityId(parsed.getIssuer());
        log.setAcsUrl(parsed.getAssertionConsumerServiceURL());
        log.setRelayState(relayState);
        logRepository.save(log);
    }
}
