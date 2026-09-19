# Tantra — Frontend Developer Guide: Home Screen

> **App concept:** Flipkart / OLX–style hyperlocal agri-marketplace for rural India.  
> Users buy, sell, and rent crops, seeds, fertilizers, pesticides, equipment, vegetables, animals, poultry, and fishery items.  
> **Base URL:** `http://<host>/api/v1`  
> **Auth:** JWT Bearer token in `Authorization: Bearer <token>` header.

---

## Table of Contents

1. [Architecture Overview](#1-architecture-overview)
2. [Home Feed API](#2-home-feed-api)
3. [Rendering the Home Screen](#3-rendering-the-home-screen)
4. [Search API](#4-search-api)
5. [Browse by Category API](#5-browse-by-category-api)
6. [Nearby Listings API](#6-nearby-listings-api)
7. [Location & Geo Features](#7-location--geo-features)
8. [Filter Panel Reference](#8-filter-panel-reference)
9. [ListingCard Component](#9-listingcard-component)
10. [PromoCard Carousel](#10-promocard-carousel)
11. [Premium / Subscription Badges](#11-premium--subscription-badges)
12. [Listing Detail & Contact Reveal](#12-listing-detail--contact-reveal)
13. [Pagination](#13-pagination)
14. [Bilingual Support (EN / HI)](#14-bilingual-support-en--hi)
15. [API Response Envelope](#15-api-response-envelope)
16. [Error Handling](#16-error-handling)
17. [Complete Screen-by-Screen Reference](#17-complete-screen-by-screen-reference)

---

## 1. Architecture Overview

```
┌──────────────────────────────────────────────────────────────────────┐
│                         HOME SCREEN                                  │
│                                                                      │
│  [Search Bar]  ──────────────────────────►  GET /api/v1/search      │
│  [Location Bar: District / GPS Range]                                │
│                                                                      │
│  [Promo Carousel]    ◄──── promoCards[]                             │
│  [Module Tabs]       ◄──── modules[]                                │
│  [Featured Listings] ◄──── featuredListings.listings[]              │
│  [Module Sections]   ◄──── moduleSections[]                         │
│  [Featured Profiles] ◄──── featuredProfiles[]                       │
│  [Recent Listings]   ◄──── recentListings[]                         │
│                                                                      │
│  All sourced from a single call: GET /api/v1/home                   │
└──────────────────────────────────────────────────────────────────────┘
```

---

## 2. Home Feed API

### `GET /api/v1/home`

**Public — no auth required.**

| Query Param | Type   | Required | Description                                      |
|-------------|--------|----------|--------------------------------------------------|
| `district`  | string | No       | Filter feed to a specific district (e.g. `Pune`) |
| `lang`      | string | No       | `EN` (default) or `HI`                           |

**Example Calls:**

```
# Default — nationwide feed in English
GET /api/v1/home

# Localized feed for Pune district in Hindi
GET /api/v1/home?district=Pune&lang=HI
```

**Response Shape:**

```json
{
  "success": true,
  "data": {
    "generatedAt": "2026-07-31T10:00:00",
    "modules": [ AppModule ],
    "promoCards": [ PromoCard ],
    "featuredListings": {
      "title": { "en": "Featured Listings", "hi": "विशेष लिस्टिंग" },
      "listings": [ ListingCardDTO ]
    },
    "moduleSections": [
      {
        "module": AppModule,
        "topCategories": [ ModuleCategory ],
        "listings": [ ListingCardDTO ]
      }
    ],
    "featuredProfiles": [ BusinessProfileCard ],
    "recentListings": [ ListingCardDTO ]
  },
  "message": { "en": "...", "hi": "..." },
  "traceId": "...",
  "timestamp": "..."
}
```

**Data Limits (server-controlled):**

| Section            | Max items |
|--------------------|-----------|
| promoCards         | All active for district |
| featuredListings   | 10        |
| topCategories/section | 4    |
| listings/section   | 6         |
| featuredProfiles   | 6         |
| recentListings     | 8         |

> **Caching:** Home feed is cached per `district:lang` combination. Stays fresh — no manual refresh needed on first load. Invalidated on new listing/subscription events.

---

## 3. Rendering the Home Screen

### Layout Hierarchy (Top → Bottom)

```
╔═══════════════════════════════════╗
║  🔍 Search bar                    ║
║  📍 Location pill  [Change]       ║
╠═══════════════════════════════════╣
║  ┌─────────────────────────────┐  ║
║  │  PROMO CARDS CAROUSEL       │  ║  ← horizontal scroll, auto-play 3s
║  └─────────────────────────────┘  ║
╠═══════════════════════════════════╣
║  MODULE TABS                      ║  ← chips/tabs: Crop | Seed | Animal…
╠═══════════════════════════════════╣
║  ⭐ FEATURED LISTINGS             ║  ← horizontal scroll, premium-border cards
║  ┌──┐ ┌──┐ ┌──┐ ┌──┐            ║
║  │  │ │  │ │  │ │  │            ║
║  └──┘ └──┘ └──┘ └──┘            ║
╠═══════════════════════════════════╣
║  MODULE SECTION: Crops            ║
║  Categories: [Wheat][Rice][Corn]  ║  ← category chips, tap → browse
║  ┌──┐ ┌──┐ ┌──┐                  ║
║  │  │ │  │ │  │                  ║
║  └──┘ └──┘ └──┘  [See all →]    ║
╠═══════════════════════════════════╣
║  MODULE SECTION: Seeds            ║
║  ... (repeats per module) ...     ║
╠═══════════════════════════════════╣
║  🏪 FEATURED BUSINESSES           ║  ← verified + premium seller shops
╠═══════════════════════════════════╣
║  🕐 RECENTLY ADDED                ║  ← freshness, no premium filter
╚═══════════════════════════════════╝
```

### Section Navigation Rules

| Tap target               | Navigate to                                                   |
|--------------------------|---------------------------------------------------------------|
| Module tab               | `BrowseScreen` with `moduleId` pre-set                        |
| Category chip            | `BrowseScreen` with `categoryId` pre-set                      |
| ListingCard              | `ListingDetailScreen` with `listingId`                        |
| "See all →" in section   | `BrowseScreen` with `moduleId` + optional `district`          |
| BusinessProfileCard      | `SellerProfileScreen` with `userId`                           |
| PromoCard CTA button     | See [PromoCard Carousel](#10-promocard-carousel)              |

---

## 4. Search API

### `GET /api/v1/search`

**Public — no auth required.**

Returns matching **listings** + matching **business profiles** in a single response.

| Query Param   | Type    | Description                                              |
|---------------|---------|----------------------------------------------------------|
| `q`           | string  | Search text — supports **Hindi and English** (bilingual) |
| `moduleId`    | integer | Filter by module                                         |
| `categoryId`  | integer | Filter by category                                       |
| `listingType` | string  | `SELL` / `RENT`                                          |
| `minPrice`    | decimal | Lower price bound (applied to offered_price)             |
| `maxPrice`    | decimal | Upper price bound                                        |
| `district`    | string  | Text filter on listing district                          |
| `lat`         | double  | User GPS latitude                                        |
| `lng`         | double  | User GPS longitude                                       |
| `radius`      | integer | Radius in km: `5`, `10`, `25`, `50`, `100` (omit = All India) |
| `sellerType`  | string  | `ALL` (default) / `SUBSCRIBED` (premium sellers only)    |
| `postedWithin`| string  | `TODAY` / `WEEK` / `MONTH` / `ALL`                       |
| `lang`        | string  | `EN` (default) / `HI`                                    |
| `page`        | integer | 0-based page number (default 0)                          |
| `size`        | integer | Items per page (default 20)                              |

**Example:**

```
GET /api/v1/search?q=गेहूं&district=Pune&radius=25&lat=18.52&lng=73.85&postedWithin=WEEK&page=0&size=20
```

**Response:**

```json
{
  "success": true,
  "data": {
    "totalListings": 142,
    "totalPages": 8,
    "currentPage": 0,
    "pageSize": 20,
    "query": "गेहूं",
    "listings": [ ListingCardDTO ],
    "businessProfiles": [ BusinessProfileCard ]
  }
}
```

### Search Bar UX Rules

1. **Debounce** input by 400ms before firing.
2. Show `businessProfiles` section above `listings` in results when both are present.
3. When `q` is empty, redirect to the Nearby / Home feed instead.
4. Support both Devanagari (Hindi) and Latin (English) keyboards simultaneously.
5. Preserve all active filters across search query changes.

---

## 5. Browse by Category API

### `GET /api/v1/listings/category/{categoryId}`

**Requires: JWT Auth.**

| Query Param   | Type    | Description                                         |
|---------------|---------|-----------------------------------------------------|
| `categoryId`  | integer | **Path param** — required                           |
| `listingType` | string  | `SELL` / `RENT`                                     |
| `minPrice`    | decimal | Minimum offered price                               |
| `maxPrice`    | decimal | Maximum offered price                               |
| `district`    | string  | Filter by district name                             |
| `state`       | string  | Filter by state name                                |
| `lat`         | double  | User GPS latitude (enables `distanceKm` in results) |
| `lng`         | double  | User GPS longitude                                  |
| `radius`      | integer | km radius: `5` / `10` / `25` / `50` / `100`        |
| `sellerType`  | string  | `ALL` / `SUBSCRIBED`                                |
| `postedWithin`| string  | `TODAY` / `WEEK` / `MONTH` / `ALL`                  |
| `page`        | integer | Default 0                                           |
| `size`        | integer | Default 20                                          |
| `sort`        | string  | e.g. `createdAt,desc`                               |

**Response:** Paginated `ListingCardDTO[]` — see [ListingCard Component](#9-listingcard-component).

**Sort priority (server-enforced):** Premium subscribers always appear above non-subscribers within the same page results.

---

## 6. Nearby Listings API

### `GET /api/v1/listings/nearby`

**Requires: JWT Auth.**

Returns listings sorted **closest-first**. Premium sellers appear first within the same distance tier.

| Query Param   | Type    | Required | Default | Description                       |
|---------------|---------|----------|---------|-----------------------------------|
| `lat`         | double  | **YES**  | —       | User's current latitude           |
| `lng`         | double  | **YES**  | —       | User's current longitude          |
| `radius`      | integer | No       | `25`    | km: `5` / `10` / `25` / `50` / `100` |
| `moduleId`    | integer | No       | —       | Filter by module                  |
| `categoryId`  | integer | No       | —       | Filter by category                |
| `listingType` | string  | No       | —       | `SELL` / `RENT`                   |
| `minPrice`    | decimal | No       | —       | Min price filter                  |
| `maxPrice`    | decimal | No       | —       | Max price filter                  |
| `district`    | string  | No       | —       | Further narrow by district        |
| `sellerType`  | string  | No       | `ALL`   | `ALL` / `SUBSCRIBED`              |
| `page`        | integer | No       | `0`     | Page number                       |
| `size`        | integer | No       | `20`    | Page size                         |

**Example:**

```
GET /api/v1/listings/nearby?lat=18.5204&lng=73.8567&radius=10&moduleId=1&page=0&size=20
```

Each card in the response includes `distanceKm` — render it on the card badge (e.g. `📍 3.2 km`).

---

## 7. Location & Geo Features

### 7.1 Obtaining User Location

```javascript
// Request GPS on screen load
navigator.geolocation.getCurrentPosition(
  ({ coords }) => {
    store.setLocation({ lat: coords.latitude, lng: coords.longitude });
    reverseGeocode(coords.latitude, coords.longitude); // → set district label
  },
  () => {
    // Permission denied → fall back to district text picker
    showDistrictPicker();
  },
  { timeout: 8000, maximumAge: 300000 }
);
```

### 7.2 Location State (Global Store)

```typescript
interface LocationState {
  lat: number | null;
  lng: number | null;
  district: string | null;      // e.g. "Pune"
  state: string | null;         // e.g. "Maharashtra"
  radiusKm: number;             // default: 25
  locationLabel: string;        // display string: "Pune • 25 km"
  permissionDenied: boolean;
}
```

### 7.3 Location Pill Component

Display a persistent location indicator in the header:

```
┌─────────────────────────────┐
│ 📍 Pune  ▾  [25 km ▾]       │
└─────────────────────────────┘
```

- Tap on district name → open **District Picker** (text search + list)
- Tap on radius chip → open **Radius Selector**

### 7.4 Radius Selector

Present as a bottom sheet or segmented control:

| Option   | Param value | Label        |
|----------|-------------|--------------|
| 5 km     | `5`         | Very nearby  |
| 10 km    | `10`        | Nearby       |
| 25 km    | `25`        | Around me ✓  |
| 50 km    | `50`        | My region    |
| 100 km   | `100`       | Wide area    |
| All India| _(omit)_    | All India    |

> When radius is changed, re-fire all active API calls with the new value.

### 7.5 How Location Flows Into Each API

| Screen         | How location is used                                       |
|----------------|------------------------------------------------------------|
| Home Feed      | `?district=Pune` (text-based, from reverse geocode)        |
| Search         | `lat=`, `lng=`, `radius=` (GPS), or `district=` (text)     |
| Browse Category| `lat=`, `lng=`, `radius=` (GPS) + `district=` optionally   |
| Nearby         | `lat=`, `lng=` required; `radius=` optional (default 25)   |

### 7.6 Reverse Geocoding (Client-Side)

The backend does **not** provide reverse geocoding. Use a client-side solution:

```javascript
// Option A — OpenStreetMap Nominatim (free)
const url = `https://nominatim.openstreetmap.org/reverse?lat=${lat}&lon=${lng}&format=json`;

// Option B — Google Maps Geocoding API (paid, higher accuracy)
// Extract 'administrative_area_level_2' → district, 'administrative_area_level_1' → state
```

Map the district/state names to the `district` and `state` query params.

---

## 8. Filter Panel Reference

Render as a **bottom sheet** or side drawer on Browse/Search screens.

### Filter Fields

```
┌─────────────────────────────────────────┐
│  FILTERS                          [Reset]│
├─────────────────────────────────────────┤
│  Listing Type                            │
│  ○ All  ● Sell  ○ Rent                   │
├─────────────────────────────────────────┤
│  Price Range                             │
│  ₹ [___Min___]  to  ₹ [___Max___]       │
│  ━━━━━●━━━━━━━━━━━━━━●━━━━━━━━  (slider)│
├─────────────────────────────────────────┤
│  Location                                │
│  District: [Pune          ▾]            │
│  State:    [Maharashtra   ▾]            │
├─────────────────────────────────────────┤
│  Nearby Radius                           │
│  [5km] [10km] [●25km] [50km] [100km]    │
├─────────────────────────────────────────┤
│  Seller Type                             │
│  ○ All Sellers  ● Verified/Premium       │
├─────────────────────────────────────────┤
│  Posted Within                           │
│  ○ Any  ● Today  ○ This Week  ○ Month   │
├─────────────────────────────────────────┤
│            [Apply Filters]               │
└─────────────────────────────────────────┘
```

### Filter → API Param Mapping

| UI Control        | Param name    | Values                                  |
|-------------------|---------------|-----------------------------------------|
| Listing Type      | `listingType` | `SELL` / `RENT` / _(omit for all)_     |
| Price Min         | `minPrice`    | decimal                                 |
| Price Max         | `maxPrice`    | decimal                                 |
| District          | `district`    | text (case-sensitive on server)         |
| State             | `state`       | text                                    |
| Radius            | `radius`      | `5` / `10` / `25` / `50` / `100`       |
| GPS lat/lng       | `lat`, `lng`  | doubles (from device GPS)               |
| Seller Type       | `sellerType`  | `ALL` / `SUBSCRIBED`                    |
| Posted Within     | `postedWithin`| `TODAY` / `WEEK` / `MONTH` / `ALL`     |

### Active Filter Chips

Show applied filters as dismissible chips above the result list:

```
[Sell ×]  [₹500–₹2000 ×]  [25 km ×]  [This Week ×]
```

---

## 9. ListingCard Component

Every listing in the feed, search results, and nearby uses this DTO.

### Field Reference

```typescript
interface ListingCardDTO {
  listingId: string;            // e.g. "LT1A2B3C4D"
  userId: string;               // seller's userId
  moduleId: number;
  categoryId: number;
  listingType: "SELL" | "RENT" | "BOTH";
  actualPrice: number | null;   // original price (show with strikethrough)
  offeredPrice: number;         // price to display prominently
  discountPct: number | null;   // e.g. 15.5 → show "15% OFF" badge
  quantity: number | null;
  unit: string | null;          // e.g. "kg", "quintal", "piece"
  isNegotiable: boolean;
  showContact: boolean;         // if false, hide contact button
  images: string[];             // image URLs (first = thumbnail)
  address: {
    village: string;
    district: string;
    state: string;
    pincode: string;
    lat: number | null;
    lng: number | null;
  };
  attributes: Record<string, any>; // dynamic fields from form definition
  status: "ACTIVE" | "INACTIVE" | "SOLD" | "DRAFT";
  viewCount: number;
  contactRevealCount: number;
  createdAt: string;            // ISO-8601
  distanceKm: number | null;    // present only when lat/lng filter used

  // Premium badge fields
  isHighlighted: boolean;
  highlightColor: string | null; // e.g. "#FFD700"
  sellerBadge: { en: string; hi: string } | null;
  sellerPlanKey: "BASIC" | "STANDARD" | "PREMIUM" | "ENTERPRISE" | null;
}
```

### Card Layout

```
╔══════════════════════════════════════╗
║  [IMAGE]          ┌─────────────────┐║  ← isHighlighted → border: highlightColor
║                   │ ⭐ PREMIUM       │║  ← sellerBadge[lang]
║  [DISCOUNT BADGE] └─────────────────┘║
╠══════════════════════════════════════╣
║  ₹1,200 /kg    ~~₹1,500~~   15% OFF  ║
║  Wheat (Crop)                        ║
║  📍 Solapur, Maharashtra  · 3.2 km   ║  ← distanceKm if available
║  🕐 2 days ago                        ║
╠══════════════════════════════════════╣
║  [📞 View Contact]  [💬 Chat]        ║
╚══════════════════════════════════════╝
```

### Card Rendering Rules

1. **Primary price:** `offeredPrice`. Show `actualPrice` with strikethrough only if `actualPrice > offeredPrice`.
2. **Discount badge:** Render `discountPct` as `"15% OFF"` — top-left corner, red background.
3. **Distance badge:** Show `📍 {distanceKm.toFixed(1)} km` only when `distanceKm` is not null.
4. **SOLD overlay:** When `status === "SOLD"` apply a grey overlay + "SOLD" stamp.
5. **Premium highlight:** When `isHighlighted === true`, apply `border: 2px solid {highlightColor}` to the card.
6. **Negotiable tag:** Show `"Negotiable"` tag when `isNegotiable === true`.
7. **No contact:** Hide contact button when `showContact === false`.
8. **Images:** Show first image as thumbnail; placeholder when `images` is empty.
9. **Unit:** Show with quantity: `"50 kg"` or `"5 quintal"`.

---

## 10. PromoCard Carousel

### Data Fields

```typescript
interface PromoCard {
  cardId: string;
  title: { en: string; hi: string };
  subtitle: { en: string; hi: string } | null;
  imageUrl: string | null;
  bgColor: string;          // hex, e.g. "#FFF3E0" — use when imageUrl is null
  textColor: string;        // hex for text overlay
  ctaLabel: { en: string; hi: string } | null;
  ctaType: "MODULE" | "CATEGORY" | "EXTERNAL" | "NONE";
  ctaValue: string | null;  // moduleId / categoryId / URL
  displayOrder: number;     // lower = first
  targetDistrict: string | null;
}
```

### CTA Navigation Logic

```javascript
function handlePromoCardTap(card) {
  switch (card.ctaType) {
    case "MODULE":
      navigate("BrowseScreen", { moduleId: parseInt(card.ctaValue) });
      break;
    case "CATEGORY":
      navigate("BrowseScreen", { categoryId: parseInt(card.ctaValue) });
      break;
    case "EXTERNAL":
      openInAppBrowser(card.ctaValue);
      break;
    case "NONE":
      // no action
      break;
  }
}
```

### Carousel Rendering

- Auto-play with 3 second interval; pause on user interaction.
- Show dot indicators at the bottom.
- When `imageUrl` is present: full-bleed image with title/subtitle overlay using `textColor`.
- When `imageUrl` is null: solid card with `bgColor` background and text in `textColor`.
- Aspect ratio: **16:6** (banner style) on mobile, **16:4** on tablet.

---

## 11. Premium / Subscription Badges

Four plan tiers affect visual rendering:

| `sellerPlanKey` | Badge Label         | Highlight Color | Visual Treatment               |
|-----------------|---------------------|-----------------|--------------------------------|
| `BASIC`         | Basic Seller        | None            | No special styling             |
| `STANDARD`      | Standard Seller     | #4CAF50 (green) | Green card border              |
| `PREMIUM`       | Premium Seller ⭐   | #FFD700 (gold)  | Gold border + star badge       |
| `ENTERPRISE`    | Top Seller 🏆       | #FF6F00 (amber) | Amber border + trophy badge    |

### Implementation

```javascript
function getCardStyle(card) {
  if (!card.isHighlighted) return {};
  return {
    border: `2px solid ${card.highlightColor}`,
    boxShadow: `0 0 8px ${card.highlightColor}40`
  };
}

function getBadgeText(card, lang = "en") {
  return card.sellerBadge?.[lang] ?? null;
}
```

> Badges also appear on `BusinessProfileCard` objects in `featuredProfiles` — same `isHighlighted`, `highlightColor`, `badge`, `planKey` fields.

---

## 12. Listing Detail & Contact Reveal

### Get Listing Detail

```
GET /api/v1/listings/{listingId}
Authorization: Bearer <token>  (optional — public endpoint)
```

Returns a single `ListingCardDTO` including full `attributes` map (all dynamic form fields).

### Similar Listings

```
GET /api/v1/listings/{listingId}/similar?limit=6
```

Returns up to 6 `ListingCardDTO` items from the same category.

### OLX-Style Contact Reveal

**Requires auth.** Reveals the seller's phone number. Deduped per buyer per 24 hours.

```
POST /api/v1/listings/{listingId}/contact
Authorization: Bearer <token>
```

**Response:**

```json
{
  "success": true,
  "data": {
    "phone": "+91-9876543210",
    "revealedAt": "2026-07-31T10:15:00",
    "listingId": "LT1A2B3C4D"
  }
}
```

**UX Flow:**

```
[View Contact]  →  (if logged out) → Login prompt
                →  (if logged in)  → POST /contact
                                   → Show phone number in modal
                                   → Show "Call" + "Copy" buttons
```

### Seller Profile Page

```
GET /api/v1/listings/by-seller/{userId}?listingType=SELL&page=0&size=20
```

Returns all active listings by a given seller — use for the seller's public profile page.

---

## 13. Pagination

All list endpoints return a page wrapper:

```typescript
interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;       // current page (0-based)
  size: number;
  first: boolean;
  last: boolean;
}
```

### Pagination Params

Append to any list endpoint:

```
?page=0&size=20&sort=createdAt,desc
```

Common sort fields: `createdAt`, `offeredPrice`, `viewCount`.

### Infinite Scroll Implementation

```javascript
async function loadMoreListings() {
  if (isLoading || currentPage >= totalPages - 1) return;
  setIsLoading(true);
  const nextPage = currentPage + 1;
  const result = await api.get(`/listings/nearby`, {
    params: { ...activeFilters, page: nextPage, size: 20 }
  });
  appendListings(result.data.content);
  setCurrentPage(nextPage);
  setIsLoading(false);
}
```

---

## 14. Bilingual Support (EN / HI)

### Language Param

Pass `?lang=HI` or `?lang=EN` on any endpoint that supports it. Default is always `EN`.

### Bilingual Fields in Responses

Several fields are returned as `{ "en": "...", "hi": "..." }` objects:

```typescript
// Examples:
module.moduleNameEn / module.moduleNameHi
featuredListings.title         // { en: "Featured Listings", hi: "विशेष लिस्टिंग" }
promoCard.title                // { en: "Summer Sale", hi: "गर्मी की सेल" }
sellerBadge                    // { en: "Premium Seller", hi: "प्रीमियम विक्रेता" }
```

### Reading Bilingual Fields

```javascript
function t(field, lang = "en") {
  if (!field) return "";
  return field[lang.toLowerCase()] ?? field["en"] ?? "";
}

// Usage
t(promoCard.title, userLang)       // "गर्मी की सेल"
t(featuredListings.title, "en")    // "Featured Listings"
```

### Language Persistence

Store the user's preferred language in `AsyncStorage` / `localStorage` and pass it on every API call.

---

## 15. API Response Envelope

Every response from the backend is wrapped:

```json
{
  "success": true,
  "data": { ... },
  "message": {
    "en": "Operation successful",
    "hi": "ऑपरेशन सफल रहा"
  },
  "traceId": "550e8400-e29b-41d4-a716-446655440000",
  "timestamp": "2026-07-31T10:00:00.000Z"
}
```

Always read from `response.data.data` (the outer `data` key from axios/fetch, then the inner `data` key from the envelope).

```javascript
// Axios interceptor example
axios.interceptors.response.use(
  (res) => res.data,        // unwrap HTTP layer → gives { success, data, message, ... }
  (err) => Promise.reject(err)
);

// Then in components:
const { data: homeFeed } = await api.get("/home");
// homeFeed = { modules, promoCards, featuredListings, ... }
```

---

## 16. Error Handling

```json
{
  "success": false,
  "message": { "en": "Resource not found", "hi": "संसाधन नहीं मिला" },
  "traceId": "...",
  "timestamp": "..."
}
```

| HTTP Status | Meaning                               | UX Action                          |
|-------------|---------------------------------------|------------------------------------|
| `400`       | Bad request / validation error        | Show field-level errors            |
| `401`       | Unauthenticated                       | Redirect to login                  |
| `403`       | Forbidden (not your resource)         | Show "Not allowed" toast           |
| `404`       | Resource not found                    | Show 404 screen                    |
| `429`       | Rate limited                          | Show "Too many requests" + backoff |
| `500`       | Server error                          | Show generic error + retry button  |

---

## 17. Complete Screen-by-Screen Reference

### Screen 1 — Home

```
API:     GET /api/v1/home?district={district}&lang={lang}
Auth:    No
Refresh: Pull-to-refresh → clear cache, re-call API
```

**Component tree:**

```
HomeScreen
├── SearchBar                → navigate to SearchScreen on focus
├── LocationPill             → district + radius display
├── PromoCarousel            → data.promoCards[]
├── ModuleTabs               → data.modules[]
├── FeaturedSection          → data.featuredListings.listings[]
├── ModuleSectionList        → data.moduleSections[]
│   └── ModuleSection (×N)
│       ├── CategoryChips    → data.moduleSections[n].topCategories[]
│       └── ListingRow       → data.moduleSections[n].listings[]
├── FeaturedProfiles         → data.featuredProfiles[]
└── RecentListings           → data.recentListings[]
```

---

### Screen 2 — Search Results

```
API:     GET /api/v1/search?q={q}&{filters}
Auth:    No
Trigger: Debounced input (400ms) or submit
```

**Layout:**

```
SearchScreen
├── SearchBar (active, with back button)
├── FilterBar (active chips)
├── ResultsTabs: [Listings ({count})] [Sellers ({count})]
├── SortBar: [Relevance] [Newest] [Price ↑] [Price ↓] [Nearest]
└── ResultList (infinite scroll)
    ├── ListingCardGrid      → data.listings[]
    └── BusinessCardList     → data.businessProfiles[]
```

---

### Screen 3 — Browse Category

```
API:     GET /api/v1/listings/category/{categoryId}?{filters}
Auth:    Yes (JWT)
Entry:   From category chip, module tab "See All", or deep link
```

---

### Screen 4 — Nearby

```
API:     GET /api/v1/listings/nearby?lat={lat}&lng={lng}&radius={radius}&{filters}
Auth:    Yes (JWT)
Entry:   Bottom nav "Nearby" tab
```

**Show distance badge on every card.** Sort = closest first (server-enforced).

---

### Screen 5 — Listing Detail

```
API:     GET /api/v1/listings/{listingId}
         GET /api/v1/listings/{listingId}/similar
Auth:    Optional (No for view, Yes for contact reveal)
```

**Contact reveal flow:**

```
POST /api/v1/listings/{listingId}/contact  (requires auth)
→ Modal: phone number + [📞 Call] [📋 Copy]
```

---

### Screen 6 — Seller Profile

```
API:     GET /api/v1/listings/by-seller/{userId}?page=0&size=20
Auth:    Yes (JWT)
Entry:   Tap seller name/avatar on any listing card
```

---

## Quick-Reference Cheat Sheet

```
┌──────────────────┬────────────────────────────────────────────────────────┐
│ Screen           │ Primary API Call                                        │
├──────────────────┼────────────────────────────────────────────────────────┤
│ Home             │ GET /api/v1/home?district=X&lang=EN                     │
│ Search           │ GET /api/v1/search?q=X&lat=&lng=&radius=&...           │
│ Browse Category  │ GET /api/v1/listings/category/{id}?lat=&lng=&radius=   │
│ Nearby           │ GET /api/v1/listings/nearby?lat=&lng=&radius=25        │
│ Listing Detail   │ GET /api/v1/listings/{listingId}                       │
│ Similar          │ GET /api/v1/listings/{listingId}/similar?limit=6       │
│ Contact Reveal   │ POST /api/v1/listings/{listingId}/contact (auth)       │
│ Seller Profile   │ GET /api/v1/listings/by-seller/{userId}                │
└──────────────────┴────────────────────────────────────────────────────────┘

Radius presets: 5 / 10 / 25 (default) / 50 / 100 / omit=All India
Sort:           premium sellers always float to top (server-enforced)
Bilingual:      pass ?lang=HI for Hindi; fields with {en,hi} use t(field, lang)
Auth header:    Authorization: Bearer <jwt>
Envelope:       always read response → .data.data for payload
```

---

*Generated for Tantra Backend v1 — Spring Boot 4.1.0 — Base: `/api/v1`*
