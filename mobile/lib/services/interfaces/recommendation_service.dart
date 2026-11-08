import '../../models/recommendation.dart';

abstract class RecommendationService {
  /// Personalised suggestions from the client's history and points.
  Future<Recommendation> getRecommendations();
}
