-- V16: Listing view tracking — 1 logged-in user counts as 1 view per listing (no IP, no anonymous)
-- Unique constraint on (listing_id, viewer_user_id) handles dedup at DB level via ON CONFLICT DO NOTHING

CREATE TABLE IF NOT EXISTS listing_views (
    id              BIGSERIAL   PRIMARY KEY,
    listing_id      VARCHAR(20) NOT NULL,
    viewer_user_id  VARCHAR(20) NOT NULL,
    viewed_at       TIMESTAMP   NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_listing_user_view UNIQUE (listing_id, viewer_user_id)
);

CREATE INDEX IF NOT EXISTS idx_listing_views_listing ON listing_views(listing_id);
CREATE INDEX IF NOT EXISTS idx_listing_views_user    ON listing_views(viewer_user_id);

-- view_count column already exists on listings table (added by JPA ddl-auto).
-- If applying on a fresh DB, ensure the column is present:
ALTER TABLE listings ADD COLUMN IF NOT EXISTS view_count BIGINT NOT NULL DEFAULT 0;
