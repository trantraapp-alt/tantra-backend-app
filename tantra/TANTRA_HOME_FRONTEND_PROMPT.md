# Tantra Home Page — Frontend Build Prompt

> Give this entire prompt to your AI coding assistant (Claude, Cursor, Copilot, etc.) to build the complete Tantra home page from scratch.  
> Reference files: `tantra_home.html` (working prototype), `TANTRA_HOME_FRONTEND_GUIDE.md` (full spec), `FRONTEND_API.md` (API contract).  
> Backend runs at: `http://localhost:8080/api/v1`

---

## Your Task

Build the complete **Tantra Agri-Marketplace Home Page** — a single HTML/CSS/JS file (or React/Vue component tree, your choice) that:

1. Loads live data from the backend APIs listed below
2. Matches the layout and behavior described in this prompt exactly
3. Handles the full user flow: **Home → Category → Sub-category → Listing Cards → Item Detail → Contact Reveal**
4. Works in both light and dark mode
5. Supports bilingual toggle (English / Hindi)

---

## Tech Stack

- **Preferred:** Vanilla HTML + CSS + JS (no framework needed — the prototype is pure HTML/CSS/JS)
- **Alternative:** React or Vue — your choice, as long as the behavior is identical
- **No external CSS libraries.** Use CSS custom properties (design tokens provided below)
- **No external JS libraries.** Use native `fetch`, `IntersectionObserver`, `SpeechRecognition`, `localStorage`

---

## Design Tokens

```css
:root {
  --green:   #1E5631;    /* primary CTA, active states */
  --gold:    #C47A0A;    /* accent, PREMIUM badge */
  --amber:   #D4920A;    /* ENTERPRISE badge */
  --bg:      #F5F5F0;    /* page background */
  --surface: #FFFFFF;    /* card background */
  --border:  #E0DDD5;    /* dividers */
  --txt:     #1A1A14;    /* primary text */
  --txt2:    #5A5A50;    /* secondary text */
  --txt3:    #8A8A80;    /* muted text */
  --radius:  10px;
  --shadow:  0 1px 6px rgba(0,0,0,.08);
  --font:    'Segoe UI', system-ui, sans-serif;
}
@media (prefers-color-scheme: dark) {
  :root {
    --bg: #141410; --surface: #1E1E18; --border: #2A2A22;
    --txt: #F0EDE6; --txt2: #B0AD9A; --txt3: #706D5E;
  }
}
```

---

## Page Layout (top → bottom)

```
┌─────────────────────────────────────────────────────────┐
│  STICKY HEADER                                          │
│  Logo | Search bar + 🎤 voice | Filter btn             │
│  🔔 Notif | 🌙 Theme | हिन्दी Lang | Login | +Sell      │
│  ─────────────────────────────────────────────────────  │
│  📍 [Pune ▾]  [25 km ▾]  ☀️ 31°C · Good day to sell   │
├─────────────────────────────────────────────────────────┤
│  ACTIVE FILTER CHIPS BAR (hidden when no filters)       │
├─────────────────────────────────────────────────────────┤
│  MODULE TABS  (dynamic from API)                        │
│  🌾 Agriculture  |  🐄 Animal & Livestock               │
├─────────────────────────────────────────────────────────┤
│  HERO CAROUSEL  (auto-play, swipe, dots)                │
│  Driven by admin Promo Cards from API                   │
├─────────────────────────────────────────────────────────┤
│  TRUST STATS BAR                                        │
│  12,400+ Farmers | 48 Districts | ₹4.2Cr+ | 98% Sat   │
├─────────────────────────────────────────────────────────┤
│  DUAL MINI BANNERS  [🌱 Kharif Seeds]  [🚜 Tractor]    │
├─────────────────────────────────────────────────────────┤
│  ┌── MAIN CONTENT AREA ──────────────────────────────┐  │
│  │  Category Emoji Grid  (4-per-row, dynamic)        │  │
│  │  🔥 Trending Searches  (chips, horizontal scroll) │  │
│  │  ⚡ Flash Deals + ⏱ Countdown  (horizontal rail)  │  │
│  │  🏛️ MSP Price Banner  (tappable)                  │  │
│  │  🇮🇳 PM-KISAN Scheme Banner                       │  │
│  │  🌟 Top Seller Stories  (circular avatars strip)   │  │
│  │  ⭐ Featured Listings Rail  (skeleton → cards)    │  │
│  │  ─── DYNAMIC MODULE RAILS ──────────────────────  │  │
│  │  🌾 Agriculture                                   │  │
│  │     [Agri Marketplace][Agri Services][Repair]     │  │
│  │     ↳ sub-chips: Crop|Seed|Pesticide|Fert|Equip  │  │
│  │     listing cards rail                            │  │
│  │  🐄 Animal & Livestock                            │  │
│  │     [Veterinary*][Animal Marketplace]             │  │
│  │     ↳ sub-chips: Poultry|Fishery|Animal           │  │
│  │     listing cards rail  (*Vet → business dir)    │  │
│  │  🏪 Businesses Grid                               │  │
│  │  🕐 Recently Added Grid                           │  │
│  │  Footer                                           │  │
│  └───────────────────────────────────────────────────┘  │
├─────────────────────────────────────────────────────────┤
│  BOTTOM NAV: 🏠 Home | 📍 Nearby | [+] FAB | ♥ Saved | 👤 Me │
└─────────────────────────────────────────────────────────┘
```

---

## API Calls — Page Load

On `DOMContentLoaded`, fire **all 5 calls in parallel** (`Promise.all`):

```js
const [homeRes, statsRes, flashRes, mspRes, trendRes] = await Promise.all([
  fetch('/api/v1/home?district=Pune&lang=EN').then(r => r.json()),
  fetch('/api/v1/stats/public').then(r => r.json()),
  fetch('/api/v1/listings/flash-deals').then(r => r.json()),
  fetch('/api/v1/msp/current').then(r => r.json()),
  fetch('/api/v1/search/trending').then(r => r.json()),
]);
```

Then render:
- `homeRes.data.promoCards[]` → Hero Carousel
- `homeRes.data.featuredListings.listings[]` → Featured Rail
- `homeRes.data.moduleSections[]` → All Module Rails (Agriculture + Animal & Livestock)
- `homeRes.data.featuredProfiles[]` → Businesses Grid + Stories Strip
- `homeRes.data.recentListings[]` → Recently Added Grid
- `statsRes.data` → Trust Stats Bar (animate counters)
- `flashRes.data` → Flash Deals Rail
- `mspRes.data` → MSP Banner headline (use wheat price)
- `trendRes.data` → Trending Search chips

---

## Section 1 — Sticky Header

```html
<header style="position:sticky;top:0;z-index:100;background:var(--surface)">
  <div class="hdr-top">
    <div class="logo">TAN<em>TRA</em></div>

    <!-- Search -->
    <div class="srch-w">
      <input id="mainSrch" type="search" placeholder="Search crops, seeds, equipment…"
             onfocus="openSearch()">
      <button onclick="startVoiceSearch()">🎤</button>
    </div>

    <button onclick="openFilter()">Filter</button>

    <div class="hdr-acts">
      <button id="notifBtn">🔔 <span class="notif-dot"></span></button>
      <button onclick="toggleTheme()">🌙</button>
      <button id="langBtn" onclick="toggleLang()">हिन्दी</button>
      <button onclick="openLogin()">Login</button>
      <button onclick="openPostListing()">+ Sell</button>
    </div>
  </div>

  <!-- Location bar -->
  <div class="loc-bar">
    <button onclick="openDistrictPicker()">Pune ▾</button>
    <button onclick="openRadiusPicker()">25 km ▾</button>
    <span>☀️ 31°C · Good day to sell</span>
    <span>Maharashtra, India</span>
  </div>
</header>
```

**Behaviors:**
- Search input `onfocus` → open Search overlay (slide in from right)
- `🎤` voice button → `window.SpeechRecognition`, fill input with result, trigger search
- `🔔` badge dot → call `GET /api/v1/notifications/unread-count` [JWT], show red dot if > 0
- District pill → repick district → re-call `GET /api/v1/home?district={new}`
- Radius pill → show options: 5km / 10km / 25km / 50km / 100km
- Login button → open login modal (overlay)
- `+ Sell` → requires JWT, else open login modal first

---

## Section 2 — Active Filter Chips Bar

```html
<div id="afcBar" style="display:none">
  <div id="afcInner">
    <!-- Chips injected by JS after filters are applied -->
    <!-- e.g.: <span class="afc-chip">SELL <button onclick="removeFilter('lt')">✕</button></span> -->
  </div>
</div>
```

- Hidden by default; shown only when 1+ filters are active
- Each chip has a `✕` that removes that filter and refreshes listings

---

## Section 3 — Module Tabs (Dynamic)

```js
// Client-side emoji map — AppModule has NO emoji in DB
const MODULE_EMOJI = {
  agriculture:      '🌾',
  animal_livestock: '🐄',
};

async function renderModuleTabs() {
  const { data: modules } = await fetch('/api/v1/masters/modules').then(r => r.json());
  const tabsEl = document.getElementById('modTabs');
  tabsEl.innerHTML = modules
    .filter(m => m.isActive)
    .map((m, i) => {
      const emoji = MODULE_EMOJI[m.moduleKey] || '📦';
      const label = lang === 'en' ? m.moduleNameEn : m.moduleNameHi;
      return `<button class="mt ${i===0?'on':''}" data-module-id="${m.id}"
                onclick="scrollToModule(${m.id})">${emoji} ${label}</button>`;
    }).join('');
}

function scrollToModule(moduleId) {
  document.getElementById('module-' + moduleId)?.scrollIntoView({ behavior: 'smooth' });
  // update active tab indicator
}
```

```html
<nav class="mtabs-sec">
  <div class="tab-indicator" id="tabIndicator"></div>
  <div class="mtabs" id="modTabs"></div>
</nav>
```

---

## Section 4 — Hero Carousel (Promo Cards)

- Render one slide per `promoCard` from `homeRes.data.promoCards[]`
- Auto-play every 4 seconds; pause on hover/touch; resume on leave
- Touch swipe: if `|touchEnd - touchStart| > 50px` → advance slide
- Dots below carousel: one dot per slide, click to jump

**PromoCard fields to use:**

| Field | Use |
|-------|-----|
| `titleEn` / `titleHi` | Slide headline |
| `subtitleEn` / `subtitleHi` | Slide subtext |
| `ctaLabelEn` / `ctaLabelHi` | Button text |
| `ctaType` | `MODULE` / `CATEGORY` / `EXTERNAL` / `NONE` |
| `ctaTarget` | Navigate to moduleId / categoryId / URL |
| `imageUrl` | Slide image (use SVG gradient if null) |
| `bgColorFrom` / `bgColorTo` | Gradient background colors |
| `textColor` | Slide text color |

```html
<section class="car-sec">
  <div class="c-track" id="cTrack"></div>
  <button class="c-arr p" onclick="moveSlide(-1)">←</button>
  <button class="c-arr n" onclick="moveSlide(1)">→</button>
  <div class="c-dots" id="cDots"></div>
</section>
```

---

## Section 5 — Trust Stats Bar

Call `GET /api/v1/stats/public`. Animate counters upward when scrolled into view (IntersectionObserver).

| API Field | Label | Format |
|-----------|-------|--------|
| `sellerCount` | Farmers | `12,400+` |
| `districtCount` | Districts | `48` |
| `totalTradeValue` | Traded | `₹4.2Cr+` (divide by 10,00,000) |
| `satisfiedPct` | Satisfied | `98%` |

```html
<div class="trust-bar">
  <div class="trust-item"><span class="trust-num" id="tFarmers">0</span><span>Farmers</span></div>
  <div class="trust-item"><span class="trust-num" id="tDistricts">0</span><span>Districts</span></div>
  <div class="trust-item"><span id="tTraded">₹0</span><span>Traded</span></div>
  <div class="trust-item"><span class="trust-num" id="tSatisfied">0</span><span>Satisfied</span></div>
</div>
```

---

## Section 6 — Dual Mini Banners

Two side-by-side banners below the trust bar. Use gradient SVG backgrounds (no images needed).

```html
<div class="dual-banners" style="display:grid;grid-template-columns:1fr 1fr;gap:.75rem">
  <div class="mini-banner" style="background:linear-gradient(135deg,#1E5631,#4CAF50)">
    <h4>🌱 Kharif Seeds Sale</h4>
    <p>Upto 20% off · Limited stock</p>
  </div>
  <div class="mini-banner" style="background:linear-gradient(135deg,#7A4500,#D4920A)">
    <h4>🚜 Rent a Tractor</h4>
    <p>Starting ₹800/day · Near you</p>
  </div>
</div>
```

---

## Section 7 — Category Emoji Grid (Dynamic)

Fetch all top-level categories across all modules. Show as a 4-per-row grid.

```js
const CATEGORY_EMOJI = {
  agri_marketplace: '🌾', agri_services: '🛠️', repair: '🔧',
  veterinary: '🩺',       animal_marketplace: '🐄',
  crop: '🌾', seed: '🌱', pesticide: '🧴', fertilizer: '🧪', equipment: '🚜',
  poultry: '🐔', fishery: '🐟', animal: '🐮',
};

async function renderCategoryGrid() {
  const { data: modules } = await fetch('/api/v1/masters/modules').then(r => r.json());
  const allCats = (await Promise.all(
    modules.filter(m => m.isActive)
           .map(m => fetch(`/api/v1/masters/modules/${m.id}/categories`).then(r => r.json()).then(j => j.data))
  )).flat().filter(c => c.isActive).sort((a,b) => a.displayOrder - b.displayOrder);

  document.getElementById('catGrid').innerHTML = allCats.map(c => {
    const emoji = CATEGORY_EMOJI[c.categoryKey] || '📦';
    const label = lang === 'en' ? c.categoryNameEn : c.categoryNameHi;
    return `<div class="cat-cell" onclick="onCategoryTap(${c.id},'${c.actionType}','${c.linkKey||''}')">
      <div class="cat-icon">${emoji}</div>
      <div class="cat-lbl">${label}</div>
    </div>`;
  }).join('');
}

function onCategoryTap(categoryId, actionType, linkKey) {
  if (actionType === 'BUSINESS_PROFILE') {
    openBusinessDirectory(linkKey);   // e.g. 'vet_clinic'
  } else {
    openListingsBrowse(categoryId);
  }
}
```

---

## Section 8 — Trending Searches

Call `GET /api/v1/search/trending`. Render top 8 as horizontal scroll chips. Clicking a chip fills the search box and opens the Search overlay.

```js
function renderTrendingChips(trends) {
  document.getElementById('trendRow').innerHTML = trends.slice(0,8).map(t =>
    `<button class="trend-chip" onclick="openSearchWith('${t.query}')">
       ${CATEGORY_EMOJI[t.query?.toLowerCase()] || '🔍'} ${t.query}
     </button>`
  ).join('');
}
```

---

## Section 9 — Flash Deals + Countdown Timer

Call `GET /api/v1/listings/flash-deals`. Render up to 6 listings as compact horizontal cards.

**Flash card:**
```html
<div class="flash-card" onclick="openDetail(listingId)">
  <div class="flash-img">
    <span>{emoji}</span>
    <span class="flash-disc-b">{discountPct}% OFF</span>
  </div>
  <div class="flash-info">
    <div class="flash-name">{listingTitle}</div>
    <div class="flash-price">₹{offeredPrice}<span>/{unit}</span></div>
    <div class="flash-old">₹{actualPrice}</div>
  </div>
</div>
```

**Countdown timer** — counts down to `discountExpiresAt` of first flash deal (or midnight if null):
```js
function startCountdown(targetISO) {
  setInterval(() => {
    const diff = new Date(targetISO) - Date.now();
    if (diff <= 0) return;
    document.getElementById('cdH').textContent = String(Math.floor(diff/3600000)).padStart(2,'0');
    document.getElementById('cdM').textContent = String(Math.floor(diff%3600000/60000)).padStart(2,'0');
    document.getElementById('cdS').textContent = String(Math.floor(diff%60000/1000)).padStart(2,'0');
  }, 1000);
}
```

---

## Section 10 — MSP Price Banner

Call `GET /api/v1/msp/current`. Find the wheat entry (`cropKey === 'WHEAT'`). Show price in banner headline. On tap → show bottom sheet with full table of all MSP prices grouped by season.

```html
<div class="msp-banner" onclick="openMspSheet()">
  <div>🏛️</div>
  <div>
    <div>Govt MSP for Wheat: ₹{wheatPrice}/q — Know your rights</div>
    <div>Minimum Support Price 2025-26 · Tap to view all crops</div>
  </div>
  <div>›</div>
</div>
```

---

## Section 11 — Govt Scheme Banner (Static)

```html
<div class="scheme-banner" onclick="window.open('https://pmkisan.gov.in','_blank')">
  <div>🇮🇳</div>
  <div>
    <div>PM-KISAN — ₹6,000/year direct to your account</div>
    <div>Check eligibility · Apply online · Instant status update</div>
  </div>
  <div>›</div>
</div>
```

---

## Section 12 — Top Seller Stories Strip

From `homeRes.data.featuredProfiles[]`. Render as horizontal scrollable story rings (Instagram-style).

```html
<div class="stories-row" id="storiesRow"></div>
```

```js
function renderTopSellers(profiles) {
  document.getElementById('storiesRow').innerHTML = profiles.slice(0,6).map(p => {
    const ringColor = { ENTERPRISE:'#D4920A', PREMIUM:'#C8A000' }[p.planKey] || '#1E5631';
    return `<div class="story-item" onclick="openSellerListings('${p.userId}')">
      <div class="story-ring" style="border:2.5px solid ${ringColor}">
        <div class="story-av">👤</div>
      </div>
      <div class="story-nm">${(p.businessName||'Seller').slice(0,10)}</div>
    </div>`;
  }).join('');
}
```

---

## Section 13 — Featured Listings Rail

From `homeRes.data.featuredListings.listings[]`. Show skeleton shimmer for 600ms minimum, then replace with real cards.

```html
<div class="skeleton-rail" id="featSkel">
  <!-- 3 skeleton cards -->
</div>
<div class="rail" id="featRail" style="display:none" role="list"></div>
```

```css
.sk-img,.sk-line {
  background: linear-gradient(90deg,#eee 25%,#f5f5f5 50%,#eee 75%);
  background-size:200% 100%;
  animation: shimmer 1.4s infinite;
}
@keyframes shimmer { to { background-position:-200% 0; } }
```

After 600ms: hide skeleton, show `featRail` with rendered cards.

---

## Section 14 — Dynamic Module + Category Rails ⭐ CORE

This is the most important section. Render one rail per module from `homeRes.data.moduleSections[]`.

### Category tree in DB:
```
Agriculture (🌾)
├── agri_marketplace → Agriculture Marketplace  [chips level 1]
│   ├── crop        → Crop (फसल)               [sub-chips level 2]
│   ├── seed        → Seed (बीज)
│   ├── pesticide   → Pesticide (कीटनाशक)
│   ├── fertilizer  → Fertilizer (उर्वरक)
│   └── equipment   → Equipment (उपकरण)
├── agri_services   → Agriculture Services
└── repair          → Repair & Maintenance

Animal & Livestock (🐄)
├── veterinary      → Veterinary  [BUSINESS_PROFILE → vet_clinic]
└── animal_marketplace → Animal Marketplace
    ├── poultry     → Poultry (मुर्गी पालन)
    ├── fishery     → Fishery (मत्स्य पालन)
    └── animal      → Animal (पशु)
```

### Render function:

```js
function renderModuleSections(moduleSections) {
  const container = document.getElementById('moduleSectionsContainer');
  container.innerHTML = '';

  moduleSections.forEach(section => {
    const { id: moduleId, moduleKey, moduleNameEn, moduleNameHi } = section.module;
    const name  = lang === 'en' ? moduleNameEn : moduleNameHi;
    const emoji = MODULE_EMOJI[moduleKey] || '📦';

    const chips = section.topCategories.map((cat, i) => {
      const catEmoji = CATEGORY_EMOJI[cat.categoryKey] || '';
      const catLabel = lang === 'en' ? cat.categoryNameEn : cat.categoryNameHi;
      return `<button class="chip ${i===0?'on':''}"
                data-cat-id="${cat.id}" data-action="${cat.actionType}"
                data-link-key="${cat.linkKey||''}"
                onclick="onChipTap(this,${moduleId})">
                ${catEmoji} ${catLabel}
              </button>`;
    }).join('');

    const cards = section.listings.length
      ? section.listings.map(renderCard).join('')
      : `<div class="empty-rail">No listings yet.</div>`;

    container.insertAdjacentHTML('beforeend', `
      <div class="msec" id="module-${moduleId}">
        <div class="msechd">
          <h2>${emoji} ${name}</h2>
          <button class="see" onclick="openListingsBrowse(null,${moduleId})">See all →</button>
        </div>
        <div class="cchips" id="chips-${moduleId}">${chips}</div>
        <div class="rail"   id="rail-${moduleId}"  role="list">${cards}</div>
      </div>`);
  });
}
```

### Chip tap (level 1):

```js
async function onChipTap(chipEl, moduleId) {
  chipEl.closest('.cchips').querySelectorAll('.chip').forEach(c => c.classList.remove('on'));
  chipEl.classList.add('on');

  const categoryId = chipEl.dataset.catId;
  const actionType = chipEl.dataset.action;
  const linkKey    = chipEl.dataset.linkKey;

  // Remove old sub-chip row
  document.getElementById('chips-' + moduleId).nextElementSibling?.classList.contains('sub-chips-row')
    && document.getElementById('chips-' + moduleId).nextElementSibling.remove();

  if (actionType === 'BUSINESS_PROFILE') {
    openBusinessDirectory(linkKey);
    return;
  }

  // Show loading skeleton
  document.getElementById('rail-' + moduleId).innerHTML = skeletonCards(4);

  // Parallel: subcategories + listings
  const [subRes, listRes] = await Promise.all([
    fetch(`/api/v1/masters/categories/${categoryId}/subcategories`).then(r => r.json()),
    fetch(`/api/v1/listings/category/${categoryId}?page=0&size=10`).then(r => r.json()),
  ]);

  renderSubChips(moduleId, subRes.data || []);

  document.getElementById('rail-' + moduleId).innerHTML =
    (listRes.data?.content?.length)
      ? listRes.data.content.map(renderCard).join('')
      : `<div class="empty-rail">No listings in this category.</div>`;
}
```

### Sub-chip tap (level 2):

```js
function renderSubChips(moduleId, subCats) {
  if (!subCats.length) return;
  const row = document.createElement('div');
  row.className = 'sub-chips-row cchips';
  row.innerHTML = subCats.map(sc => {
    const e = CATEGORY_EMOJI[sc.categoryKey] || '';
    const l = lang === 'en' ? sc.categoryNameEn : sc.categoryNameHi;
    return `<button class="chip sub" data-cat-id="${sc.id}"
              onclick="onSubChipTap(this,${moduleId})">${e} ${l}</button>`;
  }).join('');
  document.getElementById('chips-' + moduleId).after(row);
}

async function onSubChipTap(chipEl, moduleId) {
  chipEl.closest('.sub-chips-row').querySelectorAll('.chip').forEach(c => c.classList.remove('on'));
  chipEl.classList.add('on');

  document.getElementById('rail-' + moduleId).innerHTML = skeletonCards(4);

  const res = await fetch(`/api/v1/listings/category/${chipEl.dataset.catId}?page=0&size=10`);
  const { data } = await res.json();

  document.getElementById('rail-' + moduleId).innerHTML =
    (data?.content?.length)
      ? data.content.map(renderCard).join('')
      : `<div class="empty-rail">No listings found.</div>`;
}

function skeletonCards(n) {
  return Array.from({length:n}).map(() =>
    `<div class="sk-card"><div class="sk-img"></div><div class="sk-line"></div><div class="sk-line sm"></div></div>`
  ).join('');
}
```

---

## Section 15 — Listing Card Component

Used in every rail, grid, search, and nearby. Must be a single reusable `renderCard(l)` function.

```js
function renderCard(l) {
  const saved   = wishlist.has(l.listingId);
  const disc    = l.discountPct ? `<span class="disc-b">${l.discountPct}% OFF</span>` : '';
  const newBadge= (!l.discountPct && l.isNew) ? '<span class="new-badge">NEW</span>' : '';
  const oldPr   = l.actualPrice ? `<span class="pro">₹${fmt(l.actualPrice)}</span>` : '';
  const dist    = l.distanceKm ? `· ${l.distanceKm} km` : '';
  const vtick   = l.sellerVerified ? '<span class="vtick" title="Verified">✓</span>' : '';
  const views   = l.viewCount > 100 ? `<span class="social-proof">🔴 ${l.viewCount} viewing</span>` : '';
  const planB   = { ENTERPRISE:'<span class="plan-b a">🏆 TOP</span>',
                    PREMIUM:   '<span class="plan-b g">⭐ PREM</span>',
                    STANDARD:  '<span class="plan-b gr">✔ STD</span>' }[l.sellerPlanKey] || '';
  const border  = l.isHighlighted ? `style="border-color:${l.highlightColor}"` : '';
  const img     = (l.images?.length)
    ? `<img src="/files/${l.images[0]}" style="width:100%;height:130px;object-fit:cover">`
    : `<div class="ciph">${MODULE_EMOJI[l.moduleKey]||'📦'}</div>`;

  return `
  <article class="lc" ${border} onclick="openDetail('${l.listingId}')" role="listitem" tabindex="0">
    <div class="ci">
      ${img}${disc}${newBadge}${planB}
      <button class="hrt ${saved?'on':''}"
              onclick="event.stopPropagation();toggleWishlist('${l.listingId}',this)">
        ${saved?'❤️':'🤍'}
      </button>
    </div>
    <div class="cb">
      <div class="pr">
        <span class="prm">₹${fmt(l.offeredPrice)}</span>
        ${l.unit?`<span class="pru">/${l.unit}</span>`:''}
        ${oldPr}
      </div>
      <div class="cn">${l.listingTitle||''}${vtick}</div>
      <div class="cm">📍 ${l.address?.district||''} ${dist} · ${relativeTime(l.createdAt)}</div>
      <div class="ctags">
        <span class="ct ${l.listingType==='SELL'?'s':'r'}">${l.listingType}</span>
        ${l.isNegotiable?'<span class="ct n">Neg</span>':''}
      </div>
      ${views}
    </div>
    <div class="cacts">
      <button class="btc-sm"
              onclick="event.stopPropagation();revealContact('${l.listingId}',false)">
        📞 Contact
      </button>
      <button class="btch-sm">💬</button>
    </div>
  </article>`;
}

function fmt(n) { return n ? Number(n).toLocaleString('en-IN') : ''; }
```

---

## Section 16 — Screen Navigation (Overlays)

All overlay screens use `position:fixed; inset:0`. Only one open at a time. Back button pops the stack.

```js
let screenStack = ['home'];

function showScreen(name) {
  document.querySelectorAll('.sco.open').forEach(el => el.classList.remove('open'));
  if (name !== 'home') {
    document.getElementById('sc-' + name)?.classList.add('open');
    document.body.style.overflow = 'hidden';
  } else {
    document.body.style.overflow = '';
  }
  screenStack.push(name);
}

function goBack() {
  screenStack.pop();
  const prev = screenStack[screenStack.length - 1] || 'home';
  showScreen(prev);
}
```

```css
.sco {
  position:fixed; inset:0; z-index:200; background:var(--bg);
  transform:translateX(100%); transition:transform .28s cubic-bezier(.4,0,.2,1);
  overflow-y:auto;
}
.sco.open { transform:translateX(0); }
```

---

## Section 17 — Detail Screen (Overlay)

**Triggered by:** `openDetail(listingId)` on any card click.

**HTML:**
```html
<div id="sc-detail" class="sco" role="dialog" aria-modal="true">
  <div class="sco-hdr">
    <button onclick="goBack()">←</button>
    <span id="detTitle">Loading…</span>
    <div>
      <button id="detHrtBtn" onclick="toggleWishlistDetail()">🤍</button>
      <button onclick="shareDetail()">🔗</button>
    </div>
  </div>
  <div id="detContent"></div>
  <div class="det-cta" style="position:sticky;bottom:0;background:var(--surface);border-top:1px solid var(--border);padding:.75rem;display:flex;gap:.5rem">
    <button id="detContactBtn" style="flex:1;background:var(--green);color:#fff;border-radius:8px;padding:.75rem;font-weight:700"
            onclick="revealContact(null,true)">📞 View Contact</button>
    <button style="width:48px;background:var(--border);border-radius:8px">💬</button>
  </div>
</div>
```

**JS:**
```js
let currentDetailId = null;

async function openDetail(listingId) {
  currentDetailId = listingId;
  document.getElementById('detTitle').textContent = 'Loading…';
  document.getElementById('detContent').innerHTML = skeletonCards(1);
  showScreen('detail');

  const [detRes, simRes] = await Promise.all([
    fetch(`/api/v1/listings/${listingId}`),
    fetch(`/api/v1/listings/${listingId}/similar?limit=6`),
  ]);
  const { data: l }       = await detRes.json();
  const { data: similar } = await simRes.json();

  document.getElementById('detTitle').textContent = l.listingTitle || 'Listing Detail';
  document.getElementById('detHrtBtn').textContent = wishlist.has(listingId) ? '❤️' : '🤍';

  const attrRows = l.attributes
    ? Object.entries(l.attributes)
        .map(([k,v]) => `<div class="attr-item"><b>${k}</b><span>${v}</span></div>`).join('')
    : '';

  const simRail = (similar||[]).length
    ? `<div><h3>Similar Listings</h3><div class="rail">${similar.map(renderCard).join('')}</div></div>` : '';

  document.getElementById('detContent').innerHTML = `
    <!-- Hero -->
    <div style="position:relative;background:var(--border);text-align:center;font-size:5rem;padding:2rem">
      ${l.images?.length ? `<img src="/files/${l.images[0]}" style="width:100%;height:220px;object-fit:cover">` : MODULE_EMOJI[l.moduleKey]||'📦'}
      ${l.discountPct ? `<span class="det-disc-b" style="position:absolute;top:1rem;left:1rem;background:red;color:#fff;padding:.2rem .5rem;border-radius:4px">${l.discountPct}% OFF</span>` : ''}
    </div>

    <!-- Stats -->
    <div style="display:flex;gap:1rem;padding:.75rem 1rem;background:var(--surface);border-bottom:1px solid var(--border)">
      <span>👁 <b>${l.viewCount||0}</b> views</span>
      <span>📞 <b>${l.contactRevealCount||0}</b> contacts</span>
      <span>📦 <b>${fmt(l.quantity)}</b> ${l.unit||'units'}</span>
    </div>

    <!-- Price + Title -->
    <div style="padding:1rem">
      <div style="font-size:1.4rem;font-weight:700">
        ₹${fmt(l.offeredPrice)}${l.unit?`<span style="font-size:.8rem;font-weight:400">/${l.unit}</span>`:''}
        ${l.actualPrice?`<span style="text-decoration:line-through;font-size:.85rem;opacity:.6;margin-left:.5rem">₹${fmt(l.actualPrice)}</span>`:''}
      </div>
      <div style="font-size:1rem;margin:.4rem 0">${l.listingTitle||''}
        ${l.sellerVerified?'<span style="color:#1976D2">✓</span>':''}
      </div>
      <div style="font-size:.8rem;opacity:.7">📍 ${l.address?.district||''} · ${relativeTime(l.createdAt)}</div>
      <div style="margin-top:.5rem">
        <span class="ct ${l.listingType==='SELL'?'s':'r'}">${l.listingType}</span>
        ${l.isNegotiable?'<span class="ct n">Negotiable</span>':''}
      </div>
    </div>

    <!-- Attributes -->
    ${attrRows ? `<div style="padding:.75rem 1rem"><h3>Details</h3><div class="attrs-grid">${attrRows}</div></div>` : ''}

    <!-- Seller -->
    <div style="padding:.75rem 1rem" onclick="openSellerListings('${l.userId}')">
      <h3>Seller</h3>
      <div class="seller-card" style="display:flex;align-items:center;gap:.75rem;padding:.75rem;background:var(--surface);border-radius:10px;border:1px solid var(--border)">
        <div style="font-size:2rem">👤</div>
        <div>
          <div style="font-weight:600">${l.sellerName||'Seller'}</div>
          <div style="font-size:.75rem;opacity:.7">${l.sellerPlanKey||'BASIC'} Plan</div>
        </div>
        <span style="margin-left:auto">›</span>
      </div>
    </div>

    ${simRail}
    <div style="height:80px"></div>`;
}
```

**API response** (`GET /api/v1/listings/{id}`) returns `ListingCardDTO`:

| Field | Used for |
|-------|---------|
| `listingId` | identify, wishlist, contact |
| `listingTitle` | heading |
| `offeredPrice` | price |
| `actualPrice` | strikethrough |
| `discountPct` | discount badge |
| `unit` | price unit |
| `quantity` | available qty |
| `listingType` | SELL/RENT badge |
| `isNegotiable` | Neg badge |
| `images[]` | hero image |
| `address.district` | location |
| `createdAt` | relative time |
| `attributes` | key-value detail grid |
| `viewCount` | stats bar |
| `contactRevealCount` | stats bar |
| `sellerVerified` | blue ✓ tick |
| `sellerPlanKey` | plan badge |
| `userId` | tap seller → browse their listings |
| `isHighlighted` + `highlightColor` | card border color |

---

## Section 18 — Contact Reveal Flow ⭐ CRITICAL

The final step. Called from card's "📞 Contact" button and detail screen's "📞 View Contact" button.

```js
async function revealContact(listingId, fromDetail) {
  const id = fromDetail ? currentDetailId : listingId;
  if (!id) return;

  // Step 1: Login check
  if (!isLoggedIn()) {
    showLoginModal(() => revealContact(id, fromDetail));
    return;
  }

  // Step 2: Loading state
  const btn = fromDetail ? document.getElementById('detContactBtn') : null;
  if (btn) { btn.disabled = true; btn.textContent = 'Loading…'; }

  try {
    const res = await fetch(`/api/v1/listings/${id}/contact`, {
      method: 'POST',
      headers: authHeader(),
    });
    const json = await res.json();

    if (!res.ok) {
      if (json.error?.code === 'CONTACT_HIDDEN') showToast("Seller hasn't enabled contact.");
      else if (res.status === 401) { localStorage.removeItem('tantra_token'); showLoginModal(() => revealContact(id, fromDetail)); }
      else showToast(json.error?.message?.en || 'Something went wrong.');
      return;
    }

    // Step 3: Show contact modal
    showContactModal(json.data);

  } finally {
    if (btn) { btn.disabled = false; btn.textContent = '📞 View Contact'; }
  }
}
```

### Contact Modal HTML (add once in body):
```html
<div id="cmOv"      style="display:none;position:fixed;inset:0;background:rgba(0,0,0,.5);z-index:400"
                    onclick="closeContactModal()"></div>
<div id="contactModal" style="display:none;position:fixed;bottom:0;left:0;right:0;
                              background:var(--surface);border-radius:18px 18px 0 0;
                              padding:1.5rem 1.25rem 2rem;z-index:401;text-align:center">
  <button onclick="closeContactModal()"
          style="position:absolute;top:1rem;right:1rem;background:none;font-size:1.2rem">✕</button>
  <div style="font-size:2rem">📞</div>
  <div style="font-size:.85rem;opacity:.6;margin:.5rem 0">Seller Contact</div>
  <div id="cmPhone" style="font-size:1.5rem;font-weight:700;letter-spacing:1px">—</div>
  <div style="display:flex;gap:.75rem;margin-top:1rem">
    <a id="cmCall" href="#"
       style="flex:1;background:var(--green);color:#fff;padding:.85rem;border-radius:10px;font-weight:700;text-decoration:none">
      📞 Call Now
    </a>
    <a id="cmWa" href="#" target="_blank" rel="noopener"
       style="flex:1;background:#25D366;color:#fff;padding:.85rem;border-radius:10px;font-weight:700;text-decoration:none">
      💬 WhatsApp
    </a>
  </div>
  <div id="cmNote" style="margin-top:.75rem;font-size:.75rem;opacity:.5"></div>
</div>
```

### Show/Hide JS:
```js
function showContactModal(data) {
  document.getElementById('cmPhone').textContent = data.phone;
  document.getElementById('cmCall').href = 'tel:' + data.phone.replace(/[\s-]/g,'');
  document.getElementById('cmWa').href   = data.whatsappUrl;
  document.getElementById('cmNote').textContent = data.alreadyRevealed ? 'Already viewed (no extra charge)' : '';
  document.getElementById('cmOv').style.display      = 'block';
  document.getElementById('contactModal').style.display = 'block';
}
function closeContactModal() {
  document.getElementById('cmOv').style.display      = 'none';
  document.getElementById('contactModal').style.display = 'none';
}
```

---

## Section 19 — Search Overlay Screen

Triggered by tapping the header search input.

```html
<div id="sc-search" class="sco">
  <div class="sco-hdr" style="background:var(--green)">
    <button onclick="goBack()" style="color:#fff">←</button>
    <input id="srchQ" type="search" placeholder="Search crops, seeds, equipment…"
           oninput="doSearch()" style="flex:1;border:none;background:transparent;color:#fff">
    <button onclick="goBack()" style="color:#fff;background:none">Cancel</button>
  </div>
  <!-- Sort bar -->
  <div class="sort-bar">
    <button class="sort-btn on" onclick="setSort(this,'RELEVANCE')">Relevance</button>
    <button class="sort-btn" onclick="setSort(this,'NEWEST')">Newest</button>
    <button class="sort-btn" onclick="setSort(this,'PRICE_ASC')">Price ↑</button>
    <button class="sort-btn" onclick="setSort(this,'PRICE_DESC')">Price ↓</button>
    <button class="sort-btn" onclick="setSort(this,'DISTANCE')">Nearest</button>
  </div>
  <div id="srchMeta" style="padding:.5rem 1rem;font-size:.8rem;opacity:.6"></div>
  <div class="rgrid" id="srchGrid"></div>
</div>
```

```js
let sortMode = 'RELEVANCE';
let searchTimeout;

function openSearch() { showScreen('search'); document.getElementById('srchQ').focus(); }
function openSearchWith(query) { openSearch(); document.getElementById('srchQ').value = query; doSearch(); }

function setSort(btn, mode) {
  document.querySelectorAll('.sort-btn').forEach(b => b.classList.remove('on'));
  btn.classList.add('on');
  sortMode = mode;
  doSearch();
}

function doSearch() {
  clearTimeout(searchTimeout);
  searchTimeout = setTimeout(runSearch, 350);  // debounce 350ms
}

async function runSearch() {
  const q = document.getElementById('srchQ').value.trim();
  if (!q) return;
  const res = await fetch(`/api/v1/search?q=${encodeURIComponent(q)}&sortBy=${sortMode}&page=0&size=20`);
  const { data } = await res.json();
  document.getElementById('srchMeta').textContent = `${data.totalElements} results for "${q}"`;
  document.getElementById('srchGrid').innerHTML = data.content.map(renderCard).join('');
}
```

---

## Section 20 — Filter Panel

```html
<div id="fpOv" style="display:none;position:fixed;inset:0;background:rgba(0,0,0,.4);z-index:300"
               onclick="closeFilter()"></div>
<aside id="fp" style="display:none;position:fixed;bottom:0;left:0;right:0;max-height:80vh;
                       background:var(--surface);border-radius:18px 18px 0 0;
                       overflow-y:auto;z-index:301;padding:1rem">
  <div style="display:flex;justify-content:space-between;align-items:center">
    <span style="font-weight:700">Filters</span>
    <div>
      <button onclick="resetFilters()">Reset all</button>
      <button onclick="closeFilter()">✕</button>
    </div>
  </div>

  <!-- Listing Type -->
  <div class="fp-sec">
    <div>Listing Type</div>
    <label><input type="radio" name="lt" value="" checked> All</label>
    <label><input type="radio" name="lt" value="SELL"> Sell</label>
    <label><input type="radio" name="lt" value="RENT"> Rent</label>
  </div>

  <!-- Price -->
  <div class="fp-sec">
    <div>Price Range (₹)</div>
    <input type="number" id="pmin" placeholder="Min">
    <input type="number" id="pmax" placeholder="Max">
  </div>

  <!-- District -->
  <div class="fp-sec">
    <div>District</div>
    <input type="text" id="fdistrict" placeholder="e.g. Pune">
  </div>

  <!-- Radius -->
  <div class="fp-sec">
    <div>Nearby Radius</div>
    <div id="radG">
      <button class="radc" data-v="5"   onclick="setRadius(this)">5 km</button>
      <button class="radc" data-v="10"  onclick="setRadius(this)">10 km</button>
      <button class="radc on" data-v="25" onclick="setRadius(this)">25 km</button>
      <button class="radc" data-v="50"  onclick="setRadius(this)">50 km</button>
      <button class="radc" data-v="100" onclick="setRadius(this)">100 km</button>
      <button class="radc" data-v=""    onclick="setRadius(this)">All India</button>
    </div>
  </div>

  <!-- Seller Type -->
  <div class="fp-sec">
    <div>Seller Type</div>
    <label><input type="radio" name="st" value="ALL" checked> All Sellers</label>
    <label><input type="radio" name="st" value="SUBSCRIBED"> ⭐ Premium Only</label>
  </div>

  <!-- Posted Within -->
  <div class="fp-sec">
    <div>Posted Within</div>
    <label><input type="radio" name="pw" value="ALL" checked> Any</label>
    <label><input type="radio" name="pw" value="TODAY"> Today</label>
    <label><input type="radio" name="pw" value="WEEK"> Week</label>
    <label><input type="radio" name="pw" value="MONTH"> Month</label>
  </div>

  <button onclick="applyFilters()"
          style="width:100%;background:var(--green);color:#fff;padding:.85rem;border-radius:10px;font-weight:700;margin-top:1rem">
    Apply Filters
  </button>
</aside>
```

---

## Section 21 — Nearby Screen

```html
<div id="sc-nearby" class="sco">
  <div class="sco-hdr"><button onclick="goBack()">←</button><span>Nearby Listings</span></div>
  <div style="padding:1.5rem;text-align:center">
    <h3>Find listings around you</h3>
    <p>Allow location access for distance-based results.</p>
    <button onclick="requestGPS()" style="background:var(--green);color:#fff;padding:.75rem 1.5rem;border-radius:10px">
      📍 Use My Location
    </button>
  </div>
  <div style="display:flex;gap:.5rem;padding:0 1rem">
    <button class="nb-radc" data-v="5" onclick="setNbRad(this)">5 km</button>
    <button class="nb-radc on" data-v="10" onclick="setNbRad(this)">10 km</button>
    <button class="nb-radc" data-v="25" onclick="setNbRad(this)">25 km</button>
    <button class="nb-radc" data-v="50" onclick="setNbRad(this)">50 km</button>
  </div>
  <div class="rgrid" id="nbGrid"></div>
</div>
```

```js
let nbRadius = 10;

function requestGPS() {
  if (!isLoggedIn()) { showLoginModal(() => requestGPS()); return; }
  navigator.geolocation.getCurrentPosition(
    pos => fetchNearby(pos.coords.latitude, pos.coords.longitude, nbRadius),
    ()  => showToast('Location permission denied.')
  );
}

async function fetchNearby(lat, lng, radius) {
  const res = await fetch(
    `/api/v1/listings/nearby?lat=${lat}&lng=${lng}&radius=${radius}&page=0&size=20`,
    { headers: authHeader() }
  );
  const { data } = await res.json();
  document.getElementById('nbGrid').innerHTML = data.content.map(renderCard).join('');
}

function setNbRad(btn) {
  document.querySelectorAll('.nb-radc').forEach(b => b.classList.remove('on'));
  btn.classList.add('on');
  nbRadius = Number(btn.dataset.v);
}
```

---

## Section 22 — Wishlist Screen (localStorage)

```js
let wishlist = new Set(JSON.parse(localStorage.getItem('tantra_wishlist') || '[]'));

function toggleWishlist(listingId, btn) {
  wishlist.has(listingId) ? wishlist.delete(listingId) : wishlist.add(listingId);
  btn.textContent = wishlist.has(listingId) ? '❤️' : '🤍';
  localStorage.setItem('tantra_wishlist', JSON.stringify([...wishlist]));
  // Update badge count
  const badge = document.getElementById('wlBadge');
  badge.textContent = wishlist.size;
  badge.style.display = wishlist.size ? 'flex' : 'none';
}

function showWishlist() {
  showScreen('wishlist');
  const ids = [...wishlist];
  if (!ids.length) {
    document.getElementById('wlContent').innerHTML = '<p style="padding:2rem;text-align:center;opacity:.5">No saved listings yet.</p>';
    return;
  }
  // Re-render cards from saved IDs using already-loaded listing data
  // For a full implementation, fetch each by ID if not in memory
  document.getElementById('wlCount').textContent = ids.length + ' saved';
}
```

---

## Section 23 — Bilingual (EN / HI)

```js
let lang = localStorage.getItem('tantra_lang') || 'en';

function toggleLang() {
  lang = lang === 'en' ? 'hi' : 'en';
  localStorage.setItem('tantra_lang', lang);
  // Update all elements with data-en / data-hi attributes
  document.querySelectorAll('[data-en]').forEach(el => {
    el.textContent = el.dataset[lang];
  });
  document.getElementById('langBtn').textContent = lang === 'en' ? 'हिन्दी' : 'English';
  // Re-render dynamic sections in new language
  renderModuleTabs();
  renderModuleSections(lastHomeData.moduleSections);
}
```

Add `data-en` / `data-hi` attributes to all static text elements in HTML.

---

## Section 24 — Auth Helpers

```js
function isLoggedIn() { return !!localStorage.getItem('tantra_token'); }
function authHeader() {
  const t = localStorage.getItem('tantra_token');
  return t ? { Authorization: 'Bearer ' + t } : {};
}

// Show Login modal, call `callback` after successful login
function showLoginModal(callback) {
  window._postLoginCallback = callback;
  document.getElementById('loginModal').style.display = 'flex';
}

function onLoginSuccess(token) {
  localStorage.setItem('tantra_token', token);
  document.getElementById('loginModal').style.display = 'none';
  if (window._postLoginCallback) { window._postLoginCallback(); window._postLoginCallback = null; }
}

// Toast notification
function showToast(msg, duration = 3000) {
  const t = document.createElement('div');
  t.className = 'toast'; t.textContent = msg;
  document.body.appendChild(t);
  setTimeout(() => t.remove(), duration);
}
```

---

## Section 25 — Bottom Navigation

```html
<nav class="bnav" style="position:fixed;bottom:0;left:0;right:0;height:56px;
                          display:flex;align-items:center;background:var(--surface);
                          border-top:1px solid var(--border);z-index:150">
  <button class="bn-item on" id="bnHome" onclick="goHome()">🏠<br><small>Home</small></button>
  <button class="bn-item" id="bnNearby" onclick="isLoggedIn()?showScreen('nearby'):showLoginModal(()=>showScreen('nearby'))">📍<br><small>Nearby</small></button>
  <div style="flex:1;display:flex;justify-content:center">
    <button onclick="isLoggedIn()?openPostListing():showLoginModal(openPostListing)"
            style="width:52px;height:52px;border-radius:50%;background:var(--green);
                   color:#fff;font-size:1.5rem;box-shadow:0 4px 12px rgba(30,86,49,.4)">+</button>
  </div>
  <button class="bn-item" id="bnWishlist" onclick="showWishlist()">
    ♥<br><small>Saved</small>
    <span id="wlBadge" style="display:none;position:absolute;top:4px;right:4px;
                               background:red;color:#fff;border-radius:10px;font-size:.6rem;padding:1px 4px">0</span>
  </button>
  <button class="bn-item" id="bnMe" onclick="isLoggedIn()?showScreen('profile'):showLoginModal()">👤<br><small>Me</small></button>
</nav>
```

---

## API Endpoint Reference

| # | Method | Endpoint | Auth | When Used |
|---|--------|----------|------|-----------|
| 1 | GET | `/api/v1/home?district=&lang=EN` | Public | Page load — all main sections |
| 2 | GET | `/api/v1/stats/public` | Public | Trust stats bar |
| 3 | GET | `/api/v1/listings/flash-deals` | Public | Flash deals rail |
| 4 | GET | `/api/v1/msp/current` | Public | MSP banner |
| 5 | GET | `/api/v1/search/trending` | Public | Trending search chips |
| 6 | GET | `/api/v1/masters/modules` | Public | Module tabs + category grid |
| 7 | GET | `/api/v1/masters/modules/{moduleId}/categories` | Public | Category grid per module |
| 8 | GET | `/api/v1/masters/categories/{parentId}/subcategories` | Public | Sub-chips on chip tap |
| 9 | GET | `/api/v1/business-profiles/top-sellers` | Public | Stories strip |
| 10 | GET | `/api/v1/business-profiles/directory?profileType=vet_clinic` | Public | Veterinary chip tap |
| 11 | GET | `/api/v1/search?q=&sortBy=&page=&size=` | Public | Search screen |
| 12 | GET | `/api/v1/listings/category/{categoryId}?page=&size=` | Public | Chip → listing cards |
| 13 | GET | `/api/v1/listings/{id}` | Public | Detail screen |
| 14 | GET | `/api/v1/listings/{id}/similar?limit=6` | Public | Detail → similar rail |
| 15 | POST | `/api/v1/listings/{id}/contact` | **JWT** | Contact reveal |
| 16 | GET | `/api/v1/listings/nearby?lat=&lng=&radius=` | **JWT** | Nearby screen |
| 17 | GET | `/api/v1/notifications/unread-count` | **JWT** | Bell badge dot |
| 18 | POST | `/api/v1/auth/signin` | Public | Login |
| 19 | POST | `/api/v1/auth/signup` | Public | Register |

**All response envelopes:** `{ success, data, message: { en, hi }, traceId }`  
**Error response:** `{ success: false, error: { code, message: { en, hi } } }`  
**Paginated responses:** `data.content[]`, `data.totalElements`, `data.totalPages`, `data.last`

---

## Complete User Flow Summary

```
1. Page loads → initHomePage() fires 5 parallel API calls → all sections render

2. User taps MODULE TAB (Agriculture / Animal & Livestock)
   → page scrolls to that module's rail (no API call needed)

3. User taps TOP-LEVEL CATEGORY CHIP (e.g. Agriculture Marketplace)
   → GET /api/v1/masters/categories/{id}/subcategories  ← parallel with ↓
   → GET /api/v1/listings/category/{id}?page=0&size=10
   → Rail refreshes with listings; sub-chips row appears below

4. User taps SUB-CATEGORY CHIP (e.g. Crop / Seed / Equipment)
   → GET /api/v1/listings/category/{subId}?page=0&size=10
   → Rail refreshes with that specific sub-category's listings

5. User taps a LISTING CARD → openDetail(listingId)
   → Detail overlay slides in (showScreen('detail'))
   → GET /api/v1/listings/{id}          ← parallel with ↓
   → GET /api/v1/listings/{id}/similar
   → Full detail screen renders with price, attributes, seller, similar listings
   → Sticky [📞 View Contact] [💬] bar at bottom

6. User taps "📞 View Contact" or "📞 Contact" on a card
   → If not logged in → Login modal → after login → retry revealContact()
   → If logged in → POST /api/v1/listings/{id}/contact  (Bearer token)
   → On 200 OK → Contact modal slides up with phone number + [📞 Call] [💬 WhatsApp]
   → On 403 CONTACT_HIDDEN → toast "Seller hasn't enabled contact"
   → On 401 → clear token, re-show login modal
```

---

## Error Handling Rules

| HTTP | Code | UI Behavior |
|------|------|-------------|
| 400 | `CONTACT_HIDDEN` | `showToast("Seller hasn't enabled contact")` |
| 400 | `LISTING_INACTIVE` | `showToast("This listing is no longer active")` |
| 401 | `UNAUTHORIZED` | Clear token → show login modal |
| 403 | `FORBIDDEN` | `showToast("You don't have permission")` |
| 404 | `NOT_FOUND` | Show empty state in current screen |
| 500 | `INTERNAL_ERROR` | `showToast("Something went wrong. Please try again.")` |

---

## Checklist Before Submitting

- [ ] `initHomePage()` fires 5 parallel API calls on load, no sequential waterfalls
- [ ] Module tabs render dynamically from `/api/v1/masters/modules` (not hardcoded)
- [ ] Category grid renders all top-level categories from DB (currently 5: agri_marketplace, agri_services, repair, veterinary, animal_marketplace)
- [ ] Veterinary chip navigates to business directory, NOT listing browse
- [ ] `renderModuleSections()` loops over all `moduleSections[]` — no hardcoded agriculture/animal sections
- [ ] Level-1 chip tap → sub-chips appear + listings refresh (both parallel)
- [ ] Level-2 sub-chip tap → listings refresh only (no sub-sub-chips)
- [ ] Every listing card `onclick` calls `openDetail(listingId)`
- [ ] Detail screen shows skeleton before data loads
- [ ] Detail screen fetches listing + similar in parallel
- [ ] "📞 View Contact" and card "📞 Contact" both call `revealContact()`
- [ ] Listing browse and detail are public — `/listings/category/**`, `/listings/*`, `/listings/*/similar` need NO JWT (guests can browse freely)
- [ ] Only `/listings/{id}/contact` (POST) and `/listings/nearby` (GET) require JWT
- [ ] `revealContact()` checks login FIRST, shows login modal if not authenticated
- [ ] Nearby screen bottom nav button shows login modal if not logged in (nearby requires JWT)
- [ ] Contact modal shows phone + Call + WhatsApp buttons
- [ ] `showScreen()` / `goBack()` manage stack correctly (back from detail returns to previous screen, not always home)
- [ ] Dark mode works via `@media (prefers-color-scheme: dark)` and `data-theme` toggle
- [ ] EN/HI toggle re-renders all dynamic sections in selected language
- [ ] Wishlist persists in `localStorage`
- [ ] Skeleton shimmer shown during all async loads
- [ ] Toast shown for all API errors
- [ ] No hardcoded category names, module names, or listing data anywhere
