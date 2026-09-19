-- ─────────────────────────────────────────────────────────────────────────────
-- V3: Rename seller_subscriptions → user_subscriptions
--     Subscription plans now apply to both buyers and sellers (unified flow).
-- ─────────────────────────────────────────────────────────────────────────────

-- Rename table
ALTER TABLE seller_subscriptions RENAME TO user_subscriptions;

-- Rename indexes to match new naming convention (idx_usub_*)
ALTER INDEX IF EXISTS idx_sub_user        RENAME TO idx_usub_user;
ALTER INDEX IF EXISTS idx_sub_user_active RENAME TO idx_usub_user_active;
ALTER INDEX IF EXISTS idx_sub_plan        RENAME TO idx_usub_plan;
ALTER INDEX IF EXISTS idx_sub_expires     RENAME TO idx_usub_expires;
