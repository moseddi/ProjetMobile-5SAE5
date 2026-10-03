package com.hangout.dto;

import lombok.Data;

@Data
public class UserDto {
    private String id;
    private String email;
    private String name;
    private String phone;
    private String avatar;
    private Integer loyaltyPoints;
    private Integer totalReservations;
    private Integer totalOrders;
    private Integer totalReviews;
}
