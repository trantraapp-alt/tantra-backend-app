-- ═══════════════════════════════════════════════════════════════════════════
-- V6 -- DB-driven carousel items for the home screen category buttons
-- ═══════════════════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS carousel_items (
    id             SERIAL PRIMARY KEY,
    label          JSONB        NOT NULL,
    icon_url       VARCHAR(500),
    cta_type       VARCHAR(20)  NOT NULL DEFAULT 'BROWSE_CATEGORY',
    cta_value      VARCHAR(255),
    listing_type   VARCHAR(10),
    bg_color       VARCHAR(10)  NOT NULL DEFAULT '#FFFFFF',
    text_color     VARCHAR(10)  NOT NULL DEFAULT '#000000',
    display_order  INTEGER      NOT NULL DEFAULT 0,
    is_active      BOOLEAN      NOT NULL DEFAULT TRUE,
    updated_by     VARCHAR(20),
    created_at     TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_carousel_active_order ON carousel_items (is_active, display_order);

-- ─── Seed: initial 7 carousel buttons ────────────────────────────────────────
-- Re-run safe: ON CONFLICT on id (use INSERT ... WHERE NOT EXISTS pattern
-- via a check on label text to stay idempotent across fresh DBs).

INSERT INTO carousel_items (label, cta_type, cta_value, listing_type, bg_color, text_color, display_order, is_active, updated_by)
SELECT '{"en":"Browse Crops","hi":"फसल देखें"}', 'BROWSE_CATEGORY', 'crop', NULL, '#E8F5E9', '#1B5E20', 1, TRUE, 'SYSTEM'
WHERE NOT EXISTS (SELECT 1 FROM carousel_items WHERE cta_value = 'crop' AND listing_type IS NULL);

INSERT INTO carousel_items (label, cta_type, cta_value, listing_type, bg_color, text_color, display_order, is_active, updated_by)
SELECT '{"en":"Browse Seeds","hi":"बीज देखें"}', 'BROWSE_CATEGORY', 'seed', NULL, '#FFF8E1', '#F57F17', 2, TRUE, 'SYSTEM'
WHERE NOT EXISTS (SELECT 1 FROM carousel_items WHERE cta_value = 'seed' AND listing_type IS NULL);

INSERT INTO carousel_items (label, cta_type, cta_value, listing_type, bg_color, text_color, display_order, is_active, updated_by)
SELECT '{"en":"Browse Fertilizers","hi":"खाद देखें"}', 'BROWSE_CATEGORY', 'fertilizer', NULL, '#E3F2FD', '#0D47A1', 3, TRUE, 'SYSTEM'
WHERE NOT EXISTS (SELECT 1 FROM carousel_items WHERE cta_value = 'fertilizer' AND listing_type IS NULL);

INSERT INTO carousel_items (label, cta_type, cta_value, listing_type, bg_color, text_color, display_order, is_active, updated_by)
SELECT '{"en":"Rent Equipment","hi":"उपकरण किराए पर"}', 'BROWSE_CATEGORY', 'equipment', 'RENT', '#F3E5F5', '#4A148C', 4, TRUE, 'SYSTEM'
WHERE NOT EXISTS (SELECT 1 FROM carousel_items WHERE cta_value = 'equipment' AND listing_type = 'RENT');

INSERT INTO carousel_items (label, cta_type, cta_value, listing_type, bg_color, text_color, display_order, is_active, updated_by)
SELECT '{"en":"Buy Equipment","hi":"उपकरण खरीदें"}', 'BROWSE_CATEGORY', 'equipment', 'SELL', '#FBE9E7', '#BF360C', 5, TRUE, 'SYSTEM'
WHERE NOT EXISTS (SELECT 1 FROM carousel_items WHERE cta_value = 'equipment' AND listing_type = 'SELL');

INSERT INTO carousel_items (label, cta_type, cta_value, listing_type, bg_color, text_color, display_order, is_active, updated_by)
SELECT '{"en":"Browse Animals","hi":"पशु देखें"}', 'BROWSE_CATEGORY', 'animal', NULL, '#FCE4EC', '#880E4F', 6, TRUE, 'SYSTEM'
WHERE NOT EXISTS (SELECT 1 FROM carousel_items WHERE cta_value = 'animal' AND listing_type IS NULL);

INSERT INTO carousel_items (label, cta_type, cta_value, listing_type, bg_color, text_color, display_order, is_active, updated_by)
SELECT '{"en":"Agri Repair","hi":"कृषि मरम्मत"}', 'BROWSE_CATEGORY', 'repair', NULL, '#E8EAF6', '#1A237E', 7, TRUE, 'SYSTEM'
WHERE NOT EXISTS (SELECT 1 FROM carousel_items WHERE cta_value = 'repair' AND listing_type IS NULL);
