package com.gamestore.repository;

import com.gamestore.entity.Purchase;
import com.gamestore.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PurchaseRepository extends JpaRepository<Purchase, Long> {
    List<Purchase> findByUser(User user);
}
