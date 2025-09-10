package com.gamestore.service;

import com.gamestore.dto.GameDTO;

import com.gamestore.entity.Game;
import com.gamestore.entity.User;
import com.gamestore.repository.GameRepository;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class GameService {
    @Autowired private GameRepository gameRepository;
    @Autowired private ModelMapper modelMapper;

    public List<GameDTO> getAllGames() {
        return gameRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public Optional<Game> getGameById(Long id) {
        return gameRepository.findById(id);
    }

    public GameDTO addGame(String title, String genre, double price,User developer) {
        Game game = new Game();
        game.setTitle(title);
        game.setGenre(genre);
        game.setPrice(price);
        game.setDeveloper(developer);
        game = gameRepository.save(game);
        return convertToDTO(game);
    }



    private GameDTO convertToDTO(Game game) {
        GameDTO dto = modelMapper.map(game, GameDTO.class);
        dto.setDeveloperUsername(game.getDeveloper().getUsername());
        return dto;
    }


    public void updateGame(Long gameId, String title, String genre, Double price, User developer) {
        Game existingGame = gameRepository.findById(gameId)
                .orElseThrow(() -> new RuntimeException("Game not found"));

        // Ensure only the developer who created the game can update it
        if (!existingGame.getDeveloper().getId().equals(developer.getId())) {
            throw new RuntimeException("Unauthorized to update this game");
        }

        // Update only if new values are provided
        if (title != null && !title.isEmpty()) {
            existingGame.setTitle(title);
        }
        if (genre != null && !genre.isEmpty()) {
            existingGame.setGenre(genre);
        }
        if (price != null) {
            existingGame.setPrice(price);
        }

        existingGame = gameRepository.save(existingGame);
        convertToDTO(existingGame);
    }


    public void deleteGame(Long gameId) {
        gameRepository.deleteById(gameId);
    }


    public List<GameDTO> searchByTitle(String title) {
        return gameRepository.findByTitleContainingIgnoreCase(title)
                .stream().map(g -> modelMapper.map(g, GameDTO.class))
                .collect(Collectors.toList());
    }

    public List<GameDTO> filterByGenre(String genre) {
        return gameRepository.findByGenreIgnoreCase(genre)
                .stream().map(g -> modelMapper.map(g, GameDTO.class))
                .collect(Collectors.toList());
    }

    public List<GameDTO> filterByPriceRange(Double min, Double max) {
        return gameRepository.findByPriceBetween(min, max)
                .stream().map(g -> modelMapper.map(g, GameDTO.class))
                .collect(Collectors.toList());
    }

    public List<GameDTO> advancedSearch(String title, String genre, Double min, Double max) {
        return gameRepository.searchGames(title, genre, min, max)
                .stream().map(g -> modelMapper.map(g, GameDTO.class))
                .collect(Collectors.toList());
    }

}
