package com.hangout.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Document(collection = "events")
public class Event {
    @Id
    private String id;
    
    private String restaurantId;
    private String title;
    private String description;
    private String image;
    private LocalDateTime eventDate;
    private Double price;
    private Integer availableSpots;
    private Integer bookedSpots = 0;
    private EventCategory category;
    
    public enum EventCategory {
        WINE_TASTING, COOKING_CLASS, LIVE_MUSIC, SPECIAL_DINNER, THEMED_NIGHT, OTHER
    }
}
