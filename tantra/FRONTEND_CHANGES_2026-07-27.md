# Tantra — Frontend Change Notes (2026-07-27)

Only **today's changes** and what the frontend must do for each. For the full contract see
`FRONTEND_DEVELOPER_GUIDE.md` / `FRONTEND_API.md`. Business-profile changes are in
`BUSINESS_PROFILE_FRONTEND_GUIDE.md` (other workstream).

The live server has been seeded (both modules, all forms) + **10 demo listings with images** in the
Agriculture Marketplace. Test user: `9000000002 / Seller@123`, Admin `9000000001 / Admin@123`.

---

## 1. Address is NO LONGER a form field — the frontend owns it
The listing forms no longer contain an ADDRESS field or a "use default address" toggle. **Render your own
address picker** on the listing screen (Default / Saved / Create-new) and pass a reference on submit:

```
POST /api/v1/listings   (and PUT for edit)
{ ...other fields...,
  "addressId": "TN..."           // a saved address  — OR
  "useDefaultAddress": true      // shortcut for the default — OR
  "address": { fullAddress, country, state, district, city, village, pinCode, latitude, longitude, mobileNumber, altMobileNumber }  // raw inline
}
```
- Populate the picker from `GET /api/v1/addresses` (+ `GET /api/v1/addresses/default`).
- **"Create new address" from the listing screen:** `POST /api/v1/addresses` first → get `addressId` → send that on the listing (so it's saved + linked). Or send a raw `address` object if the user doesn't want to save it.
- The **backend** snapshots the chosen address onto the listing and stores the reference.

**On a listing read you now get BOTH:**
- `listing.addressId` — the saved-address reference (null if a raw address was used) → use to highlight/preselect in the picker on edit.
- `listing.address` — the frozen snapshot (city, pincode, lat/long, mobile) → **display this**. It never changes even if the saved address is later edited/deleted.

---

## 2. New Contact section on every listing form
Two new fields (section `contact`):
- **`contactNumber`** (TEXT, **required**, 10-digit) — the number for this listing. It's a **common/top-level** field (send at the top level of the listing body, not in `attributes`). On the "Post listing" confirmation you can prefill it from the default address's number or let the user type a new one — either way it's saved **only on the listing**, never written back to the address.
- **`showContact`** (BOOLEAN) — "**Show my contact number & address to buyers**". When **off**, the number *and* address stay hidden until a buyer's contact-request is approved. Render the switch; send `showContact` top-level. (The reveal/masking itself comes with the contact-request feature; for now just capture the toggle.)

```
POST /api/v1/listings
{ "categoryId":.., "listingType":"SELL", "actualPrice":.., "offeredPrice":..,
  "contactNumber":"9000000002", "showContact":true, "addressId":"TN..",
  "images":["/files/..."], "attributes": { ...category fields... } }
```

---

## 3. Pricing simplified + Rent/Sell show-hide
- **Removed** all per-unit price fields (Price/Kg, Price/g, Price/Quintal, Rate Slab, etc.). The Pricing
  section is now just **Actual Price · Offered Price · Discount % (auto) · Is Negotiable**. Seller sets
  actual/offered against the quantity+unit.
- **Equipment & Animal (Rent OR Sell)** — the form has `availableFor` (rent/sell). Honour `visibleWhen`:
  - `availableFor = sell` → show **Actual Price** + Offered Price + Discount
  - `availableFor = rent` → show **Rent Per Hour** + Offered Price + Discount
  - Offered Price + Discount show in **both** modes.
- **Discount** is a read-only AUTO_CALC (compute for display: `(base − offered)/base` where base = actualPrice
  for sell, rentPerHour for rent). The backend also stores `listing.discountPct` correctly for both — so you
  can just read `discountPct` from a listing.

---

## 4. Quantity + Unit — single required pair, unit can cascade
- One **Quantity** (number, required) + one **Unit** (dropdown, required). The old duplicate unit fields
  (cropUnit/seedUnit) and the split qtyLiquid/qtyPowder are gone.
- **Fertilizer & Pesticide:** the **Unit cascades from the Powder/Liquid Type** (see §6):
  - Type = Liquid → Unit options: `ml, litre`
  - Type = Powder → Unit options: `gram, kg, quintal`

---

## 5. "Pack Of" field
New integer field **`packOf`** (in the quantity section) on **Seed, Pesticide, Fertilizer** (packaged goods).
Render as a number input; send in `attributes.packOf`.

---

## 6. Cascading dropdowns — how to render (important)
Several dropdowns now **cascade** (child options depend on a parent field). A child field carries
**`parentField`** (the parent's `fieldKey`); every option has its own **`id`** and a **`parent`** (the
parent option's id).

**To render a child dropdown:** when the user picks the parent, take that option's `id`, then show only the
child options where `option.parent === pickedParentId`. (Options are already inlined in the form; for large
sets you may instead call `GET /api/v1/option-sets/{setKey}/items?parentItemId={pickedParentId}`.)

Cascades in play today:
| Category | Parent → Child |
|---|---|
| Crop | cropType → cropName |
| Seed | seedType → seedName |
| Fertilizer | fertilizerCategory → fertilizerName · **fertilizerType (Powder/Liquid) → Unit** |
| Pesticide | **pesticideType (Powder/Liquid) → Unit** (name is now free text — see §7) |
| Equipment | equipmentCategory → equipmentName |
| Poultry | poultryCategory → poultryName |
| Services | serviceCategory → serviceType (Labour → a multiselect) |
| Repair | repairCategory → repairType |

If `parentField` is set and the parent value is `__other__`, the child hides and a free-text "Please Specify"
field shows instead (handled by `visibleWhen`).

---

## 7. Pesticide form specifics
- **`pesticideName` is now a free TEXT field (required)** — not a dropdown. Seller types the product name.
- **`pesticideDescription`** and **`pesticideUsage`** are now **required**.
- New required field **`pesticideUsedFor`** — "Used For (Disease / Pest)".
- Category (Insecticide/Fungicide/…) is still a dropdown; Type (Powder/Liquid) drives the Unit cascade.

---

## 8. Field order (readability)
Forms are ordered **Name → Description → rest**. `displayOrder` already reflects this — just render fields
in the order the API returns them.

---

## 9. Per-field flags recap (honour these on render)
`required`, `readOnly`, `fieldLength`, `validation` (min/max/maxLength/regex/`integer`), `visibleWhen`
`{field,operator,value}` (operators: equals / notEquals / **in** / **notIn**), `allowOther`, **`parentField`**
(cascade), **`help`** (hint text under the field — new; e.g. on `showContact`), `editableOnUpdate` (lock on
edit), `inlineEditable` (grid quick-edit).

---

## 10. Business Profile (other workstream) — today's additions
Brief pointers (full detail in `BUSINESS_PROFILE_FRONTEND_GUIDE.md`):
- New **BLOCKED** status (permanent take-down; owner can't edit/resubmit) alongside REJECTED (fixable).
- Every write returns `status` **+ `reason`** (rejected/blocked reason) — always show it.
- Admin **Approval Tracker**: `GET /admin/business-profiles/stats` (count tiles) + `/history` list + `/block` action.

---

## 11. Ready-to-use demo data (already on the server)
- Agriculture Marketplace: **2 listings per category** (Crop, Seed, Pesticide, Fertilizer, Equipment) with
  real images served at `/files/...`, correct cascade attributes, discounts, contact fields, and address-book
  linkage (8 → Farm/Bhopal, 2 equipment → Warehouse/Vidisha).
- Seller address book: **Farm (Bhopal, default)**, Warehouse (Vidisha), Home (Indore).
- Fetch: `GET /api/v1/listings/mine` (as the seller) or `GET /api/v1/listings/category/{id}`.
