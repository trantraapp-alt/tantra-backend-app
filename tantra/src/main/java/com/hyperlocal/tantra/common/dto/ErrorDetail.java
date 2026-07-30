package com.hyperlocal.tantra.common.dto;

import com.hyperlocal.tantra.modules.forms.model.LocalizedText;
import lombok.AllArgsConstructor;
import lombok.Data;

/** Error payload inside {@link ApiResponse}: a stable machine code + a bilingual message. */
@Data
@AllArgsConstructor
public class ErrorDetail {
    private String code;
    private LocalizedText message;
}
