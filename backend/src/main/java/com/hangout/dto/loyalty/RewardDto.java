package com.hangout.dto.loyalty;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RewardDto {
    private String id;
    private String name;
    private String description;
    private String category;
    private Integer pointsCost;
    private Integer stock;
    private String imageUrl;
    private String termsAndConditions;
    private boolean affordable;
    private Integer pointsMissing;
    private String restaurantId;
    private LocalDateTime expiryDate;
}
