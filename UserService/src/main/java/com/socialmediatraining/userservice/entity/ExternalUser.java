package com.socialmediatraining.userservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "external_user")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class ExternalUser {

    @Id
    private UUID id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(name = "last_activity_at")
    private LocalDateTime lastActivityAt;

    @OneToMany(mappedBy = "followingUserId", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<ExternalUserFollow> following = new HashSet<>();

    @OneToMany(mappedBy = "followedUserId", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<ExternalUserFollow> followers = new HashSet<>();

    public ExternalUserFollow follow(ExternalUser userToFollow) {
        if (userToFollow == null || this.equals(userToFollow)) {
            return null;
        }

        ExternalUserFollow follow = new ExternalUserFollow(this, userToFollow);
        if (!following.contains(follow)) {
            following.add(follow);
            userToFollow.getFollowers().add(follow);
        }
        return follow;
    }

    public void unfollow(ExternalUser userToUnfollow) {
        if (userToUnfollow == null) {
            return;
        }

        following.removeIf(f -> f.getFollowedUserId().equals(userToUnfollow));
        userToUnfollow.getFollowers().removeIf(f -> f.getFollowingUserId().equals(this));
    }
}
