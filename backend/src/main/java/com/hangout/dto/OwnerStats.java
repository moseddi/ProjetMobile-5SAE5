package com.hangout.dto;

import lombok.Data;

@Data
public class OwnerStats {
    private long totalReservations;
    private long pendingReservations;
    private long confirmedReservations;
    private long completedReservations;
    private long totalMenuItems;
    private long totalEvents;
    private long totalReviews;
    private Double averageRating;
}
