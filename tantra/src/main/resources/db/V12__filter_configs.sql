-- V12: Filter Configuration Master Data
-- Drives the filter bottom-sheet on the listing browse / category screen.
-- Scope hierarchy: GLOBAL (all modules) → CATEGORY (override per category_key)
-- The service merges: global first, then category overrides replace matching filterKey.

CREATE TABLE filter_configs (
    id            SERIAL       PRIMARY KEY,
    scope         VARCHAR(20)  NOT NULL DEFAULT 'GLOBAL',  -- GLOBAL | CATEGORY
    scope_key     VARCHAR(50)  NULL,    -- NULL for GLOBAL; category_key for CATEGORY scope
    filter_key    VARCHAR(50)  NOT NULL,
    label_en      VARCHAR(100) NOT NULL,
    label_hi      VARCHAR(100) NOT NULL,
    filter_type   VARCHAR(20)  NOT NULL,  -- CHIP_SELECT | RANGE
    config        JSONB        NOT NULL,
    display_order INTEGER      NOT NULL DEFAULT 0,
    is_active     BOOLEAN      NOT NULL DEFAULT true
);

-- Partial unique indexes (handles NULL scope_key correctly — UNIQUE constraint skips NULLs)
CREATE UNIQUE INDEX uq_filter_global
    ON filter_configs (scope, filter_key)
    WHERE scope_key IS NULL;

CREATE UNIQUE INDEX uq_filter_category
    ON filter_configs (scope, scope_key, filter_key)
    WHERE scope_key IS NOT NULL;

CREATE INDEX idx_filter_scope_active
    ON filter_configs (scope, scope_key, is_active);

-- ── GLOBAL FILTERS ────────────────────────────────────────────────────────────
-- Shown for every module / category. Category-scope rows override matching filter_key.

INSERT INTO filter_configs (scope, scope_key, filter_key, label_en, label_hi, filter_type, config, display_order) VALUES

-- 1. Listing Type  ──────────────────────────────────────────────────────────────
('GLOBAL', NULL, 'listingType',
 'Listing Type', 'लिस्टिंग प्रकार',
 'CHIP_SELECT',
 '{
   "multiSelect": false,
   "defaultValue": "ALL",
   "options": [
     {"value": "ALL",  "labelEn": "All",  "labelHi": "सभी"},
     {"value": "SELL", "labelEn": "Sell", "labelHi": "बेचना"},
     {"value": "RENT", "labelEn": "Rent", "labelHi": "किराया"}
   ]
 }',
 1),

-- 2. Price Range (default — overridden per category below)  ─────────────────────
('GLOBAL', NULL, 'priceRange',
 'Price Range', 'मूल्य सीमा',
 'RANGE',
 '{
   "min": 0,
   "max": 2000000,
   "step": 1000,
   "displayMin": "₹0",
   "displayMax": "₹20L+"
 }',
 2),

-- 3. Distance  ──────────────────────────────────────────────────────────────────
('GLOBAL', NULL, 'distance',
 'Distance', 'दूरी',
 'CHIP_SELECT',
 '{
   "multiSelect": false,
   "defaultValue": -1,
   "options": [
     {"value": 1,   "labelEn": "1 km",      "labelHi": "1 किमी"},
     {"value": 5,   "labelEn": "5 km",      "labelHi": "5 किमी"},
     {"value": 10,  "labelEn": "10 km",     "labelHi": "10 किमी"},
     {"value": 25,  "labelEn": "25 km",     "labelHi": "25 किमी"},
     {"value": 50,  "labelEn": "50 km",     "labelHi": "50 किमी"},
     {"value": 100, "labelEn": "100 km",    "labelHi": "100 किमी"},
     {"value": -1,  "labelEn": "All India", "labelHi": "पूरा भारत"}
   ]
 }',
 3),

-- 4. Seller Type (linked to subscription plan tiers)  ──────────────────────────
('GLOBAL', NULL, 'sellerType',
 'Seller Type', 'विक्रेता प्रकार',
 'CHIP_SELECT',
 '{
   "multiSelect": false,
   "defaultValue": "ALL",
   "options": [
     {"value": "ALL",     "labelEn": "All Sellers", "labelHi": "सभी विक्रेता"},
     {"value": "BRONZE",  "labelEn": "Bronze+",     "labelHi": "ब्रॉन्ज+"},
     {"value": "SILVER",  "labelEn": "Silver+",     "labelHi": "सिल्वर+"},
     {"value": "GOLD",    "labelEn": "Gold+",        "labelHi": "गोल्ड+"},
     {"value": "DIAMOND", "labelEn": "Diamond",      "labelHi": "डायमंड"}
   ]
 }',
 4),

-- 5. Posted Within  ─────────────────────────────────────────────────────────────
('GLOBAL', NULL, 'postedWithin',
 'Posted Within', 'कब पोस्ट हुई',
 'CHIP_SELECT',
 '{
   "multiSelect": false,
   "defaultValue": "ANY",
   "options": [
     {"value": "ANY",        "labelEn": "Any Time",   "labelHi": "कभी भी"},
     {"value": "TODAY",      "labelEn": "Today",      "labelHi": "आज"},
     {"value": "THIS_WEEK",  "labelEn": "This Week",  "labelHi": "इस सप्ताह"},
     {"value": "THIS_MONTH", "labelEn": "This Month", "labelHi": "इस महीने"}
   ]
 }',
 5);


-- ── CATEGORY-SPECIFIC OVERRIDES ───────────────────────────────────────────────
-- Only priceRange differs per category.  More filter overrides (e.g. listingType
-- for equipment SELL+RENT) can be added here later without any code change.

INSERT INTO filter_configs (scope, scope_key, filter_key, label_en, label_hi, filter_type, config, display_order) VALUES

-- crop: typical quintal / per-bag pricing (₹0 – ₹2L)
('CATEGORY', 'crop', 'priceRange',
 'Price Range', 'मूल्य सीमा', 'RANGE',
 '{"min": 0, "max": 200000, "step": 500, "displayMin": "₹0", "displayMax": "₹2L+"}',
 2),

-- seed: per-packet / per-kg (₹0 – ₹10K)
('CATEGORY', 'seed', 'priceRange',
 'Price Range', 'मूल्य सीमा', 'RANGE',
 '{"min": 0, "max": 10000, "step": 50, "displayMin": "₹0", "displayMax": "₹10K+"}',
 2),

-- pesticide: small unit pricing (₹0 – ₹20K)
('CATEGORY', 'pesticide', 'priceRange',
 'Price Range', 'मूल्य सीमा', 'RANGE',
 '{"min": 0, "max": 20000, "step": 100, "displayMin": "₹0", "displayMax": "₹20K+"}',
 2),

-- fertilizer: per-bag / per-tonne (₹0 – ₹20K)
('CATEGORY', 'fertilizer', 'priceRange',
 'Price Range', 'मूल्य सीमा', 'RANGE',
 '{"min": 0, "max": 20000, "step": 100, "displayMin": "₹0", "displayMax": "₹20K+"}',
 2),

-- equipment: tractors, harvesters — high-value machinery (₹0 – ₹50L)
('CATEGORY', 'equipment', 'priceRange',
 'Price Range', 'मूल्य सीमा', 'RANGE',
 '{"min": 0, "max": 5000000, "step": 10000, "displayMin": "₹0", "displayMax": "₹50L+"}',
 2),

-- animal: cattle, buffalo, goat (₹0 – ₹5L)
('CATEGORY', 'animal', 'priceRange',
 'Price Range', 'मूल्य सीमा', 'RANGE',
 '{"min": 0, "max": 500000, "step": 1000, "displayMin": "₹0", "displayMax": "₹5L+"}',
 2),

-- poultry: hens, chicks, eggs (₹0 – ₹50K)
('CATEGORY', 'poultry', 'priceRange',
 'Price Range', 'मूल्य सीमा', 'RANGE',
 '{"min": 0, "max": 50000, "step": 100, "displayMin": "₹0", "displayMax": "₹50K+"}',
 2),

-- fishery: fish stock, prawn, fingerlings (₹0 – ₹1L)
('CATEGORY', 'fishery', 'priceRange',
 'Price Range', 'मूल्य सीमा', 'RANGE',
 '{"min": 0, "max": 100000, "step": 500, "displayMin": "₹0", "displayMax": "₹1L+"}',
 2);
