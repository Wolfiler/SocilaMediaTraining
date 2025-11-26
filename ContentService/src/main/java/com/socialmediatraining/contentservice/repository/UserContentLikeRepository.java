package com.socialmediatraining.contentservice.repository;

import com.socialmediatraining.contentservice.entity.Content;
import com.socialmediatraining.contentservice.entity.ExternalUser;
import com.socialmediatraining.contentservice.entity.UserContentLike;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserContentLikeRepository extends JpaRepository<UserContentLike, UUID> {

    Optional<Page<UserContentLike>> findAllByUser_Id(UUID userId, Pageable pageable);

    Optional<UserContentLike> findByContentAndUser_Id(Content content,UUID userId);

    boolean existsByUserIdAndContentId(UUID userId, UUID contentId);

}
