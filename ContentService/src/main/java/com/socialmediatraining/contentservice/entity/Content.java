package com.socialmediatraining.contentservice.entity;

import com.vladmihalcea.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.*;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.ReadOnlyProperty;

import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(name = "content")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@DynamicUpdate
public class Content {
    @Id
    @UuidGenerator
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private UUID id;

    @NotNull
    @Column(name = "creator_id", nullable = false)
    private UUID creatorId;

    @Column(name = "parent_id")
    private UUID parentId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime  createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Size(min = 1,max = 255)
    private String text;

    @Type(JsonType.class)
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "media_urls", columnDefinition = "jsonb")
    private Map<String, String> mediaUrls = new HashMap<>();

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @OneToMany(mappedBy = "content",fetch = FetchType.LAZY,cascade = CascadeType.ALL, orphanRemoval = true)
    @Setter(AccessLevel.PRIVATE)
    private Set<UserContentLike> likes = new LinkedHashSet<>() {};

    @OneToMany(mappedBy = "content",fetch = FetchType.LAZY,cascade = CascadeType.ALL, orphanRemoval = true)
    @Setter(AccessLevel.PRIVATE)
    private Set<UserContentFavorite> favorites = new LinkedHashSet<>() {};

    public void addFavorite(UserContentFavorite favorite){
        favorites.add(favorite);
    }

    public void removeFavorite(UserContentFavorite favorite){
        favorites.remove(favorite);
    }

    public void addLike(UserContentLike like){
        if(like == null){
            throw new NullPointerException("Like cannot be null");
        }

        if(likes == null){
            likes = new HashSet<>();
        }
        likes.add(like);
    }

    public void removeLike(ExternalUser user){
        if(user != null){
            getLikesInternal().removeIf(contentLiked -> contentLiked.getUser().equals(user));
        }
    }

    public Set<UserContentLike> getLikes() {
        return likes == null ? Collections.unmodifiableSet(new HashSet<>()) : Collections.unmodifiableSet(likes);
    }

    public Set<UserContentFavorite> getFavorites() {
        return favorites == null ? Collections.unmodifiableSet(new HashSet<>()) : Collections.unmodifiableSet(favorites);
    }

    private Set<UserContentLike> getLikesInternal() {
        return likes == null ? new HashSet<>() : likes;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
