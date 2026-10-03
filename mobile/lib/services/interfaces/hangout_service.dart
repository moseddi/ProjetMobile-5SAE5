import '../../models/hangout.dart';

abstract class HangOutService {
  Future<List<HangOut>> getAllHangOuts();
  Future<HangOut?> getHangOutById(String id);
  Future<HangOut> createHangOut(HangOut hangout);
  Future<JoinRequest> requestToJoin(String hangoutId, String userId, String userName, String userAvatar);
  Future<void> acceptRequest(String hangoutId, String requestId);
  Future<void> declineRequest(String hangoutId, String requestId);
  Future<List<HangOut>> getUserHangOuts(String userId);
  Future<List<JoinRequest>> getUserPendingRequests(String userId);
}
