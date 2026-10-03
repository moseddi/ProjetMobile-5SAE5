package com.hangout.service;

import com.hangout.dto.ReviewRequest;
import com.hangout.dto.ReviewResponse;
import com.hangout.exception.BadRequestException;
import com.hangout.exception.ResourceNotFoundException;
import com.hangout.model.Restaurant;
import com.hangout.model.Review;
import com.hangout.model.User;
import com.hangout.repository.RestaurantRepository;
import com.hangout.repository.ReviewRepository;
import com.hangout.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReviewService {
    
    private final ReviewRepository reviewRepository;
    private final RestaurantRepository restaurantRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    
    public List<ReviewResponse> getAllReviews() {
        return reviewRepository.findAll().stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }
    
    public List<ReviewResponse> getRestaurantReviews(String restaurantId) {
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
            .orElseThrow(() -> new ResourceNotFoundException("Restaurant not found"));
        
        return reviewRepository.findByRestaurantIdAndStatus(restaurantId, Review.ReviewStatus.PUBLISHED)
            .stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }
    
    public List<ReviewResponse> getUserReviews(String userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        return reviewRepository.findByUserId(userId).stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }
    
    @Transactional
    public ReviewResponse createReview(String restaurantId, String userId, ReviewRequest request) {
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
            .orElseThrow(() -> new ResourceNotFoundException("Restaurant not found"));
        
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        Review review = new Review();
        review.setRestaurantId(restaurantId);
        review.setUserId(userId);
        review.setRating(request.getRating());
        review.setTitle(request.getTitle());
        review.setComment(request.getComment());
        review.setPhotoUrl(request.getPhotoUrl());
        review.setStatus(Review.ReviewStatus.PUBLISHED);
        
        review = reviewRepository.save(review);
        
        // Update user stats
        user.setTotalReviews(user.getTotalReviews() + 1);
        userRepository.save(user);
        
        // Update restaurant rating
        updateRestaurantRating(restaurantId);
        
        // Create notification for restaurant owner (optional)
        
        return toResponse(review);
    }
    
    @Transactional
    public void updateRestaurantRating(String restaurantId) {
        List<Review> reviews = reviewRepository.findByRestaurantIdAndStatus(
            restaurantId, Review.ReviewStatus.PUBLISHED
        );
        
        if (!reviews.isEmpty()) {
            double avgRating = reviews.stream()
                .mapToDouble(Review::getRating)
                .average()
                .orElse(0.0);
            
            Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant not found"));
            
            restaurant.setRating(Math.round(avgRating * 10.0) / 10.0);
            restaurant.setReviewCount(reviews.size());
            restaurantRepository.save(restaurant);
        }
    }
    
    @Transactional
    public void markHelpful(String reviewId, String userId) {
        Review review = reviewRepository.findById(reviewId)
            .orElseThrow(() -> new ResourceNotFoundException("Review not found"));
        
        review.setHelpfulCount(review.getHelpfulCount() + 1);
        reviewRepository.save(review);
    }
    
    @Transactional
    public void deleteReview(String reviewId, String userId) {
        Review review = reviewRepository.findById(reviewId)
            .orElseThrow(() -> new ResourceNotFoundException("Review not found"));
        
        if (!review.getUserId().equals(userId)) {
            throw new BadRequestException("You can only delete your own reviews");
        }
        
        review.setStatus(Review.ReviewStatus.DELETED);
        reviewRepository.save(review);
        
        // Update restaurant rating
        updateRestaurantRating(review.getRestaurantId());
        
        // Update user stats
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.setTotalReviews(Math.max(0, user.getTotalReviews() - 1));
        userRepository.save(user);
    }
    
    private ReviewResponse toResponse(Review review) {
        User user = userRepository.findById(review.getUserId()).orElse(null);
        Restaurant restaurant = restaurantRepository.findById(review.getRestaurantId()).orElse(null);
        
        ReviewResponse response = new ReviewResponse();
        response.setId(review.getId());
        response.setUserId(review.getUserId());
        response.setUserName(user != null ? user.getName() : "Unknown");
        response.setUserAvatar(user != null ? user.getAvatar() : "");
        response.setRestaurantId(review.getRestaurantId());
        response.setRestaurantName(restaurant != null ? restaurant.getName() : "Unknown");
        response.setRestaurantImage(restaurant != null ? restaurant.getImage() : "");
        response.setRating(review.getRating());
        response.setTitle(review.getTitle());
        response.setComment(review.getComment());
        response.setPhotoUrl(review.getPhotoUrl());
        response.setHelpfulCount(review.getHelpfulCount());
        response.setStatus(review.getStatus().name());
        response.setCreatedAt(review.getCreatedAt());
        response.setUpdatedAt(review.getUpdatedAt());
        
        return response;
    }
}
