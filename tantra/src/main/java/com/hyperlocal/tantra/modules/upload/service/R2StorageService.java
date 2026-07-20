package com.hyperlocal.tantra.modules.upload.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Cloudflare R2 (S3-compatible) storage. Active only when {@code app.storage.type=r2}; otherwise
 * {@link LocalStorageService} handles uploads. Chosen for production because R2 has free egress —
 * the dominant cost for an image-heavy marketplace — and is S3-API compatible.
 *
 * <p>The real upload code is commented out so the project builds without the AWS SDK on the
 * classpath. To go live on R2:</p>
 * <ol>
 *   <li>Uncomment the AWS SDK v2 dependency in {@code pom.xml} and run a build to fetch it.</li>
 *   <li>Uncomment the imports, the {@code S3Client} field + {@code @PostConstruct} init, and the
 *       upload body below; delete the {@code throw}.</li>
 *   <li>Set {@code app.storage.type=r2} and the {@code app.storage.r2.*} properties (endpoint,
 *       bucket, keys, public-base-url) — ideally from environment variables, not committed.</li>
 * </ol>
 *
 * <p>Later optimization: switch to <b>presigned direct uploads</b> (client PUTs straight to R2 so
 * bytes never touch this server) and a {@code tmp/} prefix + R2 lifecycle rule to auto-expire
 * orphaned uploads for free.</p>
 */
@Service
@ConditionalOnProperty(prefix = "app.storage", name = "type", havingValue = "r2")
public class R2StorageService implements StorageService {

    @Value("${app.storage.r2.bucket:}")
    private String bucket;

    /** Public/CDN base the objects are served from, e.g. https://cdn.tantra.app. */
    @Value("${app.storage.r2.public-base-url:}")
    private String publicBaseUrl;

    // @Value("${app.storage.r2.endpoint:}")   private String endpoint;    // https://<account>.r2.cloudflarestorage.com
    // @Value("${app.storage.r2.access-key:}") private String accessKey;
    // @Value("${app.storage.r2.secret-key:}") private String secretKey;

    // ---- Uncomment after adding the AWS SDK v2 (software.amazon.awssdk:s3) dependency ----
    // private software.amazon.awssdk.services.s3.S3Client s3;
    //
    // @jakarta.annotation.PostConstruct
    // void init() {
    //     s3 = software.amazon.awssdk.services.s3.S3Client.builder()
    //             .endpointOverride(java.net.URI.create(endpoint))
    //             .region(software.amazon.awssdk.regions.Region.of("auto"))
    //             .credentialsProvider(software.amazon.awssdk.auth.credentials.StaticCredentialsProvider.create(
    //                     software.amazon.awssdk.auth.credentials.AwsBasicCredentials.create(accessKey, secretKey)))
    //             .build();
    // }

    private static final java.util.Map<String, String> ALLOWED = java.util.Map.of(
            "image/jpeg", ".jpg", "image/png", ".png", "image/webp", ".webp");

    @Override
    public List<String> store(List<MultipartFile> files) {
        // ===== R2 upload — enable per the class Javadoc steps =====
        // java.util.List<String> urls = new java.util.ArrayList<>();
        // for (MultipartFile file : files) {
        //     String ext = ALLOWED.get(file.getContentType());
        //     if (ext == null) throw new com.hyperlocal.tantra.exception.LocalizedException(
        //             com.hyperlocal.tantra.constants.MessageConstants.UPLOAD_INVALID_TYPE_EN,
        //             com.hyperlocal.tantra.constants.MessageConstants.UPLOAD_INVALID_TYPE_HI);
        //     String key = "listings/" + java.util.UUID.randomUUID().toString().replace("-", "") + ext;
        //     try {
        //         s3.putObject(
        //                 software.amazon.awssdk.services.s3.model.PutObjectRequest.builder()
        //                         .bucket(bucket).key(key).contentType(file.getContentType()).build(),
        //                 software.amazon.awssdk.core.sync.RequestBody.fromInputStream(
        //                         file.getInputStream(), file.getSize()));
        //     } catch (java.io.IOException e) {
        //         throw new RuntimeException("Failed to upload to R2: " + key, e);
        //     }
        //     urls.add(publicBaseUrl + "/" + key);
        // }
        // return urls;

        throw new UnsupportedOperationException(
                "R2 storage selected (app.storage.type=r2) but not enabled yet. Add the AWS SDK v2 "
                        + "dependency in pom.xml, uncomment the S3 client + upload code in R2StorageService, "
                        + "and set the app.storage.r2.* properties.");
    }
}
