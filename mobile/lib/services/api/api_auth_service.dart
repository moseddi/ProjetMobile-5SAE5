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
      await ApiClient.setToken(data['token']);
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
      await ApiClient.setToken(data['token']);
      _currentUser = User.fromJson(data['user']);
      return _currentUser;
    }

    return null;
  }

  @override
  Future<void> submitOwnerApplication(Map<String, dynamic> data) async {
    final response = await ApiClient.post('/auth/owner-application', data);
    ApiClient.handleResponse(response);
  }

  @override
  Future<void> logout() async {
    _currentUser = null;
    await ApiClient.clearToken();
  }

  @override
  User? getCurrentUser() {
    return _currentUser;
  }

  @override
  Future<User?> fetchCurrentUser() async {
    if (!ApiClient.hasToken) return null;

    final response = await ApiClient.get('/auth/me');
    final data = ApiClient.handleResponse(response);

    if (data != null) {
      _currentUser = User.fromJson(data);
      return _currentUser;
    }

    return null;
  }
}
