package com.hangout.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Result computed from the database only. The chatbot must never claim availability without one of these.
 */
public record AvailabilityResult(
    boolean available,
    String restaurantId,
    LocalDateTime requestedDateTime,
    int partySize,
    int remainingSeats,
    Reason reason,
    List<LocalDateTime> alternatives
) {
    public enum Reason {
        AVAILABLE,
        NOT_ENOUGH_SEATS,
        OUTSIDE_OPENING_HOURS,
        RESTAURANT_CLOSED,
        PARTY_TOO_LARGE_FOR_RESTAURANT
    }
}
