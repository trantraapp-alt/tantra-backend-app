# Tantra — Frontend API Reference

> **Base URL (local):** `http://localhost:8080/api/v1`  
> On a phone/device on the same network use the machine's LAN IP, e.g. `http://192.168.1.5:8080/api/v1`.  
> In production, swap to the deployed domain. Keep this in one config constant.

---

## Response Envelope

Every endpoint returns the same wrapper. Read `data` for the payload:

```json
// success
{
  "success": true,
  "data": { ... },
  "message": { "en": "Done", "hi": "सफल" },
  "traceId": "abc123",
  "timestamp": "2026-07-30T10:00:00"
}

// error
{
  "success": false,
  "error": {
    "code": "NOT_FOUND",
    "message": { "en": "Listing not found.", "hi": "लिस्टिंग नहीं मिली।" }
  },
  "traceId": "abc123"
}
```

- Actual payload is always in `data`
- `traceId` also appears in `X-Trace-Id` response header — log it on error screens for support
- Build one API client that unwraps `data` and surfaces `error.message` globally

### Language
Add `?lang=EN` (default) or `?lang=HI` to any endpoint to get bilingual messages in the preferred language.

---

## Authentication

JWT bearer token. Obtain from Sign In. Pass in every authenticated request:

```
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

Token validity: **365 days**. No refresh token — re-login on expiry.

---

## Table of Contents

1. [Auth](#1-auth)
2. [Home Feed (Flipkart-style)](#2-home-feed)
3. [Global Search (Bilingual)](#3-global-search)
4. [Listings — Browse, Nearby, Detail](#4-listings)
5. [Contact Reveal (OLX-style)](#5-contact-reveal)
6. [Business Profile Directory](#6-business-profile-directory)
7. [Subscription Plans (View)](#7-subscription-plans)
8. [Self-Serve Payment (Buy a Plan)](#8-self-serve-payment)
9. [Admin — Subscription Plans CRUD](#9-admin-subscription-plans-crud)
10. [Admin — Grant / Revoke Subscriptions](#10-admin-grant--revoke-subscriptions)
11. [Admin — Promo / Feature Cards CRUD](#11-admin-promo--feature-cards-crud)
12. [Addresses](#12-addresses)
13. [Business Profiles (My Own)](#13-business-profiles)
14. [Notifications](#14-notifications)
15. [File Uploads](#15-file-uploads)
16. [Forms (Dynamic)](#16-forms-dynamic)
17. [Master Data](#17-master-data)
18. [ListingCard — Full Field Reference](#listingcard-full-field-reference)
19. [PromoCard — Field Reference](#promocard-field-reference)
20. [Premium Badge Rendering Guide](#premium-badge-rendering-guide)
21. [Navigation Architecture & Filter Developer Guide](#navigation-architecture--filter-developer-guide)
22. [Filter Reference](#filter-reference)
23. [Pagination](#pagination)
24. [Error Codes](#error-codes)
25. [DB Migration (One-Time)](#db-migration)

---

## 1. Auth

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| POST | `/auth/signup` | Public | Register |
| POST | `/auth/signin` | Public | Login → JWT |
| POST | `/auth/forgot-password/request` | Public | Send OTP |
| POST | `/auth/forgot-password/reset` | Public | Reset with OTP |
| GET | `/auth/profile` | JWT | Own profile |
| GET | `/auth/verify-session` | JWT | Token still valid? |

### Sign Up
```http
POST /auth/signup
Content-Type: application/json

{
  "mobileNumber": "9876543210",
  "password": "yourpassword",
  "firstName": "Ramesh",
  "lastName": "Kumar",
  "appUsageRole": "ROLE_USER",
  "preferredLanguage": "EN"
}
```

### Sign In
```http
POST /auth/signin
Content-Type: application/json

{
  "mobileNumber": "9876543210",
  "password": "yourpassword"
}
```

Response `data`:
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "userId": "TN3A1B2C",
  "firstName": "Ramesh"
}
```

---

## 2. Home Feed

> **Flipkart-style home page.** No auth required — show to all visitors.

```
GET /home?district=Pune&lang=EN
```

| Param | Type | Required | Description |
|-------|------|----------|-------------|
| `district` | string | No | Filter feed to a district |
| `lang` | EN / HI | No | Language (default EN) |

> Server caches this response per `district+lang`. Do not client-cache aggressively — the server handles it.

### Response `data` Structure

```json
{
  "generatedAt": "2026-07-30T10:00:00",

  "modules": [
    {
      "id": 1,
      "moduleKey": "crop",
      "moduleNameEn": "Crops",
      "moduleNameHi": "फसलें",
      "displayOrder": 1,
      "isActive": true
    }
  ],

  "promoCards": [
    {
      "cardId": "PC3A9BKD",
      "title": { "en": "Kharif Season Sale", "hi": "खरीफ सीजन सेल" },
      "subtitle": { "en": "Best seeds at best prices", "hi": "बेहतरीन दाम पर बेहतरीन बीज" },
      "imageUrl": "http://localhost:8080/files/promo1.jpg",
      "bgColor": "#FFF3E0",
      "textColor": "#212121",
      "ctaLabel": { "en": "Shop Seeds", "hi": "बीज देखें" },
      "ctaType": "CATEGORY",
      "ctaValue": "3",
      "displayOrder": 1,
      "targetDistrict": null,
      "isActive": true,
      "validFrom": "2026-06-01T00:00:00",
      "validTo": "2026-09-30T23:59:59"
    }
  ],

  "featuredListings": {
    "title": { "en": "Featured Listings", "hi": "विशेष लिस्टिंग" },
    "listings": [ ...ListingCard[] ]
  },

  "moduleSections": [
    {
      "module": { "id": 1, "moduleNameEn": "Crops", "moduleNameHi": "फसलें" },
      "topCategories": [
        {
          "id": 10,
          "categoryNameEn": "Wheat",
          "categoryNameHi": "गेहूँ",
          "iconUrl": "https://example.com/icon.png"
        }
      ],
      "listings": [ ...ListingCard[] ]
    }
  ],

  "featuredProfiles": [
    {
      "profileId": "BP4T1NRK92",
      "businessName": "Ramesh Agri Seeds",
      "profileType": "seed_dealer",
      "district": "Pune",
      "verificationStatus": "APPROVED",
      "isHighlighted": true,
      "highlightColor": "#FFA726",
      "badge": { "en": "Premium Seller", "hi": "प्रीमियम विक्रेता" },
      "planKey": "PREMIUM"
    }
  ],

  "recentListings": [ ...ListingCard[] ]
}
```

### Rendering the Home Screen
```
┌─────────────────────────────────────────┐
│  Module chips: [Crops] [Seeds] [Equip]  │
├─────────────────────────────────────────┤
│  FEATURED carousel (featuredListings)   │
│  ← premium sellers only, gold/orange   │
├─────────────────────────────────────────┤
│  MODULE SECTION: Crops                  │
│    Category chips: [Wheat] [Rice]...    │
│    Listing cards (6 items)              │
├─────────────────────────────────────────┤
│  FEATURED BUSINESS PROFILES             │
│    Profile cards with badges            │
├─────────────────────────────────────────┤
│  RECENTLY ADDED (recentListings)        │
└─────────────────────────────────────────┘
```

---

## 3. Global Search

> Bilingual full-text search. Hindi and English queries both work.

```
GET /search?q=wheat&district=Pune&lat=18.5204&lng=73.8567&radius=25&page=0&size=20
```

| Param | Type | Description |
|-------|------|-------------|
| `q` | string | Query — Hindi or English |
| `moduleId` | int | Filter by module |
| `categoryId` | int | Filter by category |
| `listingType` | SELL / RENT | |
| `minPrice` | decimal | |
| `maxPrice` | decimal | |
| `district` | string | |
| `lat` | double | User GPS latitude |
| `lng` | double | User GPS longitude |
| `radius` | int (km) | 5 / 10 / 25 / 50 / 100 — omit for All India |
| `sellerType` | ALL / SUBSCRIBED | SUBSCRIBED = premium sellers only |
| `postedWithin` | TODAY / WEEK / MONTH / ALL | |
| `lang` | EN / HI | |
| `page` | int | 0-indexed |
| `size` | int | Default 20, max 50 |

### Response `data`

```json
{
  "query": "wheat",
  "totalListings": 47,
  "totalPages": 3,
  "currentPage": 0,
  "pageSize": 20,
  "listings": [ ...ListingCard[] ],
  "businessProfiles": [
    {
      "profileId": "BP4T1NRK92",
      "businessName": "Ramesh Agri Seeds",
      "profileType": "seed_dealer",
      "district": "Pune",
      "isHighlighted": true,
      "highlightColor": "#FFA726",
      "badge": { "en": "Premium Seller", "hi": "प्रीमियम विक्रेता" },
      "planKey": "PREMIUM"
    }
  ]
}
```

> **Bilingual example:** `?q=गेहूँ` returns the same results as `?q=wheat` — both are indexed together.  
> Business profiles are only included on page 0 (max 6 results). Use `/business-profiles/directory` for dedicated profile browsing.

---

## 4. Listings

### 4a. Carousel Browse — by Category Key (Public)

> **Use this for carousel buttons** like "Browse Crops", "Browse Seeds", "Browse Equipment".  
> Accepts the stable string key so the frontend never needs to hard-code numeric IDs.

```
GET /listings/browse/{categoryKey}?page=0&size=20
```

**Auth: Public (no token needed)**

| `categoryKey` | What it shows |
|---|---|
| `crop` | All crop listings |
| `seed` | All seed listings |
| `fertilizer` | All fertilizer listings |
| `pesticide` | All pesticide listings |
| `equipment` | Equipment — both rent and sell (or filter with `listingType`) |
| `animal` | Animal listings |
| `repair` | Agri repair & maintenance |

**Equipment — split Rent vs Sell:**
```
GET /listings/browse/equipment?listingType=RENT&page=0&size=20
GET /listings/browse/equipment?listingType=SELL&page=0&size=20
```

**Optional filters (same as search):**

| Param | Type | Description |
|-------|------|-------------|
| `listingType` | SELL / RENT / BOTH | Filter by listing type |
| `minPrice` | decimal | Minimum price |
| `maxPrice` | decimal | Maximum price |
| `district` | string | Filter by district |
| `state` | string | Filter by state |
| `lat` + `lng` + `radius` | double / int km | Geo filter |
| `postedWithin` | TODAY / WEEK / MONTH / ALL | Recency filter |
| `page` | int | 0-indexed (default 0) |
| `size` | int | Page size (default 20) |

Returns: `Page<ListingCard>` — same shape as category browse.

---

### 4b. Browse by Category ID (with all filters)

```
GET /listings/category/{categoryId}?listingType=SELL&district=Pune&lat=18.5204&lng=73.8567&radius=25&page=0&size=20
```

**Auth: Public**

All the same filter params as Search (minus `q`). Premium sellers always appear first — no explicit sort param needed.

### 4c. Nearby Listings

```
GET /listings/nearby?lat=18.5204&lng=73.8567&radius=25&page=0&size=20
```

**Auth: JWT required**

- `lat` and `lng` are **required**
- `radius` defaults to 25 km; presets: `5` / `10` / `25` / `50` / `100`
- Each card includes `distanceKm` (e.g. `12.4`)
- Sort: premium sellers first, then closest within each tier

### 4d. Listing Detail

```
GET /listings/{listingId}
```

**Auth: JWT required**  
Side effect: increments `viewCount` on the listing.

### 4e. Similar Listings

```
GET /listings/{listingId}/similar?limit=6
```

**Auth: JWT required**  
Returns up to `limit` listings in the same category (premium-first).

### 4f. Listings by Seller (Public Storefront)

```
GET /listings/by-seller/{userId}?page=0&size=20
```

**Auth: Public**  
Shows all active listings from a specific seller. Use on seller profile screens.

### 4f. My Listings

```
GET /listings/mine?listingType=SELL&status=ACTIVE&page=0&size=20
```

**Auth: JWT required**

### 4g. Create Listing

```http
POST /listings
Authorization: Bearer <token>
Content-Type: application/json

{
  "categoryId": 1,
  "moduleId": 1,
  "listingType": "SELL",
  "actualPrice": 1000,
  "offeredPrice": 800,
  "quantity": 50,
  "unit": "kg",
  "isNegotiable": true,
  "contactNumber": "9876543210",
  "showContact": true,
  "images": ["http://localhost:8080/files/img1.jpg"],
  "addressId": "ADDR123",
  "attributes": {
    "variety": "Lokwan",
    "quality": "Grade A"
  }
}
```

### 4h. Update Listing

```http
PUT /listings/{listingId}
Authorization: Bearer <token>
Content-Type: application/json
```
Same body as create. Only owner can update.

### 4i. Delete Listing

```
DELETE /listings/{listingId}
```

**Auth: JWT required** (owner only). Soft delete.

---

## 5. Contact Reveal

> **OLX-style "Get Contact" flow.** No in-app payment. The buyer taps once, gets the seller's number, and deals offline.

```http
POST /listings/{listingId}/contact
Authorization: Bearer <token>
```

No request body.

### Response `data`

```json
{
  "contactNumber": "9876543210",
  "listingId": "LT3A9BKD",
  "sellerUserId": "TN3A1B2C"
}
```

### Rules
| Condition | Behaviour |
|-----------|-----------|
| Listing `showContact: false` | 400 — contact hidden |
| Listing not ACTIVE | 400 — listing unavailable |
| Same buyer, same listing, within 24h | Returns number, does NOT re-increment counter |
| Different buyer or after 24h | Increments `contactRevealCount` + logs event |

### UI Flow
```
Listing card → "Get Contact" button (requires login)
    ↓ POST /listings/{id}/contact
    ↓
Show phone number overlay
    + "Call" button  (tel: deeplink)
    + "WhatsApp" button  (https://wa.me/91{number})
    ↓
Deal happens offline — platform is done
```

---

## 6. Business Profile Directory

```
GET /business-profiles/directory?profileType=seed_dealer&district=Pune&lat=18.5204&lng=73.8567&radius=25&page=0&size=20
```

**Auth: Public**

| Param | Type | Description |
|-------|------|-------------|
| `profileType` | string | Filter by type (seed_dealer, vet_clinic, equipment_dealer…) |
| `district` | string | |
| `lat` | double | User GPS lat |
| `lng` | double | User GPS lng |
| `radius` | int (km) | Radius filter |
| `page` | int | 0-indexed |
| `size` | int | Default 20 |

Response is a paginated list of business profile cards, same `isHighlighted` / `highlightColor` / `badge` pattern as listings.

---

## 7. Subscription Plans

### All Plans (Public — for plan picker screen)

```
GET /subscriptions/plans
```

Response `data` — list of all active plans sorted by sort_weight:

**Default plan tiers (seeded in DB):**

| planKey | Monthly | Yearly | Max Listings | Home Feed | Sort Weight | Badge Color |
|---------|---------|--------|--------------|-----------|-------------|-------------|
| FREE | ₹0 | ₹0 | 5 | No | 99 (last) | None |
| BASIC | ₹99 | ₹999 | 20 | No | 70 | #90CAF9 (blue) |
| STANDARD | ₹299 | ₹2999 | 50 | 2 slots | 40 | #66BB6A (green) |
| PREMIUM | ₹599 | ₹5999 | Unlimited | 4 slots | 20 | #FFA726 (orange) |
| ENTERPRISE | ₹1499 | ₹14999 | Unlimited | 6 slots | 5 (top) | #FFD700 (gold) |

> Admin can add/edit/remove tiers at any time via the Admin Plan CRUD API (Section 9).

### My Active Subscription

```
GET /subscriptions/mine
Authorization: Bearer <token>
```

Returns the active `SellerSubscription` with nested plan details, or `null` if on FREE tier.

### My Subscription History

```
GET /subscriptions/mine/history
Authorization: Bearer <token>
```

Returns all subscriptions (ACTIVE / EXPIRED / CANCELLED / PENDING) newest first.

---

## 8. Self-Serve Payment (Buy a Plan)

Sellers can purchase a plan directly. Local test mode is active by default — no real money involved.

### Step 1 — Initiate

```http
POST /payments/initiate
Authorization: Bearer <token>
Content-Type: application/json

{
  "planKey": "PREMIUM",
  "billingCycle": "MONTHLY"
}
```

Response `data`:
```json
{
  "paymentId": "PAY3A9BKD",
  "orderId": "MOCK_ORDER_PAY3A9BKD",
  "amount": 599.00,
  "currency": "INR",
  "planKey": "PREMIUM",
  "billingCycle": "MONTHLY",
  "isTestMode": true,
  "razorpayKeyId": null
}
```

### Step 2 — Verify (completes the purchase)

```http
POST /payments/verify
Authorization: Bearer <token>
Content-Type: application/json

{
  "paymentId": "PAY3A9BKD",
  "razorpayOrderId": "MOCK_ORDER_PAY3A9BKD",
  "razorpayPaymentId": "TEST_PAY_001",
  "razorpaySignature": "any_string_in_test_mode"
}
```

On success the subscription is immediately ACTIVE. The seller's listings get premium sort + badge.

### Local Test Flow
```
POST /payments/initiate  →  get paymentId + orderId (MOCK_ORDER_xxx)
POST /payments/verify    →  pass any razorpayPaymentId string
→  Subscription ACTIVE instantly, no real money
```

### Production Razorpay Flow (when app.payment.test-mode=false)
```
POST /payments/initiate  →  get orderId + razorpayKeyId (real Razorpay values)
→  open Razorpay Checkout SDK with orderId + razorpayKeyId
→  user pays with UPI / card / wallet
→  SDK returns razorpayPaymentId + razorpaySignature
POST /payments/verify    →  HMAC verified → subscription granted
```

### My Payment History
```
GET /payments/mine
Authorization: Bearer <token>
```

---

## 9. Admin — Subscription Plans CRUD

Admin can create custom plan tiers (e.g. a GOLD plan between STANDARD and PREMIUM).  
All require `ROLE_ADMIN` JWT.

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/admin/subscription-plans` | All plans (active + inactive) |
| GET | `/admin/subscription-plans/{id}` | Single plan |
| POST | `/admin/subscription-plans` | Create new tier |
| PUT | `/admin/subscription-plans/{id}` | Update price / limits / badge |
| PATCH | `/admin/subscription-plans/{id}/toggle` | Activate / deactivate |
| DELETE | `/admin/subscription-plans/{id}` | Soft-delete (deactivates) |

### Create Plan Body
```json
{
  "planKey": "GOLD",
  "name": { "en": "Gold Seller", "hi": "गोल्ड विक्रेता" },
  "priceMonthly": 399,
  "priceYearly": 3999,
  "maxListings": 75,
  "homeFeedSlots": 3,
  "sortWeight": 30,
  "badgeLabel": { "en": "Gold Seller", "hi": "गोल्ड विक्रेता" },
  "highlightColor": "#FFD700",
  "listingHighlight": true,
  "profileHighlight": true,
  "isActive": true
}
```

> `planKey` must be unique and cannot be changed after creation. `sortWeight` determines listing sort order — lower = higher priority.

---

## 10. Admin — Grant / Revoke Subscriptions

For manual grants (trials, comps, offline payments). All require `ROLE_ADMIN` JWT.

### Grant Plan to Seller

```http
POST /admin/subscriptions/grant
Authorization: Bearer <admin-token>
Content-Type: application/json

{
  "userId": "TN3A1B2C",
  "planKey": "PREMIUM",
  "durationDays": 30,
  "paymentRef": "OFFLINE_CASH_001",
  "paymentGateway": "MANUAL",
  "autoRenew": false,
  "notes": "30-day complimentary trial"
}
```

Auto-cancels any existing active subscription before creating the new one.

### Revoke

```
POST /admin/subscriptions/{subscriptionId}/revoke?reason=Policy+violation
```

### List All Subscriptions

```
GET /admin/subscriptions?status=ACTIVE&page=0&size=20
```

`status` values: `ACTIVE` / `EXPIRED` / `CANCELLED` / `PENDING` / omit for all

### Admin Payment List

```
GET /payments/admin?status=CAPTURED&page=0&size=20
```

`status` values: `PENDING` / `CAPTURED` / `FAILED` / `REFUNDED`

---

## 11. DB-Driven Carousel

The home screen carousel buttons are stored in the database. The frontend reads them on startup and uses the `ctaValue` + `listingType` to build the browse URL.

### 11a. Public — Get Active Carousel Items

```
GET /carousel
```

**Auth: Public.** Returns active items in `displayOrder` ascending. Cached server-side.

Response `data` is an array:

```json
[
  {
    "id": 1,
    "label": { "en": "Browse Crops", "hi": "फसल देखें" },
    "iconUrl": null,
    "ctaType": "BROWSE_CATEGORY",
    "ctaValue": "crop",
    "listingType": null,
    "bgColor": "#E8F5E9",
    "textColor": "#1B5E20",
    "displayOrder": 1,
    "isActive": true
  },
  {
    "id": 4,
    "label": { "en": "Rent Equipment", "hi": "उपकरण किराए पर" },
    "ctaType": "BROWSE_CATEGORY",
    "ctaValue": "equipment",
    "listingType": "RENT",
    "bgColor": "#F3E5F5",
    "textColor": "#4A148C",
    "displayOrder": 4,
    "isActive": true
  }
]
```

**How the frontend uses each item:**

| `ctaType` | `ctaValue` | `listingType` | Navigate to |
|---|---|---|---|
| `BROWSE_CATEGORY` | `"crop"` | `null` | `GET /listings/browse/crop` |
| `BROWSE_CATEGORY` | `"equipment"` | `"RENT"` | `GET /listings/browse/equipment?listingType=RENT` |
| `BROWSE_CATEGORY` | `"equipment"` | `"SELL"` | `GET /listings/browse/equipment?listingType=SELL` |
| `BROWSE_MODULE` | `"agriculture"` | — | open module screen |
| `EXTERNAL_URL` | `"https://..."` | — | open in browser |
| `NONE` | — | — | display only, no tap |

**Default seeded buttons (7):**

| id | Label | ctaValue | listingType |
|---|---|---|---|
| 1 | Browse Crops | crop | — |
| 2 | Browse Seeds | seed | — |
| 3 | Browse Fertilizers | fertilizer | — |
| 4 | Rent Equipment | equipment | RENT |
| 5 | Buy Equipment | equipment | SELL |
| 6 | Browse Animals | animal | — |
| 7 | Agri Repair | repair | — |

---

### 11b. Admin — Carousel CRUD

All require `ROLE_ADMIN` JWT.

| Method | Endpoint | Description |
|---|---|---|
| GET | `/admin/carousel` | All items (incl. inactive) |
| GET | `/admin/carousel/{id}` | Single item |
| POST | `/admin/carousel` | Create |
| PUT | `/admin/carousel/{id}` | Full update |
| PATCH | `/admin/carousel/{id}/toggle` | Flip `isActive` |
| DELETE | `/admin/carousel/{id}` | Hard delete |

**Create / Update body:**

```json
{
  "label": { "en": "Browse Pesticides", "hi": "कीटनाशक देखें" },
  "iconUrl": "https://cdn.example.com/icons/pesticide.png",
  "ctaType": "BROWSE_CATEGORY",
  "ctaValue": "pesticide",
  "listingType": null,
  "bgColor": "#F9FBE7",
  "textColor": "#33691E",
  "displayOrder": 8,
  "isActive": true
}
```

Any write operation clears the carousel cache so the public endpoint reflects changes immediately.

---

## 12. Admin — Promo / Feature Cards CRUD

Admin creates promotional cards (banners, seasonal campaigns, announcements) that appear at the top of the home feed as a carousel.

All write endpoints require `ROLE_ADMIN` JWT.

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| GET | `/admin/promo-cards` | ADMIN | All cards (active + inactive) |
| GET | `/admin/promo-cards/{id}` | ADMIN | Single card |
| POST | `/admin/promo-cards` | ADMIN | Create |
| PUT | `/admin/promo-cards/{id}` | ADMIN | Full update |
| PATCH | `/admin/promo-cards/{id}/toggle` | ADMIN | Flip isActive |
| DELETE | `/admin/promo-cards/{id}` | ADMIN | Hard delete |
| GET | `/promo-cards?district=Pune` | **Public** | Active cards (home feed) |

### Create Promo Card Body
```json
{
  "title": { "en": "Kharif Season Sale", "hi": "खरीफ सीजन सेल" },
  "subtitle": { "en": "Best seeds at best prices", "hi": "बेहतरीन दाम पर बेहतरीन बीज" },
  "imageUrl": "http://localhost:8080/files/promo-banner.jpg",
  "bgColor": "#FFF3E0",
  "textColor": "#212121",
  "ctaLabel": { "en": "Shop Seeds", "hi": "बीज देखें" },
  "ctaType": "CATEGORY",
  "ctaValue": "3",
  "displayOrder": 1,
  "targetDistrict": null,
  "isActive": true,
  "validFrom": "2026-06-01T00:00:00",
  "validTo": "2026-09-30T23:59:59"
}
```

### `ctaType` Values
| Value | `ctaValue` format | Action |
|-------|-------------------|--------|
| `MODULE` | moduleId (integer as string) | Navigate to module listing screen |
| `CATEGORY` | categoryId (integer as string) | Navigate to category listing screen |
| `EXTERNAL` | full URL | Open in webview / browser |
| `NONE` | — | Display-only card, no tap action |

### Rendering the Promo Carousel
```
homeResponse.promoCards  →  horizontal scroll carousel at top of home screen
Each card:
  - Show imageUrl as background (if present) or bgColor
  - Title text in textColor
  - Subtitle (optional)
  - CTA button with ctaLabel text
  - On tap: navigate based on ctaType + ctaValue
  - Cards with validFrom/validTo are filtered server-side — just render what you receive
```

---

## 12. Addresses

All require JWT.

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/addresses` | All addresses for logged-in user |
| GET | `/addresses/default` | Default address |
| POST | `/addresses` | Create new (max 10 per user) |
| PUT | `/addresses/{addressId}` | Full update |
| PATCH | `/addresses/{addressId}/default` | Set as default |
| DELETE | `/addresses/{addressId}` | Soft delete |

### Address Object
```json
{
  "addressId": "ADDR3A1B2C",
  "fullAddress": "Survey No. 42, Village Shirur",
  "village": "Shirur",
  "taluka": "Shirur",
  "district": "Pune",
  "state": "Maharashtra",
  "pinCode": "412210",
  "latitude": 18.8300,
  "longitude": 74.3660,
  "isDefault": true
}
```

> When creating a listing, pass `addressId`. The backend copies address data and extracts lat/lng to top-level listing columns for geo indexing.

---

## 13. Business Profiles

All require JWT (for own profile management).

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| GET | `/business-profiles` | JWT | My profiles |
| POST | `/business-profiles` | JWT | Create (status → PENDING) |
| PUT | `/business-profiles/{id}` | JWT | Update (status → PENDING re-verify) |
| DELETE | `/business-profiles/{id}` | JWT | Soft delete |
| GET | `/admin/business-profiles` | ADMIN | All pending |
| POST | `/admin/business-profiles/{id}/approve` | ADMIN | Approve |
| POST | `/admin/business-profiles/{id}/reject` | ADMIN | Reject |
| POST | `/admin/business-profiles/{id}/block` | ADMIN | Block |

### Create Business Profile
```json
{
  "businessName": "Ramesh Agri Seeds",
  "profileType": "seed_dealer",
  "description": "Premium quality seeds since 1990",
  "contactNumber": "9876543210",
  "whatsappNumber": "9876543210",
  "website": "https://example.com",
  "addressId": "ADDR3A1B2C",
  "images": ["http://localhost:8080/files/logo.jpg"]
}
```

Profile goes to `PENDING` status and must be admin-approved before appearing in directory.

---

## 14. Notifications

All require JWT.

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/notifications?page=0&size=20` | Paginated list |
| GET | `/notifications/unread-count` | Badge count (integer) |
| PATCH | `/notifications/{id}/read` | Mark as read |

---

## 15. File Uploads

```http
POST /uploads
Authorization: Bearer <token>
Content-Type: multipart/form-data

files: file1.jpg, file2.jpg   (max 10 MB per file, JPG/PNG/WEBP only)
```

Response `data`:
```json
["http://localhost:8080/files/uuid-1.jpg", "http://localhost:8080/files/uuid-2.jpg"]
```

Files are served at `GET /files/{filename}` — public, no auth. Pass these URLs in the `images` array when creating listings or business profiles.

---

## 16. Forms (Dynamic)

The listing create/edit form fields are driven by the category. Fetch the form before showing the form screen.

```
GET /forms/render?categoryId=1&listingType=SELL&lang=EN
```

**Auth: JWT required**

Response `data`:
```json
{
  "formId": "...",
  "categoryId": 1,
  "listingType": "SELL",
  "fields": [
    {
      "fieldKey": "variety",
      "label": { "en": "Variety", "hi": "किस्म" },
      "fieldType": "TEXT",
      "isRequired": true,
      "placeholder": { "en": "e.g. Lokwan", "hi": "जैसे लोकवन" },
      "options": null,
      "displayOrder": 1
    },
    {
      "fieldKey": "quality",
      "label": { "en": "Quality", "hi": "गुणवत्ता" },
      "fieldType": "SELECT",
      "isRequired": false,
      "options": [
        { "value": "Grade A", "label": { "en": "Grade A", "hi": "ग्रेड ए" } },
        { "value": "Grade B", "label": { "en": "Grade B", "hi": "ग्रेड बी" } }
      ]
    }
  ]
}
```

`fieldType` values: `TEXT`, `NUMBER`, `SELECT`, `MULTISELECT`, `DATE`, `BOOLEAN`

Store field values in the listing's `attributes` map: `{ "variety": "Lokwan", "quality": "Grade A" }`.

---

## 17. Master Data

Public — no auth.

```
GET /master/modules              — All modules
GET /master/modules/{id}/categories  — Categories for a module (tree)
GET /master/districts            — All districts
GET /master/states               — All states
```

---

## ListingCard — Full Field Reference

Every listing returned from browse / search / home / nearby has this shape:

```json
{
  "listingId": "LT3A9BKD",
  "userId": "TN3A1B2C",
  "moduleId": 1,
  "categoryId": 5,
  "listingType": "SELL",

  "actualPrice": 1000.00,
  "offeredPrice": 800.00,
  "discountPct": 20.00,
  "quantity": 50,
  "unit": "kg",
  "isNegotiable": true,

  "showContact": true,
  "contactNumber": "9876543210",

  "images": ["http://localhost:8080/files/abc.jpg"],

  "address": {
    "fullAddress": "Village Shirur, Pune",
    "district": "Pune",
    "state": "Maharashtra",
    "village": "Shirur",
    "latitude": 18.5204,
    "longitude": 73.8567
  },

  "attributes": {
    "variety": "Lokwan",
    "quality": "Grade A"
  },

  "status": "ACTIVE",
  "viewCount": 45,
  "contactRevealCount": 12,
  "createdAt": "2026-07-15T10:30:00",
  "updatedAt": "2026-07-20T08:00:00",

  "distanceKm": 12.4,

  "isHighlighted": true,
  "highlightColor": "#FFA726",
  "sellerBadge": { "en": "Premium Seller", "hi": "प्रीमियम विक्रेता" },
  "sellerPlanKey": "PREMIUM"
}
```

### Key Fields Explained

| Field | When present | Notes |
|-------|--------------|-------|
| `distanceKm` | Nearby + category browse with lat/lng | Distance from user's location, 1 decimal |
| `isHighlighted` | Always | `true` for BASIC and above plans |
| `highlightColor` | When `isHighlighted=true` | CSS hex color for card border/accent |
| `sellerBadge` | When `isHighlighted=true` | Bilingual badge label map |
| `sellerPlanKey` | Always | FREE / BASIC / STANDARD / PREMIUM / ENTERPRISE / null |
| `discountPct` | When actualPrice > offeredPrice | Calculated: `(1 - offered/actual) * 100`, rounded |
| `contactRevealCount` | Always | Number of unique contact reveals (seller analytics) |

---

## Premium Badge Rendering Guide

### Logic (pseudocode)
```javascript
function renderListingCard(listing) {
  if (listing.isHighlighted) {
    card.borderColor = listing.highlightColor      // e.g. "#FFA726"
    badge.text = listing.sellerBadge[currentLang]  // e.g. "Premium Seller"
    badge.visible = true
  } else {
    card.borderColor = 'default'
    badge.visible = false
  }
}
```

### Plan → Visual
| planKey | Border / Accent Color | Badge Text (EN) | Card Treatment |
|---------|----------------------|-----------------|----------------|
| FREE | none | — | Standard card |
| BASIC | #90CAF9 (blue) | "Basic Seller" | Blue border |
| STANDARD | #66BB6A (green) | "Verified Seller" | Green border |
| PREMIUM | #FFA726 (orange) | "Premium Seller" | Orange border + top of list |
| ENTERPRISE | #FFD700 (gold) | "Top Seller" | Gold border + top of list |

### Sort Order
Sort is handled entirely **server-side**. Render cards in the order they arrive. ENTERPRISE/PREMIUM sellers appear at the top automatically because of `sort_weight` in the SQL ORDER BY.

### Home Feed Featured Section
`featuredListings` in the home feed contains only sellers with `home_feed_slots > 0` (STANDARD and above). Render this as a horizontal carousel with badges visible.

---

## PromoCard — Field Reference

Every card in `homeResponse.promoCards` or `GET /promo-cards`:

```json
{
  "cardId": "PC3A9BKD",
  "title": { "en": "Kharif Season Sale", "hi": "खरीफ सीजन सेल" },
  "subtitle": { "en": "Best seeds at best prices", "hi": "बेहतरीन दाम पर बेहतरीन बीज" },
  "imageUrl": "http://localhost:8080/files/promo1.jpg",
  "bgColor": "#FFF3E0",
  "textColor": "#212121",
  "ctaLabel": { "en": "Shop Seeds", "hi": "बीज देखें" },
  "ctaType": "CATEGORY",
  "ctaValue": "3",
  "displayOrder": 1,
  "targetDistrict": null,
  "isActive": true,
  "validFrom": "2026-06-01T00:00:00",
  "validTo": "2026-09-30T23:59:59"
}
```

| Field | Notes |
|-------|-------|
| `title` / `subtitle` / `ctaLabel` | Bilingual map — use `[currentLang]` key |
| `imageUrl` | Show as card background; fall back to `bgColor` if null |
| `bgColor` / `textColor` | CSS hex — use when imageUrl absent or as overlay |
| `ctaType` | `MODULE` / `CATEGORY` / `EXTERNAL` / `NONE` |
| `ctaValue` | Module ID, category ID, or external URL depending on ctaType |
| `displayOrder` | Cards arrive pre-sorted; render in received order |
| `targetDistrict` | Already filtered server-side; null = nationwide |
| `validFrom` / `validTo` | Already filtered server-side; always safe to render |

---

## Filter Reference

### `postedWithin`
| Value | Meaning |
|-------|---------|
| `TODAY` | Last 24 hours |
| `WEEK` | Last 7 days |
| `MONTH` | Last 30 days |
| `ALL` or omit | No date filter |

### `sellerType`
| Value | Meaning |
|-------|---------|
| `ALL` or omit | All sellers |
| `SUBSCRIBED` | Only paid plan sellers (BASIC+) |

### `listingType`
| Value | Meaning |
|-------|---------|
| `SELL` | For sale |
| `RENT` | For rent/hire |
| omit | Both types |

### Radius Presets (km)
Display a segmented control: **5 km → 10 km → 25 km → 50 km → 100 km → All India**  
Send as `radius=25`. Omit to disable radius filtering (All India).

---

## Pagination

All paginated endpoints accept `page` (0-indexed) and `size` (default 20). Response `data` structure:

```json
{
  "content": [ ...items ],
  "totalElements": 150,
  "totalPages": 8,
  "number": 0,
  "size": 20,
  "first": true,
  "last": false
}
```

Render a "Load More" button when `!last`. Infinite scroll: increment `page` on each request.

---

## Error Codes

| HTTP | Code | Meaning |
|------|------|---------|
| 400 | VALIDATION_ERROR | Bad request body / missing required field |
| 400 | CONTACT_HIDDEN | Listing's `showContact` is false |
| 400 | LISTING_INACTIVE | Listing not in ACTIVE status |
| 401 | UNAUTHORIZED | Missing / expired JWT |
| 403 | FORBIDDEN | Wrong role (e.g. non-admin hitting `/admin/*`) |
| 404 | NOT_FOUND | Resource doesn't exist |
| 409 | CONFLICT | Duplicate (e.g. mobile already registered) |
| 500 | INTERNAL_ERROR | Server error — show `traceId` to support |

---

## DB Migration

After first `mvn spring-boot:run` (which creates tables via JPA DDL), run once:

```bash
psql -U postgres -d tantra_db -f src/main/resources/db/V1__home_search_subscription.sql
```

This script:
1. Creates GIN index on `listings.search_vector` for full-text search
2. Seeds 5 subscription plans (FREE → ENTERPRISE) — idempotent (`ON CONFLICT DO UPDATE`)
3. Backfills `search_vector` for existing listings
4. Backfills `latitude` / `longitude` from address JSON for listings and business profiles
5. Creates all performance indexes (geo, price, subscription, contact reveal)

---

---

## 25. Wishlist (Favourites)

> Full guide: **`WISHLIST_FRONTEND_GUIDE.md`**  
> **Auth required** for all endpoints.

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/wishlist/{listingId}` | Add to wishlist |
| `DELETE` | `/wishlist/{listingId}` | Remove from wishlist |
| `GET` | `/wishlist` | All saved items (full listing cards, newest first) |
| `GET` | `/wishlist/{listingId}/status` | Is this listing wishlisted? |

### Add / Remove response `data`
```json
{ "en": "Listing added to wishlist!", "hi": "लिस्टिंग विशलिस्ट में जोड़ी गई!" }
```

### Status response `data`
```json
{ "listingId": "LT3A9BKD12", "wishlisted": true }
```

### Wishlist list response `data` — `WishlistItem[]`
```json
[
  {
    "listingId": "LT3A9BKD12",
    "savedAt": "2026-08-09T14:30:00",
    "listing": { ...ListingCard }
  },
  {
    "listingId": "LT9XDEL007",
    "savedAt": "2026-08-05T09:15:00",
    "listing": null
  }
]
```
`listing: null` = listing was deleted after it was saved — show "No longer available" placeholder.

**Listing status in wishlist:**

| `status` | UI |
|----------|----|
| `ACTIVE` | Normal card |
| `SOLD` | Dimmed + "Sold" red badge |
| `INACTIVE` | Dimmed + "Unavailable" grey badge |
| *(listing null)* | Deleted placeholder + remove button |

---

*Last updated: 2026-08-09 — Added: wishlist/favourites (§25)*
