package com.hyperlocal.tantra.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.hyperlocal.tantra.modules.forms.model.LocalizedText;
import lombok.Data;

/**
 * The single API envelope for every endpoint:
 * <pre>
 * success: { success:true,  data:{...}, message:{en,hi}, traceId, timestamp }
 * error:   { success:false, error:{ code, message:{en,hi} }, traceId, timestamp }
 * </pre>
 * Wrapping is applied automatically (see ResponseWrapperAdvice); controllers just return their data.
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private boolean success;
    private T data;
    private LocalizedText message;
    private ErrorDetail error;
    private String traceId;
    private String timestamp;

    public static <T> ApiResponse<T> ok(T data) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setSuccess(true);
        response.setData(data);
        return response;
    }

    public static <T> ApiResponse<T> ok(T data, LocalizedText message) {
        ApiResponse<T> response = ok(data);
        response.setMessage(message);
        return response;
    }

    public static <T> ApiResponse<T> fail(ErrorDetail error) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setSuccess(false);
        response.setError(error);
        return response;
    }
}
