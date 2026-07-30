package com.hyperlocal.tantra.modules.business.model;

/** Verification lifecycle of a business profile — admin-driven (manual now, KYC-API later). */
public enum VerificationStatus {
    PENDING,
    APPROVED,
    /** Rejected on review — the owner can edit &amp; resubmit (→ PENDING). */
    REJECTED,
    /** Permanently blocked by an admin (offensive/policy violation) — the owner cannot edit or resubmit. */
    BLOCKED
}
