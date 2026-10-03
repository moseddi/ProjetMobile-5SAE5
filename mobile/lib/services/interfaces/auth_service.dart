import '../../models/user.dart';

abstract class AuthService {
  Future<User?> login(String email, String password);
  Future<User?> register(String name, String email, String password);
  Future<void> logout();
  User? getCurrentUser();
}
