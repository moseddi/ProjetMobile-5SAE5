package com.hangout.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Document(collection = "reservations")
public class Reservation {
    @Id
    private String id;

    private String userId;
    private String restaurantId;

    // Set when this reservation is a registration for an Event rather than a plain table booking
    private String eventId;

    // Who is coming, as filled in by the customer when requesting
    private String contactName;
    private String contactPhone;

    private LocalDateTime reservationDateTime;
    private Integer partySize;
    private String specialRequests;
    private ReservationStatus status = ReservationStatus.PENDING;

    // Optional food pre-order placed alongside the table booking
    private List<ReservationItem> items;
    private Double subtotal;
    private Double tax;
    private Double total;

    @CreatedDate
    private LocalDateTime createdAt;

    @Data
    public static class ReservationItem {
        private String menuItemId;
        private String name;
        private Integer quantity;
        private Double price;
    }

    /**
     * PENDING   - requested, waiting for the restaurant owner
     * CONFIRMED - accepted by the owner (cancelled automatically if the guest is 30 min late)
     * ARRIVED   - the guest showed up in time and the owner checked them in
     * REFUSED   - declined by the owner
     * LATE      - cancelled automatically: the guest was more than 30 minutes late
     * CANCELLED - cancelled by the guest
     * COMPLETED - legacy final state
     */
    public enum ReservationStatus {
        PENDING, CONFIRMED, ARRIVED, REFUSED, LATE, CANCELLED, COMPLETED
    }
}
