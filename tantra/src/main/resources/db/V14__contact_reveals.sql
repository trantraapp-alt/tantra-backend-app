-- ─────────────────────────────────────────────────────────────────
-- V14 : contact_reveals — OLX-style contact reveal tracking
--
-- Tracks every buyer→listing contact reveal event.
-- One row per buyer+listing per 24-hour window (dedup handled in service).
-- Used for: seller analytics, platform analytics, future rate-limiting.
-- ─────────────────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS contact_reveals (
    id              BIGSERIAL       PRIMARY KEY,
    listing_id      VARCHAR(20)     NOT NULL,
    seller_user_id  VARCHAR(20)     NOT NULL,
    buyer_user_id   VARCHAR(20)     NOT NULL,
    contact_number  VARCHAR(15),
    ip_address      VARCHAR(45),
    created_at      TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_cr_listing FOREIGN KEY (listing_id)
        REFERENCES listings(listing_id) ON DELETE CASCADE,
    CONSTRAINT fk_cr_seller  FOREIGN KEY (seller_user_id)
        REFERENCES users(user_id)       ON DELETE CASCADE,
    CONSTRAINT fk_cr_buyer   FOREIGN KEY (buyer_user_id)
        REFERENCES users(user_id)       ON DELETE CASCADE
);

-- Indexes for common query patterns
CREATE INDEX IF NOT EXISTS idx_cr_listing      ON contact_reveals(listing_id);
CREATE INDEX IF NOT EXISTS idx_cr_buyer        ON contact_reveals(buyer_user_id);
CREATE INDEX IF NOT EXISTS idx_cr_seller       ON contact_reveals(seller_user_id);
CREATE INDEX IF NOT EXISTS idx_cr_listing_buyer ON contact_reveals(listing_id, buyer_user_id, created_at);
