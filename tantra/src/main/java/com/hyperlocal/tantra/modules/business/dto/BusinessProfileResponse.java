package com.hyperlocal.tantra.modules.business.dto;

import com.hyperlocal.tantra.modules.business.model.VerificationStatus;
import com.hyperlocal.tantra.modules.forms.model.LocalizedText;
import lombok.Data;

/** Compact response for business-profile writes/verification actions. */
@Data
public class BusinessProfileResponse {

    private boolean success;
    private String profileId;
    private VerificationStatus status;
    /** The reason behind the current status — rejected reason / blocked reason (null for PENDING/APPROVED). */
    private String reason;
    private LocalizedText message;

    public static BusinessProfileResponse ok(String profileId, VerificationStatus status, String reason, LocalizedText message) {
        BusinessProfileResponse response = new BusinessProfileResponse();
        response.setSuccess(true);
        response.setProfileId(profileId);
        response.setStatus(status);
        response.setReason(reason);
        response.setMessage(message);
        return response;
    }
}
