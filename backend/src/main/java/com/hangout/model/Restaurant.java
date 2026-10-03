package com.hangout.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Document(collection = "restaurants")
public class Restaurant {
    @Id
    private String id;
    
    private String name;
    private String cuisine;
    private String description;
    private String image;
    private String address;
    private String phone;
    private String openingHours;
    private Double rating; // Calculated from reviews
    private Integer reviewCount = 0;
    private Boolean isOpen;
    private List<String> features;
    private PriceRange priceRange;
    
    @CreatedDate
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    private LocalDateTime updatedAt;
    
    public enum PriceRange {
        BUDGET, MODERATE, EXPENSIVE, LUXURY
    }
}
