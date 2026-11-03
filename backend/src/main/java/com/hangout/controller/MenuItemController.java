package com.hangout.controller;

import com.hangout.model.MenuItem;
import com.hangout.service.MenuItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/menu-items")
@RequiredArgsConstructor
public class MenuItemController {

    private final MenuItemService menuItemService;

    @GetMapping("/restaurant/{restaurantId}")
    public ResponseEntity<List<MenuItem>> getByRestaurant(@PathVariable String restaurantId) {
        return ResponseEntity.ok(menuItemService.getByRestaurant(restaurantId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<MenuItem> getById(@PathVariable String id) {
        return ResponseEntity.ok(menuItemService.getById(id));
    }
}
