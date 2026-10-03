import '../../core/constants/app_constants.dart';
import '../../models/user.dart';
import '../../data/mock/mock_users.dart';
import '../interfaces/auth_service.dart';

class MockAuthService implements AuthService {
  User? _currentUser;

  @override
  Future<User?> login(String email, String password) async {
    await Future.delayed(AppConstants.apiDelay);
    
    final user = MockUsers.users.firstWhere(
      (u) => u.email == email,
      orElse: () => MockUsers.users.first,
    );
    
    if (email == AppConstants.demoEmail && password == AppConstants.demoPassword) {
      _currentUser = user;
      return user;
    }
    
    throw Exception('Invalid credentials');
  }

  @override
  Future<User?> register(String name, String email, String password) async {
    await Future.delayed(AppConstants.apiDelay);
    
    final newUser = User(
      id: 'user_${DateTime.now().millisecondsSinceEpoch}',
      name: name,
      email: email,
      avatar: AppConstants.placeholderAvatar,
      points: 0,
      loyaltyLevel: 'Bronze',
    );
    
    MockUsers.users.add(newUser);
    _currentUser = newUser;
    return newUser;
  }

  @override
  Future<void> logout() async {
    await Future.delayed(AppConstants.shortDelay);
    _currentUser = null;
  }

  @override
  User? getCurrentUser() {
    return _currentUser;
  }
}
