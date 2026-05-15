package com.hellochat.backend.common;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleValidation(Exception ex) {
        log.warn("Validation failed: {}", ex.getMessage());
        if (ex instanceof MethodArgumentNotValidException validationException) {
            String field = validationException.getBindingResult().getFieldError() == null
                ? "parameter"
                : validationException.getBindingResult().getFieldError().getField();
            if ("password".equals(field) || "newPassword".equals(field)) {
                return ApiResponse.failure(40001, "password must contain uppercase, lowercase, and number");
            }
        }
        return ApiResponse.failure(40001, "parameter validation failed");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("Business exception: {}", ex.getMessage(), ex);
        return ApiResponse.failure(resolveCode(ex.getMessage()), ex.getMessage());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException ex) {
        log.warn("Upload size exceeded: {}", ex.getMessage(), ex);
        return ApiResponse.failure(40001, "upload file too large, backend limit is 200MB");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public ApiResponse<Void> handleMethodNotSupported(
        HttpRequestMethodNotSupportedException ex,
        HttpServletRequest request
    ) {
        log.warn(
            "Method not supported: method={}, uri={}, query={}, supportedMethods={}",
            request.getMethod(),
            request.getRequestURI(),
            request.getQueryString(),
            ex.getSupportedHttpMethods()
        );
        return ApiResponse.failure(40500, "request method not supported");
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<Void> handleException(Exception ex, HttpServletRequest request) {
        log.error(
            "Unhandled exception: method={}, uri={}, query={}",
            request.getMethod(),
            request.getRequestURI(),
            request.getQueryString(),
            ex
        );
        return ApiResponse.failure(50000, "internal server error");
    }

    private int resolveCode(String message) {
        if (message == null) {
            return 40001;
        }
        if (message.contains("captcha")) {
            return 40005;
        }
        if (message.contains("registered")) {
            return 40006;
        }
        if (message.contains("not found")) {
            return 40002;
        }
        if (message.contains("Authorization") || message.contains("token")) {
            return 40003;
        }
        return 40001;
    }
}
