# Carousel Feature — Frontend Developer Guide

> Complete guide for the DB-driven home screen carousel.
> Covers: fetching buttons, navigating to listings, nearby sort, and admin management.

---

## Overview

The carousel on the home screen shows category browse buttons (Browse Crops, Browse Seeds, Rent Equipment, etc.).  
These buttons are **not hardcoded** — they come from the database.  
An admin can add, remove, reorder, or disable buttons at any time without any app update.

**Full flow:**

```
App start
  └─ GET /carousel              ← fetch buttons from DB
       └─ user taps a button
            └─ GET /listings/browse/{ctaValue}?listingType=...&lat=...&lng=...
                 └─ show listing cards sorted nearest first
```

---

## Base URL

```
http://localhost:8080/api/v1
```

Replace with your production domain before release.

---

## Step 1 — Fetch Carousel Buttons

Call this once when the home screen loads.

```
GET /api/v1/carousel
```

**Auth:** Public — no token needed.

### Response

```json
{
  "success": true,
  "data": [
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
      "iconUrl": null,
      "ctaType": "BROWSE_CATEGORY",
      "ctaValue": "equipment",
      "listingType": "RENT",
      "bgColor": "#F3E5F5",
      "textColor": "#4A148C",
      "displayOrder": 4,
      "isActive": true
    }
  ]
}
```

### Fields

| Field | Type | Description |
|---|---|---|
| `id` | int | Unique item ID |
| `label.en` | string | Button text in English |
| `label.hi` | string | Button text in Hindi |
| `iconUrl` | string / null | Optional icon image URL |
| `ctaType` | string | Navigation type — see table below |
| `ctaValue` | string | What to navigate to |
| `listingType` | string / null | `RENT`, `SELL`, or null (all types) |
| `bgColor` | string | Hex background color for the tile |
| `textColor` | string | Hex text color for the label |
| `displayOrder` | int | Sort order (lower = shown first) |
| `isActive` | boolean | Only `true` items are returned |

### ctaType values

| ctaType | What to do on tap |
|---|---|
| `BROWSE_CATEGORY` | Call `/listings/browse/{ctaValue}` — see Step 2 |
| `BROWSE_MODULE` | Navigate to the module screen |
| `EXTERNAL_URL` | Open `ctaValue` in the browser |
| `NONE` | Display only — no tap action |

---

## Step 2 — Browse Listings When Button is Tapped

When `ctaType = BROWSE_CATEGORY`, build the listing URL using `ctaValue` and `listingType`.

```
GET /api/v1/listings/browse/{ctaValue}
```

**Auth:** Public — no token needed.

### Building the URL

```
// Pseudocode
url = "/api/v1/listings/browse/" + item.ctaValue

if (item.listingType != null) {
  url += "?listingType=" + item.listingType
}

// Always pass user GPS for nearby sort
if (userLocation != null) {
  url += (url.contains("?") ? "&" : "?")
  url += "lat=" + userLocation.lat + "&lng=" + userLocation.lng
}
```

### Real examples

| Carousel Button | URL to call |
|---|---|
| Browse Crops | `/api/v1/listings/browse/crop?lat=23.18&lng=79.98` |
| Browse Seeds | `/api/v1/listings/browse/seed?lat=23.18&lng=79.98` |
| Browse Fertilizers | `/api/v1/listings/browse/fertilizer?lat=23.18&lng=79.98` |
| Rent Equipment | `/api/v1/listings/browse/equipment?listingType=RENT&lat=23.18&lng=79.98` |
| Buy Equipment | `/api/v1/listings/browse/equipment?listingType=SELL&lat=23.18&lng=79.98` |
| Browse Animals | `/api/v1/listings/browse/animal?lat=23.18&lng=79.98` |
| Agri Repair | `/api/v1/listings/browse/repair?lat=23.18&lng=79.98` |

### Optional query params

| Param | Type | Description |
|---|---|---|
| `lat` | float | User GPS latitude — enables nearby sort |
| `lng` | float | User GPS longitude — enables nearby sort |
| `radius` | int (km) | Show only listings within this radius. Presets: `5`, `10`, `25`, `50`, `100`. Omit = no cutoff |
| `listingType` | `SELL` / `RENT` / `BOTH` | Filter by type (usually comes from `item.listingType`) |
| `minPrice` | decimal | Minimum price filter |
| `maxPrice` | decimal | Maximum price filter |
| `district` | string | Filter by district name |
| `state` | string | Filter by state |
| `postedWithin` | `TODAY` / `WEEK` / `MONTH` / `ALL` | Recency filter |
| `page` | int | Page number, 0-indexed (default 0) |
| `size` | int | Page size (default 20) |

---

## Step 3 — Render Listing Cards

### Response shape

```json
{
  "success": true,
  "data": {
    "content": [
      {
        "listingId": "LT68GVHRTR",
        "listingTitle": "Premium Wheat - Grade A",
        "listingType": "SELL",
        "actualPrice": 2200.00,
        "offeredPrice": 2000.00,
        "discountPct": 9.09,
        "quantity": 50.0,
        "unit": "quintal",
        "isNegotiable": true,
        "distanceKm": 0.3,
        "address": {
          "city": "Jabalpur",
          "district": "Jabalpur",
          "state": "MP",
          "pinCode": "482001",
          "latitude": 23.1815,
          "longitude": 79.9864
        },
        "images": [],
        "isNew": true,
        "isHighlighted": false,
        "sellerVerified": false,
        "createdAt": "2026-08-02T14:25:00"
      }
    ],
    "totalElements": 11,
    "totalPages": 1,
    "number": 0,
    "size": 20
  }
}
```

### Key card fields

| Field | Description |
|---|---|
| `listingId` | Unique ID — use to open detail screen |
| `listingTitle` | Display title |
| `offeredPrice` | Price to show (discounted) |
| `actualPrice` | Strikethrough price (if different from offeredPrice) |
| `discountPct` | e.g. `9.09` → show "9% off" badge |
| `distanceKm` | km from user — shown only when `lat`+`lng` were passed |
| `isNew` | `true` if posted within last 24 hours — show "New" badge |
| `isHighlighted` | `true` = premium seller — show gold border |
| `highlightColor` | Gold/orange hex — use as card border color |
| `sellerBadge.en` | e.g. "Premium Seller" — show badge text |
| `sellerVerified` | `true` = show blue verified tick |
| `images` | Array of image URLs (may be empty) |

### Sort behavior

| What you pass | How results are sorted |
|---|---|
| `lat` + `lng` | Premium sellers first → then nearest first |
| No coordinates | Premium sellers first → then newest first |

---

## Step 4 — Pagination

```
GET /api/v1/listings/browse/crop?lat=23.18&lng=79.98&page=0&size=20
GET /api/v1/listings/browse/crop?lat=23.18&lng=79.98&page=1&size=20
```

Use `data.totalPages` and `data.number` to control infinite scroll or pagination UI.

```
data.number          // current page (0-indexed)
data.totalPages      // total pages available
data.totalElements   // total listing count
data.content         // array of cards for this page
```

---

## Complete Code Example (JavaScript / React Native)

```js
// 1. Load carousel on home screen mount
async function loadCarousel() {
  const res = await fetch(`${BASE_URL}/api/v1/carousel`)
  const json = await res.json()
  return json.data  // array of carousel items
}

// 2. Build browse URL when user taps a button
function buildBrowseUrl(item, userLocation) {
  let url = `${BASE_URL}/api/v1/listings/browse/${item.ctaValue}`
  const params = []

  if (item.listingType) {
    params.push(`listingType=${item.listingType}`)
  }
  if (userLocation) {
    params.push(`lat=${userLocation.lat}`)
    params.push(`lng=${userLocation.lng}`)
  }

  return params.length > 0 ? `${url}?${params.join('&')}` : url
}

// 3. Fetch listings for the browse screen
async function fetchListings(item, userLocation, page = 0) {
  const url = buildBrowseUrl(item, userLocation)
  const separator = url.includes('?') ? '&' : '?'
  const res = await fetch(`${url}${separator}page=${page}&size=20`)
  const json = await res.json()
  return json.data  // { content, totalPages, totalElements, number }
}

// 4. Render a card — key display logic
function renderCard(card) {
  const price = card.offeredPrice
  const originalPrice = card.actualPrice !== card.offeredPrice ? card.actualPrice : null
  const distance = card.distanceKm != null ? `${card.distanceKm} km away` : null
  const isPremium = card.isHighlighted
  const isNew = card.isNew

  // Use card.images[0] for thumbnail, or show placeholder
  const thumb = card.images && card.images.length > 0 ? card.images[0] : null

  return { price, originalPrice, distance, isPremium, isNew, thumb }
}
```

---

## Admin — Managing Carousel Buttons

All admin endpoints require an `ROLE_ADMIN` JWT token:

```
Authorization: Bearer {adminToken}
```

### Get all items (including inactive)

```
GET /api/v1/admin/carousel
```

### Create a new button

```http
POST /api/v1/admin/carousel
Content-Type: application/json
Authorization: Bearer {adminToken}

{
  "label": { "en": "Browse Pesticides", "hi": "कीटनाशक देखें" },
  "ctaType": "BROWSE_CATEGORY",
  "ctaValue": "pesticide",
  "listingType": null,
  "bgColor": "#F9FBE7",
  "textColor": "#33691E",
  "displayOrder": 8,
  "isActive": true
}
```

### Update an existing button

```http
PUT /api/v1/admin/carousel/{id}
Content-Type: application/json
Authorization: Bearer {adminToken}

{
  "label": { "en": "Browse Crops (Rabi)", "hi": "रबी फसल देखें" },
  "displayOrder": 1
}
```

### Toggle active/inactive (show/hide without deleting)

```http
PATCH /api/v1/admin/carousel/{id}/toggle
Authorization: Bearer {adminToken}
```

### Delete a button

```http
DELETE /api/v1/admin/carousel/{id}
Authorization: Bearer {adminToken}
```

---

## Default Buttons in DB

| id | English Label | ctaValue | listingType | bgColor |
|---|---|---|---|---|
| 1 | Browse Crops | `crop` | — | `#E8F5E9` |
| 2 | Browse Seeds | `seed` | — | `#FFF8E1` |
| 3 | Browse Fertilizers | `fertilizer` | — | `#E3F2FD` |
| 4 | Rent Equipment | `equipment` | `RENT` | `#F3E5F5` |
| 5 | Buy Equipment | `equipment` | `SELL` | `#FBE9E7` |
| 6 | Browse Animals | `animal` | — | `#FCE4EC` |
| 7 | Agri Repair | `repair` | — | `#E8EAF6` |

---

## Valid categoryKey Values

These are the string keys currently seeded in the database:

| categoryKey | What it browses |
|---|---|
| `crop` | Crop listings |
| `seed` | Seed listings |
| `fertilizer` | Fertilizer listings |
| `pesticide` | Pesticide listings |
| `equipment` | Equipment (rent + sell) |
| `animal` | Animal listings |
| `repair` | Agri repair and maintenance |

To add a new category to the carousel, the category must exist in the `module_categories` table with a matching `category_key`. Then create a carousel item pointing to it.

---

## Error Responses

```json
// Invalid category key
{
  "success": false,
  "error": {
    "code": "NOT_FOUND",
    "message": { "en": "Error: Category not found.", "hi": "त्रुटि: श्रेणी नहीं मिली।" }
  }
}
```

```json
// Admin endpoint without token
{
  "authenticated": false,
  "message": "User is not logged in"
}
```
