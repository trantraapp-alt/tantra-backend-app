# Tantra — Frontend Developer Guide (A → Z)

Everything the app needs, as built so far, in one place. **Tantra** is a **multi-vertical, multi-module
marketplace** powered by ONE **config-driven engine** — every screen (forms, dropdowns, navigation) is
drawn from backend metadata, so new modules / categories / fields / dropdown values appear **without an
app release**. Two roles: `USER` (buy / sell / rent / manage) and `ADMIN` (maintains catalog + verifies
business profiles).

- **Base URL (local):** `http://localhost:8080` — on a phone use the LAN IP (`http://192.168.x.x:8080`); prod = the deployed domain. Keep it in ONE constant.
- **Postman:** `postman/` folder — import to see every request. Test users: Admin `9000000001 / Admin@123`, Seller `9000000002 / Seller@123`.
- **Companion doc:** `FRONTEND_API.md` — the low-level request/response contract.

> **Work split (two frontend developers).** **Business Profile + Verification** is a separate workstream
> owned by the other developer — see **`BUSINESS_PROFILE_FRONTEND_GUIDE.md`** (self-contained). **This guide
> covers everything else** (auth, navigation, forms, listings, address, notifications). The only shared code
> is the API client (§0) and the metadata form renderer (§3) — agree on those together, build them once.

---

## 0. Golden rules (read first)

**A. One response envelope — always read your payload from `data`.**
```json
// success
{ "success": true, "data": { ... }, "message": {"en":"..","hi":".."}, "traceId":"ab12", "timestamp":".." }
// error
{ "success": false, "error": { "code":"NOT_FOUND", "message": {"en":"..","hi":".."} }, "traceId":"ab12" }
```
Build ONE API client that: unwraps `data`, throws on `success:false` surfacing `error.code` + `error.message`,
and captures `traceId` (also in the `X-Trace-Id` response header) for support / error screens.

**B. Dual language (Hindi + English).** All labels/messages come as `{ "en": "...", "hi": "..." }`. Pick by
the user's `preferredLanguage`. Success/error toasts use the response `message`. More languages later = same shape.

**C. Config-driven — never hardcode.** Forms, dropdowns, navigation, validation all come from the backend.
A new field or dropdown value is a backend data change, not an app release. Render from metadata every time.

**D. Auth.** Every protected call sends `Authorization: Bearer <token>`.

---

## 1. Auth

```
POST /api/v1/auth/signup   { firstName, lastName, mobileNumber, password, appUsageRole, preferredLanguage }
POST /api/v1/auth/signin   { mobileNumber, password }   → data: { token, role, userId }
```
Store `data.token`; send it on every protected request. Roles: **`USER`** (buy/sell/rent/manage) and
**`ADMIN`** (catalog + verification); login `role` comes back prefixed (`ROLE_USER` / `ROLE_ADMIN`).
**Full auth details — signup, login, profile, forgot-password, token handling — are in `AUTH_FRONTEND_GUIDE.md`.**

---

## 2. Navigation — Module → Category → Subcategory (a tree)

The home screen is a list of **app modules**; each drills into a category tree.

```
GET /api/v1/masters/modules?onlyActive=true                              → Home cards (module list)
GET /api/v1/masters/modules/{moduleId}/categories?onlyActive=true        → top-level categories of a module
GET /api/v1/masters/categories/{parentId}/subcategories?onlyActive=true  → subcategories (the leaf categories)
```
Categories are a tree via `parentId`. The **leaf** category's `id` is what feeds the form / listing APIs.
Render everything from the API — the structure below is just today's seeded data:

| Module (`moduleKey`) | Top-level | Leaf categories |
|---|---|---|
| **Agriculture** (`agriculture`) | Agriculture Marketplace | Crop · Seed · Pesticide · Fertilizer · Equipment |
| | Agriculture Services | (form on the top-level itself) |
| | Repair & Maintenance | (form on the top-level itself) |
| **Animal & Livestock** (`animal_livestock`) | Veterinary | *(business-profile category — see §8)* |
| | Animal Marketplace | Poultry · Fishery · Animal |

Each category carries **`actionType`** — decide what to open on tap:
- `LISTING` (default) → open the **dynamic form** (§3) → post a **listing** (§6)
- `BUSINESS_PROFILE` → open the **business-profile** flow (§8), prefilling `profileType = category.linkKey`
  (e.g. **Veterinary** → `vet_clinic`)

---

## 3. The dynamic form renderer (your most reused component)

ONE generic component renders the form for **any** category. This is the heart of the app.

```
GET /api/v1/categories/{categoryId}/form?listingType=SELL      (SELL | RENT — no token needed)
```
Returns `sections[]`, each with `fields[]`. Render each field by `type` and honour its flags:

| field.type | Render as |
|---|---|
| TEXT / TEXTAREA / NUMBER / DECIMAL | input / textarea |
| DROPDOWN / RADIO | `field.options[]` — each `{ id, value, label{en,hi}, parent }` |
| MULTISELECT / CHECKBOX_GROUP | multi-select from `options` (e.g. Labour types, Health status) |
| BOOLEAN | switch |
| DATE | date picker |
| IMAGE | uploader (§5) |
| ADDRESS | address block (§7) |
| AUTO_CALC | computed, read-only (e.g. discount %) |

**Per-field flags — honour them all:**
- `required` — mandatory
- `readOnly` — show, never edit (e.g. auto-calculated discount)
- `fieldLength` — maxlength for text/textarea
- `validation` — `min / max / maxLength / regex / acceptedTypes` (+ image `sources`, `capture`)
- `visibleWhen` `{ field, operator, value }` — show only when another field matches (`equals` / `notEquals` / `in` / `notIn`)
- `allowOther` — show a “Please Specify” box when the user picks the `__other__` option
- `parentField` — this dropdown **cascades** from another field (§4)
- `editableOnUpdate` — if `false`, **lock** this field on the edit form (core identity field)
- `inlineEditable` — editable directly in the My-Listings grid (§6)

**Sections = steps.** Render sections as a multi-step wizard or one long scroll — your choice; the backend
is identical either way.

**For Rent/Sell categories** (Equipment, Animal — `listingType = BOTH`): always request `?listingType=SELL`
or `RENT`; the backend serves the one shared form. The listing's real type is derived from the
`availableFor` field the user picks (Rent vs Sell), never from your query.

---

## 4. Cascading dropdowns (Category → Name) — important

Many categories use a **parent → child** dropdown pair: pick a *type/category*, then only that group's
*names* show. Examples: **Crop Type → Crop Name**, **Pesticide Category → Pesticide Name**, and
Country → State → District for addresses.

**How it's expressed in the form metadata:**
- The child field carries **`parentField`** = the driving field's `fieldKey`.
- Every option has its own **`id`** and a **`parent`** (the parent option's id; `null` for top-level).

**How to render (two equivalent ways):**
1. **Client-side filter (default — options are already inlined):** the form response already contains all
   child options, each tagged with `parent`. When the user picks the parent, grab that option's `id` and
   show only child options where `option.parent === pickedParentId`.
2. **On-demand fetch (big sets like geo):** `GET /api/v1/option-sets/{setKey}/items?parentItemId={pickedParentId}`.

**Example — Crop:** `cropType` (Cereal / Pulse / Oilseed / …) drives `cropName` (`parentField:"cropType"`).
Pick *Cereal* → show only Wheat / Paddy / Maize / … Submit both keys in `attributes`:
`{ "cropType":"cereal", "cropName":"wheat" }`. If the parent is set to `__other__`, the child dropdown
hides and a free-text “Please Specify” field shows instead (`visibleWhen` handles this automatically).

**Cascade / list map (today's seeded data — render from API, don't hardcode):**

| Category | Parent field | Child field | Shape |
|---|---|---|---|
| Crop | `cropType` (6) | `cropName` (79) | cascade |
| Seed | `seedType` (9) | `seedName` (82) | cascade |
| Pesticide | `pesticideCategory` (7) | `pesticideName` (42) | cascade |
| Fertilizer | `fertilizerCategory` (7) | `fertilizerName` (38) | cascade |
| Equipment | `equipmentCategory` (8) | `equipmentName` (28) | cascade + Rent/Sell |
| Poultry | `poultryCategory` (2) | `poultryName` (19) | cascade |
| Fishery | — | `fisheryType` (24 fish) | flat |
| Animal | — | `animalName` (13) | flat + Rent/Sell |

The parent + child fields are `editableOnUpdate:false` (locked once listed — to change them, delete &
re-create). Seed/Crop/etc. child keys may be type-prefixed (e.g. `vegetable_coriander`) — that's the stored
`value`; always show the option's `label`.

---

## 5. Images (upload + camera capture)

Two steps: upload the files first → get URLs → put the URLs in the listing. **Photos and listing never
travel together.**
```
POST /api/v1/uploads      (Authorization; multipart/form-data, field name "files", 1–10 jpg/png/webp)
   → data: { urls: ["/files/ab12.jpg", ...] }
```
- Don't set `Content-Type` manually — the browser sets the multipart boundary.
- Display an image: `BASE_URL + url` → `http://localhost:8080/files/ab12.jpg` (public, no token).
- Camera vs gallery is UI-only: the IMAGE field's `validation.sources = ["camera","gallery"]` / `capture:true`
  tell you to show both. Both produce a file → same `/uploads` endpoint.
- Web: `<input type="file" accept="image/*" capture="environment" multiple>`. Mobile: native camera/gallery picker.

---

## 6. Listings (create + My Listings + edit)

**Every listing write returns** `{ success, listingId, userId, message{en,hi} }` (inside `data`).

**Create** — common fields at top level, category-specific fields under `attributes` (keyed by each field's
`fieldKey` from §3):
```
POST /api/v1/listings
{ "categoryId":5, "listingType":"SELL", "actualPrice":300, "offeredPrice":280, "isNegotiable":true,
  "images":["/files/a.jpg"],
  "addressId":"TN9ABC1"        /* or "useDefaultAddress":true, or a raw "address" object */,
  "attributes": { "cropType":"cereal", "cropName":"wheat", "qty":50, "qtyMeasurement":"quintal" } }
```
Backend injects userId / listingId / mobile / audit; discount % is recomputed server-side. Don't send
those yourself.

**My Listings (grid)** — paginated, filter by type/status:
```
GET /api/v1/listings/mine?listingType=SELL&status=ACTIVE&page=0&size=20
```
Returns a **page** object → read `data.content`; use `data.totalPages` / `data.last` for infinite scroll.

**Browse a category** — paginated:
```
GET /api/v1/listings/category/{categoryId}?listingType=SELL&page=0&size=20
```

| Action | Call |
|---|---|
| One listing | `GET /listings/{listingId}` |
| Quick inline edit (grid) | `PATCH /listings/{listingId}` — only `inlineEditable` fields (price / qty / description) or `{ "status":"SOLD" }` |
| Full edit (form) | `PUT /listings/{listingId}` — pre-filled form; `editableOnUpdate:false` fields locked; removed images auto-deleted |
| Delete (soft) | `DELETE /listings/{listingId}` |

**Full-edit pre-fill:** `GET form` (§3) + `GET /listings/{id}` → common fields from the top level, category
fields from `attributes[fieldKey]`. Locked fields (the cascade parent + name, `availableFor`) show disabled
— to change them, delete & re-create.

**RENT vs SELL display:** if `listingType == RENT`, show `attributes.rentPerHour`; else `offeredPrice`
(+ strike-through `actualPrice`, `discountPct`).

---

## 7. Address book (Profile ▸ Address) + location

Max **10** per user, exactly **one default** (backend-managed, transactional). All authenticated.
```
GET    /api/v1/addresses            → list (default first)
GET    /api/v1/addresses/default
POST   /api/v1/addresses            → { success, addressId, message }
PUT    /api/v1/addresses/{addressId}
PATCH  /api/v1/addresses/{addressId}/default
DELETE /api/v1/addresses/{addressId}
```
Body: `label, fullAddress, country, state, district, city, village, pinCode, latitude, longitude,
mobileNumber, altMobileNumber, isDefault`.

**Using an address in a listing:** send `addressId` (or `useDefaultAddress:true`) on the listing
`POST`/`PUT`; the backend **snapshots** it onto the listing, so editing/deleting the saved address later
**never** changes past listings. You can still send a raw `address` object instead (the fallback).

**Country / State / District** = cascading dropdowns (§4) from seeded geo option-sets
(`GET /option-sets/{state|district}/items?parentItemId=`); City / Village free-text.
**"Use Current Location"** (frontend only): Geolocation → reverse-geocode (Nominatim / Google / Mappls) →
auto-fill fields → match state/district to the dropdown values → keep lat/long. On deny → manual entry.
**"Create New Address" from the listing form:** navigate to the Address tab with a `returnTo`, save, redirect
back — and **preserve the in-progress listing draft** (global store / localStorage).

---

## 8. Business Profile + verification → separate guide

**Owned by the other developer** — see **`BUSINESS_PROFILE_FRONTEND_GUIDE.md`** (self-contained). It covers
dealer/vet profiles, the metadata-driven profile form, documents, admin verification and the verified badge.

One touchpoint you (this workstream) own: a category can have `actionType = "BUSINESS_PROFILE"` (e.g.
**Veterinary**). When the user taps it, **hand off** to the Business Profile flow, passing
`profileType = category.linkKey` (Veterinary → `vet_clinic`). That's the only integration point.

---

## 9. Notifications

```
GET   /api/v1/notifications?page=0&size=20   → page of { type, title{en,hi}, body{en,hi}, isRead, refType, refId, createdAt }
GET   /api/v1/notifications/unread-count      → { count }
PATCH /api/v1/notifications/{id}/read
```
Show the unread badge; on tap deep-link via `refType` + `refId` (e.g. `BUSINESS_PROFILE` → open that
profile). Users are notified on verification approve/reject.

---

## 10. Endpoint quick reference

| Area | Endpoints |
|---|---|
| Auth | `POST /auth/signup`, `POST /auth/signin` |
| Navigation | `GET /masters/modules`, `GET /masters/modules/{id}/categories`, `GET /masters/categories/{parentId}/subcategories` |
| Form | `GET /categories/{id}/form?listingType=`, `GET /option-sets/{key}/items?parentItemId=` |
| Images | `POST /uploads`, view `GET /files/{name}` |
| Listing | `POST /listings`, `PUT/PATCH/DELETE /listings/{id}`, `GET /listings/mine`, `GET /listings/category/{id}`, `GET /listings/{id}` |
| Address | `GET/POST /addresses`, `GET /addresses/default`, `PUT/DELETE /addresses/{id}`, `PATCH /addresses/{id}/default` |
| Business Profile | *→ separate workstream, see `BUSINESS_PROFILE_FRONTEND_GUIDE.md`* |
| Notifications | `GET /notifications`, `GET /notifications/unread-count`, `PATCH /notifications/{id}/read` |

---

## 11. Category cheat-sheet (all modules, today's seed)

| Module | Category | Listing type | Key fields (attributes) |
|---|---|---|---|
| Agriculture | Crop | SELL | `cropType`→`cropName`, quality, harvest, qty/unit, price |
| Agriculture | Seed | SELL | `seedType`→`seedName`, brand, variety, qty/unit, price |
| Agriculture | Pesticide | SELL | `pesticideCategory`→`pesticideName`, `pesticideType` (Powder/Liquid), qty/unit |
| Agriculture | Fertilizer | SELL | `fertilizerCategory`→`fertilizerName`, `fertilizerType` (Powder/Liquid), qty/unit |
| Agriculture | Equipment | RENT/SELL | `equipmentCategory`→`equipmentName`, `availableFor`, brand/model/year, qty |
| Agriculture | Agriculture Services | (service) | `serviceType` (+ Labour multiselect), rate model |
| Agriculture | Repair & Maintenance | (service) | `repairType`, expertise, visit charge |
| Animal & Livestock | Veterinary | *business profile* | uses the business-profile form (`vet_clinic`) |
| Animal & Livestock | Poultry | SELL | `poultryCategory`→`poultryName`, purpose, age, health, qty/unit |
| Animal & Livestock | Fishery | SELL | `fisheryType` (fish), purpose, production method, qty/unit |
| Animal & Livestock | Animal | RENT/SELL | `animalName`, breed, gender, `availableFor`, age, qty |

---

## 12. Not built yet (don't wire these — coming next)

Buyer browse/filter (metadata-driven filters + nearby), contact-request → approval → number reveal, chat,
seller ratings, wishlist, moderation/reporting, payments/subscriptions, FCM push + SMS. This guide will be
extended as those land.

---

## Reference
- `AUTH_FRONTEND_GUIDE.md` — signup / login / roles / forgot-password (shared by both workstreams).
- `BUSINESS_PROFILE_FRONTEND_GUIDE.md` — the other developer's workstream (business profiles + verification).
- `FRONTEND_API.md` — detailed request/response contract (covers all areas, both workstreams).
- `postman/` — runnable collections. Seed a clean DB in this order: `00_Agriculture_Seed` →
  `12_AnimalLivestock_Seed` → `11_BusinessProfile`. Per-category collections `01`–`09` are isolated testers.
