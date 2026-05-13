package com.gamestore.authuser.service;

import com.gamestore.authuser.dto.AuthResponse;
import com.gamestore.authuser.dto.LoginRequest;
import com.gamestore.authuser.dto.RegisterRequest;
import com.gamestore.authuser.dto.UserResponse;
import com.gamestore.authuser.entity.User;
import com.gamestore.authuser.repository.UserRepository;
import com.gamestore.authuser.security.JwtService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

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
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String supabaseUrl;
    private final String supabaseAnonKey;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       JwtService jwtService,
                       ObjectMapper objectMapper,
                       @Value("${supabase.url}") String supabaseUrl,
                       @Value("${supabase.anon-key}") String supabaseAnonKey) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.objectMapper = objectMapper;
        this.restTemplate = new RestTemplate();
        this.supabaseUrl = supabaseUrl;
        this.supabaseAnonKey = supabaseAnonKey;
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

    public AuthResponse exchangeSupabaseToken(String accessToken) {
        if (accessToken == null || accessToken.isBlank()) {
            throw new IllegalArgumentException("Supabase access token is required");
        }

        JsonNode supabaseUser = fetchSupabaseUser(accessToken.trim());
        String email = textValue(supabaseUser, "email");
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Supabase user email is required");
        }

        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        String displayName = resolveDisplayName(supabaseUser);
        User user;

        synchronized (this) {
            user = userRepository.findByEmailIgnoreCase(normalizedEmail).orElse(null);
            if (user == null) {
                user = createSupabaseUser(normalizedEmail, displayName);
            }
        }

        if ((displayName != null && !displayName.isBlank()) && (user.getDisplayName() == null || user.getDisplayName().isBlank())) {
            user.setDisplayName(displayName.trim());
            user = userRepository.save(user);
        }

        Set<String> normalizedRoles = normalizeRoles(user.getRoles());
        if (!normalizedRoles.equals(user.getRoles())) {
            user.setRoles(normalizedRoles);
            user = userRepository.save(user);
        }

        String token = jwtService.generateToken(user.getUsername(), normalizedRoles);
        return new AuthResponse(token, jwtService.getExpirationSeconds(), toResponse(user));
    }

    public UserResponse me(String username) {
        User user = userRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return toResponse(user);
    }

    public AuthResponse becomeDeveloper(String username) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("User name is required");
        }

        User user = userRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Set<String> normalizedRoles = normalizeRoles(user.getRoles());
        if (!normalizedRoles.contains("DEVELOPER") && !normalizedRoles.contains("ADMIN")) {
            Set<String> promotedRoles = new LinkedHashSet<>();
            promotedRoles.add("DEVELOPER");
            promotedRoles.addAll(normalizedRoles);
            user.setRoles(promotedRoles);
            user = userRepository.save(user);
            normalizedRoles = normalizeRoles(user.getRoles());
        } else if (!normalizedRoles.equals(user.getRoles())) {
            user.setRoles(normalizedRoles);
            user = userRepository.save(user);
            normalizedRoles = normalizeRoles(user.getRoles());
        }

        String token = jwtService.generateToken(user.getUsername(), normalizedRoles);
        return new AuthResponse(token, jwtService.getExpirationSeconds(), toResponse(user));
    }

    public UserResponse bootstrapFirstAdmin(RegisterRequest request) {
        if (userRepository.existsByRolesContaining("ADMIN")) {
            throw new IllegalArgumentException("Admin account already exists. Use the normal registration flow.");
        }

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
        user.setRoles(new LinkedHashSet<>(Set.of("ADMIN")));

        User saved = userRepository.save(user);
        return toResponse(saved);
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

    private JsonNode fetchSupabaseUser(String accessToken) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            headers.set("apikey", supabaseAnonKey);
            headers.setContentType(MediaType.APPLICATION_JSON);

            ResponseEntity<String> response = restTemplate.exchange(
                    supabaseUrl + "/auth/v1/user",
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    String.class
            );

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null || response.getBody().isBlank()) {
                throw new IllegalArgumentException("Unable to verify Supabase session");
            }

            return objectMapper.readTree(response.getBody());
        } catch (RestClientException ex) {
            throw new IllegalArgumentException("Unable to verify Supabase session", ex);
        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalArgumentException("Unable to parse Supabase profile", ex);
        }
    }

    private User createSupabaseUser(String email, String displayName) {
        User user = new User();
        user.setEmail(email);
        user.setDisplayName(displayName == null || displayName.isBlank() ? email : displayName.trim());
        user.setUsername(generateUniqueUsername(user.getDisplayName(), email));
        user.setPasswordHash(passwordEncoder.encode(java.util.UUID.randomUUID().toString()));
        user.setRoles(new LinkedHashSet<>(Set.of("NORMAL_USER")));
        return userRepository.save(user);
    }

    private String generateUniqueUsername(String displayName, String email) {
        String base = sanitizeUsername(displayName);
        if (base.isBlank()) {
            base = sanitizeUsername(email.contains("@") ? email.substring(0, email.indexOf('@')) : email);
        }
        if (base.isBlank()) {
            base = "player";
        }

        String candidate = base;
        int suffix = 1;
        while (userRepository.existsByUsernameIgnoreCase(candidate)) {
            candidate = base + suffix;
            suffix++;
        }
        return candidate;
    }

    private String sanitizeUsername(String value) {
        if (value == null) {
            return "";
        }

        String normalized = value.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "")
                .trim();
        return normalized.length() > 24 ? normalized.substring(0, 24) : normalized;
    }

    private String resolveDisplayName(JsonNode supabaseUser) {
        String email = textValue(supabaseUser, "email");
        String displayName = textValue(supabaseUser.path("user_metadata"), "full_name");
        if (displayName == null || displayName.isBlank()) {
            displayName = textValue(supabaseUser.path("user_metadata"), "name");
        }
        if (displayName == null || displayName.isBlank()) {
            displayName = textValue(supabaseUser.path("user_metadata"), "preferred_username");
        }
        if (displayName == null || displayName.isBlank()) {
            displayName = email;
        }
        return displayName;
    }

    private String textValue(JsonNode node, String fieldName) {
        if (node == null || fieldName == null || node.isMissingNode()) {
            return null;
        }
        JsonNode value = node.get(fieldName);
        return value == null || value.isNull() ? null : value.asText();
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