package com.hangout.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ReviewResponse {
    private String id;
    private String userId;
    private String userName;
    private String userAvatar;
    private String restaurantId;
    private String restaurantName;
    private String restaurantImage;
    private Double rating;
    private String title;
    private String comment;
    private String photoUrl;
    private Integer helpfulCount;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
