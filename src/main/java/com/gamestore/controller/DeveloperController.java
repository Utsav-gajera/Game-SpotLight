package com.gamestore.controller;

import com.gamestore.dto.UserDTO;
import com.gamestore.entity.Role;
import com.gamestore.dto.GameDTO;
import com.gamestore.entity.Game;
import com.gamestore.entity.User;
import com.gamestore.service.GameService;
import com.gamestore.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;
import java.util.Optional;

@RestController
@RequestMapping("/api/developer")
public class DeveloperController {

    @Autowired
    private GameService gameService;

    @Autowired
    private UserService userService;

    private User getSessionUser(HttpSession session) {
        if (session == null) throw new IllegalArgumentException("No session present.");
        Object userObj = session.getAttribute("user");

        User sessionUser = (User) userObj;
        return userService.findByUsername(sessionUser.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("User not found."));
    }

    @PostMapping("/games/add")
    public ResponseEntity<?> addGame(@RequestBody GameDTO gameDTO, HttpSession session) {
        User developer = getSessionUser(session);

        GameDTO created = gameService.addGame(
                gameDTO.getTitle(),
                gameDTO.getGenre(),
                gameDTO.getPrice(),
                developer
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/games/{gameId}")
    public ResponseEntity<?> updateGame(
            @PathVariable Long gameId,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String genre,
            @RequestParam(required = false) Double price,
            HttpSession session) {

        User developer = getSessionUser(session);

        Optional<Game> gameOpt = gameService.getGameById(gameId);
        if (gameOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Game not found.");
        }

        Game game = gameOpt.get();
        if (game.getDeveloper() == null || !game.getDeveloper().getId().equals(developer.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Unauthorized to update this game.");
        }

        gameService.updateGame(gameId, title, genre, price, developer);
        return ResponseEntity.ok("Game updated successfully.");
    }

    @DeleteMapping("/games/{gameId}")
    public ResponseEntity<?> removeGame(@PathVariable Long gameId, HttpSession session) {
        User developer = getSessionUser(session);
        Optional<Game> gameOpt = gameService.getGameById(gameId);

        if (gameOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Game not found.");
        }

        Game game = gameOpt.get();
        if (game.getDeveloper() == null || !game.getDeveloper().getId().equals(developer.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Unauthorized to delete this game.");
        }

        gameService.deleteGame(gameId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/profile")
    public ResponseEntity<UserDTO> viewProfile(HttpSession session) {
        User user = getSessionUser(session);
        return ResponseEntity.ok(userService.convertToDTO(user));
    }

}
