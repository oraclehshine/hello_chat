package com.hellochat.backend.controller;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.hellochat.backend.common.ApiResponse;
import com.hellochat.backend.common.CurrentUser;
import com.hellochat.backend.common.TokenProvider;
import com.hellochat.backend.dto.UploadResponse;
import com.hellochat.backend.sentinel.SentinelBlockHandlers;
import com.hellochat.backend.sentinel.SentinelResourceNames;
import com.hellochat.backend.service.FileStorageService;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/files")
public class FileController {

    private final FileStorageService fileStorageService;
    private final TokenProvider tokenProvider;

    public FileController(FileStorageService fileStorageService, TokenProvider tokenProvider) {
        this.fileStorageService = fileStorageService;
        this.tokenProvider = tokenProvider;
    }

    @PostMapping("/upload")
    @SentinelResource(
        value = SentinelResourceNames.FILE_UPLOAD,
        blockHandlerClass = SentinelBlockHandlers.class,
        blockHandler = "blockedFileUpload"
    )
    public ApiResponse<UploadResponse> upload(
        HttpServletRequest request,
        @RequestParam("file") MultipartFile file,
        @RequestParam(defaultValue = "attachment") String scene
    ) {
        return ApiResponse.success(fileStorageService.upload(CurrentUser.requireUserId(request, tokenProvider), file, scene));
    }

    @GetMapping("/redirect")
    public ResponseEntity<Void> redirect(@RequestParam("source") String sourceUrl) {
        URI target = fileStorageService.resolveAccessUri(sourceUrl);
        return ResponseEntity.status(HttpStatus.FOUND).location(target).build();
    }
}
