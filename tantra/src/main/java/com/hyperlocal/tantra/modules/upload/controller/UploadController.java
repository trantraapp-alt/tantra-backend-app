package com.hyperlocal.tantra.modules.upload.controller;

import com.hyperlocal.tantra.constants.MessageConstants;
import com.hyperlocal.tantra.modules.forms.model.LocalizedText;
import com.hyperlocal.tantra.modules.upload.dto.UploadResponse;
import com.hyperlocal.tantra.modules.upload.service.StorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Image upload for listings. Authenticated users POST files (multipart/form-data, field "files")
 * and get back URLs to place in the listing's images array. Locked to authenticated via
 * /api/v1/uploads/** in SecurityConfig.
 */
@RestController
@RequestMapping("/api/v1/uploads")
public class UploadController {

    @Autowired private StorageService storageService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UploadResponse> upload(@RequestParam("files") List<MultipartFile> files) {
        List<String> urls = storageService.store(files);
        return ResponseEntity.ok(UploadResponse.ok(urls,
                LocalizedText.of(MessageConstants.UPLOAD_SUCCESS_EN, MessageConstants.UPLOAD_SUCCESS_HI)));
    }
}
