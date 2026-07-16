Your architecture is a good fit for **Keycloak as a centralized Identity Provider (IdP)**. Since your application consists of:

* **React Frontend**
* **Node.js Backend**
* **Cube API**
* **MS SQL Server**
* All application services running in **Docker containers**
* Multiple isolated environments:

  * Env-1 (Unclassified)
  * Env-2 (Classified)
  * Env-3 (Classified)
  * Env-4 (Classified)
* **One Keycloak instance providing SSO**

I would recommend an architecture based on **OIDC (OpenID Connect)** with **JWT tokens**, **environment-specific clients**, and **centralized RBAC**.

---

# High-Level Architecture

```text
                          +--------------------------------+
                          |        Central Keycloak        |
                          |--------------------------------|
                          | Identity Provider (OIDC)       |
                          | User Federation (LDAP/AD)      |
                          | MFA                            |
                          | SSO Session                    |
                          | RBAC                           |
                          +---------------+----------------+
                                          |
                           HTTPS (OIDC/OAuth2)
                                          |
               -------------------------------------------------
               |                 |               |             |
          Env-1 Client      Env-2 Client   Env-3 Client  Env-4 Client
          React App         React App      React App     React App
```

Each environment has its own application stack.

```text
               +---------------------------------------------+
               |                 ENV-1                       |
               |---------------------------------------------|
               |                                             |
Browser -----> | React SPA                                  |
               |     |                                      |
               |     | JWT                                  |
               |     V                                      |
               | Node.js API                               |
               |     |                                     |
               |     | JWT Verification                    |
               |     |                                     |
               | Cube API                                  |
               |     |                                     |
               | SQL Server                                |
               +---------------------------------------------+
```

Same architecture repeats for Env-2, Env-3, Env-4.

---

# Why Use One Keycloak?

Keycloak keeps:

* Users
* Groups
* Roles
* MFA
* Password Policy
* Identity Providers
* Sessions

Only one place manages authentication.

Applications only verify JWT tokens.

---

# Recommended Keycloak Structure

Instead of multiple realms, use **one realm** unless environments have different security boundaries or administrative domains.

```
Realm
    STEMX365

        Users

        Groups

        Roles

        Clients
            react-env1
            react-env2
            react-env3
            react-env4

            node-env1
            node-env2
            node-env3
            node-env4
```

Each environment has separate clients.

This allows:

* Separate redirect URIs
* Separate secrets
* Independent logout URLs
* Different token lifetimes if necessary

---

# Login Flow

```
User opens

https://env1.company.com
↓
React checks login
↓
Redirect to Keycloak
↓
User authenticates
↓
Keycloak creates SSO session
↓
Returns JWT
↓
React stores Access Token
↓
Calls Node API
```

---

Now the user opens

```
https://env2.company.com
```

React redirects to Keycloak.

Keycloak already has an SSO session.

No login screen.

Immediately returns another token for Env-2.

That's Single Sign-On.

---

# Token Flow

```
Browser

Access Token
Refresh Token
↓
React
↓
Authorization:
Bearer eyJhbGciOi...
↓
Node.js
↓
JWT Validation
↓
Cube API
↓
SQL
```

Node should validate JWTs using Keycloak's public keys (JWKS) rather than calling Keycloak for every request.

---

# Node Authentication

Node never authenticates users.

It only verifies tokens.

Example middleware:

```javascript
Bearer Token
↓
Verify Signature
↓
Verify Issuer
↓
Verify Audience
↓
Verify Expiration
↓
Extract Roles
↓
Authorize
```

---

# Cube API

Cube should also validate JWTs.

```
React
↓
Node
↓
Cube API
↓
Verify JWT
↓
Apply Security Context
↓
SQL
```

Cube can use the JWT claims to implement row-level security.

---

# SQL Server

SQL Server should **not** authenticate users directly.

```
React
↓
Node
↓
Cube
↓
Stored Procedure
↓
SQL
```

Only Node/Cube connect using service accounts.

Never allow browsers to access SQL.

---

# RBAC

Keep authorization in Keycloak.

Example roles:

```
Admin

Analyst

Viewer

Researcher

Instructor
```

Node checks:

```javascript
roles.includes("Admin")
```

---

# Environment Authorization

Users should also receive environment roles.

```
ENV1_USER

ENV2_USER

ENV3_USER

ENV4_USER
```

Example JWT

```json
{
  "preferred_username": "alice",

  "realm_access": {
    "roles": [
      "ENV1_USER",
      "REPORT_VIEWER",
      "ADMIN"
    ]
  }
}
```

Node simply checks

```
if (!roles.includes("ENV1_USER"))
    deny();
```

---

# Dataset Authorization

Store dataset permissions in SQL.

Example

```
User
↓
Keycloak
↓
Role
↓
Node
↓
Permission Service
↓
SQL
```

Table

```
UserRole

RoleDataset

Dataset

Permission
```

Node maps JWT roles to datasets.

---

# Column Authorization

Same idea.

```
Dataset
↓
Allowed Columns
↓
Cube
↓
Visible Columns
```

Cube can dynamically expose only permitted columns.

---

# Feature Authorization

Example:

```
Create Dashboard

Export

Delete Dashboard

Admin

Manage Users
```

Roles

```
REPORT_CREATE

REPORT_DELETE

EXPORT

ADMIN
```

React hides unauthorized UI.

Node enforces authorization.

---

# Docker

Every environment contains:

```
NGINX

React

Node

Cube

Redis (optional)
```

Keycloak is external.

```
Docker Network

React
↓
Node
↓
Cube
↓
SQL
```

---

# Deployment

```
Internet
↓
Load Balancer
↓
NGINX
↓
React
↓
Node
↓
Cube
↓
SQL
```

Keycloak is shared.

```
              Keycloak

                 ▲

                 │

Env1

Env2

Env3

Env4
```

---

# High Availability

Run Keycloak in HA.

```
            Load Balancer

          /                \

Keycloak-1           Keycloak-2

        \            /

       PostgreSQL Cluster
```

---

# Token Validation

Do not call Keycloak on every request.

Instead

```
Node

↓

Download Public Keys

↓

Cache Keys

↓

Verify JWT Offline
```

This scales much better.

---

# Security Best Practices

* Use **Authorization Code Flow with PKCE** for the React SPA.
* Keep access tokens short-lived (e.g., 5–15 minutes) and use refresh tokens where appropriate.
* Terminate TLS at your reverse proxy and use HTTPS end-to-end where feasible.
* Configure CORS carefully so each environment only allows its own frontend origin.
* Store tokens securely (avoid `localStorage` when possible; consider in-memory storage plus silent refresh).
* Use client-specific redirect URIs and logout URIs.
* Enable MFA for privileged users.
* Rotate client secrets for confidential clients (Node backends).

---

# Recommended Overall Architecture

```text
                                 +------------------------------------------------------+
                                 |              Central Keycloak Cluster                 |
                                 |------------------------------------------------------|
                                 | Realm: STEMX365                                      |
                                 | Users / Groups / Roles                               |
                                 | OIDC • SSO • MFA • LDAP/AD Federation                |
                                 +-------------------------+----------------------------+
                                                           |
                                      OIDC Authorization Code + PKCE
                                                           |
      ---------------------------------------------------------------------------------------------------------
      |                           |                            |                            |
+---------------------+  +---------------------+   +---------------------+   +---------------------+
| Env-1 (Unclassified)|  | Env-2 (Classified)  |   | Env-3 (Classified)  |   | Env-4 (Classified)  |
|---------------------|  |---------------------|   |---------------------|   |---------------------|
| React SPA           |  | React SPA           |   | React SPA           |   | React SPA           |
|        │            |  |        │            |   |        │            |   |        │            |
|        ▼            |  |        ▼            |   |        ▼            |   |        ▼            |
| Node.js API         |  | Node.js API         |   | Node.js API         |   | Node.js API         |
|  • JWT Validation   |  |  • JWT Validation   |   |  • JWT Validation   |   |  • JWT Validation   |
|  • RBAC             |  |  • RBAC             |   |  • RBAC             |   |  • RBAC             |
|        │            |  |        │            |   |        │            |   |        │            |
|        ▼            |  |        ▼            |   |        ▼            |   |        ▼            |
| Cube API            |  | Cube API            |   | Cube API            |   | Cube API            |
|  • RLS              |  |  • RLS              |   |  • RLS              |   |  • RLS              |
|        │            |  |        │            |   |        │            |   |        │            |
|        ▼            |  |        ▼            |   |        ▼            |   |        ▼            |
| SQL Server          |  | SQL Server          |   | SQL Server          |   | SQL Server          |
+---------------------+  +---------------------+   +---------------------+   +---------------------+
```

This design provides:

* **Single Sign-On (SSO)** across all environments with one login.
* **Environment-specific clients** and redirect URIs while sharing the same user identity.
* **Offline JWT validation** in Node.js and Cube API for high performance.
* **Centralized RBAC** in Keycloak, with optional dataset-, row-, column-, and feature-level authorization enforced in Node.js/Cube.
* A clear separation between authentication (Keycloak), authorization (Keycloak + application), analytics (Cube), and data storage (SQL Server).

One additional consideration: if your "classified" environments are on isolated networks that cannot communicate with a central identity service, a single live Keycloak instance will not be sufficient. In that case, you would typically deploy a local Keycloak instance in each classified network and use identity federation or scheduled synchronization, rather than direct SSO across security boundaries. If all environments can securely reach the central Keycloak over approved network connections, then the centralized architecture above is appropriate.
