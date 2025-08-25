package com.gamestore.service;

import com.gamestore.dto.UserDTO;

import com.gamestore.entity.Role;
import com.gamestore.entity.User;
import com.gamestore.repository.UserRepository;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class UserService {
    @Autowired private UserRepository userRepository;
    @Autowired private ModelMapper modelMapper;
    @Autowired private BCryptPasswordEncoder passwordEncoder;  
    public UserDTO registerUser(String username, String password, Role role) {
        if (userRepository.findByUsername(username).isPresent()) {
            throw new RuntimeException("Username already exists");
        }
        User user = new User();

        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(role);
        user = userRepository.save(user);
        return modelMapper.map(user, UserDTO.class);
    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public UserDTO convertToDTO(User user) {
        return modelMapper.map(user, UserDTO.class);
    }
    
    public List<UserDTO> getAllUsers() {
        return userRepository.findAll()
            .stream()
            .map(user -> modelMapper.map(user, UserDTO.class))
            .collect(Collectors.toList());
    }
    
    public void deleteUser(Long userId) {
        userRepository.deleteById(userId);
    }
}