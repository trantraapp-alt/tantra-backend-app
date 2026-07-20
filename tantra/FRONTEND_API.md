# Tantra — Frontend API Guide (Listings + Images)

Base URL (local): `http://localhost:8080`
> On a phone/other device use the backend machine's LAN IP (e.g. `http://192.168.1.5:8080`), not `localhost`.
> In production this becomes the deployed domain. Keep it in one config constant.

All responses are JSON. Bilingual messages come as `{ "en": "...", "hi": "..." }` — pick by the user's language.

---

## 1. Auth — get a token

**Sign in**
```
POST /api/v1/auth/signin
{ "mobileNumber": "9000000002", "password": "Seller@123" }

→ { "token": "<JWT>", "role": "ROLE_SELLER", "userId": "TN3XYZ45" }
```
Store the `token`. Send it on every protected call:
```
Authorization: Bearer <token>
```
(Sign up: `POST /api/v1/auth/signup` with firstName, lastName, mobileNumber, password, appUsageRole, preferredLanguage.)

---

## 2. Get the form to render (metadata-driven UI)

```
GET /api/v1/categories/{categoryId}/form?listingType=SELL      (no token needed)
```
Returns the form as **sections → fields**. Render each field by its `type`:

| field.type | Render as |
|---|---|
| TEXT / TEXTAREA / NUMBER / DECIMAL | input / textarea |
| DROPDOWN / RADIO / MULTISELECT / CHECKBOX_GROUP | use `field.options` (each has `value` + `label{en,hi}`) |
| BOOLEAN | switch / checkbox |
| IMAGE | the uploader (see §3) |
| ADDRESS | the address sub-form |

Each field also carries these control flags — honor them all:
- `required` — mandatory field (isMandatory)
- `readOnly` — show the value but don't let the user edit it (e.g. auto-calculated discount)
- `fieldLength` — max characters for text/textarea inputs
- `validation` — min / max / maxLength / regex / acceptedTypes
- `visibleWhen` — show this field only when another field equals a value (conditional)
- `allowOther` — show a “Please Specify” text box when the user picks `__other__`

**Cascading dropdowns** (e.g. State→City):
```
GET /api/v1/option-sets/{setKey}/items?parentItemId={id}
```

---

## 3. Image upload (STEP 1 of listing)

Upload the picked/captured photos FIRST, get back URLs.
```
POST /api/v1/uploads          (Authorization required)
Content-Type: multipart/form-data
field name: "files"           ← one or many image files
```
- Do **not** set Content-Type manually — the browser sets the multipart boundary.
- Allowed: jpg, png, webp. Max: 10 files (from the IMAGE field's `validation`).

Response:
```json
{ "success": true, "urls": ["/files/ab12.jpg", "/files/cd34.jpg"] }
```
Keep these `urls`. To show an image: `BASE_URL + url` → `http://localhost:8080/files/ab12.jpg` (public, no token).

Camera vs gallery is a UI choice — both upload the same way:
```html
<input type="file" accept="image/*" capture="environment" multiple>  <!-- Take Photo -->
<input type="file" accept="image/*" multiple>                        <!-- Gallery -->
```

---

## 4. Create the listing (STEP 2)

Send form data + the image URLs from step 3 (NOT the files again).
```
POST /api/v1/listings         (Authorization required)
{
  "categoryId": 5,
  "listingType": "SELL",
  "actualPrice": 300,
  "offeredPrice": 280,
  "isNegotiable": true,
  "images": ["/files/ab12.jpg", "/files/cd34.jpg"],
  "useDefaultAddress": false,
  "address": { "fullAddress":"...", "country":"IN", "state":"MP", "city":"Bhopal",
               "pinCode":"462001", "latitude":23.25, "longitude":77.41,
               "mobileNumber":"9000000002", "altMobileNumber":"9000000003" },
  "attributes": { "cropName":"wheat", "qty":50, "qtyMeasurement":"quintal" }
}

→ { "success": true, "listingId": "TN7ABC12", "userId": "TN3XYZ45",
    "message": { "en":"Listing created successfully!", "hi":"..." } }
```
- **Common fields** (price, images, isNegotiable, address) are top-level.
- **Category-specific fields** go inside `attributes`, keyed by each field's `fieldKey` from §2.
- Don't send userId / listingId / mobile — backend fills them from the token.

**Read back:** `GET /api/v1/listings/mine` (Authorization) → the user's listings with full data.

---

## 5. Rules to remember
1. Upload field name is **`files`**.
2. Image URLs are relative (`/files/..`) → prepend the base URL to display.
3. `max 10` images, types jpg/png/webp — enforce from the IMAGE field's `validation`.
4. Two steps: **upload → get URLs → submit listing with URLs.** Photos and listing never travel together.
5. Everything is metadata-driven — render fields from §2, don't hardcode forms.

---

## Reference
Ready-to-run Postman collections are in the `postman/` folder (one per category) — import to see every request with sample bodies.
```
