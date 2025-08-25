package com.gamestore.service;

import com.gamestore.dto.WishlistDTO;
import com.gamestore.entity.Game;
import com.gamestore.entity.User;
import com.gamestore.entity.Wishlist;
import com.gamestore.repository.GameRepository;
import com.gamestore.repository.WishlistRepository;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class WishlistService {
    @Autowired private WishlistRepository wishlistRepository;
    @Autowired private GameRepository gameRepository;
    @Autowired private ModelMapper modelMapper;

    // Create Wishlist
    public WishlistDTO createWishlist(User user, String wishlistName) {
        Wishlist wishlist = new Wishlist();
        wishlist.setName(wishlistName);
        wishlist.setUser(user);
        wishlist = wishlistRepository.save(wishlist);
        return convertToDTO(wishlist);
    }

    // Add a game to wishlist
    public String addToWishlist(User user, Long wishlistId, Long gameId) {
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new RuntimeException("Game not found"));

        Wishlist wishlist = wishlistRepository.findById(wishlistId)
                .orElseThrow(() -> new RuntimeException("Wishlist not found"));

        if (!wishlist.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Unauthorized action");
        }

        wishlist.getGames().add(game);
        wishlistRepository.save(wishlist);
        return "Game added to wishlist!";
    }

    // Show all wishlists of a user
    public List<WishlistDTO> getWishlist(User user) {
        return wishlistRepository.findByUser(user).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    // Remove a game
    public String removeFromWishlist(User user, Long wishlistId, Long gameId) {
        Wishlist wishlist = wishlistRepository.findById(wishlistId)
                .orElseThrow(() -> new RuntimeException("Wishlist not found"));

        if (!wishlist.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Unauthorized action");
        }

        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new RuntimeException("Game not found"));

        wishlist.getGames().remove(game);
        wishlistRepository.save(wishlist);
        return "Game removed from wishlist!";
    }

    // Update a wishlist item
    public String updateWishlistItem(User user, Long wishlistId, Long oldGameId, Long newGameId) {
        Wishlist wishlist = wishlistRepository.findById(wishlistId)
                .orElseThrow(() -> new RuntimeException("Wishlist not found"));

        if (!wishlist.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Unauthorized to update this wishlist");
        }

        Game oldGame = gameRepository.findById(oldGameId)
                .orElseThrow(() -> new RuntimeException("Old game not found"));
        Game newGame = gameRepository.findById(newGameId)
                .orElseThrow(() -> new RuntimeException("New game not found"));

        if (!wishlist.getGames().contains(oldGame)) {
            throw new RuntimeException("Old game not found in wishlist");
        }

        wishlist.getGames().remove(oldGame);
        wishlist.getGames().add(newGame);
        wishlistRepository.save(wishlist);

        return "Wishlist updated successfully!";
    }

    // ✅ Helper method: Convert Wishlist → WishlistDTO
    private WishlistDTO convertToDTO(Wishlist wishlist) {
        WishlistDTO dto = modelMapper.map(wishlist, WishlistDTO.class);
        dto.setUsername(wishlist.getUser().getUsername()); // map user → username
        dto.setGameTitles(
                wishlist.getGames().stream()
                        .map(Game::getTitle)
                        .collect(Collectors.toSet())
        );
        return dto;
    }
}
