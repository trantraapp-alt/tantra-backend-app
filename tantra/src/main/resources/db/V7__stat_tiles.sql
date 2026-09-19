-- Configurable stats ribbon tiles for the home screen trust bar.
-- Admin can change label, icon, order, or toggle active without a code deploy.
-- Backend computes the actual value based on stat_key at runtime.

CREATE TABLE stat_tiles (
    id            SERIAL PRIMARY KEY,
    stat_key      VARCHAR(40)  NOT NULL UNIQUE,
    label_en      VARCHAR(60)  NOT NULL,
    label_hi      VARCHAR(60)  NOT NULL,
    icon          VARCHAR(10)  NOT NULL,
    display_order INT          NOT NULL DEFAULT 0,
    is_active     BOOLEAN      NOT NULL DEFAULT TRUE
);

INSERT INTO stat_tiles (stat_key, label_en, label_hi, icon, display_order) VALUES
('ACTIVE_LISTINGS',  'Active Listings',   'सक्रिय लिस्टिंग',    '📋', 1),
('DISTRICTS',        'Districts',         'जिले',                '📍', 2),
('VERIFIED_SELLERS', 'Verified Sellers',  'सत्यापित विक्रेता',  '✅', 3),
('TODAY_NEW',        'New Today',         'आज नई',              '🆕', 4);
