# Tantra — Frontend API Guide (Listings + Images)

Base URL (local): `http://localhost:8080`
> On a phone/other device use the backend machine's LAN IP (e.g. `http://192.168.1.5:8080`), not `localhost`.
> In production this becomes the deployed domain. Keep it in one config constant.

## ⚠️ Every response uses one envelope

All endpoints now return a standard wrapper — **read your payload from `data`**:
```json
// success
{ "success": true, "data": { ... }, "message": {"en":"..","hi":".."}, "traceId":"ab12..", "timestamp":".." }
// error
{ "success": false, "error": { "code":"NOT_FOUND", "message": {"en":"..","hi":".."} }, "traceId":"ab12.." }
```
- Actual payload is in **`data`** (e.g. token at `data.token`, listing at `data`, page at `data.content`).
- `message` = bilingual success message (show toast). On error, `error.code` + `error.message{en,hi}`.
- `traceId` is also in the `X-Trace-Id` response header — log it / show on error screens for support.
- Build one API client that unwraps `data` and surfaces `error` globally.

Bilingual messages come as `{ "en": "...", "hi": "..." }` — pick by the user's language.

---

## 1. Auth — get a token

**Sign in**
```
POST /api/v1/auth/signin
{ "mobileNumber": "9000000002", "password": "Seller@123" }

→ { "token": "<JWT>", "role": "ROLE_USER", "userId": "TN3XYZ45" }
```
Store the `token`. Send it on every protected call:
```
Authorization: Bearer <token>
```
(Sign up: `POST /api/v1/auth/signup` with firstName, lastName, mobileNumber, password, appUsageRole, preferredLanguage.)
Roles are **`USER`** and **`ADMIN`** (login `role` is prefixed: `ROLE_USER` / `ROLE_ADMIN`). Full auth flow → `AUTH_FRONTEND_GUIDE.md`.

---

## 1b. Category navigation (module → category → subcategory)

```
GET /api/v1/masters/modules?onlyActive=true                          → modules (Home cards: Agriculture, Animal & Livestock, …)
GET /api/v1/masters/modules/{moduleId}/categories?onlyActive=true    → top-level categories
GET /api/v1/masters/categories/{parentId}/subcategories?onlyActive=true → subcategories (Crop/Seed/…)
```
Top-level per module: **Agriculture** → Marketplace / Services / Repair; **Animal & Livestock** → Veterinary / Animal Marketplace (Poultry, Fishery, Animals). Don't hardcode — read them from the API.
Categories form a tree (`parentId`): a category with no `parentId` is top-level under the module;
its children are subcategories. The **leaf** subcategory's `id` is what you pass to the form/listing
APIs (§2, §4). (Same `GET .../categories?parentId={id}` also returns a parent's children.)

Each category carries **`actionType`** — decide what to open on tap:
- `LISTING` (default) → open the dynamic form (§2) → post a listing (§4)
- `BUSINESS_PROFILE` → open the business-profile flow (§4d), prefilling `profileType` = the category's `linkKey` (e.g. Veterinary → `vet_clinic`, in the Animal & Livestock module)

## 2. Get the form to render (metadata-driven UI)

```
GET /api/v1/categories/{categoryId}/form?listingType=SELL      (no token needed)
```
Returns the form as **sections → fields**. Render each field by its `type`:

| field.type | Render as |
|---|---|
| TEXT / TEXTAREA / NUMBER / DECIMAL | input / textarea |
| DROPDOWN / RADIO / MULTISELECT / CHECKBOX_GROUP | use `field.options` (each has `id`, `value`, `label{en,hi}`, `parent`) |
| BOOLEAN | switch / checkbox |
| IMAGE | the uploader (see §3) |
| ADDRESS | the address sub-form |

Each field also carries these control flags — honor them all:
- `required` — mandatory field (isMandatory)
- `readOnly` — show the value but don't let the user edit it (e.g. auto-calculated discount)
- `fieldLength` — max characters for text/textarea inputs
- `editableOnUpdate` — if `false`, lock this field on the **edit** form (core/identity field; not editable once listed)
- `inlineEditable` — if `true`, editable directly in the **My-Listings grid** (no full form)
- `validation` — min / max / maxLength / regex / acceptedTypes
- `visibleWhen` — show this field only when another field equals a value (conditional)
- `allowOther` — show a “Please Specify” text box when the user picks `__other__`
- `parentField` — this dropdown **cascades** from another field: show only the options whose `parent` equals the id of the option the user picked in `parentField` (see below)

**Cascading dropdowns** (e.g. Crop **Type → Name**, State→City):
A child field carries `parentField` (the driving field's `fieldKey`). Every option has its own `id` and a
`parent` (the parent option's id, `null` for top-level). Two equivalent ways to render the child:
- **Client-side (options already inlined):** the form response already contains all child options, each
  tagged with `parent`. When the user picks the parent, take that option's `id` and filter the child's
  options to `option.parent === pickedParentId`.
- **On-demand fetch** (large sets like geo): `GET /api/v1/option-sets/{setKey}/items?parentItemId={pickedParentId}`.

Example — Crop: `cropType` (Cereal / Pulse / Oilseed / …) drives `cropName` (`parentField:"cropType"`).
Pick Cereal → show only Wheat / Paddy / Maize / … Submit both keys in `attributes`:
`{ "cropType":"cereal", "cropName":"wheat" }`. If `cropType` = `__other__`, `cropName` hides and the
free-text `cropNameOther` shows instead.

---

## 3. Image upload (STEP 1 of listing)

Upload the picked/captured photos FIRST, get back URLs.
```
POST /api/v1/uploads          (Authorization required)
Content-Type: multipart/form-data
field name: "files"           ← one or many image files
```
- Do **not** set Content-Type manually — the browser sets the multipart boundary.
- Allowed: jpg, png, webp. Max: 10 files (from the IMAGE field's `validation`).

Response:
```json
{ "success": true, "urls": ["/files/ab12.jpg", "/files/cd34.jpg"] }
```
Keep these `urls`. To show an image: `BASE_URL + url` → `http://localhost:8080/files/ab12.jpg` (public, no token).

Camera vs gallery is a UI choice — both upload the same way:
```html
<input type="file" accept="image/*" capture="environment" multiple>  <!-- Take Photo -->
<input type="file" accept="image/*" multiple>                        <!-- Gallery -->
```

---

## 4. Create the listing (STEP 2)

Send form data + the image URLs from step 3 (NOT the files again).
```
POST /api/v1/listings         (Authorization required)
{
  "categoryId": 5,
  "listingType": "SELL",
  "actualPrice": 300,
  "offeredPrice": 280,
  "isNegotiable": true,
  "images": ["/files/ab12.jpg", "/files/cd34.jpg"],
  "useDefaultAddress": false,
  "address": { "fullAddress":"...", "country":"IN", "state":"MP", "city":"Bhopal",
               "pinCode":"462001", "latitude":23.25, "longitude":77.41,
               "mobileNumber":"9000000002", "altMobileNumber":"9000000003" },
  "attributes": { "cropName":"wheat", "qty":50, "qtyMeasurement":"quintal" }
}

→ { "success": true, "listingId": "TN7ABC12", "userId": "TN3XYZ45",
    "message": { "en":"Listing created successfully!", "hi":"..." } }
```
- **Common fields** (price, images, isNegotiable, address) are top-level.
- **Category-specific fields** go inside `attributes`, keyed by each field's `fieldKey` from §2.
- Don't send userId / listingId / mobile — backend fills them from the token.

All listing writes (create / update / patch / delete) return the same compact shape:
```json
{ "success": true, "listingId": "TN7ABC12", "userId": "TN3XYZ45",
  "message": { "en": "...", "hi": "..." } }
```

---

## 4b. My Listings — read, edit, delete

**List the user's own listings** (My-Listings page) — paginated, filter by type/status:
```
GET /api/v1/listings/mine?listingType=SELL&status=ACTIVE&page=0&size=20     (Authorization)
```

**Browse a category's listings** — paginated:
```
GET /api/v1/listings/category/{categoryId}?listingType=SELL&page=0&size=20  (Authorization)
```

Both return a **page** object (not a plain array). Use `page`/`size` (and optional `sort=createdAt,desc`); default size 20:
```json
{
  "content": [ /* listings */ ],
  "page": 0, "size": 20,
  "totalElements": 340, "totalPages": 17,
  "first": true, "last": false
}
```
Read the listings from `content`; use `totalPages`/`last` for infinite-scroll or page numbers.

**One listing:** `GET /api/v1/listings/{listingId}`

**Quick inline edit** (grid — only fields with `inlineEditable:true`; send just what changed):
```
PATCH /api/v1/listings/{listingId}
{ "offeredPrice": 260 }            // or { "quantity": 30 } or { "status": "SOLD" }
```

**Full edit** (the pre-filled form — everything except locked core fields):
```
PUT /api/v1/listings/{listingId}
{ "offeredPrice": 250, "images": ["/files/.."], "attributes": { ... } }
```
- Fields with `editableOnUpdate:false` (category type/name, availableFor) are **locked** — backend ignores changes to them. To change those, delete and create a new listing.
- Removed images: on `PUT`, drop them from the `images` array — the backend deletes those files automatically.

**Delete** (soft):
```
DELETE /api/v1/listings/{listingId}
```

---

## 4c. Address book (Profile ▸ Address)

Max 10 per user, exactly one default (backend-managed). All authenticated.

| Action | Call |
|---|---|
| List addresses | `GET /api/v1/addresses` |
| Default address | `GET /api/v1/addresses/default` |
| Create | `POST /api/v1/addresses` |
| Update | `PUT /api/v1/addresses/{addressId}` |
| Set default | `PATCH /api/v1/addresses/{addressId}/default` |
| Delete | `DELETE /api/v1/addresses/{addressId}` |

Create/update body: `label, fullAddress, country, state, district, city, village, pinCode,
latitude, longitude, mobileNumber, altMobileNumber, isDefault`. Writes return
`{ success, addressId, message:{en,hi} }`.

**Using an address in a listing** — send `addressId` (or `useDefaultAddress: true`) on the listing
`POST`/`PUT`; the backend copies (snapshots) that address onto the listing:
```
POST /api/v1/listings
{ "categoryId": 5, "addressId": "TN9ABC1", ...attributes... }   // or "useDefaultAddress": true
```
The listing keeps its own copy — editing/deleting the saved address later does **not** change past
listings. You can still send a raw `address` object instead (no address book) — that's the fallback.

Country/State/District will be **cascading dropdowns** (seeded geo data); City/Village free-text.
"Use Current Location" is frontend: Geolocation → reverse-geocode → auto-fill fields (+ lat/long).

---

## 4d. Business Profile (Profile ▸ Business Profile)

Dealers create profiles → admin verifies → verified badge. A user can have several. All authenticated.

**Metadata-driven form (same renderer as §3).**

| Action | Call |
|---|---|
| **Form to render** | `GET /api/v1/business-profiles/form?profileType={type}` |
| Create | `POST /api/v1/business-profiles` |
| My profiles | `GET /api/v1/business-profiles/mine` |
| View one | `GET /api/v1/business-profiles/{profileId}` (owner, or if APPROVED+visible) |
| Update | `PUT /api/v1/business-profiles/{profileId}` (resets to PENDING) |
| Delete | `DELETE /api/v1/business-profiles/{profileId}` |

Render `GET .../form` (fields per type, shared fallback), then submit: **common** (`profileType, businessName, address, isVisible`) top-level + the rest in **`attributes`** (ownerName, description, mobileNumber, email, gstNumber, registrationNumber…). `profileType` options come from the `business_profile_type` option set.
Writes return `{ success, profileId, status, reason, message{en,hi} }`. `status` = PENDING / APPROVED /
REJECTED / BLOCKED; `reason` is the rejected/blocked reason (null otherwise) — **always show it**. On
create/update it's **PENDING** until an admin approves. **REJECTED** = editable (edit → back to PENDING);
**BLOCKED** = permanent (editing is refused). The profile object also carries `rejectReason` / `blockReason`.

## 4e. Notifications

```
GET   /api/v1/notifications?page=0&size=20     → page of {type, title{en,hi}, body{en,hi}, isRead, refType, refId}
GET   /api/v1/notifications/unread-count        → { count }
PATCH /api/v1/notifications/{id}/read
```
Show the unread badge; on tap, deep-link via `refType` + `refId` (e.g. BUSINESS_PROFILE).

## 5. Rules to remember
1. Upload field name is **`files`**.
2. Image URLs are relative (`/files/..`) → prepend the base URL to display.
3. `max 10` images, types jpg/png/webp — enforce from the IMAGE field's `validation`.
4. Two steps: **upload → get URLs → submit listing with URLs.** Photos and listing never travel together.
5. Everything is metadata-driven — render fields from §2, don't hardcode forms.

---

## Reference
Ready-to-run Postman collections are in the `postman/` folder (one per category) — import to see every request with sample bodies.
```
