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
    @UuidGenerator
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private UUID id;

    @Column(name = "user_id")
    private String userId;

    @Column(name = "username")
    private String username;

    @OneToMany(mappedBy = "user",fetch = FetchType.LAZY,cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<UserContentLike> contentLiked = new HashSet<>();;

    @OneToMany(mappedBy = "user",fetch = FetchType.LAZY,cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<UserContentFavorite> contentFavorites = new HashSet<>();;

    public void addContentFavorite(UserContentFavorite contentFavorite){
        contentFavorites.add(contentFavorite);
    }

    public void removeContentFavorite(UserContentFavorite contentFavorite){
        contentFavorites.remove(contentFavorite);
    }

    public void addContentLike(UserContentLike contentLike){
        contentLiked.add(contentLike);
    }

    public void removeContentLike(UserContentLike contentLike){
        contentLiked.remove(contentLike);
    }
}
