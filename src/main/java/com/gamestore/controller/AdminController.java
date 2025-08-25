package com.gamestore.controller;

import com.gamestore.dto.GameDTO;
import com.gamestore.dto.PurchaseDTO;
import com.gamestore.dto.UserDTO;
import com.gamestore.entity.Purchase;
import com.gamestore.entity.User;
import com.gamestore.service.GameService;
import com.gamestore.service.PurchaseService;
import com.gamestore.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Autowired 
    private UserService userService;
    
    @Autowired 
    private GameService gameService;

    @Autowired
    private PurchaseService purchaseService;

    private User getSessionUser(HttpSession session) {

        User username = (User) session.getAttribute("user");
        return userService.findByUsername(username.getUsername()).orElseThrow(() -> new RuntimeException("User not found"));
    }

    @GetMapping("/users")
    public List<UserDTO> getAllUsers() {
        return userService.getAllUsers();
    }

    @GetMapping("/games")
    public List<GameDTO> getAllGames() {
        return gameService.getAllGames();
    }

    @DeleteMapping("/users/{userId}")
    public String deleteUser(@PathVariable Long userId) {
        userService.deleteUser(userId);
        return "User deleted successfully!";
    }	

    @DeleteMapping("/games/{gameId}")
    public String deleteGame(@PathVariable Long gameId) {
        gameService.deleteGame(gameId);
        return "Game deleted successfully!";
    }

    @GetMapping("/purchases")
    public List<PurchaseDTO> getAllPurchases() {
        return purchaseService.getAllPurchases();
    }

    @GetMapping("/profile")
    public UserDTO viewProfile(HttpSession session) {
        User user = getSessionUser(session);
        return userService.convertToDTO(user);
    }


}
