import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import 'package:provider/provider.dart';
import '../../../core/theme/app_colors.dart';
import '../../../models/reservation.dart';
import '../../../widgets/reservation_status.dart';
import '../../../widgets/reservation_filters.dart';
import '../../../widgets/admin_toolbar.dart';
import '../../../models/restaurant.dart';
import '../providers/admin_provider.dart';
import '../../restaurants/providers/restaurant_provider.dart';

/// Simple, read-only oversight list of every reservation on the platform.
/// The raw `/api/admin/reservations` payload only carries ids (no
/// restaurantName), so restaurant names are cross-referenced against the
/// restaurant list loaded here.
class AdminReservationsScreen extends StatefulWidget {
  const AdminReservationsScreen({super.key});

  @override
  State<AdminReservationsScreen> createState() => _AdminReservationsScreenState();
}

class _AdminReservationsScreenState extends State<AdminReservationsScreen> {
  String _query = '';
  AdminSort _sort = AdminSort.newest;
  ReservationKind _kind = ReservationKind.all;
  ReservationStatusGroup _group = ReservationStatusGroup.all;

  List<Reservation> _filtered(List<Reservation> all) {
    final q = _query.trim().toLowerCase();
    final list = all.where((r) {
      if (!_kind.matches(r) || !_group.contains(r)) return false;
      if (q.isEmpty) return true;
      final name = (_restaurantsById[r.restaurantId]?.name ?? r.restaurantName).toLowerCase();
      return name.contains(q) ||
          r.contactName.toLowerCase().contains(q) ||
          r.contactPhone.contains(q) ||
          (r.eventTitle ?? '').toLowerCase().contains(q);
    });
    return sortByAdded(list, _sort, (r) => r.id);
  }

  Widget _buildToolbar(List<Reservation> all) {
    final scoped = all.where(_kind.matches).toList();
    return Padding(
      padding: const EdgeInsets.fromLTRB(16, 12, 16, 4),
      child: Column(
        children: [
          AdminSearchBar(
            hint: 'Search restaurant, guest or event',
            sort: _sort,
            onQueryChanged: (v) => setState(() => _query = v),
            onSortChanged: (v) => setState(() => _sort = v),
          ),
          const SizedBox(height: 10),
          ReservationKindTabs(
            all: all,
            selected: _kind,
            onChanged: (k) => setState(() {
              _kind = k;
              _group = ReservationStatusGroup.all;
            }),
          ),
          const SizedBox(height: 10),
          ReservationStatusPills(
            scoped: scoped,
            selected: _group,
            onChanged: (g) => setState(() => _group = g),
          ),
        ],
      ),
    );
  }
  Map<String, Restaurant> get _restaurantsById =>
      {for (final r in context.read<RestaurantProvider>().restaurants) r.id: r};
  bool get _isLoadingRestaurants => context.read<RestaurantProvider>().isLoading;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (mounted) context.read<AdminProvider>().loadReservations();
    });
    _loadRestaurants();
  }

  Future<void> _loadRestaurants() => context.read<RestaurantProvider>().loadRestaurants();

  Color _statusColor(ReservationStatus status) => reservationStatusColor(status);

  @override
  Widget build(BuildContext context) {
    // Rebuild when the restaurant names finish loading.
    context.watch<RestaurantProvider>();
    final admin = context.watch<AdminProvider>();
    final items = _filtered(admin.reservations);
    final isLoading = (admin.isLoadingReservations && admin.reservations.isEmpty) || _isLoadingRestaurants;

    return Scaffold(
      backgroundColor: AppColors.background,
      appBar: AppBar(
        title: const Text('All Reservations'),
      ),
      body: Column(
        children: [
          _buildToolbar(admin.reservations),
          Expanded(
            child: RefreshIndicator(
        onRefresh: () async {
          await context.read<AdminProvider>().loadReservations();
          await _loadRestaurants();
        },
        child: isLoading
            ? const Center(child: CircularProgressIndicator())
            : admin.reservationsError != null && admin.reservations.isEmpty
                ? _buildError(admin.reservationsError!)
                : admin.reservations.isEmpty
                    ? _buildEmptyState()
                    : items.isEmpty
                        ? ListView(children: [
                            AdminNoResults(onReset: () => setState(() {
                                  _query = '';
                                  _kind = ReservationKind.all;
                                  _group = ReservationStatusGroup.all;
                                })),
                          ])
                        : ListView.builder(
                            padding: const EdgeInsets.all(16),
                            itemCount: items.length,
                            itemBuilder: (context, index) => _buildCard(items[index]),
                          ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildError(String message) {
    return ListView(
      padding: const EdgeInsets.all(20),
      children: [
        const SizedBox(height: 60),
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

  Widget _buildEmptyState() {
    return ListView(
      padding: const EdgeInsets.all(20),
      children: [
        const SizedBox(height: 60),
        Icon(Icons.calendar_today_outlined, size: 80, color: AppColors.textHint),
        const SizedBox(height: 16),
        const Center(
          child: Text(
            'No reservations yet',
            style: TextStyle(fontSize: 18, fontWeight: FontWeight.w600, color: AppColors.textPrimary),
          ),
        ),
      ],
    );
  }

  Widget _buildCard(Reservation reservation) {
    final restaurant = _restaurantsById[reservation.restaurantId];
    final restaurantName = restaurant?.name ?? 'Restaurant #${reservation.restaurantId}';

    return Card(
      margin: const EdgeInsets.only(bottom: 12),
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Expanded(
                  child: Text(
                    restaurantName,
                    style: const TextStyle(fontSize: 15, fontWeight: FontWeight.bold, color: AppColors.textPrimary),
                  ),
                ),
                Container(
                  padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                  decoration: BoxDecoration(
                    color: _statusColor(reservation.status).withOpacity(0.1),
                    borderRadius: BorderRadius.circular(12),
                  ),
                  child: Text(
                    reservation.status.displayName,
                    style: TextStyle(fontSize: 12, fontWeight: FontWeight.w600, color: _statusColor(reservation.status)),
                  ),
                ),
              ],
            ),
            const SizedBox(height: 10),
            Row(
              children: [
                const Icon(Icons.calendar_today, size: 14, color: AppColors.textSecondary),
                const SizedBox(width: 6),
                Text(
                  DateFormat('MMM dd, yyyy - h:mm a').format(reservation.datetime),
                  style: const TextStyle(fontSize: 13, color: AppColors.textSecondary),
                ),
                const SizedBox(width: 16),
                const Icon(Icons.people, size: 14, color: AppColors.textSecondary),
                const SizedBox(width: 6),
                Text(
                  '${reservation.guests}',
                  style: const TextStyle(fontSize: 13, color: AppColors.textSecondary),
                ),
              ],
            ),
            if (reservation.notes.isNotEmpty) ...[
              const SizedBox(height: 6),
              Text(
                reservation.notes,
                style: const TextStyle(fontSize: 13, color: AppColors.textSecondary, fontStyle: FontStyle.italic),
              ),
            ],
            const SizedBox(height: 6),
            Text(
              'User #${reservation.userId}',
              style: const TextStyle(fontSize: 11, color: AppColors.textHint),
            ),
          ],
        ),
      ),
    );
  }
}
