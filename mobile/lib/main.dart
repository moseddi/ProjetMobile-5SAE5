import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'core/theme/app_theme.dart';
import 'core/navigation/app_router.dart';
import 'core/config/app_config.dart';
import 'services/mock/mock_auth_service.dart';
import 'services/mock/mock_restaurant_service.dart';
import 'services/mock/mock_reservation_service.dart';
import 'services/mock/mock_loyalty_service.dart';
import 'services/mock/mock_event_service.dart';
import 'services/mock/mock_hangout_service.dart';
import 'services/mock/mock_profile_service.dart';
import 'services/mock/mock_review_service.dart';
import 'services/api/api_auth_service.dart';
import 'services/api/api_restaurant_service.dart';
import 'services/api/api_review_service.dart';
import 'services/api/api_reservation_service.dart';
import 'services/interfaces/auth_service.dart';
import 'services/interfaces/restaurant_service.dart';
import 'services/interfaces/review_service.dart';
import 'services/interfaces/reservation_service.dart';
import 'features/auth/providers/auth_provider.dart';
import 'features/loyalty/providers/loyalty_provider.dart';
import 'features/hangout/providers/hangout_provider.dart';
import 'features/profile/providers/profile_provider.dart';
import 'features/reviews/providers/review_provider.dart';

void main() {
  runApp(const HangOutApp());
}

class HangOutApp extends StatelessWidget {
  const HangOutApp({super.key});

  @override
  Widget build(BuildContext context) {
    // Choose services based on configuration
    final AuthService authService = AppConfig.useApi 
        ? ApiAuthService() 
        : MockAuthService();
    
    final RestaurantService restaurantService = AppConfig.useApi
        ? ApiRestaurantService()
        : MockRestaurantService();
    
    final ReviewService reviewService = AppConfig.useApi
        ? ApiReviewService()
        : MockReviewService();
    
    final ReservationService reservationService = AppConfig.useApi
        ? ApiReservationService()
        : MockReservationService();
    
    return MultiProvider(
      providers: [
        Provider<AuthService>.value(value: authService),
        Provider<RestaurantService>.value(value: restaurantService),
        Provider(create: (_) => reservationService),
        Provider(create: (_) => MockLoyaltyService()),
        Provider(create: (_) => MockEventService()),
        Provider(create: (_) => MockHangOutService()),
        Provider(create: (_) => MockProfileService()),
        Provider<ReviewService>.value(value: reviewService),
        ChangeNotifierProvider(
          create: (context) => AuthProvider(
            context.read<AuthService>(),
            context.read<MockProfileService>(),
          ),
        ),
        ChangeNotifierProvider(
          create: (context) => LoyaltyProvider(
            context.read<MockLoyaltyService>(),
          ),
        ),
        ChangeNotifierProvider(
          create: (context) => HangOutProvider(
            context.read<MockHangOutService>(),
          ),
        ),
        ChangeNotifierProvider(
          create: (context) => ProfileProvider(
            context.read<MockProfileService>(),
          ),
        ),
        ChangeNotifierProvider(
          create: (context) => ReviewProvider(
            context.read<ReviewService>(),
          ),
        ),
      ],
      child: MaterialApp(
        title: 'HangOut',
        debugShowCheckedModeBanner: false,
        theme: AppTheme.lightTheme,
        initialRoute: AppRouter.splash,
        onGenerateRoute: AppRouter.onGenerateRoute,
      ),
    );
  }
}
