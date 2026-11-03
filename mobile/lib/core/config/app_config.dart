import 'package:flutter/foundation.dart' show kIsWeb;

class AppConfig {
  // Every feature talks to the real backend/database - there is no mock data
  // path to toggle.

  // API base URL. 10.0.2.2 is how the Android emulator reaches the host machine's
  // localhost; a web build talks to the same machine directly via localhost.
  // Override with --dart-define=API_BASE_URL=... for a different host/port.
  static const String apiBaseUrl = kIsWeb
      ? String.fromEnvironment('API_BASE_URL', defaultValue: 'http://localhost:8080/api')
      : String.fromEnvironment('API_BASE_URL', defaultValue: 'http://10.0.2.2:8080/api');

  // Feature flags
  static const bool enableLogging = true;
}
