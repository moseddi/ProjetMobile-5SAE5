import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../../core/theme/app_colors.dart';
import '../../../widgets/stat_card.dart';
import '../providers/admin_provider.dart';
import 'admin_owner_requests_screen.dart';
import 'admin_reservations_screen.dart';

class AdminDashboardScreen extends StatefulWidget {
  const AdminDashboardScreen({super.key});

  @override
  State<AdminDashboardScreen> createState() => _AdminDashboardScreenState();
}

class _AdminDashboardScreenState extends State<AdminDashboardScreen> {
  @override
  void initState() {
    super.initState();
    context.read<AdminProvider>().loadStats();
  }

  @override
  Widget build(BuildContext context) {
    final admin = context.watch<AdminProvider>();

    return Scaffold(
      backgroundColor: AppColors.background,
      appBar: AppBar(
        title: const Text('Dashboard'),
      ),
      body: RefreshIndicator(
        onRefresh: () => context.read<AdminProvider>().loadStats(),
        child: admin.isLoadingStats && admin.stats == null
            ? const Center(child: CircularProgressIndicator())
            : admin.statsError != null && admin.stats == null
                ? _buildError(admin.statsError!)
                : _buildContent(admin),
      ),
    );
  }

  Widget _buildError(String message) {
    return ListView(
      padding: const EdgeInsets.all(20),
      children: [
        const SizedBox(height: 80),
        const Icon(Icons.error_outline, size: 64, color: AppColors.error),
        const SizedBox(height: 16),
        Center(
          child: Text(
            message.replaceAll('Exception: ', ''),
            textAlign: TextAlign.center,
            style: const TextStyle(color: AppColors.textSecondary),
          ),
        ),
      ],
    );
  }

  Widget _buildContent(AdminProvider admin) {
    final stats = admin.stats;
    final unassigned = stats?.unassignedRestaurants ?? 0;

    return ListView(
      padding: const EdgeInsets.all(20),
      children: [
        DashboardHero(child: _buildHeroContent(stats)),
        if ((stats?.pendingOwnerApplications ?? 0) > 0) ...[
          const SizedBox(height: 16),
          _buildPendingBanner(stats!.pendingOwnerApplications),
        ],
        const SizedBox(height: 28),
        const DashboardSectionHeader(title: 'Users', icon: Icons.people_outline),
        const SizedBox(height: 14),
        GridView.count(
          crossAxisCount: 2,
          shrinkWrap: true,
          physics: const NeverScrollableScrollPhysics(),
          mainAxisSpacing: 14,
          crossAxisSpacing: 14,
          childAspectRatio: 1.35,
          children: [
            StatCard(
              label: 'Clients',
              value: (stats?.totalClients ?? 0).toString(),
              icon: Icons.person_outline,
              color: AppColors.info,
            ),
            StatCard(
              label: 'Restaurant Owners',
              value: (stats?.totalRestaurantOwners ?? 0).toString(),
              icon: Icons.storefront,
              color: AppColors.success,
            ),
            StatCard(
              label: 'Admins',
              value: (stats?.totalAdmins ?? 0).toString(),
              icon: Icons.admin_panel_settings,
              color: AppColors.primary,
            ),
            StatCard(
              label: 'Total Users',
              value: (stats?.totalUsers ?? 0).toString(),
              icon: Icons.people,
              color: AppColors.accent,
            ),
          ],
        ),
        const SizedBox(height: 24),
        const DashboardSectionHeader(title: 'Restaurants', icon: Icons.restaurant_outlined),
        const SizedBox(height: 14),
        GridView.count(
          crossAxisCount: 2,
          shrinkWrap: true,
          physics: const NeverScrollableScrollPhysics(),
          mainAxisSpacing: 14,
          crossAxisSpacing: 14,
          childAspectRatio: 1.35,
          children: [
            StatCard(
              label: 'Total Restaurants',
              value: (stats?.totalRestaurants ?? 0).toString(),
              icon: Icons.restaurant,
              color: AppColors.accent,
            ),
            StatCard(
              label: 'Need an Owner',
              value: unassigned.toString(),
              icon: Icons.flag,
              color: unassigned > 0 ? AppColors.error : AppColors.success,
              highlight: unassigned > 0,
            ),
          ],
        ),
        const SizedBox(height: 24),
        const DashboardSectionHeader(title: 'Activity', icon: Icons.insights_outlined),
        const SizedBox(height: 14),
        GridView.count(
          crossAxisCount: 2,
          shrinkWrap: true,
          physics: const NeverScrollableScrollPhysics(),
          mainAxisSpacing: 14,
          crossAxisSpacing: 14,
          childAspectRatio: 1.35,
          children: [
            StatCard(
              label: 'Reservations',
              value: (stats?.totalReservations ?? 0).toString(),
              icon: Icons.calendar_today,
              color: AppColors.info,
              onTap: () => Navigator.of(context).push(
                MaterialPageRoute(builder: (_) => const AdminReservationsScreen()),
              ),
            ),
            StatCard(
              label: 'Reviews',
              value: (stats?.totalReviews ?? 0).toString(),
              icon: Icons.rate_review,
              color: AppColors.ratingActive,
            ),
            StatCard(
              label: 'Hang Outs',
              value: (stats?.totalHangOuts ?? 0).toString(),
              icon: Icons.group,
              color: AppColors.success,
            ),
            StatCard(
              label: 'Events',
              value: (stats?.totalEvents ?? 0).toString(),
              icon: Icons.event,
              color: AppColors.accent,
            ),
          ],
        ),
        const SizedBox(height: 24),
        SizedBox(
          width: double.infinity,
          child: OutlinedButton.icon(
            onPressed: () => Navigator.of(context).push(
              MaterialPageRoute(builder: (_) => const AdminReservationsScreen()),
            ),
            icon: const Icon(Icons.calendar_today_outlined),
            label: const Text('View All Reservations'),
          ),
        ),
        const SizedBox(height: 24),
      ],
    );
  }

  Widget _buildPendingBanner(int count) {
    return Material(
      color: AppColors.warning.withOpacity(0.12),
      borderRadius: BorderRadius.circular(18),
      child: InkWell(
        borderRadius: BorderRadius.circular(18),
        onTap: () => Navigator.of(context).push(
          MaterialPageRoute(builder: (_) => const AdminOwnerRequestsScreen()),
        ),
        child: Container(
          padding: const EdgeInsets.all(16),
          decoration: BoxDecoration(
            borderRadius: BorderRadius.circular(18),
            border: Border.all(color: AppColors.warning.withOpacity(0.5)),
          ),
          child: Row(
            children: [
              Container(
                padding: const EdgeInsets.all(10),
                decoration: const BoxDecoration(color: AppColors.warning, shape: BoxShape.circle),
                child: const Icon(Icons.how_to_reg, color: Colors.white, size: 22),
              ),
              const SizedBox(width: 14),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      '$count restaurant ${count == 1 ? 'request' : 'requests'} waiting',
                      style: const TextStyle(fontSize: 15, fontWeight: FontWeight.w800, color: AppColors.textPrimary),
                    ),
                    const SizedBox(height: 2),
                    const Text('Review and accept new owners',
                        style: TextStyle(fontSize: 13, color: AppColors.textSecondary)),
                  ],
                ),
              ),
              const Icon(Icons.chevron_right, color: AppColors.textSecondary),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildHeroContent(dynamic stats) {
    final totalUsers = stats?.totalUsers ?? 0;
    final unassigned = stats?.unassignedRestaurants ?? 0;

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          children: [
            Container(
              padding: const EdgeInsets.all(12),
              decoration: BoxDecoration(
                color: Colors.white.withOpacity(0.12),
                borderRadius: BorderRadius.circular(14),
              ),
              child: const Icon(Icons.admin_panel_settings, color: Colors.white, size: 26),
            ),
            const SizedBox(width: 14),
            const Expanded(
              child: Text(
                'Platform Overview',
                style: TextStyle(fontSize: 20, fontWeight: FontWeight.bold, color: Colors.white),
              ),
            ),
          ],
        ),
        const SizedBox(height: 20),
        Container(height: 1, color: Colors.white.withOpacity(0.12)),
        const SizedBox(height: 16),
        Row(
          children: [
            _buildHeroMetric(Icons.people, totalUsers.toString(), 'Total users'),
            Container(width: 1, height: 36, color: Colors.white.withOpacity(0.12)),
            _buildHeroMetric(
              Icons.restaurant,
              (stats?.totalRestaurants ?? 0).toString(),
              'Restaurants',
            ),
            Container(width: 1, height: 36, color: Colors.white.withOpacity(0.12)),
            _buildHeroMetric(
              unassigned > 0 ? Icons.flag : Icons.check_circle,
              unassigned.toString(),
              'Need an owner',
            ),
          ],
        ),
      ],
    );
  }

  Widget _buildHeroMetric(IconData icon, String value, String label) {
    return Expanded(
      child: Column(
        children: [
          Icon(icon, color: AppColors.accentLight, size: 20),
          const SizedBox(height: 6),
          Text(
            value,
            style: const TextStyle(fontSize: 18, fontWeight: FontWeight.bold, color: Colors.white),
          ),
          const SizedBox(height: 2),
          Text(
            label,
            style: TextStyle(fontSize: 11, color: Colors.white.withOpacity(0.7)),
            textAlign: TextAlign.center,
          ),
        ],
      ),
    );
  }
}
