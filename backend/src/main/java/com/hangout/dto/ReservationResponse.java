package com.hangout.dto;

import com.hangout.model.Reservation;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ReservationResponse {
    private String id;
    private String userId;
    private String restaurantId;
    private String restaurantName;
    private String restaurantImage;
    private String eventId;
    private String eventTitle;
    private String contactName;
    private String contactPhone;
    private LocalDateTime reservationDateTime;
    private Integer partySize;
    private String specialRequests;
    private String status;
    private List<Reservation.ReservationItem> items;
    private Double subtotal;
    private Double tax;
    private Double total;
    private LocalDateTime createdAt;
}
