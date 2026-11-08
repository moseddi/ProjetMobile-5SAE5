import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../providers/owner_provider.dart';
import 'owner_dashboard_screen.dart';
import 'owner_reservations_screen.dart';
import 'owner_cashier_screen.dart';
import 'owner_menu_screen.dart';
import 'owner_events_screen.dart';
import 'owner_reviews_screen.dart';
import 'owner_profile_screen.dart';

/// Shell for the Restaurant Owner console: one tab per area of the owner's
/// restaurant (dashboard, reservations, menu, events, reviews) plus a
/// profile/settings tab, mirroring the structure of the client-facing
/// MainScreen (lib/features/home/screens/main_screen.dart).
class OwnerMainScreen extends StatefulWidget {
  const OwnerMainScreen({super.key});

  @override
  State<OwnerMainScreen> createState() => _OwnerMainScreenState();
}

class _OwnerMainScreenState extends State<OwnerMainScreen> {
  int _selectedIndex = 0;

  final List<Widget> _screens = const [
    OwnerDashboardScreen(),
    OwnerReservationsScreen(),
    OwnerCashierScreen(),
    OwnerMenuScreen(),
    OwnerEventsScreen(),
    OwnerReviewsScreen(),
    OwnerProfileScreen(),
  ];

  @override
  void initState() {
    super.initState();
    // Loaded once here so the restaurant's name/branding is available to
    // every tab (e.g. the dashboard's AppBar) without each one re-fetching it.
    context.read<OwnerProvider>().loadRestaurant();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: _screens[_selectedIndex],
      bottomNavigationBar: BottomNavigationBar(
        currentIndex: _selectedIndex,
        onTap: (index) {
          setState(() {
            _selectedIndex = index;
          });
        },
        type: BottomNavigationBarType.fixed,
        items: const [
          BottomNavigationBarItem(
            icon: Icon(Icons.dashboard_outlined),
            activeIcon: Icon(Icons.dashboard),
            label: 'Dashboard',
          ),
          BottomNavigationBarItem(
            icon: Icon(Icons.event_note_outlined),
            activeIcon: Icon(Icons.event_note),
            label: 'Bookings',
          ),
          BottomNavigationBarItem(
            icon: Icon(Icons.qr_code_scanner),
            activeIcon: Icon(Icons.qr_code_scanner),
            label: 'Cashier',
          ),
          BottomNavigationBarItem(
            icon: Icon(Icons.restaurant_menu_outlined),
            activeIcon: Icon(Icons.restaurant_menu),
            label: 'Menu',
          ),
          BottomNavigationBarItem(
            icon: Icon(Icons.celebration_outlined),
            activeIcon: Icon(Icons.celebration),
            label: 'Events',
          ),
          BottomNavigationBarItem(
            icon: Icon(Icons.rate_review_outlined),
            activeIcon: Icon(Icons.rate_review),
            label: 'Reviews',
          ),
          BottomNavigationBarItem(
            icon: Icon(Icons.person_outline),
            activeIcon: Icon(Icons.person),
            label: 'Profile',
          ),
        ],
      ),
    );
  }
}
