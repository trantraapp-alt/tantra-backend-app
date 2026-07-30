package com.hyperlocal.tantra.modules.address.dto;

import com.hyperlocal.tantra.modules.forms.model.LocalizedText;
import lombok.Data;

/**
 * Compact response for address writes: outcome + addressId + bilingual message.
 */
@Data
public class AddressResponse {

    private boolean success;
    private String addressId;
    private LocalizedText message;

    public static AddressResponse ok(String addressId, LocalizedText message) {
        AddressResponse response = new AddressResponse();
        response.setSuccess(true);
        response.setAddressId(addressId);
        response.setMessage(message);
        return response;
    }
}
