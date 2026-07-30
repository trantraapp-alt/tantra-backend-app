# Tantra — Authentication (Signup / Login) Frontend Guide

Everything for **user registration, login, session and password reset**. Shared by both frontend
workstreams — build the auth + API client once. Base URL local: `http://localhost:8080`.

---

## 0. Roles (current model — 2 roles only)

| appUsageRole (sent at signup) | Stored / returned as | Who | Can do |
|---|---|---|---|
| `USER` | `ROLE_USER` | Normal user | Buy, sell, rent, manage own listings & business profiles |
| `ADMIN` | `ROLE_ADMIN` | Staff | Maintain the catalog (categories/dropdowns/forms) + verify business profiles |

> The old `BUYER / SELLER / BOTH` roles are **gone** — a single `USER` now does everything a buyer and
> seller need. Only `ADMIN` is special (it gates the `/api/v1/admin/**` endpoints).

- Whatever you send as `appUsageRole` is stored as `ROLE_<VALUE>` (uppercased). Send **`USER`** for normal
  sign-ups; **`ADMIN`** is created by staff/seeding, not from the public app.
- The login response's `role` is the **prefixed** value (`ROLE_USER` / `ROLE_ADMIN`). Gate admin screens on
  `role === "ROLE_ADMIN"`.

---

## 1. Response shape

Auth endpoints return the standard envelope — **read your payload from `data`**:
```json
// success
{ "success": true, "data": { ... }, "message": {...}|null, "traceId": "ab12", "timestamp": "..." }
```
⚠️ **Auth is a legacy area:** on failure these endpoints return **HTTP 400** with an **`error` string**
inside the payload (e.g. `data.error = "Error: Invalid password!"`) rather than the standard
`error.code` object. So in your auth calls: **if HTTP is 4xx, show `data.error` (a plain string).**
The `error`/`message` strings are already localised by the `lang` you pass (see below).

---

## 2. Sign up

```
POST /api/v1/auth/signup
{
  "firstName": "Ravi",
  "lastName": "Kumar",
  "mobileNumber": "9000000002",
  "password": "Secret@123",
  "appUsageRole": "USER",          // USER for the public app (ADMIN is staff-only)
  "preferredLanguage": "HI"        // HI | EN
}
```
**Success →** `data: { message, userId }` (e.g. `userId: "TN3XYZ45"`).
**Failure (400) →** `data: { error: "Error: Mobile number already registered!" }`.

Notes:
- `mobileNumber` must be **unique** (duplicate → 400).
- `preferredLanguage` drives the user's language across the app (defaults to `EN` if omitted).
- Signup does **not** log the user in — follow it with a sign-in (or auto-call sign-in with the same creds).
- Recommended client-side validation (backend doesn't enforce these yet): 10-digit mobile `^[6-9][0-9]{9}$`,
  password min length/strength, required names.

---

## 3. Sign in

```
POST /api/v1/auth/signin?lang=HI          // lang (EN|HI) localises the message; default EN
{ "mobileNumber": "9000000002", "password": "Secret@123" }
```
**Success →** `data: { message, token, role, userId }`
```json
{ "token": "<JWT>", "role": "ROLE_USER", "userId": "TN3XYZ45" }
```
**Failure (400) →** `data.error`: "Mobile number not registered!" or "Invalid password!".

Store `data.token`. Route by `role`:
- `ROLE_ADMIN` → admin dashboard (catalog + verification)
- anything else (`ROLE_USER`) → the normal app

---

## 4. Using the token

Send it on every protected call:
```
Authorization: Bearer <token>
```
- The JWT is **long-lived** and **stateless** — there's no server session. Persist it securely
  (secure storage / httpOnly-style handling on web).
- **Logout = delete the stored token** on the client (no logout endpoint needed).
- No refresh-token yet — when a call returns 401 (expired/invalid), send the user back to login.
- Public endpoints (browse modules/categories, render a form, view files) work **without** a token; all
  writes and "my …" reads require it.

---

## 5. Current user profile

```
GET /api/v1/auth/profile        (Authorization: Bearer <token>;  optional ?lang=EN|HI)
→ data: { userId, firstName, lastName, mobileNumber, appUsageRole, preferredLanguage, sessionToken, sessionExpiry }
```
Use it to hydrate the profile screen and to check `sessionExpiry`. On an invalid/expired token it returns
**401** with an error body → route to login.

---

## 6. Forgot password (OTP)

Two steps. OTP is 6 digits, valid **5 minutes**. (SMS gateway not wired yet — in dev the OTP is printed to
the **server console/terminal**.)
```
POST /api/v1/auth/forgot-password/request?mobileNumber=9000000002&lang=HI
   → 200 "OTP sent successfully."   |   400 "Error: Number not found."

POST /api/v1/auth/forgot-password/reset?lang=HI
{ "mobileNumber": "9000000002", "otp": "123456", "newPassword": "NewSecret@123" }
   → 200 "Password reset successful."  |  400 "Error: OTP has expired!" / "Error: Invalid OTP!"
```
These two return a **plain localised string** body (not the object envelope) — show it directly. After a
successful reset, send the user to sign-in.

---

## 7. Endpoint quick reference

| Action | Endpoint | Auth |
|---|---|---|
| Sign up | `POST /api/v1/auth/signup` | — |
| Sign in | `POST /api/v1/auth/signin?lang=` | — |
| Current profile | `GET /api/v1/auth/profile` | Bearer |
| Forgot password — request OTP | `POST /api/v1/auth/forgot-password/request?mobileNumber=&lang=` | — |
| Forgot password — reset | `POST /api/v1/auth/forgot-password/reset?lang=` | — |

**Test users (seeded via Postman):** Admin `9000000001 / Admin@123` (`ROLE_ADMIN`), User `9000000002 / Seller@123` (`ROLE_USER`).

---

## 8. Build checklist
- [ ] API client: attach `Authorization` header, unwrap `data`, and for auth calls surface `data.error` on 4xx
- [ ] Signup screen (fields above; `appUsageRole = USER`; client-side validation)
- [ ] Signin screen → store `token`, route by `role` (`ROLE_ADMIN` vs user)
- [ ] Token persistence + "logout clears token" + 401 → redirect to login
- [ ] Profile screen from `GET /auth/profile`
- [ ] Forgot-password: request OTP → enter OTP + new password → reset → back to login
- [ ] Language: pass `lang` (from `preferredLanguage`) so messages come back localised
