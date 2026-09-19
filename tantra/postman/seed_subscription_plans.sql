-- =============================================================================
-- Tantra Subscription Plans Seed
-- Bronze / Silver / Gold / Diamond  ×  Monthly / Quarterly / Half-Yearly / Yearly
--
-- Run once in tantra_db (psql or pgAdmin).
-- Safe to re-run: ON CONFLICT (plan_key) DO UPDATE keeps data current.
-- =============================================================================

-- Step 1: Add feature columns if they don't exist yet
-- (Hibernate @ColumnDefault handles fresh tables; this covers existing rows)
ALTER TABLE subscription_plans
    ADD COLUMN IF NOT EXISTS max_contact_views            INT NOT NULL DEFAULT -1,
    ADD COLUMN IF NOT EXISTS max_modifications_per_listing INT NOT NULL DEFAULT -1,
    ADD COLUMN IF NOT EXISTS priority_visibility_days     INT NOT NULL DEFAULT 0;

-- Step 2: Also fix contact_reveal_count on listings if not done yet
ALTER TABLE listings
    ADD COLUMN IF NOT EXISTS contact_reveal_count BIGINT NOT NULL DEFAULT 0;

-- =============================================================================
-- Step 3: Insert / upsert all 16 plans
-- Columns: plan_key, name, price, duration_months,
--          max_listings, max_contact_views, max_modifications_per_listing,
--          priority_visibility_days, features,
--          sort_weight, listing_highlight, profile_highlight,
--          home_feed_slots, highlight_color, badge_label, is_active,
--          created_at, updated_at
-- =============================================================================

INSERT INTO subscription_plans (
    plan_key, name, price, duration_months,
    max_listings, max_contact_views, max_modifications_per_listing, priority_visibility_days,
    features,
    sort_weight, listing_highlight, profile_highlight,
    home_feed_slots, highlight_color, badge_label, is_active,
    created_at, updated_at
) VALUES

-- ─────────────────────────────────────────────────────────────────────────────
-- BRONZE  (₹49/mo · 5 listings · 10 contacts · 1 mod · 7-day boost)
-- ─────────────────────────────────────────────────────────────────────────────
('BRONZE_MONTHLY',
 '{"en":"Bronze Monthly","hi":"ब्रॉन्ज़ मासिक"}',
 49.00, 1,
 5, 10, 1, 7,
 '{"en":["5 Active Listings","10 Contact Views / month","Edit each listing 1 time","7-day priority visibility"],"hi":["5 सक्रिय लिस्टिंग","10 संपर्क दृश्य / माह","प्रत्येक लिस्टिंग 1 बार संपादन","7 दिन प्राथमिकता दृश्यता"]}',
 11, false, false, 0, '#CD7F32',
 '{"en":"Bronze Seller","hi":"ब्रॉन्ज़ विक्रेता"}',
 true, NOW(), NOW()),

('BRONZE_QUARTERLY',
 '{"en":"Bronze Quarterly","hi":"ब्रॉन्ज़ त्रैमासिक"}',
 129.00, 3,
 5, 30, 1, 7,
 '{"en":["5 Active Listings","30 Contact Views / quarter","Edit each listing 1 time","7-day priority visibility","Save ₹18 vs monthly"],"hi":["5 सक्रिय लिस्टिंग","30 संपर्क दृश्य / तिमाही","प्रत्येक लिस्टिंग 1 बार संपादन","7 दिन प्राथमिकता दृश्यता","मासिक से ₹18 बचत"]}',
 12, false, false, 0, '#CD7F32',
 '{"en":"Bronze Seller","hi":"ब्रॉन्ज़ विक्रेता"}',
 true, NOW(), NOW()),

('BRONZE_HALF_YEARLY',
 '{"en":"Bronze Half-Yearly","hi":"ब्रॉन्ज़ छमाही"}',
 239.00, 6,
 5, 60, 1, 7,
 '{"en":["5 Active Listings","60 Contact Views / 6 months","Edit each listing 1 time","7-day priority visibility","Save ₹55 vs monthly"],"hi":["5 सक्रिय लिस्टिंग","60 संपर्क दृश्य / 6 माह","प्रत्येक लिस्टिंग 1 बार संपादन","7 दिन प्राथमिकता दृश्यता","मासिक से ₹55 बचत"]}',
 13, false, false, 0, '#CD7F32',
 '{"en":"Bronze Seller","hi":"ब्रॉन्ज़ विक्रेता"}',
 true, NOW(), NOW()),

('BRONZE_YEARLY',
 '{"en":"Bronze Yearly","hi":"ब्रॉन्ज़ वार्षिक"}',
 449.00, 12,
 5, 120, 1, 7,
 '{"en":["5 Active Listings","120 Contact Views / year","Edit each listing 1 time","7-day priority visibility","Save ₹139 vs monthly"],"hi":["5 सक्रिय लिस्टिंग","120 संपर्क दृश्य / वर्ष","प्रत्येक लिस्टिंग 1 बार संपादन","7 दिन प्राथमिकता दृश्यता","मासिक से ₹139 बचत"]}',
 14, false, false, 0, '#CD7F32',
 '{"en":"Bronze Seller","hi":"ब्रॉन्ज़ विक्रेता"}',
 true, NOW(), NOW()),

-- ─────────────────────────────────────────────────────────────────────────────
-- SILVER  (₹99/mo · 15 listings · 15 contacts · 2 mods · 15-day boost)
-- ─────────────────────────────────────────────────────────────────────────────
('SILVER_MONTHLY',
 '{"en":"Silver Monthly","hi":"सिल्वर मासिक"}',
 99.00, 1,
 15, 15, 2, 15,
 '{"en":["15 Active Listings","15 Contact Views / month","Edit each listing 2 times","15-day priority visibility"],"hi":["15 सक्रिय लिस्टिंग","15 संपर्क दृश्य / माह","प्रत्येक लिस्टिंग 2 बार संपादन","15 दिन प्राथमिकता दृश्यता"]}',
 21, false, false, 0, '#C0C0C0',
 '{"en":"Silver Seller","hi":"सिल्वर विक्रेता"}',
 true, NOW(), NOW()),

('SILVER_QUARTERLY',
 '{"en":"Silver Quarterly","hi":"सिल्वर त्रैमासिक"}',
 269.00, 3,
 15, 45, 2, 15,
 '{"en":["15 Active Listings","45 Contact Views / quarter","Edit each listing 2 times","15-day priority visibility","Save ₹28 vs monthly"],"hi":["15 सक्रिय लिस्टिंग","45 संपर्क दृश्य / तिमाही","प्रत्येक लिस्टिंग 2 बार संपादन","15 दिन प्राथमिकता दृश्यता","मासिक से ₹28 बचत"]}',
 22, false, false, 0, '#C0C0C0',
 '{"en":"Silver Seller","hi":"सिल्वर विक्रेता"}',
 true, NOW(), NOW()),

('SILVER_HALF_YEARLY',
 '{"en":"Silver Half-Yearly","hi":"सिल्वर छमाही"}',
 519.00, 6,
 15, 90, 2, 15,
 '{"en":["15 Active Listings","90 Contact Views / 6 months","Edit each listing 2 times","15-day priority visibility","Save ₹75 vs monthly"],"hi":["15 सक्रिय लिस्टिंग","90 संपर्क दृश्य / 6 माह","प्रत्येक लिस्टिंग 2 बार संपादन","15 दिन प्राथमिकता दृश्यता","मासिक से ₹75 बचत"]}',
 23, false, false, 0, '#C0C0C0',
 '{"en":"Silver Seller","hi":"सिल्वर विक्रेता"}',
 true, NOW(), NOW()),

('SILVER_YEARLY',
 '{"en":"Silver Yearly","hi":"सिल्वर वार्षिक"}',
 979.00, 12,
 15, 180, 2, 15,
 '{"en":["15 Active Listings","180 Contact Views / year","Edit each listing 2 times","15-day priority visibility","Save ₹209 vs monthly"],"hi":["15 सक्रिय लिस्टिंग","180 संपर्क दृश्य / वर्ष","प्रत्येक लिस्टिंग 2 बार संपादन","15 दिन प्राथमिकता दृश्यता","मासिक से ₹209 बचत"]}',
 24, false, false, 0, '#C0C0C0',
 '{"en":"Silver Seller","hi":"सिल्वर विक्रेता"}',
 true, NOW(), NOW()),

-- ─────────────────────────────────────────────────────────────────────────────
-- GOLD  (₹199/mo · 30 listings · 30 contacts · 5 mods · 30-day boost · highlight)
-- ─────────────────────────────────────────────────────────────────────────────
('GOLD_MONTHLY',
 '{"en":"Gold Monthly","hi":"गोल्ड मासिक"}',
 199.00, 1,
 30, 30, 5, 30,
 '{"en":["30 Active Listings","30 Contact Views / month","Edit each listing 5 times","30-day priority visibility","Gold badge on listings","1 home feed slot"],"hi":["30 सक्रिय लिस्टिंग","30 संपर्क दृश्य / माह","प्रत्येक लिस्टिंग 5 बार संपादन","30 दिन प्राथमिकता दृश्यता","लिस्टिंग पर गोल्ड बैज","1 होम फ़ीड स्लॉट"]}',
 31, true, false, 1, '#FFD700',
 '{"en":"Gold Seller","hi":"गोल्ड विक्रेता"}',
 true, NOW(), NOW()),

('GOLD_QUARTERLY',
 '{"en":"Gold Quarterly","hi":"गोल्ड त्रैमासिक"}',
 549.00, 3,
 30, 90, 5, 30,
 '{"en":["30 Active Listings","90 Contact Views / quarter","Edit each listing 5 times","30-day priority visibility","Gold badge on listings","1 home feed slot","Save ₹48 vs monthly"],"hi":["30 सक्रिय लिस्टिंग","90 संपर्क दृश्य / तिमाही","प्रत्येक लिस्टिंग 5 बार संपादन","30 दिन प्राथमिकता दृश्यता","लिस्टिंग पर गोल्ड बैज","1 होम फ़ीड स्लॉट","मासिक से ₹48 बचत"]}',
 32, true, false, 1, '#FFD700',
 '{"en":"Gold Seller","hi":"गोल्ड विक्रेता"}',
 true, NOW(), NOW()),

('GOLD_HALF_YEARLY',
 '{"en":"Gold Half-Yearly","hi":"गोल्ड छमाही"}',
 1049.00, 6,
 30, 180, 5, 30,
 '{"en":["30 Active Listings","180 Contact Views / 6 months","Edit each listing 5 times","30-day priority visibility","Gold badge on listings","1 home feed slot","Save ₹145 vs monthly"],"hi":["30 सक्रिय लिस्टिंग","180 संपर्क दृश्य / 6 माह","प्रत्येक लिस्टिंग 5 बार संपादन","30 दिन प्राथमिकता दृश्यता","लिस्टिंग पर गोल्ड बैज","1 होम फ़ीड स्लॉट","मासिक से ₹145 बचत"]}',
 33, true, false, 1, '#FFD700',
 '{"en":"Gold Seller","hi":"गोल्ड विक्रेता"}',
 true, NOW(), NOW()),

('GOLD_YEARLY',
 '{"en":"Gold Yearly","hi":"गोल्ड वार्षिक"}',
 1999.00, 12,
 30, 360, 5, 30,
 '{"en":["30 Active Listings","360 Contact Views / year","Edit each listing 5 times","30-day priority visibility","Gold badge on listings","1 home feed slot","Save ₹389 vs monthly"],"hi":["30 सक्रिय लिस्टिंग","360 संपर्क दृश्य / वर्ष","प्रत्येक लिस्टिंग 5 बार संपादन","30 दिन प्राथमिकता दृश्यता","लिस्टिंग पर गोल्ड बैज","1 होम फ़ीड स्लॉट","मासिक से ₹389 बचत"]}',
 34, true, false, 1, '#FFD700',
 '{"en":"Gold Seller","hi":"गोल्ड विक्रेता"}',
 true, NOW(), NOW()),

-- ─────────────────────────────────────────────────────────────────────────────
-- DIAMOND  (₹399/mo · unlimited listings · 100 contacts · unlimited mods ·
--           60-day boost · listing+profile highlight · 3 home feed slots)
-- ─────────────────────────────────────────────────────────────────────────────
('DIAMOND_MONTHLY',
 '{"en":"Diamond Monthly","hi":"डायमंड मासिक"}',
 399.00, 1,
 -1, 100, -1, 60,
 '{"en":["Unlimited Active Listings","100 Contact Views / month","Unlimited listing edits","60-day priority visibility","Diamond badge on listings","Business profile highlight","3 home feed slots"],"hi":["असीमित सक्रिय लिस्टिंग","100 संपर्क दृश्य / माह","असीमित लिस्टिंग संपादन","60 दिन प्राथमिकता दृश्यता","लिस्टिंग पर डायमंड बैज","बिज़नेस प्रोफ़ाइल हाइलाइट","3 होम फ़ीड स्लॉट"]}',
 41, true, true, 3, '#00BFFF',
 '{"en":"Diamond Seller","hi":"डायमंड विक्रेता"}',
 true, NOW(), NOW()),

('DIAMOND_QUARTERLY',
 '{"en":"Diamond Quarterly","hi":"डायमंड त्रैमासिक"}',
 1099.00, 3,
 -1, 300, -1, 60,
 '{"en":["Unlimited Active Listings","300 Contact Views / quarter","Unlimited listing edits","60-day priority visibility","Diamond badge on listings","Business profile highlight","3 home feed slots","Save ₹98 vs monthly"],"hi":["असीमित सक्रिय लिस्टिंग","300 संपर्क दृश्य / तिमाही","असीमित लिस्टिंग संपादन","60 दिन प्राथमिकता दृश्यता","लिस्टिंग पर डायमंड बैज","बिज़नेस प्रोफ़ाइल हाइलाइट","3 होम फ़ीड स्लॉट","मासिक से ₹98 बचत"]}',
 42, true, true, 3, '#00BFFF',
 '{"en":"Diamond Seller","hi":"डायमंड विक्रेता"}',
 true, NOW(), NOW()),

('DIAMOND_HALF_YEARLY',
 '{"en":"Diamond Half-Yearly","hi":"डायमंड छमाही"}',
 2099.00, 6,
 -1, 600, -1, 60,
 '{"en":["Unlimited Active Listings","600 Contact Views / 6 months","Unlimited listing edits","60-day priority visibility","Diamond badge on listings","Business profile highlight","3 home feed slots","Save ₹295 vs monthly"],"hi":["असीमित सक्रिय लिस्टिंग","600 संपर्क दृश्य / 6 माह","असीमित लिस्टिंग संपादन","60 दिन प्राथमिकता दृश्यता","लिस्टिंग पर डायमंड बैज","बिज़नेस प्रोफ़ाइल हाइलाइट","3 होम फ़ीड स्लॉट","मासिक से ₹295 बचत"]}',
 43, true, true, 3, '#00BFFF',
 '{"en":"Diamond Seller","hi":"डायमंड विक्रेता"}',
 true, NOW(), NOW()),

('DIAMOND_YEARLY',
 '{"en":"Diamond Yearly","hi":"डायमंड वार्षिक"}',
 3999.00, 12,
 -1, 1200, -1, 60,
 '{"en":["Unlimited Active Listings","1200 Contact Views / year","Unlimited listing edits","60-day priority visibility","Diamond badge on listings","Business profile highlight","3 home feed slots","Save ₹789 vs monthly"],"hi":["असीमित सक्रिय लिस्टिंग","1200 संपर्क दृश्य / वर्ष","असीमित लिस्टिंग संपादन","60 दिन प्राथमिकता दृश्यता","लिस्टिंग पर डायमंड बैज","बिज़नेस प्रोफ़ाइल हाइलाइट","3 होम फ़ीड स्लॉट","मासिक से ₹789 बचत"]}',
 44, true, true, 3, '#00BFFF',
 '{"en":"Diamond Seller","hi":"डायमंड विक्रेता"}',
 true, NOW(), NOW())

ON CONFLICT (plan_key) DO UPDATE SET
    name                         = EXCLUDED.name,
    price                        = EXCLUDED.price,
    duration_months              = EXCLUDED.duration_months,
    max_listings                 = EXCLUDED.max_listings,
    max_contact_views            = EXCLUDED.max_contact_views,
    max_modifications_per_listing = EXCLUDED.max_modifications_per_listing,
    priority_visibility_days     = EXCLUDED.priority_visibility_days,
    features                     = EXCLUDED.features,
    sort_weight                  = EXCLUDED.sort_weight,
    listing_highlight            = EXCLUDED.listing_highlight,
    profile_highlight            = EXCLUDED.profile_highlight,
    home_feed_slots              = EXCLUDED.home_feed_slots,
    highlight_color              = EXCLUDED.highlight_color,
    badge_label                  = EXCLUDED.badge_label,
    is_active                    = EXCLUDED.is_active,
    updated_at                   = NOW();

-- Verify
SELECT plan_key, price, duration_months, max_listings, max_contact_views,
       max_modifications_per_listing, priority_visibility_days, sort_weight,
       listing_highlight, highlight_color
FROM   subscription_plans
ORDER  BY sort_weight;
