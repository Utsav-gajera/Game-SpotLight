	package com.gamestore.controller;
	
	import com.gamestore.dto.UserDTO;
    import com.gamestore.entity.Role;
	
	import com.gamestore.dto.GameDTO;
	import com.gamestore.entity.Game;
	import com.gamestore.entity.User;
    import com.gamestore.service.GameService;
	import com.gamestore.service.UserService;
	import org.springframework.beans.factory.annotation.Autowired;
	import org.springframework.security.core.annotation.AuthenticationPrincipal;
	import org.springframework.security.core.userdetails.UserDetails;
	import org.springframework.web.bind.annotation.*;
	
	import jakarta.servlet.http.HttpSession;
    import org.springframework.web.multipart.MultipartFile;

    import java.io.IOException;
    import java.util.Optional;
	
	@RestController
	@RequestMapping("/api/developer")
	public class DeveloperController {
	
	    @Autowired 
	    private GameService gameService;
	    
	    @Autowired 
	    private UserService userService;


	    private User getSessionUser(HttpSession session) {
	    	User username = (User) session.getAttribute("user");
	    	System.out.println("Session username: " + username);
	    	return userService.findByUsername(username.getUsername())
	    	        .orElseThrow(() -> new RuntimeException("User not found"));
	
	    }

        @PostMapping("/games/add")
        public GameDTO addGame(@RequestBody GameDTO gameDTO, HttpSession session) {

            User developer = getSessionUser(session);

            if (developer.getRole() != Role.DEVELOPER) {
                throw new RuntimeException("User does not have permission to add games.");
            }

            return gameService.addGame(
                    gameDTO.getTitle(),
                    gameDTO.getGenre(),
                    gameDTO.getPrice(),
                    developer
            );
        }



        @PutMapping("/games/{gameId}")
        public String updateGame(
                @PathVariable Long gameId,
                @RequestParam(required = false) String title,
                @RequestParam(required = false) String genre,
                @RequestParam(required = false) Double price,
                HttpSession session) {

            User developer = getSessionUser(session);

            gameService.updateGame(gameId, title, genre, price, developer);

            return "Game updated successfully!";
        }


        @DeleteMapping("/games/{gameId}")
	    public String removeGame(@PathVariable Long gameId, HttpSession session) {
	        User developer = getSessionUser(session);
	        Optional<Game> gameOpt = gameService.getGameById(gameId);

	        if (gameOpt.isPresent() && gameOpt.get().getDeveloper().equals(developer)) {
	            gameService.deleteGame(gameId);
	            return "Game removed successfully!";
	        }
	        throw new RuntimeException("Unauthorized to delete this game or game not found.");
	    }

        @GetMapping("/profile")
        public UserDTO viewProfile(HttpSession session) {
            User user = getSessionUser(session);
            return userService.convertToDTO(user);
        }

	}
