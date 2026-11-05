import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import 'package:provider/provider.dart';
import '../../../core/theme/app_colors.dart';
import '../../../models/event.dart';
import '../../../widgets/admin_toolbar.dart';
import '../providers/admin_provider.dart';

class AdminEventsScreen extends StatefulWidget {
  const AdminEventsScreen({super.key});

  @override
  State<AdminEventsScreen> createState() => _AdminEventsScreenState();
}

class _AdminEventsScreenState extends State<AdminEventsScreen> {
  String _query = '';
  String? _ownerFilter; // null = all owners
  AdminSort _sort = AdminSort.newest;

  @override
  void initState() {
    super.initState();
    context.read<AdminProvider>().loadAdminEvents();
  }

  List<Event> _filtered(List<Event> events, AdminProvider admin) {
    final q = _query.trim().toLowerCase();
    final list = events.where((e) {
      if (_ownerFilter != null) {
        final ownerName = admin.ownerNameForRestaurant(e.restaurantId) ?? '';
        if (!ownerName.toLowerCase().contains(_ownerFilter!.toLowerCase())) return false;
      }
      if (q.isEmpty) return true;
      return e.title.toLowerCase().contains(q) ||
          e.restaurantName.toLowerCase().contains(q) ||
          e.description.toLowerCase().contains(q);
    });
    return sortByAdded(list, _sort, (e) => e.id);
  }

  Future<void> _deleteEvent(Event event, AdminProvider admin) async {
    final confirm = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('Delete event'),
        content: Text('Remove "${event.title}" from ${event.restaurantName}? This cannot be undone.'),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(ctx, false),
            child: const Text('Cancel'),
          ),
          ElevatedButton(
            onPressed: () => Navigator.pop(ctx, true),
            style: ElevatedButton.styleFrom(backgroundColor: AppColors.error),
            child: const Text('Delete'),
          ),
        ],
      ),
    );

    if (confirm != true || !mounted) return;

    try {
      await admin.deleteAdminEvent(event.id);
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Event deleted'), backgroundColor: AppColors.success),
        );
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text(e.toString().replaceAll('Exception: ', ''))),
        );
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final admin = context.watch<AdminProvider>();
    final filtered = _filtered(admin.adminEvents, admin);

    // Build unique owner names for the filter chips
    final ownerNames = admin.adminEvents
        .map((e) => admin.ownerNameForRestaurant(e.restaurantId))
        .whereType<String>()
        .toSet()
        .toList()
      ..sort();

    return Scaffold(
      backgroundColor: AppColors.background,
      appBar: AppBar(title: const Text('Events')),
      body: Column(
        children: [
          Padding(
            padding: const EdgeInsets.fromLTRB(16, 12, 16, 4),
            child: AdminSearchBar(
              hint: 'Search title, restaurant or description',
              sort: _sort,
              onQueryChanged: (v) => setState(() => _query = v),
              onSortChanged: (v) => setState(() => _sort = v),
            ),
          ),
          if (ownerNames.isNotEmpty)
            Padding(
              padding: const EdgeInsets.fromLTRB(16, 6, 16, 0),
              child: AdminChips<String?>(
                options: [
                  ('All owners', null),
                  for (final name in ownerNames) (name, name),
                ],
                selected: _ownerFilter,
                onChanged: (v) => setState(() => _ownerFilter = v),
              ),
            ),
          const Divider(height: 1),
          Expanded(
            child: RefreshIndicator(
              onRefresh: () => admin.loadAdminEvents(),
              child: admin.isLoadingEvents && admin.adminEvents.isEmpty
                  ? const Center(child: CircularProgressIndicator())
                  : admin.eventsError != null && admin.adminEvents.isEmpty
                      ? _buildError(admin.eventsError!)
                      : admin.adminEvents.isEmpty
                          ? _buildEmpty()
                          : filtered.isEmpty
                              ? ListView(children: [
                                  AdminNoResults(
                                    onReset: () => setState(() {
                                      _query = '';
                                      _ownerFilter = null;
                                    }),
                                  ),
                                ])
                              : ListView.builder(
                                  padding: const EdgeInsets.all(16),
                                  itemCount: filtered.length,
                                  itemBuilder: (_, i) => _buildCard(filtered[i], admin),
                                ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildCard(Event event, AdminProvider admin) {
    final ownerName = admin.ownerNameForRestaurant(event.restaurantId);

    return Card(
      margin: const EdgeInsets.only(bottom: 12),
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Row(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // Event image thumbnail
            ClipRRect(
              borderRadius: BorderRadius.circular(10),
              child: SizedBox(
                width: 64,
                height: 64,
                child: Image.network(
                  event.image,
                  fit: BoxFit.cover,
                  errorBuilder: (_, __, ___) => Container(
                    color: AppColors.border,
                    child: const Icon(Icons.event, color: AppColors.textHint),
                  ),
                ),
              ),
            ),
            const SizedBox(width: 14),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  // Event title
                  Text(
                    event.title,
                    style: const TextStyle(
                      fontSize: 15,
                      fontWeight: FontWeight.bold,
                      color: AppColors.textPrimary,
                    ),
                  ),
                  const SizedBox(height: 4),
                  // Restaurant name
                  Row(
                    children: [
                      const Icon(Icons.restaurant, size: 14, color: AppColors.textSecondary),
                      const SizedBox(width: 4),
                      Expanded(
                        child: Text(
                          event.restaurantName,
                          style: const TextStyle(fontSize: 13, color: AppColors.textSecondary),
                          overflow: TextOverflow.ellipsis,
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 2),
                  // Owner name
                  Row(
                    children: [
                      const Icon(Icons.person_outline, size: 14, color: AppColors.textSecondary),
                      const SizedBox(width: 4),
                      Text(
                        ownerName ?? 'No owner',
                        style: TextStyle(
                          fontSize: 13,
                          color: ownerName != null ? AppColors.textSecondary : AppColors.textHint,
                          fontStyle: ownerName != null ? FontStyle.normal : FontStyle.italic,
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 6),
                  // Date
                  Text(
                    DateFormat('MMM dd, yyyy • h:mm a').format(event.datetime),
                    style: const TextStyle(fontSize: 12, color: AppColors.textHint),
                  ),
                ],
              ),
            ),
            IconButton(
              onPressed: () => _deleteEvent(event, admin),
              icon: const Icon(Icons.delete_outline, color: AppColors.error),
              tooltip: 'Delete event',
            ),
          ],
        ),
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
            message,
            textAlign: TextAlign.center,
            style: const TextStyle(color: AppColors.textSecondary),
          ),
        ),
      ],
    );
  }

  Widget _buildEmpty() {
    return ListView(
      padding: const EdgeInsets.all(20),
      children: [
        const SizedBox(height: 60),
        Icon(Icons.event_outlined, size: 80, color: AppColors.textHint),
        const SizedBox(height: 16),
        const Center(
          child: Text(
            'No events yet',
            style: TextStyle(fontSize: 18, fontWeight: FontWeight.w600, color: AppColors.textPrimary),
          ),
        ),
      ],
    );
  }
}
