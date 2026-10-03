import '../../core/constants/app_constants.dart';
import '../../models/review.dart';
import '../../data/mock/mock_reviews.dart';
import '../interfaces/review_service.dart';

class MockReviewService implements ReviewService {
  @override
  Future<List<Review>> getReviews({
    String? restaurantId,
    double? minRating,
    String? sortBy,
  }) async {
    await Future.delayed(AppConstants.apiDelay);
    
    var filteredReviews = List<Review>.from(MockReviews.reviews);
    
    if (restaurantId != null) {
      filteredReviews = filteredReviews
          .where((r) => r.restaurantId == restaurantId)
          .toList();
    }
    
    if (minRating != null) {
      filteredReviews = filteredReviews
          .where((r) => r.rating >= minRating)
          .toList();
    }
    
    if (sortBy == 'rating') {
      filteredReviews.sort((a, b) => b.rating.compareTo(a.rating));
    } else {
      // Sort by newest by default
      filteredReviews.sort((a, b) => b.createdAt.compareTo(a.createdAt));
    }
    
    return filteredReviews;
  }

  @override
  Future<Review?> getReviewById(String id) async {
    await Future.delayed(AppConstants.shortDelay);
    try {
      return MockReviews.reviews.firstWhere((r) => r.id == id);
    } catch (e) {
      return null;
    }
  }

  @override
  Future<List<Review>> getRestaurantReviews(String restaurantId) async {
    await Future.delayed(AppConstants.shortDelay);
    return MockReviews.reviews
        .where((r) => r.restaurantId == restaurantId)
        .toList()
      ..sort((a, b) => b.createdAt.compareTo(a.createdAt));
  }

  @override
  Future<List<Review>> getUserReviews(String userId) async {
    await Future.delayed(AppConstants.shortDelay);
    return MockReviews.reviews
        .where((r) => r.userId == userId)
        .toList()
      ..sort((a, b) => b.createdAt.compareTo(a.createdAt));
  }

  @override
  Future<Review> createReview(Review review) async {
    await Future.delayed(AppConstants.apiDelay);
    
    final newReview = review.copyWith(
      id: 'rev_${DateTime.now().millisecondsSinceEpoch}',
      createdAt: DateTime.now(),
      status: ReviewStatus.published,
    );
    
    MockReviews.reviews.insert(0, newReview);
    return newReview;
  }

  @override
  Future<Review> updateReview(String id, Review review) async {
    await Future.delayed(AppConstants.apiDelay);
    
    final index = MockReviews.reviews.indexWhere((r) => r.id == id);
    if (index != -1) {
      final updatedReview = review.copyWith(
        id: id,
        updatedAt: DateTime.now(),
      );
      MockReviews.reviews[index] = updatedReview;
      return updatedReview;
    }
    throw Exception('Review not found');
  }

  @override
  Future<void> deleteReview(String id) async {
    await Future.delayed(AppConstants.shortDelay);
    MockReviews.reviews.removeWhere((r) => r.id == id);
  }

  @override
  Future<void> markHelpful(String reviewId) async {
    await Future.delayed(AppConstants.shortDelay);
    
    final index = MockReviews.reviews.indexWhere((r) => r.id == reviewId);
    if (index != -1) {
      final review = MockReviews.reviews[index];
      MockReviews.reviews[index] = review.copyWith(
        helpfulCount: review.helpfulCount + 1,
      );
    }
  }
}
