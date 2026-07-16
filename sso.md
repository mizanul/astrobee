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



---

**PKCE (Proof Key for Code Exchange)** is an extension to the OAuth 2.0 Authorization Code Flow that protects public clients—such as **React Single Page Applications (SPAs)** and **mobile apps**—from authorization code interception attacks.

In modern OAuth/OIDC deployments, **PKCE should always be used for React applications**.

---

# Why PKCE is Needed

A React application runs entirely in the user's browser.

Unlike a Node.js backend, it **cannot securely store a client secret** because anyone can inspect the JavaScript code.

Without PKCE, an attacker who somehow intercepts the authorization code could exchange it for an access token.

PKCE prevents this by proving that the application exchanging the authorization code is the same one that initiated the login.

---

# How PKCE Works

### Step 1 – React Generates a Random Secret

Before redirecting the user to Keycloak, React generates a long random string called the **Code Verifier**.

Example:

```text
code_verifier =
m8L4vXz7QdA8Pw4k9MzN2Rb3Yf6Gc...
```

This string is **never sent** to Keycloak at this stage.

---

### Step 2 – React Creates a Code Challenge

React hashes the code verifier using SHA-256.

```text
SHA256(code_verifier)
```

Then Base64URL encodes it.

Example:

```text
code_challenge =

J2dP89cGgRk9s...
```

---

### Step 3 – Redirect to Keycloak

React redirects the browser to Keycloak.

```text
GET

https://keycloak.company.com/

?client_id=react-env1

&response_type=code

&code_challenge=J2dP89...

&code_challenge_method=S256
```

Notice:

React only sends

```text
code_challenge
```

Not

```text
code_verifier
```

---

### Step 4 – User Logs In

User enters

* username
* password
* MFA

Keycloak authenticates them.

---

### Step 5 – Keycloak Returns Authorization Code

```text
https://env1.company.com

?code=ABCD123456
```

This code expires quickly (typically within about a minute).

---

### Step 6 – React Exchanges the Code

React sends:

```http
POST /token

grant_type=authorization_code

code=ABCD123456

code_verifier=m8L4vXz7QdA...
```

Now Keycloak computes

```text
SHA256(code_verifier)
```

If it matches the original challenge

```text
J2dP89...
```

Keycloak issues

* Access Token
* ID Token
* Refresh Token (if configured)

---

# Why This Stops Attackers

Suppose an attacker intercepts

```text
code=ABCD123456
```

Without PKCE they could call

```http
POST /token

code=ABCD123456
```

and obtain an access token.

With PKCE they also need

```text
code_verifier
```

which never traveled through the browser redirect.

Only your React application has it.

So Keycloak rejects the request.

---

# Sequence Diagram

```text
+---------+        +------------+        +-----------+
| Browser |        | Keycloak   |        | Node API  |
+---------+        +------------+        +-----------+

    |
    | Generate Code Verifier
    |
    | Hash -> Code Challenge
    |
    |------------------------------>
    | Authorization Request
    | code_challenge
    |
    |<------------------------------
    | Login Page
    |
    | Username/Password
    |
    |------------------------------>
    |
    |<------------------------------
    | Authorization Code
    |
    |------------------------------>
    | POST /token
    | code
    | code_verifier
    |
    |<------------------------------
    | Access Token
    |
    |------------------------------>
    | Authorization: Bearer JWT
    |
    |<------------------------------
    | API Response
```

---

# Where PKCE Fits in Your Architecture

```
                   Central Keycloak
                         │
          Authorization Code + PKCE
                         │
                  React Frontend
                         │
                  Access Token (JWT)
                         │
                  Node.js Backend
                         │
                    Cube API
                         │
                   SQL Server
```

Only the **React frontend** communicates directly with Keycloak.

Your **Node.js backend** does not participate in the PKCE exchange. Instead, it receives the JWT access token from React and validates it using Keycloak's public signing keys (JWKS).

---

# PKCE vs Client Secret

| React SPA        | Node Backend                              |
| ---------------- | ----------------------------------------- |
| No client secret | Has a client secret (confidential client) |
| Uses PKCE        | Uses client credentials or validates JWTs |
| Runs in browser  | Runs on a trusted server                  |
| Public client    | Confidential client                       |

---

# PKCE in Keycloak

For your React application, configure the Keycloak client as:

* **Client Type:** Public
* **Standard Flow:** Enabled
* **Direct Access Grants:** Disabled (unless specifically needed)
* **PKCE Code Challenge Method:** `S256`
* **Valid Redirect URIs:** Your React application's callback URLs for each environment
* **Web Origins:** Your React application's origins

For your Node.js backend, configure it as a **confidential client** if it needs to call other protected services on its own behalf. If it only validates JWTs from React, it doesn't need to perform user logins itself.

---

# Using PKCE with React

The easiest approach is to use Keycloak's JavaScript adapter or a standards-based OIDC library such as `oidc-client-ts`, both of which automatically implement the Authorization Code Flow with PKCE. You don't need to manually generate the verifier or challenge—they handle the PKCE details securely behind the scenes.

For your multi-environment architecture (Env-1 through Env-4), each React application would use Authorization Code Flow with PKCE against its own Keycloak client, while all clients share the same Keycloak realm and SSO session. That gives users a seamless single sign-on experience without exposing client secrets in the browser.

