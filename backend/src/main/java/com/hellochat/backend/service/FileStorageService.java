package com.hellochat.backend.service;

import com.hellochat.backend.dto.UploadResponse;
import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {

    UploadResponse upload(Long uploaderId, MultipartFile file, String scene);
}
