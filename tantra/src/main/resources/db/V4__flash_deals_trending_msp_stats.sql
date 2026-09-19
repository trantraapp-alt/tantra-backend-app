-- ═══════════════════════════════════════════════════════════════════════════
-- V4 — Flash Deals, Trending Searches, MSP Prices, PromoCard type fields
-- ═══════════════════════════════════════════════════════════════════════════

-- 1. listings: add flash_deal flag + discount expiry
ALTER TABLE listings
    ADD COLUMN IF NOT EXISTS flash_deal         BOOLEAN       NOT NULL DEFAULT false,
    ADD COLUMN IF NOT EXISTS discount_expires_at TIMESTAMPTZ;

-- Index for the flash-deals query (flash_deal=true, not expired)
CREATE INDEX IF NOT EXISTS idx_listings_flash_deal
    ON listings (flash_deal, discount_expires_at)
    WHERE flash_deal = true;

-- 2. promo_cards: add card_type for CAROUSEL / MINI / SCHEME differentiation
ALTER TABLE promo_cards
    ADD COLUMN IF NOT EXISTS card_type VARCHAR(20) NOT NULL DEFAULT 'CAROUSEL';

-- 3. search_queries: new table for trending search tracking
CREATE TABLE IF NOT EXISTS search_queries (
    id           BIGSERIAL     PRIMARY KEY,
    query_term   VARCHAR(200)  NOT NULL,
    user_id      VARCHAR(20),
    result_count INTEGER,
    searched_at  TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_sq_term_time ON search_queries (query_term, searched_at);
CREATE INDEX IF NOT EXISTS idx_sq_user      ON search_queries (user_id)
    WHERE user_id IS NOT NULL;

-- Auto-delete search history older than 90 days (keep table lean)
-- (schedule via a cron job or pg_cron; this comment serves as a reminder)

-- 4. msp_prices: government minimum support price table
CREATE TABLE IF NOT EXISTS msp_prices (
    id                    BIGSERIAL     PRIMARY KEY,
    crop_key              VARCHAR(50)   NOT NULL,
    crop_name             JSONB         NOT NULL,   -- {"en":"Wheat","hi":"गेहूं"}
    emoji                 VARCHAR(10),
    season                VARCHAR(20)   NOT NULL,   -- RABI / KHARIF / COMMERCIAL
    year                  INTEGER       NOT NULL,
    price_per_quintal     NUMERIC(10,2) NOT NULL,
    prev_price_per_quintal NUMERIC(10,2),
    notified_on           DATE,
    is_active             BOOLEAN       NOT NULL DEFAULT true
);

CREATE INDEX IF NOT EXISTS idx_msp_season_year ON msp_prices (season, year);
CREATE INDEX IF NOT EXISTS idx_msp_crop        ON msp_prices (crop_key);

-- 5. Seed MSP 2025-26 data (announced June 2025)
INSERT INTO msp_prices (crop_key, crop_name, emoji, season, year, price_per_quintal, prev_price_per_quintal, notified_on, is_active)
VALUES
  ('wheat',       '{"en":"Wheat","hi":"गेहूं"}',            '🌾', 'RABI',       2026,  2275.00,  2275.00, '2025-10-23', true),
  ('mustard',     '{"en":"Mustard","hi":"सरसों"}',          '🌿', 'RABI',       2026,  5950.00,  5650.00, '2025-10-23', true),
  ('gram',        '{"en":"Gram (Chana)","hi":"चना"}',       '🫘', 'RABI',       2026,  5650.00,  5440.00, '2025-10-23', true),
  ('lentil',      '{"en":"Lentil (Masur)","hi":"मसूर"}',   '🫘', 'RABI',       2026,  6700.00,  6425.00, '2025-10-23', true),
  ('paddy',       '{"en":"Paddy","hi":"धान"}',              '🌾', 'KHARIF',     2026,  2300.00,  2183.00, '2025-06-18', true),
  ('jowar',       '{"en":"Jowar","hi":"ज्वार"}',            '🌾', 'KHARIF',     2026,  3371.00,  3180.00, '2025-06-18', true),
  ('bajra',       '{"en":"Bajra","hi":"बाजरा"}',            '🌾', 'KHARIF',     2026,  2625.00,  2500.00, '2025-06-18', true),
  ('maize',       '{"en":"Maize (Corn)","hi":"मक्का"}',    '🌽', 'KHARIF',     2026,  2225.00,  2090.00, '2025-06-18', true),
  ('tur_dal',     '{"en":"Tur Dal (Arhar)","hi":"तुअर दाल"}','🫘','KHARIF',    2026,  7550.00,  7000.00, '2025-06-18', true),
  ('moong',       '{"en":"Moong","hi":"मूंग"}',             '🫘', 'KHARIF',     2026,  8682.00,  8558.00, '2025-06-18', true),
  ('urad',        '{"en":"Urad","hi":"उड़द"}',              '🫘', 'KHARIF',     2026,  7400.00,  7400.00, '2025-06-18', true),
  ('groundnut',   '{"en":"Groundnut","hi":"मूंगफली"}',     '🥜', 'KHARIF',     2026,  6783.00,  6377.00, '2025-06-18', true),
  ('soybean',     '{"en":"Soybean","hi":"सोयाबीन"}',       '🫘', 'KHARIF',     2026,  4892.00,  4600.00, '2025-06-18', true),
  ('cotton_long', '{"en":"Cotton (Long Staple)","hi":"कपास (लंबा)"}','🌿','KHARIF',2026,7521.00, 7020.00,'2025-06-18', true),
  ('cotton_med',  '{"en":"Cotton (Med Staple)","hi":"कपास (मध्यम)"}','🌿','KHARIF',2026,7121.00, 6620.00,'2025-06-18', true),
  ('sugarcane',   '{"en":"Sugarcane","hi":"गन्ना"}',        '🌾', 'COMMERCIAL', 2026,  340.00,   315.00,  '2025-09-01', true)
ON CONFLICT DO NOTHING;
