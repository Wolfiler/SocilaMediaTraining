package com.socialmediatraining.contentservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

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
    private Set<UserContentLike> contentLiked = new HashSet<>();;

    @OneToMany(mappedBy = "user",fetch = FetchType.LAZY,cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<UserContentFavorite> contentFavorites = new HashSet<>();;

    //Same as like, and kinda same behavior, might implement it one day, but no priority
    public void addContentFavorite(UserContentFavorite contentFavorite){
        throw new UnsupportedOperationException("Not implemented");
    }

    public void removeContentFavorite(UserContentFavorite contentFavorite){
        throw new UnsupportedOperationException("Not implemented");
    }

    public UserContentLike addContentLike(Content content){
        UserContentLike userContentLike = UserContentLike.builder()
                .content(content)
                .user(this)
                .build();
        contentLiked.add(userContentLike);
        content.addLike(userContentLike);
        return userContentLike;
    }

    public void removeContentLike(Content content){
        contentLiked.removeIf(contentLiked -> contentLiked.getContent().equals(content));
        content.getLikes().removeIf(contentLiked -> contentLiked.getUser().equals(this));
    }
}
