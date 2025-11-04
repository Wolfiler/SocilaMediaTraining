package com.socialmediatraining.authservice.controller;

import com.socialmediatraining.authservice.dto.UserResponse;
import com.socialmediatraining.authservice.dto.UserSignUpRequest;
import com.socialmediatraining.authservice.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/v1")
@Validated
public class AuthController {//Might be worth it to split the controller into User controller and general controller

    private final AuthService authService;

    @Autowired
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/auth/signup")
    public ResponseEntity<String> signUp(@Valid @RequestBody UserSignUpRequest request) {
        return ResponseEntity.created(URI.create("/api/v1/auth/signup")).body(authService.signUp(request));
    }

    @GetMapping("/auth/welcome")
    @PreAuthorize("@roleUtils.hasAnyUserRole(authentication)")
    public ResponseEntity<String> welcome() {
        return ResponseEntity.ok().body("Welcome user !");
    }

    //Sign in is handled by keycloak, and should be done through the keycloak login page, when using front end.
    @GetMapping("/auth/signin")
    public ResponseEntity<String> signIn() {
        return ResponseEntity.ok().body("Sign in here !");
    }

    @GetMapping("/user/")
    public ResponseEntity<UserResponse> getUserInformation(@RequestHeader("Authorization") String authHeader){
        return ResponseEntity.ok().body(authService.getUserInformation(authHeader));
    }
}
