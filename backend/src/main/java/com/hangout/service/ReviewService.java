package com.hangout.service;

import com.hangout.util.Newest;
import com.hangout.dto.ReviewRequest;
import com.hangout.dto.ReviewResponse;
import com.hangout.exception.ResourceNotFoundException;
import com.hangout.model.Restaurant;
import com.hangout.model.Review;
import com.hangout.model.User;
import com.hangout.repository.RestaurantRepository;
import com.hangout.repository.ReviewRepository;
import com.hangout.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private static final int POINTS_FOR_REVIEW = 10;

    private final ReviewRepository reviewRepository;
    private final RestaurantRepository restaurantRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final LoyaltyService loyaltyService;

    public List<ReviewResponse> getAllReviews() {
        return reviewRepository.findAll().stream()
            .filter(r -> r.getStatus() != Review.ReviewStatus.DELETED)
            .sorted(Newest.first(Review::getId))
            .map(this::toResponse)
            .collect(Collectors.toList());
    }

    public ReviewResponse getReviewById(String id) {
        return toResponse(getReviewOrThrow(id));
    }

    public List<ReviewResponse> getRestaurantReviews(String restaurantId) {
        restaurantRepository.findById(restaurantId)
            .orElseThrow(() -> new ResourceNotFoundException("Restaurant not found"));

        return reviewRepository.findByRestaurantIdAndStatus(restaurantId, Review.ReviewStatus.PUBLISHED)
            .stream()
            .sorted(Newest.first(Review::getId))
            .map(this::toResponse)
            .collect(Collectors.toList());
    }

    public List<ReviewResponse> getUserReviews(String userId) {
        return reviewRepository.findByUserId(userId).stream()
            .filter(r -> r.getStatus() != Review.ReviewStatus.DELETED)
            .sorted(Newest.first(Review::getId))
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

        user.setTotalReviews(user.getTotalReviews() + 1);
        userRepository.save(user);

        updateRestaurantRating(restaurantId);

        loyaltyService.awardPoints(userId, POINTS_FOR_REVIEW,
            com.hangout.model.PointTransaction.TransactionType.EARN_REVIEW,
            "Thanks for your review!", review.getId());

        if (restaurant.getOwnerId() != null) {
            var notification = new com.hangout.model.Notification();
            notification.setUserId(restaurant.getOwnerId());
            notification.setTitle("New review");
            notification.setMessage(user.getName() + " left a " + request.getRating() + "-star review");
            notification.setType(com.hangout.model.Notification.NotificationType.REVIEW);
            notification.setRelatedId(review.getId());
            notificationService.createNotification(notification);
        }

        return toResponse(review);
    }

    @Transactional
    public ReviewResponse updateOwnReview(String reviewId, String userId, ReviewRequest request) {
        Review review = getReviewOrThrow(reviewId);
        if (!review.getUserId().equals(userId)) {
            throw new AccessDeniedException("You can only edit your own reviews");
        }

        review.setRating(request.getRating());
        review.setTitle(request.getTitle());
        review.setComment(request.getComment());
        review.setPhotoUrl(request.getPhotoUrl());
        review = reviewRepository.save(review);

        updateRestaurantRating(review.getRestaurantId());

        return toResponse(review);
    }

    @Transactional
    public void updateRestaurantRating(String restaurantId) {
        List<Review> reviews = reviewRepository.findByRestaurantIdAndStatus(
            restaurantId, Review.ReviewStatus.PUBLISHED
        );

        Restaurant restaurant = restaurantRepository.findById(restaurantId)
            .orElseThrow(() -> new ResourceNotFoundException("Restaurant not found"));

        if (!reviews.isEmpty()) {
            double avgRating = reviews.stream()
                .mapToDouble(Review::getRating)
                .average()
                .orElse(0.0);
            restaurant.setRating(Math.round(avgRating * 10.0) / 10.0);
        } else {
            restaurant.setRating(0.0);
        }
        restaurant.setReviewCount(reviews.size());
        restaurantRepository.save(restaurant);
    }

    @Transactional
    public void markHelpful(String reviewId) {
        Review review = getReviewOrThrow(reviewId);
        review.setHelpfulCount(review.getHelpfulCount() + 1);
        reviewRepository.save(review);
    }

    @Transactional
    public void deleteReview(String reviewId, String userId, boolean isAdmin) {
        Review review = getReviewOrThrow(reviewId);

        if (!isAdmin && !review.getUserId().equals(userId)) {
            throw new AccessDeniedException("You can only delete your own reviews");
        }

        review.setStatus(Review.ReviewStatus.DELETED);
        reviewRepository.save(review);

        updateRestaurantRating(review.getRestaurantId());

        userRepository.findById(review.getUserId()).ifPresent(user -> {
            user.setTotalReviews(Math.max(0, user.getTotalReviews() - 1));
            userRepository.save(user);
        });
    }

    private Review getReviewOrThrow(String id) {
        return reviewRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Review not found"));
    }

    private ReviewResponse toResponse(Review review) {
        User user = review.getUserId() != null
            ? userRepository.findById(review.getUserId()).orElse(null)
            : null;
        Restaurant restaurant = review.getRestaurantId() != null
            ? restaurantRepository.findById(review.getRestaurantId()).orElse(null)
            : null;

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
