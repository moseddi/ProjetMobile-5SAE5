package com.hangout.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ReservationRequest {
    @NotBlank(message = "Restaurant ID is required")
    private String restaurantId;

    @NotNull(message = "Reservation date and time is required")
    @Future(message = "Reservation must be in the future")
    private LocalDateTime reservationDateTime;

    @NotNull(message = "Party size is required")
    @Min(value = 1, message = "Party size must be at least 1")
    @Max(value = 20, message = "Party size cannot exceed 20")
    private Integer partySize;

    private String specialRequests;

    @NotBlank(message = "Your name is required")
    @Size(max = 80, message = "Name must be at most 80 characters")
    private String contactName;

    @NotBlank(message = "Your phone number is required")
    @Pattern(regexp = "^[+]?[0-9 ()-]{6,20}$", message = "Enter a valid phone number")
    private String contactPhone;

    // Optional food pre-order
    private List<ItemRequest> items;

    @Data
    public static class ItemRequest {
        @NotBlank(message = "Menu item ID is required")
        private String menuItemId;

        @NotNull(message = "Quantity is required")
        @Min(value = 1, message = "Quantity must be at least 1")
        private Integer quantity;
    }
}
