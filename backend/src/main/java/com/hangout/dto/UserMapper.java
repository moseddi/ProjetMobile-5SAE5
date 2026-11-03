package com.hangout.dto;

import com.hangout.model.User;

public final class UserMapper {

    private UserMapper() {}

    public static UserDto toDto(User user) {
        UserDto dto = new UserDto();
        dto.setId(user.getId());
        dto.setEmail(user.getEmail());
        dto.setName(user.getName());
        dto.setPhone(user.getPhone());
        dto.setAvatar(user.getAvatar());
        dto.setLoyaltyPoints(user.getLoyaltyPoints());
        dto.setTotalReservations(user.getTotalReservations());
        dto.setTotalOrders(user.getTotalOrders());
        dto.setTotalReviews(user.getTotalReviews());
        dto.setRole(user.getRole());
        dto.setRestaurantId(user.getRestaurantId());
        dto.setActive(user.getActive());
        return dto;
    }
}
