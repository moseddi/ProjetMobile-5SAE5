import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../../core/theme/app_colors.dart';
import '../providers/owner_requests_provider.dart';
import 'admin_dashboard_screen.dart';
import 'admin_owner_requests_screen.dart';
import 'admin_profile_screen.dart';
import 'admin_restaurants_screen.dart';
import 'admin_reviews_screen.dart';
import 'admin_users_screen.dart';

/// Shell for the ADMIN role, routed to from AppRouter.homeRouteForRole.
class AdminMainScreen extends StatefulWidget {
  const AdminMainScreen({super.key});

  @override
  State<AdminMainScreen> createState() => _AdminMainScreenState();
}

class _AdminMainScreenState extends State<AdminMainScreen> {
  int _selectedIndex = 0;

  final List<Widget> _screens = const [
    AdminDashboardScreen(),
    AdminOwnerRequestsScreen(),
    AdminUsersScreen(),
    AdminRestaurantsScreen(),
    AdminReviewsScreen(),
    AdminProfileScreen(),
  ];

  @override
  void initState() {
    super.initState();
    // Needed up front so the Requests tab can show how many owners are waiting.
    WidgetsBinding.instance.addPostFrameCallback((_) => context.read<OwnerRequestsProvider>().load());
  }

  @override
  Widget build(BuildContext context) {
    final pending = context.select<OwnerRequestsProvider, int>((p) => p.pendingCount);

    return Scaffold(
      // Rebuilt on each switch so every tab reloads fresh data (e.g. a restaurant
      // just created by accepting a request).
      body: _screens[_selectedIndex],
      bottomNavigationBar: NavigationBar(
        selectedIndex: _selectedIndex,
        onDestinationSelected: (index) => setState(() => _selectedIndex = index),
        backgroundColor: AppColors.surface,
        indicatorColor: AppColors.accent.withOpacity(0.2),
        labelBehavior: NavigationDestinationLabelBehavior.alwaysShow,
        height: 68,
        destinations: [
          const NavigationDestination(
            icon: Icon(Icons.dashboard_outlined),
            selectedIcon: Icon(Icons.dashboard),
            label: 'Dashboard',
          ),
          NavigationDestination(
            icon: Badge(
              isLabelVisible: pending > 0,
              label: Text('$pending'),
              child: const Icon(Icons.how_to_reg_outlined),
            ),
            selectedIcon: Badge(
              isLabelVisible: pending > 0,
              label: Text('$pending'),
              child: const Icon(Icons.how_to_reg),
            ),
            label: 'Requests',
          ),
          const NavigationDestination(
            icon: Icon(Icons.people_outline),
            selectedIcon: Icon(Icons.people),
            label: 'Users',
          ),
          const NavigationDestination(
            icon: Icon(Icons.restaurant_outlined),
            selectedIcon: Icon(Icons.restaurant),
            label: 'Restaurants',
          ),
          const NavigationDestination(
            icon: Icon(Icons.rate_review_outlined),
            selectedIcon: Icon(Icons.rate_review),
            label: 'Reviews',
          ),
          const NavigationDestination(
            icon: Icon(Icons.person_outline),
            selectedIcon: Icon(Icons.person),
            label: 'Profile',
          ),
        ],
      ),
    );
  }
}
