package com.gamestore.controller;

import com.gamestore.dto.GameDTO;
import com.gamestore.dto.PurchaseDTO;
import com.gamestore.dto.UserDTO;
import com.gamestore.dto.WishlistDTO;
import com.gamestore.entity.User;
import com.gamestore.service.GameService;
import com.gamestore.service.PurchaseService;
import com.gamestore.service.UserService;
import com.gamestore.service.WishlistService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;
import java.util.List;

@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired private UserService userService;
    @Autowired private GameService gameService;
    @Autowired private WishlistService wishlistService;
    @Autowired private PurchaseService purchaseService;

    /**
     * Helper: read the 'user' attribute from HttpSession (set by your AuthController).
     * Throws IllegalArgumentException when not authenticated or session missing.
     */
    private User getSessionUser(HttpSession session) {
        if (session == null) throw new IllegalArgumentException("No session present.");
        Object userObj = session.getAttribute("user");
        if (userObj == null || !(userObj instanceof User)) {
            throw new IllegalArgumentException("Not authenticated.");
        }
        User sessionUser = (User) userObj;
        return userService.findByUsername(sessionUser.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("User not found."));
    }

    @GetMapping("/us")
    public ResponseEntity<User> getSessionUserEndpoint(HttpSession session) {
        User user = getSessionUser(session);
        return ResponseEntity.ok(user);
    }

    @GetMapping("/all")
    public ResponseEntity<List<UserDTO>> getAllUsers(HttpSession session) {
        // ensure caller is authenticated
        getSessionUser(session);
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @GetMapping("/games")
    public ResponseEntity<List<GameDTO>> viewAllGames() {
        return ResponseEntity.ok(gameService.getAllGames());
    }

    @PostMapping("/wishlist/create")
    public ResponseEntity<?> createWishlist(@RequestParam String name, HttpSession session) {
        User user = getSessionUser(session);
        try {
            WishlistDTO wishlist = wishlistService.createWishlist(user, name);
            return ResponseEntity.status(HttpStatus.CREATED).body(wishlist);
        } catch (IllegalArgumentException ex) {
            // Duplicate name or other validation -> map to 409 or 400 via handler
            throw ex;
        }
    }

    @PostMapping("/wishlist/{wishlistId}/add/{gameId}")
    public ResponseEntity<String> addToWishlist(@PathVariable Long wishlistId,
                                                @PathVariable Long gameId,
                                                HttpSession session) {
        User user = getSessionUser(session);
        String response = wishlistService.addToWishlist(user, wishlistId, gameId);

        // some service implementations return error messages as strings; interpret them
        String lower = (response == null) ? "" : response.toLowerCase();
        if (lower.contains("not found") || lower.contains("does not exist")) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
        if (lower.contains("already") || lower.contains("exists") || lower.contains("duplicate")) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        }

        return ResponseEntity.ok(response);
    }

    @GetMapping("/wishlist")
    public ResponseEntity<List<WishlistDTO>> showWishlist(HttpSession session) {
        User user = getSessionUser(session);
        return ResponseEntity.ok(wishlistService.getWishlist(user));
    }

    /**
     * Remove a game from a specific wishlist.
     * - If the service returns a message indicating the game was not present,
     *   we return 404 Not Found instead of 200 OK.
     * - If the service deletes the wishlist because it became empty, the service
     *   should return a message describing that — we return 200 OK with that message.
     */
    @DeleteMapping("/wishlist/{wishlistId}/remove/{gameId}")
    public ResponseEntity<String> removeFromWishlist(@PathVariable Long wishlistId,
                                                     @PathVariable Long gameId,
                                                     HttpSession session) {
        User user = getSessionUser(session);
        String response = wishlistService.removeFromWishlist(user, wishlistId, gameId);

        if (response == null) response = "";

        String lower = response.toLowerCase();
        // common error replies from services
        if (lower.contains("not found") || lower.contains("not in wishlist") || lower.contains("does not exist")) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
        // if service indicates deletion due to empty wishlist, return 200 with that message
        return ResponseEntity.ok(response);
    }

    @PutMapping("/wishlist/{wishlistId}/update/{oldGameId}/{newGameId}")
    public ResponseEntity<String> updateWishlistItem(@PathVariable Long wishlistId,
                                                     @PathVariable Long oldGameId,
                                                     @PathVariable Long newGameId,
                                                     HttpSession session) {
        User user = getSessionUser(session);
        String response = wishlistService.updateWishlistItem(user, wishlistId, oldGameId, newGameId);

        String lower = (response == null) ? "" : response.toLowerCase();
        if (lower.contains("not found") || lower.contains("does not exist") || lower.contains("not in wishlist")) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
        if (lower.contains("already") || lower.contains("exists") || lower.contains("duplicate")) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        }

        return ResponseEntity.ok(response);
    }

    @PostMapping("/purchase/{gameId}")
    public ResponseEntity<String> purchaseGame(@PathVariable Long gameId, HttpSession session) {
        User user = getSessionUser(session);
        String response = purchaseService.purchaseGame(user, gameId);

        String lower = (response == null) ? "" : response.toLowerCase();
        if (lower.contains("not found") || lower.contains("does not exist")) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/purchases")
    public ResponseEntity<List<PurchaseDTO>> checkPurchaseStatus(HttpSession session) {
        User user = getSessionUser(session);
        return ResponseEntity.ok(purchaseService.getPurchaseHistory(user));
    }

    @GetMapping("/profile")
    public ResponseEntity<UserDTO> viewProfile(HttpSession session) {
        User user = getSessionUser(session);
        return ResponseEntity.ok(userService.convertToDTO(user));
    }

    /* -------------------------
       Exception handlers
       ------------------------- */

    /**
     * Map IllegalArgumentException messages to appropriate HTTP status codes.
     * - "not found", "not in wishlist", "does not exist" -> 404
     * - "exists", "already", "duplicate" -> 409
     * - otherwise -> 400
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleIllegalArgument(IllegalArgumentException ex) {
        String msg = (ex.getMessage() == null) ? "" : ex.getMessage().toLowerCase();
        if (msg.contains("not found") || msg.contains("not in wishlist") || msg.contains("does not exist")) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
        }
        if (msg.contains("exists") || msg.contains("already") || msg.contains("duplicate")) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage());
        }
        return ResponseEntity.badRequest().body(ex.getMessage());
    }

    /**
     * Security-related failures (if thrown as SecurityException) -> 403 Forbidden
     */
    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<String> handleSecurity(SecurityException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ex.getMessage());
    }

    /**
     * Generic fallback -> 500 Internal Server Error
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleServerError(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Server error: " + ex.getMessage());
    }
}
