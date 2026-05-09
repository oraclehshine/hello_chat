package com.hellochat.backend.repository;

import com.hellochat.backend.entity.EmailCaptcha;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmailCaptchaRepository extends JpaRepository<EmailCaptcha, Long> {
    Optional<EmailCaptcha> findTopByEmailAndSceneOrderByCreatedAtDesc(String email, String scene);
}
