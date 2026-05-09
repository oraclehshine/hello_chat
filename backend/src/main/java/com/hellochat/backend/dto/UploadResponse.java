package com.hellochat.backend.dto;

public class UploadResponse {

    private final Long fileId;
    private final String fileUrl;
    private final String fileName;
    private final String fileType;
    private final String mimeType;
    private final Long fileSize;
    private final String scene;

    public UploadResponse(Long fileId, String fileUrl, String fileName, String fileType, String mimeType, Long fileSize, String scene) {
        this.fileId = fileId;
        this.fileUrl = fileUrl;
        this.fileName = fileName;
        this.fileType = fileType;
        this.mimeType = mimeType;
        this.fileSize = fileSize;
        this.scene = scene;
    }

    public Long getFileId() {
        return fileId;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public String getFileName() {
        return fileName;
    }

    public String getFileType() {
        return fileType;
    }

    public String getMimeType() {
        return mimeType;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public String getScene() {
        return scene;
    }
}
