package com.hyperlocal.tantra.modules.admin.dto;

import lombok.Data;

/**
 * Request body for PUT /api/v1/admin/categories/{categoryId}/quality-desc
 * Admin sets the quality-assured description shown on listing detail pages for a category.
 */
@Data
public class AdminQualityDescRequest {

    /** Quality assurance text in English. Pass null to clear. */
    private String qualityDescEn;

    /** Quality assurance text in Hindi. Pass null to clear. */
    private String qualityDescHi;
}
