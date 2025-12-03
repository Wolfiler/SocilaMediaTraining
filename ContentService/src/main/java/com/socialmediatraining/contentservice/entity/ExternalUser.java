package com.socialmediatraining.contentservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.*;

@Entity
@Table(name = "external_user")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExternalUser {
    @Id
    private UUID id;

    @Column(name = "username")
    private String username;

    @OneToMany(mappedBy = "user",fetch = FetchType.LAZY,cascade = CascadeType.ALL, orphanRemoval = true)
    @Setter(AccessLevel.PRIVATE)
    private Set<UserContentLike> contentLiked = new HashSet<>();

    @OneToMany(mappedBy = "user",fetch = FetchType.LAZY,cascade = CascadeType.ALL, orphanRemoval = true)
    @Setter(AccessLevel.PRIVATE)
    private Set<UserContentFavorite> contentFavorites = new HashSet<>();

    public UserContentLike addContentLike(Content content){
        if(content == null){
            throw new NullPointerException("Content cannot be null");
        }

        UserContentLike like = UserContentLike.builder()
                .content(content)
                .user(this)
                .build();
        content.addLike(like);
        if(contentLiked == null){
            contentLiked = new HashSet<>();
        }
        contentLiked.add(like);
        return like;
    }

    public void removeContentLike(Content content){
        if(content != null){
            getContentLikedInternal().removeIf(contentLiked -> contentLiked.getContent().equals(content));
            content.removeLike(this);
        }
    }

    public Set<UserContentLike> getLikes() {
        return contentLiked == null ? Collections.unmodifiableSet(new HashSet<>()) : Collections.unmodifiableSet(contentLiked);
    }

    public Set<UserContentFavorite> getContentFavorites() {
        return contentFavorites == null ? Collections.unmodifiableSet(new HashSet<>()) : Collections.unmodifiableSet(contentFavorites);
    }

    //Same as like, and kinda same behavior, might implement it one day, but no priority
    public void addContentFavorite(UserContentFavorite contentFavorite){
        throw new UnsupportedOperationException("Not implemented");
    }

    public void removeContentFavorite(UserContentFavorite contentFavorite){
        throw new UnsupportedOperationException("Not implemented");
    }

    private Set<UserContentLike> getContentLikedInternal() {
        if (contentLiked == null) {
            contentLiked = new HashSet<>();
        }
        return contentLiked;
    }
}
