For an enterprise application, I would build `server.ts` as an **Authentication Gateway** rather than a simple Express backend. The gateway owns the entire authentication lifecycle while React remains unaware of the identity provider.

```
                       +----------------+
                       | React          |
                       |----------------|
                       | /api/auth/me   |
                       | /api/...       |
                       +-------+--------+
                               |
                     HTTP Cookie
                               |
                     +---------v---------+
                     | server.ts         |
                     | Authentication    |
                     | Authorization     |
                     | Session           |
                     +---------+---------+
                               |
                  OpenID Connect/OAuth2
                               |
                  +------------v------------+
                  | Keycloak                |
                  | Identity Broker         |
                  +------------+------------+
                               |
               +---------------+----------------+
               |                                |
        Local Users                    Corporate IdP
                                  (Azure AD/Okta/ADFS)
```

---

# Recommended project structure

```
server/

    src/

        app.ts

        config/
            auth.ts

        middleware/
            authenticate.ts
            authorize.ts

        auth/
            login.ts
            callback.ts
            logout.ts
            me.ts

        services/
            KeycloakService.ts
            SessionService.ts

        routes/
            authRoutes.ts
            apiRoutes.ts

        types/
```

---

# Libraries

```bash
npm install \
express \
openid-client \
express-session \
cookie-parser \
passport \
passport-openidconnect \
jsonwebtoken \
helmet \
cors

npm install -D \
typescript \
ts-node \
@types/express \
@types/express-session
```

I recommend **openid-client** instead of Passport because it gives more control and better supports modern OAuth 2.1/OpenID Connect flows.

---

# Keycloak setup

Your Keycloak acts as an Identity Broker.

```
Keycloak

Identity Providers

    Local Users

    Azure AD

    Okta

    ADFS

    Google

    LDAP
```

When the user visits

```
/api/auth/login
```

Keycloak automatically displays

```
Choose login

○ Local Account

○ Corporate Login
```

No React changes are needed.

---

# Environment variables

```text
PORT=4000

KEYCLOAK_URL=https://login.company.com

KEYCLOAK_REALM=stemx

KEYCLOAK_CLIENT_ID=stemx-web

KEYCLOAK_CLIENT_SECRET=xxxxxxxxxxxx

SESSION_SECRET=mysecret

REDIRECT_URI=https://app.company.com/api/auth/callback
```

---

# Initialize OpenID Client

```typescript
import { Issuer } from "openid-client";

const issuer = await Issuer.discover(
    `${process.env.KEYCLOAK_URL}/realms/${process.env.KEYCLOAK_REALM}`
);

export const client = new issuer.Client({

    client_id: process.env.KEYCLOAK_CLIENT_ID!,

    client_secret: process.env.KEYCLOAK_CLIENT_SECRET!,

    redirect_uris: [
        process.env.REDIRECT_URI!
    ],

    response_types: [
        "code"
    ]
});
```

---

# Login endpoint

```typescript
router.get("/login", (req, res) => {

    const verifier =
        generators.codeVerifier();

    const challenge =
        generators.codeChallenge(verifier);

    req.session.codeVerifier = verifier;

    const url = client.authorizationUrl({

        scope:
            "openid profile email",

        code_challenge: challenge,

        code_challenge_method: "S256"

    });

    res.redirect(url);

});
```

Browser:

```
React

↓

GET /api/auth/login

↓

server.ts

↓

Keycloak

↓

Corporate Login

↓

Corporate IdP
```

---

# Callback

```typescript
router.get("/callback", async (req, res) => {

    const params =
        client.callbackParams(req);

    const tokenSet =
        await client.callback(

            process.env.REDIRECT_URI!,

            params,

            {
                code_verifier:
                    req.session.codeVerifier
            }

        );

    const userInfo =
        await client.userinfo(tokenSet);

    req.session.user = {

        id: userInfo.sub,

        username:
            userInfo.preferred_username,

        email:
            userInfo.email,

        roles:
            tokenSet.claims().realm_access.roles
    };

    res.redirect("/");

});
```

---

# Current user

```typescript
router.get("/me", (req, res) => {

    if (!req.session.user) {

        return res
            .status(401)
            .json({
                authenticated: false
            });

    }

    res.json({

        authenticated: true,

        ...req.session.user

    });

});
```

React calls only

```
GET /api/auth/me
```

---

# Logout

```typescript
router.get("/logout", (req, res) => {

    req.session.destroy(() => {

        res.redirect(

            `${KEYCLOAK_URL}/realms/${REALM}` +

            `/protocol/openid-connect/logout`

        );

    });

});
```

---

# Authentication middleware

```typescript
export function authenticate(

    req,
    res,
    next

) {

    if (!req.session.user)

        return res.sendStatus(401);

    next();

}
```

---

# Authorization middleware

```typescript
export function authorize(
    ...roles: string[]
) {

    return (req, res, next) => {

        const user = req.session.user;

        if (!user)

            return res.sendStatus(401);

        const ok = roles.some(role =>
            user.roles.includes(role)
        );

        if (!ok)

            return res.sendStatus(403);

        next();

    };

}
```

Use it like this:

```typescript
router.get(

    "/users",

    authenticate,

    authorize("Admin"),

    controller

);
```

---

# API flow

```
React
      |
      | GET /api/reports
      |
      v
server.ts
      |
Authenticate
      |
Authorize
      |
Cube API
      |
Transform Data
      |
React
```

The browser never communicates directly with Keycloak or the corporate IdP.

---

# Role mapping

Instead of exposing raw Keycloak roles to your application, map them to application-specific permissions. For example:

| Keycloak Role     | Application Permission |
| ----------------- | ---------------------- |
| `realm-admin`     | `Admin`                |
| `finance-user`    | `Report.View`          |
| `finance-manager` | `Report.Edit`          |
| `student`         | `Mission.Execute`      |

Your authorization middleware can then check permissions such as `Report.View` or `Mission.Execute`, making your application independent of the identity provider's role names.

---

## Since you're already using a React → `server.ts` → Cube API architecture

A good fit for your stack is:

* **React**: UI, routing, and auth state only.
* **server.ts (Express + TypeScript)**:

  * OpenID Connect client to Keycloak using Authorization Code Flow with PKCE.
  * Secure session management with HTTP-only cookies.
  * Role-to-permission mapping.
  * RBAC, dataset-level, column-level, and feature-level authorization.
  * Audit logging for every protected API request.
  * Proxy and transform requests to Cube API.
* **Keycloak**: Identity broker for local users and corporate IdPs.
* **Cube API**: Data access layer protected by the middleware.
* **Database**: Accessed only through Cube or other backend services, never directly from React.

This design cleanly separates authentication, authorization, business logic, and data access, making it easier to maintain and extend as you add additional identity providers or authorization policies.




## backend:

If you keep authentication modules separate, your `server.ts` becomes much cleaner. Since you are not using Express, your `server.ts` acts as the **main HTTP dispatcher/router** and imports handlers from modules.

A good structure for your current architecture:

```
server/
│
├── server.ts                  <-- Main HTTP server
│
├── auth/
│   ├── login.ts               <-- /api/auth/login
│   ├── callback.ts            <-- /api/auth/callback
│   ├── logout.ts              <-- /api/auth/logout
│   ├── me.ts                  <-- /api/auth/me
│   ├── session.ts             <-- Session management
│   ├── keycloak.ts            <-- Keycloak OIDC functions
│   └── types.ts
│
├── middleware/
│   ├── authMiddleware.ts
│   └── authorization.ts
│
├── config/
│   └── config.ts
│
└── services/
    ├── audit.ts
    └── cube.ts
```

---

## server.ts

Your main file becomes only responsible for:

1. Creating HTTP server
2. Parsing URL
3. Routing requests
4. Calling modules

```typescript
// server.ts

import http from "http";
import { URL } from "url";

import { authLogin } from "./auth/login";
import { authCallback } from "./auth/callback";
import { authLogout } from "./auth/logout";
import { authMe } from "./auth/me";


const PORT = 8443;



const server = http.createServer(
    
    async (
        req:http.IncomingMessage,
        res:http.ServerResponse
    )=>{


        try {


            const url =
                new URL(
                    req.url!,
                    "http://localhost"
                );


            console.log(
                req.method,
                url.pathname
            );



            /*
             * Authentication APIs
             */


            if(
                req.method === "GET" &&
                url.pathname === "/api/auth/login"
            ){

                await authLogin(
                    req,
                    res
                );

                return;

            }



            if(
                req.method === "GET" &&
                url.pathname === "/api/auth/callback"
            ){

                await authCallback(
                    req,
                    res
                );

                return;

            }



            if(
                req.method === "GET" &&
                url.pathname === "/api/auth/logout"
            ){

                await authLogout(
                    req,
                    res
                );

                return;

            }



            if(
                req.method === "GET" &&
                url.pathname === "/api/auth/me"
            ){

                await authMe(
                    req,
                    res
                );

                return;

            }



            /*
             * Other APIs
             */

            if(
                url.pathname.startsWith("/api/cube")
            ){

                // future:
                // authenticate
                // authorize
                // call Cube API

                res.writeHead(200);

                res.end(
                    "Cube API"
                );

                return;

            }



            /*
             * React static files
             */

            res.writeHead(404);

            res.end(
                "Not Found"
            );


        }
        catch(error){


            console.error(error);


            res.writeHead(
                500,
                {
                    "Content-Type":
                    "application/json"
                }
            );


            res.end(
                JSON.stringify({
                    error:
                    "Internal Server Error"
                })
            );

        }


    }
);



server.listen(
    PORT,
    ()=>{

        console.log(
            `Server running ${PORT}`
        );

    }
);
```

---

# login.ts

Example:

```typescript
// auth/login.ts

import http from "http";
import crypto from "crypto";

import {
    sessions
} from "./session";

import {
    createAuthorizationURL
} from "./keycloak";


export async function authLogin(

    req:http.IncomingMessage,

    res:http.ServerResponse

){


    const sessionId =
        crypto.randomUUID();


    const login =
        createAuthorizationURL(
            sessionId
        );


    res.writeHead(
        302,
        {

            Location:
                login.url,


            "Set-Cookie":
            `SESSIONID=${sessionId};
             HttpOnly;
             Secure;
             SameSite=Lax;
             Path=/`

        }
    );


    sessions.set(
        sessionId,
        {

            state:
            login.state,

            verifier:
            login.verifier

        }
    );


    res.end();

}
```

---

# callback.ts

```typescript
// auth/callback.ts

import http from "http";


export async function authCallback(

    req:http.IncomingMessage,

    res:http.ServerResponse

){


    /*
       1. Read code
       2. Validate state
       3. Exchange token
       4. Validate JWT
       5. Create user session
    */



    res.writeHead(
        302,
        {
            Location:"/"
        }
    );


    res.end();

}
```

---

# logout.ts

```typescript
// auth/logout.ts

import http from "http";


export async function authLogout(

    req:http.IncomingMessage,

    res:http.ServerResponse

){


    /*
       1. Remove session
       2. Clear cookie
       3. Logout from Keycloak
    */


    res.writeHead(
        302,
        {
            Location:"/"
        }
    );


    res.end();

}
```

---

# me.ts

```typescript
// auth/me.ts

import http from "http";


export async function authMe(

    req:http.IncomingMessage,

    res:http.ServerResponse

){


    const user = {

        username:
            "john",

        roles:[
            "Admin"
        ]

    };


    res.writeHead(
        200,
        {
            "Content-Type":
            "application/json"
        }
    );


    res.end(
        JSON.stringify({

            authenticated:true,

            user

        })
    );

}
```

---

## Why this structure fits your project

For your React → `server.ts` → Cube API architecture, this separation will scale:

```
server.ts
 |
 +-- auth/
 |     |
 |     +-- login.ts
 |     +-- callback.ts
 |     +-- logout.ts
 |     +-- me.ts
 |
 +-- middleware/
 |     |
 |     +-- authenticate.ts
 |     +-- authorize.ts
 |
 +-- api/
 |     |
 |     +-- cube.ts
 |     +-- student.ts
 |     +-- mission.ts
 |
 +-- services/
       |
       +-- keycloak.ts
       +-- audit.ts
```

Your `server.ts` stays small, while authentication, authorization, Cube API integration, audit logging, and business APIs can evolve independently. This is much closer to a production enterprise Node/TypeScript gateway design than putting everything into one file.
