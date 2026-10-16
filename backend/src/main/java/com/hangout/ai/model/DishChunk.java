package com.hangout.ai.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Document(collection = "dish_chunks")
@Data @AllArgsConstructor @NoArgsConstructor
public class DishChunk {
    @Id private String id;
    private String restaurantId;
    private String restaurantName;
    private String dishName;
    private String text;
    private Double price;
    @JsonIgnore private List<Double> embedding;
}
