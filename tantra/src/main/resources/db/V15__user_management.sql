-- ─────────────────────────────────────────────────────────────────
-- V15 : user_management — admin block/unblock + activity tracking
-- ─────────────────────────────────────────────────────────────────

ALTER TABLE users ADD COLUMN IF NOT EXISTS is_blocked     BOOLEAN     NOT NULL DEFAULT FALSE;
ALTER TABLE users ADD COLUMN IF NOT EXISTS blocked_at     TIMESTAMP;
ALTER TABLE users ADD COLUMN IF NOT EXISTS blocked_reason VARCHAR(200);
ALTER TABLE users ADD COLUMN IF NOT EXISTS last_login_at  TIMESTAMP;

CREATE INDEX IF NOT EXISTS idx_users_is_blocked  ON users(is_blocked);
CREATE INDEX IF NOT EXISTS idx_users_created_at  ON users(created_at);
