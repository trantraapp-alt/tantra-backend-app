package com.hyperlocal.tantra.modules.forms.model;

import java.util.HashMap;

/**
 * A language-keyed text value stored as a single jsonb column, e.g. {"en":"Wheat","hi":"गेहूं"}.
 * Scales to any number of languages without a schema change or a translations table.
 */
public class LocalizedText extends HashMap<String, String> {

    public LocalizedText() {
        super();
    }

    public static LocalizedText of(String en, String hi) {
        LocalizedText text = new LocalizedText();
        if (en != null) text.put("en", en);
        if (hi != null) text.put("hi", hi);
        return text;
    }
}
