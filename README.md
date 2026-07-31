# Astrobee
		 	 	 		
			
				
					
## Running on your own machine
					
You can also run the program on your own machine. 

This provides a procedure to set up the Astrobee simulator. 
					

### Requirements
					
The following requirements are needed to set up a simulation environment on your machine.
					
64-bit processor
8GBRAM(16GBRAM recommended)
Ubuntu16.04(64-bitversion)(http://releases.ubuntu.com/16.04/)

#Note: You will need 4 GBs of RAM to compile the software. If you don't have that much RAM available, please use swap space.

#Note: Preferably install Ubuntu 16.04. At this time we do not officially support any other operating system or Ubuntu version. Experimental #instructions steps for Ubuntu 18 installation are included

#Note: Please ensure you install the 64-bit version of Ubuntu. We do not support running Astrobee Robot Software on 32-bit systems.
					
### Setting up the Astrobee Robot Software
					
					
Run the follwing script in the Ubuntu terminal
								
```
#!/bin/bash

# Author: Mizanul Hoq Chowdhury, MIT

sudo apt-get install build-essential git
export ASTROBEE_WS=$HOME/astrobee

git clone https://github.com/nasa/astrobee.git $ASTROBEE_WS/src
pushd $ASTROBEE_WS/src
git submodule update --init --depth 1 description/media
popd

git submodule update --init --depth 1 submodules/android

pushd $ASTROBEE_WS
cd src/scripts/setup
./add_ros_repository.sh
sudo apt-get update
cd debians
./build_install_debians.sh
cd ../
./install_desktop_packages.sh
sudo rosdep init
rosdep update
popd

export WORKSPACE_PATH=$ASTROBEE_WS
export INSTALL_PATH=$ASTROBEE_WS/install

source /opt/ros/kinetic/setup.sh
cd /src/astrobee
catkin clean --setup-files
ls -la $ASTROBEE_WS/src/cmake
CMAKE_PREFIX_PATH=$CMAKE_PREFIX_PATH:$ASTROBEE_WS/src/cmake
cd $ASTROBEE_WS
./src/scripts/configure.sh -l -F -D -p -T $INSTALL_PATH -w $WORKSPACE_PATH
catkin build --status-rate 0.01


```
					
			
				
### Run Astrobee Simulator				

```
pushd $BUILD_PATH
source devel/setup.bash
popd
roslaunch astrobee sim.launch dds:=false robot:=sim_pub rviz:=true

```




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

The backend middleware (`server.js` in TypeScript) will manage both authentication and authorization, I recommend **not** using the Keycloak JavaScript adapter in React. Instead:

* React is a thin client that only knows whether a user is authenticated and what roles/permissions have been granted.
* `server.js` handles the full OpenID Connect Authorization Code Flow with PKCE, exchanges tokens with Keycloak, validates them, and stores only a secure, HTTP-only session cookie.
* `server.js` exposes endpoints such as `/api/auth/login`, `/api/auth/callback`, `/api/auth/logout`, and `/api/auth/me`.
* Keycloak is configured as an identity broker so users can authenticate with either local Keycloak accounts or a federated corporate IdP (such as Azure AD, Okta, or ADFS). React does not need to know which identity provider the user chose.
* All application APIs go through `server.js`, which performs role and permission checks before accessing downstream services such as your Cube API or databases.

This architecture scales well for enterprise applications because it centralizes security concerns, keeps tokens out of the browser, and makes it easy to support multiple identity providers without changing the React application.

				
			
			 			
