package com.hellochat.backend.service.impl;

import com.aliyun.oss.OSS;
import com.aliyun.oss.model.ObjectMetadata;
import com.hellochat.backend.config.AliyunOssProperties;
import com.hellochat.backend.dto.UploadResponse;
import com.hellochat.backend.entity.FileAsset;
import com.hellochat.backend.repository.FileAssetRepository;
import com.hellochat.backend.service.FileStorageService;
import java.io.IOException;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileStorageServiceImpl implements FileStorageService {

    private static final long IMAGE_MAX_SIZE = 10L * 1024 * 1024;
    private static final long FILE_MAX_SIZE = 100L * 1024 * 1024;
    private static final long VIDEO_MAX_SIZE = 200L * 1024 * 1024;
    private static final String DEFAULT_SCENE = "attachment";
    private static final Set<String> IMAGE_SCENES = Set.of("avatar", "group-avatar", "chat-image", "moment-image", "image");
    private static final Set<String> FILE_SCENES = Set.of("chat-file", "attachment", "file");
    private static final Set<String> VIDEO_SCENES = Set.of("moment-video", "video");

    private final ObjectProvider<OSS> ossClientProvider;
    private final AliyunOssProperties properties;
    private final FileAssetRepository fileAssetRepository;

    public FileStorageServiceImpl(ObjectProvider<OSS> ossClientProvider, AliyunOssProperties properties, FileAssetRepository fileAssetRepository) {
        this.ossClientProvider = ossClientProvider;
        this.properties = properties;
        this.fileAssetRepository = fileAssetRepository;
    }

    @Override
    public UploadResponse upload(Long uploaderId, MultipartFile file, String scene) {
        if (uploaderId == null) {
            throw new IllegalArgumentException("userId is required");
        }
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("file is required");
        }
        String safeScene = (scene == null || scene.isBlank()) ? DEFAULT_SCENE : scene.trim();
        String contentType = file.getContentType() == null ? "" : file.getContentType();
        validateScene(safeScene, contentType, file.getSize());
        String originalName = file.getOriginalFilename() == null ? "upload.bin" : file.getOriginalFilename();
        String extension = extractExtension(originalName);
        String objectKey = buildObjectKey(uploaderId, safeScene, extension);
        String url = resolvePublicUrl(objectKey);
        try {
            OSS ossClient = ossClientProvider.getIfAvailable();
            if (ossClient == null) {
                throw new IllegalArgumentException("oss is not configured");
            }
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(file.getSize());
            if (!contentType.isBlank()) {
                metadata.setContentType(contentType);
            }
            ossClient.putObject(properties.getBucketName(), objectKey, file.getInputStream(), metadata);
        } catch (IOException ex) {
            throw new IllegalArgumentException("oss upload failed");
        }

        FileAsset asset = new FileAsset();
        asset.setUploaderId(uploaderId);
        asset.setFileName(originalName);
        asset.setFileUrl(url);
        asset.setFileType(safeScene);
        asset.setMimeType(contentType);
        asset.setFileSize(file.getSize());
        asset.setScene(safeScene);
        FileAsset saved = fileAssetRepository.save(asset);

        return new UploadResponse(
            saved.getId(),
            saved.getFileUrl(),
            saved.getFileName(),
            saved.getFileType(),
            saved.getMimeType(),
            saved.getFileSize(),
            saved.getScene()
        );
    }

    private void validateScene(String scene, String contentType, long fileSize) {
        if (IMAGE_SCENES.contains(scene)) {
            if (!contentType.startsWith("image/")) {
                throw new IllegalArgumentException("only image upload is supported for scene " + scene);
            }
            if (fileSize > IMAGE_MAX_SIZE) {
                throw new IllegalArgumentException("image size must not exceed 10MB");
            }
            return;
        }
        if (VIDEO_SCENES.contains(scene)) {
            if (!contentType.startsWith("video/")) {
                throw new IllegalArgumentException("only video upload is supported for scene " + scene);
            }
            if (fileSize > VIDEO_MAX_SIZE) {
                throw new IllegalArgumentException("video size must not exceed 200MB");
            }
            return;
        }
        if (FILE_SCENES.contains(scene)) {
            if (fileSize > FILE_MAX_SIZE) {
                throw new IllegalArgumentException("file size must not exceed 100MB");
            }
            return;
        }
        throw new IllegalArgumentException("upload scene unsupported");
    }

    private String buildObjectKey(Long uploaderId, String scene, String extension) {
        String normalizedScene = scene.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9/_-]", "_");
        String suffix = extension.isBlank() ? "" : "." + extension;
        return "uploads/" + normalizedScene + "/" + uploaderId + "/" + UUID.randomUUID() + suffix;
    }

    private String resolvePublicUrl(String objectKey) {
        String prefix = properties.getPublicUrlPrefix();
        if (prefix != null && !prefix.isBlank()) {
            return prefix.endsWith("/") ? prefix + objectKey : prefix + "/" + objectKey;
        }
        String endpoint = properties.getEndpoint();
        return "https://" + properties.getBucketName() + "." + endpoint + "/" + objectKey;
    }

    private String extractExtension(String filename) {
        int index = filename.lastIndexOf('.');
        if (index < 0 || index == filename.length() - 1) {
            return "";
        }
        return filename.substring(index + 1).toLowerCase(Locale.ROOT);
    }
}
