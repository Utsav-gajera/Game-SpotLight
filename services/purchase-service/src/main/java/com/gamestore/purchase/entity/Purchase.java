package com.gamestore.purchase.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.CompoundIndex;

import java.time.Instant;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "purchases")
@CompoundIndex(name = "user_game_unique_idx", def = "{'userId': 1, 'gameId': 1}", unique = true)
public class Purchase {
    @Id
    private String id;
    private String userId;
    private String gameId;
    private Double price;
    private String purchaseStatus;
    @CreatedDate
    private Instant purchasedAt;
}
