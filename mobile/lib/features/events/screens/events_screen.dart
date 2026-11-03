import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../../core/theme/app_colors.dart';
import '../../../core/navigation/app_router.dart';
import '../../../widgets/event_widgets.dart';
import '../providers/event_provider.dart';

class EventsScreen extends StatefulWidget {
  const EventsScreen({super.key});

  @override
  State<EventsScreen> createState() => _EventsScreenState();
}

class _EventsScreenState extends State<EventsScreen> {
  @override
  void initState() {
    super.initState();
    context.read<EventProvider>().loadEvents();
  }

  @override
  Widget build(BuildContext context) {
    final events = context.watch<EventProvider>();
    final filtered = events.filteredEvents;

    return Scaffold(
      backgroundColor: AppColors.background,
      appBar: AppBar(
        title: const Text('Events'),
      ),
      body: RefreshIndicator(
        onRefresh: events.loadEvents,
        child: ListView(
          padding: const EdgeInsets.fromLTRB(20, 16, 20, 32),
          children: [
            EventFilterPanel(
              filters: events.filters,
              defaults: EventProvider.defaultFilters,
              resultCount: filtered.length,
              customCategories: events.customCategories,
              onChanged: events.setFilters,
            ),
            const SizedBox(height: 6),
            if (events.isLoading && !events.hasEvents)
              const Padding(
                padding: EdgeInsets.symmetric(vertical: 60),
                child: Center(child: CircularProgressIndicator()),
              )
            else if (events.error != null && !events.hasEvents)
              _buildError(events.error!, events.loadEvents)
            else if (filtered.isEmpty)
              EventEmptyState(
                filtered: !events.filters.sameAs(EventProvider.defaultFilters) || events.hasEvents,
                onReset: events.resetFilters,
              )
            else
              EventCardGrid(
                children: [
                  for (final event in filtered)
                    EventCard(
                      title: event.title,
                      restaurantName: event.restaurantName,
                      image: event.image,
                      date: event.datetime,
                      category: event.category,
                      price: event.price,
                      availableSpots: event.availableSpots,
                      bookedSpots: event.bookedSpots,
                      onTap: () async {
                        await Navigator.of(context).pushNamed(AppRouter.eventDetails, arguments: event);
                        if (context.mounted) events.loadEvents();
                      },
                    ),
                ],
              ),
          ],
        ),
      ),
    );
  }

  Widget _buildError(String message, Future<void> Function() retry) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 40),
      child: Column(
        children: [
          const Icon(Icons.error_outline, size: 48, color: AppColors.error),
          const SizedBox(height: 12),
          Text(message, textAlign: TextAlign.center, style: const TextStyle(color: AppColors.textSecondary)),
          const SizedBox(height: 14),
          ElevatedButton(onPressed: retry, child: const Text('Retry')),
        ],
      ),
    );
  }
}
