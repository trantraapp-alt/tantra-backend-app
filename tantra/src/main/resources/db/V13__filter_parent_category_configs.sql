-- V13: Filter configs for parent / top-level categories
-- When user is browsing at module level (agri_marketplace, animal_marketplace)
-- or has multiple sub-categories selected, these wider price ranges apply.
-- The service picks the widest (max of maxes) when merging multiple categories.

INSERT INTO filter_configs (scope, scope_key, filter_key, label_en, label_hi, filter_type, config, display_order) VALUES

-- agri_marketplace: widest range across all agri sub-categories (equipment is highest at ₹50L)
('CATEGORY', 'agri_marketplace', 'priceRange',
 'Price Range', 'मूल्य सीमा', 'RANGE',
 '{"min": 0, "max": 5000000, "step": 5000, "displayMin": "₹0", "displayMax": "₹50L+"}',
 2),

-- agri_services: service fee range
('CATEGORY', 'agri_services', 'priceRange',
 'Price Range', 'मूल्य सीमा', 'RANGE',
 '{"min": 0, "max": 100000, "step": 500, "displayMin": "₹0", "displayMax": "₹1L+"}',
 2),

-- repair: repair/maintenance charges
('CATEGORY', 'repair', 'priceRange',
 'Price Range', 'मूल्य सीमा', 'RANGE',
 '{"min": 0, "max": 50000, "step": 100, "displayMin": "₹0", "displayMax": "₹50K+"}',
 2),

-- animal_marketplace: widest range across all animal sub-categories
('CATEGORY', 'animal_marketplace', 'priceRange',
 'Price Range', 'मूल्य सीमा', 'RANGE',
 '{"min": 0, "max": 500000, "step": 1000, "displayMin": "₹0", "displayMax": "₹5L+"}',
 2),

-- veterinary: consultation / treatment fee range
('CATEGORY', 'veterinary', 'priceRange',
 'Price Range', 'मूल्य सीमा', 'RANGE',
 '{"min": 0, "max": 20000, "step": 100, "displayMin": "₹0", "displayMax": "₹20K+"}',
 2);
