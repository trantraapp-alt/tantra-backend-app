# Tantra — Functional Flow & Onboarding (read this first)

A single mental-model doc for anyone new to Tantra — backend or frontend. It explains **what the product
is, how it's built, and how every flow works end-to-end**, across all modules built so far. For exact API
requests/responses, see `FRONTEND_API.md`; for screen-by-screen frontend work, see
`FRONTEND_DEVELOPER_GUIDE.md` (+ `BUSINESS_PROFILE_FRONTEND_GUIDE.md`).

---

## 1. What Tantra is

Tantra is a **multi-vertical, multi-module marketplace** (think OLX for rural/agri India), fully
**bilingual (Hindi + English)** and built to grow **without app releases**.

The core idea: **everything the user sees — modules, categories, forms, dropdowns — is data, not code.**
Admins define it via API/DB; the app renders whatever the backend returns. Add a new crop, a new dropdown
value, a whole new category or module → it's a **data change**, not a deployment.

Two roles:
- **USER** — buys, sells, rents, and manages their own listings & business profiles.
- **ADMIN** — maintains the catalog (categories, dropdowns, forms) and verifies business profiles.

---

## 2. The building blocks (the whole system is 4 config tables + listings)

```
App Module            e.g. Agriculture, Animal & Livestock
   └─ Category (tree) e.g. Marketplace → Crop / Seed ...   (self-referencing parent_id)
        ├─ Form Definition   the fields to show for this category (JSON, versioned)
        └─ Option Sets/Items reusable dropdowns (Crop Type, Units, ...) — cascading via parent_item_id

User submits ─────────►  Listing   (one table for ALL categories: common columns + `attributes` JSONB)
```

- **Module** — a top-level vertical (Agriculture, Animal & Livestock).
- **Category** — a tree (`parentId`). Leaf categories carry a **form**. Each category has an `actionType`:
  `LISTING` (open a form → post a listing) or `BUSINESS_PROFILE` (open the dealer/vet profile flow).
- **Form Definition** — the list of fields for a category, stored as JSON and **versioned**. Editing a form
  is a data edit; the listing stamps the `formId`/`version` it was created with.
- **Option Sets / Items** — reusable dropdowns. Items can have a **parent** (`parentItemId`) → this powers
  **cascading dropdowns** (Crop Type → Crop Name, Country → State → District).
- **Listing** — ONE table for every category. Common fields (price, images, address, status) are typed
  columns; category-specific answers live in an `attributes` JSON blob keyed by each field's `fieldKey`.

Why one listings table + JSON? So a brand-new category needs **zero schema changes** — just seed its form
and dropdowns and it's live.

---

## 3. The modules today

### Module A — Agriculture (`agriculture`)
```
Agriculture
├─ Agriculture Marketplace
│   ├─ Crop        (Type → Name cascade: 6 types → 79 crops)
│   ├─ Seed        (Type → Name cascade: 9 types → 82 seeds)
│   ├─ Pesticide   (Category → Name cascade: 7 → 42; + Powder/Liquid type)
│   ├─ Fertilizer  (Category → Name cascade: 7 → 38; + Powder/Liquid type)
│   └─ Equipment   (Category → Name cascade: 8 → 28; RENT or SELL)
├─ Agriculture Services   (serviceType + Labour multiselect; own rate model)
└─ Repair & Maintenance   (repairType; visit charge)
```

### Module B — Animal & Livestock (`animal_livestock`)
```
Animal & Livestock
├─ Veterinary            (actionType = BUSINESS_PROFILE → opens the dealer/vet profile flow, linkKey vet_clinic)
└─ Animal Marketplace
    ├─ Poultry           (Category → Bird cascade: 2 → 19)
    ├─ Fishery           (flat: 24 fish)
    └─ Animal            (flat: 13 animals; RENT or SELL)
```

Everything above is **seeded data** (Postman `00_Agriculture_Seed`, `12_AnimalLivestock_Seed`,
`11_BusinessProfile`) — the frontend must read the tree from the API, never hardcode it.

---

## 4. Cascading dropdowns (the pattern used almost everywhere)

Many categories use a **parent → child** dropdown pair: pick a *type/category*, then only that group's
*names* show.

- The child field carries **`parentField`** = the parent field's `fieldKey`.
- Every option has its own **`id`** and a **`parent`** (the parent option's id).
- The app shows child options where `option.parent === (selected parent option's id)`. Options are already
  inlined in the form; big sets can be fetched on demand via
  `GET /option-sets/{setKey}/items?parentItemId=`.

Example: **Crop Type = Cereal → Crop Name shows Wheat/Paddy/Maize…** Submitted as
`attributes: { cropType:"cereal", cropName:"wheat" }`. If the parent = "Other", the child dropdown hides and
a free-text box appears. The parent + name fields are **locked once a listing is created** (identity fields).

---

## 5. End-to-end functional flows

### Flow 1 — Admin sets up the catalog (one-time / ongoing, no code)
```
Sign in (ADMIN)
   → Create Module               POST /masters/modules
   → Create Categories (tree)    POST /masters/categories   (parentId links the tree)
   → Create Option Sets + Items  POST /admin/option-sets , .../items/bulk   (cascading = parent items first)
   → Create Form Definition      POST /admin/forms          (fields JSON, version 1)
```
Result: the category is now fully live for users. To change a dropdown or add a field later → another admin
data call; no deployment.

### Flow 2 — User creates a listing (the main money path)
```
Sign in (USER)
   → Home: list Modules              GET /masters/modules
   → Tap module: Categories          GET /masters/modules/{id}/categories
   → Tap category: Subcategories     GET /masters/categories/{parentId}/subcategories
   → Leaf category → GET FORM        GET /categories/{catId}/form?listingType=SELL
        • render sections → fields by type, honour flags (required/readOnly/visibleWhen/parentField…)
        • cascading dropdowns filter child options by the picked parent
   → Upload photos FIRST             POST /uploads  → returns /files/... URLs
   → Submit the listing              POST /listings
        • common fields top-level (price, images, address)
        • category answers in `attributes` (keyed by fieldKey)
        • backend injects userId / listingId / audit, recomputes discount, derives RENT vs SELL
   → Response: { listingId, userId, message{en,hi} }
```
**Images are always a 2-step:** upload → get URLs → submit listing with URLs. Photos and listing never
travel in the same request.

### Flow 3 — User manages listings (My Listings)
```
GET /listings/mine?listingType=&status=&page=&size=     → paginated grid
Grid quick edit   PATCH /listings/{id}   (only inlineEditable fields: price/qty/description/status)
Full edit         PUT   /listings/{id}   (whole form; locked identity fields ignored; removed images auto-deleted)
Delete (soft)     DELETE /listings/{id}
Browse a category GET /listings/category/{catId}
```

### Flow 4 — Address book + snapshot
```
User saves up to 10 addresses (one default)    /addresses ...
On a listing, send addressId (or useDefaultAddress)
   → backend SNAPSHOTS the address onto the listing
   → editing/deleting the saved address later does NOT change past listings
```
Country/State/District are cascading dropdowns; "Use current location" = frontend geolocation + reverse-geocode.

### Flow 5 — Business Profile + verification (dealers & vets)  *(separate frontend workstream)*
```
User (dealer/vet)
   → GET form for a profile type      GET /business-profiles/form?profileType=vet_clinic
   → Create profile                   POST /business-profiles     → status PENDING
Admin
   → Approval Tracker (dashboard)     GET /admin/business-profiles/stats   → {total,pending,approved,rejected,blocked, reviewedByMe}
        • each count is a tile → tap opens that list: GET /admin/business-profiles?status=APPROVED&sort=verifiedAt,desc
   → Verification queue (PENDING)     GET /admin/business-profiles?status=PENDING
   → Approve / Reject / Block (+reason) POST /admin/business-profiles/{id}/approve | /reject | /block
   → Approve REMOVES it from the queue; owner NOTIFIED; profile APPROVED + verified badge (if visible → buyers see it)
   → Approval HISTORY                 GET /admin/business-profiles/history   (approved / rejected / blocked, newest first)
   → REJECT (fixable): owner can edit & resubmit → PENDING          | reason = rejectReason
   → BLOCK  (permanent, offensive content): owner CANNOT resubmit   | reason = blockReason
     both work on a PENDING or an already-APPROVED profile, and both auto-hide it from buyers
Every action returns { status, reason } and notifies the owner WITH the reason. Editing a BLOCKED profile is refused.
```
Veterinary is just a category with `actionType=BUSINESS_PROFILE` that opens this flow with
`profileType=vet_clinic`. Full detail: `BUSINESS_PROFILE_FRONTEND_GUIDE.md`.

### Flow 6 — Notifications
```
GET /notifications , GET /notifications/unread-count , PATCH /notifications/{id}/read
```
Users are notified on verification approve/reject; deep-link via `refType`+`refId`.

---

## 6. Cross-cutting rules (true for every endpoint)

- **One response envelope:** every response is `{ success, data, message{en,hi}, error{code}, traceId, timestamp }`.
  Always read the payload from **`data`**; on error read `error.code` + `error.message`.
- **Dual language:** all labels/messages are `{ en, hi }` — pick by the user's `preferredLanguage`.
- **traceId:** per-request id in every response + the `X-Trace-Id` header + every server log line → use it for support.
- **Audit log:** listing & address & verification writes are recorded server-side.
- **Caching:** form render + dropdowns are cached in-memory and evicted when an admin edits them.
- **Storage:** images saved via a storage abstraction (local now, Cloudflare R2 behind a flag later); listings store only URLs.
- **Auth:** all protected calls send `Authorization: Bearer <token>`.

---

## 7. Current state (what exists vs what's next)

**Built ✅:** config-driven engine (modules/categories/forms/option-sets), cascading dropdowns, both modules
seeded, listings (create/edit/patch/delete/mine/browse) with image upload + address snapshot, business
profiles + admin verification + verified-badge helper, in-app notifications, response envelope + traceId +
audit + caching, bilingual content.

**Next (not wired yet):** buyer browse/filter + nearby, contact-request → approval → phone-number reveal,
chat, seller ratings, wishlist, content moderation/reporting, payments/subscriptions, FCM push + SMS,
Flyway migrations, secrets → vault, and future modules (Real Estate, Local Wages, Construction, Marriage).

---

## 8. Where to look

| Need | Where |
|---|---|
| This mental model | `FUNCTIONAL_FLOW.md` (you're here) |
| Signup / login / roles / forgot-password | `AUTH_FRONTEND_GUIDE.md` |
| Screen-by-screen frontend (everything except business profile) | `FRONTEND_DEVELOPER_GUIDE.md` |
| Business Profile + verification frontend (self-contained) | `BUSINESS_PROFILE_FRONTEND_GUIDE.md` |
| Exact API request/response contract | `FRONTEND_API.md` |
| Runnable examples on a clean DB | `postman/` — seed order: `00_Agriculture_Seed` → `12_AnimalLivestock_Seed` → `11_BusinessProfile` |
| Backend roadmap / status | `PHASE1_PLAN.md` |
| Backend code | `src/main/java/com/hyperlocal/tantra/` — `modules/{forms,listing,master,business,address,notification,audit,upload}` + `common/` (envelope, traceId) |
