package com.hyperlocal.tantra.modules.forms.model;

import lombok.Data;

import java.util.Map;

/**
 * One field inside a form definition. Stored inside {@code form_definitions.fields} (jsonb),
 * so adding/removing a field is a data edit — no entity or migration.
 */
@Data
public class FormField {

    /** Machine key used in the submission payload, e.g. "cropName". */
    private String fieldKey;

    private FieldType type;

    private LocalizedText label;

    /** Optional grouping so the render endpoint can bucket fields into sections. */
    private String sectionKey;
    private LocalizedText sectionTitle;

    /** Whether the field is mandatory (isMandatory). */
    private Boolean required = false;

    /** When true the field is shown but not editable — e.g. auto-calculated values. */
    private Boolean readOnly = false;

    /** Max character length for text/textarea inputs (also mirrored in validation.maxLength). */
    private Integer fieldLength;

    /** false = locked when editing a listing (core/identity field); true = editable. Default true. */
    private Boolean editableOnUpdate = true;

    /** true = editable directly in the My-Listings grid without opening the full form. Default false. */
    private Boolean inlineEditable = false;

    private Integer displayOrder = 0;

    private LocalizedText placeholder;
    private LocalizedText help;

    /** For DROPDOWN/RADIO/MULTISELECT/CHECKBOX_GROUP — the reusable option set to pull values from. */
    private String optionSetKey;

    /**
     * For cascading dropdowns — the {@code fieldKey} of the field whose selected value filters this
     * field's options (e.g. cropName has {@code parentField="cropType"}). The frontend shows only the
     * options whose {@code parent} equals the id of the parent field's selected option.
     */
    private String parentField;

    /** When true, the frontend shows a "Please Specify" text box if the user picks the "Other" value. */
    private Boolean allowOther = false;

    /** For IMAGE and multi-select fields. */
    private Boolean multiple = false;

    /** When true the backend persists this into a typed Listing column instead of the attributes jsonb. */
    private Boolean common = false;

    /** Free-form rules the frontend and backend both honour: min, max, maxLength, regex, acceptedTypes... */
    private Map<String, Object> validation;

    /** For AUTO_CALC fields: {"formula": "...", "readOnly": true}. */
    private Map<String, Object> computed;

    /** Conditional visibility, e.g. show only when availableFor == RENT. */
    private VisibleWhen visibleWhen;

    @Data
    public static class VisibleWhen {
        private String field;
        /** equals | notEquals | in | notIn */
        private String operator;
        private Object value;
    }
}
