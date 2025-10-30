package com.socialmediatraining.authservice.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth/admin")
@PreAuthorize("@roleUtils.hasAnyAdminRole(authentication)")
public class AuthAdminController {

    @GetMapping("/welcome")
    public ResponseEntity<String> signIn() {
        return ResponseEntity.ok().body("Welcome admin");
    }
}
