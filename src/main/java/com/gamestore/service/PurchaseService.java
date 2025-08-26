package com.gamestore.service;

import com.gamestore.dto.GameDTO;
import com.gamestore.dto.PurchaseDTO;

import com.gamestore.entity.Game;
import com.gamestore.entity.Purchase;
import com.gamestore.entity.User;
import com.gamestore.repository.GameRepository;
import com.gamestore.repository.PurchaseRepository;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PurchaseService {
    @Autowired private PurchaseRepository purchaseRepository;
    @Autowired private GameRepository gameRepository;
    @Autowired private ModelMapper modelMapper;

    public String purchaseGame(User user, Long gameId) {
        Game game = gameRepository.findById(gameId).orElseThrow(() -> new RuntimeException("Game not found"));

        boolean alreadyPurchased = purchaseRepository
                .existsByUserAndGame(user, game);

        if (alreadyPurchased) {
            throw new RuntimeException("You have already purchased this game!");
        }

        Purchase purchase = new Purchase();
        purchase.setUser(user);
        purchase.setGame(game);
        purchase.setPurchaseDate(LocalDateTime.now());
        purchaseRepository.save(purchase);
        return "Game purchased successfully!";
    }

    public List<PurchaseDTO> getPurchaseHistory(User user) {
        return purchaseRepository.findByUser(user).stream().map(purchase -> {
            PurchaseDTO dto = modelMapper.map(purchase, PurchaseDTO.class);
            dto.setUsername(purchase.getUser().getUsername());
            return dto;
        }).collect(Collectors.toList());
    }

    private PurchaseDTO convertToDTO(Purchase purchase) {
        PurchaseDTO dto = modelMapper.map(purchase, PurchaseDTO.class);
        dto.setGameTitle(purchase.getGame().getTitle());
        dto.setUsername(purchase.getUser().getUsername());
        return dto;
    }

    public List<PurchaseDTO> getAllPurchases() {
        return purchaseRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

}
