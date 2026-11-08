import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'core/theme/app_theme.dart';
import 'core/navigation/app_router.dart';
import 'core/api/api_client.dart';
import 'services/api/api_auth_service.dart';
import 'services/api/api_restaurant_service.dart';
import 'services/api/api_review_service.dart';
import 'services/api/api_reservation_service.dart';
import 'services/api/api_loyalty_service.dart';
import 'services/api/api_recommendation_service.dart';
import 'services/api/api_event_service.dart';
import 'services/api/api_hangout_service.dart';
import 'services/api/api_profile_service.dart';
import 'services/api/api_menu_item_service.dart';
import 'services/interfaces/auth_service.dart';
import 'services/interfaces/restaurant_service.dart';
import 'services/interfaces/review_service.dart';
import 'services/interfaces/reservation_service.dart';
import 'services/interfaces/loyalty_service.dart';
import 'services/interfaces/recommendation_service.dart';
import 'services/interfaces/event_service.dart';
import 'services/interfaces/hangout_service.dart';
import 'services/interfaces/profile_service.dart';
import 'services/interfaces/menu_item_service.dart';
import 'features/auth/providers/auth_provider.dart';
import 'features/loyalty/providers/loyalty_provider.dart';
import 'features/recommendations/providers/recommendation_provider.dart';
import 'features/owner/providers/cashier_provider.dart';
import 'features/hangout/providers/hangout_provider.dart';
import 'features/profile/providers/profile_provider.dart';
import 'features/reviews/providers/review_provider.dart';
import 'features/owner/providers/owner_provider.dart';
import 'features/admin/providers/admin_provider.dart';
import 'features/admin/providers/owner_requests_provider.dart';
import 'features/restaurants/providers/restaurant_provider.dart';
import 'features/home/providers/home_provider.dart';
import 'features/events/providers/event_provider.dart';
import 'features/reservations/providers/reservation_provider.dart';
import 'services/interfaces/owner_service.dart';
import 'services/interfaces/admin_service.dart';
import 'services/api/api_owner_service.dart';
import 'services/api/api_admin_service.dart';

void main() async {
  WidgetsFlutterBinding.ensureInitialized();
  await ApiClient.init();
  runApp(const HangOutApp());
}

/// Every feature in this app is backed by the real backend/database - there is
/// no static/mock data path. All services below are the HTTP implementations.
class HangOutApp extends StatelessWidget {
  const HangOutApp({super.key});

  @override
  Widget build(BuildContext context) {
    final AuthService authService = ApiAuthService();
    final RestaurantService restaurantService = ApiRestaurantService();
    final ReviewService reviewService = ApiReviewService();
    final ReservationService reservationService = ApiReservationService();
    final LoyaltyService loyaltyService = ApiLoyaltyService();
    final RecommendationService recommendationService = ApiRecommendationService();
    final EventService eventService = ApiEventService();
    final HangOutService hangOutService = ApiHangOutService();
    final ProfileService profileService = ApiProfileService();
    final MenuItemService menuItemService = ApiMenuItemService();
    final OwnerService ownerService = ApiOwnerService();
    final AdminService adminService = ApiAdminService();

    return MultiProvider(
      providers: [
        Provider<AuthService>.value(value: authService),
        Provider<RestaurantService>.value(value: restaurantService),
        Provider<ReservationService>.value(value: reservationService),
        Provider<LoyaltyService>.value(value: loyaltyService),
        Provider<RecommendationService>.value(value: recommendationService),
        Provider<EventService>.value(value: eventService),
        Provider<HangOutService>.value(value: hangOutService),
        Provider<ProfileService>.value(value: profileService),
        Provider<MenuItemService>.value(value: menuItemService),
        Provider<ReviewService>.value(value: reviewService),
        Provider<OwnerService>.value(value: ownerService),
        Provider<AdminService>.value(value: adminService),
        ChangeNotifierProvider(
          create: (context) => AuthProvider(context.read<AuthService>()),
        ),
        ChangeNotifierProvider(
          create: (context) => LoyaltyProvider(context.read<LoyaltyService>()),
        ),
        ChangeNotifierProvider(
          create: (context) => HangOutProvider(context.read<HangOutService>()),
        ),
        ChangeNotifierProvider(
          create: (context) => ProfileProvider(context.read<ProfileService>()),
        ),
        ChangeNotifierProvider(
          create: (context) => ReviewProvider(context.read<ReviewService>()),
        ),
        ChangeNotifierProvider(
          create: (context) => RestaurantProvider(
            context.read<RestaurantService>(),
            context.read<MenuItemService>(),
          ),
        ),
        ChangeNotifierProvider(
          create: (context) => HomeProvider(context.read<RestaurantProvider>()),
        ),
        ChangeNotifierProvider(
          create: (context) => EventProvider(context.read<EventService>()),
        ),
        ChangeNotifierProvider(
          create: (context) => ReservationProvider(context.read<ReservationService>()),
        ),
        ChangeNotifierProvider(
          create: (context) => OwnerProvider(context.read<OwnerService>()),
        ),
        ChangeNotifierProvider(
          create: (context) => AdminProvider(context.read<AdminService>()),
        ),
        ChangeNotifierProvider(
          create: (context) => OwnerRequestsProvider(context.read<AdminService>()),
        ),
        ChangeNotifierProvider(
          create: (context) => CashierProvider(context.read<OwnerService>()),
        ),
        ChangeNotifierProvider(
          create: (context) => RecommendationProvider(context.read<RecommendationService>()),
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
