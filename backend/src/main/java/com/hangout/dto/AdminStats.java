package com.hangout.dto;

import lombok.Data;

@Data
public class AdminStats {
    private long totalUsers;
    private long totalClients;
    private long totalRestaurantOwners;
    private long totalAdmins;
    private long totalRestaurants;
    private long unassignedRestaurants;
    private long totalReservations;
    private long totalReviews;
    private long totalHangOuts;
    private long totalEvents;
    private long pendingOwnerApplications;
}
