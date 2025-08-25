    package com.gamestore.controller;

    import com.gamestore.dto.GameDTO;


    import com.gamestore.dto.PurchaseDTO;
    import com.gamestore.dto.UserDTO;
    import com.gamestore.dto.WishlistDTO;
    import com.gamestore.entity.Game;
    import com.gamestore.entity.User;
    import com.gamestore.service.GameService;
    import com.gamestore.service.PurchaseService;
    import com.gamestore.service.UserService;
    import com.gamestore.service.WishlistService;
    import org.springframework.beans.factory.annotation.Autowired;
    import org.springframework.http.HttpHeaders;
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

        // Get currently logged-in user from session
        @GetMapping("/us")
        private User getSessionUser(HttpSession session) {
            User username = (User) session.getAttribute("user");
            return userService.findByUsername(username.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found"));
        }

        @GetMapping("/all")
        public List<UserDTO> getAllUsers() {
            return userService.getAllUsers();
        }

        @GetMapping("/games")
        public List<GameDTO> viewAllGames() {
            return gameService.getAllGames();
        }

        // Create a new wishlist
        @PostMapping("/wishlist/create")
        public WishlistDTO createWishlist(@RequestParam String name, HttpSession session) {
            User user = getSessionUser(session);
            return wishlistService.createWishlist(user, name);
        }

        // Add a game to a specific wishlist
        @PostMapping("/wishlist/{wishlistId}/add/{gameId}")
        public String addToWishlist(@PathVariable Long wishlistId, @PathVariable Long gameId, HttpSession session) {
            User user = getSessionUser(session);
            return wishlistService.addToWishlist(user, wishlistId, gameId);
        }

        // Show all wishlists of the user
        @GetMapping("/wishlist")
        public List<WishlistDTO> showWishlist(HttpSession session) {
            User user = getSessionUser(session);
            return wishlistService.getWishlist(user);
        }

        // Remove a game from a specific wishlist
        @DeleteMapping("/wishlist/{wishlistId}/remove/{gameId}")
        public String removeFromWishlist(@PathVariable Long wishlistId,
                                         @PathVariable Long gameId,
                                         HttpSession session) {
            User user = getSessionUser(session);
            return wishlistService.removeFromWishlist(user, wishlistId, gameId);
        }

        // Update a game in a wishlist
        @PutMapping("/wishlist/{wishlistId}/update/{oldGameId}/{newGameId}")
        public String updateWishlistItem(@PathVariable Long wishlistId,
                                         @PathVariable Long oldGameId,
                                         @PathVariable Long newGameId,
                                         HttpSession session) {
            User user = getSessionUser(session);
            return wishlistService.updateWishlistItem(user, wishlistId, oldGameId, newGameId);
        }

        // Purchase a game
        @PostMapping("/purchase/{gameId}")
        public String purchaseGame(@PathVariable Long gameId, HttpSession session) {
            User user = getSessionUser(session);
            return purchaseService.purchaseGame(user, gameId);
        }

        // Get purchase history
        @GetMapping("/purchases")
        public List<PurchaseDTO> checkPurchaseStatus(HttpSession session) {
            User user = getSessionUser(session);
            return purchaseService.getPurchaseHistory(user);
        }

        // View profile
        @GetMapping("/profile")
        public UserDTO viewProfile(HttpSession session) {
            User user = getSessionUser(session);
            return userService.convertToDTO(user);
        }
    }
