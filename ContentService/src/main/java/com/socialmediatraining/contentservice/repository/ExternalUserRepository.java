package com.socialmediatraining.contentservice.repository;

import com.socialmediatraining.contentservice.entity.ExternalUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ExternalUserRepository extends JpaRepository<ExternalUser, UUID> {

    Optional<ExternalUser> findExternalUserByUserId(String userId);
    boolean existsExternalUserByUserId(String userId);

    Optional<ExternalUser> findExternalUserByUsername(String username);
}
