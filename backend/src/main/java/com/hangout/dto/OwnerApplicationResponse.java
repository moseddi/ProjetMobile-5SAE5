package com.hangout.dto;

import com.hangout.model.OwnerApplication;
import lombok.Data;

import java.time.LocalDateTime;

/** Application as shown to the admin: never exposes the password hash. */
@Data
public class OwnerApplicationResponse {
    private String id;
    private String ownerName;
    private String email;
    private String phone;
    private String restaurantName;
    private String cuisine;
    private String description;
    private String image;
    private String address;
    private String restaurantPhone;
    private String openingHours;
    private String priceRange;
    private String status;
    private String rejectionReason;
    private String createdUserId;
    private String createdRestaurantId;
    private LocalDateTime createdAt;
    private LocalDateTime reviewedAt;

    public static OwnerApplicationResponse from(OwnerApplication a) {
        OwnerApplicationResponse r = new OwnerApplicationResponse();
        r.setId(a.getId());
        r.setOwnerName(a.getOwnerName());
        r.setEmail(a.getEmail());
        r.setPhone(a.getPhone());
        r.setRestaurantName(a.getRestaurantName());
        r.setCuisine(a.getCuisine());
        r.setDescription(a.getDescription());
        r.setImage(a.getImage());
        r.setAddress(a.getAddress());
        r.setRestaurantPhone(a.getRestaurantPhone());
        r.setOpeningHours(a.getOpeningHours());
        r.setPriceRange(a.getPriceRange() != null ? a.getPriceRange().name() : null);
        r.setStatus(a.getStatus().name());
        r.setRejectionReason(a.getRejectionReason());
        r.setCreatedUserId(a.getCreatedUserId());
        r.setCreatedRestaurantId(a.getCreatedRestaurantId());
        r.setCreatedAt(a.getCreatedAt());
        r.setReviewedAt(a.getReviewedAt());
        return r;
    }
}
