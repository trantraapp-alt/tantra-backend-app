# Module Listings API — Frontend Guide

> Feature: Tapping a home-page module card (Agriculture, Animal & Livestock, etc.)
> opens a listings page showing **all listings for that module, nearest-first**.

---

## Primary Endpoint

```
GET /api/v1/listings/nearby
```

This is the **correct endpoint** for this screen. It:
- Filters listings by module
- Sorts: **premium listings first**, then **nearest first** (Haversine distance)
- Falls back to newest-first when the user denies location permission

---

## Step 1 — Get the Module ID

Fetch the active modules from:

```
GET /api/v1/masters/modules?onlyActive=true
```

Response shape (array):

```json
[
  { "id": 1, "moduleKey": "agriculture",        "moduleNameEn": "Agriculture" },
  { "id": 2, "moduleKey": "animal_livestock",   "moduleNameEn": "Animal and Livestock" }
]
```

Store the `id` for the tapped module. Pass it as `moduleId` in every listings call.

---

## Step 2 — Fetch Listings (with user location)

**When location is available** (preferred flow):

```
GET /api/v1/listings/nearby
  ?moduleId=1
  &lat=28.6139
  &lng=77.2090
  &radius=25
  &page=0
  &size=20
```

| Parameter | Type    | Required | Default | Notes                          |
|-----------|---------|----------|---------|--------------------------------|
| `moduleId`| Integer | Yes      | —       | From masters API above         |
| `lat`     | Double  | Yes      | —       | User's current latitude        |
| `lng`     | Double  | Yes      | —       | User's current longitude       |
| `radius`  | Integer | No       | 25      | Search radius in **km**        |
| `page`    | Integer | No       | 0       | Zero-indexed page number       |
| `size`    | Integer | No       | 20      | Items per page                 |

**Sort order returned by backend:**
1. Premium/sponsored listings (sort_weight) — shown first
2. Distance ascending (nearest to farthest) within non-premium

---

## Step 3 — Fetch Listings (location denied / unavailable)

Use the same endpoint but **omit `lat`, `lng`, `radius`**. The backend falls back to **newest-first** sort.

```
GET /api/v1/listings/nearby
  ?moduleId=1
  &page=0
  &size=20
```

You can also let the user manually pick a district:

```
GET /api/v1/listings/nearby
  ?moduleId=1
  &district=Lucknow
  &page=0
  &size=20
```

---

## Response Shape

```json
{
  "content": [
    {
      "listingId": "uuid",
      "title": "Wheat Seeds - High Yield Variety",
      "price": 2500.00,
      "listingType": "SELL",
      "categoryNameEn": "Seeds",
      "moduleNameEn": "Agriculture",
      "sellerName": "Ramesh Kumar",
      "sellerType": "INDIVIDUAL",
      "village": "Rampur",
      "district": "Bareilly",
      "state": "Uttar Pradesh",
      "distanceKm": 3.4,
      "primaryImageUrl": "https://...",
      "isVerifiedSeller": false,
      "createdAt": "2026-07-28T10:30:00Z"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 84,
  "totalPages": 5,
  "last": false
}
```

**Key field:** `distanceKm` — show this on the listing card (e.g. "3.4 km away"). It is `null` when no coordinates were provided.

---

## Optional Filters (all compatible with moduleId)

These can be added as additional query params on the same endpoint:

| Param           | Values                          | Example                   |
|-----------------|---------------------------------|---------------------------|
| `listingType`   | `SELL`, `BUY`, `RENT`, `BOTH`   | `&listingType=SELL`        |
| `minPrice`      | number                          | `&minPrice=500`            |
| `maxPrice`      | number                          | `&maxPrice=10000`          |
| `district`      | string                          | `&district=Lucknow`        |
| `postedWithin`  | `TODAY`, `WEEK`, `MONTH`, `ALL` | `&postedWithin=WEEK`       |
| `sellerType`    | `ALL`, `SUBSCRIBED`             | `&sellerType=SUBSCRIBED`   |
| `withPhoto`     | `true` / `false`                | `&withPhoto=true`          |
| `verifiedSeller`| `true` / `false`                | `&verifiedSeller=true`     |
| `categoryId`    | Integer                         | `&categoryId=5`            |

To build the filter UI dynamically, call:

```
GET /api/v1/filter-form
```

(no `categoryId` param needed for module-level filters — returns standard groups: listingType, priceRange, postedWithin, sellerType, location)

---

## Recommended Frontend Flow

```
User taps module card
        │
        ▼
GET /api/v1/masters/modules  →  pick moduleId
        │
        ▼
Request device location
    ┌───┴──────────────────────────────┐
    │ Granted                          │ Denied
    ▼                                  ▼
GET /nearby                      GET /nearby
  ?moduleId=X                      ?moduleId=X
  &lat=...                         (no lat/lng)
  &lng=...                         sort = newest first
  &radius=25
  sort = nearest first
        │
        ▼
Render ListingCard list
Show distanceKm badge if present
Paginate: increment &page on scroll
```

---

## Pagination

All responses are paginated. On infinite scroll / load-more:

- Increment `page` by 1 each time
- Stop when `last: true` in response
- `totalElements` gives total count to display (e.g. "84 listings found")

---

## Notes

- `moduleId` values come from the database — **do not hardcode them**. Always fetch from `/api/v1/masters/modules`.
- Distance sorting happens entirely on the backend. Do not re-sort on the frontend.
- If the user moves location mid-session, re-fetch from `page=0` with new coordinates.
- The `radius` default is **25 km**. You may expose a radius selector (e.g. 10 / 25 / 50 / 100 km) if needed.
