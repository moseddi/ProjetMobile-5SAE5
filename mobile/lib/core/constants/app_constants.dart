class AppConstants {
  // App Info
  static const String appName = 'HangOut';
  static const String appTagline = 'Discover, Reserve, Connect';
  
  // Demo Credentials
  static const String demoEmail = 'demo@example.com';
  static const String demoPassword = '123456';
  
  // API Delays (simulated)
  static const Duration apiDelay = Duration(milliseconds: 800);
  static const Duration shortDelay = Duration(milliseconds: 400);
  
  // Loyalty Levels
  static const Map<String, int> loyaltyLevels = {
    'Bronze': 0,
    'Silver': 500,
    'Gold': 1000,
    'Platinum': 2000,
  };
  
  // Image URLs (placeholder - can be replaced with local assets)
  static const String placeholderRestaurant = 'https://images.unsplash.com/photo-1517248135467-4c7edcad34c4?w=800';
  static const String placeholderFood = 'https://images.unsplash.com/photo-1546069901-ba9599a7e63c?w=600';
  static const String placeholderAvatar = 'https://images.unsplash.com/photo-1633332755192-727a05c4013d?w=200';
  static const String placeholderEvent = 'https://images.unsplash.com/photo-1511795409834-ef04bbd61622?w=800';
}
