package com.hangout.dto;

import com.hangout.model.Event;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class EventResponse {
    private String id;
    private String restaurantId;
    private String restaurantName;
    private String restaurantImage;
    private String title;
    private String description;
    private String image;
    private LocalDateTime eventDate;
    private Double price;
    private Integer availableSpots;
    private Integer bookedSpots;
    private String category;
}
