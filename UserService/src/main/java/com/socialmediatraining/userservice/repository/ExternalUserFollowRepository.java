package com.socialmediatraining.userservice.repository;

import com.socialmediatraining.userservice.entity.ExternalUserFollow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface  ExternalUserFollowRepository extends JpaRepository<ExternalUserFollow, UUID> {

}
