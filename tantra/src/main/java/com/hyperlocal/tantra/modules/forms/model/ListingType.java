package com.hyperlocal.tantra.modules.forms.model;

/**
 * Whether a form/listing is for selling, renting, or both. A category can have a
 * separate form definition per listing type (e.g. Equipment has SELL and RENT variants).
 */
public enum ListingType {
    SELL,
    RENT,
    BOTH
}
