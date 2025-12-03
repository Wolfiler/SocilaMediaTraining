package com.socialmediatraining.contentservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "user_content_like",
        uniqueConstraints = @UniqueConstraint(
                name = "uc_user_content_like",
                columnNames = {"user_id", "content_id"}
        )
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class UserContentLike {
    @Id
    @UuidGenerator
    @GeneratedValue
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private ExternalUser user;

    @ManyToOne
    @JoinColumn(name = "content_id")
    private Content content;

    @Column(name = "created_at")
    @CreationTimestamp
    private LocalDateTime createdAt;

    public void setUser(ExternalUser user){
        if(user == null && this.user != null){
            this.user.removeContentLike(this.content);
            this.content = null;
        }
        this.user = user;
    }

    public void setContent(Content content){
        if(content == null && this.content != null){
            this.user.removeContentLike(this.content);
            this.user = null;
        }
        this.content = content;
    }
}
