package com.hyperlocal.tantra.modules.listing.dto;

import com.hyperlocal.tantra.modules.forms.model.LocalizedText;
import lombok.Data;

/**
 * Compact response returned after creating a listing — just the outcome, the ids the client needs,
 * and a bilingual message. The full listing is intentionally not echoed back.
 */
@Data
public class ListingResponse {

    private boolean success;
    private String listingId;
    private String userId;
    private LocalizedText message;

    public static ListingResponse ok(String listingId, String userId, LocalizedText message) {
        ListingResponse response = new ListingResponse();
        response.setSuccess(true);
        response.setListingId(listingId);
        response.setUserId(userId);
        response.setMessage(message);
        return response;
    }
}
