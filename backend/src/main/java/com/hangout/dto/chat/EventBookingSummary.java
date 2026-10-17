package com.hangout.dto.chat;

import com.hangout.model.EventBooking.BookingStatus;

import java.time.LocalDateTime;

public record EventBookingSummary(
    int number,
    String id,
    String eventId,
    String eventTitle,
    LocalDateTime eventDate,
    Integer partySize,
    BookingStatus status
) {}
