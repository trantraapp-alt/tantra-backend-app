package com.hyperlocal.tantra.modules.payment.dto;

import lombok.Data;

@Data
public class InitiatePaymentRequest {
    /** Plan to purchase, e.g. FREE_TRIAL / QUARTERLY / HALF_YEARLY */
    private String planKey;
}
