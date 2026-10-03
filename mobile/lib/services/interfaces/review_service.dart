import '../../models/review.dart';

abstract class ReviewService {
  Future<List<Review>> getReviews({
    String? restaurantId,
    double? minRating,
    String? sortBy,
  });
  
  Future<Review?> getReviewById(String id);
  
  Future<List<Review>> getRestaurantReviews(String restaurantId);
  
  Future<List<Review>> getUserReviews(String userId);
  
  Future<Review> createReview(Review review);
  
  Future<Review> updateReview(String id, Review review);
  
  Future<void> deleteReview(String id);
  
  Future<void> markHelpful(String reviewId);
}
