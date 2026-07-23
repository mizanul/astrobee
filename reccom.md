Your architecture is clean and follows a modern analytics stack. The main strengths are:

* Clear separation of UI, API, analytics engine, and database.
* Development tools (pgAdmin, Metabase, Cube Playground) are optional through Docker profiles.
* Cube API and Refresh Worker are separated, which is the recommended production architecture.
* Database initialization (seed) is isolated from the application.

However, there are several improvements I would recommend before considering this production-ready.

---

# README.md

# AVOID Architecture

## Overview

AVOID is a containerized analytics platform built using Docker Compose.

The platform consists of five major layers:

```
                    +----------------------+
                    |      Web UI          |
                    |   React / Nginx      |
                    +----------+-----------+
                               |
                               v
                    +----------------------+
                    |     Backend API      |
                    |   Authentication     |
                    | Business Logic       |
                    +----------+-----------+
                               |
                 Cube JWT/API  |
                               v
                    +----------------------+
                    |      Cube API        |
                    | Semantic Layer       |
                    | SQL Generation       |
                    +----------+-----------+
                               |
                Refresh Worker |
                               |
                     Cube Store Cache
                               |
                               v
                    +----------------------+
                    |    PostgreSQL        |
                    | Operational Database |
                    +----------------------+
```

---

# Components

## 1. PostgreSQL

Stores all application data.

Responsibilities

* Primary relational database
* Stores business data
* Source for Cube semantic layer

Persistent volume

```
postgres_data
```

---

## 2. Cube API

Cube provides the semantic layer between applications and PostgreSQL.

Responsibilities

* SQL generation
* Metrics
* Dimensions
* Authorization
* Query caching
* REST API

The backend communicates with Cube instead of querying PostgreSQL directly.

```
Backend
    ↓
Cube API
    ↓
PostgreSQL
```

---

## 3. Cube Refresh Worker

Runs asynchronous refresh jobs.

Responsibilities

* Build pre-aggregations
* Cache warming
* Background refresh
* Scheduled aggregation

Separating this worker prevents analytical queries from blocking API requests.

---

## 4. Cube Store

High-performance storage for Cube pre-aggregations.

Responsibilities

* Cache
* Aggregated datasets
* Query acceleration

Persistent volume

```
cubestore_data
```

---

## 5. Backend

Implements application business logic.

Responsibilities

* Authentication
* Authorization
* User management
* API gateway
* Cube token generation
* REST endpoints

The backend never connects directly to PostgreSQL.

Instead

```
Browser
      ↓
Backend
      ↓
Cube API
      ↓
PostgreSQL
```

---

## 6. Web

React application served by Nginx.

Responsibilities

* Dashboard
* Authentication UI
* Visualization
* User interaction

---

## Development Tools

The following services are optional and enabled through Docker Compose profiles.

### pgAdmin

Database administration.

```
docker compose --profile tools up pgadmin
```

---

### Metabase

Self-service BI.

Can be connected to PostgreSQL or Cube.

---

### Cube Playground

Cube schema development.

Useful for

* Testing cubes
* SQL inspection
* Performance tuning

---

## Seed Generator

Optional profile

```
docker compose --profile seed up
```

Generates synthetic data for testing.

Supports

* Variable dataset size
* Time ranges
* Active production lines
* Message density

---

# Network

All services communicate using an internal Docker bridge network.

```
AVOID-network
```

Only these ports are exposed externally.

| Service         | Port         |
| --------------- | ------------ |
| Web             | 8080         |
| Backend         | 3001         |
| PostgreSQL      | 5432         |
| pgAdmin         | configurable |
| Metabase        | configurable |
| Cube Playground | 4000         |

---

# Volumes

Persistent data

```
postgres_data
metabase_data
pgadmin_data
cubestore_data
```

Application containers remain stateless.

---

# Request Flow

```
Browser

      │

      ▼

React UI

      │

      ▼

Backend API

      │

      ▼

Cube API

      │

      ▼

Cube SQL Generator

      │

      ▼

PostgreSQL

      │

      ▼

Results

      │

      ▼

Cube Cache

      │

      ▼

Backend

      │

      ▼

Browser
```

---

# Authentication Flow

```
User

↓

Backend Login

↓

JWT

↓

Backend

↓

Cube Token

↓

Cube API

↓

Authorized SQL
```

---

# Deployment Profiles

Normal

```
docker compose up
```

Development

```
docker compose --profile tools up
```

Seed data

```
docker compose --profile seed up
```

---

# Recommended Improvements

## 1. Add Nginx Reverse Proxy

Instead of exposing every service directly.

```
Internet
     │
     ▼
 Nginx
 ├── /
 ├── /api
 ├── /cube
 └── /auth
```

Advantages

* TLS termination
* Single public port (443)
* Rate limiting
* Compression
* Security headers
* Easier authentication

---

## 2. Do NOT expose PostgreSQL

Instead of

```yaml
ports:
  - "5432:5432"
```

Use

```yaml
expose:
  - "5432"
```

Only pgAdmin and internal services should access PostgreSQL.

---

## 3. Backend should be the only public API

Instead of

```
Browser
      │
      ├── Cube
      └── Backend
```

Use

```
Browser
      │
Backend
      │
Cube
```

This hides Cube and centralizes authentication, auditing, and API versioning.

---

## 4. Add Redis

Currently you use

```
memory
```

for

```
CUBEJS_CACHE_AND_QUEUE_DRIVER
```

In production use

```
Redis
```

Benefits

* Shared cache across instances
* Distributed queues
* Horizontal scaling
* Faster refresh jobs

---

## 5. Add Healthchecks

Backend

Web

Cube API

Cube Worker

Currently only PostgreSQL has one.

---

## 6. Add Logging

Use

```
Loki
Grafana
Promtail
```

or

```
ELK
```

Centralized logs make troubleshooting much easier.

---

## 7. Monitoring

Add

* Prometheus
* Grafana
* cAdvisor
* Node Exporter

Monitor:

* CPU
* Memory
* Container health
* Query latency
* Cube refresh duration
* PostgreSQL performance

---

## 8. Secrets

Avoid storing secrets directly in `.env` for production. Consider Docker Secrets, HashiCorp Vault, or your cloud provider's secret manager to better protect credentials and API keys.

---

## 9. Container Security

Run containers as non-root users where possible, make the root filesystem read-only for stateless services, drop unnecessary Linux capabilities, and set memory and CPU limits in the Compose file.

---

## 10. Production Architecture

For a production deployment, I'd recommend the following overall layout:

```
                 Internet
                     │
                     ▼
             Nginx / Traefik
                     │
          ┌──────────┴──────────┐
          │                     │
          ▼                     ▼
      React Web           Backend API
                                │
                                ▼
                           Cube API
                        ┌───────────┐
                        │           │
                  Redis Cache   Cube Worker
                        │           │
                        └─────┬─────┘
                              ▼
                         Cube Store
                              │
                              ▼
                         PostgreSQL
```

=================================



Adding **Keycloak** is a very good architectural choice. It centralizes authentication, supports Single Sign-On (SSO), and enables federation with enterprise identity providers (Microsoft Entra ID, Google, GitHub, LDAP, Active Directory, SAML, etc.). Your backend no longer needs to manage passwords or issue its own login sessions.

I would update the architecture as follows.

# Updated Architecture

```text
                           Internet
                               │
                               ▼
                      Nginx / Reverse Proxy
                               │
             ┌─────────────────┴─────────────────┐
             │                                   │
             ▼                                   ▼
        React Web                          Keycloak
             │                          Identity Provider
             │                     (OIDC / OAuth2 / SAML)
             │                                   │
             │                                   ▼
             │                     External Identity Providers
             │                ┌─────────────┬─────────────┐
             │                │             │             │
             │                ▼             ▼             ▼
             │          Microsoft Entra   Google      LDAP/AD
             │             GitHub         Okta        SAML IdP
             │
             │ Access Token (JWT)
             ▼
        Backend API
             │
      Validate JWT
             │
             ▼
       Generate Cube JWT
             │
             ▼
         Cube API
        Semantic Layer
             │
      ┌──────┴────────┐
      │               │
      ▼               ▼
Cube Refresh      Redis Cache
   Worker         & Queue
      │               │
      └──────┬────────┘
             ▼
        Cube Store
             │
             ▼
        PostgreSQL
```

---

# Authentication Flow

```text
User

 │

 ▼

React Application

 │

 ▼

Redirect to Keycloak

 │

 ▼

Login Screen

 │

 ▼

Federated Identity Provider
(Google / Microsoft / LDAP / SAML)

 │

 ▼

Keycloak

 │

 ▼

OIDC Access Token (JWT)

 │

 ▼

React

 │

 ▼

Backend API

 │

Verify Token

 │

Map Roles

 │

Generate Cube Token

 │

▼

Cube API

 │

▼

PostgreSQL
```

---

# Responsibilities

## Keycloak

Authentication server.

Responsibilities

* User authentication
* Single Sign-On (SSO)
* OAuth2
* OpenID Connect (OIDC)
* SAML
* Multi-factor authentication (MFA)
* Password policies
* User management
* Identity federation
* Token issuance
* Refresh token management
* Role and group management

Keycloak becomes the **single source of identity** for the platform.

---

## React Web

Responsibilities

* Redirect users to Keycloak for login.
* Store access and refresh tokens securely (preferably using Authorization Code Flow with PKCE).
* Attach the access token to every backend API request.
* Never communicate directly with PostgreSQL or Cube.

---

## Backend API

Responsibilities

* Validate Keycloak JWTs using Keycloak's public keys (JWKS).
* Map Keycloak roles/groups to application permissions.
* Apply business authorization (RBAC/ABAC).
* Generate short-lived Cube JWTs for the authenticated user.
* Expose REST APIs to the frontend.

The backend should **not** authenticate users itself or store passwords.

---

## Cube API

Responsibilities

* Accept only trusted Cube JWTs issued by the backend.
* Enforce row-level security based on claims.
* Generate SQL against PostgreSQL.
* Cache analytical queries.

---

# Token Flow

```text
+------------------+
|    Keycloak      |
| Access JWT       |
+------------------+
          │
          ▼
+------------------+
| Backend API      |
| Verify JWT       |
| Authorize User   |
+------------------+
          │
          ▼
+------------------+
| Cube JWT         |
| Short-lived      |
+------------------+
          │
          ▼
+------------------+
| Cube API         |
+------------------+
```

This separation is important because your Cube API secret remains internal, and the browser never receives it.

---

# Federated Login

Keycloak supports integration with multiple external identity providers, allowing users to authenticate with existing organizational or social accounts.

Examples include:

* Microsoft Entra ID (Azure AD)
* Google Workspace
* GitHub
* LDAP / Active Directory
* SAML 2.0 Identity Providers
* Okta
* Auth0

This enables organizations to use their existing credentials without creating separate accounts in your application.

---

# Role-Based Authorization

A recommended role hierarchy is:

```text
Realm
 ├── Super Admin
 ├── Administrator
 ├── Analyst
 ├── Data Engineer
 ├── Manager
 ├── Viewer
 └── Guest
```

Your backend can translate these roles into application-specific permissions and Cube security context for row-level and feature-level authorization.

---

# Production Deployment

In production, only the reverse proxy should be exposed to the Internet. Internal services, including Keycloak, Backend, Cube API, Redis, Cube Store, and PostgreSQL, should communicate over the private Docker network.

```text
                    Internet
                        │
                        ▼
                 Nginx (HTTPS)
                        │
     ┌──────────────────┼──────────────────┐
     │                  │                  │
     ▼                  ▼                  ▼
 React Web         Backend API         Keycloak
                          │
                          ▼
                    Cube API
                          │
                ┌─────────┴─────────┐
                ▼                   ▼
             Redis             Cube Worker
                │                   │
                └─────────┬─────────┘
                          ▼
                     Cube Store
                          │
                          ▼
                     PostgreSQL
```

This architecture follows common enterprise patterns by separating identity management (Keycloak), application logic (Backend), and analytics (Cube), making the system more secure, scalable, and easier to integrate with corporate identity providers.

