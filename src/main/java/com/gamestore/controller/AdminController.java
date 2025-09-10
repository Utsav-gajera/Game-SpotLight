package com.gamestore.controller;

import com.gamestore.dto.GameDTO;
import com.gamestore.dto.PurchaseDTO;
import com.gamestore.dto.UserDTO;
import com.gamestore.entity.User;
import com.gamestore.entity.Role;
import com.gamestore.service.GameService;
import com.gamestore.service.PurchaseService;
import com.gamestore.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
        if (session == null) throw new IllegalArgumentException("No session present.");
        Object userObj = session.getAttribute("user");

        User sessionUser = (User) userObj;
        return userService.findByUsername(sessionUser.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("User not found."));
    }



    @GetMapping("/users")
    public ResponseEntity<?> getAllUsers(HttpSession session) {

        List<UserDTO> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    @GetMapping("/games")
    public ResponseEntity<?> getAllGames(HttpSession session) {

        List<GameDTO> games = gameService.getAllGames();
        return ResponseEntity.ok(games);
    }

    @DeleteMapping("/users/{userId}")
    public ResponseEntity<?> deleteUser(@PathVariable Long userId, HttpSession session) {


        userService.deleteUser(userId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/games/{gameId}")
    public ResponseEntity<?> deleteGame(@PathVariable Long gameId, HttpSession session) {


        gameService.deleteGame(gameId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/purchases")
    public ResponseEntity<?> getAllPurchases(HttpSession session) {


        List<PurchaseDTO> purchases = purchaseService.getAllPurchases();
        return ResponseEntity.ok(purchases);
    }

    @GetMapping("/profile")
    public ResponseEntity<?> viewProfile(HttpSession session) {
        User user = getSessionUser(session);
        UserDTO dto = userService.convertToDTO(user);
        return ResponseEntity.ok(dto);
    }


}
