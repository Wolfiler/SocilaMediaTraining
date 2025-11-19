package com.socialmediatraining.contentservice.service.post;

import com.socialmediatraining.authenticationcommons.dto.SimpleUserDataObject;
import com.socialmediatraining.contentservice.dto.post.ContentRequest;
import com.socialmediatraining.contentservice.dto.post.ContentResponse;
import com.socialmediatraining.contentservice.dto.post.ContentResponseAdmin;
import com.socialmediatraining.contentservice.entity.Content;
import com.socialmediatraining.contentservice.entity.ExternalUser;
import com.socialmediatraining.contentservice.repository.ContentRepository;
import com.socialmediatraining.contentservice.repository.ExternalUserRepository;
import com.socialmediatraining.contentservice.repository.UserContentLikeRepository;
import com.socialmediatraining.exceptioncommons.exception.PostNotFoundException;
import com.socialmediatraining.exceptioncommons.exception.UserActionForbiddenException;
import com.socialmediatraining.exceptioncommons.exception.UserDoesntExistsException;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.socialmediatraining.authenticationcommons.JwtUtils.getUsernameFromAuthHeader;
import static com.socialmediatraining.authenticationcommons.JwtUtils.getSubIdFromAuthHeader;

@Service
@Transactional
@Slf4j
public class ContentService {

    protected final ContentRepository contentRepository;
    protected final ExternalUserRepository externalUserRepository;
    protected final UserContentLikeRepository userContentLikeRepository;

    @Autowired
    public ContentService(ContentRepository contentRepository, ExternalUserRepository externalUserRepository, UserContentLikeRepository userContentLikeRepository) {
        this.contentRepository = contentRepository;
        this.externalUserRepository = externalUserRepository;
        this.userContentLikeRepository = userContentLikeRepository;
    }

    @KafkaListener(topics = "created-new-user", groupId = "content-service" )
    @CachePut(value = "users", key = "#simpleUserData.username()", unless = "#result == null")
    public void createNewUser(SimpleUserDataObject simpleUserData) {
        ExternalUser newUser = externalUserRepository.findExternalUserByUsername(simpleUserData.username()).orElse(null);
        if(newUser != null){//This should never be the case, might want to remove this check.
            log.error("User already exists in database. This should not happen, there might be an issue in user creation flow");
            return;
        }

        newUser = ExternalUser.builder()
                .userId(simpleUserData.id())
                .username(simpleUserData.username())
                .build();
        externalUserRepository.save(newUser);
        log.info("Kafka topic caught -> New user created: {}", simpleUserData);
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

    @CachePut(value = "posts", key = "#result.id", unless = "#result == null")
    public ContentResponse createContent(String authHeader, ContentRequest post){
        ExternalUser externalUser = getOrCreatNewExternalUserIfNotExists(
                getSubIdFromAuthHeader(authHeader),
                getUsernameFromAuthHeader(authHeader));

        if(post.parentId() != null){
            boolean parentExists = contentRepository.existsByIdAndDeletedAtIsNull(post.parentId());
            if(!parentExists){
                throw new PostNotFoundException("Parent post with id " + post.parentId() + " doesn't exists");
            }
        }

        Content newPost = Content.builder()
                .creatorId(externalUser.getId())
                .parentId(post.parentId())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .text(post.text())
                .mediaUrls(post.media_urls())
                .build();

        Content contentReturn = contentRepository.save(newPost);

        return new ContentResponse(
                contentReturn.getId(),
                contentReturn.getCreatorId(),
                contentReturn.getParentId(),
                contentReturn.getCreatedAt(),
                contentReturn.getUpdatedAt(),
                contentReturn.getText(),
                contentReturn.getMediaUrls()
        );
    }

    @CachePut(value = "posts", key = "#postId", unless = "#result == null")
    public ContentResponse updateContent(UUID postId, String authHeader, ContentRequest postRequest){
        String username = getUsernameFromAuthHeader(authHeader);
        ExternalUser externalUser = getExternalUserByUsername(username);

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

        return new ContentResponse(
                contentReturn.getId(),
                contentReturn.getCreatorId(),
                contentReturn.getParentId(),
                contentReturn.getCreatedAt(),
                contentReturn.getUpdatedAt(),
                contentReturn.getText(),
                contentReturn.getMediaUrls()
        );
    }

    @CacheEvict(value = "posts", key = "#postId")
    public String softDeleteContent(UUID postId, String authHeader){
        ExternalUser externalUser = getOrCreatNewExternalUserIfNotExists(
                getSubIdFromAuthHeader(authHeader),
                getUsernameFromAuthHeader(authHeader));
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

    public ContentResponse getVisibleContentById(UUID contentId) {
        return getContentById(contentId,false);
    }

    public ContentResponse getContentByIdWithDeleted(UUID contentId) {
        return getContentById(contentId,true);
    }

    private ContentResponse getContentById(UUID contentId, boolean getDeletedContent) {
        Content content = (getDeletedContent ?
                contentRepository.findById(contentId)
                : contentRepository.findByIdAndDeletedAtIsNull(contentId))
                .orElse(null);
        if(content == null){
            throw new PostNotFoundException("Cannot find post with id "+ contentId);
        }

        return new ContentResponse(
                content.getId(),
                content.getCreatorId(),
                content.getParentId(),
                content.getCreatedAt(),
                content.getUpdatedAt(),
                content.getText(),
                content.getMediaUrls()
        );
    }

    public Page<ContentResponse> getAllVisibleContentFromUser(String username, Pageable pageable,String postType){
        List<ContentResponseAdmin> posts = getAllContentFromUser(username,pageable,false,postType);
        List<ContentResponse> contentResponses = posts.stream().map(ContentResponseAdmin::postResponse).collect(Collectors.toList());
        return new PageImpl<>(contentResponses,pageable,contentResponses.size());
    }

    public Page<ContentResponseAdmin> getAllContentFromUser(String username, Pageable pageable){
        List<ContentResponseAdmin> content = getAllContentFromUser(username,pageable,true,"all");
        return new PageImpl<>(content,pageable,content.size());
    }

    private List<ContentResponseAdmin> getAllContentFromUser(String username, Pageable pageable, boolean getDeletedContents,String postType){
        ExternalUser externalUser = getExternalUserByUsername(username);
        if(externalUser == null){
            throw new UserDoesntExistsException("User with username " + username + " doesn't exists");
            // -> maybe return null instead of error
            //User might not exist in content database but can still be an existing user in the auth user database
        }

        Page<Content> contentList = switch (postType) {
            case "all" -> (getDeletedContents ?
                    contentRepository.findAllByCreatorId(externalUser.getId(), pageable) :
                    contentRepository.findAllByCreatorIdAndDeletedAtIsNull(externalUser.getId(), pageable))
                    .orElse(new PageImpl<>(new ArrayList<>()));
            case "post" -> (getDeletedContents ?
                    contentRepository.findAllByCreatorIdAndParentIdIsNull(externalUser.getId(), pageable) :
                    contentRepository.findAllByCreatorIdAndParentIdIsNullAndDeletedAtIsNull(externalUser.getId(), pageable))
                    .orElse(new PageImpl<>(new ArrayList<>()));
            case "comment" -> (getDeletedContents ?
                    contentRepository.findAllByCreatorIdAndParentIdIsNotNull(externalUser.getId(), pageable) :
                    contentRepository.findAllByCreatorIdAndParentIdIsNotNullAndDeletedAtIsNull(externalUser.getId(), pageable))
                    .orElse(new PageImpl<>(new ArrayList<>()));
            default -> throw new RuntimeException("Invalid post type");//TODO custom exception
        };

        return contentList.getContent().stream().map( content -> new ContentResponseAdmin(
                        new ContentResponse(
                                content.getId(),
                                content.getCreatorId(),
                                content.getParentId(),
                                content.getCreatedAt(),
                                content.getUpdatedAt(),
                                content.getText(),
                                content.getMediaUrls()

                        ),
                        content.getDeletedAt()
                )
        ).collect(Collectors.toList());
    }
}