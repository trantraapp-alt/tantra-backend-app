-- Re-seed stat_tiles with ASCII-safe values.
-- ON CONFLICT DO UPDATE ensures this is idempotent and overwrites any bad rows from V7.

INSERT INTO stat_tiles (stat_key, label_en, label_hi, icon, display_order, is_active) VALUES
('ACTIVE_LISTINGS',  'Active Listings',  'Active Listings',  '#',  1, true),
('DISTRICTS',        'Districts',        'Districts',        '@',  2, true),
('VERIFIED_SELLERS', 'Verified Sellers', 'Verified Sellers', '+',  3, true),
('TODAY_NEW',        'New Today',        'New Today',        '!',  4, true)
ON CONFLICT (stat_key) DO UPDATE SET
    label_en      = EXCLUDED.label_en,
    label_hi      = EXCLUDED.label_hi,
    icon          = EXCLUDED.icon,
    display_order = EXCLUDED.display_order,
    is_active     = EXCLUDED.is_active;
