class AppConstants {
  // App Info
  static const String appName = 'HangOut';
  static const String appTagline = 'Discover, Reserve, Connect';

  // Demo Credentials (pre-fills the login form; the account itself lives in the database)
  static const String demoEmail = 'demo@example.com';
  static const String demoPassword = '123456';

  // Loyalty level thresholds (business rule, not stored data)
  static const Map<String, int> loyaltyLevels = {
    'Bronze': 0,
    'Silver': 500,
    'Gold': 1000,
    'Platinum': 2000,
  };
}
