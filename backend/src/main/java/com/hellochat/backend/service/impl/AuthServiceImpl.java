package com.hellochat.backend.service.impl;

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
import com.hellochat.backend.entity.AuthToken;
import com.hellochat.backend.entity.EmailCaptcha;
import com.hellochat.backend.entity.User;
import com.hellochat.backend.repository.AuthTokenRepository;
import com.hellochat.backend.repository.EmailCaptchaRepository;
import com.hellochat.backend.repository.UserRepository;
import com.hellochat.backend.service.AuthService;
import com.hellochat.backend.common.PasswordCodec;
import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final EmailCaptchaRepository emailCaptchaRepository;
    private final AuthTokenRepository authTokenRepository;
    private final PasswordCodec passwordCodec;
    private final TokenProvider tokenProvider;

    public AuthServiceImpl(
        UserRepository userRepository,
        EmailCaptchaRepository emailCaptchaRepository,
        AuthTokenRepository authTokenRepository,
        PasswordCodec passwordCodec,
        TokenProvider tokenProvider
    ) {
        this.userRepository = userRepository;
        this.emailCaptchaRepository = emailCaptchaRepository;
        this.authTokenRepository = authTokenRepository;
        this.passwordCodec = passwordCodec;
        this.tokenProvider = tokenProvider;
    }

    @Override
    public String sendCaptcha(CaptchaRequest request) {
        String scene = request.getScene();
        if (!"register".equals(scene) && !"reset_password".equals(scene) && !"modify_email".equals(scene)) {
            throw new IllegalArgumentException("captcha scene invalid");
        }
        EmailCaptcha captcha = new EmailCaptcha();
        captcha.setEmail(request.getEmail());
        captcha.setScene(scene);
        captcha.setCaptcha(String.format("%06d", (int) (Math.random() * 1000000)));
        captcha.setExpiresAt(LocalDateTime.now().plusMinutes(10));
        emailCaptchaRepository.save(captcha);
        return captcha.getCaptcha();
    }

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("email already registered");
        }
        EmailCaptcha captcha = emailCaptchaRepository
            .findTopByEmailAndSceneOrderByCreatedAtDesc(request.getEmail(), "register")
            .orElseThrow(() -> new IllegalArgumentException("captcha not found"));
        if (captcha.getConsumedAt() != null || !captcha.getCaptcha().equals(request.getCaptcha())
            || captcha.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("captcha invalid");
        }
        User user = new User();
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordCodec.encode(request.getPassword()));
        user.setNickname(request.getEmail().split("@")[0]);
        user.setStatus(User.Status.ACTIVE);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
        captcha.setConsumedAt(LocalDateTime.now());
        emailCaptchaRepository.save(captcha);
        return authResponse(user);
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
            .orElseThrow(() -> new IllegalArgumentException("user not found"));
        if (!passwordCodec.matches(request.getPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("password invalid");
        }
        user.setLastLoginAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
        return authResponse(user);
    }

    @Override
    @Transactional
    public AuthResponse refresh(RefreshTokenRequest request) {
        AuthToken authToken = authTokenRepository.findByRefreshToken(request.getRefreshToken())
            .orElseThrow(() -> new IllegalArgumentException("token invalid"));
        if (authToken.getRevokedAt() != null || authToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("token invalid");
        }
        User user = userRepository.findById(authToken.getUserId())
            .orElseThrow(() -> new IllegalArgumentException("user not found"));
        return new AuthResponse(
            tokenProvider.createAccessToken(user.getId()),
            authToken.getRefreshToken(),
            user.getId(),
            user.getEmail(),
            user.getNickname(),
            user.getAvatarUrl()
        );
    }

    @Override
    @Transactional
    public void logout(String refreshToken) {
        authTokenRepository.findByRefreshToken(refreshToken).ifPresent(authToken -> {
            authToken.setRevokedAt(LocalDateTime.now());
            authTokenRepository.save(authToken);
        });
    }

    @Override
    public void resetPassword(ResetPasswordRequest request) {
        EmailCaptcha captcha = emailCaptchaRepository
            .findTopByEmailAndSceneOrderByCreatedAtDesc(request.getEmail(), "reset_password")
            .orElseThrow(() -> new IllegalArgumentException("captcha not found"));
        if (captcha.getConsumedAt() != null || !captcha.getCaptcha().equals(request.getCaptcha())
            || captcha.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("captcha invalid");
        }
        User user = userRepository.findByEmail(request.getEmail())
            .orElseThrow(() -> new IllegalArgumentException("user not found"));
        user.setPasswordHash(passwordCodec.encode(request.getNewPassword()));
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
        captcha.setConsumedAt(LocalDateTime.now());
        emailCaptchaRepository.save(captcha);
    }

    @Override
    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("user not found"));
        if (!passwordCodec.matches(request.getOldPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("password invalid");
        }
        user.setPasswordHash(passwordCodec.encode(request.getNewPassword()));
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
    }

    @Override
    @Transactional
    public AuthResponse updateProfile(Long userId, UpdateProfileRequest request) {
        if (userId == null) {
            throw new IllegalArgumentException("userId is required");
        }
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("user not found"));
        if (request.getNickname() != null) {
            user.setNickname(request.getNickname());
        }
        if (request.getAvatarUrl() != null) {
            user.setAvatarUrl(request.getAvatarUrl());
        }
        if (request.getSignature() != null) {
            user.setSignature(request.getSignature());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }
        if (request.getGender() != null) {
            user.setGender(request.getGender());
        }
        if (request.getAge() != null) {
            user.setAge(request.getAge());
        }
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
        return authResponse(user);
    }

    @Override
    @Transactional
    public AuthResponse updateEmail(UpdateEmailRequest request) {
        if (request.getUserId() == null) {
            throw new IllegalArgumentException("userId is required");
        }
        if (request.getNewEmail() == null || request.getNewEmail().isEmpty()) {
            throw new IllegalArgumentException("newEmail is required");
        }
        if (request.getCaptcha() == null || request.getCaptcha().isEmpty()) {
            throw new IllegalArgumentException("captcha is required");
        }

        // Verify captcha
        EmailCaptcha captcha = emailCaptchaRepository
            .findTopByEmailAndSceneOrderByCreatedAtDesc(request.getNewEmail(), "modify_email")
            .orElseThrow(() -> new IllegalArgumentException("captcha not found"));
        if (!captcha.getCaptcha().equals(request.getCaptcha()) || captcha.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("captcha invalid");
        }

        // Check if new email already exists
        if (userRepository.existsByEmail(request.getNewEmail())) {
            throw new IllegalArgumentException("new email already registered");
        }

        // Update user email
        User user = userRepository.findById(request.getUserId())
            .orElseThrow(() -> new IllegalArgumentException("user not found"));
        user.setEmail(request.getNewEmail());
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        return authResponse(user);
    }

    private AuthResponse authResponse(User user) {
        AuthToken authToken = new AuthToken();
        authToken.setUserId(user.getId());
        authToken.setRefreshToken(UUID.randomUUID().toString().replace("-", ""));
        authToken.setExpiresAt(LocalDateTime.now().plusDays(30));
        authTokenRepository.save(authToken);
        return new AuthResponse(
            tokenProvider.createAccessToken(user.getId()),
            authToken.getRefreshToken(),
            user.getId(),
            user.getEmail(),
            user.getNickname(),
            user.getAvatarUrl()
        );
    }
}
