import '../../models/game_result.dart';

abstract class LoyaltyService {
  Future<int> getUserPoints(String userId);
  Future<void> addPoints(String userId, int points);
  Future<List<GameResult>> getGameHistory(String userId);
  Future<GameResult> playGame(String userId, GameType gameType, int pointsWon);
  String getLoyaltyLevel(int points);
  int getNextLevelPoints(int currentPoints);
}
