package com.socialmediatraining.contentservice.service.post;

import com.socialmediatraining.contentservice.dto.post.ContentRequest;
import com.socialmediatraining.contentservice.dto.post.ContentResponse;
import com.socialmediatraining.contentservice.dto.post.ContentResponseAdmin;
import com.socialmediatraining.contentservice.entity.Content;
import com.socialmediatraining.contentservice.entity.ExternalUser;
import com.socialmediatraining.contentservice.repository.ContentRepository;
import com.socialmediatraining.contentservice.repository.ExternalUserRepository;
import com.socialmediatraining.dtoutils.dto.SimpleUserDataObject;
import com.socialmediatraining.dtoutils.dto.UserCommentNotification;
import com.socialmediatraining.exceptioncommons.exception.PostNotFoundException;
import com.socialmediatraining.exceptioncommons.exception.UserActionForbiddenException;
import com.socialmediatraining.exceptioncommons.exception.UserDoesntExistsException;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.core.HttpHeaders;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static com.socialmediatraining.authenticationcommons.JwtUtils.getUsernameFromAuthHeader;
import static com.socialmediatraining.authenticationcommons.JwtUtils.getSubIdFromAuthHeader;

@Service
@Transactional
@Slf4j
public class ContentService {

    private final ContentRepository contentRepository;
    private final ExternalUserRepository externalUserRepository;
    private final KafkaTemplate<String, SimpleUserDataObject> userDataKafkaTemplate;
    private final KafkaTemplate<String, UserCommentNotification> userCommentKafkaTemplate;
    private final WebClient.Builder webClientBuilder;

    @Autowired
    public ContentService(ContentRepository contentRepository, ExternalUserRepository externalUserRepository, KafkaTemplate<String, SimpleUserDataObject> userDataKafkaTemplate, KafkaTemplate<String, UserCommentNotification> userCommentKafkaTemplate, WebClient.Builder webClientBuilder) {
        this.contentRepository = contentRepository;
        this.externalUserRepository = externalUserRepository;
        this.userDataKafkaTemplate = userDataKafkaTemplate;
        this.userCommentKafkaTemplate = userCommentKafkaTemplate;
        this.webClientBuilder = webClientBuilder;
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
                .userId(UUID.fromString(simpleUserData.userId()))
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
                    .userId(UUID.fromString(subId))
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
                throw new PostNotFoundException("Parent post with userId " + post.parentId() + " doesn't exists");
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
        userDataKafkaTemplate.send("created-new-content",
                SimpleUserDataObject.create(externalUser.getUserId().toString(), externalUser.getUsername()));

        if(contentReturn.getParentId() != null){
            Content parentPost = contentRepository.findByIdAndDeletedAtIsNull(post.parentId()).orElse(null);
            userCommentKafkaTemplate.send("new-comment",
                    UserCommentNotification.create(
                            parentPost.getCreatorId().toString(),
                            externalUser.getId().toString(),
                            externalUser.getUsername(),
                            newPost.getId().toString(),
                            parentPost.getText().substring(0, Math.min(parentPost.getText().length(), 20))
                    ));
        }

        return ContentResponse.create(
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
            throw new PostNotFoundException("Cannot find post with userId "+ postId + " to edit");
        }

        if(!content.getCreatorId().equals(externalUser.getId())){
            throw new UserActionForbiddenException("User is not authorized to update the post of another post");
        }

        content.setText(postRequest.text());
        content.setMediaUrls(postRequest.media_urls());
        content.setUpdatedAt(LocalDateTime.now());

        Content contentReturn = contentRepository.save(content);

        return ContentResponse.create(
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
            throw new PostNotFoundException("Cannot find post with userId "+ postId + " to delete");
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
            throw new PostNotFoundException("Cannot find post with userId "+ contentId);
        }

        return ContentResponse.create(
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
            default -> throw new RuntimeException("Invalid post type");
        };

        return contentList.getContent().stream().map( content -> ContentResponseAdmin.create(
                        content.getId(),
                        content.getCreatorId(),
                        content.getParentId(),
                        content.getCreatedAt(),
                        content.getUpdatedAt(),
                        content.getText(),
                        content.getMediaUrls(),
                        content.getDeletedAt()
                )
        ).collect(Collectors.toList());
    }

    private Flux<SimpleUserDataObject> getListOfFollowedUser(String authHeader){
        String username = getUsernameFromAuthHeader(authHeader);

        return webClientBuilder.baseUrl("http://user-service").build()
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v1/follow/follows/{username}")
                        .queryParam("limit", 50)
                        .queryParam("orderBy", "activity")
                        .build(username))
                .header(HttpHeaders.AUTHORIZATION, authHeader)
                .header("Service","content-service")
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError,
                        response -> response.bodyToMono(String.class)
                                .flatMap(errorBody -> {
                                    log.error("Client error from user-service: {}", errorBody);
                                    return Mono.error(
                                            new ResponseStatusException(
                                                    response.statusCode(),
                                                    errorBody
                                    ));
                                })
                )
                .onStatus(HttpStatusCode::is5xxServerError, response ->
                        Mono.error(new ResponseStatusException(
                                HttpStatus.SERVICE_UNAVAILABLE,
                                "User service is currently unavailable"
                        ))
                )
                .bodyToFlux(SimpleUserDataObject.class)
                .doOnNext(user -> log.info("Received user: {}", user));
    }

    private Mono<Page<ContentResponse>> getContentPage(List<String> userIds, Pageable pageable) {
        return Mono.fromCallable(() ->
                        contentRepository.findAllByCreatorIdInAndDeletedAtIsNull(userIds, pageable)
                                .orElse(Page.empty(pageable)))
                .subscribeOn(Schedulers.boundedElastic())
                .onErrorResume(e -> {
                    log.error("Error while fetching content for user feed", e);
                    return Mono.error(new ResponseStatusException(
                            HttpStatus.INTERNAL_SERVER_ERROR,
                            "Error processing your feed"
                    ));
                })
                .map(contentPage -> contentPage.map(content ->
                        ContentResponse.create(
                                content.getId(),
                                content.getCreatorId(),
                                content.getParentId(),
                                content.getCreatedAt(),
                                content.getUpdatedAt(),
                                content.getText(),
                                content.getMediaUrls()
                        )
                ));
    }

    public Flux<Page<ContentResponse>> getUserFeed(String authHeader, Pageable pageable){
        return getListOfFollowedUser(authHeader)
                 .collectList()
                 .flatMapMany( users -> {
                     if (users.isEmpty()) {
                         log.info("No followed users found, returning empty feed");
                         return Flux.just(Page.<ContentResponse>empty(pageable));
                     }
                     List<String> ids = users.stream()
                             .map(SimpleUserDataObject::userId)
                             .collect(Collectors.toList()
                     );
                     return getContentPage(ids, pageable);
                 })
                .onErrorResume(ResponseStatusException.class, e -> {
                    log.error("Error while fetching user feed", e);
                    return Mono.error(e);
                });
    }
}