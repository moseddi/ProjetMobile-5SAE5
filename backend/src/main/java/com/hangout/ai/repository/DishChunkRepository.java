package com.hangout.ai.repository;

import com.hangout.ai.model.DishChunk;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface DishChunkRepository extends MongoRepository<DishChunk, String> {
    void deleteByRestaurantId(String restaurantId);
    List<DishChunk> findByRestaurantId(String restaurantId);
}
