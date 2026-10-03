import '../../core/constants/app_constants.dart';
import '../../models/hangout.dart';
import '../../data/mock/mock_hangouts.dart';
import '../interfaces/hangout_service.dart';

class MockHangOutService implements HangOutService {
  @override
  Future<List<HangOut>> getAllHangOuts() async {
    await Future.delayed(AppConstants.apiDelay);
    return MockHangOuts.hangouts;
  }

  @override
  Future<HangOut?> getHangOutById(String id) async {
    await Future.delayed(AppConstants.shortDelay);
    try {
      return MockHangOuts.hangouts.firstWhere((h) => h.id == id);
    } catch (e) {
      return null;
    }
  }

  @override
  Future<HangOut> createHangOut(HangOut hangout) async {
    await Future.delayed(AppConstants.apiDelay);
    MockHangOuts.hangouts.add(hangout);
    return hangout;
  }

  @override
  Future<JoinRequest> requestToJoin(
    String hangoutId,
    String userId,
    String userName,
    String userAvatar,
  ) async {
    await Future.delayed(AppConstants.shortDelay);
    
    final request = JoinRequest(
      id: 'req_${DateTime.now().millisecondsSinceEpoch}',
      userId: userId,
      userName: userName,
      userAvatar: userAvatar,
      hangoutId: hangoutId,
      requestedAt: DateTime.now(),
      status: JoinRequestStatus.pending,
    );
    
    final hangoutIndex = MockHangOuts.hangouts.indexWhere((h) => h.id == hangoutId);
    if (hangoutIndex != -1) {
      final hangout = MockHangOuts.hangouts[hangoutIndex];
      final updatedRequests = List<JoinRequest>.from(hangout.pendingRequests)..add(request);
      MockHangOuts.hangouts[hangoutIndex] = hangout.copyWith(pendingRequests: updatedRequests);
    }
    
    return request;
  }

  @override
  Future<void> acceptRequest(String hangoutId, String requestId) async {
    await Future.delayed(AppConstants.shortDelay);
    
    final hangoutIndex = MockHangOuts.hangouts.indexWhere((h) => h.id == hangoutId);
    if (hangoutIndex != -1) {
      final hangout = MockHangOuts.hangouts[hangoutIndex];
      final request = hangout.pendingRequests.firstWhere((r) => r.id == requestId);
      
      final updatedRequests = hangout.pendingRequests
          .where((r) => r.id != requestId)
          .toList();
      
      final updatedParticipants = List<String>.from(hangout.participants)
        ..add(request.userId);
      
      MockHangOuts.hangouts[hangoutIndex] = hangout.copyWith(
        participants: updatedParticipants,
        pendingRequests: updatedRequests,
      );
    }
  }

  @override
  Future<void> declineRequest(String hangoutId, String requestId) async {
    await Future.delayed(AppConstants.shortDelay);
    
    final hangoutIndex = MockHangOuts.hangouts.indexWhere((h) => h.id == hangoutId);
    if (hangoutIndex != -1) {
      final hangout = MockHangOuts.hangouts[hangoutIndex];
      final updatedRequests = hangout.pendingRequests
          .where((r) => r.id != requestId)
          .toList();
      
      MockHangOuts.hangouts[hangoutIndex] = hangout.copyWith(
        pendingRequests: updatedRequests,
      );
    }
  }

  @override
  Future<List<HangOut>> getUserHangOuts(String userId) async {
    await Future.delayed(AppConstants.shortDelay);
    return MockHangOuts.hangouts
        .where((h) => h.creatorId == userId || h.participants.contains(userId))
        .toList();
  }

  @override
  Future<List<JoinRequest>> getUserPendingRequests(String userId) async {
    await Future.delayed(AppConstants.shortDelay);
    final List<JoinRequest> allRequests = [];
    
    for (var hangout in MockHangOuts.hangouts) {
      if (hangout.creatorId == userId) {
        allRequests.addAll(hangout.pendingRequests);
      }
    }
    
    return allRequests;
  }
}
