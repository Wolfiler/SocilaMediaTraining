package com.socialmediatraining.contentservice.unittests.service.user;

import com.socialmediatraining.contentservice.entity.ExternalUser;
import com.socialmediatraining.contentservice.repository.ExternalUserRepository;
import com.socialmediatraining.contentservice.service.user.UserCacheService;
import com.socialmediatraining.dtoutils.dto.SimpleUserDataObject;
import com.socialmediatraining.exceptioncommons.exception.UserDoesntExistsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.HttpServerErrorException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserCacheServiceTest {
    @Mock
    private ExternalUserRepository userRepository;

    @InjectMocks
    private UserCacheService userCacheService;
    private ExternalUser testUser;

    @BeforeEach
    void setUp() {
        testUser = new ExternalUser();
        testUser.setId(UUID.randomUUID());
        testUser.setUsername("testuser");
    }

    @Test
    void getUserDataByUsername_WhenUserExists_ShouldReturnUserData() {
        when(userRepository.findExternalUserByUsername(testUser.getUsername())).thenReturn(Optional.of(testUser));

        SimpleUserDataObject result = userCacheService.getUserDataByUsername(testUser.getUsername());

        assertThat(result).isNotNull();
        assertThat(result.userId()).isEqualTo(testUser.getId().toString());
        assertThat(result.username()).isEqualTo(testUser.getUsername());
    }

    @Test
    void getUserDataByUsername_WhenUserNotExists_ShouldThrowException() {
        when(userRepository.findExternalUserByUsername(testUser.getUsername())).thenReturn(Optional.empty());

        assertThrows(UserDoesntExistsException.class,
                () -> userCacheService.getUserDataByUsername(testUser.getUsername())
        );
    }

    @Test
    void getOrCreateNewExternalUserIfNotExists_WhenUserExists_ShouldReturnExistingUser() {
        when(userRepository.findExternalUserByUsername(testUser.getUsername())).thenReturn(Optional.of(testUser));

        SimpleUserDataObject result = userCacheService
                .getOrCreatNewExternalUserIfNotExists("dummy", testUser.getUsername());

        assertThat(result).isNotNull();
        assertThat(result.userId()).isEqualTo(testUser.getId().toString());
    }

    @Test
    void getOrCreateNewExternalUserIfNotExists_WhenUserNotExists_ShouldCreateNewUser() {
        when(userRepository.findExternalUserByUsername(testUser.getUsername())).thenReturn(Optional.empty());
        when(userRepository.save(any(ExternalUser.class))).thenReturn(testUser);

        SimpleUserDataObject result = userCacheService
                .getOrCreatNewExternalUserIfNotExists(testUser.getId().toString(), testUser.getUsername());

        assertThat(result).isNotNull();
        assertThat(result.username()).isEqualTo(testUser.getUsername());
    }

    @Test
    void createNewUser_ShouldSaveAndReturnUser() {
        SimpleUserDataObject userData = new SimpleUserDataObject(testUser.getId().toString(), testUser.getUsername());
        when(userRepository.save(any(ExternalUser.class))).thenReturn(testUser);

        SimpleUserDataObject result = userCacheService.createNewUser(userData);

        assertThat(result).isNotNull();
        assertThat(result.userId()).isEqualTo(testUser.getId().toString());
        assertThat(result.username()).isEqualTo(testUser.getUsername());
    }

    @Test
    void getUserDataByUsername_WhenUsernameIsNull_ShouldThrowException() {
        assertThrows(UserDoesntExistsException.class,
                () -> userCacheService.getUserDataByUsername(null)
        );
    }

    @Test
    void getUserDataByUsername_WhenUsernameIsEmpty_ShouldThrowException() {
        assertThrows(UserDoesntExistsException.class,
                () -> userCacheService.getUserDataByUsername("")
        );
    }

    @Test
    void getOrCreateNewExternalUserIfNotExists_WhenSubIdIsNull_ShouldThrowException() {
        assertThrows(UserDoesntExistsException.class,
                () -> userCacheService.getOrCreatNewExternalUserIfNotExists(null, testUser.getUsername())
        );
    }

    @Test
    void getOrCreateNewExternalUserIfNotExists_WhenUsernameIsNull_ShouldThrowException() {
        assertThrows(UserDoesntExistsException.class,
                () -> userCacheService.getOrCreatNewExternalUserIfNotExists(testUser.getId().toString(), null)
        );
    }

    @Test
    void getOrCreateNewExternalUserIfNotExists_WhenSubIdIsInvalid_ShouldThrowException() {
        assertThrows(IllegalArgumentException.class,
                () -> userCacheService.getOrCreatNewExternalUserIfNotExists("invalid-uuid", testUser.getUsername())
        );
    }

    @Test
    void getOrCreateNewExternalUserIfNotExists_WhenSaveFails_ShouldPropagateException() {
        when(userRepository.findExternalUserByUsername(testUser.getUsername())).thenReturn(Optional.empty());
        when(userRepository.save(any(ExternalUser.class))).thenThrow(new RuntimeException("Database error"));

        assertThrows(RuntimeException.class,
                () -> userCacheService.getOrCreatNewExternalUserIfNotExists(testUser.getId().toString(), testUser.getUsername())
        );
    }

    @Test
    void createNewUser_WhenUserDataIsNull_ShouldThrowException() {
        assertThrows(HttpServerErrorException.class,
                () -> userCacheService.createNewUser(null)
        );
    }
}