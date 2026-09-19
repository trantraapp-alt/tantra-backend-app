-- ═══════════════════════════════════════════════════════════════════════════
-- V5 — Carousel banner columns + seed 3 home-screen slides
-- ═══════════════════════════════════════════════════════════════════════════

-- 1. New columns for rich carousel design
ALTER TABLE promo_cards
    ADD COLUMN IF NOT EXISTS eyebrow          JSONB,
    ADD COLUMN IF NOT EXISTS cta_bg_color     VARCHAR(20),
    ADD COLUMN IF NOT EXISTS illustration_key VARCHAR(30);

-- 2. Seed the 3 carousel banner slides
--    ON CONFLICT on card_id so re-running the migration is idempotent.

-- Slide 1 — Wheat / Rabi Season (amber dawn)
INSERT INTO promo_cards (
    card_id, card_type,
    eyebrow, title, subtitle,
    bg_color, text_color,
    cta_label, cta_bg_color, cta_type, cta_value,
    illustration_key,
    display_order, is_active,
    created_by, created_at, updated_at
) VALUES (
    'PCWHT000001', 'CAROUSEL',
    '{"en":"Rabi Season 2026","hi":"रबी सीज़न 2026"}',
    '{"en":"Sell Your Wheat Directly to Buyers","hi":"अपना गेहूं सीधे बेचें"}',
    '{"en":"No middlemen. Better prices. Guaranteed.","hi":"बिचौलिया नहीं। बेहतर दाम। गारंटी।"}',
    '#6E2C00', '#FFFFFF',
    '{"en":"Browse Crops →","hi":"फसल देखें →"}', '#F0C040', 'CATEGORY', '4',
    'WHEAT',
    1, true,
    'SYSTEM', NOW(), NOW()
) ON CONFLICT (card_id) DO NOTHING;

-- Slide 2 — Seeds / Kharif (deep forest green)
INSERT INTO promo_cards (
    card_id, card_type,
    eyebrow, title, subtitle,
    bg_color, text_color,
    cta_label, cta_bg_color, cta_type, cta_value,
    illustration_key,
    display_order, is_active,
    created_by, created_at, updated_at
) VALUES (
    'PCSEED000001', 'CAROUSEL',
    '{"en":"Kharif 2026 · Certified Seeds","hi":"खरीफ 2026 · प्रमाणित बीज"}',
    '{"en":"Better Seeds, Better Harvest","hi":"प्रमाणित बीज, बेहतर उपज"}',
    '{"en":"Verified sellers · Guaranteed germination.","hi":"प्रमाणित विक्रेता · अंकुरण की गारंटी।"}',
    '#083A18', '#FFFFFF',
    '{"en":"Shop Seeds →","hi":"बीज खरीदें →"}', '#28D060', 'CATEGORY', '13',
    'SEEDLING',
    2, true,
    'SYSTEM', NOW(), NOW()
) ON CONFLICT (card_id) DO NOTHING;

-- Slide 3 — Farm Equipment (dark dusk / tractor)
INSERT INTO promo_cards (
    card_id, card_type,
    eyebrow, title, subtitle,
    bg_color, text_color,
    cta_label, cta_bg_color, cta_type, cta_value,
    illustration_key,
    display_order, is_active,
    created_by, created_at, updated_at
) VALUES (
    'PCEQP000001', 'CAROUSEL',
    '{"en":"Rental Service · Near You","hi":"किराया सुविधा · आपके पास"}',
    '{"en":"Rent Tractor & Equipment","hi":"ट्रैक्टर-उपकरण किराए पर लें"}',
    '{"en":"Affordable daily rates · Verified owners.","hi":"सस्ती दरें · प्रमाणित मालिक।"}',
    '#2C1448', '#FFFFFF',
    '{"en":"View Equipment →","hi":"उपकरण देखें →"}', '#FF7A20', 'CATEGORY', NULL,
    'TRACTOR',
    3, true,
    'SYSTEM', NOW(), NOW()
) ON CONFLICT (card_id) DO NOTHING;
