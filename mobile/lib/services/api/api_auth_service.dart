import '../../core/api/api_client.dart';
import '../../models/user.dart';
import '../interfaces/auth_service.dart';

class ApiAuthService implements AuthService {
  User? _currentUser;
  
  @override
  Future<User?> login(String email, String password) async {
    final body = {
      'email': email,
      'password': password,
    };
    
    final response = await ApiClient.post('/auth/login', body);
    final data = ApiClient.handleResponse(response);
    
    if (data != null && data['user'] != null) {
      final token = data['token'];
      // TODO: Store token in secure storage
      
      _currentUser = User.fromJson(data['user']);
      return _currentUser;
    }
    
    return null;
  }

  @override
  Future<User?> register(String name, String email, String password) async {
    final body = {
      'name': name,
      'email': email,
      'password': password,
      'phone': '',
      'avatar': 'https://i.pravatar.cc/150?img=1',
    };
    
    final response = await ApiClient.post('/auth/register', body);
    final data = ApiClient.handleResponse(response);
    
    if (data != null && data['user'] != null) {
      final token = data['token'];
      // TODO: Store token in secure storage
      
      _currentUser = User.fromJson(data['user']);
      return _currentUser;
    }
    
    return null;
  }

  @override
  Future<void> logout() async {
    _currentUser = null;
    // TODO: Clear token from secure storage
  }

  @override
  User? getCurrentUser() {
    return _currentUser;
  }
}
