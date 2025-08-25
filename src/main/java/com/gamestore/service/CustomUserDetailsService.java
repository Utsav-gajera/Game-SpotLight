package com.gamestore.service;

import com.gamestore.entity.User;
import com.gamestore.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CustomUserDetailsService implements UserDetailsService {
	@Autowired
    private UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        System.out.println("Loading user: " + username);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));

        System.out.println("User found: " + user.getUsername() + " Role: " + user.getRole().toString());
        System.out.println("Stored password (hashed): " + user.getPassword());

        return new org.springframework.security.core.userdetails.User(
            user.getUsername(),
            user.getPassword(),
            (List.of(new SimpleGrantedAuthority("ROLE_"+ user.getRole().toString())))
        );
        
    }
}

