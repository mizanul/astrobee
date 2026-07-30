Keycloak is a good fit for this architecture because it separates **authentication** from your applications while allowing you to broker identities from your company's existing Identity Provider (IdP). Your applications never need to know how users are authenticated—they only trust Keycloak.


## High-Level Architecture

```text
                      +----------------------+
                      | Central User DB      |
                      +----------+-----------+
                                 |
                                 |
                      +----------v-----------+
                      | Corporate IdP        |
                      | (Azure AD, ADFS,     |
                      | Ping, Okta, etc.)    |
                      +----------+-----------+
                                 |
                        SAML/OIDC Federation
                                 |
                    +------------v------------+
                    |      Keycloak           |
                    | Identity Broker         |
                    | MFA                     |
                    | Token Exchange          |
                    | User Federation         |
                    +------------+------------+
                                 |
          --------------------------------------------------
          |                  |                |             |
          |                  |                |             |
  Restricted Env     Non-Restricted    Mobile App     API Gateway
      React              React
          |                  |                |
          +------------------+----------------+
                             |
                     OAuth2/OIDC
                             |
                     JWT Access Token
                             |
                      API Gateway
                             |
        -----------------------------------------
        |               |            |           |
     Flask API      Node API     Java API    Python API
                             |
                     Microservices
```

---

# Recommended Design

## 1. Corporate IdP remains the Source of Truth

Do **not** synchronize passwords into Keycloak.

Instead:

* Corporate AD
* Azure AD
* Okta
* PingFederate
* ADFS

authenticate users.

Keycloak acts only as

* Identity Broker
* Token Service
* Authorization Server

This makes Keycloak stateless regarding authentication.

---

## 2. One Keycloak Cluster

Don't deploy multiple Keycloak instances unless required.

```
keycloak.company.com
```

Example realms:

```
Company-Realm
```

Within the realm:

```
Clients

student-portal
admin-portal
analytics
mobile
api-gateway
```

---

## 3. Separate Clients per Environment

Example:

```
restricted-ui

restricted-api

public-ui

public-api

mobile

partner-api
```

Each client gets:

```
Client ID

Client Secret

Scopes

Roles

Audience

Token Lifetime
```

---

# 4. Token Flow

```
User

↓

Corporate Login

↓

Corporate IdP

↓

Keycloak

↓

Access Token (5 minutes)

Refresh Token (30 minutes)

↓

React

↓

API Gateway

↓

Backend APIs
```

Only the access token is sent to APIs.

---

# 5. Short-Lived Access Tokens

Example:

```
Access Token

5 minutes
```

Refresh Token

```
30 minutes
```

Offline Token

```
Optional
```

Advantages

* Stolen token expires quickly.
* Lower replay risk.
* Better Zero Trust posture.

---

# 6. Different Security Levels

Suppose you have

```
Restricted

Non-Restricted
```

Do **not** create separate user databases.

Instead use

```
Authentication Level

or

Client Policies
```

Example

Non-restricted

```
Password

5-minute token
```

Restricted

```
Password

+

MFA

+

Device Trust

+

3-minute token
```

---

# 7. Audience Restriction

Never let every API accept every token.

Example

```
aud = restricted-api
```

Only

```
restricted-api
```

accepts that token.

Your public API rejects it.

Likewise

```
aud = public-api
```

cannot call

```
restricted-api
```

This prevents token confusion attacks.

---

# 8. API Gateway

A gateway should validate JWTs before requests reach your services.

```
React

↓

NGINX

↓

API Gateway

↓

Flask

↓

Node

↓

Python

↓

ROS Service
```

The gateway validates:

* Signature
* Expiration
* Audience
* Issuer
* Required scopes

If invalid, it returns 401/403 immediately.

---

# 9. JWT Claims

Example

```json
{
  "sub": "12345",

  "preferred_username": "mizan",

  "department": "Engineering",

  "roles": [
      "student",
      "researcher"
  ],

  "scope": "openid profile api",

  "aud": "restricted-api",

  "iss": "https://keycloak.company.com",

  "exp": 1753873200
}
```

---

# 10. Backend Authorization

Don't only check if the user is logged in.

Check

```
Role

Department

Audience

Scope

Clearance
```

Example

```
Student

↓

Can submit robot code

Cannot approve deployments

Administrator

↓

Can deploy

Researcher

↓

Can access simulation

Guest

↓

Read only
```

---

# 11. Token Exchange (Optional)

If one service needs to call another on behalf of the user, use OAuth 2.0 Token Exchange rather than forwarding the original access token everywhere. This lets each downstream service receive a token with an audience and scopes appropriate for that service, limiting blast radius if a token is compromised.

---

# 12. Multi-Environment Strategy

For environments such as Dev, Test, Staging, and Production, use separate realms or separate Keycloak deployments so identities, clients, and signing keys are isolated. Within a production environment, model "restricted" and "non-restricted" as separate clients, scopes, roles, and authentication policies rather than separate realms unless there is a hard regulatory requirement for complete isolation.

---

# 13. High Availability

For production:

```
                Load Balancer
                      |
         -------------------------
         |                       |
    Keycloak #1            Keycloak #2
         |                       |
         -----------+-------------
                     |
                 PostgreSQL
```

Use:

* PostgreSQL for Keycloak's database
* Multiple Keycloak instances behind a load balancer
* Redis or Infinispan if needed for distributed caching (depending on your Keycloak version and deployment model)
* Kubernetes or another orchestrator if you're already running containerized workloads

---

# 14. Your Existing Technology Stack

Given your stack (React frontend, Flask and Node.js services, NGINX, and multiple backend services), a practical request flow would look like:

```text
Corporate IdP
        │
        ▼
    Keycloak
        │
OIDC Authorization Code + PKCE
        │
        ▼
   React SPA
        │
Bearer Access Token
        ▼
NGINX / API Gateway
        │
JWT Validation
        │
 ┌──────┴───────────────┐
 ▼                      ▼
Flask APIs         Node.js APIs
        │
        ▼
   Internal Services
```

In this design:

* The React SPA uses the Authorization Code Flow with PKCE.
* Only Keycloak communicates with the corporate IdP.
* The API gateway validates JWTs using Keycloak's JWKS endpoint and enforces issuer, audience, and scope checks before forwarding requests.
* Backend services perform fine-grained authorization (roles, scopes, resource ownership) but do not need to revalidate token signatures if the gateway is your trusted enforcement point. For defense in depth, many organizations also validate JWTs in each service.

This architecture gives a centralized identity management, consistent JWT issuance, support for multiple security levels, and a clean separation between authentication, authorization, and application logic. It also scales well if you later add mobile apps, partner portals, or additional microservices.
