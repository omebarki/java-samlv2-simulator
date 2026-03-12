# Java SAML v2 IdP Simulator

A Spring Boot application that simulates a federated external SAML v2 Identity Provider (IdP).
It generates and sends SAML v2 messages to a federating IdP, with a web interface to configure every aspect of the exchange.

---

## 1. Purpose

When integrating with a SAML v2 federation, the external IdP is often unavailable, slow to configure, or hard to test edge cases against.
This simulator acts as a **stand-in external IdP** that:

- Sends SAML Responses (assertions) to the federating IdP's Assertion Consumer Service (ACS).
- Receives and processes SAML AuthnRequests from the federating IdP.
- Lets the tester control every parameter through a web UI, including raw XML editing.

---

## 2. Supported SAML Flows

### 2.1 IdP-Initiated SSO
The simulator generates an **unsolicited SAML Response** and POSTs it to the federating IdP's ACS URL.
No prior AuthnRequest is needed.

### 2.2 SP-Initiated SSO
The federating IdP sends a SAML **AuthnRequest** to the simulator (via HTTP-Redirect or HTTP-POST).
The simulator parses the request, presents it in the UI, and lets the user craft and send back a SAML Response.

---

## 3. Supported Bindings

| Binding | Direction | Usage |
|---|---|---|
| **HTTP-POST** | Simulator → Federating IdP | Sending SAML Responses to the ACS |
| **HTTP-Redirect** | Federating IdP → Simulator | Receiving AuthnRequests |
| **HTTP-POST** | Federating IdP → Simulator | Receiving AuthnRequests (alternative) |

---

## 4. Web UI Features

The web interface (Thymeleaf server-rendered) provides the following pages and capabilities:

### 4.1 Dashboard
- Overview of saved configuration profiles.
- Quick-launch buttons to initiate an IdP-Initiated SSO flow.
- Log of recent SAML exchanges (request/response pairs with timestamps).

### 4.2 Configuration Profile Management
- **Create / Edit / Delete / Clone** named configuration profiles.
- Each profile stores a complete set of parameters for a target federating IdP.
- Profiles are persisted in an embedded H2 database.

### 4.3 Profile Parameters

#### Identity Provider Settings
| Parameter | Description | Example |
|---|---|---|
| Profile name | Human-readable label | `MyCompany Staging` |
| IdP Entity ID (Issuer) | The entityID the simulator claims | `https://simulator.example.com/idp` |
| SSO Service URL | URL where the simulator receives AuthnRequests | auto-generated from app base URL |

#### Service Provider (Federating IdP) Settings
| Parameter | Description | Example |
|---|---|---|
| SP Entity ID | The entityID of the federating IdP | `https://federation.example.com/sp` |
| ACS URL | Assertion Consumer Service URL to POST responses to | `https://federation.example.com/saml/acs` |
| Audience | Expected audience restriction | same as SP Entity ID |

#### User / Subject Settings
| Parameter | Description | Example |
|---|---|---|
| NameID | Subject identifier | `user@example.com` |
| NameID Format | Format URI | `urn:oasis:names:tc:SAML:1.1:nameid-format:emailAddress` |
| Session Index | Optional session index value | `_session_abc123` |

#### SAML Attributes
- Dynamic list of attribute name/value pairs.
- Support for multi-valued attributes.
- Common presets (e.g., `email`, `givenName`, `surname`, `groups`).

#### Assertion Timing
| Parameter | Description | Default |
|---|---|---|
| Issue Instant | When the assertion is issued | now |
| NotBefore | Assertion not valid before | now - 5 minutes |
| NotOnOrAfter | Assertion expires at | now + 5 minutes |
| Session NotOnOrAfter | Session expiry | now + 8 hours |

#### Signing & Encryption Settings
| Parameter | Description |
|---|---|
| Signing Key + Certificate | Used to sign the SAML Response and/or Assertion |
| Key format | PEM, JKS, or PKCS#12 |
| Sign Response | Toggle: sign the `<Response>` element |
| Sign Assertion | Toggle: sign the `<Assertion>` element |
| Signature Algorithm | RSA-SHA256 (default), RSA-SHA1, RSA-SHA512 |
| Digest Algorithm | SHA-256 (default), SHA-1, SHA-512 |

### 4.4 Advanced: Raw XML Editor
- After the SAML Response XML is generated from the profile parameters, the user can **view and edit the raw XML** before sending.
- Syntax-highlighted XML editor (CodeMirror or similar).
- "Reset to generated" button to discard manual edits.

### 4.5 Send & Inspect
- **Send** button generates a self-submitting HTML form (HTTP-POST binding) that POSTs the Base64-encoded SAML Response to the ACS URL.
- Before sending, display:
  - The generated XML (pretty-printed).
  - The Base64-encoded value.
  - The RelayState (configurable).
- After sending, log the exchange.

### 4.6 Incoming AuthnRequest Viewer
- When the simulator receives an AuthnRequest, it:
  1. Decodes and parses the request.
  2. Displays the parsed details (Issuer, ACS URL, RequestID, etc.).
  3. Pre-fills a response form with values extracted from the request.
  4. Lets the user review/edit and send the response.

---

## 5. Technical Stack

| Component | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.x |
| Build tool | Maven |
| Frontend | Thymeleaf + Bootstrap 5 |
| SAML library | Standard Java XML DOM + Apache Santuario (XML Digital Signatures) |
| PEM/Key parsing | Bouncy Castle |
| XML editor | CodeMirror (via CDN or webjars) |
| Database | H2 (embedded, file-based) |
| Key management | Java KeyStore API (PEM, JKS, PKCS#12) |

---

## 6. Project Structure

```
src/main/java/com/samlv2simulator/
├── SamlSimulatorApplication.java          # Spring Boot entry point
├── config/
│   └── OpenSamlConfig.java                # OpenSAML bootstrap configuration
├── model/
│   ├── SamlProfile.java                   # JPA entity: configuration profile
│   ├── SamlAttribute.java                 # JPA entity: attribute name/value
│   └── SamlExchangeLog.java              # JPA entity: logged exchanges
├── repository/
│   ├── SamlProfileRepository.java         # Spring Data JPA
│   └── SamlExchangeLogRepository.java
├── service/
│   ├── SamlResponseBuilder.java           # Builds SAML Response XML using OpenSAML
│   ├── SamlRequestParser.java             # Parses incoming AuthnRequests
│   ├── SigningService.java                # Signs XML with configurable algorithms
│   └── KeyStoreService.java              # Loads keys from PEM / JKS / PKCS#12
├── controller/
│   ├── DashboardController.java           # Dashboard page
│   ├── ProfileController.java             # CRUD for configuration profiles
│   ├── SsoController.java                 # IdP-Initiated and SP-Initiated SSO endpoints
│   └── AuthnRequestController.java        # Receives and displays AuthnRequests
└── util/
    └── SamlEncodingUtils.java             # Base64, deflate, URL encoding utilities

src/main/resources/
├── application.yml                        # Spring Boot configuration
├── templates/
│   ├── layout.html                        # Thymeleaf layout (header, nav, footer)
│   ├── dashboard.html
│   ├── profile-form.html
│   ├── profile-list.html
│   ├── send-response.html                 # Review XML + send
│   ├── authn-request-view.html            # Incoming AuthnRequest viewer
│   └── post-form.html                     # Auto-submitting POST form (hidden)
└── static/
    ├── css/
    └── js/
```

---

## 7. API / Endpoints

| Method | Path | Description |
|---|---|---|
| GET | `/` | Dashboard |
| GET | `/profiles` | List all profiles |
| GET | `/profiles/new` | New profile form |
| POST | `/profiles` | Save profile |
| GET | `/profiles/{id}/edit` | Edit profile form |
| PUT | `/profiles/{id}` | Update profile |
| DELETE | `/profiles/{id}` | Delete profile |
| GET | `/profiles/{id}/clone` | Clone a profile |
| GET | `/sso/idp-init/{profileId}` | Prepare IdP-Initiated SSO (shows review page) |
| POST | `/sso/send` | Send SAML Response to ACS |
| GET | `/sso/sso` | Receive AuthnRequest (HTTP-Redirect) |
| POST | `/sso/sso` | Receive AuthnRequest (HTTP-POST) |
| GET | `/logs` | View exchange logs |

---

## 8. Key Workflows

### 8.1 IdP-Initiated SSO
```
User → [Dashboard: click "Send SSO"]
     → [Review page: generated XML, edit if needed]
     → [Click "Send"]
     → [Simulator POSTs SAML Response to federating IdP ACS URL]
     → [Exchange logged]
```

### 8.2 SP-Initiated SSO
```
Federating IdP → [Sends AuthnRequest to simulator /sso/sso]
               → [Simulator parses request, shows in UI]
               → [User reviews, adjusts parameters]
               → [Click "Send Response"]
               → [Simulator POSTs SAML Response back to ACS]
               → [Exchange logged]
```

---

## 9. Configuration (application.yml)

```yaml
server:
  port: 8080

spring:
  datasource:
    url: jdbc:h2:file:./data/saml-simulator
    driver-class-name: org.h2.Driver
  h2:
    console:
      enabled: true
  jpa:
    hibernate:
      ddl-auto: update

saml:
  simulator:
    base-url: http://localhost:8080
    default-entity-id: https://simulator.example.com/idp
```

---

## 10. Build & Run

```bash
# Build
mvn clean package

# Run
java -jar target/saml-simulator.jar

# Or with Maven
mvn spring-boot:run
```

The web UI is available at `http://localhost:8080`.

---

## 11. Security Considerations

This application is a **development/testing tool** and should NOT be exposed to production networks.

- The H2 console is enabled by default for debugging.
- Private keys are stored in the embedded database — suitable for test keys only.
- No authentication is required on the simulator UI itself.
- CSRF protection is disabled on endpoints that receive SAML messages (POST binding).
