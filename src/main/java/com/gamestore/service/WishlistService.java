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
        boolean exists = wishlistRepository.existsByUserAndName(user, wishlistName);
        if (exists) {
            // controllers will map "exists" -> 409 Conflict
            throw new IllegalArgumentException("Wishlist with the same name already exists.");
        }

        Wishlist wishlist = new Wishlist();
        wishlist.setName(wishlistName);
        wishlist.setUser(user);
        wishlist = wishlistRepository.save(wishlist);
        return convertToDTO(wishlist);
    }

    // Add a game to wishlist
    public String addToWishlist(User user, Long wishlistId, Long gameId) {
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new IllegalArgumentException("Game not found"));

        Wishlist wishlist = wishlistRepository.findById(wishlistId)
                .orElseThrow(() -> new IllegalArgumentException("Wishlist not found"));

        if (!wishlist.getUser().getId().equals(user.getId())) {
            // controllers will map SecurityException -> 403 Forbidden
            throw new SecurityException("Unauthorized action");
        }

        // Prevent duplicate entries (by id)
        boolean alreadyPresent = wishlist.getGames().stream()
                .anyMatch(g -> g.getId() != null && g.getId().equals(gameId));
        if (alreadyPresent) {
            throw new IllegalArgumentException("Game already exists in wishlist");
        }

        wishlist.getGames().add(game);
        wishlistRepository.save(wishlist);
        return "Game added to wishlist!";
    }

    // Show all wishlists of a user (only non-empty shown)
    public List<WishlistDTO> getWishlist(User user) {
        return wishlistRepository.findByUser(user).stream()
                .filter(w -> w.getGames() != null && !w.getGames().isEmpty())
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    // Remove a game. If wishlist becomes empty, delete the wishlist.
    public String removeFromWishlist(User user, Long wishlistId, Long gameId) {
        Wishlist wishlist = wishlistRepository.findById(wishlistId)
                .orElseThrow(() -> new IllegalArgumentException("Wishlist not found"));

        if (!wishlist.getUser().getId().equals(user.getId())) {
            throw new SecurityException("Unauthorized action");
        }

        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new IllegalArgumentException("Game not found"));

        boolean removed = wishlist.getGames().removeIf(g -> g.getId() != null && g.getId().equals(gameId));
        if (!removed) {
            // controllers will map "not in wishlist" / "not found" messages -> 404
            throw new IllegalArgumentException("Game not found in wishlist");
        }

        // If wishlist empty -> delete it and return an informative message
        if (wishlist.getGames().isEmpty()) {
            wishlistRepository.delete(wishlist);
            return "Game removed. Wishlist is now empty and has been deleted.";
        }

        wishlistRepository.save(wishlist);
        return "Game removed from wishlist!";
    }

    // Update a wishlist item (replace old game with new)
    public String updateWishlistItem(User user, Long wishlistId, Long oldGameId, Long newGameId) {
        Wishlist wishlist = wishlistRepository.findById(wishlistId)
                .orElseThrow(() -> new IllegalArgumentException("Wishlist not found"));

        if (!wishlist.getUser().getId().equals(user.getId())) {
            throw new SecurityException("Unauthorized to update this wishlist");
        }

        Game oldGame = gameRepository.findById(oldGameId)
                .orElseThrow(() -> new IllegalArgumentException("Old game not found"));
        Game newGame = gameRepository.findById(newGameId)
                .orElseThrow(() -> new IllegalArgumentException("New game not found"));

        boolean containsOld = wishlist.getGames().stream()
                .anyMatch(g -> g.getId() != null && g.getId().equals(oldGameId));
        if (!containsOld) {
            throw new IllegalArgumentException("Old game not found in wishlist");
        }

        boolean alreadyHasNew = wishlist.getGames().stream()
                .anyMatch(g -> g.getId() != null && g.getId().equals(newGameId));
        if (alreadyHasNew) {
            throw new IllegalArgumentException("New game already exists in wishlist");
        }

        wishlist.getGames().removeIf(g -> g.getId() != null && g.getId().equals(oldGameId));
        wishlist.getGames().add(newGame);
        wishlistRepository.save(wishlist);

        return "Wishlist updated successfully!";
    }

    // Helper method: Convert Wishlist → WishlistDTO
    private WishlistDTO convertToDTO(Wishlist wishlist) {
        WishlistDTO dto = modelMapper.map(wishlist, WishlistDTO.class);
        dto.setUsername(wishlist.getUser().getUsername());
        dto.setGameTitles(
                wishlist.getGames().stream()
                        .map(Game::getTitle)
                        .collect(Collectors.toSet())
        );
        return dto;
    }
}
