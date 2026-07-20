package com.hyperlocal.tantra.modules.forms.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.hyperlocal.tantra.modules.forms.model.LocalizedText;
import lombok.Data;

/**
 * Compact envelope for admin write actions: outcome + bilingual message + the affected id
 * (or extra {@code data} for bulk), instead of echoing the whole entity back.
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse {

    private boolean success;
    private LocalizedText message;
    /** Single entity id for create/update (null for bulk/delete). */
    private Object id;
    /** Optional extra payload, e.g. {"ids":[...],"count":n} for bulk inserts. */
    private Object data;

    public static ApiResponse ok(LocalizedText message) {
        ApiResponse response = new ApiResponse();
        response.setSuccess(true);
        response.setMessage(message);
        return response;
    }

    public static ApiResponse ok(Object id, LocalizedText message) {
        ApiResponse response = ok(message);
        response.setId(id);
        return response;
    }

    public static ApiResponse okData(Object data, LocalizedText message) {
        ApiResponse response = ok(message);
        response.setData(data);
        return response;
    }
}
