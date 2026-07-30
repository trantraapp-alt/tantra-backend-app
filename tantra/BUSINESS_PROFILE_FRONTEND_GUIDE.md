# Tantra — Business Profile Frontend Guide (self-contained)

**Owner:** the Business Profile developer. This is everything you need to build the **Business Profile +
Verification** feature end-to-end, without reading the main guide. (The other developer owns the rest of the
app; the only shared surface is the API client and the form renderer described below — build them the same way.)

Dealers, shops and vets create a **Business Profile** → an **Admin verifies** it (approve/reject) → the
profile gets a **verified badge**. A user can have several profiles.

- **Base URL (local):** `http://localhost:8080` — on a phone use the LAN IP; prod = deployed domain. Keep it in ONE constant.
- **Postman:** `postman/11_BusinessProfile.postman_collection.json` — the full flow (seed types → create → verify). Test users: Admin `9000000001 / Admin@123`, Seller `9000000002 / Seller@123`.
- **Functional flow (states, journeys, screens):** `BUSINESS_PROFILE_FLOW.md` — read it first for the big picture.
- **Detailed contract:** `FRONTEND_API.md` §4d.

---

## 0. Shared foundations (build these first — same as the rest of the app)

**A. One response envelope — read your payload from `data`.**
```json
// success
{ "success": true, "data": { ... }, "message": {"en":"..","hi":".."}, "traceId":"ab12", "timestamp":".." }
// error
{ "success": false, "error": { "code":"NOT_FOUND", "message": {"en":"..","hi":".."} }, "traceId":"ab12" }
```
Build ONE API client that unwraps `data`, throws on `success:false` surfacing `error.code` +
`error.message`, and reads `traceId` (also in the `X-Trace-Id` header) for error/support screens.

**B. Dual language.** All labels/messages are `{ "en": "...", "hi": "..." }`. Pick by the user's
`preferredLanguage`. Toasts use the response `message`.

**C. Config-driven.** The business-profile **form is metadata-driven** — you render it from backend data
(§2), you do **not** hardcode fields. New field or profile type = backend change, no app release.

**D. Auth.** All calls send `Authorization: Bearer <token>`.
```
POST /api/v1/auth/signin { mobileNumber, password }  → data: { token, role, userId }
```
Store `data.token`. Roles: `USER` (creates profiles), `ADMIN` (verifies them).

---

## 1. Where the feature is reached

Two entry points, both landing on the same create/edit flow:
1. **Profile ▸ Business Profile** — user manages their own profiles.
2. **A category with `actionType = "BUSINESS_PROFILE"`** — e.g. **Veterinary** (in the Animal & Livestock
   module). When the user taps it, open the create flow prefilling `profileType = category.linkKey`
   (Veterinary → `vet_clinic`). You get `actionType` and `linkKey` on the category object from navigation
   (the other dev's screens hand you these — or read them yourself:
   `GET /api/v1/masters/modules/{moduleId}/categories`).

---

## 2. Render the form (metadata-driven — the core of this feature)

```
GET /api/v1/business-profiles/form?profileType={type}      (Authorization)
```
- Returns the form as `sections[] → fields[]`, **per profile type**, falling back to a shared
  `business_profile` form when a type-specific one isn't configured.
- `profileType` values come from the `business_profile_type` option set — fetch them to build the
  "Profile Type" picker:
  ```
  GET /api/v1/option-sets/business_profile_type/items
     → [{ id, value, label{en,hi}, parent }]   // seed_dealer, fertilizer_shop, vet_clinic, ...
  ```

**Render each field by `type`:**

| field.type | Render as |
|---|---|
| TEXT / TEXTAREA / NUMBER / DECIMAL | input / textarea |
| DROPDOWN / RADIO | `field.options[]` — each `{ id, value, label{en,hi}, parent }` |
| MULTISELECT / CHECKBOX_GROUP | multi-select from `options` |
| BOOLEAN | switch (e.g. `isVisible`) |
| IMAGE | photo uploader — the `photos` field, **max 3**, camera + gallery (§3) |
| ADDRESS | address block (§4) |

**Honour every per-field flag:** `required`, `readOnly`, `fieldLength`, `validation`
(`min/max/maxLength/regex/acceptedTypes`), `visibleWhen {field,operator,value}` (conditional show),
`allowOther` (show a “Please Specify” box on the `__other__` option), `parentField` (cascading — §2a),
`editableOnUpdate` (lock on edit).

### 2a. Cascading dropdowns (Country→State→District inside the address, or any typed field)
A child field carries **`parentField`** = the driving field's `fieldKey`; each option has its own **`id`**
and a **`parent`** (the parent option's id). When the user picks the parent, take its `id` and show only
child options where `option.parent === pickedParentId`. Options are inlined; for big sets you may instead
fetch `GET /api/v1/option-sets/{setKey}/items?parentItemId={pickedParentId}`.

---

## 3. Business photos upload (two-step, camera + gallery, **max 3**)

The form has an IMAGE field **`photos`** — up to **3** photos of the shop/business. Same 2-step flow as
listings: upload files first → get URLs → put the URLs in the submit payload. **Files and profile never
travel together.**
```
POST /api/v1/uploads     (Authorization; multipart/form-data, field name "files", jpg/png/webp)
   → data: { urls: ["/files/ab12.jpg", ...] }
```
- The `photos` field's `validation` carries the rules: `max: 3`, `acceptedTypes: [jpg,png,webp]`,
  `sources: ["camera","gallery"]`, `capture: true` — **enforce max 3 on the client** and show **both**
  camera-capture and gallery-pick.
  - Web: `<input type="file" accept="image/*" capture="environment" multiple>` (Take Photo) + a plain
    `<input type="file" accept="image/*" multiple>` (Gallery). Mobile: native camera/gallery picker.
- Don't set `Content-Type` (the browser sets the multipart boundary).
- Display: `BASE_URL + url` (public, no token).
- Put the returned URLs into **`attributes.photos`** (the IMAGE field's `fieldKey`). It's not a common
  field, so it goes inside `attributes`.

---

## 4. Address block

The form includes an ADDRESS field. Collect: `fullAddress, country, state, district, city, village,
pinCode, latitude, longitude, mobileNumber, altMobileNumber`. Country/State/District are cascading
dropdowns (§2a); City/Village free-text. "Use Current Location" (optional): Geolocation → reverse-geocode →
auto-fill + keep lat/long. Send the whole object as the top-level `address` field on submit (§5).

---

## 5. Create / read / edit / delete (owner)

**Submit shape:** common fields at the **top level** (`profileType, businessName, address, isVisible`);
everything else in **`attributes`** (ownerName, description, mobileNumber, email, gstNumber,
registrationNumber, **photos**…), keyed by each field's `fieldKey` from §2.

```
POST /api/v1/business-profiles          (Authorization)
{
  "profileType": "vet_clinic",
  "businessName": "Rampur Veterinary Clinic",
  "isVisible": true,
  "address": { "fullAddress":"...", "country":"IN", "state":"MP", "district":"Bhopal",
               "city":"Bhopal", "pinCode":"462001", "mobileNumber":"9000000002" },
  "attributes": {
    "ownerName":"Dr. Ravi Kumar", "description":"Large & small animal care",
    "mobileNumber":"9000000002", "email":"ravi@example.com",
    "gstNumber":"22ABCDE1234F1Z5", "registrationNumber":"VET-MP-1234",
    "photos":["/files/shop1.jpg","/files/shop2.jpg"]   // max 3, from §3
  }
}
→ data: { success:true, profileId:"TN...", status:"PENDING", message:{en,hi} }
```
Every write returns `{ success, profileId, status, reason, message{en,hi} }` inside `data`.

| Action | Call | Notes |
|---|---|---|
| Create | `POST /api/v1/business-profiles` | status starts **PENDING** |
| My profiles | `GET /api/v1/business-profiles/mine` | list for the manage screen |
| View one | `GET /api/v1/business-profiles/{profileId}` | owner always; others only if APPROVED + `isVisible` |
| Edit | `PUT /api/v1/business-profiles/{profileId}` | **any change resets status to PENDING** → back to admin approval |
| Delete | `DELETE /api/v1/business-profiles/{profileId}` | soft delete (owner removes their own) |

> **Every content change needs re-approval.** Create **and** edit (including editing photos or toggling
> `isVisible`) set the profile back to **PENDING** — it is not public/verified again until an admin
> re-approves. Show the "Under review" state after any edit. (Delete is the owner's own removal — no approval.)

**Edit pre-fill:** `GET .../form?profileType=` (§2) + `GET .../{profileId}` → common fields from the top
level, the rest from `attributes[fieldKey]`.

**Status → UI.** Every write and every profile read returns the current `status` **and its `reason`**
(`data.reason` on writes; `rejectReason` / `blockReason` on the profile object) — always show the reason so
the user knows *why*:
- `PENDING` — "Under review" chip; not yet public; `reason` = null.
- `APPROVED` — **verified badge**; if `isVisible`, buyers can see it; `reason` = null.
- `REJECTED` — show the **Rejected Reason** + an **"Edit & resubmit"** CTA (editing sends it back to PENDING).
- `BLOCKED` — show the **Blocked Reason**; **permanent** — the edit/resubmit CTA is **disabled** (a `PUT`
  returns an error: "blocked by the admin and cannot be edited"). Offer a support/contact link instead.

---

## 6. Verification (Admin panel side — role `ADMIN`)

If you also build the admin verification screen (otherwise skip). Two lists + three actions:
```
GET  /api/v1/admin/business-profiles?status=PENDING&page=0&size=20   → PENDING queue (page → data.content)
GET  /api/v1/admin/business-profiles/history?page=0&size=20          → APPROVED + REJECTED + BLOCKED, newest review first
POST /api/v1/admin/business-profiles/{profileId}/approve             → APPROVED, notifies owner
POST /api/v1/admin/business-profiles/{profileId}/reject  { "reason":"..." }  → REJECTED (fixable), notifies owner
POST /api/v1/admin/business-profiles/{profileId}/block   { "reason":"..." }  → BLOCKED (permanent), notifies owner
```
These are `ADMIN`-only (403 otherwise). Every action writes an audit record + pushes an in-app notification
to the owner **with the reason**, and the write response carries `status` + `reason`.

**Approval Tracker (dashboard landing).** One call powers the dashboard's count tiles:
```
GET /api/v1/admin/business-profiles/stats
   → data: {
       total, pending, approved, rejected, blocked,
       reviewedByMe: { approved, rejected, blocked }   // this admin's own tally
     }
```
Render a tile per count (Pending / Approved / Rejected / Blocked). **Tapping a tile opens the matching
list** — reuse the queue endpoint with that status, sorted by most-recent action:
```
GET /api/v1/admin/business-profiles?status=APPROVED&sort=verifiedAt,desc&page=0&size=20
```
(same for `REJECTED` / `BLOCKED`; `PENDING` is the review queue). `reviewedByMe` lets you show "You approved
N / rejected N / blocked N" for the logged-in admin.

**Two-list model behind the tracker:**
- **Queue** (`?status=PENDING`, the default) = profiles awaiting review. **Approving removes it from the queue.**
- **History** (`/history`) = everything already acted on (APPROVED / REJECTED / BLOCKED), each row carrying
  `verificationStatus`, `verifiedBy`, `verifiedAt`, `rejectReason`, `blockReason`. (The per-status drill-down
  above is the same data filtered to one status.)

**Reject vs Block (both work on a PENDING *or* an already-APPROVED profile; both auto-hide it from buyers):**
| Action | Meaning | Owner can resubmit? | Reason field |
|---|---|---|---|
| **Reject** | Fixable issue (unclear docs, wrong info) | ✅ Yes — edit → back to PENDING | `rejectReason` |
| **Block** | Offensive / policy-violating content | ❌ No — edit is refused | `blockReason` |

On **approved** rows in History show both a **"Reject"** and a **"Block"** button (each opens a reason
prompt). Blocking is the permanent take-down for offensive content. A `reason` is required for both so the
owner is told exactly why.

---

## 7. Notifications (verification results)

The owner is notified in-app when their profile is approved/rejected. Show the unread badge and deep-link.
```
GET   /api/v1/notifications?page=0&size=20   → page of { type, title{en,hi}, body{en,hi}, isRead, refType, refId, createdAt }
GET   /api/v1/notifications/unread-count      → { count }
PATCH /api/v1/notifications/{id}/read
```
On tap, if `refType == "BUSINESS_PROFILE"`, open that profile (`refId` = profileId).

---

## 8. Endpoint quick reference (this feature)

| Area | Endpoints |
|---|---|
| Auth | `POST /auth/signin` |
| Profile-type list | `GET /option-sets/business_profile_type/items` |
| Form | `GET /business-profiles/form?profileType=`, `GET /option-sets/{key}/items?parentItemId=` |
| Uploads | `POST /uploads`, view `GET /files/{name}` |
| Owner CRUD | `POST /business-profiles`, `GET /business-profiles/mine`, `GET/PUT/DELETE /business-profiles/{id}` |
| Admin tracker | `GET /admin/business-profiles/stats` (counts → tiles) |
| Admin verify | `GET /admin/business-profiles?status=` (queue/drill-down), `GET /admin/business-profiles/history`, `POST .../{id}/approve`, `POST .../{id}/reject` (fixable), `POST .../{id}/block` (permanent) |
| Notifications | `GET /notifications`, `GET /notifications/unread-count`, `PATCH /notifications/{id}/read` |

---

## 9. Build checklist
- [ ] API client (envelope unwrap, error surface, bearer token) — shared with the other dev; build identically
- [ ] Profile-type picker from `business_profile_type`
- [ ] Generic metadata form renderer (types + all flags + cascading) for `GET .../form`
- [ ] Business **photos** upload (max 3, camera + gallery) → URLs into `attributes.photos`
- [ ] Create / My Profiles / View / Edit (**any change → PENDING re-approval**) / Delete
- [ ] Status UI: PENDING chip · APPROVED verified badge · REJECTED reason + resubmit · BLOCKED reason (no resubmit) — always show `reason`
- [ ] Entry from **Profile ▸ Business Profile** AND from a `BUSINESS_PROFILE` category (prefill `profileType = linkKey`)
- [ ] (Admin) **Approval Tracker** dashboard — count tiles from `/stats` (pending/approved/rejected/blocked + "reviewed by me"), each tile drills into `?status=` list
- [ ] (Admin) PENDING queue + approve/reject
- [ ] (Admin) Approval History (approved / rejected / blocked) + **Reject** (fixable) and **Block** (permanent, offensive) on approved rows → auto-hidden, owner notified with reason
- [ ] Verification notifications (badge + deep-link)
