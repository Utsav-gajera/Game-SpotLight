package com.gamestore.controller;

import com.gamestore.dto.GameDTO;
import com.gamestore.service.GameService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/all")
public class AllController {

    @Autowired
    private GameService gameService;

    /**
     * Search games by title (partial match).
     * - 200 OK with results (possibly empty)
     * - 400 BAD REQUEST if title param is empty
     */
    @GetMapping("/games/search")
    public ResponseEntity<List<GameDTO>> searchByTitle(@RequestParam String title) {
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("Query parameter 'title' must not be empty.");
        }
        List<GameDTO> results = gameService.searchByTitle(title.trim());
        return ResponseEntity.ok(results);
    }

    /**
     * Filter by genre.
     * - 200 OK with results (possibly empty)
     * - 400 BAD REQUEST if genre param is empty
     */
    @GetMapping("/games/genre")
    public ResponseEntity<List<GameDTO>> filterByGenre(@RequestParam String genre) {
        if (genre == null || genre.trim().isEmpty()) {
            throw new IllegalArgumentException("Query parameter 'genre' must not be empty.");
        }
        List<GameDTO> results = gameService.filterByGenre(genre.trim());
        return ResponseEntity.ok(results);
    }

    /**
     * Filter by price range.
     * - If min or max is omitted, sensible defaults are used (min = 0.0, max = Double.MAX_VALUE).
     * - 200 OK with results (possibly empty)
     * - 400 BAD REQUEST if min > max
     */
    @GetMapping("/games/price")
    public ResponseEntity<List<GameDTO>> filterByPriceRange(
            @RequestParam(required = false) Double min,
            @RequestParam(required = false) Double max) {

        double minVal = (min == null) ? 0.0 : min;
        double maxVal = (max == null) ? Double.MAX_VALUE : max;

        if (minVal < 0 || maxVal < 0) {
            throw new IllegalArgumentException("Price values must be non-negative.");
        }
        if (minVal > maxVal) {
            throw new IllegalArgumentException("min price cannot be greater than max price.");
        }

        List<GameDTO> results = gameService.filterByPriceRange(minVal, maxVal);
        return ResponseEntity.ok(results);
    }

    /**
     * Advanced search combining optional filters.
     * Any combination of title, genre, minPrice, maxPrice may be provided.
     * - 200 OK with results (possibly empty)
     * - 400 BAD REQUEST if minPrice > maxPrice
     */
    @GetMapping("/games/filter")
    public ResponseEntity<List<GameDTO>> advancedSearch(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String genre,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice) {

        double minVal = (minPrice == null) ? 0.0 : minPrice;
        double maxVal = (maxPrice == null) ? Double.MAX_VALUE : maxPrice;

        if (minVal < 0 || maxVal < 0) {
            throw new IllegalArgumentException("Price values must be non-negative.");
        }
        if (minVal > maxVal) {
            throw new IllegalArgumentException("minPrice cannot be greater than maxPrice.");
        }

        List<GameDTO> results = gameService.advancedSearch(
                (title == null || title.trim().isEmpty()) ? null : title.trim(),
                (genre == null || genre.trim().isEmpty()) ? null : genre.trim(),
                (minPrice == null) ? null : minVal,
                (maxPrice == null) ? null : maxVal
        );

        return ResponseEntity.ok(results);
    }

    /**
     * Get all games
     */
    @GetMapping("/games")
    public ResponseEntity<List<GameDTO>> getAllGames() {
        List<GameDTO> all = gameService.getAllGames();
        return ResponseEntity.ok(all);
    }


}
