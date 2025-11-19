package com.socialmediatraining.userservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "user_follow")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ExternalUserFollow {

    @Id
    @UuidGenerator
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "followed_user_id", nullable = false)
    private ExternalUser followedUserId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "following_user_id", nullable = false)
    private ExternalUser followingUserId;

    @Column(name = "created_at",nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    public ExternalUserFollow(ExternalUser follower, ExternalUser followed) {
        this.followingUserId = follower;
        this.followedUserId = followed;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ExternalUserFollow that = (ExternalUserFollow) o;
        return Objects.equals(followedUserId, that.followedUserId) &&
                Objects.equals(followingUserId, that.followingUserId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(followedUserId, followingUserId);
    }

}
