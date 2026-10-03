import 'dart:convert';
import 'package:http/http.dart' as http;

class ApiClient {
  static const String baseUrl = String.fromEnvironment(
    'API_BASE_URL',
    defaultValue: 'http://10.0.2.2:8080/api', // Android emulator
  );
  
  static const Duration timeout = Duration(seconds: 30);
  
  static Map<String, String> get _headers => {
    'Content-Type': 'application/json',
    'Accept': 'application/json',
  };
  
  static Future<http.Response> get(String endpoint) async {
    final uri = Uri.parse('$baseUrl$endpoint');
    try {
      return await http.get(uri, headers: _headers).timeout(timeout);
    } catch (e) {
      throw ApiException('Network error: $e');
    }
  }
  
  static Future<http.Response> post(String endpoint, Map<String, dynamic> body) async {
    final uri = Uri.parse('$baseUrl$endpoint');
    try {
      return await http.post(
        uri,
        headers: _headers,
        body: jsonEncode(body),
      ).timeout(timeout);
    } catch (e) {
      throw ApiException('Network error: $e');
    }
  }
  
  static Future<http.Response> put(String endpoint, Map<String, dynamic> body) async {
    final uri = Uri.parse('$baseUrl$endpoint');
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
    final uri = Uri.parse('$baseUrl$endpoint');
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
    } else if (response.statusCode == 400) {
      final error = jsonDecode(response.body);
      throw ApiException(error['message'] ?? 'Bad request');
    } else if (response.statusCode == 404) {
      throw ApiException('Resource not found');
    } else if (response.statusCode == 500) {
      throw ApiException('Server error');
    } else {
      throw ApiException('Unexpected error: ${response.statusCode}');
    }
  }
}

class ApiException implements Exception {
  final String message;
  ApiException(this.message);
  
  @override
  String toString() => message;
}
