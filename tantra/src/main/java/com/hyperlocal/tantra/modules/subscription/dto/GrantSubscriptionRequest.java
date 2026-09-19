package com.hyperlocal.tantra.modules.subscription.dto;

import lombok.Data;

@Data
public class GrantSubscriptionRequest {
    private String userId;
    private String planKey;
    private Integer durationDays;
    private String paymentRef;
    private String paymentGateway;
    private Boolean autoRenew;
    private String notes;
}
