-- ============================================================
-- Tantra DB Migration: Home Feed + Search + Subscription
-- Run once against tantra_db after application startup
-- (JPA DDL=update will create the new tables/columns first)
-- ============================================================

-- ──────────────────────────────────────────────────────────────
-- 1. GIN index for full-text search on listings.search_vector
-- ──────────────────────────────────────────────────────────────
CREATE INDEX IF NOT EXISTS idx_listings_fts
    ON listings USING GIN(search_vector);

-- ──────────────────────────────────────────────────────────────
-- 2. Subscription Plans seed data
-- ──────────────────────────────────────────────────────────────
INSERT INTO subscription_plans
    (plan_key, name, price_monthly, price_yearly,
     max_listings, home_feed_slots, sort_weight,
     badge_label, highlight_color,
     listing_highlight, profile_highlight, is_active)
VALUES
    ('FREE',
     '{"en":"Free","hi":"निःशुल्क"}',
     0.00, 0.00,
     5, 0, 99,
     NULL, NULL,
     false, false, true),

    ('BASIC',
     '{"en":"Basic Seller","hi":"बेसिक विक्रेता"}',
     99.00, 999.00,
     20, 0, 70,
     '{"en":"Basic Seller","hi":"बेसिक विक्रेता"}',
     '#90CAF9',
     true, false, true),

    ('STANDARD',
     '{"en":"Standard Seller","hi":"स्टैंडर्ड विक्रेता"}',
     299.00, 2999.00,
     50, 2, 40,
     '{"en":"Verified Seller","hi":"सत्यापित विक्रेता"}',
     '#66BB6A',
     true, true, true),

    ('PREMIUM',
     '{"en":"Premium Seller","hi":"प्रीमियम विक्रेता"}',
     599.00, 5999.00,
     -1, 4, 20,
     '{"en":"Premium Seller","hi":"प्रीमियम विक्रेता"}',
     '#FFA726',
     true, true, true),

    ('ENTERPRISE',
     '{"en":"Enterprise Seller","hi":"एंटरप्राइज विक्रेता"}',
     1499.00, 14999.00,
     -1, 6, 5,
     '{"en":"Top Seller","hi":"टॉप विक्रेता"}',
     '#FFD700',
     true, true, true)
ON CONFLICT (plan_key) DO UPDATE SET
    name              = EXCLUDED.name,
    price_monthly     = EXCLUDED.price_monthly,
    price_yearly      = EXCLUDED.price_yearly,
    max_listings      = EXCLUDED.max_listings,
    home_feed_slots   = EXCLUDED.home_feed_slots,
    sort_weight       = EXCLUDED.sort_weight,
    badge_label       = EXCLUDED.badge_label,
    highlight_color   = EXCLUDED.highlight_color,
    listing_highlight = EXCLUDED.listing_highlight,
    profile_highlight = EXCLUDED.profile_highlight,
    is_active         = EXCLUDED.is_active,
    updated_at        = NOW();

-- ──────────────────────────────────────────────────────────────
-- 3. Backfill search_vector for existing listings
--    (run once; new listings are handled by ListingService)
-- ──────────────────────────────────────────────────────────────
UPDATE listings SET
    search_vector = to_tsvector('simple',
        coalesce(address->>'district', '') || ' ' ||
        coalesce(address->>'state', '')    || ' ' ||
        coalesce(address->>'village', '')  || ' ' ||
        coalesce(address->>'city', ''))
WHERE search_vector IS NULL
  AND is_deleted = false;

-- ──────────────────────────────────────────────────────────────
-- 4. Backfill lat/lng from address jsonb for existing listings
-- ──────────────────────────────────────────────────────────────
UPDATE listings SET
    latitude  = (address->>'latitude')::DECIMAL,
    longitude = (address->>'longitude')::DECIMAL
WHERE latitude IS NULL
  AND address->>'latitude' IS NOT NULL
  AND is_deleted = false;

-- ──────────────────────────────────────────────────────────────
-- 5. Backfill lat/lng for existing business_profiles
-- ──────────────────────────────────────────────────────────────
UPDATE business_profiles SET
    latitude  = (address->>'latitude')::DECIMAL,
    longitude = (address->>'longitude')::DECIMAL
WHERE latitude IS NULL
  AND address->>'latitude' IS NOT NULL
  AND is_deleted = false;

-- ──────────────────────────────────────────────────────────────
-- 6. Additional performance indexes (idempotent)
-- ──────────────────────────────────────────────────────────────
CREATE INDEX IF NOT EXISTS idx_listings_geo
    ON listings (latitude, longitude)
    WHERE is_active = true AND is_deleted = false;

CREATE INDEX IF NOT EXISTS idx_listings_category_geo
    ON listings (category_id, latitude, longitude)
    WHERE is_active = true AND is_deleted = false;

CREATE INDEX IF NOT EXISTS idx_listings_module_geo
    ON listings (module_id, latitude, longitude)
    WHERE is_active = true AND is_deleted = false;

CREATE INDEX IF NOT EXISTS idx_listings_price
    ON listings (offered_price)
    WHERE is_active = true AND is_deleted = false;

CREATE INDEX IF NOT EXISTS idx_sub_user_active
    ON seller_subscriptions (user_id, status, expires_at);

CREATE INDEX IF NOT EXISTS idx_cr_listing_buyer
    ON contact_reveals (listing_id, buyer_user_id, created_at);

CREATE INDEX IF NOT EXISTS idx_bp_directory
    ON business_profiles (verification_status, is_active, is_deleted)
    WHERE is_deleted = false;
