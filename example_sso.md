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




## Implementation

A clean enterprise architecture is to keep **React responsible only for UI and authentication state**, while **server.js** owns the authentication flow with Keycloak and the corporate IdP.

The flow should look like this:

```
+-------------------+
| React             |
|-------------------|
| ThemeProvider     |
| AuthProvider      |
| AppLayout         |
| AppContent        |
+---------+---------+
          |
          | REST
          |
+---------v------------------+
| server.js (Express)        |
|----------------------------|
| Passport/OpenID Connect    |
| Session/JWT                |
| Authorization              |
+---------+------------------+
          |
          |
    +-----+------+
    |            |
+---v----+   +---v----------------+
|Keycloak|   |Corporate IdP       |
|Broker  |   |Azure AD/Okta/ADFS  |
+--------+   +--------------------+
```

This keeps all OpenID Connect secrets away from React.

---

# Folder structure

```
src/
    App.tsx
    index.tsx

    auth/
        AuthContext.tsx
        AuthProvider.tsx
        PrivateRoute.tsx
        types.ts
        authApi.ts

    components/
        AppLayout.tsx
        AppContent.tsx
```

---

# Auth Types

**auth/types.ts**

```typescript
export interface User {
    id: string;
    username: string;
    fullName: string;
    email: string;

    roles: string[];

    authenticated: boolean;
}

export interface AuthContextType {

    user: User | null;

    loading: boolean;

    login: () => void;

    logout: () => void;

    refresh: () => Promise<void>;

    hasRole: (role: string) => boolean;
}
```

---

# API

**auth/authApi.ts**

```typescript
export async function getCurrentUser() {

    const response = await fetch("/api/auth/me", {
        credentials: "include"
    });

    if (response.status === 401)
        return null;

    return await response.json();
}
```

---

# Auth Context

**AuthContext.tsx**

```typescript
import { createContext } from "react";
import { AuthContextType } from "./types";

export const AuthContext = createContext<AuthContextType>(
    {} as AuthContextType
);
```

---

# Auth Provider

**AuthProvider.tsx**

```typescript
import React, {
    useState,
    useEffect,
    useCallback
} from "react";

import { AuthContext } from "./AuthContext";
import { User } from "./types";
import { getCurrentUser } from "./authApi";

interface Props {
    children: React.ReactNode;
}

const LOGIN_URL = "/api/auth/login";
const LOGOUT_URL = "/api/auth/logout";

export const AuthProvider: React.FC<Props> = ({ children }) => {

    const [user, setUser] = useState<User | null>(null);

    const [loading, setLoading] = useState(true);

    const refresh = useCallback(async () => {

        setLoading(true);

        const me = await getCurrentUser();

        setUser(me);

        setLoading(false);

    }, []);

    useEffect(() => {
        refresh();
    }, [refresh]);

    const login = () => {

        window.location.href = LOGIN_URL;

    };

    const logout = () => {

        window.location.href = LOGOUT_URL;

    };

    const hasRole = (role: string) => {

        return user?.roles.includes(role) ?? false;

    };

    return (

        <AuthContext.Provider
            value={{
                user,
                loading,
                login,
                logout,
                refresh,
                hasRole
            }}
        >

            {children}

        </AuthContext.Provider>

    );

};
```

---

# Hook

```typescript
import { useContext } from "react";
import { AuthContext } from "./AuthContext";

export const useAuth = () => useContext(AuthContext);
```

---

# Private Route

```typescript
import React from "react";
import { useAuth } from "./useAuth";

interface Props {
    children: React.ReactNode;
}

export const PrivateRoute: React.FC<Props> = ({ children }) => {

    const auth = useAuth();

    if (auth.loading)
        return <div>Loading...</div>;

    if (!auth.user) {

        auth.login();

        return null;
    }

    return <>{children}</>;
};
```

---

# App.tsx

```tsx
import { ThemeProvider } from "./theme";
import { AuthProvider } from "./auth/AuthProvider";
import { PrivateRoute } from "./auth/PrivateRoute";

import AppLayout from "./components/AppLayout";
import AppContent from "./components/AppContent";

const App: React.FC = () => {

    return (

        <ThemeProvider>

            <AuthProvider>

                <PrivateRoute>

                    <AppLayout>

                        <AppContent />

                    </AppLayout>

                </PrivateRoute>

            </AuthProvider>

        </ThemeProvider>

    );

};

export default App;
```

---

# Login Button

```tsx
import { useAuth } from "../auth/useAuth";

export default function LoginButton() {

    const auth = useAuth();

    if (auth.user) {

        return (

            <button onClick={auth.logout}>
                Logout
            </button>

        );

    }

    return (

        <button onClick={auth.login}>
            Login
        </button>

    );

}
```

---

# Server.js API

Your React application never talks directly to Keycloak.

```
GET  /api/auth/login
```

Redirects to

```
Keycloak
```

Keycloak lets the user choose

```
Corporate Login

or

Local Login
```

After login

```
Keycloak
    ↓
server.js callback

/api/auth/callback
```

server.js creates

```
HTTP Only Cookie

or

JWT Cookie
```

React then calls

```
GET /api/auth/me
```

Example response:

```json
{
    "id":"123",
    "username":"john",
    "fullName":"John Smith",
    "email":"john@company.com",
    "roles":[
        "Admin",
        "Report.View"
    ],
    "authenticated":true
}
```

---

# Authorization

Once the authenticated user is available, authorization is straightforward:

```tsx
const auth = useAuth();

if (auth.hasRole("Admin")) {
    return <AdminPage />;
}

return <AccessDenied />;
```

or

```tsx
{auth.hasRole("Report.View") && (
    <ReportMenu />
)}
```

---

## Recommended production architecture

Since you've indicated that your backend middleware (`server.js` in TypeScript) will manage both authentication and authorization, I recommend **not** using the Keycloak JavaScript adapter in React. Instead:

* React is a thin client that only knows whether a user is authenticated and what roles/permissions have been granted.
* `server.js` handles the full OpenID Connect Authorization Code Flow with PKCE, exchanges tokens with Keycloak, validates them, and stores only a secure, HTTP-only session cookie.
* `server.js` exposes endpoints such as `/api/auth/login`, `/api/auth/callback`, `/api/auth/logout`, and `/api/auth/me`.
* Keycloak is configured as an identity broker so users can authenticate with either local Keycloak accounts or a federated corporate IdP (such as Azure AD, Okta, or ADFS). React does not need to know which identity provider the user chose.
* All application APIs go through `server.js`, which performs role and permission checks before accessing downstream services such as your Cube API or databases.

This architecture scales well for enterprise applications because it centralizes security concerns, keeps tokens out of the browser, and makes it easy to support multiple identity providers without changing the React application.

