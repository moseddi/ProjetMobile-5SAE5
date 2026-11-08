package com.hangout.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class VoucherResponse {
    private String id;
    private String code;
    private String restaurantId;
    private String restaurantName;
    private String restaurantImage;
    private String rewardType;
    private String title;
    private String description;
    private int pointsSpent;
    /** ACTIVE, USED or EXPIRED */
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    private LocalDateTime usedAt;
}
