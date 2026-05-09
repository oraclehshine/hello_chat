package com.hellochat.backend.service;

import com.hellochat.backend.dto.AuthResponse;
import com.hellochat.backend.dto.CaptchaRequest;
import com.hellochat.backend.dto.ChangePasswordRequest;
import com.hellochat.backend.dto.LoginRequest;
import com.hellochat.backend.dto.RefreshTokenRequest;
import com.hellochat.backend.dto.RegisterRequest;
import com.hellochat.backend.dto.ResetPasswordRequest;
import com.hellochat.backend.dto.UpdateProfileRequest;
import com.hellochat.backend.dto.UpdateEmailRequest;

public interface AuthService {
    String sendCaptcha(CaptchaRequest request);

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    AuthResponse refresh(RefreshTokenRequest request);

    void logout(String refreshToken);

    void resetPassword(ResetPasswordRequest request);

    void changePassword(Long userId, ChangePasswordRequest request);

    AuthResponse updateProfile(Long userId, UpdateProfileRequest request);

    AuthResponse updateEmail(UpdateEmailRequest request);
}
