package com.hyperlocal.tantra.modules.upload.dto;

import com.hyperlocal.tantra.modules.forms.model.LocalizedText;
import lombok.Data;

import java.util.List;

/**
 * Response of the image-upload endpoint: the URLs the client then puts into the listing's
 * {@code images} array, plus a bilingual message.
 */
@Data
public class UploadResponse {

    private boolean success;
    private List<String> urls;
    private LocalizedText message;

    public static UploadResponse ok(List<String> urls, LocalizedText message) {
        UploadResponse response = new UploadResponse();
        response.setSuccess(true);
        response.setUrls(urls);
        response.setMessage(message);
        return response;
    }
}
