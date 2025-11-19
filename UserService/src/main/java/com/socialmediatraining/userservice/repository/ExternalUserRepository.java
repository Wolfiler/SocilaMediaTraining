package com.socialmediatraining.userservice.repository;

import com.socialmediatraining.userservice.entity.ExternalUser;
import org.springframework.cache.annotation.CachePut;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ExternalUserRepository extends JpaRepository<ExternalUser, UUID> {

    @CachePut(value = "users", key = "#username", condition = "#result != null")
    Optional<ExternalUser> findExternalUserByUsername(String username);
}
