import '../../core/constants/app_constants.dart';
import '../../models/game_result.dart';
import '../../data/mock/mock_users.dart';
import '../interfaces/loyalty_service.dart';

class MockLoyaltyService implements LoyaltyService {
  final List<GameResult> _gameHistory = [];

  @override
  Future<int> getUserPoints(String userId) async {
    await Future.delayed(AppConstants.shortDelay);
    final user = MockUsers.users.firstWhere((u) => u.id == userId);
    return user.points;
  }

  @override
  Future<void> addPoints(String userId, int points) async {
    await Future.delayed(AppConstants.shortDelay);
    final userIndex = MockUsers.users.indexWhere((u) => u.id == userId);
    if (userIndex != -1) {
      final user = MockUsers.users[userIndex];
      final newPoints = user.points + points;
      final newLevel = getLoyaltyLevel(newPoints);
      MockUsers.users[userIndex] = user.copyWith(
        points: newPoints,
        loyaltyLevel: newLevel,
      );
    }
  }

  @override
  Future<List<GameResult>> getGameHistory(String userId) async {
    await Future.delayed(AppConstants.shortDelay);
    return _gameHistory.where((g) => g.userId == userId).toList();
  }

  @override
  Future<GameResult> playGame(String userId, GameType gameType, int pointsWon) async {
    await Future.delayed(AppConstants.apiDelay);
    
    final result = GameResult(
      id: 'game_${DateTime.now().millisecondsSinceEpoch}',
      userId: userId,
      gameType: gameType,
      pointsWon: pointsWon,
      date: DateTime.now(),
    );
    
    _gameHistory.add(result);
    await addPoints(userId, pointsWon);
    
    return result;
  }

  @override
  String getLoyaltyLevel(int points) {
    if (points >= AppConstants.loyaltyLevels['Platinum']!) {
      return 'Platinum';
    } else if (points >= AppConstants.loyaltyLevels['Gold']!) {
      return 'Gold';
    } else if (points >= AppConstants.loyaltyLevels['Silver']!) {
      return 'Silver';
    } else {
      return 'Bronze';
    }
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
