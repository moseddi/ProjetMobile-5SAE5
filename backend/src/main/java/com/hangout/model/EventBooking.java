package com.hangout.model;

import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/** A user's booking of spots at an {@link Event}. */
@Data
@Document(collection = "event_bookings")
public class EventBooking {
    @Id
    private String id;

    private String userId;
    private String eventId;
    private Integer partySize;
    private BookingStatus status = BookingStatus.PENDING;

    @CreatedDate
    private LocalDateTime createdAt;

    public enum BookingStatus {
        PENDING, CONFIRMED, CANCELLED
    }
}
