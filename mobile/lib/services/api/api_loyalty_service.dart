import '../../core/api/api_client.dart';
import '../../models/loyalty.dart';
import '../interfaces/loyalty_service.dart';

class ApiLoyaltyService implements LoyaltyService {
  @override
  Future<LoyaltyOverview> getOverview() async {
    final data = ApiClient.handleResponse(await ApiClient.get('/loyalty/overview'));
    return LoyaltyOverview.fromJson(data as Map<String, dynamic>);
  }

  @override
  Future<GamePlayResult> playGame(GameType type, String restaurantId) async {
    final response = await ApiClient.post('/games/${type.apiName}/play', {'restaurantId': restaurantId});
    return GamePlayResult.fromJson(ApiClient.handleResponse(response) as Map<String, dynamic>);
  }

  @override
  Future<Voucher> redeem(String restaurantId, String rewardType) async {
    final response = await ApiClient.post('/loyalty/redeem', {
      'restaurantId': restaurantId,
      'rewardType': rewardType,
    });
    return Voucher.fromJson(ApiClient.handleResponse(response) as Map<String, dynamic>);
  }

  @override
  Future<List<Voucher>> getVouchers() async {
    final data = ApiClient.handleResponse(await ApiClient.get('/loyalty/vouchers'));
    if (data == null) return [];
    return (data as List).map((v) => Voucher.fromJson(v as Map<String, dynamic>)).toList();
  }
}
