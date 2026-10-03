class AppConfig {
  // Set to true to use real API, false to use mock data
  static const bool useApi = bool.fromEnvironment('USE_API', defaultValue: false);
  
  // API base URL
  static const String apiBaseUrl = String.fromEnvironment(
    'API_BASE_URL',
    defaultValue: 'http://10.0.2.2:8080/api',
  );
  
  // Feature flags
  static const bool enableLogging = true;
}
