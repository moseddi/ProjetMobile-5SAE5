import '../../models/user.dart';

abstract class AuthService {
  Future<User?> login(String email, String password);
  Future<User?> register(String name, String email, String password);
  Future<void> logout();

  /// Public: a restaurant owner asks to join. Nothing is created until an admin accepts.
  Future<void> submitOwnerApplication(Map<String, dynamic> data);
  User? getCurrentUser();

  /// Re-validates any persisted session (e.g. a saved JWT) against the backend
  /// and returns the up-to-date user, or null if there is no valid session.
  Future<User?> fetchCurrentUser();
}
