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

    //Forced to do this because ids are not the same between databases,
    //and content has the database userId saved as creator_id, instead of the user_id,
    //shared between databases and used as a unique userId along with username.
    //This will need to be changed in the future if I have the time
    @Query( "SELECT c " +
            "FROM Content c " +
            "INNER JOIN ExternalUser u ON c.creatorId = u.id " +
            "WHERE u.id IN :ids " +
            "AND c.deletedAt IS NULL " +
            "ORDER BY c.createdAt DESC")
    Optional<Page<Content>> findAllByCreatorIdInAndDeletedAtIsNull(List<String> ids, Pageable pageable);


    boolean existsByIdAndDeletedAtIsNull(UUID id);
}
