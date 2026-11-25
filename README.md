# Let's Play — Project Documentation

## Table of Contents

1. [Overview](#overview)
2. [Technology Stack](#technology-stack)
3. [Quick Start](#quick-start)
4. [Data Model](#data-model)
5. [Architecture](#architecture)
6. [Authentication & Authorization](#authentication--authorization)
7. [API Reference](#api-reference)
8. [Error Handling](#error-handling)
9. [Security Measures](#security-measures)
10. [Configuration & Environment](#configuration--environment)
11. [Audit Checklist](#audit-checklist)

---

## Overview

Let's Play is a RESTful CRUD API that manages **users** and **products**, built with Spring Boot and MongoDB. Anyone can browse products; registered users manage their own products; the `root` account manages everything. Access is controlled with JWT sessions and role-based permissions. The API is versioned under `/api/v1/` and always speaks JSON, over HTTPS.

---

## Technology Stack

| Technology                                | Version | Role                                               |
| ----------------------------------------- | ------- | -------------------------------------------------- |
| Java                                      | 21      | Language                                           |
| Spring Boot                               | 4.0.1   | Application framework                              |
| Spring MVC (`spring-boot-starter-webmvc`) | —       | REST layer                                         |
| Spring Data MongoDB                       | —       | Persistence (`@Document`, `MongoRepository`)       |
| Spring Security Crypto                    | —       | BCrypt password hashing (no security filter chain) |
| Spring Validation                         | —       | Bean validation on request DTOs                    |
| jjwt (api / impl / jackson)               | 0.11.5  | JWT creation and parsing                           |
| dotenv-java (`cdimascio`)                 | 3.0.0   | `.env` loading at startup                          |
| MongoDB                                   | 7       | Database                                           |
| Docker (multi-stage) + Compose            | —       | Build and run                                      |

---

## Quick Start

### With Docker (recommended)

```bash
# 1. create .env with fresh secrets (values are random; edit ROOT_PASSWORD if you like)
cat > .env <<EOT
JWT_SECRET=$(openssl rand -base64 48)
MONGO_PASSWORD=$(openssl rand -hex 16)
SSL_KEYSTORE_PASSWORD=$(openssl rand -hex 16)
ROOT_PASSWORD=ChangeMe123!
EOT

# 2. build and start MongoDB + the API
docker compose up --build
```

The API is then available at **https://localhost:8443/api/v1**. On first start a self-signed TLS certificate is generated, so clients must accept it (`curl -k`, or disable "SSL certificate verification" in Postman).

### Without Docker

Requires Java 21, Maven, a reachable MongoDB, and a keystore:

```bash
mkdir -p certs
keytool -genkeypair -alias letsplay -keyalg RSA -keysize 2048 -validity 365 \
  -storetype PKCS12 -keystore certs/keystore.p12 -storepass <keystore-password> \
  -dname "CN=localhost" -ext "SAN=dns:localhost,ip:127.0.0.1"

# .env (same variables as above) must exist in the working directory, with
# SPRING_MONGODB_URI added if MongoDB is not on mongodb://localhost:27017/letsplay
mvn spring-boot:run
```

### Try it

The API authenticates with an HttpOnly `jwt` cookie set by `/register` and `/login`, so `curl` needs a cookie jar (`-c` to save, `-b` to send):

```bash
# list products (public)
curl -k https://localhost:8443/api/v1/products

# register (also logs you in: the jwt cookie is saved to cookies.txt)
curl -k -c cookies.txt -X POST https://localhost:8443/api/v1/register \
  -H 'Content-Type: application/json' \
  -d '{"name":"alice","email":"alice@mail.com","password":"correct-horse-1"}'

# create a product with that session
curl -k -b cookies.txt -X POST https://localhost:8443/api/v1/products \
  -H 'Content-Type: application/json' \
  -d '{"name":"Guitar","description":"6 strings","price":199.9}'
```

In Postman the cookie is stored and sent automatically (turn off "SSL certificate verification" for the self-signed certificate).

`scripts/smoke-test.sh` runs the whole audit scenario (CRUD, roles, errors) against a running instance and prints PASS/FAIL per check:

```bash
scripts/smoke-test.sh https://localhost:8443
```

---

## Data Model

```
users     — id, name, email (unique), password (BCrypt hash), role ("root" | "default")
products  — id, name, description, price, userId  → users.id
sessions  — userId (id), jwt, createdAt            (one active session per user)
```

A user owns many products. `id` is MongoDB's `ObjectId`, exposed as a string. Deleting a user also deletes their products.

---

## Architecture

```
src/main/java/com/z01/letsplay/
├── Main.java                  — Entry point; loads .env before Spring starts
├── annotation/                — @Auth.User, @EntityAccess, @AccessMethod, @RequiresPermission
├── api/v1/                    — REST controllers
├── config/                    — JwtConfig, WebConfig, JwtPrincipalProvider, PermissionValidator,
│                                GlobalExceptionHandler, RootBootstrap
├── exception/                 — AppError enum + AppException
├── infrastructure/            — EntityRegistry, EntityAccessResolver, PermissionInterceptor, RestrictedEntity, ...
├── model/                     — @Document entities + nested `repo` interfaces
│   └── DTO/                   — request classes (validated) and response records
├── resolver/                  — AuthResolver (resolves @Auth.User from the jwt cookie)
└── services/                  — AuthService, UserService
```

Authorization follows one rule: **an endpoint is protected by what its signature or annotations declare**, and controllers contain no access-control code.

| Declaration                             | Effect                                                                                     |
| --------------------------------------- | ------------------------------------------------------------------------------------------ |
| `@Auth.User String userId`              | the caller must be logged in; injects their id                                             |
| `@EntityAccess(mode = Write) Product p` | loads the product from the path variable (404 if missing) and calls `Product.ensureAccess` |
| `@RequiresPermission(scope = "...")`    | `PermissionInterceptor` checks the caller's role grants the scope (403 otherwise)          |
| none of the above                       | public endpoint                                                                            |

### Request Flow

```
HTTPS request
    │
    ▼
PermissionInterceptor   @RequiresPermission -> PermissionValidator (role -> scopes)
    │                   runs BEFORE the body is read: 401 / 403 win over an invalid body
    ▼
Argument resolvers      in signature order: @Auth.User / @EntityAccess (read the jwt cookie,
    │                   validate the session, load user / entity, ensureAccess), then
    │                   @RequestBody + @Valid (400)
    ▼
Controller / Service    business logic, repositories
    │
    ▼
GlobalExceptionHandler  every exception -> JSON { status, error, message }
```

Keep `@Auth.User` / `@EntityAccess` **before** `@RequestBody` in controller signatures, otherwise an invalid body is reported (400) before a missing login (401) or a forbidden entity (403/404).

---

## Authentication & Authorization

- `POST /register` creates an account with the **default** role (the role is never read from the request) and logs it in.
- `POST /login` logs in. Both set an **HttpOnly, Secure, SameSite=Lax cookie named `jwt`**, valid 3 days. The browser / Postman / `curl -b` sends it back on every request.
- The JWT is signed with `JWT_SECRET`. It is only valid while it matches the user's row in `sessions` (one active session per user: logging in again replaces the previous token). The user and their role are loaded from the database on every request, so deleting a user or changing a role applies immediately.
- The **root** account (email `root`) is created at startup by `RootBootstrap`, exactly like in 01blog: its password comes from `ROOT_PASSWORD`, or is typed on the console (with confirmation) when the app runs interactively, and startup fails if neither is available. No default password exists. The `root` role is re-assigned to that account on every start. Root can create further users with `POST /users`.

| Who            | Can do                                                            |
| -------------- | ----------------------------------------------------------------- |
| Anyone         | register, log in, read products                                   |
| `default` role | create products; update / delete **their own** products           |
| `root` role    | everything a user can, on **any** product; full CRUD on all users |

Where the rules live: ownership in `Product.ensureAccess` (owner or root may write), role permissions in `PermissionValidator` (`root` holds `v1:users:read` and `v1:users:write`, `default` holds none).

---

## API Reference

All endpoints are prefixed with `/api/v1/` and return JSON.

### Auth

| Method | Path        | Auth | Success | Description                                                      |
| ------ | ----------- | ---- | ------- | ---------------------------------------------------------------- |
| POST   | `/register` | —    | 201     | Register (default role), sets the `jwt` cookie; returns the user |
| POST   | `/login`    | —    | 200     | Log in, sets the `jwt` cookie; returns the user                  |

### Products

| Method | Path             | Auth          | Success | Description             |
| ------ | ---------------- | ------------- | ------- | ----------------------- |
| GET    | `/products`      | —             | 200     | List all products       |
| GET    | `/products/{product}` | —        | 200     | Get one product         |
| POST   | `/products`      | logged in     | 201     | Create (owner = caller) |
| PUT    | `/products/{product}` | owner or root | 200 | Update                  |
| DELETE | `/products/{product}` | owner or root | 204 | Delete                  |

Every endpoint that takes an id uses the entity's name as the path variable (`/products/{product}`, `/users/{user}`): that name is how `@EntityAccess` finds the id, and it must match the parameter name. Body for POST / PUT: `{"name": "Guitar", "description": "6 strings", "price": 199.9}` (`description` optional).

### Users (root only)

| Method | Path          | Success | Description                                      |
| ------ | ------------- | ------- | ------------------------------------------------ |
| GET    | `/users`      | 200     | List users                                       |
| GET    | `/users/{user}` | 200   | Get one user                                     |
| POST   | `/users`      | 201     | Create `{name,email,password,role}`              |
| PUT    | `/users/{user}` | 200   | Update `{name,email,role}` (+ optional password) |
| DELETE | `/users/{user}` | 204   | Delete the user and their products               |

Responses never contain the `password` field.

---

## Error Handling

Failures are always returned in one JSON shape, including those raised by Spring MVC itself (unknown route, wrong method, wrong content type):

```json
{
  "status": 403,
  "error": "PERMISSION_DENIED",
  "message": "You do not have permission to perform this action"
}
```

| Status    | `error` (examples)                                                     | When                                                          |
| --------- | ---------------------------------------------------------------------- | ------------------------------------------------------------- |
| 400       | `VALIDATION_FAILED`, `MALFORMED_REQUEST`, `PASSWORD_TOO_LONG`          | invalid field, broken / wrongly typed JSON                    |
| 401       | `MISSING_JWT_TOKEN`, `INVALID_JWT`, `INVALID_EMAIL_OR_PASSWORD`        | no / bad / expired session, bad credentials                   |
| 403       | `PERMISSION_DENIED`, `ACCESS_DENIED`                                   | role lacks the permission / not the owner                     |
| 404       | `USER_NOT_FOUND`, `PRODUCT_NOT_FOUND`, `ENTITY_NOT_FOUND`, `NOT_FOUND` | unknown resource or route                                     |
| 405 / 415 | `METHOD_NOT_ALLOWED`, `UNSUPPORTED_MEDIA_TYPE`                         | wrong method / content type                                   |
| 409       | `EMAIL_ALREADY_EXISTS`                                                 | duplicate email (also enforced by a unique index)             |
| 500       | `INTERNAL_ERROR`                                                       | genuine server fault only; details are logged, never returned |

Add a new error by adding a constant to `AppError` and throwing `AppError.X.asException()`.

---

## Security Measures

| Measure                        | How                                                                                                                                                                     |
| ------------------------------ | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Password hashing + salting     | `BCryptPasswordEncoder` (random salt per hash). Passwords are limited to 72 bytes, BCrypt's real limit                                                                  |
| MongoDB injection              | typed DTOs (a JSON object sent where a string is expected is rejected with 400), Spring Data derived queries only (bound parameters, no query strings built from input) |
| Input validation               | `@NotBlank`, `@NotNull`, `@Size`, `@Email`, `@Pattern`, `@DecimalMin/Max` on every DTO + `@Valid`; email trimmed and lower-cased                                        |
| No sensitive data in responses | responses are dedicated records without `password`; entities are never serialized                                                                                       |
| HTTPS                          | TLS on port 8443 (PKCS12 keystore); the session cookie is `Secure`, `HttpOnly`, `SameSite=Lax`                                                                          |
| Strict access control          | every non-public endpoint declares `@Auth.User` / `@EntityAccess` / `@RequiresPermission`; the owner is always the logged-in user, never taken from the body            |
| Privilege escalation           | self-registration always yields the `default` role; roles can only be set by root                                                                                       |
| Secrets                        | JWT secret, DB password, root password, keystore password come from the environment; `.env` is git-ignored                                                              |

---

## Configuration & Environment

| Variable                | Required    | Description                                                                                                       |
| ----------------------- | ----------- | ----------------------------------------------------------------------------------------------------------------- |
| `JWT_SECRET`            | yes         | HMAC key, at least 32 characters (startup fails otherwise)                                                        |
| `MONGO_PASSWORD`        | Docker      | MongoDB root password (URL-safe characters only)                                                                  |
| `SPRING_MONGODB_URI`    | no          | defaults to `mongodb://localhost:27017/letsplay`; set by `docker-compose`                                         |
| `SSL_KEYSTORE_PASSWORD` | yes         | Password of the TLS keystore                                                                                      |
| `SSL_KEYSTORE_PATH`     | no          | defaults to `certs/keystore.p12`                                                                                  |
| `ROOT_PASSWORD`         | first start | Password of the `root` account, used when it has to be created (otherwise asked on the console, or startup fails) |

Variables can be provided through a `.env` file (loaded by `dotenv-java` before Spring starts) or as real environment variables. See `.env.example`.
