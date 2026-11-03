import '../../models/hangout.dart';

abstract class HangOutService {
  /// Every hangout. Each one carries the signed-in user's own request status
  /// ([HangOut.myRequestStatus]) and, for the organizer, the pending requests.
  Future<List<HangOut>> getAllHangOuts();

  Future<HangOut> createHangOut(HangOut hangout);

  /// Asks the organizer to let the signed-in user join.
  Future<HangOut> requestToJoin(String hangoutId);

  Future<void> acceptRequest(String hangoutId, String requestId);
  Future<void> declineRequest(String hangoutId, String requestId);
}
