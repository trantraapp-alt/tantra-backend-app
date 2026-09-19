-- ============================================================
-- V2: Subscription Plan Redesign — Duration-based plans
-- Run after V1 and after the application has started once
-- (Hibernate ddl-auto=update creates new entity columns first)
-- ============================================================

-- ──────────────────────────────────────────────────────────────
-- 1. Set defaults on old pricing columns so they don't reject
--    INSERTs from the new entity that no longer maps them.
--    (ddl-auto=update does not add DEFAULT to existing columns)
-- ──────────────────────────────────────────────────────────────
ALTER TABLE subscription_plans
    ALTER COLUMN price_monthly     SET DEFAULT 0.00,
    ALTER COLUMN price_yearly      SET DEFAULT 0.00,
    ALTER COLUMN home_feed_slots   SET DEFAULT 0,
    ALTER COLUMN listing_highlight SET DEFAULT false,
    ALTER COLUMN profile_highlight SET DEFAULT false;

-- ──────────────────────────────────────────────────────────────
-- 2. Add new columns if Hibernate has not created them yet
--    (these are already handled by ddl-auto=update — this is a
--     safety net in case the SQL is run before app startup)
-- ──────────────────────────────────────────────────────────────
ALTER TABLE subscription_plans
    ADD COLUMN IF NOT EXISTS price           DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    ADD COLUMN IF NOT EXISTS duration_months INTEGER       NOT NULL DEFAULT 1,
    ADD COLUMN IF NOT EXISTS features        JSONB;

-- ──────────────────────────────────────────────────────────────
-- 3. Deactivate old tier-based plans
-- ──────────────────────────────────────────────────────────────
UPDATE subscription_plans
SET    is_active = false,
       updated_at = NOW()
WHERE  plan_key IN ('FREE', 'BASIC', 'STANDARD', 'PREMIUM', 'ENTERPRISE');

-- ──────────────────────────────────────────────────────────────
-- 4. Seed new duration-based plans
-- ──────────────────────────────────────────────────────────────
INSERT INTO subscription_plans
    (plan_key, name, price, duration_months, max_listings, features, sort_weight, is_active)
VALUES

    ('FREE_TRIAL',
     '{"en":"Free Trial","hi":"निःशुल्क परीक्षण"}',
     0.00, 1, 25,
     NULL,
     10, true),

    ('QUARTERLY',
     '{"en":"Quarterly","hi":"त्रैमासिक"}',
     3249.00, 3, 25,
     NULL,
     20, true),

    ('HALF_YEARLY',
     '{"en":"Half Yearly","hi":"अर्धवार्षिक"}',
     6499.00, 6, 50,
     NULL,
     30, true)

ON CONFLICT (plan_key) DO UPDATE SET
    name            = EXCLUDED.name,
    price           = EXCLUDED.price,
    duration_months = EXCLUDED.duration_months,
    max_listings    = EXCLUDED.max_listings,
    features        = EXCLUDED.features,
    sort_weight     = EXCLUDED.sort_weight,
    is_active       = EXCLUDED.is_active,
    updated_at      = NOW();
