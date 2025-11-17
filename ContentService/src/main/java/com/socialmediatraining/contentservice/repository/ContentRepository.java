package com.socialmediatraining.contentservice.repository;

import com.socialmediatraining.contentservice.entity.Content;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ContentRepository extends JpaRepository<Content, UUID> {

    Optional<Page<Content>> findAllByCreatorIdAndDeletedAtIsNull(UUID creatorId, Pageable pageable);

    Optional<Content> findByIdAndDeletedAtIsNull(UUID id);

}
