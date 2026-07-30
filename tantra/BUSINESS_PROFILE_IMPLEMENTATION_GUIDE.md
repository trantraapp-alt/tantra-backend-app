# Business Profile — Frontend Implementation Guide

Build-oriented, screen-by-screen guide for the **Business Profile + Verification** feature (the other
frontend workstream). Companion to `BUSINESS_PROFILE_FRONTEND_GUIDE.md` (contract) and
`BUSINESS_PROFILE_FLOW.md` (state machine). Base URL local: `http://localhost:8080`; read payload from
`data`; text is `{en,hi}`.

Reuses the **same metadata form renderer** as listings — if that's shared code, you're 80% done here.

---

## 0. What you're building
- **Owner side** (USER): create / view / **edit (→ back to PENDING)** / delete their business profiles; upload up to **3 photos**; see status + reason.
- **Admin side** (ADMIN): Approval Tracker + queue + history; **Approve / Reject / Block**.
- Ids are opaque + prefixed (`BP…`). Never build UI logic off the id.

---

## 1. Entry points
```
Profile ▸ Business Profile                 → "My Profiles" list (owner)
Tap a BUSINESS_PROFILE category (e.g. Veterinary in Animal & Livestock)
                                           → create flow, profileType prefilled = category.linkKey (vet_clinic)
```
Get `actionType` + `linkKey` from navigation (`GET /masters/modules/{id}/categories`).

---

## 2. Fetch the form (per profile type)
```
GET /api/v1/business-profiles/form?profileType={type}    (Authorization)
→ data: { formId, version, title{en,hi}, sections:[ {key,title,fields:[RenderField]} ] }
```
Profile-type picker options:
```
GET /api/v1/option-sets/business_profile_type/items   → [{ id, value, label{en,hi} }]  (seed_dealer, vet_clinic, …)
```
Flow: pick a profile type (skip if prefilled from a category) → GET the form for it → render.

---

## 3. Render the fields (reuse the generic renderer)
Same renderer as listings — map `type` → widget, honour `required / readOnly / fieldLength / validation /
visibleWhen / allowOther / parentField / help`. Today's business-profile form has these fields:

| field | type | notes |
|---|---|---|
| profileType | DROPDOWN | from `business_profile_type` (prefilled if from a category) |
| businessName | TEXT (required) | |
| ownerName, description | TEXT / TEXTAREA | |
| mobileNumber, email, gstNumber, registrationNumber | TEXT | contact section |
| **photos** | IMAGE, **max 3** | camera + gallery — see §4 |
| isVisible | BOOLEAN | "Show my profile to buyers" |
| address | ADDRESS | see §5 |

Submit split: **common** (`profileType, businessName, address, isVisible`) → top level; **everything else**
(ownerName, description, mobile, email, gst, registrationNumber, **photos**) → `attributes` keyed by fieldKey.

---

## 4. Photos (IMAGE field `photos`, max 3, camera + gallery)
Two-step, same as listings. The field's `validation` = `{ max:3, acceptedTypes:[jpg,png,webp], sources:[camera,gallery], capture:true }`.
```
POST /api/v1/uploads   (Authorization; multipart/form-data, field "files")  → data: { urls:[...] }
```
- **Enforce max 3 on the client.** Show both **Take Photo** (camera) and **Choose from Gallery**.
  - Web: `<input type="file" accept="image/*" capture="environment" multiple>` + `<input type="file" accept="image/*" multiple>`.
- Put the returned URLs into `attributes.photos`. Display: `BASE_URL + url`.

```js
async function pickAndUpload(files) {
  if (currentPhotos.length + files.length > 3) return toast('Max 3 photos');
  const form = new FormData(); [...files].forEach(f => form.append('files', f));
  const { data } = await api.post('/uploads', form);   // no manual Content-Type
  currentPhotos = [...currentPhotos, ...data.urls].slice(0, 3);
}
```

---

## 5. Address block (ADDRESS field)
Collect `fullAddress, country, state, district, city, village, pinCode, latitude, longitude, mobileNumber,
altMobileNumber`. Country/State/District are cascading dropdowns (`parentField`); City/Village free-text.
Send as the **top-level `address`** object. (This is the *business's own* address — not the address book.)

---

## 6. Submit (create / edit)
```
POST /api/v1/business-profiles                 // create
PUT  /api/v1/business-profiles/{profileId}     // edit
{
  "profileType":"vet_clinic", "businessName":"Rampur Vet Clinic", "isVisible":true,
  "address": { ... },
  "attributes": { "ownerName":"Dr. Ravi", "description":"...", "mobileNumber":"...", "email":"...",
                  "gstNumber":"...", "registrationNumber":"...", "photos":["/files/1.jpg","/files/2.jpg"] }
}
→ data: { success, profileId, status, reason, message{en,hi} }
```
Every write returns `status` **and `reason`** — always surface them.

**Edit pre-fill:** `GET .../form?profileType=` + `GET .../{profileId}` → common from top level, the rest from
`attributes[fieldKey]` (including `photos`).

---

## 7. Status + reason handling (IMPORTANT)
Read `status` (and `reason`) from every write and from the profile object (`rejectReason` / `blockReason`).

| status | UI |
|---|---|
| `PENDING` | "Under review" chip; not public yet |
| `APPROVED` | **verified badge**; toggle `isVisible` to show/hide to buyers |
| `REJECTED` | show **Rejected Reason** + **[Edit & Resubmit]** (edit → PENDING) |
| `BLOCKED` | show **Blocked Reason**; **edit disabled** (a `PUT` is refused) — offer a support link |

> **Every content change needs re-approval.** Create **and any edit** (including editing photos or toggling
> `isVisible`) sets the profile back to **PENDING** — not verified/public again until an admin re-approves.
> Show "Under review" after any save. (Delete is the owner's own removal — no approval.)

---

## 8. My Profiles screen (owner)
```
GET /api/v1/business-profiles/mine            → list (cards with status chip + reason + verified badge)
GET /api/v1/business-profiles/{profileId}     → detail (owner always; others only if APPROVED + isVisible)
PUT /api/v1/business-profiles/{profileId}     → edit (→ PENDING)
DELETE /api/v1/business-profiles/{profileId}  → soft delete
```
Card actions by status: Edit (all except BLOCKED) · Resubmit (REJECTED) · Delete (all). Show the reason on
REJECTED/BLOCKED.

---

## 9. Admin verification (role ADMIN)
```
GET  /api/v1/admin/business-profiles/stats                          → tracker counts (tiles)
GET  /api/v1/admin/business-profiles?status=PENDING&page=&size=     → the review queue
GET  /api/v1/admin/business-profiles/history?page=&size=           → APPROVED + REJECTED + BLOCKED (newest review first)
GET  /api/v1/admin/business-profiles?status=APPROVED&sort=verifiedAt,desc   → tracker drill-down (also REJECTED/BLOCKED)
POST /api/v1/admin/business-profiles/{id}/approve
POST /api/v1/admin/business-profiles/{id}/reject   { "reason":"..." }   // fixable — owner can resubmit
POST /api/v1/admin/business-profiles/{id}/block    { "reason":"..." }   // permanent — owner cannot resubmit
```
- **Tracker** (`/stats`): render count tiles `{ total, pending, approved, rejected, blocked, reviewedByMe:{approved,rejected,blocked} }`; tap a tile → the matching `?status=` list.
- **Review a profile:** show fields + **photos** + address, then Approve / Reject(reason) / Block(reason).
- Reject **and** Block also work on an already-APPROVED profile (take-down); both auto-hide it from buyers and notify the owner with the reason. Show both buttons on approved rows in history.

---

## 10. Notifications (owner)
```
GET   /api/v1/notifications , GET /api/v1/notifications/unread-count , PATCH /api/v1/notifications/{id}/read
```
Owner is notified on approve / reject / block (with the reason). `refType=BUSINESS_PROFILE`, `refId=profileId`
→ deep-link to the profile.

---

## 11. Build checklist
- [ ] API client (envelope unwrap, bearer, surface `error`) — shared with listings
- [ ] Profile-type picker from `business_profile_type`
- [ ] Metadata form renderer (types + flags + cascading) for `GET .../form`
- [ ] **Photos**: upload max 3, camera + gallery → `attributes.photos`
- [ ] Address block (cascading geo dropdowns)
- [ ] Submit split: common top-level vs `attributes`; Create + Edit
- [ ] **Status UI**: PENDING chip · APPROVED badge · REJECTED reason+resubmit · BLOCKED reason (no edit) — always show `reason`
- [ ] **Any edit → PENDING** re-approval messaging
- [ ] Entry from Profile ▸ Business Profile AND from a `BUSINESS_PROFILE` category (prefill `profileType`)
- [ ] (Admin) Approval Tracker tiles → drill-down; Queue / History; Approve / Reject / Block (with reason)
- [ ] Verification notifications (badge + deep-link)

---

## Reference
- `BUSINESS_PROFILE_FRONTEND_GUIDE.md` — endpoint/field contract
- `BUSINESS_PROFILE_FLOW.md` — lifecycle state machine & journeys
- `postman/11_BusinessProfile.postman_collection.json` — the whole flow runnable
