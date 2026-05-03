package com.gamestore.game.service;

import com.gamestore.game.dto.GameDTO;
import com.gamestore.game.dto.ReviewDTO;
import com.gamestore.game.entity.Game;
import com.gamestore.game.entity.Review;
import com.gamestore.game.repository.GameRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class GameService {

    private final GameRepository gameRepository;
    private final RestTemplate restTemplate;
    
    @Value("${storage.service.url:http://localhost:8085/api}")
    private String storageServiceUrl;

    public GameService(GameRepository gameRepository, RestTemplate restTemplate) {
        this.gameRepository = gameRepository;
        this.restTemplate = restTemplate;
    }

    public List<GameDTO> getAllGames() {
        return gameRepository.findAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public GameDTO getGameById(String id) {
        return gameRepository.findById(id)
                .map(this::toDTO)
                .orElse(null);
    }

    public GameDTO addReview(String id, Integer rating, String comment, String username) {
        return gameRepository.findById(id).map(game -> {
            if (game.getReviews() == null) {
                game.setReviews(new ArrayList<>());
            }
            String reviewerName = normalizeReviewerName(username);
            Review review = new Review(reviewerName, rating, comment, Instant.now());
            int existingIndex = findReviewIndex(game.getReviews(), reviewerName);
            if (existingIndex >= 0) {
                game.getReviews().set(existingIndex, review);
            } else {
                game.getReviews().add(review);
            }
            Game saved = gameRepository.save(game);
            return toDTO(saved);
        }).orElse(null);
    }

    public GameDTO deleteReview(String id, String username) {
        return gameRepository.findById(id).map(game -> {
            if (game.getReviews() == null || game.getReviews().isEmpty()) {
                return toDTO(game);
            }
            String reviewerName = normalizeReviewerName(username);
            int existingIndex = findReviewIndex(game.getReviews(), reviewerName);
            if (existingIndex >= 0) {
                game.getReviews().remove(existingIndex);
                Game saved = gameRepository.save(game);
                return toDTO(saved);
            }
            return toDTO(game);
        }).orElse(null);
    }

    public List<GameDTO> getGamesByGenre(String genre) {
        return gameRepository.findByGenre(genre).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public List<GameDTO> searchGames(String title) {
        if (title == null || title.isBlank()) {
            return getAllGames();
        }

        return gameRepository.findByTitleContainingIgnoreCase(title).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public List<GameDTO> filterGames(String title, String genre, Double minPrice, Double maxPrice) {
        return gameRepository.findAll().stream()
                .filter(game -> title == null || title.isBlank() || containsIgnoreCase(game.getTitle(), title))
                .filter(game -> genre == null || genre.isBlank() || equalsIgnoreCase(game.getGenre(), genre))
                .filter(game -> minPrice == null || game.getPrice() == null || game.getPrice() >= minPrice)
                .filter(game -> maxPrice == null || game.getPrice() == null || game.getPrice() <= maxPrice)
                .sorted(Comparator.comparing(Game::getTitle, Comparator.nullsLast(String::compareToIgnoreCase)))
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public List<GameDTO> getGamesByPrice(Double minPrice, Double maxPrice) {
        return filterGames(null, null, minPrice, maxPrice);
    }

    public GameDTO createGame(GameDTO dto) {
        Game game = new Game();
        game.setTitle(dto.getTitle());
        game.setDescription(dto.getDescription());
        game.setGenre(dto.getGenre());
        game.setPrice(dto.getPrice());
        game.setDeveloper(dto.getDeveloper());
        game.setImageUrl(dto.getImageUrl());
        game.setGameFileUrl(dto.getGameFileUrl());
        game.setGalleryImageUrls(dto.getGalleryImageUrls());
        game.setSizeInBytes(dto.getSizeInBytes());
        game.setVersion(dto.getVersion());
        game.setPlatform(dto.getPlatform());
        game.setAgeRating(dto.getAgeRating());
        game.setSystemRequirements(dto.getSystemRequirements());
        game.setReleaseDate(dto.getReleaseDate());
        Game saved = gameRepository.save(game);
        return toDTO(saved);
    }

    public GameDTO updateGame(String id, GameDTO dto) {
        return gameRepository.findById(id).map(game -> {
            game.setTitle(dto.getTitle());
            game.setDescription(dto.getDescription());
            game.setGenre(dto.getGenre());
            game.setPrice(dto.getPrice());
            game.setDeveloper(dto.getDeveloper());
            game.setImageUrl(dto.getImageUrl());
            game.setGameFileUrl(dto.getGameFileUrl());
            game.setGalleryImageUrls(dto.getGalleryImageUrls());
            game.setSizeInBytes(dto.getSizeInBytes());
            game.setVersion(dto.getVersion());
            game.setPlatform(dto.getPlatform());
            game.setAgeRating(dto.getAgeRating());
            game.setSystemRequirements(dto.getSystemRequirements());
            game.setReleaseDate(dto.getReleaseDate());
            Game updated = gameRepository.save(game);
            return toDTO(updated);
        }).orElse(null);
    }

    public void deleteGame(String id) {
        gameRepository.deleteById(id);
    }

    private GameDTO toDTO(Game game) {
        GameDTO dto = new GameDTO();
        dto.setId(game.getId());
        dto.setTitle(game.getTitle());
        dto.setDescription(game.getDescription());
        dto.setGenre(game.getGenre());
        dto.setPrice(game.getPrice());
        dto.setDeveloper(game.getDeveloper());
        dto.setImageUrl(game.getImageUrl());
        dto.setGameFileUrl(game.getGameFileUrl());
        dto.setGalleryImageUrls(game.getGalleryImageUrls());
        dto.setSizeInBytes(game.getSizeInBytes());
        dto.setCreatedAt(game.getCreatedAt());
        dto.setVersion(game.getVersion());
        dto.setPlatform(game.getPlatform());
        dto.setAgeRating(game.getAgeRating());
        dto.setSystemRequirements(game.getSystemRequirements());
        dto.setReleaseDate(game.getReleaseDate());
        dto.setReviews(game.getReviews() == null ? List.of() : game.getReviews().stream()
            .map(review -> new ReviewDTO(
                review.getUsername(),
                review.getRating(),
                review.getComment(),
                review.getCreatedAt()
            ))
            .collect(Collectors.toList()));
        return dto;
    }

    private boolean containsIgnoreCase(String value, String query) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(query.toLowerCase(Locale.ROOT));
    }

    private boolean equalsIgnoreCase(String left, String right) {
        return left != null && right != null && left.equalsIgnoreCase(right);
    }

    private String normalizeReviewerName(String username) {
        return (username == null || username.isBlank()) ? "Anonymous" : username.trim();
    }

    private int findReviewIndex(List<Review> reviews, String username) {
        for (int index = 0; index < reviews.size(); index++) {
            Review review = reviews.get(index);
            if (review != null && review.getUsername() != null && review.getUsername().equalsIgnoreCase(username)) {
                return index;
            }
        }
        return -1;
    }

    public String generateSignedDownloadUrl(String gameFileUrl) throws Exception {
        if (gameFileUrl == null || gameFileUrl.isBlank()) {
            throw new Exception("No game file URL available");
        }

        // Extract fileId from direct Supabase URL
        // URL format: https://qdohwmucmbjsotaibsbh.supabase.co/storage/v1/object/public/game-files/UUID.ext
        String fileId = extractFileIdFromUrl(gameFileUrl);
        if (fileId == null) {
            System.err.println("⚠️ Could not extract fileId from URL: " + gameFileUrl);
            return gameFileUrl; // Fallback to original URL
        }

        try {
            String signedUrlEndpoint = storageServiceUrl + "/storage/signed-url/" + fileId;
            Map<String, String> response = restTemplate.getForObject(signedUrlEndpoint, Map.class);
            if (response != null && response.containsKey("url")) {
                System.out.println("✅ Generated signed URL for fileId: " + fileId);
                return response.get("url");
            }
        } catch (Exception e) {
            System.err.println("⚠️ Failed to generate signed URL: " + e.getMessage());
        }

        return gameFileUrl; // Fallback to original URL
    }

    private String extractFileIdFromUrl(String url) {
        if (url == null || !url.contains("/game-files/")) {
            return null;
        }
        String[] parts = url.split("/game-files/");
        if (parts.length > 1) {
            // Return the filename without extension as fileId
            String filename = parts[1].split("\\?")[0]; // Remove query params
            return filename.replaceAll("\\.[^.]*$", ""); // Remove extension
        }
        return null;
    }
}
