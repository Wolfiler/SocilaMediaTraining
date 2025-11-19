package com.socialmediatraining.contentservice.service.like;

import com.socialmediatraining.contentservice.dto.post.ContentResponse;
import com.socialmediatraining.contentservice.entity.Content;
import com.socialmediatraining.contentservice.entity.ExternalUser;
import com.socialmediatraining.contentservice.entity.UserContentLike;
import com.socialmediatraining.contentservice.repository.ContentRepository;
import com.socialmediatraining.contentservice.repository.ExternalUserRepository;
import com.socialmediatraining.contentservice.repository.UserContentLikeRepository;
import com.socialmediatraining.exceptioncommons.exception.PostNotFoundException;
import com.socialmediatraining.exceptioncommons.exception.UserActionForbiddenException;
import com.socialmediatraining.exceptioncommons.exception.UserDoesntExistsException;
import org.apache.catalina.User;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.socialmediatraining.authenticationcommons.JwtUtils.getSubIdFromAuthHeader;
import static com.socialmediatraining.authenticationcommons.JwtUtils.getUsernameFromAuthHeader;

@Service
public class LikeService {
    private final ContentRepository contentRepository;
    private final UserContentLikeRepository userContentLikeRepository;
    private final ExternalUserRepository externalUserRepository;

    public LikeService(ContentRepository contentRepository, UserContentLikeRepository userContentLikeRepository, ExternalUserRepository externalUserRepository) {
        this.contentRepository = contentRepository;
        this.userContentLikeRepository = userContentLikeRepository;
        this.externalUserRepository = externalUserRepository;
    }

    @CachePut(value = "users", key = "#username", unless = "#result == null")
    public ExternalUser getOrCreatNewExternalUserIfNotExists(String subId, String username){
        ExternalUser externalUser = externalUserRepository.findExternalUserByUsername(username).orElse(null);
        if(externalUser == null){
            ExternalUser newUser = ExternalUser.builder()
                    .userId(subId)
                    .username(username)
                    .build();
            externalUser = externalUserRepository.save(newUser);
        }
        return externalUser;
    }

    @CachePut(value = "users", key = "#username", unless = "#result == null")
    public ExternalUser getExternalUserByUsername(String username){
        ExternalUser externalUser = externalUserRepository.findExternalUserByUsername(username).orElse(null);
        if(externalUser == null){
            throw new UserDoesntExistsException("User with username " + username + " doesn't exists");
        }
        return externalUser;
    }

    @CacheEvict(value = "posts", key = "#contentId")
    public String likeContent(String authHeader, UUID contentId){
        String subId = getSubIdFromAuthHeader(authHeader);
        ExternalUser externalUser = getOrCreatNewExternalUserIfNotExists(
                getSubIdFromAuthHeader(authHeader),
                getUsernameFromAuthHeader(authHeader));

        Content post = contentRepository.findByIdAndDeletedAtIsNull(contentId)
                .orElse(null);
        if(post == null){
            throw new PostNotFoundException("Post with id " + contentId + " doesn't exists");
        }

        if (userContentLikeRepository.existsByUserIdAndContentId(externalUser.getId(), contentId)) {
            throw new UserActionForbiddenException("User " + externalUser.getUsername() + " already liked post with id " + contentId);
        }

        UserContentLike userContentLike = UserContentLike.builder()
                .user(externalUser)
                .content(post)
                .build();

        userContentLikeRepository.save(userContentLike);

        externalUser.addContentLike(userContentLike);
        post.addLike(userContentLike);

        return String.format("User %s liked post with id %s",subId,contentId);
    }

    @CacheEvict(value = "posts", key = "#contentId")
    public String deleteLike(String authHeader, UUID contentId){
        String username = getUsernameFromAuthHeader(authHeader);
        ExternalUser externalUser = getExternalUserByUsername(username);
        if(externalUser == null){
            throw new UserDoesntExistsException("Error while fetching user from database when deleting like");
        }

        Content post = contentRepository.findByIdAndDeletedAtIsNull(contentId)
                .orElse(null);
        if(post == null){
            throw new PostNotFoundException("Post with id " + contentId + " doesn't exists");
        }

        UserContentLike userContentLike = userContentLikeRepository.findByUserAndContent(externalUser,post)
                .orElse(null);
        if(userContentLike == null){
            throw new UserActionForbiddenException("Post with id " + contentId + " isn't liked by user " + externalUser.getUsername());
        }

        userContentLikeRepository.delete(userContentLike);

        externalUser.removeContentLike(userContentLike);
        post.removeLike(userContentLike);

        return String.format("User %s unliked post with id %s",username,contentId);
    }

    public Page<ContentResponse> getAllLikedContentsByUser(String username, Pageable pageable) {
        ExternalUser externalUser = getExternalUserByUsername(username);
        if(externalUser == null){
            throw new UserDoesntExistsException("User with username " + username + " doesn't exists");
            // -> maybe return null instead of error
            //User might not exist in content database but can still be an existing user in the auth user database
        }

        Page<UserContentLike> userContentLikeList = userContentLikeRepository.findAllByUser(externalUser,pageable)
                .orElse(null);

        if(userContentLikeList == null || userContentLikeList.getContent().isEmpty()){
            return new PageImpl<>(new ArrayList<>(),pageable,0);
        }

        List<ContentResponse> userContentLikes = userContentLikeList.getContent().stream().map(userContentLike -> new ContentResponse(
                userContentLike.getContent().getId(),
                userContentLike.getContent().getCreatorId(),
                userContentLike.getContent().getParentId(),
                userContentLike.getContent().getCreatedAt(),
                userContentLike.getContent().getUpdatedAt(),
                userContentLike.getContent().getText(),
                userContentLike.getContent().getMediaUrls()
        )).toList();

        return new PageImpl<>(userContentLikes,pageable,userContentLikes.size());
    }
}
