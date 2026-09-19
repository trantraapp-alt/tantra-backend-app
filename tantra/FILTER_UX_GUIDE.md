# Tantra — Filter UX Guide for Frontend Developer

> **Rule:** Filters are NEVER shown on the Home screen.  
> Show the filter icon only when `moduleId` or `categoryId` is present in the current browse context.  
> Like Flipkart / OLX — home is for discovery, browse is for filtering.

---

## 1. Screen-by-Screen Filter Rules

| Screen | Route | Filter Shown? | What Changes |
|--------|-------|---------------|--------------|
| Home Feed | `/home` | ❌ Never | — |
| Module Browse | `/browse?moduleId=1` | ✅ Yes | + Category multi-select on top |
| Category Browse | `/browse?categoryId=4` | ✅ Yes | + Sub-category selector + Attribute filters |
| Sub-category Browse | `/browse?categoryId=45` (leaf) | ✅ Yes | + Attribute filters only |

---

## 2. System Filter Block — Always the Same

Build this **once** as a shared `<SystemFilterBlock />` component.  
Drop it into the filter drawer at **every browse stage without any changes**.  
Values are hardcoded — no API call needed for this block.

| # | Filter Name | Input Type | Options / Behavior |
|---|-------------|------------|-------------------|
| 1 | Listing Type | RADIO | Sell / Rent / Buy |
| 2 | Price Range | PRICE_RANGE | Min (₹) + Max (₹) text inputs |
| 3 | Posted Within | RADIO | Today / This Week / This Month |
| 4 | Seller Type | RADIO | All Sellers / Premium Only (`SUBSCRIBED`) |
| 5 | Location | Two dropdowns | District + State (free text OK) |

> These 5 filters are **identical** at Module browse, Category browse, and Sub-category browse.  
> Only the context selectors and attribute filters change per stage.

---

## 3. Per-Stage Filter Drawer Layout

### Stage 2 — Module Browse (`/browse?moduleId=1`)

```
Filter Drawer
├── Category multi-select          ← fetched from GET /master/categories?moduleId=1
└── <SystemFilterBlock />          ← same 5 filters, no change
```

**API call on screen enter:**
```js
const categories = await fetch('/api/v1/master/categories?moduleId=1')
// Build category checkbox list from response.data
// Render <SystemFilterBlock /> below — no filter-form call needed
```

---

### Stage 3 — Category Browse (`/browse?categoryId=4`)

```
Filter Drawer
├── Sub-category selector          ← fetched from GET /master/categories?parentId=4
├── <SystemFilterBlock />          ← same 5 filters, no change
└── Attribute filters              ← fetched from GET /api/v1/filter-form?categoryId=4
    (variety, breed, brand, etc.)    render each group by inputType
```

**API calls on screen enter:**
```js
const [filterForm, subCats] = await Promise.all([
  fetch('/api/v1/filter-form?categoryId=4'),
  fetch('/api/v1/master/categories?parentId=4')
])
// subCats.data   → render as sub-category chip strip / multi-select
// filterForm.data.groups filtered by attributeKey → render attribute filters
// <SystemFilterBlock /> stays the same component
```

---

### Stage 4 — Sub-category Browse (`/browse?categoryId=45` — leaf node)

```
Filter Drawer
├── <SystemFilterBlock />          ← same 5 filters, no change
└── Attribute filters              ← fetched from GET /api/v1/filter-form?categoryId=45
```

**API call on screen enter:**
```js
const filterForm = await fetch('/api/v1/filter-form?categoryId=45')
// No sub-category strip (leaf node — no children)
// filterForm.data.groups → render attribute filters
// <SystemFilterBlock /> stays the same component
```

---

## 4. Filter State Object

```js
const [filters, setFilters] = useState({
  // context — read-only, drives which filter UI to show
  moduleId:   null,
  categoryId: null,

  // system filters — same keys at every browse stage
  listingType:  null,   // 'SELL' | 'RENT' | 'BUY'
  minPrice:     null,
  maxPrice:     null,
  postedWithin: null,   // 'TODAY' | 'WEEK' | 'MONTH'
  sellerType:   null,   // 'ALL' | 'SUBSCRIBED'
  district:     null,
  state:        null,

  // attribute filters — keys come from filter-form API, reset on category change
  attributes: {}        // e.g. { variety: 'HDA19', breed: null }
})
```

**Reset attributes when category changes:**
```js
function navigateToCategory(categoryId) {
  setFilters(prev => ({ ...prev, categoryId, attributes: {} }))
  router.push(`/browse?categoryId=${categoryId}`)
}
```

---

## 5. Building the Listing API URL from Filter State

```js
function buildBrowseUrl(filters) {
  const p = new URLSearchParams()
  if (filters.categoryId)   p.set('categoryId',   filters.categoryId)
  if (filters.moduleId)     p.set('moduleId',     filters.moduleId)
  if (filters.listingType)  p.set('listingType',  filters.listingType)
  if (filters.minPrice)     p.set('minPrice',     filters.minPrice)
  if (filters.maxPrice)     p.set('maxPrice',     filters.maxPrice)
  if (filters.postedWithin) p.set('postedWithin', filters.postedWithin)
  if (filters.sellerType)   p.set('sellerType',   filters.sellerType)
  if (filters.district)     p.set('district',     filters.district)
  if (filters.state)        p.set('state',        filters.state)
  // attribute filters — prefix with attr_
  Object.entries(filters.attributes).forEach(([k, v]) => {
    if (v) p.set(`attr_${k}`, v)
  })
  return `/api/v1/listings/category/${filters.categoryId}?${p}`
}
```

**Example URL:**
```
GET /api/v1/listings/category/4
  ?listingType=SELL
  &minPrice=500&maxPrice=5000
  &postedWithin=WEEK
  &sellerType=SUBSCRIBED
  &district=Pune
  &attr_variety=HDA19
```

---

## 6. Sticky Filter Bar (browse screens only)

```
[ Sort ▾ ] | [ ⚙ Filter (2) ] | [ Sell ✕ ] [ Near Me ] [ Today ]
```

- Sticks below the app header on scroll (`position: sticky; top: <header-height>`)
- **Sort button** → bottom sheet: Newest / Price Low→High / Price High→Low / Nearest
- **Filter button** → opens full filter drawer; badge = count of active filters
- **Quick chips** → single-tap applies one filter immediately (no drawer needed)

```js
const QUICK_CHIPS = [
  { label: 'Sell',    filter: { listingType: 'SELL'    } },
  { label: 'Rent',    filter: { listingType: 'RENT'    } },
  { label: 'Near Me', filter: { district: userDistrict } },
  { label: 'Today',   filter: { postedWithin: 'TODAY'  } },
]

function activeFilterCount(filters) {
  const systemKeys = ['listingType','minPrice','maxPrice','postedWithin','sellerType','district','state']
  const sys  = systemKeys.filter(k => filters[k] != null).length
  const attr = Object.values(filters.attributes).filter(v => v != null).length
  return sys + attr  // show on badge; hide badge if 0
}
```

---

## 7. Sort Options

Pass as `sortBy` + `sortDir` params to `/api/v1/listings/category/{id}`:

| Label | sortBy | sortDir |
|-------|--------|---------|
| Newest First | `createdAt` | `DESC` |
| Price: Low → High | `price` | `ASC` |
| Price: High → Low | `price` | `DESC` |
| Distance: Nearest | `distance` | `ASC` |

---

## 8. Implementation Rules

1. **Hide filter icon on Home screen completely.** Only show it when `moduleId` or `categoryId` is in route context.
2. **Build `<SystemFilterBlock />` once — reuse it unchanged at every browse stage.** The 5 system filters are identical everywhere.
3. **Reset `attributes` when category changes.** Attribute groups are category-specific — stale values from another category will send invalid params.
4. **Show sub-category strip only when children exist.** Check `GET /master/categories?parentId=X`. If empty array → leaf node → skip the strip.
5. **Prefix attribute filter URL params with `attr_`.** System params go in directly; attribute params go as `attr_variety=HDA19`.
6. **Show an active filter count badge** on the filter button. Count = non-null system filters + non-null attributes.
7. **Always provide a "Clear All" button** inside the filter drawer. Resets everything except `moduleId` / `categoryId`.

---

## 9. API Endpoint Summary

| Endpoint | When Called | Returns |
|----------|-------------|---------|
| `GET /api/v1/master/categories?moduleId=1` | Module browse opens | Root categories for that module |
| `GET /api/v1/master/categories?parentId=4` | Category browse opens | Sub-categories (children) |
| `GET /api/v1/filter-form?categoryId=4` | Category browse opens | System groups + attribute groups |
| `GET /api/v1/listings/category/{id}?…` | Filter applied / page loads | Paginated listings matching filters |
