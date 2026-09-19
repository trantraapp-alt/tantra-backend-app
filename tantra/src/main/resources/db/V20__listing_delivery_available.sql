-- V20 : Add delivery_available flag to listings
-- Applies only to SELL listings (RENT = delivery concept doesn't apply).
-- Default false — existing listings have no delivery commitment.

ALTER TABLE listings
    ADD COLUMN IF NOT EXISTS delivery_available BOOLEAN NOT NULL DEFAULT FALSE;

COMMENT ON COLUMN listings.delivery_available IS
    'True = seller offers home/farm delivery for this SELL listing. Shown as a badge on the listing card.';

CREATE INDEX IF NOT EXISTS idx_listings_delivery ON listings(delivery_available)
    WHERE delivery_available = true AND is_deleted = false;
