import 'dart:convert';
import 'package:http/http.dart' as http;
import 'package:shared_preferences/shared_preferences.dart';
import '../config/app_config.dart';

class ApiClient {
  static const Duration timeout = Duration(seconds: 30);
  static const String _tokenKey = 'auth_token';

  static String? _token;

  static bool get hasToken => _token != null && _token!.isNotEmpty;

  /// Loads any previously-saved auth token into memory. Call once at app startup,
  /// before the first widget that might need to know whether a session exists.
  static Future<void> init() async {
    final prefs = await SharedPreferences.getInstance();
    _token = prefs.getString(_tokenKey);
  }

  static Future<void> setToken(String token) async {
    _token = token;
    final prefs = await SharedPreferences.getInstance();
    await prefs.setString(_tokenKey, token);
  }

  static Future<void> clearToken() async {
    _token = null;
    final prefs = await SharedPreferences.getInstance();
    await prefs.remove(_tokenKey);
  }

  static Map<String, String> get _headers => {
        'Content-Type': 'application/json',
        'Accept': 'application/json',
        if (hasToken) 'Authorization': 'Bearer $_token',
      };

  static Future<http.Response> get(String endpoint) async {
    final uri = Uri.parse('${AppConfig.apiBaseUrl}$endpoint');
    try {
      return await http.get(uri, headers: _headers).timeout(timeout);
    } catch (e) {
      throw ApiException('Network error: $e');
    }
  }

  static Future<http.Response> post(
    String endpoint,
    Map<String, dynamic> body, {
    Duration? requestTimeout,
  }) async {
    final uri = Uri.parse('${AppConfig.apiBaseUrl}$endpoint');
    try {
      return await http
          .post(
            uri,
            headers: _headers,
            body: jsonEncode(body),
          )
          .timeout(requestTimeout ?? timeout);
    } catch (e) {
      throw ApiException('Network error: $e');
    }
  }

  static Future<http.Response> put(String endpoint, Map<String, dynamic> body) async {
    final uri = Uri.parse('${AppConfig.apiBaseUrl}$endpoint');
    try {
      return await http.put(
        uri,
        headers: _headers,
        body: jsonEncode(body),
      ).timeout(timeout);
    } catch (e) {
      throw ApiException('Network error: $e');
    }
  }

  static Future<http.Response> delete(String endpoint) async {
    final uri = Uri.parse('${AppConfig.apiBaseUrl}$endpoint');
    try {
      return await http.delete(uri, headers: _headers).timeout(timeout);
    } catch (e) {
      throw ApiException('Network error: $e');
    }
  }

  static dynamic handleResponse(http.Response response) {
    if (response.statusCode >= 200 && response.statusCode < 300) {
      if (response.body.isEmpty) return null;
      return jsonDecode(response.body);
    }

    String message = 'Unexpected error (${response.statusCode})';
    try {
      final decoded = jsonDecode(response.body);
      if (decoded is Map) {
        if (decoded['message'] != null) {
          message = decoded['message'].toString();
        } else if (decoded['errors'] is Map && (decoded['errors'] as Map).isNotEmpty) {
          message = (decoded['errors'] as Map).values.first.toString();
        }
      }
    } catch (_) {
      // Response body wasn't JSON; fall back to the generic message above.
    }

    if (response.statusCode == 401 || response.statusCode == 403) {
      throw UnauthorizedException(message);
    }
    throw ApiException(message);
  }
}

class ApiException implements Exception {
  final String message;
  ApiException(this.message);

  @override
  String toString() => message;
}

/// Thrown on 401/403 — the caller should treat the session as invalid
/// (clear it and route back to login) rather than showing a generic error.
class UnauthorizedException extends ApiException {
  UnauthorizedException(super.message);
}
