package com.hyperlocal.tantra.modules.upload.service;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Abstraction over where uploaded images live. Local-disk today; swap in an S3/R2 implementation
 * later without touching the controller or the listing flow.
 */
public interface StorageService {

    /** Stores the given files and returns their public URLs, in order. */
    List<String> store(List<MultipartFile> files);
}
