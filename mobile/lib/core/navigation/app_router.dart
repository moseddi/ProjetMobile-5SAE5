import 'package:flutter/material.dart';
import '../../features/auth/screens/splash_screen.dart';
import '../../features/auth/screens/login_screen.dart';
import '../../features/auth/screens/owner_signup_screen.dart';
import '../../features/auth/screens/register_screen.dart';
import '../../features/home/screens/main_screen.dart';
import '../../features/restaurants/screens/restaurant_details_screen.dart';
import '../../features/reservations/screens/reservation_screen.dart';
import '../../features/reservations/screens/reservation_summary_screen.dart';
import '../../features/events/screens/event_details_screen.dart';
import '../../features/hangout/screens/create_hangout_screen.dart';
import '../../features/hangout/screens/hangout_details_screen.dart';
import '../../features/reviews/screens/add_review_screen.dart';
import '../../features/owner/screens/owner_main_screen.dart';
import '../../features/admin/screens/admin_main_screen.dart';
import '../../models/restaurant.dart';
import '../../models/event.dart';
import '../../models/hangout.dart';
import '../../models/reservation.dart';
import '../../models/user.dart';

class AppRouter {
  static const String splash = '/';
  static const String login = '/login';
  static const String register = '/register';
  static const String ownerSignup = '/owner-signup';
  static const String main = '/main';
  static const String ownerMain = '/owner-main';
  static const String adminMain = '/admin-main';

  /// The top-level shell route for a given role, used by splash/login/register
  /// to land each account type on its own home.
  static String homeRouteForRole(UserRole role) {
    switch (role) {
      case UserRole.admin:
        return adminMain;
      case UserRole.restaurantOwner:
        return ownerMain;
      case UserRole.client:
        return main;
    }
  }
  static const String restaurantDetails = '/restaurant-details';
  static const String reservation = '/reservation';
  static const String reservationSummary = '/reservation-summary';
  static const String eventDetails = '/event-details';
  static const String createHangout = '/create-hangout';
  static const String hangoutDetails = '/hangout-details';
  static const String addReview = '/add-review';

  static Route<dynamic> onGenerateRoute(RouteSettings settings) {
    switch (settings.name) {
      case splash:
        return MaterialPageRoute(builder: (_) => const SplashScreen());
      case login:
        return MaterialPageRoute(builder: (_) => const LoginScreen());
      case ownerSignup:
        return MaterialPageRoute(builder: (_) => const OwnerSignupScreen());
      case register:
        return MaterialPageRoute(builder: (_) => const RegisterScreen());
      case main:
        return MaterialPageRoute(builder: (_) => const MainScreen());
      case ownerMain:
        return MaterialPageRoute(builder: (_) => const OwnerMainScreen());
      case adminMain:
        return MaterialPageRoute(builder: (_) => const AdminMainScreen());
      case restaurantDetails:
        final restaurant = settings.arguments as Restaurant;
        return MaterialPageRoute(
          builder: (_) => RestaurantDetailsScreen(restaurant: restaurant),
        );
      case reservation:
        final restaurant = settings.arguments as Restaurant;
        return MaterialPageRoute(
          builder: (_) => ReservationScreen(restaurant: restaurant),
        );
      case reservationSummary:
        final reservation = settings.arguments as Reservation;
        return MaterialPageRoute(
          builder: (_) => ReservationSummaryScreen(reservation: reservation),
        );
      case eventDetails:
        final event = settings.arguments as Event;
        return MaterialPageRoute(
          builder: (_) => EventDetailsScreen(event: event),
        );
      case createHangout:
        return MaterialPageRoute(builder: (_) => const CreateHangOutScreen());
      case hangoutDetails:
        final hangout = settings.arguments as HangOut;
        return MaterialPageRoute(
          builder: (_) => HangOutDetailsScreen(hangout: hangout),
        );
      case addReview:
        final restaurantId = settings.arguments as String?;
        return MaterialPageRoute(
          builder: (_) => AddReviewScreen(preselectedRestaurantId: restaurantId),
        );
      default:
        return MaterialPageRoute(
          builder: (_) => Scaffold(
            body: Center(
              child: Text('No route defined for ${settings.name}'),
            ),
          ),
        );
    }
  }
}
