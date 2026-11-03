import 'package:flutter/foundation.dart';
import '../../../models/user.dart';
import '../../../services/interfaces/auth_service.dart';

class AuthProvider with ChangeNotifier {
  final AuthService _authService;

  User? _currentUser;
  bool _isLoading = false;
  String? _errorMessage;

  AuthProvider(this._authService);

  User? get currentUser => _currentUser;
  bool get isLoading => _isLoading;
  String? get errorMessage => _errorMessage;
  bool get isAuthenticated => _currentUser != null;

  /// Tries to resume a previously-saved session (e.g. a stored JWT). Returns
  /// true and populates [currentUser] if that session is still valid.
  Future<bool> tryRestoreSession() async {
    try {
      final user = await _authService.fetchCurrentUser();
      if (user != null) {
        _currentUser = user;
        notifyListeners();
        return true;
      }
    } catch (_) {
      // No valid session (expired/invalid token, offline, etc.) - fall through to login.
    }
    return false;
  }

  Future<bool> login(String email, String password) async {
    _isLoading = true;
    _errorMessage = null;
    notifyListeners();

    try {
      final user = await _authService.login(email, password);
      _currentUser = user;
      _isLoading = false;
      notifyListeners();
      return true;
    } catch (e) {
      _errorMessage = e.toString().replaceAll('Exception: ', '');
      _isLoading = false;
      notifyListeners();
      return false;
    }
  }

  Future<bool> register(String name, String email, String password) async {
    _isLoading = true;
    _errorMessage = null;
    notifyListeners();

    try {
      final user = await _authService.register(name, email, password);
      _currentUser = user;
      _isLoading = false;
      notifyListeners();
      return true;
    } catch (e) {
      _errorMessage = e.toString().replaceAll('Exception: ', '');
      _isLoading = false;
      notifyListeners();
      return false;
    }
  }

  /// Returns null on success, otherwise the error message to show.
  Future<String?> submitOwnerApplication(Map<String, dynamic> data) async {
    _isLoading = true;
    notifyListeners();
    try {
      await _authService.submitOwnerApplication(data);
      return null;
    } catch (e) {
      return e.toString().replaceAll('Exception: ', '');
    } finally {
      _isLoading = false;
      notifyListeners();
    }
  }

  Future<void> logout() async {
    await _authService.logout();
    _currentUser = null;
    notifyListeners();
  }

  void updateUser(User user) {
    _currentUser = user;
    notifyListeners();
  }
}
