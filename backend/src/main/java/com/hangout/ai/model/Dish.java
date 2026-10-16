package com.hangout.ai.model;

public record Dish(String name, String description, String category,
                   Boolean vegetarian, Double price) {}
