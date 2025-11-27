package com.socialmediatraining.authservice.controller;

import com.socialmediatraining.authservice.dto.UserResponse;
import com.socialmediatraining.authservice.dto.UserSignUpRequest;
import com.socialmediatraining.authservice.dto.UserUpdateRequest;
import com.socialmediatraining.authservice.service.AuthService;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/v1")
@Validated
@Tag(name = "Authentication Service", description = "APIs for user operations")
public class AuthController {
    private final AuthService authService;

    @Autowired
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(summary = "Register a new user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "User created",
                    content = { @Content(mediaType = "application/json",
                            schema = @Schema(implementation = UserSignUpRequest.class)) }),
            })
    @PostMapping("/auth/signup")
    public ResponseEntity<String> signUp(@Valid @RequestBody UserSignUpRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.signUp(request));
    }

    //Sign in is handled by keycloak, and should be done through the keycloak login page, when using front end,
    // or with postman redirect for testing when using only backend.
    @Hidden
    @GetMapping("/auth/signin")
    public ResponseEntity<String> signIn() {
        return ResponseEntity.status(HttpStatus.OK).body("Sign in here !");
    }

    @Operation(summary = "Get all user information, such as email, username, date of birth, etc")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User information found successfully",
                    content = { @Content(mediaType = "application/json",
                            schema = @Schema(implementation = UserResponse.class)) }),
    })
    @GetMapping("/user")
    @PreAuthorize("@roleUtils.hasAnyUserRole(authentication)")
    public ResponseEntity<UserResponse> getAuthenticatedUserInformation(@RequestHeader("Authorization") String authHeader){
        return ResponseEntity.status(HttpStatus.OK).body(authService.getUserInformation(authHeader));
    }

    @Operation(summary = "Update user information")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User information updated successfully",
                    content = { @Content(mediaType = "application/json",
                            schema = @Schema(implementation = UserResponse.class)) }),
    })
    @PutMapping("/user")
    @PreAuthorize("@roleUtils.hasAnyUserRole(authentication)")
    public ResponseEntity<UserResponse> updateAuthenticatedUserInformation(
            @RequestHeader("Authorization") String authHeader,
            @Valid @RequestBody UserUpdateRequest request){
        return ResponseEntity.status(HttpStatus.OK).body(authService.updateUserInformation(authHeader, request));
    }

    @Operation(summary = "Delete user - Irreversible action, will delete all related data")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User deleted successfully",
                    content = { @Content(mediaType = "application/json",
                            schema = @Schema(implementation = UserResponse.class)) }),
    })
    @DeleteMapping("/user")
    @PreAuthorize("@roleUtils.hasAnyUserRole(authentication)")
    public ResponseEntity<String> deleteAuthenticatedUser(
            @RequestHeader("Authorization") String authHeader){
        return ResponseEntity.status(HttpStatus.OK).body(authService.deleteUser(authHeader));
    }
}
