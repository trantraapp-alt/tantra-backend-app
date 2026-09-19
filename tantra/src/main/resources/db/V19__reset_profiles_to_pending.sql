-- V19 : Reset all 3 test business profiles to PENDING
-- Clears verified_by, verified_at, reject_reason so they appear in the admin queue cleanly.

UPDATE business_profiles
SET
    verification_status = 'PENDING',
    verified_by         = NULL,
    verified_at         = NULL,
    reject_reason       = NULL,
    block_reason        = NULL,
    is_visible          = true,
    updated_at          = NOW()
WHERE profile_id IN ('BPSEED00001', 'BPVET000001', 'BPEQP000001');
