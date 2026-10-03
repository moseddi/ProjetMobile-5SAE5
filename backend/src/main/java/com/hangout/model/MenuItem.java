package com.hangout.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document(collection = "menu_items")
public class MenuItem {
    @Id
    private String id;
    
    private String restaurantId;
    private String name;
    private String description;
    private String image;
    private Double price;
    private MenuCategory category;
    private Boolean available = true;
    
    public enum MenuCategory {
        STARTERS, MAIN_COURSES, DESSERTS, DRINKS, SPECIALS
    }
}
