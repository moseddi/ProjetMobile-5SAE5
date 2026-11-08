import '../../core/api/api_client.dart';
import '../../models/recommendation.dart';
import '../interfaces/recommendation_service.dart';

class ApiRecommendationService implements RecommendationService {
  @override
  Future<Recommendation> getRecommendations() async {
    final data = ApiClient.handleResponse(await ApiClient.get('/recommendations'));
    return Recommendation.fromJson(data as Map<String, dynamic>);
  }
}
