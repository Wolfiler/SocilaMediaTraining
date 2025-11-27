package com.socialmediatraining.contentservice.service.post;

import com.socialmediatraining.contentservice.dto.post.ContentRequest;
import com.socialmediatraining.contentservice.dto.post.ContentResponse;
import com.socialmediatraining.contentservice.dto.post.ContentResponseAdmin;
import com.socialmediatraining.contentservice.entity.Content;
import com.socialmediatraining.contentservice.repository.ContentRepository;
import com.socialmediatraining.contentservice.service.user.UserCacheService;
import com.socialmediatraining.dtoutils.dto.PageResponse;
import com.socialmediatraining.dtoutils.dto.SimpleUserDataObject;
import com.socialmediatraining.dtoutils.dto.UserCommentNotification;
import com.socialmediatraining.exceptioncommons.exception.PostNotFoundException;
import com.socialmediatraining.exceptioncommons.exception.UserActionForbiddenException;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.core.HttpHeaders;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
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
    private final KafkaTemplate<String, SimpleUserDataObject> userDataKafkaTemplate;
    private final KafkaTemplate<String, UserCommentNotification> userCommentKafkaTemplate;
    private final WebClient.Builder webClientBuilder;

    private final UserCacheService userCacheService;

    @Autowired
    public ContentService(ContentRepository contentRepository, KafkaTemplate<String, SimpleUserDataObject> userDataKafkaTemplate, KafkaTemplate<String, UserCommentNotification> userCommentKafkaTemplate, WebClient.Builder webClientBuilder, UserCacheService userCacheService) {
        this.contentRepository = contentRepository;
        this.userDataKafkaTemplate = userDataKafkaTemplate;
        this.userCommentKafkaTemplate = userCommentKafkaTemplate;
        this.webClientBuilder = webClientBuilder;
        this.userCacheService = userCacheService;
    }

    public ContentResponse createContent(String authHeader, ContentRequest post){
        SimpleUserDataObject userData = userCacheService.getOrCreatNewExternalUserIfNotExists(
                getSubIdFromAuthHeader(authHeader),
                getUsernameFromAuthHeader(authHeader));


        if(post.parentId() != null){
            boolean parentExists = contentRepository.existsByIdAndDeletedAtIsNull(post.parentId());
            if(!parentExists){
                throw new PostNotFoundException("Parent post with userId " + post.parentId() + " doesn't exists");
            }
        }

        Content newPost = Content.builder()
                .creatorId(UUID.fromString(userData.userId()))
                .parentId(post.parentId())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .text(post.text())
                .mediaUrls(post.media_urls())
                .build();

        Content contentReturn = contentRepository.save(newPost);
        userDataKafkaTemplate.send("created-new-content", userData);

        if(contentReturn.getParentId() != null){
            contentRepository.findByIdAndDeletedAtIsNull(post.parentId()).ifPresent(
                    parentPost -> userCommentKafkaTemplate.send("new-comment",
                            UserCommentNotification.create(
                                    parentPost.getCreatorId().toString(),
                                    userData.userId(),
                                    userData.username(),
                                    newPost.getId().toString(),
                                    parentPost.getText().substring(0, Math.min(parentPost.getText().length(), 20))
                            )
                    )
            );
        }

        return ContentResponse.fromEntity(contentReturn);
    }

    public ContentResponse updateContent(UUID postId, String authHeader, ContentRequest postRequest){
        String username = getUsernameFromAuthHeader(authHeader);

        SimpleUserDataObject userDataObject = userCacheService.getUserDataByUsername(username);

        Content content = contentRepository.findByIdAndDeletedAtIsNull(postId).orElseThrow(
                () -> new PostNotFoundException("Cannot find post with userId "+ postId + " to edit")
        );

        if(!content.getCreatorId().toString().equals(userDataObject.userId())){
            throw new UserActionForbiddenException("User is not authorized to update the post of another post");
        }

        content.setText(postRequest.text());
        content.setMediaUrls(postRequest.media_urls());
        content.setUpdatedAt(LocalDateTime.now());

        Content contentReturn = contentRepository.save(content);

        return ContentResponse.fromEntity(contentReturn);
    }

    public String softDeleteContent(UUID postId, String authHeader){
        SimpleUserDataObject userData = userCacheService.getOrCreatNewExternalUserIfNotExists(
                getSubIdFromAuthHeader(authHeader),
                getUsernameFromAuthHeader(authHeader));
        Content content = contentRepository.findByIdAndDeletedAtIsNull(postId)
                .orElse(null);
        if(content == null || content.getDeletedAt() != null){
            throw new PostNotFoundException("Cannot find post with userId "+ postId + " to delete");
        }

        if(!content.getCreatorId().toString().equals(userData.userId())){
            throw new UserActionForbiddenException("Cannot delete post of another user !");
        }

        content.setDeletedAt(LocalDateTime.now());
        content.setText("Deleted");
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
                .orElseThrow(() ->
                        new PostNotFoundException("Cannot find post with userId "+ contentId));

        return ContentResponse.fromEntity(content);
    }

    public PageResponse<ContentResponse> getAllVisibleContentFromUser(String username, Pageable pageable,String postType){
        List<ContentResponseAdmin> posts = getAllContentFromUser(username,pageable,false,postType);
        List<ContentResponse> contentResponses = posts.stream().map(ContentResponseAdmin::postResponse).collect(Collectors.toList());
        return PageResponse.from(new PageImpl<>(contentResponses,pageable,contentResponses.size()));
    }

    public PageResponse<ContentResponseAdmin> getAllContentFromUser(String username, Pageable pageable){
        List<ContentResponseAdmin> content = getAllContentFromUser(username,pageable,true,"all");
        return PageResponse.from(new PageImpl<>(content,pageable,content.size()));
    }

    private List<ContentResponseAdmin> getAllContentFromUser(String username, Pageable pageable, boolean getDeletedContents,String postType){
        SimpleUserDataObject userData = userCacheService.getUserDataByUsername(username);

        Page<Content> contentList = switch (postType) {
            case "all" -> (getDeletedContents ?
                    contentRepository.findAllByCreatorId(UUID.fromString(userData.userId()), pageable) :
                    contentRepository.findAllByCreatorIdAndDeletedAtIsNull(UUID.fromString(userData.userId()), pageable))
                    .orElse(new PageImpl<>(new ArrayList<>()));
            case "post" -> (getDeletedContents ?
                    contentRepository.findAllByCreatorIdAndParentIdIsNull(UUID.fromString(userData.userId()), pageable) :
                    contentRepository.findAllByCreatorIdAndParentIdIsNullAndDeletedAtIsNull(UUID.fromString(userData.userId()), pageable))
                    .orElse(new PageImpl<>(new ArrayList<>()));
            case "comment" -> (getDeletedContents ?
                    contentRepository.findAllByCreatorIdAndParentIdIsNotNull(UUID.fromString(userData.userId()), pageable) :
                    contentRepository.findAllByCreatorIdAndParentIdIsNotNullAndDeletedAtIsNull(UUID.fromString(userData.userId()), pageable))
                    .orElse(new PageImpl<>(new ArrayList<>()));
            default -> throw new RuntimeException("Invalid post type");
        };

        return contentList.getContent().stream().map(ContentResponseAdmin::fromEntity)
                .collect(Collectors.toList());
    }

    private Flux<SimpleUserDataObject> getListOfFollowedUser(String username, String authHeader){
        return webClientBuilder.baseUrl("http://user-service").build()
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v1/follow/follows/{username}")
                        .queryParam("limit", 50)
                        .queryParam("orderBy", "activity")
                        .build(username)
                )
                .header(HttpHeaders.AUTHORIZATION, authHeader)
                .header("Service","content-service")
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError,
                        response -> response.bodyToMono(String.class)
                                .flatMap(errorBody -> {
                                    log.error("Client error from user-service: {}", errorBody);
                                    return Mono.error(
                                            new ResponseStatusException(response.statusCode(), errorBody
                                    ));
                                })
                )
                .onStatus(HttpStatusCode::is5xxServerError, response ->
                        Mono.error(new ResponseStatusException(
                                HttpStatus.SERVICE_UNAVAILABLE, "User service is currently unavailable"
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
                            HttpStatus.INTERNAL_SERVER_ERROR, "Error processing your feed"
                    ));
                })
                .map(contentPage -> contentPage.map(ContentResponse::fromEntity
                ));
    }

    @Cacheable(value = "userFeed", key = "#username+':'+pageable.pageNumber", condition = "#result != null")
    public Flux<PageResponse<ContentResponse>> getUserFeed(String username, String authHeader, Pageable pageable){
        return getListOfFollowedUser(username,authHeader)
                 .collectList()
                 .flatMapMany( users -> {
                     if (users.isEmpty()) {
                         return Flux.just(PageResponse.from(Page.<ContentResponse>empty(pageable)));
                     }
                     List<String> ids = users.stream()
                             .map(SimpleUserDataObject::userId)
                             .collect(Collectors.toList()
                     );
                     return getContentPage(ids, pageable)
                             .map(PageResponse::from);
                 })
                .onErrorResume(ResponseStatusException.class, e -> {
                    log.error("Error while fetching user feed", e);
                    return Mono.error(e);
                });
    }
}