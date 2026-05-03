package com.gamestore.game.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReviewDTO {
    private String username;
    private Integer rating;
    private String comment;
    private Instant createdAt;
}
