package com.hellochat.backend.dto;

import com.hellochat.backend.entity.FileAsset;
import com.hellochat.backend.entity.MomentMedia;

public class MomentMediaResponse {

    private final Long fileId;
    private final String fileUrl;
    private final String fileName;
    private final String fileType;
    private final String mimeType;
    private final Long fileSize;
    private final Integer sortOrder;

    public MomentMediaResponse(MomentMedia media, FileAsset fileAsset) {
        this.fileId = media.getFileId();
        this.fileUrl = fileAsset == null ? "" : fileAsset.getFileUrl();
        this.fileName = fileAsset == null ? "" : fileAsset.getFileName();
        this.fileType = fileAsset == null ? "" : fileAsset.getFileType();
        this.mimeType = fileAsset == null ? "" : fileAsset.getMimeType();
        this.fileSize = fileAsset == null ? 0L : fileAsset.getFileSize();
        this.sortOrder = media.getSortOrder();
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

    public Integer getSortOrder() {
        return sortOrder;
    }
}
