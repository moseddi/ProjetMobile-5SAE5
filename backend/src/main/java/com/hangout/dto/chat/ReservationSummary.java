package com.hangout.dto.chat;

import com.hangout.model.Reservation.ReservationStatus;

import java.time.LocalDateTime;

public record ReservationSummary(
    int number,
    String id,
    String restaurantId,
    String restaurantName,
    LocalDateTime reservationDateTime,
    Integer partySize,
    ReservationStatus status
) {}
