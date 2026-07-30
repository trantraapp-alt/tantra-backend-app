# Phase 1 — Agriculture End-to-End (Backend + Frontend)

Goal: **Agriculture module 100% usable + production-ready.** Built on Phase-0 foundation.
Tags: ✅ already built · 🔧 adapt existing · 🆕 new

---

# BACKEND

## 0. Foundation (do FIRST — everything sits on this)
- [x] 🆕 Unified **response envelope** `{success, data, message{en,hi}, error{code}, traceId, timestamp}` + error-code catalog ✅ (`common/dto/ApiResponse`, `ErrorDetail`, `common/error/ErrorCode`)
- [x] 🔧 Global exception handler → all controllers auto-wrapped ✅ (`ResponseWrapperAdvice` wraps every response; `GlobalExceptionHandler` returns error envelope + code + traceId)
- [x] 🆕 Per-request **traceId** (MDC filter + `X-Trace-Id` header) ✅ (`common/web/TraceIdFilter`)
- [x] 🆕 **Structured logging** — traceId in every log line (`logging.pattern.level`); prod JSON via `LOGGING_STRUCTURED_FORMAT_CONSOLE=ecs` (Spring Boot native, no dep) ✅
- [x] 🆕 **`audit_log`** table + `AuditService` + wired into listing (create/update/delete) & address (create/update/default/delete) ✅ — extend to verify/moderate as those are built
- [x] 🆕 **Category tree**: `module_categories.parent_id` (self-ref) + tree reads (`GET /modules/{id}/categories?parentId=`, `GET /categories/{parentId}/subcategories`) + parent validation ✅ (in-place, backward-compatible — form/listing point to the leaf category, no FK change)
- [ ] 🆕 **Flyway** migrations (replace `ddl-auto=update`) + dev/staging/prod profiles
- [ ] 🆕 Secrets → **env/vault** (JWT secret, DB creds)
- [ ] 🆕 **i18n**: MessageSource bundles (en/hi) for system messages; keep `LocalizedText` (jsonb) for content
- [ ] 🆕 JWT **refresh token** + shorter access token; **rate limiting**; **@Valid** input validation; CORS restrict

## 1. Category / Forms / Dropdowns (Agriculture)
- [x] 🆕 Seed **category tree** across TWO modules ✅ — **Agriculture** (`00_Agriculture_Seed`): Marketplace(Crop/Seed/Pesticide/Fertilizer/Equipment) · Services(serviceType + Labour multiselect) · Repair(repairType). **Animal & Livestock** (`12_AnimalLivestock_Seed`): Veterinary(business-profile) + Animal Marketplace(Poultry/Fishery/Animal). Animal/Poultry/Fishery moved out of Agriculture into the new module.
- [x] ✅ Form engine (render + admin CRUD) — reuse ✅
- [x] 🔧 Forms + dropdowns for all subcategories seeded (both modules) ✅
- [x] 🆕 **Cascading dropdowns** (Category→Name): field `parentField` + option `id`/`parent`; Crop/Seed/Pesticide/Fertilizer/Equipment/Poultry are cascades; Fishery/Animal flat ✅
- [x] 🆕 Veterinary = business-profile category (`actionType=BUSINESS_PROFILE`, linkKey `vet_clinic`) ✅
- [ ] 🆕 **`filterable`** flag on form fields (drives buyer filters, config-driven)

## 2. Listing (adapt existing to tree + moderation)
- [ ] 🔧 Listing create/edit/patch/delete/mine → category tree + new envelope
- [ ] ✅ RENT/SELL derive, snapshot address, image upload/delete — done
- [ ] 🆕 Moderation **status** (DRAFT/PENDING_REVIEW/ACTIVE/REJECTED/BLOCKED) + category `moderation_required` flag
- [ ] 🆕 **`prohibited_term`** table + keyword check on create (BLOCK/FLAG)
- [ ] 🆕 Filter columns (`display_price`, state/city/district) + **GIN index** on attributes
- [ ] 🆕 **Geo/nearby** (PostGIS or lat/long+Haversine) — nearby listings

## 3. Business Profile + Verification (Agri dealers)
- [x] 🆕 `business_profile` (profileType from `business_profile_type` option set, status) + owner CRUD ✅ (`modules/business`)
- [x] 🆕 Verification workflow PENDING→APPROVED/REJECTED + **admin approval** queue/approve/reject + audit ✅
- [x] 🆕 Owner **notified** on approve/reject (in-app) ✅
- [ ] 🆕 **Verified badge** on listings — `getVerifiedProfiles(userId)` ready; embed into listing/browse responses pending
- [x] 🆕 Veterinary = a `BUSINESS_PROFILE` category (`actionType`/`linkKey=vet_clinic`) in the Animal & Livestock module — opens the business-profile flow, no separate form ✅

## 4. Moderation (Trust & Safety)
- [ ] 🆕 `listing_report` + `POST /listings/{id}/report` + duplicate-block + **threshold auto-hide**
- [ ] 🆕 Admin **moderation queue** (PENDING_REVIEW + reported) + approve/reject/block + audit + notify seller

## 5. Ratings / Reviews
- [ ] 🆕 `seller_rating` (seller_user_id, rated_by, **category_id**, rating, review, status) + `seller_rating_summary` (denormalized avg/count)
- [ ] 🆕 **Contact-gated** rating (only contact-approved buyer) + CRUD; review report/hide
- [ ] 🆕 Embed rating summary in listing/profile responses

## 6. Buyer + Contact
- [ ] 🆕 **Browse/filter** endpoint (category-scoped, nearby, pagination, metadata-driven filters) — buyer sees others' ACTIVE listings, not own
- [ ] 🆕 `contact_request` (REQUESTED→APPROVED/REJECTED) + endpoints; **contact reveal only after approval** (number privacy)
- [ ] 🆕 **My Orders** = contact-request tracking
- [ ] 🆕 Notifications for contact request/approval

## 7. Notification service (infra — build early)
- [x] 🆕 **In-app notifications**: `notification` table + `NotificationService` + list / unread-count / mark-read endpoints ✅ (`modules/notification`)
- [ ] 🆕 **FCM push** + **SMS gateway** delivery (replace dummy OTP) — layered on top later

## 8. Wishlist (small)
- [ ] 🆕 `wishlist` (favorites) — add/remove/list

---

# FRONTEND

## Core reusable pieces (build once, used everywhere)
- [ ] 🆕 **Generic dynamic-form renderer** — reads form metadata: types, `required`, `validation`, `visibleWhen`, `allowOther`, `readOnly`, `editableOnUpdate`, `inlineEditable`, **sections-as-steps** (wizard)
- [ ] 🆕 **Dual-language** (en/hi) — labels/options/messages from `{en,hi}` + user preference
- [ ] 🆕 Response/error handling for the **unified envelope** (traceId, error codes, bilingual toasts)
- [ ] 🆕 Auth: login, token store, **refresh token**, force-update check
- [ ] 🆕 Image **upload + camera capture** (multipart, `sources`/`capture` from metadata)

## Screens (from UI flow)
- [ ] 🆕 **Home** — module cards (Agriculture · Animal & Livestock · Real Estate / Local Wages later)
- [ ] 🆕 **Agriculture categories** — Marketplace / Services / Repair · **Animal & Livestock** — Veterinary / Animal Marketplace(Poultry/Fishery/Animal)
- [ ] 🆕 **Subcategory selector + dynamic form** (the "same for all subcategory" generic screen) → POST
- [ ] 🔧 **My Listings** — grid, inline edit (PATCH), full edit (PUT), delete (APIs built)
- [ ] 🆕 **Business Profile** — list/create/edit/delete (same page) + verification status/badge
- [ ] 🆕 **Buyer Browse/Filter** — category-scoped, **nearby**, filters rendered from metadata + **listing detail**
- [ ] 🆕 **Contact flow** — "Get Contact" → request → status; **Requests menu** (seller approves) → number revealed
- [ ] 🆕 **Ratings** — rate seller (category), see reviews; **Report/Flag** listing & review
- [ ] 🔧 **Address book** (APIs built) + **current location** (Geolocation → reverse-geocode → autofill) + cascading geo dropdowns
- [ ] 🆕 **My Orders** — contact-request tracking
- [ ] 🆕 **Wishlist**
- [ ] 🆕 **Notifications** — in-app list + push (FCM)
- [ ] 🆕 **Profile / drawer** menu (Profile, Your Listing, Wishlist, Business Profile, My Orders, Addresses, Payments*, Subscriptions*, Logout) — *later

---

# Definition of Done (Agriculture launchable)
1. Seller posts any Agri subcategory (metadata-driven form) → moderated → live
2. Buyer browses nearby, filters, sees verified badge + rating
3. Buyer requests contact → seller approves → number revealed (privacy kept)
4. Seller manages listings (My Listings) + business profile
5. Everything: unified response, bilingual, logged + audited, on Flyway migrations
6. Report/flag + admin moderation queue working

# Deferred to later phases
- Chat (Phase 1.5) · Payments/Subscriptions/Boost/contact-credits (Phase 2) · Real Estate (Phase 3, mostly config) · Local Wages/Construction/Marriage (config) · image-AI/ML moderation
