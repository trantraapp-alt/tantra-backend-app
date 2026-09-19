# Tantra — Wishlist (Favourites) Frontend Guide

> **Scope:** Only the Wishlist feature. For auth, listings, addresses etc. see `FRONTEND_API.md`.  
> **Base URL (local):** `http://localhost:8080/api/v1`  
> **Auth:** All wishlist endpoints require `Authorization: Bearer <token>` — guest users cannot use wishlist.

---

## Table of Contents

1. [Overview](#1-overview)
2. [API Endpoints](#2-api-endpoints)
3. [Response Shapes](#3-response-shapes)
4. [Status Handling — What to Show for Each Listing State](#4-status-handling)
5. [Heart Button — Add / Remove Toggle](#5-heart-button)
6. [Wishlist Screen](#6-wishlist-screen)
7. [Wishlist Badge Counter](#7-wishlist-badge-counter)
8. [Error Handling](#8-error-handling)
9. [Implementation Checklist](#9-implementation-checklist)

---

## 1. Overview

The wishlist lets a logged-in user save any listing with a heart button. Saved listings appear on a dedicated Wishlist screen. Key behaviours:

- **Heart toggle** on every listing card → POST (add) / DELETE (remove)
- **Status check** on page load → GET `/{listingId}/status` to pre-fill the heart state
- **Wishlist screen** → GET `/wishlist` returns full listing cards (same shape as browse)
- **Stale entries handled** — if a listing is deleted after it was saved, `listing: null` comes back → show "No longer available"
- **SOLD / INACTIVE listings** stay in wishlist with their status visible — user can see what happened
- **favorite_count** on the listing card reflects how many users have wishlisted it — backend keeps it in sync automatically

---

## 2. API Endpoints

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| `POST` | `/wishlist/{listingId}` | JWT | Add listing to wishlist |
| `DELETE` | `/wishlist/{listingId}` | JWT | Remove listing from wishlist |
| `GET` | `/wishlist` | JWT | Get all wishlist items (full cards) |
| `GET` | `/wishlist/{listingId}/status` | JWT | Is this listing wishlisted? |

---

## 3. Response Shapes

### 3a. Add to Wishlist

```http
POST /api/v1/wishlist/LT3A9BKD12
Authorization: Bearer <token>
```

**Success — newly added:**
```json
{
  "success": true,
  "data": { "en": "Listing added to wishlist!", "hi": "लिस्टिंग विशलिस्ट में जोड़ी गई!" },
  "traceId": "abc123"
}
```

**Success — already in wishlist (idempotent, not an error):**
```json
{
  "success": true,
  "data": { "en": "This listing is already in your wishlist.", "hi": "यह लिस्टिंग पहले से आपकी विशलिस्ट में है।" },
  "traceId": "abc123"
}
```

> `data` here is the `LocalizedText` message — display it as a toast if you want, or ignore it.

---

### 3b. Remove from Wishlist

```http
DELETE /api/v1/wishlist/LT3A9BKD12
Authorization: Bearer <token>
```

**Success:**
```json
{
  "success": true,
  "data": { "en": "Listing removed from wishlist.", "hi": "लिस्टिंग विशलिस्ट से हटा दी गई।" },
  "traceId": "abc123"
}
```

**Error — not in wishlist:**
```json
{
  "success": false,
  "error": {
    "code": "NOT_FOUND",
    "message": { "en": "Error: This listing is not in your wishlist.", "hi": "त्रुटि: यह लिस्टिंग आपकी विशलिस्ट में नहीं है।" }
  },
  "traceId": "abc123"
}
```

---

### 3c. Get Wishlist

```http
GET /api/v1/wishlist
Authorization: Bearer <token>
```

**Response `data` — array of `WishlistItem`:**
```json
[
  {
    "listingId": "LT3A9BKD12",
    "savedAt": "2026-08-09T14:30:00",
    "listing": {
      "listingId": "LT3A9BKD12",
      "userId": "TN3A1B2C",
      "moduleId": 1,
      "categoryId": 10,
      "listingTitle": "50 kg Wheat — Premium Grade",
      "listingType": "SELL",
      "actualPrice": 2500.00,
      "offeredPrice": 2200.00,
      "discountPct": 12.00,
      "quantity": 50.0,
      "unit": "kg",
      "isNegotiable": true,
      "showContact": true,
      "images": ["http://localhost:8080/files/img1.jpg"],
      "address": {
        "district": "Pune",
        "state": "Maharashtra",
        "village": "Hadapsar"
      },
      "attributes": { "variety": "Sharbati", "moistureContent": "12%" },
      "status": "ACTIVE",
      "viewCount": 34,
      "contactRevealCount": 5,
      "favoriteCount": 12,
      "createdAt": "2026-08-01T10:00:00",
      "isNew": false,
      "sellerVerified": true,
      "isHighlighted": true,
      "highlightColor": "#FFD700",
      "sellerBadge": { "en": "Premium Seller", "hi": "प्रीमियम विक्रेता" },
      "sellerPlanKey": "PREMIUM",
      "flashDeal": false,
      "discountExpiresAt": null
    }
  },
  {
    "listingId": "LT9XDEL007",
    "savedAt": "2026-08-05T09:15:00",
    "listing": null
  }
]
```

> **`listing: null`** means the listing was deleted after the user saved it.  
> Always show newest saved item first (server already sorts by `savedAt DESC`).

---

### 3d. Wishlist Status Check

```http
GET /api/v1/wishlist/LT3A9BKD12/status
Authorization: Bearer <token>
```

**Response `data`:**
```json
{
  "listingId": "LT3A9BKD12",
  "wishlisted": true
}
```

Use this on listing detail screens to correctly pre-fill the heart button state.

---

## 4. Status Handling

Each wishlist item's `listing.status` tells you what to render.  
**Never filter these out on the frontend — show them with appropriate UI treatment.**

| `listing` value | `status` | What to show |
|-----------------|----------|-------------|
| `{ ...card }` | `"ACTIVE"` | Normal card — fully clickable |
| `{ ...card }` | `"SOLD"` | Dimmed card + **"Bik Gayi / Sold"** red badge |
| `{ ...card }` | `"INACTIVE"` | Dimmed card + **"Abhi Upalabdh Nahi / Unavailable"** grey badge |
| `null` | *(deleted)* | Placeholder card: **"Yeh listing ab available nahi hai"** + remove button |

### CSS approach (React Native / Flutter equivalent):

```js
// listing card opacity
const cardOpacity = listing && listing.status === 'ACTIVE' ? 1.0 : 0.5;

// overlay badge
function getStatusBadge(listing) {
  if (!listing) return { label: { en: 'No longer available', hi: 'अब उपलब्ध नहीं' }, color: '#9E9E9E' };
  if (listing.status === 'SOLD')     return { label: { en: 'Sold', hi: 'बिक गई' }, color: '#F44336' };
  if (listing.status === 'INACTIVE') return { label: { en: 'Unavailable', hi: 'उपलब्ध नहीं' }, color: '#9E9E9E' };
  return null; // ACTIVE — no badge needed
}
```

---

## 5. Heart Button

### Logic

```
User NOT logged in → tapping heart → redirect to Login screen
User logged in:
  On listing card / detail screen load:
    → GET /wishlist/{listingId}/status   (check current state)
    → set heart = filled (red) if wishlisted = true, else outline
  On heart tap:
    if wishlisted → DELETE /wishlist/{listingId}  → heart = outline
    if not        → POST   /wishlist/{listingId}  → heart = filled
```

### Optimistic UI (recommended)

Toggle the heart immediately on tap, then fire the API call in background. Revert on error.

```js
async function toggleWishlist(listingId, currentlyWishlisted) {
  // 1. Optimistic update — instant feedback
  setWishlisted(!currentlyWishlisted);
  updateBadgeCount(!currentlyWishlisted ? +1 : -1);

  try {
    if (currentlyWishlisted) {
      await api.delete(`/wishlist/${listingId}`);
    } else {
      await api.post(`/wishlist/${listingId}`);
    }
  } catch (err) {
    // 2. Revert on failure
    setWishlisted(currentlyWishlisted);
    updateBadgeCount(currentlyWishlisted ? +1 : -1);
    showToast(err.message);  // e.g. "Session expired"
  }
}
```

### Heart icon states

| State | Icon | Color |
|-------|------|-------|
| Not wishlisted | ♡ outline | `#757575` (grey) |
| Wishlisted | ♥ filled | `#E53935` (red) |
| Loading | spinner / disabled | — |

---

## 6. Wishlist Screen

### Screen structure

```
[ ← Back ]  My Wishlist (12)
────────────────────────────
[ WishlistCard ]   ← ACTIVE listing
[ WishlistCard ]   ← SOLD listing (dimmed + badge)
[ WishlistCard ]   ← listing = null (deleted placeholder)
...

[ Empty state — if list is empty ]
  "Aapki wishlist khaali hai"
  "Dil pasand listings save karein ❤️"
  [ Browse Listings button ]
```

### Fetching

```js
// On screen mount
const response = await api.get('/wishlist');
const items = response.data;  // WishlistItem[]
```

### Rendering each item

```js
items.map(item => {
  if (!item.listing) {
    return <DeletedPlaceholder listingId={item.listingId} onRemove={() => removeFromWishlist(item.listingId)} />;
  }
  return (
    <WishlistCard
      listing={item.listing}
      savedAt={item.savedAt}
      statusBadge={getStatusBadge(item.listing)}
      onRemove={() => removeFromWishlist(item.listingId)}
      onTap={() => item.listing.status === 'ACTIVE' && navigateToDetail(item.listingId)}
    />
  );
})
```

### Remove from wishlist screen

Each wishlist card has a remove button (trash icon / filled heart). On tap:

```js
async function removeFromWishlist(listingId) {
  // Remove from local list immediately
  setItems(prev => prev.filter(i => i.listingId !== listingId));
  await api.delete(`/wishlist/${listingId}`);
}
```

---

## 7. Wishlist Badge Counter

The navbar / profile drawer shows a badge with the wishlist count.

```js
// On app init / after login: fetch wishlist to get count
const items = await api.get('/wishlist');
setWishlistCount(items.data.length);

// On add → increment by 1
// On remove → decrement by 1 (min 0)
```

Show the badge only when count > 0. No badge when 0.

---

## 8. Error Handling

| HTTP | `error.code` | When | What to do |
|------|-------------|------|-----------|
| 401 | `UNAUTHORIZED` | Not logged in / token expired | Redirect to Login |
| 404 | `NOT_FOUND` | Listing doesn't exist (add) | Show toast — "Listing nahi mili" |
| 404 | `NOT_FOUND` | Not in wishlist (remove) | Silently ignore — already removed |

> For the 404 on remove — this happens if the user double-taps or the state is out of sync.  
> Just ignore the error and keep the heart as outline.

---

## 9. Implementation Checklist

- [ ] Heart button on every `ListingCard` — outline by default
- [ ] On login: check wishlist status for visible listing cards (`/wishlist/{id}/status`)
- [ ] Optimistic toggle on heart tap — revert on error
- [ ] Wishlist screen in Profile / Drawer
- [ ] Navbar badge counter (count of saved items)
- [ ] Status-aware rendering: SOLD → dimmed + red badge, INACTIVE → dimmed + grey badge
- [ ] Deleted listing (`listing: null`) → placeholder with remove button
- [ ] Empty state on wishlist screen
- [ ] Guest user → redirect to login on heart tap

---

*Last updated: 2026-08-09 — Wishlist module implemented (V10 migration, full backend)*
