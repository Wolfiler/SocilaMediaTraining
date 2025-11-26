package com.socialmediatraining.userservice.service;

import com.socialmediatraining.authenticationcommons.JwtUtils;
import com.socialmediatraining.dtoutils.dto.SimpleUserDataObject;
import com.socialmediatraining.dtoutils.dto.UserFollowNotification;
import com.socialmediatraining.exceptioncommons.exception.UserActionForbiddenException;
import com.socialmediatraining.exceptioncommons.exception.UserDoesntExistsException;
import com.socialmediatraining.userservice.dto.ExternalUserResponse;
import com.socialmediatraining.userservice.entity.ExternalUser;
import com.socialmediatraining.userservice.entity.ExternalUserFollow;
import com.socialmediatraining.userservice.repository.ExternalUserFollowRepository;
import com.socialmediatraining.userservice.repository.ExternalUserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
@Slf4j
public class FollowService {

    private final ExternalUserRepository externalUserRepository;
    private final ExternalUserFollowRepository userFollowRepository;

    private final KafkaTemplate<String, UserFollowNotification> kafkaTemplate;

    @Autowired
    public FollowService(ExternalUserRepository followRepository, ExternalUserFollowRepository userFollowRepository, KafkaTemplate<String, UserFollowNotification> kafkaTemplate) {
        this.externalUserRepository = followRepository;
        this.userFollowRepository = userFollowRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @KafkaListener(topics = "created-new-content", groupId = "user-service" )
    public void updateUserLastActivity(SimpleUserDataObject simpleUserData) {
        ExternalUser newUser = externalUserRepository.findExternalUserByUsername(simpleUserData.username()).orElse(null);
        if(newUser == null){
            log.error("User not in database. This should not happen, there might be an issue in user creation flow");
            return;
        }

        newUser.setLastActivityAt(LocalDateTime.now());
        externalUserRepository.save(newUser);
        log.info("Kafka topic caught -> User {} activity updated", simpleUserData);
    }

    @KafkaListener(topics = "created-new-user", groupId = "user-service" )
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

    public String followUser(String username, String token) {
        String userUsername = JwtUtils.getUsernameFromAuthHeader(token);
        if(userUsername.equals(username)){
            throw new UserActionForbiddenException("You cannot follow yourself");
        }

        ExternalUser userToFollow = externalUserRepository.findExternalUserByUsername(username).orElse(null);
        if(userToFollow == null){
            throw new UserDoesntExistsException("User " + username + " to follow not found");
        }

        ExternalUser user = externalUserRepository.findExternalUserByUsername(userUsername).orElse(null);
        if(user == null){
            throw new UserDoesntExistsException("Logged user " + userUsername + " doesn't exists in database");
        }

        boolean alreadyFollowing = user.getFollowing().stream().anyMatch(f -> f.getFollowedUserId().equals(userToFollow));
        if (alreadyFollowing) {
            throw new UserActionForbiddenException(user.getUsername() +  " is already following " + username);
        }

        ExternalUserFollow userFollow = user.follow(userToFollow);
        userFollowRepository.save(userFollow);

        kafkaTemplate.send("new-follower", UserFollowNotification.create(
                userToFollow.getUserId().toString(),userToFollow.getUsername(),
                user.getUserId().toString(),user.getUsername()));

        return String.format("User %s followed user %s successfully", userUsername, username);
    }

    public String unfollowUser(String username, String token) {
        String userUsername = JwtUtils.getUsernameFromAuthHeader(token);
        if(userUsername.equals(username)){
            throw new UserActionForbiddenException("You cannot follow yourself");
        }

        ExternalUser userToFollow = externalUserRepository.findExternalUserByUsername(username).orElse(null);
        if(userToFollow == null){
            throw new UserDoesntExistsException("User " + username + " to follow not found");
        }

        ExternalUser user = externalUserRepository.findExternalUserByUsername(userUsername).orElse(null);
        if(user == null){
            throw new UserDoesntExistsException("Logged user " + userUsername + " doesn't exists in database");
        }

        user.unfollow(userToFollow);
        externalUserRepository.save(user);

        return String.format("User %s unfollowed user %s successfully", userUsername, username);
    }

    public Page<ExternalUserResponse> getAllFollowersOfUser(String username, Pageable pageable) {
        ExternalUser user = externalUserRepository.findExternalUserByUsername(username).orElse(null);
        if(user == null){
            throw new UserDoesntExistsException("User " + username + " doesn't exists");
        }

        List<ExternalUserResponse> followersList =  user.getFollowers().stream().map(
                externalUserFollow ->
                        ExternalUserResponse.create(
                                externalUserFollow.getFollowingUserId().getUserId().toString(),
                                externalUserFollow.getFollowingUserId().getUsername()
                        )
        ).toList();
        return new PageImpl<>(followersList,pageable,followersList.size());
    }

    public List<ExternalUserResponse> getAllFollowOfUser(String username, int limit, String orderBy) {
        ExternalUser user = externalUserRepository.findExternalUserByUsername(username).orElse(null);
        if(user == null){
            throw new UserDoesntExistsException("User " + username + " doesn't exists");
        }

        List<ExternalUser> followedUsers = new ArrayList<>(user.getFollowing().stream().map(ExternalUserFollow::getFollowedUserId).toList());

        switch (orderBy){
            case "activity":
                followedUsers.sort(Comparator.comparing(
                        userToSort -> userToSort.getLastActivityAt() != null ? userToSort.getLastActivityAt() : LocalDateTime.MIN,
                        Comparator.nullsLast(Comparator.naturalOrder())
                ));
                break;
            case "none":
                break;
            default:
                throw new IllegalArgumentException("Invalid order by value: " + orderBy);
        }
        followedUsers.subList(0, Math.min(limit, followedUsers.size()));

        return followedUsers.stream().map(
                followedUser ->
                        ExternalUserResponse.create(
                                followedUser.getUserId().toString(),
                                followedUser.getUsername()
                        )
        ).toList();
    }
}
