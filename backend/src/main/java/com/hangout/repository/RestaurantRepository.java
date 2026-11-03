package com.hangout.repository;

import com.hangout.model.Restaurant;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RestaurantRepository extends MongoRepository<Restaurant, String> {
    List<Restaurant> findByCuisine(String cuisine);
    List<Restaurant> findByIsOpen(Boolean isOpen);
    Optional<Restaurant> findByOwnerId(String ownerId);
    Optional<Restaurant> findByName(String name);
}
