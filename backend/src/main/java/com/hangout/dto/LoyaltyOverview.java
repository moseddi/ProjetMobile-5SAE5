package com.hangout.dto;

import lombok.Data;

import java.util.List;

/** Everything the Loyalty screen needs, in one call. */
@Data
public class LoyaltyOverview {
    private int totalPoints;
    /** The client's permanent QR code value. */
    private String memberCode;
    /** Part of the total not tied to a restaurant (earned before restaurants were tracked). */
    private int unassignedPoints;
    private List<GameStatusResponse> games;
    private List<RestaurantLoyalty> restaurants;

    @Data
    public static class RestaurantLoyalty {
        private String restaurantId;
        private String name;
        private String image;
        private String cuisine;
        private int points;
        /** False when the owner switched the loyalty programme off. */
        private boolean loyaltyEnabled;
        private List<Offer> offers;
    }

    @Data
    public static class Offer {
        private String type;
        private String title;
        private String description;
        private int cost;
        private boolean canRedeem;
        /** Points still missing to afford it (0 when affordable). */
        private int missingPoints;
    }
}
