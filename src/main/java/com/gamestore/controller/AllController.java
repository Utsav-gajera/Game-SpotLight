package com.gamestore.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.gamestore.dto.GameDTO;
import com.gamestore.service.GameService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/all")
public class AllController {

    @Autowired private GameService gameService;

    // Search by title
    @GetMapping("/games/search")
    public List<GameDTO> searchByTitle(@RequestParam String title) {
        return gameService.searchByTitle(title);
    }

    // Filter by genre
    @GetMapping("/games/genre")
    public List<GameDTO> filterByGenre(@RequestParam String genre) {
        return gameService.filterByGenre(genre);
    }

    // Filter by price range
    @GetMapping("/games/price")
    public List<GameDTO> filterByPriceRange(@RequestParam Double min,
                                            @RequestParam Double max) {
        return gameService.filterByPriceRange(min, max);
    }

    // Advanced search (optional: combine filters)
    @GetMapping("/games/filter")
    public List<GameDTO> advancedSearch(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String genre,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice) {
        return gameService.advancedSearch(title, genre, minPrice, maxPrice);
    }

    @GetMapping("/games")
    public List<GameDTO> getAllGames() {
        return gameService.getAllGames();
    }


}
