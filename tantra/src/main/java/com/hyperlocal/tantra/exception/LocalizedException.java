package com.hyperlocal.tantra.exception;

import com.hyperlocal.tantra.common.error.ErrorCode;
import com.hyperlocal.tantra.modules.forms.model.LocalizedText;

/**
 * A business/validation error carrying a bilingual message plus a stable error code and HTTP status.
 * The 2-arg constructor defaults to 400 / BAD_REQUEST so existing call sites keep working.
 */
public class LocalizedException extends RuntimeException {

    private final LocalizedText localized;
    private final String code;
    private final int status;

    public LocalizedException(String en, String hi) {
        this(en, hi, ErrorCode.BAD_REQUEST, 400);
    }

    public LocalizedException(String en, String hi, String code, int status) {
        super(en);
        this.localized = LocalizedText.of(en, hi);
        this.code = code;
        this.status = status;
    }

    public LocalizedText getLocalized() {
        return localized;
    }

    public String getCode() {
        return code;
    }

    public int getStatus() {
        return status;
    }
}
