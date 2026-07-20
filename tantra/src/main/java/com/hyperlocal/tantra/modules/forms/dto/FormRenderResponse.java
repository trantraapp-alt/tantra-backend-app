package com.hyperlocal.tantra.modules.forms.dto;

import com.hyperlocal.tantra.modules.forms.model.FieldType;
import com.hyperlocal.tantra.modules.forms.model.FormField;
import com.hyperlocal.tantra.modules.forms.model.ListingType;
import com.hyperlocal.tantra.modules.forms.model.LocalizedText;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * The app-facing contract the frontend consumes to render a category form in one call.
 * Dropdown options are resolved inline (in every configured language) so no extra round-trip
 * is needed except for cascading lookups.
 */
@Data
public class FormRenderResponse {

    private Integer formId;
    private Integer version;
    private ListingType listingType;
    private Integer categoryId;
    private LocalizedText title;
    private List<Section> sections;

    @Data
    public static class Section {
        private String key;
        private LocalizedText title;
        private List<RenderField> fields;
    }

    @Data
    public static class RenderField {
        private String fieldKey;
        private FieldType type;
        private LocalizedText label;
        private boolean required;
        private boolean readOnly;
        private Integer fieldLength;
        private LocalizedText placeholder;
        private LocalizedText help;
        private Integer displayOrder;
        private boolean allowOther;
        private boolean multiple;
        private boolean common;
        private String optionSetKey;
        private List<Option> options;
        private Map<String, Object> validation;
        private Map<String, Object> computed;
        private FormField.VisibleWhen visibleWhen;
    }

    @Data
    public static class Option {
        private String value;
        private LocalizedText label;
        /** id of the parent option item, for cascading dropdowns (null for top-level values). */
        private Integer parent;
    }
}
