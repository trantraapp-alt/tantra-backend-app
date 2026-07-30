# Veterinary — Frontend Wiring (why the form isn't showing)

Quick fix guide for "the Veterinary form isn't showing." Veterinary is **not a listing** — it's a
**business-profile** category. So the *listing* form endpoint returns nothing for it, and nothing renders.

---

## The root cause
Veterinary is a category with **`actionType = "BUSINESS_PROFILE"`** (`linkKey = "vet_clinic"`). It has **no
listing form**. If the app calls the listing endpoint for it:
```
GET /api/v1/categories/{veterinaryCatId}/form?listingType=SELL     ❌  → error: "No active form for category…"
```
…there's nothing to render. That's the bug — the wrong endpoint is being called for this category.

---

## The fix: branch on `actionType`
Every category from navigation carries `actionType` (and `linkKey`). Decide what to open **on tap**:

```js
function onCategoryTap(category) {
  if (category.actionType === 'BUSINESS_PROFILE') {
    // Veterinary, and any other verified-professional category
    openBusinessProfileFlow({ profileType: category.linkKey });   // linkKey = 'vet_clinic'
  } else {
    // LISTING (default) — Crop, Poultry, Animal, …
    openListingForm(category.id);                                 // GET /categories/{id}/form
  }
}
```
Get `actionType` + `linkKey` from `GET /api/v1/masters/modules/{moduleId}/categories` (or subcategories).
For the Animal & Livestock module, **Veterinary** returns `actionType=BUSINESS_PROFILE`, `linkKey=vet_clinic`.

---

## The Veterinary form comes from the business-profile endpoint
```
GET /api/v1/business-profiles/form?profileType=vet_clinic     (Authorization: Bearer <token> REQUIRED)
→ data: { formId, version, title{en,hi}, sections:[ { key, title, fields:[…] } ] }
```
- **Send the auth token.** `/business-profiles/**` is authenticated — calling it without a bearer token
  fails, which also makes the form "not show." (The *listing* form endpoint is public; this one is not.)
- The vet form **falls back to the shared `business_profile` form**: businessName, ownerName, description,
  mobileNumber, email, gstNumber, registrationNumber, **photos (IMAGE, max 3, camera+gallery)**, isVisible,
  address. Render it with the **same metadata renderer** you use for listings.

---

## Render → submit (same engine as everything else)
1. Render `sections[] → fields[]` by `type`, honouring flags (`required`, `visibleWhen`, `parentField`, `help`, IMAGE, ADDRESS).
2. Photos: upload via `POST /uploads` (max 3) → URLs into `attributes.photos`.
3. Submit:
```
POST /api/v1/business-profiles            (create)
{ "profileType":"vet_clinic", "businessName":"...", "isVisible":true, "address":{...},
  "attributes":{ "ownerName":"Dr. ...", "description":"...", "mobileNumber":"...", "photos":["/files/.."] } }
→ data: { profileId, status:"PENDING", reason, message }
```
→ status **PENDING** until an admin approves. Any edit resets it to PENDING. Full detail:
`BUSINESS_PROFILE_IMPLEMENTATION_GUIDE.md`.

---

## "Form not showing" — checklist
1. **Calling `/categories/{id}/form` for Veterinary** instead of `/business-profiles/form?profileType=vet_clinic`. ← most common
2. **No auth token** on `/business-profiles/form` (it's authenticated; the listing form isn't).
3. Not reading `category.actionType` / `linkKey` from navigation, so the app treats Veterinary like a listing.
4. Wrong `profileType` (must be exactly `vet_clinic` — from the category's `linkKey`).
5. Not unwrapping `data` from the envelope (the form is at `response.data`, sections at `data.sections`).

---

## Backend is fine — it's a routing decision
The backend serves the vet form correctly at `GET /business-profiles/form?profileType=vet_clinic` (verified
live). "Veterinary" simply needs the frontend to open the **business-profile flow**, not the listing form —
that's the whole fix.

See also: `BUSINESS_PROFILE_IMPLEMENTATION_GUIDE.md` (build guide), `BUSINESS_PROFILE_FLOW.md` (state machine).
