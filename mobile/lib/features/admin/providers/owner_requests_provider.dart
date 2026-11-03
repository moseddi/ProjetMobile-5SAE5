import 'package:flutter/foundation.dart';
import '../../../models/owner_application.dart';
import '../../../services/interfaces/admin_service.dart';

/// Admin > Requests: restaurant owners asking to join the platform.
class OwnerRequestsProvider extends ChangeNotifier {
  final AdminService _adminService;

  OwnerRequestsProvider(this._adminService);

  List<OwnerApplication> _applications = [];
  bool _isLoading = false;
  String? _error;
  OwnerApplicationStatus? _filter = OwnerApplicationStatus.pending;

  List<OwnerApplication> get applications => _applications;
  bool get isLoading => _isLoading;
  String? get error => _error;
  OwnerApplicationStatus? get filter => _filter;

  int get pendingCount => _applications.where((a) => a.isPending).length;

  int countFor(OwnerApplicationStatus? status) =>
      status == null ? _applications.length : _applications.where((a) => a.status == status).length;

  List<OwnerApplication> get visible =>
      _filter == null ? _applications : _applications.where((a) => a.status == _filter).toList();

  void setFilter(OwnerApplicationStatus? status) {
    _filter = status;
    notifyListeners();
  }

  Future<void> load() async {
    _isLoading = true;
    _error = null;
    notifyListeners();
    try {
      _applications = await _adminService.getOwnerApplications();
    } catch (e) {
      _error = e.toString().replaceAll('Exception: ', '');
    }
    _isLoading = false;
    notifyListeners();
  }

  /// Throws with the server's message when it refuses (e.g. e-mail already taken).
  Future<void> accept(String id) async {
    await _adminService.acceptOwnerApplication(id);
    await load();
  }

  Future<void> reject(String id, {String? reason}) async {
    await _adminService.rejectOwnerApplication(id, reason: reason);
    await load();
  }
}
