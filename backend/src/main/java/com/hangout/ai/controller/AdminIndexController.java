package com.hangout.ai.controller;

import com.hangout.ai.service.IndexingService;
import com.hangout.model.Restaurant;
import com.hangout.repository.RestaurantRepository;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminIndexController {
    private final IndexingService indexing;
    private final RestaurantRepository restaurants;

    public AdminIndexController(IndexingService indexing, RestaurantRepository restaurants) {
        this.indexing = indexing;
        this.restaurants = restaurants;
    }

    @PostMapping("/index/{restaurantId}")
    public Map<String, Object> index(@PathVariable String restaurantId) throws Exception {
        return indexing.indexStoredMenu(restaurants.findById(restaurantId).orElseThrow());
    }

    @PostMapping("/index-all")
    public List<Map<String, Object>> indexAll() {
        List<Map<String, Object>> out = new ArrayList<>();
        for (Restaurant r : restaurants.findAll()) {
            try {
                out.add(indexing.indexStoredMenu(r));
            } catch (Exception e) {
                out.add(Map.of("restaurant", String.valueOf(r.getName()), "error", String.valueOf(e.getMessage())));
            }
        }
        return out;
    }
}
