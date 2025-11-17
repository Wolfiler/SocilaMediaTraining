package com.socialmediatraining.contentservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "user_content_favorite",
        uniqueConstraints = @UniqueConstraint(
                name = "uc_user_content_like",
                columnNames = {"user_id", "content_id"}
        )
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserContentFavorite {
    @Id
    @UuidGenerator
    @GeneratedValue
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private ExternalUser user;

    @ManyToOne
    @JoinColumn(name = "content_id", nullable = false)
    private Content content;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
