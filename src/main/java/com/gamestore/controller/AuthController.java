package com.gamestore.controller;

import ch.qos.logback.core.net.SyslogOutputStream;
import com.gamestore.dto.LoginRequest;

import com.gamestore.dto.RegisterRequest;
import com.gamestore.entity.Role;
import com.gamestore.entity.User;
import com.gamestore.service.CustomUserDetailsService;
import com.gamestore.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    
    @Autowired 
    private UserService userService;
    
    @Autowired 
    private AuthenticationManager authenticationManager;
    
    @Autowired 
    private CustomUserDetailsService userDetailsService;
    
    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody RegisterRequest registerRequest,HttpSession session) {
        if(registerRequest.getRole().equals("ADMIN")) {
            return ResponseEntity.ok("You can not register as ADMIN");
        }

        User existingUser = (User) session.getAttribute("user");

        if(registerRequest.getUsername().equals(existingUser.getUsername())){
            return ResponseEntity.badRequest().body("Registration failed: You are already logged in" );
        }

        System.out.println(registerRequest.getRole());
        try {
            String username = registerRequest.getUsername();
            String password = registerRequest.getPassword();
            String roleStr = registerRequest.getRole();
            Role role = Role.valueOf(roleStr.toUpperCase());
            
            userService.registerUser(username, password, role);
            return ResponseEntity.ok("User registered successfully!");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Registration failed: " + e.getMessage());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody LoginRequest loginRequest, HttpSession session) {
        try {
            // check if someone is already logged in
            User existingUser = (User) session.getAttribute("user");



            if (existingUser != null) {
                if(existingUser.getUsername().equals(loginRequest.getUsername())){
                    return ResponseEntity.badRequest()
                            .body("You are already logged in.");
                }
                return ResponseEntity.badRequest()
                        .body("Another user (" + existingUser.getUsername() + ") is already logged in. Please logout first.");
            }

            //System.out.println("Attempting login for: " + loginRequest.getUsername());

            UsernamePasswordAuthenticationToken authenticationToken =
                    new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword());

            Authentication authentication = authenticationManager.authenticate(authenticationToken);

            SecurityContextHolder.getContext().setAuthentication(authentication);

            session.setAttribute("SPRING_SECURITY_CONTEXT", SecurityContextHolder.getContext());

            System.out.println("Authentication successful for user: " + authentication.getName());

            User user = userService.findByUsername(loginRequest.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found"));
            session.setAttribute("user", user);

            return ResponseEntity.ok("Login successful!");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Login failed: " + e.getMessage());
        }
    }


    @PostMapping("/custom-logout")
    public ResponseEntity<String> logout(HttpSession session) {
        session.invalidate();
        SecurityContextHolder.clearContext();
        return ResponseEntity.ok("Logged out successfully!");
    }




}
