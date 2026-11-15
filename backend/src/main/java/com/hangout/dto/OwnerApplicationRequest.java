package com.hangout.dto;

import com.hangout.model.Restaurant;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class OwnerApplicationRequest {
    @NotBlank(message = "Your name is required")
    private String ownerName;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;

    @NotBlank(message = "Your phone number is required")
    private String phone;

    @NotBlank(message = "Restaurant name is required")
    private String restaurantName;

    @NotBlank(message = "Cuisine is required")
    private String cuisine;

    @NotBlank(message = "Description is required")
    private String description;

    private String image;
    private String menuImage;

    @NotBlank(message = "Address is required")
    private String address;

    private String restaurantPhone;
    private String openingHours;
    private Restaurant.PriceRange priceRange;
}
