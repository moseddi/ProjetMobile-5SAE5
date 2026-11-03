import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../../core/theme/app_colors.dart';
import '../../../models/owner_event.dart';
import '../../../widgets/event_widgets.dart';
import '../providers/owner_provider.dart';
import 'owner_event_form_screen.dart';

class OwnerEventsScreen extends StatefulWidget {
  const OwnerEventsScreen({super.key});

  @override
  State<OwnerEventsScreen> createState() => _OwnerEventsScreenState();
}

class _OwnerEventsScreenState extends State<OwnerEventsScreen> {
  // An owner manages every event of their restaurant, so past ones stay visible by default.
  static const _defaults = EventFilters(dateFilter: EventDateFilter.any);

  EventFilters _filters = _defaults;

  @override
  void initState() {
    super.initState();
    context.read<OwnerProvider>().loadEvents();
  }

  Future<void> _reload() => context.read<OwnerProvider>().loadEvents();

  Future<void> _openForm({OwnerEvent? event}) async {
    await Navigator.of(context).push(
      MaterialPageRoute(builder: (_) => OwnerEventFormScreen(event: event)),
    );
  }

  Future<void> _deleteEvent(OwnerEvent event) async {
    final confirm = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Delete event'),
        content: Text('Remove "${event.title}"? This cannot be undone.'),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context, false),
            child: const Text('Cancel'),
          ),
          ElevatedButton(
            onPressed: () => Navigator.pop(context, true),
            style: ElevatedButton.styleFrom(backgroundColor: AppColors.error),
            child: const Text('Delete'),
          ),
        ],
      ),
    );

    if (confirm != true) return;

    try {
      await context.read<OwnerProvider>().removeEvent(event.id);
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

  List<OwnerEvent> _applyFilters(List<OwnerEvent> events) {
    final list = events
        .where((e) => _filters.matches(
              title: e.title,
              restaurantName: e.restaurantName,
              description: e.description,
              category: e.category,
              date: e.eventDate,
            ))
        .toList();

    // Most recently added event first (ids start with their creation time).
    list.sort((a, b) => b.id.compareTo(a.id));
    return list;
  }

  @override
  Widget build(BuildContext context) {
    final ownerProvider = context.watch<OwnerProvider>();
    final all = ownerProvider.events;
    final filtered = _applyFilters(all);

    return Scaffold(
      backgroundColor: AppColors.background,
      appBar: AppBar(
        title: const Text('Events'),
      ),
      floatingActionButton: FloatingActionButton.extended(
        onPressed: () => _openForm(),
        backgroundColor: AppColors.primary,
        foregroundColor: Colors.white,
        icon: const Icon(Icons.add),
        label: const Text('New event'),
      ),
      body: RefreshIndicator(
        onRefresh: _reload,
        child: ListView(
          padding: const EdgeInsets.fromLTRB(20, 16, 20, 110),
          children: [
            EventFilterPanel(
              filters: _filters,
              defaults: _defaults,
              resultCount: filtered.length,
              customCategories: customEventCategories(all.map((e) => e.category)),
              onChanged: (value) => setState(() => _filters = value),
            ),
            const SizedBox(height: 6),
            if (ownerProvider.isLoadingEvents && all.isEmpty)
              const Padding(
                padding: EdgeInsets.symmetric(vertical: 60),
                child: Center(child: CircularProgressIndicator()),
              )
            else if (filtered.isEmpty)
              EventEmptyState(
                filtered: all.isNotEmpty,
                onReset: () => setState(() => _filters = _defaults),
              )
            else
              EventCardGrid(
                children: [
                  for (final event in filtered)
                    EventCard(
                      title: event.title,
                      restaurantName: event.restaurantName,
                      image: event.image,
                      date: event.eventDate,
                      category: event.category,
                      price: event.price,
                      availableSpots: event.availableSpots,
                      bookedSpots: event.bookedSpots,
                      onTap: () => _openForm(event: event),
                      actions: Row(
                        mainAxisAlignment: MainAxisAlignment.end,
                        children: [
                          TextButton.icon(
                            onPressed: () => _openForm(event: event),
                            icon: const Icon(Icons.edit_outlined, size: 18),
                            label: const Text('Edit'),
                          ),
                          TextButton.icon(
                            onPressed: () => _deleteEvent(event),
                            style: TextButton.styleFrom(foregroundColor: AppColors.error),
                            icon: const Icon(Icons.delete_outline, size: 18),
                            label: const Text('Delete'),
                          ),
                        ],
                      ),
                    ),
                ],
              ),
          ],
        ),
      ),
    );
  }
}
