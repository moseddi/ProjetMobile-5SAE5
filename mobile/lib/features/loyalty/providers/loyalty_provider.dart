import 'package:flutter/foundation.dart';
import '../../../models/loyalty.dart';
import '../../../services/interfaces/loyalty_service.dart';

/// Loyalty module: points per restaurant, the four daily games, exchanges and vouchers.
class LoyaltyProvider with ChangeNotifier {
  final LoyaltyService _loyaltyService;

  LoyaltyProvider(this._loyaltyService);

  LoyaltyOverview? _overview;
  List<Voucher> _vouchers = [];
  bool _isLoading = false;
  String? _error;

  LoyaltyOverview? get overview => _overview;
  List<Voucher> get vouchers => _vouchers;
  bool get isLoading => _isLoading;
  String? get error => _error;

  int? get totalPoints => _overview?.totalPoints;

  GameStatus? statusOf(GameType type) {
    for (final status in _overview?.games ?? const <GameStatus>[]) {
      if (status.type == type) return status;
    }
    return null;
  }

  Future<void> load() async {
    _isLoading = true;
    _error = null;
    notifyListeners();
    try {
      final results = await Future.wait([_loyaltyService.getOverview(), _loyaltyService.getVouchers()]);
      _overview = results[0] as LoyaltyOverview;
      _vouchers = results[1] as List<Voucher>;
    } catch (e) {
      _error = e.toString().replaceAll('Exception: ', '');
    }
    _isLoading = false;
    notifyListeners();
  }

  /// Forget everything when another account signs in.
  void clear() {
    _overview = null;
    _vouchers = [];
    _error = null;
    notifyListeners();
  }

  /// Plays a game. Throws with the server's message when refused.
  Future<GamePlayResult> play(GameType type, String restaurantId) async {
    final result = await _loyaltyService.playGame(type, restaurantId);
    await load();
    return result;
  }

  /// Buys a reward with a restaurant's points. Throws when points are not enough.
  Future<Voucher> redeem(String restaurantId, String rewardType) async {
    final voucher = await _loyaltyService.redeem(restaurantId, rewardType);
    await load();
    return voucher;
  }
}
