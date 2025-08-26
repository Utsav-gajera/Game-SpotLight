package com.gamestore.repository;

import com.gamestore.entity.Game;
import com.gamestore.entity.Purchase;
import com.gamestore.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PurchaseRepository extends JpaRepository<Purchase, Long> {
    boolean existsByUserAndGame(User user, Game game);
    List<Purchase> findByUser(User user);
}
