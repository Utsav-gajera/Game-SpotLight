package com.gamestore.purchase.config;

import com.gamestore.purchase.entity.Purchase;
import com.gamestore.purchase.repository.PurchaseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.IndexOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Enforces purchase data integrity at startup:
 * 1) removes duplicate purchases for the same (userId, gameId), keeping the earliest row
 * 2) creates/ensures a unique compound index on (userId, gameId)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PurchaseDataIntegrityInitializer implements ApplicationRunner {

    private final MongoTemplate mongoTemplate;
    private final PurchaseRepository purchaseRepository;

    @Override
    public void run(ApplicationArguments args) {
        try {
            cleanupDuplicatePurchases();
            ensureUserGameUniqueIndex();
        } catch (Exception ex) {
            // Do not block service startup on transient Atlas connectivity.
            log.warn("Purchase integrity bootstrap skipped due to temporary Mongo connectivity issue: {}", ex.getMessage());
        }
    }

    private void cleanupDuplicatePurchases() {
        List<Purchase> allPurchases = purchaseRepository.findAll();
        int removed = 0;

        for (Purchase purchase : allPurchases) {
            if (purchase == null || purchase.getUserId() == null || purchase.getGameId() == null) {
                continue;
            }

            Query query = Query.query(
                    Criteria.where("userId").is(purchase.getUserId())
                            .and("gameId").is(purchase.getGameId()))
                .with(Sort.by(Sort.Direction.ASC, "purchasedAt", "id"));

            List<Purchase> duplicates = mongoTemplate.find(query, Purchase.class);
            if (duplicates.size() <= 1) {
                continue;
            }

            // Keep the oldest purchase and remove all later duplicates.
            List<Purchase> toDelete = duplicates.subList(1, duplicates.size());
            purchaseRepository.deleteAll(toDelete);
            removed += toDelete.size();
        }

        if (removed > 0) {
            log.warn("Removed {} duplicate purchase records during startup integrity check.", removed);
        } else {
            log.info("No duplicate purchase records found during startup integrity check.");
        }
    }

    private void ensureUserGameUniqueIndex() {
        IndexOperations indexOps = mongoTemplate.indexOps(Purchase.class);
        Index uniqueUserGameIndex = new Index()
                .on("userId", Sort.Direction.ASC)
                .on("gameId", Sort.Direction.ASC)
                .unique()
                .named("user_game_unique_idx");

        indexOps.ensureIndex(uniqueUserGameIndex);
        log.info("Ensured unique index user_game_unique_idx on purchases(userId, gameId).");
    }
}
