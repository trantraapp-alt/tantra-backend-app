-- V22: Add deliveryAvailable field to all SELL listing form definitions.
-- Uses JSONB array append (||) so it works on any number of SELL forms across all categories.
-- Idempotent: the WHERE clause skips forms that already have the field.

UPDATE form_definitions
SET fields = fields || '[
  {
    "fieldKey"      : "deliveryAvailable",
    "type"          : "BOOLEAN",
    "label"         : { "en": "Delivery Available", "hi": "डिलीवरी उपलब्ध है" },
    "placeholder"   : null,
    "help"          : {
      "en": "Check if you are willing to deliver the item to the buyer''s location",
      "hi": "यदि आप खरीदार के स्थान पर सामान पहुंचा सकते हैं तो यह चुनें"
    },
    "required"      : false,
    "common"        : true,
    "inlineEditable": true,
    "editableOnUpdate": true,
    "readOnly"      : false,
    "displayOrder"  : 999,
    "allowOther"    : false,
    "multiple"      : false,
    "visibleWhen"   : null
  }
]'::jsonb
WHERE listing_type = 'SELL'
  AND form_type    = 'LISTING'
  AND NOT (fields @> '[{"fieldKey":"deliveryAvailable"}]');
