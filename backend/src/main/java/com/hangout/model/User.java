package com.hangout.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.LocalDateTime;

@Data
@Document(collection = "users")
public class User {
    @Id
    private String id;
    
    @Indexed(unique = true)
    private String email;
    
    private String password; // Hashed
    private String name;
    private String phone;
    private String avatar;
    private Integer loyaltyPoints = 0;
    private Integer totalReservations = 0;
    private Integer totalOrders = 0;
    private Integer totalReviews = 0;

    private Role role = Role.CLIENT;

    // Only set when role == RESTAURANT_OWNER: the restaurant this user manages
    private String restaurantId;

    // Admin can deactivate an account instead of deleting it
    private Boolean active = true;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    public enum Role {
        CLIENT, RESTAURANT_OWNER, ADMIN
    }
}
