import 'package:flutter/foundation.dart';
import '../../../models/loyalty.dart';
import '../../../services/interfaces/owner_service.dart';

/// Owner > Cashier: look a client up from their QR code and validate their rewards.
class CashierProvider with ChangeNotifier {
  final OwnerService _ownerService;

  CashierProvider(this._ownerService);

  MemberLookup? _member;
  bool _isLoading = false;
  String? _error;

  MemberLookup? get member => _member;
  bool get isLoading => _isLoading;
  String? get error => _error;

  /// Strips what a QR scanner or a paste may add around the code.
  String _clean(String raw) => raw.trim().toUpperCase().replaceAll(RegExp(r'\s+'), '');

  Future<void> lookup(String rawCode) async {
    final code = _clean(rawCode);
    if (code.isEmpty) {
      _error = 'Enter or scan the client code';
      notifyListeners();
      return;
    }
    _isLoading = true;
    _error = null;
    notifyListeners();
    try {
      _member = await _ownerService.lookupMember(code);
    } catch (e) {
      _member = null;
      _error = e.toString().replaceAll('Exception: ', '');
    }
    _isLoading = false;
    notifyListeners();
  }

  /// Validates a reward, then refreshes the client so it disappears from "active".
  /// Throws with the server's message when it is refused.
  Future<void> useVoucher(String voucherId) async {
    final current = _member;
    await _ownerService.useVoucher(voucherId);
    if (current != null) {
      _member = await _ownerService.lookupMember(current.memberCode);
      notifyListeners();
    }
  }

  void clear() {
    _member = null;
    _error = null;
    notifyListeners();
  }
}
