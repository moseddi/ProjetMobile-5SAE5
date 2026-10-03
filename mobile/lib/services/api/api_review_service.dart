import '../../core/api/api_client.dart';
import '../../models/review.dart';
import '../interfaces/review_service.dart';

class ApiReviewService implements ReviewService {
  @override
  Future<List<Review>> getReviews({
    String? restaurantId,
    double? minRating,
    String? sortBy,
  }) async {
    String endpoint = '/reviews';
    if (restaurantId != null) {
      endpoint = '/reviews/restaurant/$restaurantId';
    }
    
    final response = await ApiClient.get(endpoint);
    final data = ApiClient.handleResponse(response);
    
    if (data == null) return [];
    
    List<Review> reviews = (data as List)
        .map((json) => Review.fromJson(json))
        .toList();
    
    // Apply filters
    if (minRating != null) {
      reviews = reviews.where((r) => r.rating >= minRating).toList();
    }
    
    // Apply sorting
    if (sortBy == 'rating') {
      reviews.sort((a, b) => b.rating.compareTo(a.rating));
    } else {
      reviews.sort((a, b) => b.createdAt.compareTo(a.createdAt));
    }
    
    return reviews;
  }

  @override
  Future<Review?> getReviewById(String id) async {
    final response = await ApiClient.get('/reviews/$id');
    final data = ApiClient.handleResponse(response);
    return data != null ? Review.fromJson(data) : null;
  }

  @override
  Future<List<Review>> getRestaurantReviews(String restaurantId) async {
    final response = await ApiClient.get('/reviews/restaurant/$restaurantId');
    final data = ApiClient.handleResponse(response);
    
    if (data == null) return [];
    
    return (data as List)
        .map((json) => Review.fromJson(json))
        .toList();
  }

  @override
  Future<List<Review>> getUserReviews(String userId) async {
    final response = await ApiClient.get('/reviews/user/$userId');
    final data = ApiClient.handleResponse(response);
    
    if (data == null) return [];
    
    return (data as List)
        .map((json) => Review.fromJson(json))
        .toList();
  }

  @override
  Future<Review> createReview(Review review) async {
    final body = {
      'rating': review.rating,
      'title': review.title,
      'comment': review.comment,
      'photoUrl': review.photoUrl,
    };
    
    final response = await ApiClient.post(
      '/reviews/restaurant/${review.restaurantId}?userId=${review.userId}',
      body,
    );
    
    final data = ApiClient.handleResponse(response);
    return Review.fromJson(data);
  }

  @override
  Future<Review> updateReview(String id, Review review) async {
    final body = {
      'rating': review.rating,
      'title': review.title,
      'comment': review.comment,
      'photoUrl': review.photoUrl,
    };
    
    final response = await ApiClient.put('/reviews/$id', body);
    final data = ApiClient.handleResponse(response);
    return Review.fromJson(data);
  }

  @override
  Future<void> deleteReview(String id) async {
    await ApiClient.delete('/reviews/$id');
  }

  @override
  Future<void> markHelpful(String reviewId) async {
    // Need userId in real implementation
    await ApiClient.put('/reviews/$reviewId/helpful?userId=current', {});
  }
}
