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

    // Either one of the built-in keys (WINE_TASTING, COOKING_CLASS, LIVE_MUSIC,
    // SPECIAL_DINNER, THEMED_NIGHT, OTHER) or a free-text type created by the owner.
    private String category;
}
