import '../../core/api/api_client.dart';
import '../../core/constants/app_constants.dart';
import '../../models/game_result.dart';
import '../interfaces/loyalty_service.dart';

class ApiLoyaltyService implements LoyaltyService {
  @override
  Future<int> getUserPoints(String userId) async {
    final response = await ApiClient.get('/loyalty/points');
    final data = ApiClient.handleResponse(response);
    return data != null ? (data['points'] ?? 0) : 0;
  }

  @override
  Future<void> addPoints(String userId, int points) async {
    // Points are only ever earned through a real action (reservation, review, game, ...),
    // each of which already credits them server-side. There is no free-standing
    // "add points" call in API mode.
  }

  @override
  Future<List<GameResult>> getGameHistory(String userId) async {
    final response = await ApiClient.get('/games/history');
    final data = ApiClient.handleResponse(response);

    if (data == null) return [];

    return (data as List).map((json) => GameResult.fromJson(json)).toList();
  }

  @override
  Future<GameResult> playGame(String userId, GameType gameType, int pointsWon) async {
    final path = gameType == GameType.luckyWheel ? 'lucky_wheel' : 'card_memory';
    final body = {
      'outcome': pointsWon > 0 ? 'WIN' : 'LOSE',
      'score': pointsWon,
    };

    final response = await ApiClient.post('/games/$path/play', body);
    final data = ApiClient.handleResponse(response);
    return GameResult.fromJson(data);
  }

  @override
  String getLoyaltyLevel(int points) {
    if (points >= AppConstants.loyaltyLevels['Platinum']!) {
      return 'Platinum';
    } else if (points >= AppConstants.loyaltyLevels['Gold']!) {
      return 'Gold';
    } else if (points >= AppConstants.loyaltyLevels['Silver']!) {
      return 'Silver';
    }
    return 'Bronze';
  }

  @override
  int getNextLevelPoints(int currentPoints) {
    if (currentPoints < AppConstants.loyaltyLevels['Silver']!) {
      return AppConstants.loyaltyLevels['Silver']!;
    } else if (currentPoints < AppConstants.loyaltyLevels['Gold']!) {
      return AppConstants.loyaltyLevels['Gold']!;
    } else if (currentPoints < AppConstants.loyaltyLevels['Platinum']!) {
      return AppConstants.loyaltyLevels['Platinum']!;
    }
    return AppConstants.loyaltyLevels['Platinum']!;
  }
}
