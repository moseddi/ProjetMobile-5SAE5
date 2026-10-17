package com.hangout.model;

import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Document(collection = "rewards")
public class Reward {
    @Id
    private String id;

    private String name;
    private String description;
    private RewardCategory category;
    private Integer pointsCost;

    /** Restaurant this reward can be redeemed at; null = platform-wide reward, not tied to one restaurant. */
    private String restaurantId;
    private Integer stock; // null = unlimited
    private Boolean active = true;
    private String imageUrl;
    private String termsAndConditions;
    private LocalDateTime expiryDate; // null = no expiry

    @CreatedDate
    private LocalDateTime createdAt;

    public enum RewardCategory {
        DISCOUNT, FREE_ITEM, EXPERIENCE, TIER_PERK
    }
}
