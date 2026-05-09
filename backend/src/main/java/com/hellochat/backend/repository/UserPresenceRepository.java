package com.hellochat.backend.repository;

import com.hellochat.backend.entity.UserPresence;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserPresenceRepository extends JpaRepository<UserPresence, Long> {
}
