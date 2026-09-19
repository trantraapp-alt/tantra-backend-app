-- ─────────────────────────────────────────────────────────────────────────────
-- V18 : Seed test user + 3 business profiles (different types + statuses)
-- Used for admin review screen testing.
--
-- User     : Ramesh Kumar  |  mobile: 9876543210  |  role: SELLER
-- Password : Test@1234  (bcrypt)
--
-- Profiles :
--   1. Shri Beej Bhandar       – seed_dealer    – APPROVED
--   2. Ramesh Pashu Chikitsa   – vet_clinic     – PENDING
--   3. Kumar Krishi Yantra     – equipment_dealer – REJECTED
-- ─────────────────────────────────────────────────────────────────────────────

-- ── 1. Test user (skip if already registered) ────────────────────────────────
INSERT INTO users (
    user_id, first_name, last_name, mobile_number, password,
    app_usage_role, preferred_language, created_at
)
VALUES (
    'USRTEST00001',
    'Ramesh',
    'Kumar',
    '9876543210',
    '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi',
    'SELLER',
    'HI',
    NOW() - INTERVAL '30 days'
)
ON CONFLICT (mobile_number) DO NOTHING;

-- ── 2. Profile 1 : Seed Dealer — APPROVED ────────────────────────────────────
INSERT INTO business_profiles (
    profile_id, user_id, profile_type, business_name,
    attributes, address,
    latitude, longitude,
    verification_status,
    verified_by, verified_at,
    is_visible, is_active, is_deleted,
    created_at, updated_at
)
VALUES (
    'BPSEED00001',
    'USRTEST00001',
    'seed_dealer',
    'Shri Beej Bhandar',
    '{
        "ownerName":    "Ramesh Kumar",
        "description":  "Certified seed dealer supplying wheat, paddy, maize and vegetable seeds. Government empanelled. 15 years in business.",
        "gstNumber":    "09AABCU9603R1ZM",
        "licenseNo":    "SD/UP/2024/00451",
        "contact":      "9876543210",
        "altContact":   "9012345678",
        "brands":       ["Syngenta", "Bayer", "Mahyco", "IARI"],
        "yearsActive":  15
    }',
    '{
        "fullAddress":   "Plot 12, Mandi Road, Sector 4",
        "country":       "India",
        "state":         "Uttar Pradesh",
        "district":      "Lucknow",
        "city":          "Lucknow",
        "village":       null,
        "pinCode":       "226001",
        "latitude":      26.8467,
        "longitude":     80.9462,
        "mobileNumber":  "9876543210",
        "altMobileNumber": "9012345678"
    }',
    26.8467, 80.9462,
    'APPROVED',
    'USRADMIN00001', NOW() - INTERVAL '20 days',
    true, true, false,
    NOW() - INTERVAL '25 days', NOW() - INTERVAL '20 days'
);

-- ── 3. Profile 2 : Vet Clinic — PENDING ──────────────────────────────────────
INSERT INTO business_profiles (
    profile_id, user_id, profile_type, business_name,
    attributes, address,
    latitude, longitude,
    verification_status,
    is_visible, is_active, is_deleted,
    created_at, updated_at
)
VALUES (
    'BPVET000001',
    'USRTEST00001',
    'vet_clinic',
    'Ramesh Pashu Chikitsa Kendra',
    '{
        "ownerName":      "Dr. Ramesh Kumar",
        "description":    "Veterinary clinic for cattle, poultry and small animals. Vaccination, surgery and routine check-up. Available 9am-8pm.",
        "registrationNo": "VET/UP/LKO/2023/0892",
        "contact":        "9876543210",
        "specializations": ["Cattle", "Poultry", "Goat", "Dog"],
        "emergencyAvailable": true,
        "yearsActive":    8
    }',
    '{
        "fullAddress":   "Near Pashu Mela Ground, Gomti Nagar",
        "country":       "India",
        "state":         "Uttar Pradesh",
        "district":      "Lucknow",
        "city":          "Lucknow",
        "village":       null,
        "pinCode":       "226010",
        "latitude":      26.8553,
        "longitude":     81.0142,
        "mobileNumber":  "9876543210",
        "altMobileNumber": null
    }',
    26.8553, 81.0142,
    'PENDING',
    true, true, false,
    NOW() - INTERVAL '3 days', NOW() - INTERVAL '3 days'
);

-- ── 4. Profile 3 : Equipment Dealer — REJECTED ───────────────────────────────
INSERT INTO business_profiles (
    profile_id, user_id, profile_type, business_name,
    attributes, address,
    latitude, longitude,
    verification_status,
    verified_by, verified_at, reject_reason,
    is_visible, is_active, is_deleted,
    created_at, updated_at
)
VALUES (
    'BPEQP000001',
    'USRTEST00001',
    'equipment_dealer',
    'Kumar Krishi Yantra Bhandar',
    '{
        "ownerName":    "Ramesh Kumar",
        "description":  "Dealer of tractors, threshers, sprayers and irrigation equipment. Repair and service facility on-site.",
        "gstNumber":    "09AABCU9603R1ZZ",
        "contact":      "9876543210",
        "brands":       ["Mahindra", "Sonalika", "AGCO"],
        "serviceAvailable": true,
        "yearsActive":  5
    }',
    '{
        "fullAddress":   "NH-27, Amausi Industrial Area",
        "country":       "India",
        "state":         "Uttar Pradesh",
        "district":      "Lucknow",
        "city":          "Lucknow",
        "village":       null,
        "pinCode":       "226009",
        "latitude":      26.7710,
        "longitude":     80.8820,
        "mobileNumber":  "9876543210",
        "altMobileNumber": null
    }',
    26.7710, 80.8820,
    'REJECTED',
    'USRADMIN00001', NOW() - INTERVAL '10 days',
    'GST number could not be verified. Please resubmit with a valid GST certificate.',
    false, true, false,
    NOW() - INTERVAL '15 days', NOW() - INTERVAL '10 days'
);
