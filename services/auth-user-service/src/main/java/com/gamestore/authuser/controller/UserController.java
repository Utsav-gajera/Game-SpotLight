package com.gamestore.authuser.controller;

import com.gamestore.authuser.dto.UserResponse;
import com.gamestore.authuser.service.AuthService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final AuthService authService;

    public UserController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/profile")
    public UserResponse profile(Authentication authentication) {
        return authService.me(authentication.getName());
    }
}