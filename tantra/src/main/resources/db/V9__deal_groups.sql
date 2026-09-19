-- V9: Deal groups — admin-configurable category groups for the home screen deals strip.
-- Each group maps to one deal card; multiple categories can be combined under one group.
-- Labels/icons seeded via DealGroupSeeder @PostConstruct (avoids Windows encoding issues).

CREATE TABLE deal_groups (
    id            SERIAL PRIMARY KEY,
    group_key     VARCHAR(40)  NOT NULL,
    label_en      VARCHAR(80)  NOT NULL DEFAULT '',
    label_hi      VARCHAR(80)  NOT NULL DEFAULT '',
    icon          VARCHAR(20)  NOT NULL DEFAULT '',
    unit_en       VARCHAR(20),
    unit_hi       VARCHAR(20),
    display_order INTEGER      NOT NULL DEFAULT 0,
    is_active     BOOLEAN      NOT NULL DEFAULT true,
    CONSTRAINT uq_deal_groups_key UNIQUE (group_key)
);

-- Join table: which module_categories belong to each deal group.
-- One group can span multiple categories (e.g. Poultry + Fishery combined).
CREATE TABLE deal_group_categories (
    deal_group_id INTEGER NOT NULL REFERENCES deal_groups(id) ON DELETE CASCADE,
    category_id   INTEGER NOT NULL REFERENCES module_categories(id) ON DELETE CASCADE,
    CONSTRAINT pk_deal_group_categories PRIMARY KEY (deal_group_id, category_id)
);

CREATE INDEX idx_deal_group_categories_group ON deal_group_categories(deal_group_id);
CREATE INDEX idx_deal_group_categories_cat   ON deal_group_categories(category_id);
