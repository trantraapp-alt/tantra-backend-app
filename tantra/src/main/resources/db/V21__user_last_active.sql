-- V21 : Track when a user was last active in the app
-- Updated automatically on every authenticated request (15-min cooldown via cache).

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS last_active_at TIMESTAMP;

COMMENT ON COLUMN users.last_active_at IS
    'Timestamp of user''s most recent authenticated API call. Updated with a 15-min cooldown '
    'to avoid per-request DB writes. Used to show "Active X ago" on seller profiles.';

-- Index for admin "recently active" queries
CREATE INDEX IF NOT EXISTS idx_users_last_active ON users(last_active_at DESC NULLS LAST)
    WHERE last_active_at IS NOT NULL;
