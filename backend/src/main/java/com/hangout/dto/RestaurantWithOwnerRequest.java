package com.hangout.dto;

import com.hangout.model.Restaurant;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** Admin creates a restaurant and, optionally, its owner account in one go. */
@Data
public class RestaurantWithOwnerRequest {
    private Restaurant restaurant;
    private NewOwner owner;
    /** Alternative to {@code owner}: assign an existing account as the owner. */
    private String existingOwnerId;

    @Data
    public static class NewOwner {
        @NotBlank(message = "Owner name is required")
        private String name;

        @NotBlank(message = "Owner email is required")
        @Email(message = "Invalid owner email format")
        private String email;

        @NotBlank(message = "Owner password is required")
        @Size(min = 6, message = "Owner password must be at least 6 characters")
        private String password;

        private String phone;
    }
}
