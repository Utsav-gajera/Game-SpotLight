package com.gamestore.purchase.service;

import com.gamestore.purchase.dto.PurchaseDTO;
import com.gamestore.purchase.entity.Purchase;
import com.gamestore.purchase.repository.PurchaseRepository;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class PurchaseService {

    private final PurchaseRepository purchaseRepository;

    public PurchaseService(PurchaseRepository purchaseRepository) {
        this.purchaseRepository = purchaseRepository;
    }

    public List<PurchaseDTO> getPurchasesByUser(String userId) {
        return purchaseRepository.findByUserId(userId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public PurchaseDTO getPurchaseById(String id) {
        return purchaseRepository.findById(id)
                .map(this::toDTO)
                .orElse(null);
    }

    public PurchaseDTO createPurchase(PurchaseDTO dto) {
        if (purchaseRepository.existsByUserIdAndGameId(dto.getUserId(), dto.getGameId())) {
            throw new IllegalStateException("You already own this game.");
        }

        Purchase purchase = new Purchase();
        purchase.setUserId(dto.getUserId());
        purchase.setGameId(dto.getGameId());
        purchase.setPrice(dto.getPrice());
        purchase.setPurchaseStatus(dto.getPurchaseStatus() != null ? dto.getPurchaseStatus() : "COMPLETED");
        try {
            Purchase saved = purchaseRepository.save(purchase);
            return toDTO(saved);
        } catch (DuplicateKeyException ex) {
            throw new IllegalStateException("You already own this game.");
        }
    }

    public PurchaseDTO completePurchase(String id) {
        return purchaseRepository.findById(id).map(purchase -> {
            purchase.setPurchaseStatus("COMPLETED");
            Purchase updated = purchaseRepository.save(purchase);
            return toDTO(updated);
        }).orElse(null);
    }

    public void deletePurchase(String id) {
        purchaseRepository.deleteById(id);
    }

    private PurchaseDTO toDTO(Purchase purchase) {
        return new PurchaseDTO(
                purchase.getId(),
                purchase.getUserId(),
                purchase.getGameId(),
                purchase.getPrice(),
                purchase.getPurchaseStatus(),
                purchase.getPurchasedAt()
        );
    }
}
