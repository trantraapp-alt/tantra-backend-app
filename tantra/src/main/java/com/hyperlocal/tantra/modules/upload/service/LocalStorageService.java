package com.hyperlocal.tantra.modules.upload.service;

import com.hyperlocal.tantra.constants.MessageConstants;
import com.hyperlocal.tantra.exception.LocalizedException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Saves uploaded images to a local folder and returns URLs served by the static resource handler
 * (see WebConfig). Only JPG/PNG/WEBP are accepted.
 */
@Service
@ConditionalOnProperty(prefix = "app.storage", name = "type", havingValue = "local", matchIfMissing = true)
public class LocalStorageService implements StorageService {

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    /** URL prefix the files are served under; can be pointed at a CDN base later. */
    @Value("${app.upload.base-url:/files}")
    private String baseUrl;

    private static final Map<String, String> ALLOWED = Map.of(
            "image/jpeg", ".jpg",
            "image/png", ".png",
            "image/webp", ".webp");

    @Override
    public List<String> store(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            throw new LocalizedException(MessageConstants.UPLOAD_EMPTY_EN, MessageConstants.UPLOAD_EMPTY_HI);
        }

        Path root = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            throw new RuntimeException("Could not create upload directory: " + root, e);
        }

        List<String> urls = new ArrayList<>();
        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                throw new LocalizedException(MessageConstants.UPLOAD_EMPTY_EN, MessageConstants.UPLOAD_EMPTY_HI);
            }
            String ext = ALLOWED.get(file.getContentType());
            if (ext == null) {
                throw new LocalizedException(
                        MessageConstants.UPLOAD_INVALID_TYPE_EN, MessageConstants.UPLOAD_INVALID_TYPE_HI);
            }
            String fileName = UUID.randomUUID().toString().replace("-", "") + ext;
            try {
                Files.copy(file.getInputStream(), root.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                throw new RuntimeException("Failed to store file " + file.getOriginalFilename(), e);
            }
            urls.add(trimTrailingSlash(baseUrl) + "/" + fileName);
        }
        return urls;
    }

    private String trimTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
