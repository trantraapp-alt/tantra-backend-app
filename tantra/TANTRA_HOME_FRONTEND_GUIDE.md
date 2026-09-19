# Tantra Home Page — Frontend Developer Guide

> **Version:** 2026-08-01  
> **Backend base URL:** `http://localhost:8080/api/v1`  
> **Reference HTML:** `tantra_home.html` (project root — full working prototype)  
> **API contract:** `FRONTEND_API.md` (project root — complete endpoint reference)

---

## Table of Contents

1. [Project Overview](#1-project-overview)
2. [Technology & Theme](#2-technology--theme)
3. [Page Layout Map](#3-page-layout-map)
4. [Section-by-Section Build Guide](#4-section-by-section-build-guide)
   - 4.1 Sticky Header
   - 4.2 Active Filter Chips Bar
   - 4.3 Module Tab Navigation
   - 4.4 Hero Carousel (Promo Cards)
   - 4.5 Trust Stats Bar
   - 4.6 Dual Mini Banners
   - 4.7 Category Emoji Grid
   - 4.8 Trending Searches
   - 4.9 Flash Deals with Countdown
   - 4.10 MSP Price Banner
   - 4.11 Govt Scheme Banner
   - 4.12 Top Seller Stories Strip
   - 4.13 Featured Listings Rail
   - 4.14 Dynamic Module + Category Rails (all categories from DB)
   - 4.15 Businesses Grid
   - 4.16 Recently Added Grid
   - 4.17 Footer
5. [Overlay Screens](#5-overlay-screens)
   - 5.1 Listing Detail Screen
   - 5.2 Search Screen
   - 5.3 Nearby Screen
   - 5.4 Saved / Wishlist Screen
   - 5.5 Filter Panel
6. [Bottom Navigation Bar](#6-bottom-navigation-bar)
7. [API Integration — Home Page Data Flow](#7-api-integration--home-page-data-flow)
8. [Listing Card Component Spec](#8-listing-card-component-spec)
9. [Business Card Component Spec](#9-business-card-component-spec)
10. [Premium Badge & Highlight System](#10-premium-badge--highlight-system)
11. [Authentication Flow](#11-authentication-flow)
12. [Bilingual (EN / HI) Support](#12-bilingual-en--hi-support)
13. [Filter & Sort Reference](#13-filter--sort-reference)
14. [Color Palette & Design Tokens](#14-color-palette--design-tokens)
15. [OLX-Style Contact Reveal Flow](#15-olx-style-contact-reveal-flow)
16. [API Endpoint Cheat Sheet (Home Page)](#16-api-endpoint-cheat-sheet-home-page)
17. [Error Handling](#17-error-handling)
18. [Pagination](#18-pagination)
19. [Complete User Flow — Start to End](#19-complete-user-flow--start-to-end)

---

## 1. Project Overview

**Tantra** is India's hyperlocal agri-marketplace. The home page is an OLX/Flipkart-style feed showing:
- Promotional banners (admin-managed Promo Cards)
- Module-based listing sections (Crops, Seeds, Fertilizers, Equipment, Animals, Vegetables, Fishery, Poultry)
- Featured premium seller listings
- Business directory profiles
- Government MSP prices and scheme info

The entire home feed is driven by a **single API call** (`GET /api/v1/home`) plus a handful of supplementary calls for stats, MSP, and trending searches.

---

## 2. Technology & Theme

**No external CSS/JS frameworks are required.** The prototype is pure HTML/CSS/JS. You can port it to React, Vue, or any framework.

### Color Tokens (CSS custom properties)

```css
:root {
  --green:    #1E5631;   /* primary brand green */
  --gold:     #C47A0A;   /* accent gold */
  --bg:       #F5F5F0;   /* page background (light) */
  --surface:  #FFFFFF;   /* card surface */
  --border:   #E0DDD5;   /* dividers */
  --txt:      #1A1A14;   /* primary text */
  --txt2:     #5A5A50;   /* secondary text */
  --txt3:     #8A8A80;   /* muted text */
  --sell:     #E8F5E9;   /* SELL badge bg */
  --rent:     #FFF8E1;   /* RENT badge bg */
  --neg:      #F3E5F5;   /* Negotiable badge bg */
  --radius:   10px;      /* default border radius */
  --shadow:   0 1px 6px rgba(0,0,0,.08);
  --font:     'Segoe UI', system-ui, sans-serif;
}

/* Dark mode */
@media (prefers-color-scheme: dark) {
  :root {
    --bg:      #141410;
    --surface: #1E1E18;
    --border:  #2A2A22;
    --txt:     #F0EDE6;
    --txt2:    #B0AD9A;
    --txt3:    #706D5E;
  }
}
```

### Plan Badge Colors

| Plan       | Border class | Badge color | Badge symbol |
|------------|-------------|-------------|--------------|
| ENTERPRISE | `amb`        | Amber `#D4920A` | 🏆 TOP SELLER |
| PREMIUM    | `gb`         | Gold `#C8A000`  | ⭐ PREMIUM    |
| STANDARD   | `greenb`     | Green `#1E5631` | ✔ STANDARD   |
| BASIC      | *(none)*     | —               | *(no badge)*  |

---

## 3. Page Layout Map

```
┌─────────────────────────────────────────────────────────┐
│  STICKY HEADER: Logo | Search | Filter | Lang | Login   │
│  Location pill | Radius pill | Weather pill             │
├─────────────────────────────────────────────────────────┤
│  ACTIVE FILTER CHIPS (shows applied filters)            │
├─────────────────────────────────────────────────────────┤
│  MODULE TABS: Agriculture | Animal & Livestock           │
│  (dynamic from GET /api/v1/masters/modules)             │
├─────────────────────────────────────────────────────────┤
│  HERO CAROUSEL (Promo Cards - admin managed)            │
├─────────────────────────────────────────────────────────┤
│  TRUST STATS BAR: Farmers | Districts | Traded | Sat%  │
├─────────────────────────────────────────────────────────┤
│  DUAL MINI BANNERS  [Seeds Sale]  [Rent Tractor]        │
├─────────────────────────────────────────────────────────┤
│  MAIN CONTENT AREA:                                     │
│    • Category Emoji Grid (dynamic from DB)              │
│    • Trending Searches chips                            │
│    • Flash Deals + Countdown timer                      │
│    • MSP Price Banner                                   │
│    • Govt Scheme Banner (PM-KISAN)                      │
│    • Top Seller Stories strip                           │
│    • ⭐ Featured Listings (horizontal rail)              │
│    • [DYNAMIC] Per-module rails (one per module):       │
│        ─ 🌾 Agriculture Rail                           │
│            chips: Agri Marketplace | Agri Services |   │
│                   Repair & Maintenance                  │
│            sub-chips (when Agri Marketplace active):   │
│                   Crop | Seed | Pesticide |             │
│                   Fertilizer | Equipment                │
│        ─ 🐄 Animal & Livestock Rail                    │
│            chips: Veterinary | Animal Marketplace       │
│            sub-chips (when Animal Marketplace active): │
│                   Poultry | Fishery | Animal            │
│    • 🏪 Businesses Grid                                 │
│    • 🕐 Recently Added Grid                             │
│    • Footer                                             │
├─────────────────────────────────────────────────────────┤
│  BOTTOM NAV: Home | Nearby | [+SELL FAB] | Saved | Me  │
└─────────────────────────────────────────────────────────┘
```

---

## 4. Section-by-Section Build Guide

### 4.1 Sticky Header

**HTML structure:**
```html
<header class="mhdr" role="banner">
  <div class="hdr-top">
    <!-- Logo -->
    <div class="logo">TAN<em>TRA</em></div>

    <!-- Search bar -->
    <div class="srch-w">
      <!-- search icon SVG -->
      <input type="search" id="mainSrch" placeholder="Search crops, seeds, equipment…"
             onfocus="openSearch()">
      <button onclick="startVoiceSearch()">🎤</button>  <!-- Web Speech API -->
    </div>

    <!-- Filter button (opens filter panel) -->
    <button onclick="openFilter()">Filter</button>

    <div class="hdr-acts">
      <button>🔔</button>                     <!-- Notifications dot -->
      <button onclick="toggleTheme()">🌙</button>  <!-- Dark/light toggle -->
      <button onclick="toggleLang()">हिन्दी</button> <!-- EN/HI toggle -->
      <button>Login</button>
      <button>+ Sell</button>
    </div>
  </div>

  <!-- Location + radius bar -->
  <div class="loc-bar">
    <button class="lpill">Pune ▾</button>         <!-- district selector -->
    <button class="rpill">25 km ▾</button>        <!-- radius selector -->
    <span class="weather-pill">☀️ 31°C · Good day to sell</span>
    <span>Maharashtra, India</span>
  </div>
</header>
```

**Behaviors:**
- Header sticks to top (`position: sticky; top: 0; z-index: 100`)
- Search input focus slides open the Search overlay screen
- Voice search uses `window.SpeechRecognition` (WebSpeech API)
- Notification dot is a small red circle — badge count from `GET /api/v1/notifications/unread-count`
- District pill → show district picker → re-fetch home feed with new `?district=` param
- Radius pill → show radius options (5/10/25/50/100 km) → update nearby filter

---

### 4.2 Active Filter Chips Bar

Shows applied filters as removable chips. Hidden when no filters are active.

```html
<div class="afc-bar" id="afcBar">
  <div class="afc-inner" id="afcInner">
    <!-- chips injected by JS when filters applied -->
    <!-- e.g.: <span class="afc-chip">SELL <button onclick="removeFilter('lt')">✕</button></span> -->
  </div>
</div>
```

**Logic:** When a filter is applied via the filter panel, render a chip here. Clicking the `✕` on a chip removes that filter and re-runs the search/home render.

---

### 4.3 Module Tab Navigation

**Tabs are fully dynamic** — rendered from `GET /api/v1/masters/modules`. There are currently **2 modules** in the DB:

| moduleKey | nameEn | nameHi |
|-----------|--------|--------|
| `agriculture` | Agriculture | कृषि |
| `animal_livestock` | Animal & Livestock | पशु एवं पशुधन |

> **Note:** `AppModule` has no `emoji` field in the DB. Use the emoji map below client-side, keyed on `moduleKey`.

**Client-side emoji map:**
```js
const MODULE_EMOJI = {
  agriculture:      '🌾',
  animal_livestock: '🐄',
};
```

**Dynamic render:**
```js
async function renderModuleTabs() {
  const res = await fetch('/api/v1/masters/modules');
  const { data: modules } = await res.json();

  const tabsEl = document.getElementById('modTabs');
  tabsEl.innerHTML = modules
    .filter(m => m.isActive)
    .map((m, i) => {
      const emoji = MODULE_EMOJI[m.moduleKey] || '📦';
      const label = lang === 'en' ? m.moduleNameEn : m.moduleNameHi;
      return `<button class="mt ${i===0?'on':''}"
                data-module-id="${m.id}"
                onclick="setTab(this, ${m.id})">
                <span>${emoji}</span>${label}
              </button>`;
    }).join('');

  animateTabIndicator(tabsEl.querySelector('.on'));
}
```

**HTML skeleton (before JS populates):**
```html
<nav class="mtabs-sec">
  <div class="tab-indicator" id="tabIndicator"></div>
  <div class="mtabs" id="modTabs">
    <!-- injected by renderModuleTabs() -->
  </div>
</nav>
```

**Behavior:**
- Active tab has class `on` + sliding underline indicator
- Clicking a tab scrolls the page to that module's section via `document.getElementById('module-'+moduleId).scrollIntoView()`
- The sliding underline is animated via JS reading `getBoundingClientRect()` of the active tab button

---

### 4.4 Hero Carousel (Promo Cards)

Auto-playing image/SVG carousel driven by admin-managed Promo Cards.

```html
<section class="car-sec">
  <div class="c-track" id="cTrack">
    <!-- One .c-slide per PromoCard -->
    <div class="c-slide">
      <div class="c-bg"><!-- SVG gradient background --></div>
      <div class="c-txt">
        <h2>Rabi Season — Sell Your Wheat Faster</h2>
        <p>Connect directly with buyers. No middlemen.</p>
        <button class="c-btn">Browse Crops →</button>
      </div>
    </div>
  </div>
  <button class="c-arr p" onclick="moveSlide(-1)">←</button>
  <button class="c-arr n" onclick="moveSlide(1)">→</button>
  <div class="c-dots" id="cDots"></div>
</section>
```

**API:** `GET /api/v1/home` → `data.promoCards[]`

**PromoCard fields:**

| Field | Type | Usage |
|-------|------|-------|
| `id` | string | unique key |
| `titleEn` | string | slide headline (EN) |
| `titleHi` | string | slide headline (HI) |
| `subtitleEn` | string | slide subtext (EN) |
| `subtitleHi` | string | slide subtext (HI) |
| `ctaLabelEn` | string | button text (EN) |
| `ctaLabelHi` | string | button text (HI) |
| `ctaType` | enum | `MODULE` / `CATEGORY` / `EXTERNAL` / `NONE` |
| `ctaTarget` | string | moduleId / categoryId / URL depending on ctaType |
| `imageUrl` | string | optional image URL (use SVG gradient if null) |
| `bgColorFrom` | string | gradient start hex |
| `bgColorTo` | string | gradient end hex |
| `textColor` | string | slide text color |
| `displayOrder` | int | sort order |
| `isActive` | boolean | only show when true |

**Auto-play:** 4-second interval, pause on touch/mouse hover, resume on leave.

**Touch swipe:** Track `touchstart` → `touchend` delta. If `|deltaX| > 50px`, advance/retreat slide.

**Dots:** Render one dot per slide. Click a dot to jump to that slide.

---

### 4.5 Trust Stats Bar

Animated counters that count up when scrolled into view (IntersectionObserver).

```html
<div class="trust-bar">
  <div class="trust-item">
    <span class="trust-num" data-target="12400" id="tFarmers">12,400+</span>
    <span class="trust-lbl">Farmers</span>
  </div>
  <div class="trust-sep"></div>
  <div class="trust-item">
    <span class="trust-num" data-target="48" id="tDistricts">48</span>
    <span class="trust-lbl">Districts</span>
  </div>
  <div class="trust-sep"></div>
  <div class="trust-item">
    <span class="trust-num" id="tTraded">₹4.2Cr+</span>
    <span class="trust-lbl">Traded</span>
  </div>
  <div class="trust-sep"></div>
  <div class="trust-item">
    <span class="trust-num" data-target="98" id="tSatisfied">98%</span>
    <span class="trust-lbl">Satisfied</span>
  </div>
</div>
```

**API:** `GET /api/v1/stats/public`

**Response fields (`PublicStatsDTO`):**

| API Field | Display Label | Format |
|-----------|---------------|--------|
| `sellerCount` | Farmers | `12,400+` (Indian locale) |
| `districtCount` | Districts | `48` |
| `totalTradeValue` | Traded | `₹4.2Cr+` (convert to Cr if ≥ 1,00,000) |
| `satisfiedPct` | Satisfied | `98%` |

**Counter animation (JS):**
```js
function animateCounter(el, target, duration = 1500) {
  let start = 0;
  const step = target / (duration / 16);
  const timer = setInterval(() => {
    start += step;
    if (start >= target) { el.textContent = target.toLocaleString('en-IN') + '+'; clearInterval(timer); return; }
    el.textContent = Math.floor(start).toLocaleString('en-IN');
  }, 16);
}
```

---

### 4.6 Dual Mini Banners

Two side-by-side promotional banners below the trust bar.

```html
<div class="dual-banners">
  <div class="mini-banner" onclick="navigate('seeds-sale')">
    <!-- SVG gradient bg -->
    <h4>🌱 Kharif Seeds Sale</h4>
    <p>Upto 20% off · Limited stock</p>
  </div>
  <div class="mini-banner" onclick="navigate('equipment-rent')">
    <!-- SVG gradient bg -->
    <h4>🚜 Rent a Tractor</h4>
    <p>Starting ₹800/day · Near you</p>
  </div>
</div>
```

**Data source:** Can be driven by `promoCards` with `ctaType: EXTERNAL` or hardcoded seasonal banners. In production, use the promo card API with a `placement: MINI_BANNER` field.

**Layout:** `display: grid; grid-template-columns: 1fr 1fr; gap: 0.75rem;`

---

### 4.7 Category Emoji Grid

**Fully dynamic** — shows every top-level category across all modules. Uses the two-level category tree from the DB.

**Full category list from DB (as of current seed):**

| moduleKey | categoryKey | nameEn | nameHi | actionType |
|-----------|-------------|--------|--------|-----------|
| `agriculture` | `agri_marketplace` | Agriculture Marketplace | कृषि बाज़ार | LISTING |
| `agriculture` | `agri_services` | Agriculture Services | कृषि सेवाएँ | LISTING |
| `agriculture` | `repair` | Repair & Maintenance | मरम्मत और रखरखाव | LISTING |
| `animal_livestock` | `veterinary` | Veterinary | पशु चिकित्सा | BUSINESS_PROFILE |
| `animal_livestock` | `animal_marketplace` | Animal Marketplace | पशु बाज़ार | LISTING |

**Client-side category emoji map** (no icon stored in DB for these top-level categories):
```js
const CATEGORY_EMOJI = {
  agri_marketplace: '🌾',
  agri_services:    '🛠️',
  repair:           '🔧',
  veterinary:       '🩺',
  animal_marketplace: '🐄',
  // sub-categories
  crop:       '🌾',
  seed:       '🌱',
  pesticide:  '🧴',
  fertilizer: '🧪',
  equipment:  '🚜',
  poultry:    '🐔',
  fishery:    '🐟',
  animal:     '🐮',
};
```

**Dynamic render:**
```js
async function renderCategoryGrid() {
  const modulesRes = await fetch('/api/v1/masters/modules');
  const { data: modules } = await modulesRes.json();

  // Fetch top-level categories for each active module
  const catRequests = modules
    .filter(m => m.isActive)
    .map(m => fetch(`/api/v1/masters/modules/${m.id}/categories`)
      .then(r => r.json())
      .then(j => j.data));

  const allTopCats = (await Promise.all(catRequests)).flat();

  const grid = document.getElementById('catGrid');
  grid.innerHTML = allTopCats
    .filter(c => c.isActive)
    .sort((a, b) => a.displayOrder - b.displayOrder)
    .map(c => {
      const emoji = CATEGORY_EMOJI[c.categoryKey] || '📦';
      const label = lang === 'en' ? c.categoryNameEn : c.categoryNameHi;
      return `<div class="cat-cell" onclick="onCategoryTap(${c.id}, '${c.actionType}', '${c.linkKey || ''}')">
        <div class="cat-icon">${emoji}</div>
        <div class="cat-lbl">${label}</div>
      </div>`;
    }).join('');
}

function onCategoryTap(categoryId, actionType, linkKey) {
  if (actionType === 'BUSINESS_PROFILE') {
    openBusinessDirectory({ profileType: linkKey }); // e.g. vet_clinic
  } else {
    openListingsBrowse({ categoryId });
  }
}
```

**CSS:** `display: grid; grid-template-columns: repeat(4, 1fr); gap: 0.65rem;`

---

### 4.8 Trending Searches

Horizontal scroll row of clickable search chips.

```html
<div class="trending-sec">
  <div class="trending-lbl">🔥 Trending Searches</div>
  <div class="trending-row">
    <button class="trend-chip" onclick="trendSearch('Wheat')">🌾 Wheat</button>
    <button class="trend-chip" onclick="trendSearch('Gir Cow')">🐄 Gir Cow</button>
    <!-- more chips -->
  </div>
</div>
```

**API:** `GET /api/v1/search/trending`

**Response:**
```json
[
  { "query": "Wheat", "count": 4200 },
  { "query": "Gir Cow", "count": 2100 },
  { "query": "Rotavator", "count": 1800 }
]
```

Render the top 8 results as chips. Clicking a chip pre-fills and opens the search overlay.

---

### 4.9 Flash Deals with Countdown

Listings with an active discount + time-limited offer. Compact horizontal rail.

```html
<div class="flash-sec">
  <div class="flash-hd">
    <div class="flash-title">⚡ Flash Deals</div>
    <div class="countdown">
      <div class="cd-block"><span id="cdH">02</span><span>Hrs</span></div>
      <span>:</span>
      <div class="cd-block"><span id="cdM">14</span><span>Min</span></div>
      <span>:</span>
      <div class="cd-block"><span id="cdS">33</span><span>Sec</span></div>
    </div>
  </div>
  <div class="flash-rail" id="flashRail">
    <!-- flash cards rendered here -->
  </div>
</div>
```

**Flash Card Component:**
```html
<div class="flash-card" onclick="openDetail(listingId)">
  <div class="flash-img">
    <span>🌾</span>                        <!-- emoji or image -->
    <span class="flash-disc-b">12% OFF</span>
  </div>
  <div class="flash-info">
    <div class="flash-name">Wheat — Sharbati Grade</div>
    <div class="flash-price">₹1,936<span>/quintal</span></div>
    <div class="flash-old">₹2,200</div>   <!-- strikethrough original price -->
  </div>
</div>
```

**API:** `GET /api/v1/listings/flash-deals` (public)

**Response:** Paginated `ListingCardDTO[]` where `flashDeal = true`.

**Countdown timer:** Set countdown to `discountExpiresAt` from the first flash deal, or to end of day (midnight). Update every second with `setInterval`.

```js
function startCountdown(targetISODate) {
  setInterval(() => {
    const diff = new Date(targetISODate) - new Date();
    const h = Math.floor(diff / 3600000);
    const m = Math.floor((diff % 3600000) / 60000);
    const s = Math.floor((diff % 60000) / 1000);
    document.getElementById('cdH').textContent = String(h).padStart(2,'0');
    document.getElementById('cdM').textContent = String(m).padStart(2,'0');
    document.getElementById('cdS').textContent = String(s).padStart(2,'0');
  }, 1000);
}
```

---

### 4.10 MSP Price Banner

Tappable banner that expands to show all MSP rates.

```html
<div class="msp-banner" onclick="openMspSheet()">
  <div class="msp-icon">🏛️</div>
  <div class="msp-body">
    <div class="msp-title">Govt MSP for Wheat: ₹2,275/q — Know your rights</div>
    <div class="msp-desc">Minimum Support Price 2025-26 · Tap to view all crops MSP rates</div>
  </div>
  <div class="msp-arr">›</div>
</div>
```

**API:** `GET /api/v1/msp/current` (public, no auth)

**Response fields (`MspPrice`):**
- `cropKey` — e.g. `"WHEAT"`, `"PADDY"`, `"TUR"`
- `cropNameEn` / `cropNameHi` — display names
- `season` — `"RABI"` | `"KHARIF"` | `"COMMERCIAL"`
- `pricePerQuintal` — BigDecimal (INR)
- `year` — e.g. `2026`
- `isActive` — boolean

**Render the banner headline** using the wheat MSP value. On tap, show a bottom sheet with the full table sorted by season, then crop name.

---

### 4.11 Govt Scheme Banner

```html
<div class="scheme-banner" onclick="openSchemeSheet()">
  <div class="scheme-icon">🇮🇳</div>
  <div class="scheme-body">
    <div class="scheme-title">PM-KISAN — ₹6,000/year direct to your account</div>
    <div class="scheme-desc">Check eligibility · Apply online · Instant status update</div>
  </div>
  <div class="scheme-arr">›</div>
</div>
```

**Data:** Hardcoded in the prototype. In production this can be a CMS-managed item. For v1, keep as static content.

---

### 4.12 Top Seller Stories Strip

Instagram-style story rings for top business profiles. Horizontally scrollable.

```html
<div class="stories-sec">
  <h2>🌟 Top Sellers</h2>
  <div class="stories-row">
    <div class="story-item" onclick="openSellerProfile(profileId)">
      <div class="story-ring">        <!-- green ring = unviewed, grey = viewed -->
        <div class="story-av">🌱</div>  <!-- emoji avatar -->
      </div>
      <div class="story-nm">Krishnamurthy</div>
      <div class="story-sub">45 listings</div>
    </div>
    <!-- more sellers -->
  </div>
</div>
```

**CSS ring:** `border: 2px solid var(--green); border-radius: 50%;` — add `.viewed` class for grey ring on seen sellers.

**API:** `GET /api/v1/business-profiles/top-sellers` (public)

**Response fields (BusinessProfileCard):**

| Field | Usage |
|-------|-------|
| `profileId` | navigate to seller profile on click |
| `businessName` | truncated display name (12 chars max) |
| `planKey` | ring color (ENTERPRISE=amber, PREMIUM=gold, else green) |
| `listingCount` | subtitle `"N listings"` |
| `emoji` or first char of name | avatar fallback |

**Show 6 sellers max** in the prototype. Scroll on mobile.

---

### 4.13 Featured Listings Rail

Premium listings shown with a skeleton shimmer loading state.

```html
<!-- Skeleton (shown while loading) -->
<div class="skeleton-rail" id="featSkel">
  <div class="sk-card">
    <div class="sk-img"></div>
    <div class="sk-line"></div>
    <div class="sk-line sm"></div>
    <div class="sk-line xs"></div>
  </div>
  <!-- repeat 3x -->
</div>

<!-- Real cards (shown after load) -->
<div class="rail" id="featRail" style="display:none" role="list"></div>
```

**Skeleton shimmer CSS:**
```css
.sk-img, .sk-line {
  background: linear-gradient(90deg, #eee 25%, #f5f5f5 50%, #eee 75%);
  background-size: 200% 100%;
  animation: shimmer 1.4s infinite;
}
@keyframes shimmer { to { background-position: -200% 0; } }
```

**API:** `GET /api/v1/home` → `data.featuredListings.listings[]`

**Render:** Use the [Listing Card Component](#8-listing-card-component-spec). Show skeleton for 600ms minimum before revealing cards (UX smoothness).

---

### 4.14 Dynamic Module + Category Rails

> **All rails are dynamic.** The home API returns one `ModuleSection` per active module. Each section is rendered as a rail — no hardcoding.

---

#### Complete Category Structure (from DB)

```
Module: Agriculture (🌾)  [id auto-assigned]
├── agri_marketplace  — Agriculture Marketplace (कृषि बाज़ार)  [LISTING, top-level]
│   ├── crop          — Crop (फसल)
│   ├── seed          — Seed (बीज)
│   ├── pesticide     — Pesticide (कीटनाशक)
│   ├── fertilizer    — Fertilizer (उर्वरक)
│   └── equipment     — Equipment (उपकरण)
├── agri_services     — Agriculture Services (कृषि सेवाएँ)  [LISTING, leaf]
└── repair            — Repair & Maintenance (मरम्मत और रखरखाव)  [LISTING, leaf]

Module: Animal & Livestock (🐄)  [id auto-assigned]
├── veterinary        — Veterinary (पशु चिकित्सा)  [BUSINESS_PROFILE → vet_clinic]
└── animal_marketplace — Animal Marketplace (पशु बाज़ार)  [LISTING, top-level]
    ├── poultry       — Poultry (मुर्गी पालन)
    ├── fishery       — Fishery (मत्स्य पालन)
    └── animal        — Animal (पशु)
```

---

#### API Calls for Module Rails

| What | Endpoint | When |
|------|----------|------|
| Initial home listings | `GET /api/v1/home` → `moduleSections[]` | page load |
| Filter by top-level category | `GET /api/v1/listings/category/{categoryId}` | chip tap |
| Load subcategories | `GET /api/v1/masters/categories/{parentId}/subcategories` | chip tap on parent |
| Vet clinic business directory | `GET /api/v1/business-profiles/directory?profileType=vet_clinic` | veterinary tap |

---

#### Dynamic Render Pattern

The home response gives one `ModuleSection` per active module:

```json
{
  "moduleSections": [
    {
      "module": { "id": 1, "moduleKey": "agriculture", "moduleNameEn": "Agriculture", "moduleNameHi": "कृषि" },
      "topCategories": [
        { "id": 10, "categoryKey": "agri_marketplace", "categoryNameEn": "Agriculture Marketplace", "parentId": null, "actionType": "LISTING" },
        { "id": 11, "categoryKey": "agri_services", "categoryNameEn": "Agriculture Services", "parentId": null, "actionType": "LISTING" },
        { "id": 12, "categoryKey": "repair", "categoryNameEn": "Repair & Maintenance", "parentId": null, "actionType": "LISTING" }
      ],
      "listings": [ ...up to 6 ListingCardDTO... ]
    },
    {
      "module": { "id": 2, "moduleKey": "animal_livestock", "moduleNameEn": "Animal & Livestock", "moduleNameHi": "पशु एवं पशुधन" },
      "topCategories": [
        { "id": 20, "categoryKey": "veterinary", "categoryNameEn": "Veterinary", "parentId": null, "actionType": "BUSINESS_PROFILE", "linkKey": "vet_clinic" },
        { "id": 21, "categoryKey": "animal_marketplace", "categoryNameEn": "Animal Marketplace", "parentId": null, "actionType": "LISTING" }
      ],
      "listings": [ ...up to 6 ListingCardDTO... ]
    }
  ]
}
```

**JS — render all module rails from one loop:**
```js
function renderModuleSections(moduleSections) {
  const container = document.getElementById('moduleSectionsContainer');
  container.innerHTML = '';

  moduleSections.forEach(section => {
    const moduleId  = section.module.id;
    const moduleKey = section.module.moduleKey;
    const name      = lang === 'en' ? section.module.moduleNameEn : section.module.moduleNameHi;
    const emoji     = MODULE_EMOJI[moduleKey] || '📦';

    // Build category chip HTML
    const chips = section.topCategories.map((cat, i) => {
      const catEmoji = CATEGORY_EMOJI[cat.categoryKey] || '';
      const catLabel = lang === 'en' ? cat.categoryNameEn : cat.categoryNameHi;
      return `<button class="chip ${i===0?'on':''}"
                data-cat-id="${cat.id}"
                data-action="${cat.actionType}"
                data-link-key="${cat.linkKey || ''}"
                onclick="onChipTap(this, ${moduleId})">
                ${catEmoji} ${catLabel}
              </button>`;
    }).join('');

    // Build listing card HTML
    const cards = section.listings.length
      ? section.listings.map(renderCard).join('')
      : `<div class="empty-rail">No listings yet in this category.</div>`;

    container.insertAdjacentHTML('beforeend', `
      <div class="msec" id="module-${moduleId}">
        <div class="msechd">
          <h2 class="sttl">${emoji} ${name}</h2>
          <button class="see" onclick="openListingsBrowse({moduleId: ${moduleId}})">
            ${lang === 'en' ? 'See all →' : 'सभी →'}
          </button>
        </div>
        <div class="cchips" id="chips-${moduleId}">${chips}</div>
        <div class="rail" id="rail-${moduleId}" role="list">${cards}</div>
      </div>`);
  });
}
```

**Chip tap — loads listings for that category (and sub-chips if it has children):**
```js
async function onChipTap(chipEl, moduleId) {
  const categoryId = chipEl.dataset.catId;
  const actionType = chipEl.dataset.action;
  const linkKey    = chipEl.dataset.linkKey;

  // Update active chip
  chipEl.closest('.cchips').querySelectorAll('.chip').forEach(c => c.classList.remove('on'));
  chipEl.classList.add('on');

  if (actionType === 'BUSINESS_PROFILE') {
    openBusinessDirectory({ profileType: linkKey });
    return;
  }

  // Load subcategories to show as a second row of sub-chips
  const subRes = await fetch(`/api/v1/masters/categories/${categoryId}/subcategories`);
  const { data: subCats } = await subRes.json();
  renderSubChips(moduleId, subCats, categoryId);

  // Load listings for this category
  const listRes = await fetch(`/api/v1/listings/category/${categoryId}?page=0&size=10`, {
    headers: authHeader()
  });
  const { data } = await listRes.json();
  document.getElementById('rail-' + moduleId).innerHTML =
    data.content.map(renderCard).join('') || `<div class="empty-rail">No listings found.</div>`;
}

function renderSubChips(moduleId, subCats, parentCatId) {
  const chipsEl = document.getElementById('chips-' + moduleId);
  // Remove any previous sub-chip row
  const existing = chipsEl.querySelector('.sub-chips-row');
  if (existing) existing.remove();

  if (!subCats.length) return;

  const row = document.createElement('div');
  row.className = 'sub-chips-row cchips';
  row.innerHTML = subCats.map(sc => {
    const e = CATEGORY_EMOJI[sc.categoryKey] || '';
    const l = lang === 'en' ? sc.categoryNameEn : sc.categoryNameHi;
    return `<button class="chip sub"
              data-cat-id="${sc.id}"
              data-action="LISTING"
              onclick="onSubChipTap(this, ${moduleId})">${e} ${l}</button>`;
  }).join('');

  chipsEl.after(row);
}

// Sub-category chip tap — loads listings for the specific leaf category
async function onSubChipTap(chipEl, moduleId) {
  // Update active sub-chip
  chipEl.closest('.sub-chips-row').querySelectorAll('.chip').forEach(c => c.classList.remove('on'));
  chipEl.classList.add('on');

  const categoryId = chipEl.dataset.catId;
  const token = localStorage.getItem('tantra_token');

  // Show skeleton while loading
  const rail = document.getElementById('rail-' + moduleId);
  rail.innerHTML = skeletonCards(4);

  const res = await fetch(
    `/api/v1/listings/category/${categoryId}?page=0&size=10`,
    { headers: token ? { Authorization: 'Bearer ' + token } : {} }
  );
  const { data } = await res.json();

  rail.innerHTML = (data.content && data.content.length)
    ? data.content.map(renderCard).join('')
    : `<div class="empty-rail">No listings found in this sub-category.</div>`;
}

function skeletonCards(n) {
  return Array.from({ length: n }).map(() =>
    `<div class="sk-card"><div class="sk-img"></div><div class="sk-line"></div><div class="sk-line sm"></div></div>`
  ).join('');
}
```

---

#### Sub-category Reference Table

**Agriculture → Agri Marketplace sub-categories:**

| categoryKey | nameEn | nameHi | emoji |
|-------------|--------|--------|-------|
| `crop` | Crop | फसल | 🌾 |
| `seed` | Seed | बीज | 🌱 |
| `pesticide` | Pesticide | कीटनाशक | 🧴 |
| `fertilizer` | Fertilizer | उर्वरक | 🧪 |
| `equipment` | Equipment | उपकरण | 🚜 |

**Animal & Livestock → Animal Marketplace sub-categories:**

| categoryKey | nameEn | nameHi | emoji |
|-------------|--------|--------|-------|
| `poultry` | Poultry | मुर्गी पालन | 🐔 |
| `fishery` | Fishery | मत्स्य पालन | 🐟 |
| `animal` | Animal | पशु | 🐮 |

---

#### Veterinary — Special Case (BUSINESS_PROFILE)

The `veterinary` category chip does **not** open a listing browse. Instead it opens the Business Directory filtered to vet clinics.

```js
// When chip with actionType === 'BUSINESS_PROFILE' is tapped:
openBusinessDirectory({ profileType: 'vet_clinic' });

// API call:
// GET /api/v1/business-profiles/directory?profileType=vet_clinic
```

Show vet clinic business cards (same Business Card Component as the Businesses Grid section), not listing cards.

---

#### HTML Container Placeholder

```html
<main class="hmain">
  <div class="w">
    <!-- ... category grid, trending, flash deals, MSP, scheme, stories, featured ... -->

    <!-- Dynamic module section rails injected here by JS -->
    <div id="moduleSectionsContainer"></div>

    <!-- Businesses Grid and Recently Added follow -->
  </div>
</main>
```

**Call order on page load:**
```js
const homeData = await fetch('/api/v1/home?district=Pune&lang=EN').then(r => r.json());
renderModuleSections(homeData.data.moduleSections);  // renders ALL module rails dynamically
```

---

### 4.15 Businesses Grid

Grid of business profile cards (not a horizontal rail — grid layout).

```html
<section class="sec">
  <div class="sechd">
    <h2>🏪 Businesses</h2>
    <button class="see">See all →</button>
  </div>
  <div class="bgrid" id="bizGrid" role="list">
    <!-- Business cards rendered here -->
  </div>
</section>
```

**Business Card Component:**
```html
<article class="bcd amb" role="listitem" onclick="openSellerProfile(profileId)">
  <!-- .amb / .gb / .greenb class for plan highlight border -->
  <div class="bav">
    <span>🌱</span>
    <span class="bdt a">🏆</span>  <!-- plan dot badge -->
  </div>
  <div class="bnm">Krishnamurthy Seeds</div>
  <div class="blc">📍 Pune</div>
  <div class="bpl a">ENTERPRISE</div>  <!-- a=amber, g=gold, gr=green, b=none -->
</article>
```

**Grid CSS:** `display: grid; grid-template-columns: repeat(auto-fill, minmax(140px, 1fr)); gap: 0.75rem;`

**API:** `GET /api/v1/home` → `data.featuredProfiles[]`

---

### 4.16 Recently Added Grid

2-column grid (vs single-column rail) showing the latest 8 listings.

```html
<section class="sec">
  <div class="sechd">
    <h2>🕐 Recently Added</h2>
    <button class="see">See all →</button>
  </div>
  <div class="rgrid" id="recentGrid" role="list">
    <!-- Listing cards -->
  </div>
</section>
```

**Grid CSS:** `display: grid; grid-template-columns: 1fr 1fr; gap: 0.65rem;`

**API:** `GET /api/v1/home` → `data.recentListings[]`

---

### 4.17 Footer

```html
<footer class="ft">
  <div class="ftg">
    <div>
      <div class="ftlogo">TAN<em>TRA</em></div>
      <div>India's hyperlocal agri-marketplace.</div>
    </div>
    <div class="ftcol">
      <h4>Browse</h4>
      <ul><li>Crops</li><li>Seeds</li><li>Equipment</li><li>Animals</li></ul>
    </div>
    <div class="ftcol">
      <h4>Sellers</h4>
      <ul><li>Post Listing</li><li>Premium Plans</li><li>Seller Guide</li></ul>
    </div>
    <div class="ftcol">
      <h4>Help</h4>
      <ul><li>Contact Us</li><li>FAQ</li><li>Privacy</li></ul>
    </div>
  </div>
  <div class="ftbot">© 2026 Tantra Agri Technologies Pvt. Ltd.</div>
</footer>
```

---

## 5. Overlay Screens

All overlay screens are full-screen panels that slide in from the right. They sit in the DOM as `position: fixed` layers. Only one screen is open at a time; a stack tracks the back-navigation history.

**CSS:**
```css
.sco {
  position: fixed; inset: 0; z-index: 200;
  background: var(--bg);
  transform: translateX(100%);
  transition: transform 0.28s cubic-bezier(.4,0,.2,1);
  overflow-y: auto;
}
.sco.open { transform: translateX(0); }
```

**Screen navigation JS (global — must be defined once before any screen is opened):**
```js
let screenStack = [];  // e.g. ['home', 'detail', 'search']

function showScreen(name) {
  // Close current open screen
  document.querySelectorAll('.sco.open').forEach(el => el.classList.remove('open'));
  if (name !== 'home') {
    const el = document.getElementById('sc-' + name);
    if (el) el.classList.add('open');
  }
  screenStack.push(name);
  // Lock body scroll while overlay is open
  document.body.style.overflow = name === 'home' ? '' : 'hidden';
}

function goBack() {
  screenStack.pop();
  const prev = screenStack[screenStack.length - 1] || 'home';
  showScreen(prev);
}
```

**Overlay header (shared HTML pattern):**
```html
<div class="sco-hdr">
  <button class="sco-back" onclick="goBack()" aria-label="Back">←</button>
  <span class="sco-title" id="scTitle">Title</span>
  <div class="sco-acts"><!-- action buttons --></div>
</div>
```

---

### 5.1 Listing Detail Screen

**Triggered by:** `openDetail(listingId)` — called from `onclick` on any listing card.

**API:** `GET /api/v1/listings/{listingId}` — returns `ListingCardDTO` (same shape as card, all fields populated). No auth needed for public listings; JWT required when `showContact=true`.

**Similar listings API:** `GET /api/v1/listings/{listingId}/similar?limit=6` — returns `ListingCardDTO[]`.

---

#### Detail Screen HTML

```html
<div id="sc-detail" class="sco" role="dialog" aria-modal="true">

  <!-- Header -->
  <div class="sco-hdr">
    <button class="sco-back" onclick="goBack()">←</button>
    <span class="sco-title" id="detTitle">Listing Detail</span>
    <div class="sco-acts">
      <button class="sco-ib" id="detHrtBtn" onclick="toggleWishlistDetail()">🤍</button>
      <button class="sco-ib" onclick="shareDetail()">
        <!-- share SVG icon -->
      </button>
    </div>
  </div>

  <!-- Scrollable content (injected by openDetail()) -->
  <div id="detContent"></div>

  <!-- Sticky CTA bar at bottom -->
  <div class="det-cta">
    <button class="btn-contact-lg" id="detContactBtn" onclick="revealContact(null, true)">
      📞 View Contact
    </button>
    <button class="btn-chat-lg" onclick="alert('Chat coming soon')">💬</button>
  </div>

</div>
```

**Sticky CTA CSS:**
```css
.det-cta {
  position: sticky; bottom: 0;
  display: flex; gap: 0.5rem;
  padding: 0.75rem 1rem;
  background: var(--surface);
  border-top: 1px solid var(--border);
  z-index: 10;
}
.btn-contact-lg { flex: 1; background: var(--green); color: #fff; border-radius: 8px; padding: 0.75rem; font-weight: 700; }
.btn-chat-lg    { width: 48px; background: var(--border); border-radius: 8px; font-size: 1.2rem; }
```

---

#### `openDetail()` JS Function

```js
let currentDetailId = null;

async function openDetail(listingId) {
  currentDetailId = listingId;

  // Show screen immediately with loading state
  document.getElementById('detTitle').textContent = 'Loading…';
  document.getElementById('detContent').innerHTML = `
    <div class="det-loading">
      <div class="sk-img" style="height:200px;border-radius:0"></div>
      <div style="padding:1rem"><div class="sk-line"></div><div class="sk-line sm"></div></div>
    </div>`;
  showScreen('detail');

  // Fetch listing detail + similar in parallel
  const token = localStorage.getItem('tantra_token');
  const headers = token ? { Authorization: 'Bearer ' + token } : {};

  const [detRes, simRes] = await Promise.all([
    fetch(`/api/v1/listings/${listingId}`, { headers }),
    fetch(`/api/v1/listings/${listingId}/similar?limit=6`, { headers })
  ]);

  const { data: l }       = await detRes.json();
  const { data: similar } = await simRes.json();

  renderDetailContent(l, similar || []);
}

function renderDetailContent(l, similar) {
  document.getElementById('detTitle').textContent = l.listingTitle || 'Listing Detail';

  // Wishlist state
  const saved = wishlist.has(l.listingId);
  document.getElementById('detHrtBtn').textContent = saved ? '❤️' : '🤍';

  // Discount badge
  const discBadge = l.discountPct
    ? `<div class="det-disc-b">${l.discountPct}% OFF</div>` : '';

  // Plan badge
  const planBadge = {
    ENTERPRISE: '<div class="det-plan-b a">🏆 TOP SELLER</div>',
    PREMIUM:    '<div class="det-plan-b g">⭐ PREMIUM</div>',
    STANDARD:   '<div class="det-plan-b gr">✔ STANDARD</div>',
  }[l.sellerPlanKey] || '';

  // Old price
  const oldPr = l.actualPrice
    ? `<span class="det-pro">₹${fmt(l.actualPrice)}</span>
       <span class="det-disc">${l.discountPct}% OFF</span>` : '';

  // Attributes key-value grid
  const attrHtml = l.attributes
    ? Object.entries(l.attributes).map(([k, v]) =>
        `<div class="attr-item"><div class="attr-k">${k}</div><div class="attr-v">${v}</div></div>`
      ).join('')
    : '';

  // Image or emoji
  const hero = (l.images && l.images.length)
    ? `<img src="/files/${l.images[0]}" alt="${l.listingTitle}" style="width:100%;height:220px;object-fit:cover">`
    : `<div class="ciph" style="font-size:5rem;text-align:center;padding:2rem">${MODULE_EMOJI[l.moduleKey] || '📦'}</div>`;

  // Type/plan tags
  const typeTag  = `<span class="det-tag ${l.listingType==='SELL'?'s':'r'}">${l.listingType}</span>`;
  const negTag   = l.isNegotiable ? '<span class="det-tag n">Negotiable</span>' : '';

  // Similar listings rail
  const simHtml = similar.length
    ? `<div class="similar-sec">
         <div class="det-sec-title">Similar Listings</div>
         <div class="rail">${similar.map(renderCard).join('')}</div>
       </div>` : '';

  // Relative time
  const ago = relativeTime(l.createdAt);

  document.getElementById('detContent').innerHTML = `
    <!-- Hero -->
    <div class="det-hero">${hero}${discBadge}${planBadge}</div>

    <!-- Stats bar -->
    <div class="det-stat-bar">
      <span>👁 <strong>${l.viewCount || 0}</strong> views</span>
      <span>📞 <strong>${l.contactRevealCount || 0}</strong> contacts</span>
      <span>📦 <strong>${fmt(l.quantity)}</strong> ${l.unit || 'units'} avail.</span>
    </div>

    <!-- Price + name -->
    <div class="det-body">
      <div class="det-pr">
        <span class="det-prm">₹${fmt(l.offeredPrice)}</span>
        ${l.unit ? `<span class="det-pru">/${l.unit}</span>` : ''}
        ${oldPr}
      </div>
      <div class="det-name">${l.listingTitle || ''}
        ${l.sellerVerified ? '<span class="vtick" title="Verified Seller">✓</span>' : ''}
      </div>
      <div class="det-tags">${typeTag}${negTag}</div>
      <div class="det-meta">
        <span>📍 ${l.address?.village || ''}, ${l.address?.district || ''}</span>
        ${l.distanceKm ? `<span>· ${l.distanceKm} km away</span>` : ''}
        <span>🕐 ${ago}</span>
      </div>
    </div>

    <!-- Attributes / Details -->
    ${attrHtml ? `<div class="det-section">
      <div class="det-sec-title">Details</div>
      <div class="attrs-grid">${attrHtml}</div>
    </div>` : ''}

    <!-- Seller card -->
    <div class="det-section">
      <div class="det-sec-title">Seller</div>
      <div class="seller-card" onclick="openSellerListings('${l.userId}')">
        <div class="seller-av">👤</div>
        <div class="seller-info">
          <div class="seller-nm">${l.sellerName || 'Seller'}</div>
          <div class="seller-pl ${planClass(l.sellerPlanKey)}">${l.sellerPlanKey || 'BASIC'}</div>
        </div>
        <span class="seller-go">›</span>
      </div>
    </div>

    <!-- Similar listings -->
    ${simHtml}
    <div style="height:80px"></div>`; /* space for sticky CTA */
}

function planClass(key) {
  return { ENTERPRISE: 'a', PREMIUM: 'g', STANDARD: 'gr' }[key] || 'b';
}

function relativeTime(isoStr) {
  if (!isoStr) return '';
  const diff = Date.now() - new Date(isoStr).getTime();
  const h = Math.floor(diff / 3600000);
  if (h < 1)  return 'Just now';
  if (h < 24) return h + 'h ago';
  const d = Math.floor(h / 24);
  return d === 1 ? '1 day ago' : d + ' days ago';
}
```

**Sections rendered inside `#detContent`:**

| # | Section | Data source |
|---|---------|-------------|
| 1 | Hero image / emoji | `l.images[0]` or module emoji fallback |
| 2 | Discount badge | `l.discountPct > 0` |
| 3 | Plan badge | `l.sellerPlanKey` |
| 4 | Stats bar | `l.viewCount`, `l.contactRevealCount`, `l.quantity` |
| 5 | Price block | `l.offeredPrice`, `l.actualPrice`, `l.discountPct`, `l.unit` |
| 6 | Title + verified tick | `l.listingTitle`, `l.sellerVerified` |
| 7 | SELL/RENT + Negotiable tags | `l.listingType`, `l.isNegotiable` |
| 8 | Location + time | `l.address`, `l.distanceKm`, `l.createdAt` |
| 9 | Attributes grid | `l.attributes` (Map<String, Object>) |
| 10 | Seller card | `l.userId`, `l.sellerPlanKey` → tap → `openSellerListings()` |
| 11 | Similar listings rail | `GET /api/v1/listings/{id}/similar` |

**Contact reveal:** Tapping "📞 View Contact" calls `revealContact(null, true)` — see [Section 15](#15-olx-style-contact-reveal-flow).

---

### 5.2 Search Screen

**API:** `GET /api/v1/search?q={query}&district=&moduleId=&categoryId=&listingType=&minPrice=&maxPrice=&postedWithin=&sellerType=&lat=&lng=&radiusKm=&sortBy=RELEVANCE&page=0&size=20`

**Sections:**
1. **Search input** (focused on open)
2. **Sort bar:** Relevance | Newest | Price ↑ | Price ↓ | Nearest
3. **Result count:** `"24 results for 'wheat'"` (use `totalElements` from pagination)
4. **Result grid** (2-column)

**Sort values mapping:**

| Button | `sortBy` param |
|--------|---------------|
| Relevance | `RELEVANCE` |
| Newest | `NEWEST` |
| Price ↑ | `PRICE_ASC` |
| Price ↓ | `PRICE_DESC` |
| Nearest | `DISTANCE` (requires lat/lng) |

---

### 5.3 Nearby Screen

**API:** `GET /api/v1/listings/nearby?lat={lat}&lng={lng}&radiusKm={r}&page=0&size=20` (requires JWT)

**Sections:**
1. **GPS permission prompt:** "Allow location access for distance-based results" + `[📍 Use My Location]` button
2. **Radius selector:** `5 km | 10 km | 25 km | 50 km | 100 km`
3. **Result grid** — cards show `distanceKm` as distance label

**GPS flow:**
```js
function requestGPS() {
  navigator.geolocation.getCurrentPosition(pos => {
    fetchNearby(pos.coords.latitude, pos.coords.longitude, activeRadius);
  }, err => showError('Location permission denied'));
}
```

---

### 5.4 Saved / Wishlist Screen

**Client-side only in v1.** Wishlist is stored in memory (or `localStorage`). The heart button on each card toggles the listing ID in/out of the wishlist set.

```js
let wishlist = new Set(JSON.parse(localStorage.getItem('tantra_wishlist') || '[]'));

function toggleWishlist(listingId, btn) {
  if (wishlist.has(listingId)) {
    wishlist.delete(listingId);
    btn.textContent = '🤍';
  } else {
    wishlist.add(listingId);
    btn.textContent = '❤️';
  }
  localStorage.setItem('tantra_wishlist', JSON.stringify([...wishlist]));
  document.getElementById('wlBadge').textContent = wishlist.size;
  document.getElementById('wlBadge').style.display = wishlist.size ? 'flex' : 'none';
}
```

**Wishlist screen:** Re-render listing cards for all IDs in the wishlist set. Show "No saved listings" empty state when empty.

---

### 5.5 Filter Panel

Slides in from the bottom on mobile, from the right on desktop. Has a dark overlay backdrop.

**Filter fields:**

| Field | Input | API Param |
|-------|-------|-----------|
| Listing Type | Radio (All/SELL/RENT) | `listingType` |
| Min Price | Number input | `minPrice` |
| Max Price | Number input | `maxPrice` |
| District | `<select>` | `district` |
| State | `<select>` | `state` |
| Radius | Button group | `radiusKm` |
| Seller Type | Radio (All/SUBSCRIBED) | `sellerType` |
| Posted Within | Radio (ALL/TODAY/WEEK/MONTH) | `postedWithin` |

**Apply → Reset:** "Apply Filters" re-fetches with params. "Reset all" clears all inputs and re-fetches.

**Active filter chips** bar (Section 4.3) is updated on apply.

---

## 6. Bottom Navigation Bar

Fixed at the bottom, 5 items with a floating Action Button in center.

```html
<nav class="bnav" role="navigation">
  <button class="bn-item on" onclick="goHome()">
    <span class="bni">🏠</span>Home
  </button>
  <button class="bn-item" onclick="showNearby()">
    <span class="bni">📍</span>Nearby
  </button>
  <div class="bnav-sw">
    <button class="bn-sell-btn" onclick="openPostListing()">+</button>  <!-- FAB -->
  </div>
  <button class="bn-item" onclick="showWishlist()">
    <span class="bni">♥</span>Saved
    <span class="bn-badge" id="wlBadge" style="display:none">0</span>
  </button>
  <button class="bn-item" onclick="openProfile()">
    <span class="bni">👤</span>Me
  </button>
</nav>
```

**CSS:** `position: fixed; bottom: 0; left: 0; right: 0; height: 56px; z-index: 150;`

**FAB (+ button):** Elevated green circle, `border-radius: 50%; width: 52px; height: 52px; background: var(--green);`

---

## 7. API Integration — Home Page Data Flow

```
Page Load
    │
    ├─► GET /api/v1/home?district={d}&lang={l}
    │       → promoCards          ──► Hero Carousel
    │       → modules             ──► Module tabs + Category grid
    │       → featuredListings    ──► Featured Rail (premium listings)
    │       → moduleSections      ──► Dynamic Module Rails (Agriculture + Animal & Livestock)
    │       → featuredProfiles    ──► Businesses Grid + Stories Strip
    │       → recentListings      ──► Recently Added Grid
    │
    ├─► GET /api/v1/stats/public
    │       → sellerCount         ──► Trust Bar "Farmers"
    │       → districtCount       ──► Trust Bar "Districts"
    │       → totalTradeValue     ──► Trust Bar "Traded"
    │       → satisfiedPct        ──► Trust Bar "Satisfied"
    │
    ├─► GET /api/v1/listings/flash-deals
    │       → listings[]          ──► Flash Deals Rail
    │
    ├─► GET /api/v1/msp/current
    │       → pricePerQuintal     ──► MSP Banner headline
    │       → full table          ──► MSP detail sheet on tap
    │
    └─► GET /api/v1/search/trending
            → query[]             ──► Trending Searches chips
```

**All four supplementary calls can run in parallel** (`Promise.all`) while the main home call loads.

---

## 8. Listing Card Component Spec

The listing card is the core reusable component used in rails, grids, search results, and nearby.

```html
<article class="lc {highlightClass}" onclick="openDetail(listingId)" role="listitem" tabindex="0">

  <!-- Image / Emoji area -->
  <div class="ci">
    <div class="ciph">{emoji or <img src=images[0]>}</div>
    {discountBadge}    <!-- "12% OFF" if discountPct > 0 -->
    {newBadge}         <!-- "NEW" if isNew=true and no discount -->
    {planBadge}        <!-- plan badge: ⭐ PREM / ✔ STD / 🏆 TOP -->
    <button class="hrt {saved?'on':''}" onclick="toggleWishlist()">🤍</button>
  </div>

  <!-- Card body -->
  <div class="cb">
    <div class="pr">
      <span class="prm">₹{offeredPrice}</span>
      <span class="pru">/{unit}</span>
      {actualPrice && <span class="pro">₹{actualPrice}</span>}  <!-- strikethrough -->
    </div>
    <div class="cn">{listingTitle} {sellerVerified && <span class="vtick">✓</span>}</div>
    <div class="cm">
      <span>📍 {address.district}</span>
      {distanceKm && <span>· {distanceKm} km</span>}
      <span>· {relativeTime(createdAt)}</span>
    </div>
    <div class="ctags">
      <span class="ct {listingType==='SELL'?'s':'r'}">{listingType}</span>
      {isNegotiable && <span class="ct n">Neg</span>}
    </div>
    {viewCount > 100 && <span class="social-proof"><span class="live-dot"></span>{viewCount} viewing</span>}
  </div>

  <!-- Actions -->
  <div class="cacts">
    <button onclick="revealContact(listingId)">📞 Contact</button>
    <button>💬</button>
  </div>

</article>
```

### Listing Card API Fields → UI Mapping

| API Field | UI Element |
|-----------|-----------|
| `listingId` | `onclick` handler arg |
| `listingType` | SELL (green) / RENT (amber) badge |
| `offeredPrice` | main price in INR |
| `actualPrice` | strikethrough price (if set) |
| `discountPct` | "12% OFF" badge |
| `unit` | price unit label |
| `isNegotiable` | "Neg" badge |
| `address.district` | location line |
| `distanceKm` | distance label (nearby/search only) |
| `createdAt` | relative time "2 days ago" |
| `isNew` | "NEW" badge (if `isNew=true` and no discount) |
| `viewCount` | social proof "248 viewing" (show if > 100) |
| `images[0]` | card image (fallback: module emoji) |
| `isHighlighted` | card highlight border |
| `highlightColor` | border color (e.g. `#FFD700`) |
| `sellerBadge.en` | plan badge text |
| `sellerPlanKey` | determines badge style (see Section 10) |
| `sellerVerified` | blue `✓` tick after listing title |
| `flashDeal` | use in flash deals section |

### Highlight Border CSS

```css
.lc { border: 2px solid transparent; }
/* Applied dynamically from highlightColor: */
/* el.style.borderColor = card.highlightColor; */

/* Plan-based CSS classes (alternative approach): */
.lc.gb   { border-color: #C8A000; }  /* PREMIUM = gold border */
.lc.amb  { border-color: #D4920A; }  /* ENTERPRISE = amber border */
.lc.greenb { border-color: #1E5631; } /* STANDARD = green border */
```

---

## 9. Business Card Component Spec

```html
<article class="bcd {planBorderClass}" role="listitem" onclick="openSellerProfile(profileId)">
  <div class="bav">
    <span>{emoji}</span>
    {planBadgeDot}  <!-- small dot: 🏆 or ⭐ or ✔ -->
  </div>
  <div class="bnm">{businessName}</div>
  <div class="blc">📍 {city}</div>
  <div class="bpl {planColorClass}">{planKey}</div>
</article>
```

| `planKey` | Border class | Text color class |
|-----------|-------------|-----------------|
| ENTERPRISE | `amb` | `a` (amber) |
| PREMIUM | `gb` | `g` (gold) |
| STANDARD | *(none)* | `gr` (green) |
| BASIC | *(none)* | `b` (grey) |

---

## 10. Premium Badge & Highlight System

Server sorts listings with premium subscribers first. Frontend only needs to render the badge.

```js
function planBadge(planKey) {
  switch (planKey) {
    case 'ENTERPRISE': return '<span class="plan-b a">🏆 TOP</span>';
    case 'PREMIUM':    return '<span class="plan-b g">⭐ PREM</span>';
    case 'STANDARD':   return '<span class="plan-b gr">✔ STD</span>';
    default:           return '';
  }
}
```

**Card border:** If `isHighlighted = true`, apply `highlightColor` as `border-color` on the card `<article>`.

**Verified tick:** If `sellerVerified = true`, append `<span class="vtick" title="Verified Seller">✓</span>` after the listing title.

---

## 11. Authentication Flow

**Token storage:** `localStorage.getItem('tantra_token')`

**All public endpoints** work without a token (home feed, search, MSP, stats, flash deals, listings browse).

**Guarded endpoints** (contact reveal, nearby, posting a listing, wishlist sync) require JWT in the header:
```
Authorization: Bearer {token}
```

**Login flow:**
1. `POST /api/v1/auth/signin` with `{ mobile, password }` or `{ email, password }`
2. Response: `{ token, user: { userId, name, mobile, role } }`
3. Store token: `localStorage.setItem('tantra_token', data.token)`
4. Update header UI to show user name + profile pic

**Session check on load:**
```js
async function checkSession() {
  const token = localStorage.getItem('tantra_token');
  if (!token) return;
  const res = await fetch('/api/v1/auth/verify-session', {
    headers: { Authorization: 'Bearer ' + token }
  });
  if (!res.ok) localStorage.removeItem('tantra_token');
}
```

---

## 12. Bilingual (EN / HI) Support

Every user-visible label has both English and Hindi versions.

### HTML approach
```html
<button data-en="Browse Crops →" data-hi="फसल देखें →">Browse Crops →</button>
```

### JS toggle function
```js
let lang = localStorage.getItem('tantra_lang') || 'en';

function toggleLang() {
  lang = lang === 'en' ? 'hi' : 'en';
  localStorage.setItem('tantra_lang', lang);
  document.querySelectorAll('[data-en]').forEach(el => {
    el.textContent = el.dataset[lang];
  });
  document.getElementById('langBtn').textContent = lang === 'en' ? 'हिन्दी' : 'English';
}
```

### API bilingual strings

The API returns bilingual objects for titles and badges:
```json
{
  "featuredListings": {
    "title": { "en": "Featured", "hi": "विशेष" },
    "listings": [...]
  },
  "sellerBadge": { "en": "Premium Seller", "hi": "प्रीमियम विक्रेता" }
}
```

Always read `obj[lang]` where `lang` is `'en'` or `'hi'`.

---

## 13. Filter & Sort Reference

### Search / Browse params

| Param | Values | Notes |
|-------|--------|-------|
| `q` | string | search query |
| `district` | string | e.g. `Pune` |
| `state` | string | e.g. `Maharashtra` |
| `moduleId` | integer | from masters API |
| `categoryId` | integer | from masters API |
| `listingType` | `SELL` / `RENT` | omit for all |
| `minPrice` | number | INR |
| `maxPrice` | number | INR |
| `postedWithin` | `TODAY` / `WEEK` / `MONTH` / `ALL` | default ALL |
| `sellerType` | `ALL` / `SUBSCRIBED` | SUBSCRIBED = premium only |
| `lat` | float | for distance sort |
| `lng` | float | for distance sort |
| `radiusKm` | int | 5/10/25/50/100 |
| `sortBy` | `RELEVANCE` / `NEWEST` / `PRICE_ASC` / `PRICE_DESC` / `DISTANCE` | |
| `page` | int | 0-based |
| `size` | int | default 20 |

### Radius presets

| Label | `radiusKm` |
|-------|-----------|
| 5 km | 5 |
| 10 km | 10 |
| 25 km | 25 (default) |
| 50 km | 50 |
| 100 km | 100 |
| All India | *(omit param)* |

---

## 14. Color Palette & Design Tokens

### Brand Colors

| Token | Hex | Usage |
|-------|-----|-------|
| `--green` | `#1E5631` | Primary CTA, active states, STANDARD badge |
| `--gold` | `#C47A0A` | Accent, PREMIUM card border |
| `--amber` | `#D4920A` | ENTERPRISE card border |
| `--red-disc` | `#B00020` | Discount prices, mandi price drop |
| `--sell-tag` | `#1B5E20` | SELL badge text (on `#E8F5E9` bg) |
| `--rent-tag` | `#E65100` | RENT badge text (on `#FFF8E1` bg) |

### Plan Colors

| Plan | Border hex | Badge text | Badge bg |
|------|-----------|-----------|---------|
| ENTERPRISE | `#D4920A` | `🏆 TOP` | `#FFF3E0` |
| PREMIUM | `#C8A000` | `⭐ PREM` | `#FFFDE7` |
| STANDARD | `#1E5631` | `✔ STD` | `#E8F5E9` |

### Type Scale

| Element | Size |
|---------|------|
| Page heading | `1.1rem / 700` |
| Card price | `1rem / 700` |
| Card title | `0.8rem / 500` |
| Card meta | `0.72rem / 400` |
| Badge text | `0.6rem / 600` |

---

## 15. OLX-Style Contact Reveal Flow

Contact numbers are hidden behind a reveal gate to prevent scraping. This is the final step in the core user journey.

---

### Step-by-step UI flow

```
User taps "📞 Contact" / "📞 View Contact"
         │
         ▼
Is user logged in?
   ├── NO  → show Login Modal → after login, retry revealContact()
   └── YES → POST /api/v1/listings/{listingId}/contact
                │
                ├── 200 OK → show Contact Modal with phone + call/WhatsApp buttons
                ├── 403 CONTACT_HIDDEN → show "Seller hasn't enabled contact"
                └── 401 UNAUTHORIZED → clear token, redirect to login
```

---

### API

```
POST /api/v1/listings/{listingId}/contact
Authorization: Bearer {token}
```

**Response 200:**
```json
{
  "success": true,
  "data": {
    "phone": "+91-9876543210",
    "whatsappUrl": "https://wa.me/919876543210",
    "alreadyRevealed": false
  }
}
```

- `alreadyRevealed: true` → user already revealed this contact in the past 24h (no extra quota deducted, same phone returned)
- `alreadyRevealed: false` → first reveal, quota decremented on seller's plan

---

### `revealContact()` JS Function

```js
// fromDetail = true when called from the detail CTA bar
// fromDetail = false when called from the card's inline "📞 Contact" button
async function revealContact(listingId, fromDetail) {
  const id = fromDetail ? currentDetailId : listingId;
  if (!id) return;

  const token = localStorage.getItem('tantra_token');

  // Step 1: auth check
  if (!token) {
    showLoginModal(() => revealContact(id, fromDetail));  // retry after login
    return;
  }

  // Step 2: show loading state on button
  const btn = fromDetail
    ? document.getElementById('detContactBtn')
    : document.querySelector(`[data-contact-btn="${id}"]`);
  if (btn) { btn.disabled = true; btn.textContent = 'Loading…'; }

  try {
    const res = await fetch(`/api/v1/listings/${id}/contact`, {
      method: 'POST',
      headers: { Authorization: 'Bearer ' + token }
    });
    const json = await res.json();

    if (!res.ok) {
      const code = json.error?.code;
      if (code === 'CONTACT_HIDDEN') {
        showToast("Seller hasn't enabled contact sharing.");
      } else if (res.status === 401) {
        localStorage.removeItem('tantra_token');
        showLoginModal(() => revealContact(id, fromDetail));
      } else {
        showToast(json.error?.message?.en || 'Something went wrong.');
      }
      return;
    }

    // Step 3: show contact modal
    showContactModal(json.data);

  } finally {
    if (btn) { btn.disabled = false; btn.textContent = '📞 View Contact'; }
  }
}
```

---

### Contact Modal HTML

Place this once in the body. It is shown/hidden by JS.

```html
<!-- Contact Modal Overlay -->
<div class="cm-ov" id="cmOv" onclick="closeContactModal()"></div>

<!-- Contact Modal -->
<div class="cm" id="contactModal" role="dialog" aria-modal="true">
  <button class="cm-close" onclick="closeContactModal()">✕</button>
  <div class="cm-icon">📞</div>
  <div class="cm-lbl">Seller Contact</div>
  <div class="cm-phone" id="cmPhone">+91-9876543210</div>
  <div class="cm-actions">
    <a class="btn-call" id="cmCall" href="#">📞 Call Now</a>
    <a class="btn-wa"   id="cmWa"   href="#" target="_blank" rel="noopener">💬 WhatsApp</a>
  </div>
  <div class="cm-note" id="cmNote"></div>
</div>
```

**Show/hide JS:**
```js
function showContactModal(data) {
  document.getElementById('cmPhone').textContent = data.phone;
  document.getElementById('cmCall').href = 'tel:' + data.phone.replace(/\s/g, '');
  document.getElementById('cmWa').href   = data.whatsappUrl;
  document.getElementById('cmNote').textContent = data.alreadyRevealed
    ? 'You already viewed this contact.' : '';
  document.getElementById('cmOv').style.display      = 'block';
  document.getElementById('contactModal').style.display = 'block';
}

function closeContactModal() {
  document.getElementById('cmOv').style.display      = 'none';
  document.getElementById('contactModal').style.display = 'none';
}
```

**Contact Modal CSS:**
```css
.cm-ov {
  display: none; position: fixed; inset: 0;
  background: rgba(0,0,0,.5); z-index: 400;
}
.cm {
  display: none; position: fixed;
  bottom: 0; left: 0; right: 0;
  background: var(--surface);
  border-radius: 18px 18px 0 0;
  padding: 1.5rem 1.25rem 2rem;
  z-index: 401; text-align: center;
}
.cm-phone  { font-size: 1.5rem; font-weight: 700; margin: 0.75rem 0; letter-spacing: 1px; }
.cm-actions { display: flex; gap: 0.75rem; margin-top: 1rem; }
.btn-call  { flex:1; background: var(--green); color:#fff; padding:0.85rem; border-radius:10px; font-weight:700; text-decoration:none; }
.btn-wa    { flex:1; background: #25D366;      color:#fff; padding:0.85rem; border-radius:10px; font-weight:700; text-decoration:none; }
.cm-note   { margin-top:0.75rem; font-size:0.75rem; opacity:0.6; }
```

---

### Login Prompt Modal (shown when not logged in)

```js
function showLoginModal(onSuccessCallback) {
  // Store callback for after login
  window._postLoginCallback = onSuccessCallback;
  document.getElementById('loginModal').style.display = 'block';
}

// After successful POST /api/v1/auth/signin:
function onLoginSuccess(token, user) {
  localStorage.setItem('tantra_token', token);
  document.getElementById('loginModal').style.display = 'none';
  if (window._postLoginCallback) {
    window._postLoginCallback();
    window._postLoginCallback = null;
  }
}
```

---

## 16. API Endpoint Cheat Sheet (Home Page)

All APIs below are **Public** (no auth needed) unless marked **[JWT]**.

| # | Method | Endpoint | Auth | Used In |
|---|--------|----------|------|---------|
| 1 | GET | `/api/v1/home?district=&lang=EN` | Public | Main home feed (featured, module rails, businesses, recent) |
| 2 | GET | `/api/v1/stats/public` | Public | Trust Stats Bar |
| 3 | GET | `/api/v1/listings/flash-deals` | Public | Flash Deals Rail |
| 4 | GET | `/api/v1/msp/current` | Public | MSP Banner |
| 5 | GET | `/api/v1/search/trending` | Public | Trending Chips |
| 6 | GET | `/api/v1/masters/modules` | Public | Module Tabs + Category Grid |
| 7 | GET | `/api/v1/masters/modules/{moduleId}/categories` | Public | Top-level category chips per module |
| 8 | GET | `/api/v1/masters/categories/{parentId}/subcategories` | Public | Sub-category chips on chip tap |
| 9 | GET | `/api/v1/business-profiles/top-sellers` | Public | Stories Strip |
| 10 | GET | `/api/v1/business-profiles/directory?profileType=vet_clinic` | Public | Veterinary category tap |
| 11 | GET | `/api/v1/search?q=&...filters` | Public | Search Screen |
| 12 | GET | `/api/v1/listings/category/{categoryId}` | Public | Category / sub-category chip tap → listing cards |
| 13 | GET | `/api/v1/listings/{id}` | Public | Detail Screen |
| 14 | GET | `/api/v1/listings/{id}/similar` | Public | Detail → Similar Rail |
| 15 | POST | `/api/v1/listings/{id}/contact` | **[JWT]** | Contact Reveal modal |
| 16 | GET | `/api/v1/listings/nearby?lat=&lng=&radiusKm=` | **[JWT]** | Nearby Screen |
| 17 | GET | `/api/v1/notifications/unread-count` | **[JWT]** | Notification bell dot |
| 18 | POST | `/api/v1/auth/signin` | Public | Login |
| 19 | POST | `/api/v1/auth/signup` | Public | Register |

---

## 17. Error Handling

All API responses follow this envelope:

**Success:**
```json
{
  "success": true,
  "data": { ... },
  "message": { "en": "...", "hi": "..." },
  "traceId": "abc-123",
  "timestamp": "2026-08-01T12:00:00"
}
```

**Error:**
```json
{
  "success": false,
  "error": {
    "code": "NOT_FOUND",
    "message": { "en": "Listing not found", "hi": "लिस्टिंग नहीं मिली" }
  },
  "traceId": "abc-123"
}
```

**Common error codes:**

| HTTP | Code | Show to user |
|------|------|-------------|
| 400 | `VALIDATION_ERROR` | Form validation message |
| 400 | `CONTACT_HIDDEN` | "Seller hasn't enabled contact" |
| 400 | `LISTING_INACTIVE` | "This listing is no longer active" |
| 401 | `UNAUTHORIZED` | Redirect to login |
| 403 | `FORBIDDEN` | "You don't have permission" |
| 404 | `NOT_FOUND` | Show 404 state in overlay |
| 409 | `CONFLICT` | "Already submitted" |
| 500 | `INTERNAL_ERROR` | "Something went wrong. Please try again." |

---

## 18. Pagination

All list endpoints return Spring paginated responses:

```json
{
  "success": true,
  "data": {
    "content": [ ...listings ],
    "totalElements": 124,
    "totalPages": 7,
    "number": 0,
    "size": 20,
    "first": true,
    "last": false
  }
}
```

**Infinite scroll implementation:**
```js
let currentPage = 0;
let isLoading = false;

window.addEventListener('scroll', () => {
  if (isLoading) return;
  if (window.innerHeight + window.scrollY >= document.body.offsetHeight - 300) {
    currentPage++;
    loadMoreListings(currentPage);
  }
});

async function loadMoreListings(page) {
  isLoading = true;
  const res = await fetch(`/api/v1/search?q=${query}&page=${page}&size=20`, {
    headers: { Authorization: 'Bearer ' + token }
  });
  const json = await res.json();
  appendCards(json.data.content);
  isLoading = json.data.last;  // stop loading if on last page
}
```

---

## 19. Complete User Flow — Start to End

This section traces the **full journey** a user takes from opening the app to getting a seller's contact number.

---

### Flow Diagram

```
App opens
    │
    ▼
[Page Load] — 4 parallel API calls:
    ├── GET /api/v1/home              → Featured rail + Module rails + Businesses + Recent
    ├── GET /api/v1/stats/public      → Trust stats bar numbers
    ├── GET /api/v1/listings/flash-deals → Flash deals rail
    ├── GET /api/v1/msp/current       → MSP banner headline price
    └── GET /api/v1/search/trending   → Trending search chips
    │
    ▼
Home Page renders all sections
    │
    ├──[A] User taps a MODULE TAB
    │       → page scrolls to that module's rail section (no API call)
    │
    ├──[B] User taps a TOP-LEVEL CATEGORY CHIP (e.g. "Agriculture Marketplace")
    │       → GET /api/v1/masters/categories/{categoryId}/subcategories
    │       → sub-chips row appears below (Crop | Seed | Pesticide | Fertilizer | Equipment)
    │       → GET /api/v1/listings/category/{categoryId}?page=0&size=10
    │       → rail refreshes with listings for that category
    │
    ├──[C] User taps a SUB-CATEGORY CHIP (e.g. "Crop")
    │       → GET /api/v1/listings/category/{subCategoryId}?page=0&size=10
    │       → rail refreshes with listings for that specific sub-category
    │       → (sub-category has no children — no further chip row)
    │
    └──[D] User taps LISTING CARD
                │
                ▼
        openDetail(listingId) called
                │
                ├── showScreen('detail')  → detail overlay slides in
                ├── GET /api/v1/listings/{listingId}            (listing data)
                ├── GET /api/v1/listings/{listingId}/similar    (similar rail)
                │
                ▼
        Detail screen renders:
            Hero | Stats | Price | Title | Attributes | Seller card | Similar rail
            Sticky CTA bar: [📞 View Contact] [💬]
                │
                ▼
        User taps "📞 View Contact"
                │
                ├── Not logged in?
                │       → showLoginModal()
                │       → POST /api/v1/auth/signin  { mobile, password }
                │       → store token → retry revealContact()
                │
                └── Logged in?
                        → POST /api/v1/listings/{listingId}/contact
                            Authorization: Bearer {token}
                                │
                                ├── 200 OK → showContactModal()
                                │           phone number + [📞 Call] [💬 WhatsApp]
                                │
                                └── 403 CONTACT_HIDDEN → toast "Seller hasn't enabled contact"
```

---

### Step 1 — Page Load (JS)

```js
async function initHomePage() {
  // Render module tabs first (fast)
  renderModuleTabs();

  // Load all home sections in parallel
  const [homeRes, statsRes, flashRes, mspRes, trendRes] = await Promise.all([
    fetch('/api/v1/home?district=Pune&lang=EN').then(r => r.json()),
    fetch('/api/v1/stats/public').then(r => r.json()),
    fetch('/api/v1/listings/flash-deals').then(r => r.json()),
    fetch('/api/v1/msp/current').then(r => r.json()),
    fetch('/api/v1/search/trending').then(r => r.json()),
  ]);

  // Render each section from its API response
  renderPromoCarousel(homeRes.data.promoCards);
  renderTrustStats(statsRes.data);
  renderCategoryGrid();                                    // calls masters/modules internally
  renderTrendingChips(trendRes.data);
  renderFlashDeals(flashRes.data);
  renderMspBanner(mspRes.data);
  renderTopSellers(homeRes.data.featuredProfiles);
  renderFeaturedRail(homeRes.data.featuredListings?.listings);
  renderModuleSections(homeRes.data.moduleSections);      // ALL module rails in one loop
  renderBusinessGrid(homeRes.data.featuredProfiles);
  renderRecentGrid(homeRes.data.recentListings);
}

document.addEventListener('DOMContentLoaded', initHomePage);
```

---

### Step 2 — Category Chip Tap

```js
// Called from chip onclick (section 4.14)
async function onChipTap(chipEl, moduleId) { /* ... defined in section 4.14 ... */ }

// Called from sub-chip onclick (section 4.14)
async function onSubChipTap(chipEl, moduleId) { /* ... defined in section 4.14 ... */ }
```

Both functions update the listing rail below the chips with fresh cards and show skeleton loading while waiting.

---

### Step 3 — Listing Card Click → Detail Screen

Every listing card rendered by `renderCard(l)` has:
```html
<article onclick="openDetail('${l.listingId}')">...</article>
```

`openDetail(listingId)` (defined in section 5.1):
1. Shows detail overlay immediately with skeleton
2. Fetches listing + similar in parallel
3. Renders all detail sections
4. Stores `currentDetailId` for the contact button

---

### Step 4 — Contact Reveal

The "📞 View Contact" button in the detail CTA calls `revealContact(null, true)`.

The inline "📞 Contact" button on cards calls `revealContact(listingId, false)`.

`revealContact()` (defined in section 15):
1. Checks `localStorage` for JWT token
2. If missing → `showLoginModal(callback)` → login → retry
3. If present → `POST /api/v1/listings/{id}/contact`
4. On success → `showContactModal(data)` → phone + call + WhatsApp

---

### Complete Screen Stack Example

```
screenStack = []                 → Home
openDetail('L001')               → screenStack = ['home', 'detail']
goBack()                         → screenStack = ['home'],   detail closes
openSearch()                     → screenStack = ['home', 'search']
openDetail('L005') from search   → screenStack = ['home', 'search', 'detail']
goBack()                         → screenStack = ['home', 'search'], detail closes
goBack()                         → screenStack = ['home'],            search closes
```

---

### Auth Helper Functions

```js
// Returns auth header object, or empty object if not logged in
function authHeader() {
  const token = localStorage.getItem('tantra_token');
  return token ? { Authorization: 'Bearer ' + token } : {};
}

function isLoggedIn() {
  return !!localStorage.getItem('tantra_token');
}

// Simple toast notification
function showToast(message, duration = 3000) {
  const t = document.createElement('div');
  t.className = 'toast';
  t.textContent = message;
  document.body.appendChild(t);
  setTimeout(() => t.remove(), duration);
}
```

**Toast CSS:**
```css
.toast {
  position: fixed; bottom: 80px; left: 50%; transform: translateX(-50%);
  background: #1A1A14; color: #fff; padding: 0.6rem 1.2rem;
  border-radius: 20px; font-size: 0.85rem; z-index: 500;
  animation: fadeInOut 3s ease forwards;
}
@keyframes fadeInOut {
  0%,100% { opacity: 0; } 10%,85% { opacity: 1; }
}
```

---

## Summary: Build Order Recommendation

Build the components in this order for fastest visible progress:

**Core flow first (ship-blocking):**
1. `renderCard()` — Listing Card component (used everywhere)
2. `initHomePage()` — all 5 parallel API calls, wire up home sections
3. `renderModuleSections()` — dynamic module rails (Agriculture + Animal & Livestock)
4. `onChipTap()` + `onSubChipTap()` — category → sub-category → listing cards
5. `openDetail()` — detail screen with full listing data + similar rail
6. `revealContact()` — contact modal + login gate

**Supporting sections:**
7. Sticky Header + search input (→ search overlay)
8. Module Tabs from masters API
9. Promo Cards carousel
10. Trust Stats Bar (animated counters)
11. Flash Deals + countdown timer
12. MSP Banner
13. Trending Search chips
14. Top Seller Stories strip
15. Category Emoji Grid (dynamic from DB)
16. Filter Panel + active chip bar

**Polish:**
17. Search Screen overlay with sort + pagination
18. Nearby Screen (GPS)
19. Wishlist Screen + localStorage
20. Dark mode + bilingual toggle
21. Toast notifications + skeleton loaders everywhere
