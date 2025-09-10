package com.gamestore.repository;

import com.gamestore.entity.Game;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface GameRepository extends JpaRepository<Game, Long> {

    // Search by title (case-insensitive)
    List<Game> findByTitleContainingIgnoreCase(String title);

    // Filter by genre
    List<Game> findByGenreIgnoreCase(String genre);

    // Filter by price range
    List<Game> findByPriceBetween(Double minPrice, Double maxPrice);

    // Combined filter (optional)
    @Query("SELECT g FROM Game g WHERE " +
            "(:title IS NULL OR LOWER(g.title) LIKE LOWER(CONCAT('%', :title, '%'))) AND " +
            "(:genre IS NULL OR LOWER(g.genre) = LOWER(:genre)) AND " +
            "(:minPrice IS NULL OR g.price >= :minPrice) AND " +
            "(:maxPrice IS NULL OR g.price <= :maxPrice)")
    List<Game> searchGames(@Param("title") String title,
                           @Param("genre") String genre,
                           @Param("minPrice") Double minPrice,
                           @Param("maxPrice") Double maxPrice);

}
