import 'package:flutter/foundation.dart';
import '../../../models/review.dart';
import '../../../services/interfaces/review_service.dart';

class ReviewProvider with ChangeNotifier {
  final ReviewService _reviewService;
  
  List<Review> _reviews = [];
  bool _isLoading = false;
  String? _errorMessage;
  
  String? _selectedRestaurantId;
  double? _selectedRating;
  String _sortBy = 'newest';

  ReviewProvider(this._reviewService);

  List<Review> get reviews => _reviews;
  bool get isLoading => _isLoading;
  String? get errorMessage => _errorMessage;
  String? get selectedRestaurantId => _selectedRestaurantId;
  double? get selectedRating => _selectedRating;
  String get sortBy => _sortBy;

  Future<void> loadReviews() async {
    _isLoading = true;
    _errorMessage = null;
    notifyListeners();
    
    try {
      _reviews = await _reviewService.getReviews(
        restaurantId: _selectedRestaurantId,
        minRating: _selectedRating,
        sortBy: _sortBy,
      );
      _isLoading = false;
      notifyListeners();
    } catch (e) {
      _errorMessage = e.toString();
      _isLoading = false;
      notifyListeners();
    }
  }

  Future<List<Review>> getRestaurantReviews(String restaurantId) async {
    return await _reviewService.getRestaurantReviews(restaurantId);
  }

  Future<void> createReview(Review review) async {
    _isLoading = true;
    notifyListeners();
    
    try {
      await _reviewService.createReview(review);
      await loadReviews();
      _isLoading = false;
      notifyListeners();
    } catch (e) {
      _errorMessage = e.toString();
      _isLoading = false;
      notifyListeners();
      rethrow;
    }
  }

  Future<void> markHelpful(String reviewId) async {
    await _reviewService.markHelpful(reviewId);
    await loadReviews();
  }

  void setRestaurantFilter(String? restaurantId) {
    _selectedRestaurantId = restaurantId;
    loadReviews();
  }

  void setRatingFilter(double? rating) {
    _selectedRating = rating;
    loadReviews();
  }

  void setSortBy(String sortBy) {
    _sortBy = sortBy;
    loadReviews();
  }

  void clearFilters() {
    _selectedRestaurantId = null;
    _selectedRating = null;
    _sortBy = 'newest';
    loadReviews();
  }
}
