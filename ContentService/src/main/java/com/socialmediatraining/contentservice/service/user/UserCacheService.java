package com.socialmediatraining.contentservice.service.user;

import com.socialmediatraining.contentservice.entity.ExternalUser;
import com.socialmediatraining.contentservice.repository.ExternalUserRepository;
import com.socialmediatraining.dtoutils.dto.SimpleUserDataObject;
import com.socialmediatraining.exceptioncommons.exception.UserDoesntExistsException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpServerErrorException;

import java.util.UUID;

@Service
@Slf4j
public class UserCacheService {

    private final ExternalUserRepository externalUserRepository;

    @Autowired
    public UserCacheService(ExternalUserRepository externalUserRepository) {
        this.externalUserRepository = externalUserRepository;
    }

    @Cacheable(value = "users", key = "#username", condition = "#result != null", sync = true)
    public SimpleUserDataObject getUserDataByUsername(String username) {
        ExternalUser user = externalUserRepository.findExternalUserByUsername(username)
                .orElseThrow(() -> new UserDoesntExistsException("User not found: " + username));
        return SimpleUserDataObject.create(user.getId().toString(),user.getUsername());
    }

    @Cacheable(value = "users", key = "#username", condition = "#result != null",sync = true)
    public SimpleUserDataObject getOrCreatNewExternalUserIfNotExists(String subId, String username){
        if(subId == null || username == null){
            throw new UserDoesntExistsException("Impossible to create a user without user id, the user account probably doesn't exists");
        }

        ExternalUser externalUser = externalUserRepository.findExternalUserByUsername(username).orElse(null);
        if(externalUser == null){
            log.info("User doesn't exists, creating it now");
            ExternalUser newUser = ExternalUser.builder()
                    .id(UUID.fromString(subId))
                    .username(username)
                    .build();
            externalUser = externalUserRepository.save(newUser);
        }
        return SimpleUserDataObject.create(externalUser.getId().toString(),externalUser.getUsername());
    }

    @KafkaListener(topics = "created-new-user", groupId = "content-service" )
    @Cacheable(value = "users", key = "#result.username", condition = "#result != null",sync = true)
    public SimpleUserDataObject createNewUser(SimpleUserDataObject simpleUserData) {
        if(simpleUserData == null){
            throw new HttpServerErrorException(HttpStatus.BAD_REQUEST,"User data cannot be null when creating user");
        }

        boolean userExists = externalUserRepository.existsExternalUserByUsername(simpleUserData.username());
        if(userExists){
            throw new UserDoesntExistsException("User already exists in database. This should not happen, there might be an issue in user creation flow");
        }

        ExternalUser newUser = ExternalUser.builder()
                .id(UUID.fromString(simpleUserData.userId()))
                .username(simpleUserData.username())
                .build();
        externalUserRepository.save(newUser);
        log.info("Kafka topic caught -> New user created: {}", simpleUserData);
        return SimpleUserDataObject.create(newUser.getId().toString(),newUser.getUsername());
    }

    public ExternalUser getExternalUserByUsername(String username){
        return externalUserRepository.findExternalUserByUsername(username)
                .orElseThrow(() -> new UserDoesntExistsException("User not found: " + username));
    }

    @CacheEvict(value = "users", key = "#simpleUserData.username()")
    @KafkaListener(topics = "user-deleted", groupId = "user-service" )
    public void deleteUser(SimpleUserDataObject simpleUserData){
        externalUserRepository.deleteById(UUID.fromString(simpleUserData.userId()));
        log.info("Kafka topic caught -> User {} deleted", simpleUserData.username());
    }

    public void saveExternalUser(ExternalUser user){
        externalUserRepository.save(user);
    }
}
