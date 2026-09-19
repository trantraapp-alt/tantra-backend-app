# Tantra — Filter Form API Guide for Frontend Developer

> This guide covers `GET /api/v1/filter-form` — the backend endpoint that tells the frontend  
> **which attribute filters to show** for a given category (variety, breed, brand, etc.).  
> System filters (Listing Type, Price, Date, Seller, Location) are **not** built from this API —  
> those are hardcoded in `<SystemFilterBlock />`. See `FILTER_UX_GUIDE.md`.

---

## 1. Endpoint

```
GET /api/v1/filter-form
GET /api/v1/filter-form?categoryId=4
```

- No auth required (public)
- `categoryId` is optional. Omit for global/module-level filter (returns system groups only)
- Response envelope: read payload from `.data`

---

## 2. Response Structure

```json
{
  "categoryId": 4,
  "groups": [
    {
      "groupKey": "listingType",
      "label": { "en": "Listing Type", "hi": "लिस्टिंग प्रकार" },
      "inputType": "RADIO",
      "displayOrder": 1,
      "queryParam": "listingType",
      "options": [
        { "value": "SELL", "label": { "en": "Sell", "hi": "बेचें" } },
        { "value": "RENT", "label": { "en": "Rent", "hi": "किराया" } },
        { "value": "BUY",  "label": { "en": "Buy",  "hi": "खरीदें" } }
      ]
    },
    {
      "groupKey": "variety",
      "label": { "en": "Variety", "hi": "किस्म" },
      "inputType": "DROPDOWN",
      "displayOrder": 6,
      "queryParam": "variety",
      "attributeKey": "variety",
      "optionSetKey": "wheat_variety",
      "options": [
        { "value": "HDA19",   "label": { "en": "HD-2967", "hi": "एच॰डी॰-2967" } },
        { "value": "LOK1",    "label": { "en": "Lokwan",  "hi": "लोकवान" } },
        { "value": "PBW621",  "label": { "en": "PBW-621", "hi": "पी॰बी॰डब्लू॰-621" } }
      ]
    }
  ]
}
```

---

## 3. FilterGroup Fields

| Field | Type | Description |
|-------|------|-------------|
| `groupKey` | string | Unique key for this filter group |
| `label` | `{en, hi}` | Display label — show in current language |
| `inputType` | enum | How to render the widget (see Section 4) |
| `displayOrder` | int | Render groups in ascending order |
| `queryParam` | string | Single param name for listing API (most groups) |
| `queryParams` | string[] | Multiple param names (PRICE_RANGE, GEO_RADIUS) |
| `attributeKey` | string | Present on attribute groups — use as `attr_{key}=value` in listing API |
| `optionSetKey` | string | Internal key for the option set (informational) |
| `options` | array | Available choices — each has `value`, `label:{en,hi}` |

> **System groups** (listingType, priceRange, postedWithin, sellerType, location) have no `attributeKey`.  
> **Attribute groups** (variety, breed, brand…) have `attributeKey` set. These are the category-specific filters.

---

## 4. InputType — Widget Reference

### RADIO
Single selection. Render as radio buttons or toggle chips.

```jsx
// groupKey: listingType, postedWithin, sellerType
<div>
  {group.options.map(opt => (
    <label key={opt.value}>
      <input type="radio"
        name={group.groupKey}
        value={opt.value}
        checked={filters[group.queryParam] === opt.value}
        onChange={() => setFilter(group.queryParam, opt.value)}
      />
      {opt.label[lang]}
    </label>
  ))}
</div>
```

**API param:** `?listingType=SELL` / `?postedWithin=WEEK` / `?sellerType=SUBSCRIBED`

---

### DROPDOWN
Single selection from a list. Render as `<select>` or a bottom-sheet picker.

```jsx
// groupKey: variety, breed, brand, model, type, cropType, animalType…
<select
  value={filters.attributes[group.attributeKey] || ''}
  onChange={e => setAttr(group.attributeKey, e.target.value)}
>
  <option value="">{lang === 'EN' ? 'Any' : 'कोई भी'}</option>
  {group.options.map(opt => (
    <option key={opt.value} value={opt.value}>{opt.label[lang]}</option>
  ))}
</select>
```

**API param:** `?attr_variety=HDA19`

---

### MULTISELECT
Multiple selections. Render as checkboxes or multi-select chips.

```jsx
// Allow selecting multiple values
const selected = filters.attributes[group.attributeKey] || []   // array

{group.options.map(opt => (
  <label key={opt.value}>
    <input type="checkbox"
      checked={selected.includes(opt.value)}
      onChange={() => toggleAttr(group.attributeKey, opt.value)}
    />
    {opt.label[lang]}
  </label>
))}
```

**API param:** `?attr_breed=HF&attr_breed=JERSEY` (repeat param for multiple values)

---

### PRICE_RANGE
Two numeric inputs — min and max price.

```jsx
// queryParams: ['minPrice', 'maxPrice']
<div>
  <input type="number" placeholder="Min ₹"
    value={filters.minPrice || ''}
    onChange={e => setFilter('minPrice', e.target.value)}
  />
  <input type="number" placeholder="Max ₹"
    value={filters.maxPrice || ''}
    onChange={e => setFilter('maxPrice', e.target.value)}
  />
</div>
```

**API params:** `?minPrice=500&maxPrice=5000`

---

### LOCATION
District + State selector. Two dropdowns or free-text inputs.

```jsx
// queryParams: ['district', 'state']
<input placeholder="District" value={filters.district || ''}
  onChange={e => setFilter('district', e.target.value)} />
<input placeholder="State" value={filters.state || ''}
  onChange={e => setFilter('state', e.target.value)} />
```

**API params:** `?district=Pune&state=Maharashtra`

---

### GEO_RADIUS
GPS-based radius filter. Show preset buttons.

```jsx
// queryParams: ['lat', 'lng', 'radius']
const RADIUS_OPTIONS = [5, 10, 25, 50, 100]

{RADIUS_OPTIONS.map(r => (
  <button key={r}
    className={filters.radius === r ? 'active' : ''}
    onClick={() => {
      setFilter('lat', userLat)
      setFilter('lng', userLng)
      setFilter('radius', r)
    }}
  >{r} km</button>
))}
```

**API params:** `?lat=18.5204&lng=73.8567&radius=25`

---

## 5. Filtering Groups: System vs Attribute

```js
// Split into system groups and attribute groups
const systemGroups = filterForm.data.groups.filter(g => !g.attributeKey)
const attrGroups   = filterForm.data.groups.filter(g =>  g.attributeKey)

// Render order in filter drawer:
// 1. Sub-category selector (if parentId has children)
// 2. <SystemFilterBlock />   ← hardcoded, do NOT use systemGroups from API
// 3. attrGroups              ← dynamic, from API, sorted by displayOrder
```

> **Important:** Even though system groups are returned by the API, use your hardcoded  
> `<SystemFilterBlock />` component for them. Only use `attrGroups` from the API.  
> This keeps the system filter UI consistent at every browse stage.

---

## 6. Full Integration Example

```js
// On entering category browse screen
async function onCategoryEnter(categoryId) {
  const [filterRes, subCatRes] = await Promise.all([
    fetch(`/api/v1/filter-form?categoryId=${categoryId}`)
      .then(r => r.json()),
    fetch(`/api/v1/master/categories?parentId=${categoryId}`)
      .then(r => r.json())
  ])

  const attrGroups = filterRes.data.groups
    .filter(g => g.attributeKey)
    .sort((a, b) => a.displayOrder - b.displayOrder)

  const subCategories = subCatRes.data   // empty array = leaf node

  setFilterContext({ categoryId, attrGroups, subCategories })
}

// Render the filter drawer
function FilterDrawer({ context, filters, onChange }) {
  return (
    <div>
      {context.subCategories.length > 0 && (
        <SubCategorySelector items={context.subCategories} ... />
      )}

      <SystemFilterBlock filters={filters} onChange={onChange} />

      {context.attrGroups.map(group => (
        <AttributeFilterGroup key={group.groupKey} group={group}
          value={filters.attributes[group.attributeKey]}
          onChange={val => onChange({ attributes: {
            ...filters.attributes,
            [group.attributeKey]: val
          }})}
        />
      ))}

      <button onClick={clearAll}>Clear All</button>
    </div>
  )
}
```

---

## 7. Known Attribute Keys (Category-Specific)

These `attributeKey` values appear in the filter-form response for relevant categories:

| attributeKey | Categories | InputType |
|---|---|---|
| `variety` | Wheat, Rice, other crops | DROPDOWN |
| `breed` | Cattle, Poultry, Animals | DROPDOWN |
| `brand` | Seeds, Fertilizers, Pesticides | DROPDOWN |
| `serviceType` | Services | DROPDOWN |
| `species` | Fishery | DROPDOWN |
| `model` | Equipment | DROPDOWN |
| `type` | General equipment/products | DROPDOWN |
| `cropType` | Crop-related categories | DROPDOWN |
| `animalType` | Animal categories | DROPDOWN |
| `equipmentType` | Equipment categories | DROPDOWN |

> Only attributes relevant to the requested `categoryId` are returned.  
> A category with no attribute filters returns only system groups.

---

## 8. Live API Test

```bash
# Global filter form (system groups only)
curl http://localhost:8080/api/v1/filter-form

# Category-specific (system + attribute groups)
curl http://localhost:8080/api/v1/filter-form?categoryId=4

# Expected: 5 system groups + 1 attribute group (variety) for categoryId=4 (Wheat)
```
