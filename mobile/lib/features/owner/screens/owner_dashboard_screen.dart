import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../../core/theme/app_colors.dart';
import '../../../models/owner_stats.dart';
import '../../../widgets/stat_card.dart';
import '../providers/owner_provider.dart';

class OwnerDashboardScreen extends StatefulWidget {
  const OwnerDashboardScreen({super.key});

  @override
  State<OwnerDashboardScreen> createState() => _OwnerDashboardScreenState();
}

class _OwnerDashboardScreenState extends State<OwnerDashboardScreen> {
  @override
  void initState() {
    super.initState();
    context.read<OwnerProvider>().loadStats();
  }

  Future<void> _refresh() async {
    await Future.wait([
      context.read<OwnerProvider>().loadStats(),
      context.read<OwnerProvider>().loadRestaurant(),
    ]);
  }

  @override
  Widget build(BuildContext context) {
    final ownerProvider = context.watch<OwnerProvider>();
    final restaurant = ownerProvider.restaurant;

    return Scaffold(
      backgroundColor: AppColors.background,
      appBar: AppBar(
        title: const Text('Dashboard'),
      ),
      body: ownerProvider.isLoadingStats && ownerProvider.stats == null
          ? const Center(child: CircularProgressIndicator())
          : ownerProvider.statsError != null && ownerProvider.stats == null
              ? _buildError(ownerProvider.statsError!)
              : RefreshIndicator(
                  onRefresh: _refresh,
                  child: ListView(
                    padding: const EdgeInsets.all(20),
                    children: [
                      DashboardHero(
                        child: _buildHeroContent(
                          restaurant?.name ?? 'Your Restaurant',
                          restaurant?.cuisine ?? '',
                          restaurant?.image ?? '',
                          restaurant?.isOpen ?? true,
                          ownerProvider.stats,
                        ),
                      ),
                      const SizedBox(height: 28),
                      const DashboardSectionHeader(title: 'Bookings', icon: Icons.event_note_outlined),
                      const SizedBox(height: 14),
                      _buildBookingsGrid(ownerProvider.stats ?? OwnerStats()),
                      const SizedBox(height: 24),
                      const DashboardSectionHeader(title: 'Restaurant', icon: Icons.storefront_outlined),
                      const SizedBox(height: 14),
                      _buildRestaurantGrid(ownerProvider.stats ?? OwnerStats()),
                    ],
                  ),
                ),
    );
  }

  Widget _buildError(String message) {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(24),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            const Icon(Icons.error_outline, size: 56, color: AppColors.error),
            const SizedBox(height: 12),
            Text(
              message,
              textAlign: TextAlign.center,
              style: const TextStyle(color: AppColors.textSecondary),
            ),
            const SizedBox(height: 16),
            ElevatedButton(
              onPressed: _refresh,
              child: const Text('Retry'),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildHeroContent(
    String name,
    String cuisine,
    String image,
    bool isOpen,
    OwnerStats? stats,
  ) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          children: [
            ClipRRect(
              borderRadius: BorderRadius.circular(14),
              child: Image.network(
                image,
                width: 60,
                height: 60,
                fit: BoxFit.cover,
                errorBuilder: (context, error, stackTrace) {
                  return Container(
                    width: 60,
                    height: 60,
                    color: Colors.white.withOpacity(0.15),
                    child: const Icon(Icons.restaurant, color: Colors.white70),
                  );
                },
              ),
            ),
            const SizedBox(width: 14),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    name,
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                    style: const TextStyle(
                      fontSize: 20,
                      fontWeight: FontWeight.bold,
                      color: Colors.white,
                    ),
                  ),
                  const SizedBox(height: 4),
                  Row(
                    children: [
                      if (cuisine.isNotEmpty)
                        Text(
                          cuisine,
                          style: TextStyle(fontSize: 13, color: Colors.white.withOpacity(0.75)),
                        ),
                      if (cuisine.isNotEmpty) const SizedBox(width: 8),
                      Container(
                        padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
                        decoration: BoxDecoration(
                          color: (isOpen ? AppColors.openStatus : AppColors.closedStatus).withOpacity(0.9),
                          borderRadius: BorderRadius.circular(12),
                        ),
                        child: Text(
                          isOpen ? 'Open' : 'Closed',
                          style: const TextStyle(fontSize: 11, fontWeight: FontWeight.w700, color: Colors.white),
                        ),
                      ),
                    ],
                  ),
                ],
              ),
            ),
          ],
        ),
        const SizedBox(height: 20),
        Container(height: 1, color: Colors.white.withOpacity(0.12)),
        const SizedBox(height: 16),
        Row(
          children: [
            _buildHeroMetric(
              Icons.hourglass_empty,
              (stats?.pendingReservations ?? 0).toString(),
              'Awaiting you',
            ),
            Container(width: 1, height: 36, color: Colors.white.withOpacity(0.12)),
            _buildHeroMetric(
              Icons.star_rounded,
              stats?.averageRating != null ? stats!.averageRating!.toStringAsFixed(1) : '—',
              'Avg rating',
            ),
            Container(width: 1, height: 36, color: Colors.white.withOpacity(0.12)),
            _buildHeroMetric(
              Icons.celebration_outlined,
              (stats?.totalEvents ?? 0).toString(),
              'Events',
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

  Widget _buildBookingsGrid(OwnerStats stats) {
    return GridView.count(
      crossAxisCount: 2,
      shrinkWrap: true,
      physics: const NeverScrollableScrollPhysics(),
      mainAxisSpacing: 14,
      crossAxisSpacing: 14,
      childAspectRatio: 1.35,
      children: [
        StatCard(
          label: 'Pending',
          value: stats.pendingReservations.toString(),
          icon: Icons.hourglass_empty,
          color: AppColors.warning,
          highlight: stats.pendingReservations > 0,
        ),
        StatCard(
          label: 'Confirmed',
          value: stats.confirmedReservations.toString(),
          icon: Icons.check_circle_outline,
          color: AppColors.success,
        ),
        StatCard(
          label: 'Completed',
          value: stats.completedReservations.toString(),
          icon: Icons.task_alt,
          color: AppColors.info,
        ),
        StatCard(
          label: 'Total Bookings',
          value: stats.totalReservations.toString(),
          icon: Icons.event_note,
          color: AppColors.accent,
        ),
      ],
    );
  }

  Widget _buildRestaurantGrid(OwnerStats stats) {
    return GridView.count(
      crossAxisCount: 2,
      shrinkWrap: true,
      physics: const NeverScrollableScrollPhysics(),
      mainAxisSpacing: 14,
      crossAxisSpacing: 14,
      childAspectRatio: 1.35,
      children: [
        StatCard(
          label: 'Menu Items',
          value: stats.totalMenuItems.toString(),
          icon: Icons.restaurant_menu,
          color: AppColors.primary,
        ),
        StatCard(
          label: 'Events',
          value: stats.totalEvents.toString(),
          icon: Icons.celebration,
          color: AppColors.accent,
        ),
        StatCard(
          label: 'Reviews',
          value: stats.totalReviews.toString(),
          icon: Icons.rate_review,
          color: AppColors.info,
        ),
        StatCard(
          label: 'Avg Rating',
          value: stats.averageRating != null ? stats.averageRating!.toStringAsFixed(1) : '—',
          icon: Icons.star,
          color: AppColors.ratingActive,
        ),
      ],
    );
  }
}
