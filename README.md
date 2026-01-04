# Auth / SSO Service (Spring Boot + JWT + Refresh Tokens)

A production-style authentication service built with **Java + Spring Boot + Spring Security + Hibernate (JPA)**.  
It provides secure **Register/Login**, **JWT access tokens**, **Refresh tokens**, **Role-based authorization**, and **Logout (token revocation)**.  
Designed to be extended into **SSO (OAuth2/OIDC)** later.

## Why this project
Most applications require strong authentication. This service demonstrates:
- secure credential handling (BCrypt)
- stateless auth with JWT
- refresh token rotation and revocation
- clean layered architecture (Controller → Service → Repository)
- database migrations (Flyway)
- Docker + CI-ready setup

---

## Features (MVP)
- User registration (email + password)
- User login (JWT access + refresh token)
- Refresh token endpoint (issue new access token)
- Logout endpoint (revoke refresh token)
- Role-based access control (USER / ADMIN)
- Protected endpoint: `/api/v1/users/me`
- Input validation using `@Valid` (email format, password length)
- Flyway migrations for schema setup
- Docker + docker-compose for local run
- GitHub Actions CI (build + test)

---

## Tech Stack
- **Java 17**
- **Spring Boot 3.x**
- **Spring Security 6**
- **Spring Data JPA (Hibernate)**
- **PostgreSQL**
- **Flyway** (DB migrations)
- **Bean Validation** (Jakarta Validation)
- **JWT** (access + refresh token flow)
- **Docker** + **docker-compose**
- **JUnit 5** / Spring Boot Test

---

## Architecture
**Layers**
- **Controller**: REST endpoints + request validation
- **Service**: business logic (register/login/refresh/logout)
- **Repository**: DB access via Spring Data JPA
- **Security**: JWT creation/validation + request filter

**Auth flow**
1. User registers → password hashed with BCrypt → user saved
2. User logs in → credentials verified → issue:
   - Access token (short expiry)
   - Refresh token (long expiry, stored in DB)
3. Client uses Access token for API calls
4. When Access expires → call refresh endpoint with Refresh token → new Access (and optionally rotate refresh)
5. Logout → refresh token revoked in DB

---

## API Endpoints
Base path: `/api/v1`

### Auth
- `POST /auth/register`  
  **Body**
  ```json
  { "email": "user@example.com", "password": "StrongPass123!" }
