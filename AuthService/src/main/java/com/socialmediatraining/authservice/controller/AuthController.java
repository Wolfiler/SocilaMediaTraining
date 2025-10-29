package com.socialmediatraining.authservice.controller;

import com.socialmediatraining.authservice.dto.UserSignUpRequest;
import com.socialmediatraining.authservice.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    @Autowired
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/signup")
    public ResponseEntity<String> signUp(@RequestBody UserSignUpRequest request) {
        return ResponseEntity.created(URI.create("/api/v1/auth/signup")).body(authService.signUp(request));
    }

    @GetMapping("/welcome")
    public ResponseEntity<String> welcome() {
        return ResponseEntity.ok().body("Welcome user !");
    }

    //Sign in is handled by keycloak, and should be done through the keycloak login page, when using front end.
    @GetMapping("/signin")
    public ResponseEntity<String> signIn() {
        return ResponseEntity.ok().body("Sign in here !");
    }

}
