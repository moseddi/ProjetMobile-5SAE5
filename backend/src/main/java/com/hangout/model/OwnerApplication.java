package com.hangout.model;

import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * A restaurant owner's request to join the platform. Nothing is created
 * (no user, no restaurant) until an admin accepts it.
 */
@Data
@Document(collection = "owner_applications")
public class OwnerApplication {
    @Id
    private String id;

    // Applicant
    private String ownerName;
    private String email;
    private String phone;
    private String passwordHash;

    // Restaurant they want to list
    private String restaurantName;
    private String cuisine;
    private String description;
    private String image;
    private String address;
    private String restaurantPhone;
    private String openingHours;
    private Restaurant.PriceRange priceRange;

    private Status status = Status.PENDING;
    private String rejectionReason;

    // Filled once accepted
    private String createdUserId;
    private String createdRestaurantId;

    @CreatedDate
    private LocalDateTime createdAt;
    private LocalDateTime reviewedAt;

    public enum Status {
        PENDING, ACCEPTED, REJECTED
    }
}
