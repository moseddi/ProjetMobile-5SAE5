package com.hangout.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Document(collection = "reservations")
public class Reservation {
    @Id
    private String id;
    
    private String userId;
    private String restaurantId;
    private LocalDateTime reservationDateTime;
    private Integer partySize;
    private String specialRequests;
    private ReservationStatus status = ReservationStatus.PENDING;
    
    @CreatedDate
    private LocalDateTime createdAt;
    
    public enum ReservationStatus {
        PENDING, CONFIRMED, CANCELLED, COMPLETED
    }
}
