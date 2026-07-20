package com.hyperlocal.tantra.exception;

import com.hyperlocal.tantra.modules.forms.model.LocalizedText;

/**
 * A validation/business error that carries its message in both English and Hindi, so the API can
 * return {@code {"success":false,"message":{"en":...,"hi":...}}} and let the client pick the language.
 */
public class LocalizedException extends RuntimeException {

    private final LocalizedText localized;

    public LocalizedException(String en, String hi) {
        super(en);
        this.localized = LocalizedText.of(en, hi);
    }

    public LocalizedText getLocalized() {
        return localized;
    }
}
