package com.hyperlocal.tantra.modules.forms.model;

/**
 * The set of field widgets a dynamic form can render. Adding a new type here is the only
 * code change ever needed on the form side; new categories/fields are pure configuration.
 */
public enum FieldType {
    TEXT,
    TEXTAREA,
    NUMBER,
    DECIMAL,
    BOOLEAN,
    DROPDOWN,
    MULTISELECT,
    RADIO,
    CHECKBOX_GROUP,
    DATE,
    IMAGE,
    AUTO_CALC,
    ADDRESS
}
