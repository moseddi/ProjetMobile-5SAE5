import 'package:flutter/foundation.dart';
import '../../../models/hangout.dart';
import '../../../services/mock/mock_hangout_service.dart';

class HangOutProvider with ChangeNotifier {
  final MockHangOutService _hangoutService;
  
  List<HangOut> _hangouts = [];
  List<JoinRequest> _pendingRequests = [];
  bool _isLoading = false;

  HangOutProvider(this._hangoutService);

  List<HangOut> get hangouts => _hangouts;
  List<JoinRequest> get pendingRequests => _pendingRequests;
  bool get isLoading => _isLoading;
  int get pendingRequestsCount => _pendingRequests.length;

  Future<void> loadHangOuts() async {
    _isLoading = true;
    notifyListeners();
    
    _hangouts = await _hangoutService.getAllHangOuts();
    
    _isLoading = false;
    notifyListeners();
  }

  Future<void> loadUserHangOuts(String userId) async {
    _isLoading = true;
    notifyListeners();
    
    _hangouts = await _hangoutService.getUserHangOuts(userId);
    
    _isLoading = false;
    notifyListeners();
  }

  Future<void> loadPendingRequests(String userId) async {
    _pendingRequests = await _hangoutService.getUserPendingRequests(userId);
    notifyListeners();
  }

  Future<void> createHangOut(HangOut hangout) async {
    _isLoading = true;
    notifyListeners();
    
    await _hangoutService.createHangOut(hangout);
    await loadHangOuts();
    
    _isLoading = false;
    notifyListeners();
  }

  Future<void> requestToJoin(String hangoutId, String userId, String userName, String userAvatar) async {
    await _hangoutService.requestToJoin(hangoutId, userId, userName, userAvatar);
    await loadHangOuts();
  }

  Future<void> acceptRequest(String hangoutId, String requestId) async {
    await _hangoutService.acceptRequest(hangoutId, requestId);
    await loadHangOuts();
    notifyListeners();
  }

  Future<void> declineRequest(String hangoutId, String requestId) async {
    await _hangoutService.declineRequest(hangoutId, requestId);
    await loadHangOuts();
    notifyListeners();
  }
}
