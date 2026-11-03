package com.hangout.service;

import com.hangout.util.Newest;
import com.hangout.exception.BadRequestException;
import com.hangout.exception.ResourceNotFoundException;
import com.hangout.model.MenuItem;
import com.hangout.model.Restaurant;
import com.hangout.repository.MenuItemRepository;
import com.hangout.repository.RestaurantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class MenuItemService {

    private final MenuItemRepository menuItemRepository;
    private final RestaurantRepository restaurantRepository;

    public List<MenuItem> getByRestaurant(String restaurantId) {
        return menuItemRepository.findByRestaurantId(restaurantId).stream()
            .sorted(Newest.first(MenuItem::getId)).collect(java.util.stream.Collectors.toList());
    }

    public MenuItem getById(String id) {
        return menuItemRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Menu item not found"));
    }

    public MenuItem create(String restaurantId, MenuItem item) {
        restaurantRepository.findById(restaurantId)
            .orElseThrow(() -> new ResourceNotFoundException("Restaurant not found"));
        item.setId(null);
        item.setRestaurantId(restaurantId);
        item.setCategory(normalizeCategory(restaurantId, item.getCategory()));
        return menuItemRepository.save(item);
    }

    public MenuItem update(String id, MenuItem item) {
        MenuItem existing = getById(id);
        item.setId(id);
        item.setRestaurantId(existing.getRestaurantId());
        item.setCategory(normalizeCategory(existing.getRestaurantId(), item.getCategory()));
        return menuItemRepository.save(item);
    }

    private static final Set<String> BUILT_IN_CATEGORIES = Set.of(
        "STARTERS", "MAIN_COURSES", "DESSERTS", "DRINKS", "SPECIALS"
    );

    /**
     * Owners may use a built-in category or create their own. Built-ins are stored
     * under their key; a custom category reuses the spelling of an identical one this
     * restaurant already has, so "Pizzas" and "pizzas" don't become two sections.
     */
    private String normalizeCategory(String restaurantId, String raw) {
        String cleaned = raw == null ? "" : raw.trim().replaceAll("\\s+", " ");
        if (cleaned.isEmpty()) {
            throw new BadRequestException("Category is required");
        }
        if (cleaned.length() > 40) {
            throw new BadRequestException("Category must be at most 40 characters");
        }

        String asKey = cleaned.toUpperCase().replace(' ', '_');
        if (BUILT_IN_CATEGORIES.contains(asKey)) {
            return asKey;
        }

        return menuItemRepository.findByRestaurantId(restaurantId).stream()
            .map(MenuItem::getCategory)
            .filter(c -> c != null && !BUILT_IN_CATEGORIES.contains(c) && c.equalsIgnoreCase(cleaned))
            .findFirst()
            .orElse(cleaned);
    }

    public void delete(String id) {
        if (!menuItemRepository.existsById(id)) {
            throw new ResourceNotFoundException("Menu item not found");
        }
        menuItemRepository.deleteById(id);
    }

    public void assertOwns(String menuItemId, String ownerUserId) {
        MenuItem item = getById(menuItemId);
        assertOwnsRestaurant(item.getRestaurantId(), ownerUserId);
    }

    public void assertOwnsRestaurant(String restaurantId, String ownerUserId) {
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
            .orElseThrow(() -> new ResourceNotFoundException("Restaurant not found"));
        if (!ownerUserId.equals(restaurant.getOwnerId())) {
            throw new AccessDeniedException("You do not manage this restaurant");
        }
    }
}
