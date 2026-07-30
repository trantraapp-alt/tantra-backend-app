# Business Profile — Functional Flow

The complete picture of how Business Profiles work, for the frontend developer building this feature. This
is the **flow / state / journey** view; for exact request & response bodies see
`BUSINESS_PROFILE_FRONTEND_GUIDE.md`.

---

## 1. What it is & who's involved

A **Business Profile** lets a dealer / shop / vet present a verified business (Seed Dealer, Fertilizer Shop,
Vet Clinic, Nursery…). An **admin reviews** it; once **approved** it shows a **verified badge** and can be
discovered by buyers.

| Actor | Does |
|---|---|
| **Owner** (USER) | Creates, edits, deletes their profiles; sees status + reason; can hold several |
| **Admin** | Reviews the queue; approves / rejects / blocks; sees the Approval Tracker + history |
| **Buyer** (USER) | Sees only APPROVED + visible profiles (badge) — read-only |

---

## 2. The lifecycle (state machine)

```
                         ┌───────── owner edits (content changed) ─────────┐
                         ▼                                                  │
   (owner creates)  ┌─────────┐   admin approve   ┌──────────┐             │
   ───────────────► │ PENDING │ ────────────────► │ APPROVED │ ──┐         │
                    └─────────┘                   └──────────┘   │         │
                       │  ▲                             │        │         │
        admin reject   │  │  owner edits & resubmits    │ admin  │ admin   │
        (fixable)      ▼  │  (REJECTED → PENDING)        │ reject │ block   │
                    ┌──────────┐ ◄──────────────────────┘ (take- │ (take-  │
                    │ REJECTED │                           down)  │  down)  │
                    └──────────┘                                  ▼         │
                       │                                     ┌─────────┐    │
                       └──── owner edits & resubmits ────────│ BLOCKED │◄───┘  admin block
                                     (→ PENDING)             └─────────┘       (from PENDING or APPROVED)
                                                                  │
                                                          (permanent — owner
                                                           CANNOT edit/resubmit)
```

**States**
| Status | Meaning | Public to buyers? | Owner can edit → resubmit? |
|---|---|---|---|
| `PENDING` | Awaiting admin review | No | — (already pending) |
| `APPROVED` | Verified (badge) | Yes, if `isVisible` | Yes → editing sends it back to PENDING |
| `REJECTED` | Fixable issue, admin gave a **Rejected Reason** | No | **Yes** → back to PENDING |
| `BLOCKED` | Permanent take-down (offensive/policy), admin gave a **Blocked Reason** | No | **No** → edit is refused |

**Key transition rules**
- Create → **PENDING**. Edit any non-blocked profile → content changed → **PENDING** (re-verification).
- **Reject** and **Block** both work on a **PENDING _or_ an already-APPROVED** profile (so an approved
  profile can be taken down later). Both **auto-hide** it from buyers (only APPROVED is public).
- **Reject = fixable** (owner resubmits). **Block = permanent** (owner cannot edit; only an admin could
  re-approve).

---

## 3. Owner journey (the dealer / vet)

```
Entry
 ├─ Profile ▸ Business Profile            → list "My Profiles"
 └─ Tap a BUSINESS_PROFILE category        → create flow, profileType prefilled = category.linkKey
    (e.g. Veterinary → vet_clinic)

Create / Edit
 1. Pick Profile Type (business_profile_type dropdown)   ── skipped when prefilled from a category
 2. GET the form for that type  →  render sections/fields (metadata-driven, same renderer as listings)
 3. Fill fields; upload docs/logo (2-step: upload → URLs → into attributes)
 4. Submit  →  status = PENDING  →  response returns { status, reason:null, message }

Manage (My Profiles list — each card shows status + reason)
 • PENDING   → "Under review" chip
 • APPROVED  → verified badge; toggle isVisible to show/hide to buyers
 • REJECTED  → show Rejected Reason + [Edit & Resubmit]  (edit → PENDING)
 • BLOCKED   → show Blocked Reason; [Edit] DISABLED (PUT is refused); show support/contact
 • [Delete] → soft-delete (any status)
```

**Always surface the reason.** Every write response includes `status` + `reason`; every profile object
includes `rejectReason` / `blockReason`. On REJECTED/BLOCKED cards, show that text so the owner knows why.

---

## 4. Admin journey (verification dashboard)

```
Approval Tracker (landing)   GET /admin/business-profiles/stats
   ┌───────────┬───────────┬───────────┬──────────┐
   │ Pending N │ Approved N│ Rejected N│ Blocked N│   + "You: approved/rejected/blocked" (reviewedByMe)
   └─────┬─────┴─────┬─────┴─────┬─────┴─────┬────┘
         │  tap a tile → open that list (status filter, newest action first)
         ▼
   List  GET /admin/business-profiles?status=PENDING            (review queue)
         GET /admin/business-profiles?status=APPROVED&sort=verifiedAt,desc   (drill-down)
         GET /admin/business-profiles/history                   (all reviewed: approved+rejected+blocked)

Review a profile (open a row → see fields, docs, address)
   • [Approve]                    → APPROVED, verified badge, leaves the PENDING queue, owner notified
   • [Reject]  (reason required)  → REJECTED (fixable), owner notified with the reason
   • [Block]   (reason required)  → BLOCKED (permanent), owner notified with the reason
```

- **Approving removes it from the PENDING queue** and moves it into History / the Approved tile.
- On an **approved** profile (opened from Approved list / History), still show **[Reject]** and **[Block]**
  so an offensive approved profile can be taken down. Both require a reason.
- Tracker counts: global per-status + `reviewedByMe` (this admin's own approved/rejected/blocked tally).

---

## 5. Visibility rules (who can read a profile)

```
GET /business-profiles/{profileId}
   • Owner            → always (any status) — so they see their own PENDING/REJECTED/BLOCKED + reason
   • Anyone else      → ONLY if status = APPROVED AND isVisible = true   (else 404-style "not found")
```
So the moment a profile is rejected/blocked/hidden, buyers can no longer open it. Verified + visible
profiles are the only ones that badge the owner's listings.

---

## 6. Notifications (owner is told at every step)

An in-app notification is pushed to the owner on **approve / reject / block**, carrying the reason:
```
GET /notifications , GET /notifications/unread-count , PATCH /notifications/{id}/read
```
- Approve → "Business profile verified".
- Reject → "Business profile rejected" + reason. (Take-down of an approved one → "removed" wording.)
- Block → "Business profile blocked" + reason.

`refType = BUSINESS_PROFILE`, `refId = profileId` → deep-link to that profile card.

---

## 7. End-to-end sequence (happy path + take-down)

```
Owner: create ──► PENDING ──► Admin: approve ──► APPROVED (badge, visible to buyers)
                                                     │
                              (offensive content found later)
                                                     ▼
                              Admin: block (reason) ──► BLOCKED ──► auto-hidden from buyers
                                                     │                 owner notified with reason
                                                     ▼
                              Owner opens profile ──► sees Blocked Reason, edit disabled
```

```
Owner: create ──► PENDING ──► Admin: reject (reason) ──► REJECTED
                                                    │
                              Owner: edit & fix ──► PENDING ──► Admin: approve ──► APPROVED
```

---

## 8. Screens to build (checklist)

- [ ] **My Profiles** list — cards with status chip + reason; actions per status (edit/resubmit/delete)
- [ ] **Create / Edit** — profile-type picker → metadata form → docs upload → submit (→ PENDING)
- [ ] **Profile detail** (owner view + public/buyer view of APPROVED+visible)
- [ ] **Admin: Approval Tracker** — count tiles from `/stats` (+ reviewedByMe), each tile drills into a list
- [ ] **Admin: Queue / Lists / History** — review rows with verifiedBy/verifiedAt/reason
- [ ] **Admin: Review actions** — Approve / Reject(reason) / Block(reason)
- [ ] **Notifications** — badge + deep-link on verification events

---

## Reference
- `BUSINESS_PROFILE_FRONTEND_GUIDE.md` — request/response contract, field flags, uploads, endpoint table.
- `postman/11_BusinessProfile.postman_collection.json` — the whole flow runnable end-to-end.
- `FUNCTIONAL_FLOW.md` — how Business Profile fits into the wider app.
