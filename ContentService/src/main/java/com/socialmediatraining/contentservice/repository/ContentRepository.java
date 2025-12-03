package com.socialmediatraining.contentservice.repository;

import com.socialmediatraining.contentservice.entity.Content;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ContentRepository extends JpaRepository<Content, UUID> {

    Optional<Page<Content>> findAllByCreatorIdAndDeletedAtIsNull(UUID creatorId, Pageable pageable);
    Optional<Page<Content>> findAllByCreatorId(UUID creatorId, Pageable pageable);
    Optional<Page<Content>> findAllByCreatorIdAndParentIdIsNullAndDeletedAtIsNull(UUID creatorId, Pageable pageable);
    Optional<Page<Content>> findAllByCreatorIdAndParentIdIsNotNullAndDeletedAtIsNull(UUID creatorId, Pageable pageable);
    Optional<Page<Content>> findAllByCreatorIdAndParentIdIsNull(UUID creatorId, Pageable pageable);
    Optional<Page<Content>> findAllByCreatorIdAndParentIdIsNotNull(UUID creatorId, Pageable pageable);
    Optional<Content> findByIdAndDeletedAtIsNull(UUID id);

    @Query( "SELECT c " +
            "FROM Content c " +
            "INNER JOIN ExternalUser u ON c.creatorId = u.id " +
            "WHERE u.id IN :ids " +
            "AND c.deletedAt IS NULL " +
            "ORDER BY c.createdAt DESC")
    Optional<Page<Content>> findAllByCreatorIdInAndDeletedAtIsNull(List<String> ids, Pageable pageable);


    boolean existsByIdAndDeletedAtIsNull(UUID id);
}
