package com.socialmediatraining.authservice.handler;

import com.socialmediatraining.authservice.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.stereotype.Component;

@Component
public class CustomLogoutHandler implements LogoutHandler {
    private final AuthService authService;
    @Autowired
    public CustomLogoutHandler(AuthService authService) {
        this.authService = authService;
    }

    @Override
    //TODO sending the token in http with a param is not the most secure way to do it.
    public void logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        authService.logout(request.getHeader("Authorization"),request.getParameter("refresh_token"));
    }
}

