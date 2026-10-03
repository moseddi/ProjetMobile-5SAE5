import 'package:flutter/foundation.dart';
import '../../../services/mock/mock_loyalty_service.dart';
import '../../../models/game_result.dart';

class LoyaltyProvider with ChangeNotifier {
  final MockLoyaltyService _loyaltyService;
  
  List<GameResult> _gameHistory = [];
  bool _isLoading = false;

  LoyaltyProvider(this._loyaltyService);

  List<GameResult> get gameHistory => _gameHistory;
  bool get isLoading => _isLoading;

  Future<void> loadGameHistory(String userId) async {
    _isLoading = true;
    notifyListeners();
    
    _gameHistory = await _loyaltyService.getGameHistory(userId);
    
    _isLoading = false;
    notifyListeners();
  }

  Future<GameResult> playGame(String userId, GameType gameType, int pointsWon) async {
    final result = await _loyaltyService.playGame(userId, gameType, pointsWon);
    _gameHistory.insert(0, result);
    notifyListeners();
    return result;
  }

  String getLoyaltyLevel(int points) {
    return _loyaltyService.getLoyaltyLevel(points);
  }

  int getNextLevelPoints(int currentPoints) {
    return _loyaltyService.getNextLevelPoints(currentPoints);
  }
}
