package com.hangout.service;

import com.hangout.util.Newest;
import com.hangout.exception.ResourceNotFoundException;
import com.hangout.model.Restaurant;
import com.hangout.repository.RestaurantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RestaurantService {
    
    private final RestaurantRepository restaurantRepository;
    
    public List<Restaurant> getAllRestaurants() {
        return restaurantRepository.findAll().stream()
            .sorted(Newest.first(Restaurant::getId)).collect(java.util.stream.Collectors.toList());
    }
    
    public Restaurant getRestaurantById(String id) {
        return restaurantRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Restaurant not found with id: " + id));
    }
    
    public List<Restaurant> getRestaurantsByCuisine(String cuisine) {
        return restaurantRepository.findByCuisine(cuisine).stream()
            .sorted(Newest.first(Restaurant::getId)).collect(java.util.stream.Collectors.toList());
    }
    
    public Restaurant createRestaurant(Restaurant restaurant) {
        return restaurantRepository.save(restaurant);
    }
    
    public Restaurant updateRestaurant(String id, Restaurant restaurant) {
        Restaurant existing = getRestaurantById(id);
        restaurant.setId(id);
        restaurant.setCreatedAt(existing.getCreatedAt());
        // The loyalty switch belongs to the owner; an admin edit must not reset it.
        restaurant.setLoyaltyEnabled(existing.getLoyaltyEnabled());
        return restaurantRepository.save(restaurant);
    }
    
    public void deleteRestaurant(String id) {
        if (!restaurantRepository.existsById(id)) {
            throw new ResourceNotFoundException("Restaurant not found with id: " + id);
        }
        restaurantRepository.deleteById(id);
    }

    public Restaurant getOwnRestaurant(String ownerId) {
        return restaurantRepository.findByOwnerId(ownerId)
            .orElseThrow(() -> new ResourceNotFoundException("You do not manage any restaurant yet"));
    }

    public Restaurant setOwnLoyaltyEnabled(String ownerId, boolean enabled) {
        Restaurant existing = getOwnRestaurant(ownerId);
        existing.setLoyaltyEnabled(enabled);
        return restaurantRepository.save(existing);
    }

    public Restaurant updateOwnRestaurant(String ownerId, Restaurant update) {
        Restaurant existing = getOwnRestaurant(ownerId);

        // Owners can edit their restaurant's public profile, not re-assign ownership or id.
        existing.setName(update.getName());
        existing.setCuisine(update.getCuisine());
        existing.setDescription(update.getDescription());
        existing.setImage(update.getImage());
        existing.setAddress(update.getAddress());
        existing.setPhone(update.getPhone());
        existing.setOpeningHours(update.getOpeningHours());
        existing.setIsOpen(update.getIsOpen());
        existing.setFeatures(update.getFeatures());
        existing.setPriceRange(update.getPriceRange());

        return restaurantRepository.save(existing);
    }
}
