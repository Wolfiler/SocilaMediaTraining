package com.socialmediatraining.contentservice.service;

import com.socialmediatraining.contentservice.dto.post.PostRequest;
import com.socialmediatraining.contentservice.dto.post.PostResponse;
import com.socialmediatraining.contentservice.entity.Content;
import com.socialmediatraining.contentservice.entity.ExternalUser;
import com.socialmediatraining.contentservice.entity.UserContentLike;
import com.socialmediatraining.contentservice.repository.ContentRepository;
import com.socialmediatraining.contentservice.repository.ExternalUserRepository;
import com.socialmediatraining.contentservice.repository.UserContentLikeRepository;
import com.socialmediatraining.exceptioncommons.exception.PostNotFoundException;
import com.socialmediatraining.exceptioncommons.exception.UserActionForbiddenException;
import com.socialmediatraining.exceptioncommons.exception.UserDoesntExistsException;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.socialmediatraining.authenticationcommons.JwtUtils.getPreferredUsernameFromAuthHeader;
import static com.socialmediatraining.authenticationcommons.JwtUtils.getSubIdFromAuthHeader;

@Service
@Transactional
public class PostService {

    private final ContentRepository contentRepository;
    private final ExternalUserRepository externalUserRepository;
    private final UserContentLikeRepository userContentLikeRepository;

    @Autowired
    public PostService(ContentRepository contentRepository, ExternalUserRepository externalUserRepository, UserContentLikeRepository userContentLikeRepository) {
        this.contentRepository = contentRepository;
        this.externalUserRepository = externalUserRepository;
        this.userContentLikeRepository = userContentLikeRepository;
    }

    private ExternalUser getOrCreatNewExternalUserIfNotExists(String authHeader){
        String subId = getSubIdFromAuthHeader(authHeader);
        ExternalUser externalUser = externalUserRepository.findExternalUserByUserId(subId).orElse(null);
        if(externalUser == null){
            String username = getPreferredUsernameFromAuthHeader(authHeader);
            ExternalUser newUser = ExternalUser.builder()
                    .userId(subId)
                    .username(username)
                    .build();
            externalUser = externalUserRepository.save(newUser);
        }
        return externalUser;
    }

    public PostResponse createNewPost(String authHeader, PostRequest post){
        ExternalUser externalUser = getOrCreatNewExternalUserIfNotExists(authHeader);

        Content newPost = Content.builder()
                .creatorId(externalUser.getId())
                .parentId(null)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .text(post.text())
                .mediaUrls(post.media_urls())
                .build();

        Content contentReturn = contentRepository.save(newPost);

        return new PostResponse(
                contentReturn.getId(),
                contentReturn.getCreatorId(),
                contentReturn.getParentId(),
                contentReturn.getCreatedAt(),
                contentReturn.getUpdatedAt(),
                contentReturn.getText(),
                contentReturn.getMediaUrls()
        );
    }

    public PostResponse updatePost(UUID postId,String authHeader,PostRequest postRequest){ // TODO make async with Kafka
        String subId = getSubIdFromAuthHeader(authHeader);
        ExternalUser externalUser = externalUserRepository.findExternalUserByUserId(subId).orElse(null);
        if(externalUser == null){
            throw new UserDoesntExistsException("Error while fetching user from database when updating post.");
        }

        Content content = contentRepository.findByIdAndDeletedAtIsNull(postId).orElse(null);
        if(content == null){
            throw new PostNotFoundException("Cannot find post with id "+ postId + " to edit");
        }

        if(!content.getCreatorId().equals(externalUser.getId())){
            throw new UserActionForbiddenException("User is not authorized to update the post of another post");
        }

        content.setText(postRequest.text());
        content.setMediaUrls(postRequest.media_urls());
        content.setUpdatedAt(LocalDateTime.now());

        Content contentReturn = contentRepository.save(content);

        return new PostResponse(
                contentReturn.getId(),
                contentReturn.getCreatorId(),
                contentReturn.getParentId(),
                contentReturn.getCreatedAt(),
                contentReturn.getUpdatedAt(),
                contentReturn.getText(),
                contentReturn.getMediaUrls()
        );
    }

    public String softDeletePost(UUID postId,String authHeader){ // TODO make async with Kafka
        ExternalUser externalUser = getOrCreatNewExternalUserIfNotExists(authHeader);
        Content content = contentRepository.findByIdAndDeletedAtIsNull(postId)
                .orElse(null);
        if(content == null || content.getDeletedAt() != null){
            throw new PostNotFoundException("Cannot find post with id "+ postId + " to delete");
        }

        if(!content.getCreatorId().equals(externalUser.getId())){
            throw new UserActionForbiddenException("Cannot delete post of another user !");
        }

        content.setDeletedAt(LocalDateTime.now());
        contentRepository.save(content);

        return String.format("Post %s deleted successfully",postId);
    }

    public PostResponse getVisiblePostById(UUID postId) {
        return getPostById(postId,false);
    }

    public PostResponse getPostByIdWithDeleted(UUID postId) {
        return getPostById(postId,true);
    }

    private PostResponse getPostById(UUID postId, boolean getDeletedPost) {
        Content content = (getDeletedPost ?
                contentRepository.findById(postId)
                : contentRepository.findByIdAndDeletedAtIsNull(postId))
                .orElse(null);
        if(content == null){
            throw new PostNotFoundException("Cannot find post with id "+ postId);
        }

        return new PostResponse(
                content.getId(),
                content.getCreatorId(),
                content.getParentId(),
                content.getCreatedAt(),
                content.getUpdatedAt(),
                content.getText(),
                content.getMediaUrls()
        );
    }

    public String likePost(String authHeader, UUID postId){ // TODO make async with Kafka
        String subId = getSubIdFromAuthHeader(authHeader);
        ExternalUser externalUser = getOrCreatNewExternalUserIfNotExists(authHeader);

        Content post = contentRepository.findByIdAndDeletedAtIsNull(postId)
                .orElse(null);
        if(post == null){
            throw new PostNotFoundException("Post with id " + postId + " doesn't exists");
        }

        if (userContentLikeRepository.existsByUserIdAndContentId(externalUser.getId(), postId)) {
            throw new UserActionForbiddenException("User " + externalUser.getUsername() + " already liked post with id " + postId);
        }

        UserContentLike userContentLike = UserContentLike.builder()
                .user(externalUser)
                .content(post)
                .build();

        userContentLikeRepository.save(userContentLike);

        externalUser.addContentLike(userContentLike);
        post.addLike(userContentLike);

        return String.format("User %s liked post with id %s",subId,postId);
    }

    public String deleteLike(String authHeader, UUID postId){ // TODO make async with Kafka
        String subId = getSubIdFromAuthHeader(authHeader);
        ExternalUser externalUser = externalUserRepository.findExternalUserByUserId(subId)
                .orElse(null);
        if(externalUser == null){
            throw new UserDoesntExistsException("Error while fetching user from database when deleting like");
        }

        Content post = contentRepository.findByIdAndDeletedAtIsNull(postId)
                .orElse(null);
        if(post == null){
            throw new PostNotFoundException("Post with id " + postId + " doesn't exists");
        }

        UserContentLike userContentLike = userContentLikeRepository.findByUserAndContent(externalUser,post)
                .orElse(null);
        if(userContentLike == null){
            throw new UserActionForbiddenException("Post with id " + postId + " isn't liked by user " + externalUser.getUsername());
        }

        userContentLikeRepository.delete(userContentLike);

        externalUser.removeContentLike(userContentLike);
        post.removeLike(userContentLike);

        return String.format("User %s unliked post with id %s",subId,postId);
    }

    public List<PostResponse> getAllVisiblePostsFromUser(String username, Pageable pageable){
        return getAllPostsFromUser(username,pageable,false);
    }

    public List<PostResponse> getAllPostsFromUser(String username, Pageable pageable){
        return getAllPostsFromUser(username,pageable,true);
    }

    private List<PostResponse> getAllPostsFromUser(String username, Pageable pageable, boolean getDeletedPosts){
        ExternalUser externalUser = externalUserRepository.findExternalUserByUsername(username).orElse(null);
        if(externalUser == null){
            throw new UserDoesntExistsException("User with username " + username + " doesn't exists");
            // -> maybe return null instead of error
            //User might not exist in content database but can still be an existing user in the auth user database
        }

        Page<Content> contentList = (getDeletedPosts ?
                    contentRepository.findAllByCreatorId(externalUser.getId(),pageable) :
                    contentRepository.findAllByCreatorIdAndDeletedAtIsNull(externalUser.getId(),pageable))
                .orElse(new PageImpl<>(new ArrayList<>()));

        return contentList.getContent().stream().map( content -> new PostResponse(
                content.getId(),
                content.getCreatorId(),
                content.getParentId(),
                content.getCreatedAt(),
                content.getUpdatedAt(),
                content.getText(),
                content.getMediaUrls()
            )
        ).collect(Collectors.toList());
    }

    public List<PostResponse> getAllLikedPostsByUser(String username, Pageable pageable) {
        ExternalUser externalUser = externalUserRepository.findExternalUserByUsername(username).orElse(null);
        if(externalUser == null){
            throw new UserDoesntExistsException("User with username " + username + " doesn't exists");
            // -> maybe return null instead of error
            //User might not exist in content database but can still be an existing user in the auth user database
        }

        Page<UserContentLike> userContentLikeList = userContentLikeRepository.findAllByUser(externalUser,pageable)
                .orElse(null);

        if(userContentLikeList == null || userContentLikeList.getContent().isEmpty()){
            return new ArrayList<>();
        }

        return userContentLikeList.getContent().stream().map(userContentLike -> new PostResponse(
                userContentLike.getContent().getId(),
                userContentLike.getContent().getCreatorId(),
                userContentLike.getContent().getParentId(),
                userContentLike.getContent().getCreatedAt(),
                userContentLike.getContent().getUpdatedAt(),
                userContentLike.getContent().getText(),
                userContentLike.getContent().getMediaUrls()
        )).collect(Collectors.toList());
    }
}
