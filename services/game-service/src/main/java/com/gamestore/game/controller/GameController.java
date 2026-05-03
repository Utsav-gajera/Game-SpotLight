package com.gamestore.game.controller;

import com.gamestore.game.dto.GameDTO;
import com.gamestore.game.dto.ReviewRequest;
import com.gamestore.game.security.JwtService;
import com.gamestore.game.service.GameService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/games")
public class GameController {

    private final GameService gameService;
    private final JwtService jwtService;

    public GameController(GameService gameService, JwtService jwtService) {
        this.gameService = gameService;
        this.jwtService = jwtService;
    }

    @GetMapping
    public ResponseEntity<List<GameDTO>> getAllGames() {
        return ResponseEntity.ok(gameService.getAllGames());
    }

    @GetMapping("/search")
    public ResponseEntity<List<GameDTO>> searchGames(@RequestParam(required = false) String title) {
        return ResponseEntity.ok(gameService.searchGames(title));
    }

    @GetMapping("/filter")
    public ResponseEntity<List<GameDTO>> filterGames(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String genre,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice) {
        return ResponseEntity.ok(gameService.filterGames(title, genre, minPrice, maxPrice));
    }

    @GetMapping("/price")
    public ResponseEntity<List<GameDTO>> getGamesByPrice(
            @RequestParam(required = false) Double min,
            @RequestParam(required = false) Double max) {
        return ResponseEntity.ok(gameService.getGamesByPrice(min, max));
    }

    @GetMapping("/{id}")
    public ResponseEntity<GameDTO> getGameById(@PathVariable String id) {
        GameDTO game = gameService.getGameById(id);
        return game != null ? ResponseEntity.ok(game) : ResponseEntity.notFound().build();
    }

    @PostMapping("/{id}/reviews")
    public ResponseEntity<GameDTO> addReview(
            @PathVariable String id,
            @RequestBody ReviewRequest reviewRequest,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        Integer rating = reviewRequest == null ? null : reviewRequest.getRating();
        String comment = reviewRequest == null ? null : reviewRequest.getComment();

        if (rating == null || rating < 1 || rating > 5) {
            return ResponseEntity.badRequest().build();
        }

        String username = extractUsernameFromHeader(authorizationHeader);
        GameDTO updated = gameService.addReview(id, rating, comment, username);
        return updated != null ? ResponseEntity.ok(updated) : ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}/reviews")
    public ResponseEntity<GameDTO> deleteReview(
            @PathVariable String id,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        String username = extractUsernameFromHeader(authorizationHeader);
        GameDTO updated = gameService.deleteReview(id, username);
        return updated != null ? ResponseEntity.ok(updated) : ResponseEntity.notFound().build();
    }

    @GetMapping("/{id}/download-url")
    public ResponseEntity<Map<String, String>> getDownloadUrl(@PathVariable String id) {
        GameDTO game = gameService.getGameById(id);
        if (game == null || game.getGameFileUrl() == null || game.getGameFileUrl().isBlank()) {
            return ResponseEntity.notFound().build();
        }
        try {
            String signedUrl = gameService.generateSignedDownloadUrl(game.getGameFileUrl());
            return ResponseEntity.ok(Map.of("url", signedUrl));
        } catch (Exception e) {
            System.err.println("⚠️ Failed to generate signed URL: " + e.getMessage());
            return ResponseEntity.status(500).body(Map.of("error", "Failed to generate download URL"));
        }
    }

    @GetMapping("/genre/{genre}")
    public ResponseEntity<List<GameDTO>> getGamesByGenre(@PathVariable String genre) {
        return ResponseEntity.ok(gameService.getGamesByGenre(genre));
    }

    @PostMapping
    public ResponseEntity<GameDTO> createGame(@RequestBody GameDTO gameDTO) {
        GameDTO created = gameService.createGame(gameDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<GameDTO> updateGame(@PathVariable String id, @RequestBody GameDTO gameDTO) {
        GameDTO updated = gameService.updateGame(id, gameDTO);
        return updated != null ? ResponseEntity.ok(updated) : ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteGame(@PathVariable String id) {
        gameService.deleteGame(id);
        return ResponseEntity.noContent().build();
    }

    private String extractUsernameFromHeader(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return "Anonymous";
        }
        String token = authorizationHeader.substring("Bearer ".length()).trim();
        if (token.isEmpty()) {
            return "Anonymous";
        }
        try {
            return jwtService.parseUsername(token).orElse("Anonymous");
        } catch (RuntimeException ex) {
            return "Anonymous";
        }
    }
}
