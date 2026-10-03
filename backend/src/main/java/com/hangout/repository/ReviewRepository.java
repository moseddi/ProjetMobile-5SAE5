package com.hangout.repository;

import com.hangout.model.Review;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewRepository extends MongoRepository<Review, String> {
    List<Review> findByRestaurantId(String restaurantId);
    List<Review> findByUserId(String userId);
    List<Review> findByRestaurantIdAndStatus(String restaurantId, Review.ReviewStatus status);
    long countByRestaurantIdAndStatus(String restaurantId, Review.ReviewStatus status);
}
