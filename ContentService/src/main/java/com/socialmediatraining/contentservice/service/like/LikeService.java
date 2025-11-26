package com.socialmediatraining.contentservice.service.like;

import com.socialmediatraining.contentservice.dto.post.ContentResponse;
import com.socialmediatraining.contentservice.entity.Content;
import com.socialmediatraining.contentservice.entity.ExternalUser;
import com.socialmediatraining.contentservice.entity.UserContentLike;
import com.socialmediatraining.contentservice.repository.ContentRepository;
import com.socialmediatraining.contentservice.repository.UserContentLikeRepository;
import com.socialmediatraining.contentservice.service.user.UserCacheService;
import com.socialmediatraining.dtoutils.dto.PageResponse;
import com.socialmediatraining.dtoutils.dto.SimpleUserDataObject;
import com.socialmediatraining.exceptioncommons.exception.PostNotFoundException;
import com.socialmediatraining.exceptioncommons.exception.UserActionForbiddenException;
import com.socialmediatraining.exceptioncommons.exception.UserDoesntExistsException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.socialmediatraining.authenticationcommons.JwtUtils.getSubIdFromAuthHeader;
import static com.socialmediatraining.authenticationcommons.JwtUtils.getUsernameFromAuthHeader;

@Service
public class LikeService {
    private final ContentRepository contentRepository;
    private final UserContentLikeRepository userContentLikeRepository;
    private final UserCacheService userCacheService;

    public LikeService(ContentRepository contentRepository, UserContentLikeRepository userContentLikeRepository, UserCacheService userCacheService) {
        this.contentRepository = contentRepository;
        this.userContentLikeRepository = userContentLikeRepository;
        this.userCacheService = userCacheService;
    }

    public String likeContent(String authHeader, UUID contentId){
        String subId = getSubIdFromAuthHeader(authHeader);
        ExternalUser externalUser = userCacheService.getExternalUserByUsername(getUsernameFromAuthHeader(authHeader));

        Content post = contentRepository.findByIdAndDeletedAtIsNull(contentId)
                .orElse(null);
        if(post == null){
            throw new PostNotFoundException("Post with userId " + contentId + " doesn't exists");
        }

        if (userContentLikeRepository.existsByUserIdAndContentId(externalUser.getId(), contentId)) {
            throw new UserActionForbiddenException("User " + externalUser.getUsername() + " already liked post with userId " + contentId);
        }

        UserContentLike userContentLike = externalUser.addContentLike(post);
        userContentLikeRepository.save(userContentLike);

        return String.format("User %s liked post with userId %s",subId,contentId);
    }

    public String deleteLike(String authHeader, UUID contentId){
        String username = getUsernameFromAuthHeader(authHeader);
        ExternalUser externalUser = userCacheService.getExternalUserByUsername(username);
        if(externalUser == null){
            throw new UserDoesntExistsException("Error while fetching user from database when deleting like");
        }

        Content post = contentRepository.findByIdAndDeletedAtIsNull(contentId)
                .orElse(null);
        if(post == null){
            throw new PostNotFoundException("Post with userId " + contentId + " doesn't exists");
        }

        UserContentLike userContentLike = userContentLikeRepository.findByContentAndUser_Id(post,externalUser.getId())
                .orElse(null);
        if(userContentLike == null){
            throw new UserActionForbiddenException("Post with userId " + contentId + " isn't liked by user " + externalUser.getUsername());
        }

        externalUser.removeContentLike(post);
        userCacheService.saveExternalUser(externalUser);

        return String.format("User %s unliked post with userId %s",username,contentId);
    }

    public PageResponse<ContentResponse> getAllLikedContentsByUser(String username, Pageable pageable) {
        SimpleUserDataObject externalUser = userCacheService.getUserDataByUsername(username);

        Page<UserContentLike> userContentLikeList = userContentLikeRepository
                .findAllByUser_Id(UUID.fromString(externalUser.userId()),pageable)
                .orElse(null);

        if(userContentLikeList == null || userContentLikeList.getContent().isEmpty()){
            return PageResponse.from(new PageImpl<>(new ArrayList<>(),pageable,0));
        }

        List<ContentResponse> userContentLikes = userContentLikeList.getContent().stream().map(userContentLike ->
                ContentResponse.fromEntity(userContentLike.getContent())
        ).toList();

        return PageResponse.from(new PageImpl<>(userContentLikes,pageable,userContentLikes.size()));
    }
}
