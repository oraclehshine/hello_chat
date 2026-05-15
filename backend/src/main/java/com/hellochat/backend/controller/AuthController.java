package com.hellochat.backend.controller;

import com.hellochat.backend.common.ApiResponse;
import com.hellochat.backend.common.CurrentUser;
import com.hellochat.backend.common.TokenProvider;
import com.hellochat.backend.dto.AuthResponse;
import com.hellochat.backend.dto.CaptchaRequest;
import com.hellochat.backend.dto.ChangePasswordRequest;
import com.hellochat.backend.dto.LoginRequest;
import com.hellochat.backend.dto.RefreshTokenRequest;
import com.hellochat.backend.dto.RegisterRequest;
import com.hellochat.backend.dto.ResetPasswordRequest;
import com.hellochat.backend.dto.UpdateProfileRequest;
import com.hellochat.backend.dto.UpdateEmailRequest;
import com.hellochat.backend.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final TokenProvider tokenProvider;

    public AuthController(AuthService authService, TokenProvider tokenProvider) {
        this.authService = authService;
        this.tokenProvider = tokenProvider;
    }

    @PostMapping("/email-captcha")
    public ApiResponse<String> sendCaptcha(@Valid @RequestBody CaptchaRequest request) {
        return ApiResponse.success(authService.sendCaptcha(request));
    }

    @GetMapping("/email-captcha")
    public ApiResponse<String> sendCaptchaByGet(
        @RequestParam("email") String email,
        @RequestParam(value = "scene", defaultValue = "register") String scene
    ) {
        CaptchaRequest request = new CaptchaRequest();
        request.setEmail(email);
        request.setScene(scene);
        return ApiResponse.success(authService.sendCaptcha(request));
    }

    @PostMapping("/register")
    public ApiResponse<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.success(authService.register(request));
    }

    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success(authService.login(request));
    }

    @PostMapping("/refresh")
    public ApiResponse<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ApiResponse.success(authService.refresh(request));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(@Valid @RequestBody RefreshTokenRequest request) {
        authService.logout(request.getRefreshToken());
        return ApiResponse.success();
    }

    @PostMapping("/password-reset-captcha")
    public ApiResponse<String> sendPasswordResetCaptcha(@Valid @RequestBody CaptchaRequest request) {
        request.setScene("reset_password");
        return ApiResponse.success(authService.sendCaptcha(request));
    }

    @GetMapping("/password-reset-captcha")
    public ApiResponse<String> sendPasswordResetCaptchaByGet(
        @RequestParam("email") String email
    ) {
        CaptchaRequest request = new CaptchaRequest();
        request.setEmail(email);
        request.setScene("reset_password");
        return ApiResponse.success(authService.sendCaptcha(request));
    }

    @PostMapping("/password-reset")
    public ApiResponse<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ApiResponse.success();
    }

    @PutMapping("/password")
    public ApiResponse<Void> changePassword(
        HttpServletRequest httpRequest,
        @Valid @RequestBody ChangePasswordRequest request
    ) {
        authService.changePassword(CurrentUser.requireUserId(httpRequest, tokenProvider), request);
        return ApiResponse.success();
    }

    @PostMapping("/profile/update")
    public ApiResponse<AuthResponse> updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        AuthResponse response = authService.updateProfile(request.getUserId(), request);
        return ApiResponse.success(response);
    }

    @PostMapping("/email/update")
    public ApiResponse<AuthResponse> updateEmail(@Valid @RequestBody UpdateEmailRequest request) {
        AuthResponse response = authService.updateEmail(request);
        return ApiResponse.success(response);
    }

    @GetMapping("/health")
    public ApiResponse<String> health() {
        return ApiResponse.success("ok");
    }
}
