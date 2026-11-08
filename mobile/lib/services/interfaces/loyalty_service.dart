import '../../models/loyalty.dart';

abstract class LoyaltyService {
  /// Total points, daily games status and per-restaurant balances with their offers.
  Future<LoyaltyOverview> getOverview();

  /// Plays a game for [restaurantId]; the server decides and returns the reward.
  /// Throws with the server's message (e.g. already played today).
  Future<GamePlayResult> playGame(GameType type, String restaurantId);

  /// Exchanges a restaurant's points for a voucher. Throws if points are not enough.
  Future<Voucher> redeem(String restaurantId, String rewardType);

  Future<List<Voucher>> getVouchers();
}
