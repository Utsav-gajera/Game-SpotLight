package com.gamestore.authuser.service;

import com.gamestore.authuser.dto.AuthResponse;
import com.gamestore.authuser.dto.LoginRequest;
import com.gamestore.authuser.dto.RegisterRequest;
import com.gamestore.authuser.dto.UserResponse;
import com.gamestore.authuser.entity.User;
import com.gamestore.authuser.repository.UserRepository;
import com.gamestore.authuser.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    public UserResponse register(RegisterRequest request) {
        if (userRepository.existsByUsernameIgnoreCase(request.username())) {
            throw new IllegalArgumentException("Username already exists");
        }
        String normalizedEmail = request.email() == null || request.email().isBlank()
                ? request.username().trim().toLowerCase() + "@game-spotlight.local"
                : request.email().trim().toLowerCase();

        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new IllegalArgumentException("Email already exists");
        }

        User user = new User();
        user.setUsername(request.username().trim());
        user.setEmail(normalizedEmail);
        user.setDisplayName(request.displayName() == null || request.displayName().isBlank() ? request.username().trim() : request.displayName().trim());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        Set<String> roles = new LinkedHashSet<>();
        String normalizedRole = normalizeRole(request.role());
        if ("ADMIN".equals(normalizedRole) && userRepository.existsByRolesContaining("ADMIN")) {
            throw new IllegalArgumentException("Only one admin account is allowed");
        }
        roles.add(normalizedRole);
        user.setRoles(roles);

        User saved = userRepository.save(user);
        return toResponse(saved);
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password())
        );

        User user = userRepository.findByUsernameIgnoreCase(request.username())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Set<String> normalizedRoles = normalizeRoles(user.getRoles());
        if (!normalizedRoles.equals(user.getRoles())) {
            user.setRoles(normalizedRoles);
            userRepository.save(user);
        }

        String token = jwtService.generateToken(user.getUsername(), normalizedRoles);
        return new AuthResponse(token, jwtService.getExpirationSeconds(), toResponse(user));
    }

    public UserResponse me(String username) {
        User user = userRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return toResponse(user);
    }

    public List<UserResponse> listUsers() {
        return userRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    public void deleteUser(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("User id is required");
        }

        userRepository.deleteById(userId);
    }

    private UserResponse toResponse(User user) {
        Set<String> normalizedRoles = normalizeRoles(user.getRoles());
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getDisplayName(),
                normalizedRoles,
                user.getCreatedAt()
        );
    }

    private Set<String> normalizeRoles(Set<String> roles) {
        if (roles == null || roles.isEmpty()) {
            return Set.of("NORMAL_USER");
        }

        return roles.stream()
                .filter(role -> role != null && !role.isBlank())
                .map(role -> role.trim().toUpperCase(Locale.ROOT))
                .map(role -> "USER".equals(role) ? "NORMAL_USER" : role)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private String normalizeRole(String role) {
        if (role == null || role.isBlank()) {
            return "NORMAL_USER";
        }

        String normalized = role.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "NORMAL_USER", "DEVELOPER", "ADMIN" -> normalized;
            case "USER" -> "NORMAL_USER";
            default -> throw new IllegalArgumentException("Unsupported role: " + role);
        };
    }
}