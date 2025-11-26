package com.socialmediatraining.contentservice.service.user;

import com.socialmediatraining.contentservice.entity.ExternalUser;
import com.socialmediatraining.contentservice.repository.ExternalUserRepository;
import com.socialmediatraining.dtoutils.dto.SimpleUserDataObject;
import com.socialmediatraining.exceptioncommons.exception.UserDoesntExistsException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.Optional;
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
    public ExternalUser getExternalUserByUsername(String username) {
        return externalUserRepository.findExternalUserByUsername(username)
                .orElseThrow(() -> new UserDoesntExistsException("User not found: " + username));
    }

    @Cacheable(value = "users", key = "#username", condition = "#result != null",sync = true)
    public Optional<ExternalUser> getOptionalExternalUserByUsername(String username){
        return externalUserRepository.findExternalUserByUsername(username);
    }

    @Cacheable(value = "users", key = "#username", condition = "#result != null",sync = true)
    public ExternalUser getOrCreatNewExternalUserIfNotExists(String subId, String username){
        ExternalUser externalUser = externalUserRepository.findExternalUserByUsername(username).orElse(null);
        if(externalUser == null){
            log.info("User doesn't exists, creating it now");
            ExternalUser newUser = ExternalUser.builder()
                    .id(UUID.fromString(subId))
                    .username(username)
                    .build();
            externalUser = externalUserRepository.save(newUser);
        }
        return externalUser;
    }

    @KafkaListener(topics = "created-new-user", groupId = "content-service" )
    @Cacheable(value = "users", key = "#result.username", condition = "#result != null",sync = true)
    public ExternalUser createNewUser(SimpleUserDataObject simpleUserData) {
        ExternalUser newUser = externalUserRepository.findExternalUserByUsername(simpleUserData.username()).orElse(null);
        if(newUser != null){
            //TODO custom error throw here
            log.error("User already exists in database. This should not happen, there might be an issue in user creation flow");
            return null;
        }

        newUser = ExternalUser.builder()
                .id(UUID.fromString(simpleUserData.userId()))
                .username(simpleUserData.username())
                .build();
        externalUserRepository.save(newUser);
        log.info("Kafka topic caught -> New user created: {}", simpleUserData);
        return newUser;
    }

    @Cacheable(value = "users", key = "#result.username", condition = "#result != null",sync = true)
    public void saveUserInDb(ExternalUser user){
        externalUserRepository.save(user);
    }
}
