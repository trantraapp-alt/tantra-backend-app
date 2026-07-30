# Tantra — Listing Form Frontend Guide (implementation)

How to build the **Create / Edit Listing screen** with everything from the 2026-07-27 work: metadata-driven
rendering, cascading dropdowns, conditional visibility, the address picker, the contact/privacy section,
Rent/Sell pricing, and the submit payload. Companion to `FRONTEND_CHANGES_2026-07-27.md` (the change list).

- **Base URL:** `http://localhost:8080` (one constant). Envelope: read payload from `data`; bilingual text is `{en,hi}`.
- **Server is seeded** with both modules + 10 demo listings (images) + a seller address book. Test user `9000000002 / Seller@123`.

---

## 0. The one idea
There is **one generic form component**. You fetch the form metadata for a category and render it. You do
**not** hardcode fields per category. Only a few things are handled outside the metadata: image upload,
the address picker, and the submit-payload split (common fields vs `attributes`).

```
Navigate to a leaf category  →  GET its form  →  render fields  →  collect answers  →  POST /listings
```

---

## 1. Fetch the form
```
GET /api/v1/categories/{categoryId}/form?listingType=SELL      (SELL | RENT; no token needed)
→ data: { formId, version, listingType, categoryId, title{en,hi}, sections: [ { key, title{en,hi}, fields: [ RenderField ] } ] }
```
For Rent/Sell categories (Equipment, Animal) request `SELL` or `RENT` — the backend serves the shared form;
the real type is decided by the `availableFor` field the user picks.

Each **RenderField**:
```
{ fieldKey, type, label{en,hi}, required, readOnly, fieldLength, editableOnUpdate, inlineEditable,
  placeholder, help{en,hi}, displayOrder, allowOther, multiple, common, optionSetKey,
  parentField, options:[ { id, value, label{en,hi}, parent } ], validation, computed, visibleWhen }
```

---

## 2. The generic field renderer
Render each field by `type`, in `displayOrder` (already Name → Description → rest). Group by section (§5).

| type | widget |
|---|---|
| TEXT / TEXTAREA | text input / textarea |
| NUMBER / DECIMAL | numeric input (NUMBER = integer if `validation.integer`) |
| DROPDOWN / RADIO | single-select from `options` (each `{id,value,label{en,hi},parent}`) |
| MULTISELECT / CHECKBOX_GROUP | multi-select from `options` |
| BOOLEAN | switch |
| DATE | date picker |
| IMAGE | uploader (§6a) |
| AUTO_CALC | read-only computed (e.g. Discount %) — see §6d |
| ADDRESS | **does not appear** in listing forms — use the address picker (§6b) |

**Honour every flag** (pseudocode):
```js
function renderField(f, form, values) {
  if (!isVisible(f, values)) return null;                       // §4 visibleWhen
  const opts = optionsFor(f, values);                           // §3 cascade filtering
  const widget = widgetFor(f.type, { options: opts,
      required: f.required,
      disabled: f.readOnly || (isEditMode && f.editableOnUpdate === false),
      maxLength: f.fieldLength,
      hint: f.help,                                             // NEW: show helper text under the field
      validate: f.validation });                                // min/max/maxLength/regex/integer
  if (f.allowOther && values[f.fieldKey] === '__other__') showSpecifyBox();
  return widget;
}
```
Submit key = `f.fieldKey`. Store the **`value`** (itemKey) for dropdowns, not the label.

---

## 3. Cascading dropdowns (parentField)
A child dropdown carries **`parentField`** (the parent's `fieldKey`). Every option has its own **`id`** and a
**`parent`** (the parent option's id). Filter the child by the parent's selected option id:

```js
function optionsFor(field, values) {
  if (!field.parentField) return field.options;                 // normal dropdown
  const parentVal = values[field.parentField];                  // e.g. cropType = "cereal"
  if (parentVal == null) return [];                             // pick parent first
  const parentOpt = fieldByKey(field.parentField).options.find(o => o.value === parentVal);
  if (!parentOpt) return [];
  return field.options.filter(o => o.parent === parentOpt.id);  // children of the chosen parent
}
```
- When the parent changes, **clear the child value**.
- Options are inlined; for very large sets you may instead `GET /api/v1/option-sets/{optionSetKey}/items?parentItemId={parentOpt.id}`.

**Cascades live today:** Crop `cropType→cropName`, Seed `seedType→seedName`, Fertilizer
`fertilizerCategory→fertilizerName` **and** `fertilizerType→qtyMeasurement (unit)`, Pesticide
`pesticideType→qtyMeasurement (unit)`, Equipment `equipmentCategory→equipmentName`, Poultry
`poultryCategory→poultryName`, Services `serviceCategory→serviceType`, Repair `repairCategory→repairType`.

> **Unit-by-type:** for Fertilizer/Pesticide, the **Unit** dropdown is a child of the Powder/Liquid **Type** —
> Liquid → `ml, litre`; Powder → `gram, kg, quintal`. Same algorithm above (parentField = the type field).

---

## 4. Conditional visibility (visibleWhen)
```js
function isVisible(f, values) {
  const w = f.visibleWhen; if (!w) return true;
  const v = values[w.field];
  switch (w.operator) {
    case 'equals':    return v === w.value;
    case 'notEquals': return v !== w.value;
    case 'in':        return Array.isArray(w.value) && w.value.includes(v);
    case 'notIn':     return Array.isArray(w.value) && !w.value.includes(v);
  }
}
```
Used for: Rent/Sell price fields (`availableFor`), the `__other__` "Please Specify" text boxes, the Services
Labour multiselect (`serviceCategory in ['labour']`), etc. Treat hidden required fields as not-required.

---

## 5. Sections
Render `sections[]` as a **multi-step wizard** or one scroll — your choice; the backend is the same. Section
keys you'll see: `details, quantity, pricing, media, contact` (address is its own picker, not a section).

---

## 6. The non-metadata blocks

### 6a. Images (IMAGE field) — two-step upload
```
POST /api/v1/uploads   (Authorization; multipart/form-data, field name "files", 1–10 jpg/png/webp)
→ data: { urls: ["/files/ab12.jpg", ...] }
```
Keep the `urls`; put them in the listing's `images` array. Display: `BASE_URL + url` (public). Camera vs
gallery from `validation.sources` / `capture`.

### 6b. Address picker (NOT a form field)
Render your own picker on the listing screen:
```
GET /api/v1/addresses           → saved addresses (populate the list)
GET /api/v1/addresses/default   → the default
```
UI: [ Use default ] · [ Select a saved address ▾ ] · [ + Create new address ]. On submit, pass ONE of:
- `addressId: "<saved id>"`  — a saved address, or
- `useDefaultAddress: true`  — the default, or
- `address: { fullAddress, country, state, district, city, village, pinCode, latitude, longitude, mobileNumber, altMobileNumber }` — raw inline.

"Create new" → `POST /api/v1/addresses` first, get `addressId`, then send that (saved + linked). The backend
snapshots it. **On edit**, preselect using `listing.addressId`; display `listing.address` (the snapshot).

### 6c. Contact section
- **`contactNumber`** (TEXT, required, 10-digit) — top-level field. On the "Post listing" confirmation,
  offer "use the default address's number" or "enter a new number". Saved only on the listing.
- **`showContact`** (BOOLEAN, has `help` text) — "Show my contact number & address to buyers". Off = number
  and address hidden until a buyer's contact request is approved. Send top-level.

### 6d. Pricing (AUTO_CALC + Rent/Sell)
Fields: `actualPrice`, `offeredPrice`, `discountPct` (AUTO_CALC, read-only), `isNegotiable`, and for
Equipment/Animal `rentPerHour`.
- Honour `visibleWhen` on `availableFor`: **sell** shows `actualPrice`; **rent** shows `rentPerHour`;
  `offeredPrice` + `discountPct` show in both.
- Compute the discount for display:
  ```js
  const base = availableFor === 'rent' ? rentPerHour : actualPrice;
  discountPct = base ? Math.round((base - offeredPrice) / base * 10000) / 100 : null;
  ```
  (The backend also stores the correct `discountPct` for both modes, so on read you can just use it.)

### 6e. Quantity + Unit
One **Quantity** (`qty`, required) + one **Unit** (`qtyMeasurement`, required, may cascade from Type — §3).
Some categories also have **`packOf`** (integer). All go in `attributes`.

---

## 7. Build the submit payload
Split by the `common` flag: **common fields → top level**, everything else → **`attributes`** (keyed by `fieldKey`).
Common fields today: `actualPrice, offeredPrice, isNegotiable, images, contactNumber, showContact` (+ the
address reference). Everything category-specific (cropType, cropName, qty, qtyMeasurement, packOf, rentPerHour, …)
goes in `attributes`.

```js
const body = { categoryId, moduleId, listingType, images: uploadedUrls,
  actualPrice, offeredPrice, isNegotiable, contactNumber, showContact,
  addressId /* or useDefaultAddress / address */,
  attributes: { /* every non-common field by fieldKey */ } };
POST /api/v1/listings   → data: { listingId, userId, message{en,hi} }
```
Don't send userId/listingId/discount/mobile — the backend fills/recomputes those.

---

## 8. Create vs Edit
- **Create:** `POST /api/v1/listings`.
- **Edit (full):** `PUT /api/v1/listings/{listingId}` — prefill from `GET form` + `GET /listings/{id}`
  (common from top-level, category from `attributes[fieldKey]`). **Lock** fields where
  `editableOnUpdate === false` (the cascade parent + name, `availableFor`) — show disabled.
- **Grid quick-edit:** `PATCH /listings/{id}` — only `inlineEditable` fields (price/qty/status).

---

## 9. Reading a listing (My Listings / detail)
```
GET /api/v1/listings/mine?listingType=&status=&page=&size=     → page: read data.content
GET /api/v1/listings/{listingId}
```
- Price: `listingType==RENT` → show `attributes.rentPerHour` (+ offered); else `offeredPrice` (+ strike `actualPrice`, `discountPct`).
- Address: display `listing.address` (snapshot). Identify source via `listing.addressId`.
- Contact: `contactNumber`; if you later show public browse, respect `showContact` (hide number+address until approved — that logic ships with contact-requests).
- Images: `BASE_URL + images[i]`.

---

## 10. End-to-end example (Crop, create)
```
1) GET /categories/{cropId}/form?listingType=SELL
2) Render: cropType (dropdown) → cropName (cascades) → cropDescription → cropVariety → qualityGrade →
   harvestYear → harvestMonth | qty + qtyMeasurement | actualPrice + offeredPrice + discount + negotiable |
   images | contactNumber + showContact
3) Address picker → user picks default → addressId
4) POST /uploads → urls
5) POST /listings:
   { categoryId, listingType:"SELL", actualPrice:2200, offeredPrice:2100, isNegotiable:true,
     contactNumber:"9000000002", showContact:true, addressId:"TN...", images:["/files/.."],
     attributes:{ cropType:"cereal", cropName:"wheat", cropDescription:"...", qualityGrade:"A",
                  harvestYear:"2025", harvestMonth:"04", qty:50, qtyMeasurement:"quintal" } }
```

---

## 11. Build checklist
- [ ] Generic renderer: type→widget + flags (required/readOnly/fieldLength/validation/help)
- [ ] Cascade filtering by `parentField` (+ clear child on parent change); unit-by-type for Fertilizer/Pesticide
- [ ] `visibleWhen` evaluator (equals/notEquals/in/notIn); `allowOther` "Please Specify"
- [ ] Sections as wizard/scroll
- [ ] Image uploader (2-step) → `images`
- [ ] Address picker (default/saved/create-new) → `addressId` | `useDefaultAddress` | `address`
- [ ] Contact section: `contactNumber` (required) + `showContact` switch (with `help`)
- [ ] Pricing: Rent/Sell `visibleWhen`; discount display; read `discountPct` on listings
- [ ] Quantity + Unit + `packOf`
- [ ] Submit split: common top-level vs `attributes`
- [ ] Edit: prefill + lock `editableOnUpdate:false`; grid PATCH for `inlineEditable`
