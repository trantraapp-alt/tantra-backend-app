-- V17: Quality-assured descriptions per category.
-- Admin sets these once; they appear on every listing detail page for that category.

ALTER TABLE module_categories
    ADD COLUMN IF NOT EXISTS quality_desc_en TEXT,
    ADD COLUMN IF NOT EXISTS quality_desc_hi TEXT;
