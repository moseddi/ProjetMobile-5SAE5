import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import '../core/theme/app_colors.dart';

// ---------------------------------------------------------------------------
// Categories
// ---------------------------------------------------------------------------

class EventCategoryOption {
  final String key;
  final String label;
  final IconData icon;

  const EventCategoryOption(this.key, this.label, this.icon);
}

const List<EventCategoryOption> eventCategoryOptions = [
  EventCategoryOption('WINE_TASTING', 'Wine Tasting', Icons.wine_bar),
  EventCategoryOption('COOKING_CLASS', 'Cooking Class', Icons.soup_kitchen),
  EventCategoryOption('LIVE_MUSIC', 'Live Music', Icons.music_note),
  EventCategoryOption('SPECIAL_DINNER', 'Special Dinner', Icons.dinner_dining),
  EventCategoryOption('THEMED_NIGHT', 'Themed Night', Icons.nightlife),
  EventCategoryOption('OTHER', 'Other', Icons.celebration),
];

/// Resolves a category key to its display option. Keys that aren't one of the
/// built-ins are custom types typed by a restaurant owner: the text itself is the label.
EventCategoryOption eventCategoryOf(String key) {
  for (final option in eventCategoryOptions) {
    if (option.key == key) return option;
  }
  return EventCategoryOption(key, key, Icons.local_activity);
}

bool isBuiltInEventCategory(String key) => eventCategoryOptions.any((c) => c.key == key);

/// The distinct custom (non built-in) types present in [keys], alphabetically.
List<String> customEventCategories(Iterable<String> keys) {
  final seen = <String>{};
  final result = <String>[];
  for (final key in keys) {
    if (key.trim().isEmpty || isBuiltInEventCategory(key)) continue;
    if (seen.add(key.toLowerCase())) result.add(key);
  }
  result.sort((a, b) => a.toLowerCase().compareTo(b.toLowerCase()));
  return result;
}

// ---------------------------------------------------------------------------
// Filters
// ---------------------------------------------------------------------------

enum EventDateFilter { upcoming, any, today, next7Days, thisMonth, past, custom }

extension EventDateFilterLabel on EventDateFilter {
  String get label {
    switch (this) {
      case EventDateFilter.upcoming:
        return 'Upcoming';
      case EventDateFilter.any:
        return 'Any date';
      case EventDateFilter.today:
        return 'Today';
      case EventDateFilter.next7Days:
        return 'Next 7 days';
      case EventDateFilter.thisMonth:
        return 'This month';
      case EventDateFilter.past:
        return 'Past';
      case EventDateFilter.custom:
        return 'Pick dates';
    }
  }

  IconData get icon {
    switch (this) {
      case EventDateFilter.upcoming:
        return Icons.upcoming_outlined;
      case EventDateFilter.any:
        return Icons.all_inclusive;
      case EventDateFilter.today:
        return Icons.today;
      case EventDateFilter.next7Days:
        return Icons.date_range;
      case EventDateFilter.thisMonth:
        return Icons.calendar_month;
      case EventDateFilter.past:
        return Icons.history;
      case EventDateFilter.custom:
        return Icons.edit_calendar;
    }
  }
}

class EventFilters {
  final String query;
  final String? categoryKey;
  final EventDateFilter dateFilter;
  final DateTimeRange? customRange;

  const EventFilters({
    this.query = '',
    this.categoryKey,
    this.dateFilter = EventDateFilter.upcoming,
    this.customRange,
  });

  EventFilters copyWith({
    String? query,
    String? Function()? categoryKey,
    EventDateFilter? dateFilter,
    DateTimeRange? Function()? customRange,
  }) {
    return EventFilters(
      query: query ?? this.query,
      categoryKey: categoryKey != null ? categoryKey() : this.categoryKey,
      dateFilter: dateFilter ?? this.dateFilter,
      customRange: customRange != null ? customRange() : this.customRange,
    );
  }

  bool sameAs(EventFilters other) {
    return query == other.query &&
        categoryKey == other.categoryKey &&
        dateFilter == other.dateFilter &&
        customRange == other.customRange;
  }

  bool matches({
    required String title,
    required String restaurantName,
    required String description,
    required String category,
    required DateTime date,
  }) {
    final q = query.trim().toLowerCase();
    if (q.isNotEmpty) {
      final haystack =
          '$title $restaurantName $description ${eventCategoryOf(category).label}'.toLowerCase();
      if (!haystack.contains(q)) return false;
    }

    if (categoryKey != null && category != categoryKey) return false;

    return matchesDateFilter(date, dateFilter, customRange);
  }
}

/// Whether [date] falls inside the chosen date filter. Shared by events and hangouts.
bool matchesDateFilter(DateTime date, EventDateFilter filter, DateTimeRange? customRange) {
  final now = DateTime.now();
  final startOfToday = DateTime(now.year, now.month, now.day);

  switch (filter) {
    case EventDateFilter.any:
      return true;
    case EventDateFilter.upcoming:
      return !date.isBefore(now);
    case EventDateFilter.past:
      return date.isBefore(now);
    case EventDateFilter.today:
      return date.year == now.year && date.month == now.month && date.day == now.day;
    case EventDateFilter.next7Days:
      return !date.isBefore(startOfToday) && date.isBefore(startOfToday.add(const Duration(days: 8)));
    case EventDateFilter.thisMonth:
      return date.year == now.year && date.month == now.month;
    case EventDateFilter.custom:
      final range = customRange;
      if (range == null) return true;
      final start = DateTime(range.start.year, range.start.month, range.start.day);
      final end = DateTime(range.end.year, range.end.month, range.end.day).add(const Duration(days: 1));
      return !date.isBefore(start) && date.isBefore(end);
  }
}

/// Date pills (Upcoming, Today, Next 7 days, This month, Past, Any date) plus a
/// "Pick dates" pill that opens a calendar range picker.
class DateFilterPills extends StatelessWidget {
  final EventDateFilter selected;
  final DateTimeRange? customRange;
  final void Function(EventDateFilter filter, DateTimeRange? range) onChanged;

  const DateFilterPills({
    super.key,
    required this.selected,
    required this.customRange,
    required this.onChanged,
  });

  Future<void> _pickRange(BuildContext context) async {
    final now = DateTime.now();
    final picked = await showDateRangePicker(
      context: context,
      firstDate: DateTime(now.year - 2),
      lastDate: DateTime(now.year + 3),
      initialDateRange: customRange,
      helpText: 'Show dates between',
    );
    if (picked != null) onChanged(EventDateFilter.custom, picked);
  }

  String _customLabel(DateTimeRange range) {
    final f = DateFormat('MMM d');
    if (range.start == range.end) return f.format(range.start);
    return '${f.format(range.start)} - ${f.format(range.end)}';
  }

  @override
  Widget build(BuildContext context) {
    const options = [
      EventDateFilter.upcoming,
      EventDateFilter.today,
      EventDateFilter.next7Days,
      EventDateFilter.thisMonth,
      EventDateFilter.past,
      EventDateFilter.any,
    ];

    return SizedBox(
      height: 42,
      child: ListView(
        scrollDirection: Axis.horizontal,
        children: [
          for (final option in options)
            _pill(option.label, option.icon, selected == option, () => onChanged(option, customRange)),
          _pill(
            selected == EventDateFilter.custom && customRange != null
                ? _customLabel(customRange!)
                : EventDateFilter.custom.label,
            EventDateFilter.custom.icon,
            selected == EventDateFilter.custom,
            () => _pickRange(context),
          ),
        ],
      ),
    );
  }

  Widget _pill(String label, IconData icon, bool isSelected, VoidCallback onTap) {
    return Padding(
      padding: const EdgeInsets.only(right: 10),
      child: Material(
        color: isSelected ? AppColors.primary : AppColors.surface,
        borderRadius: BorderRadius.circular(22),
        child: InkWell(
          onTap: onTap,
          borderRadius: BorderRadius.circular(22),
          child: Container(
            padding: const EdgeInsets.symmetric(horizontal: 14),
            decoration: BoxDecoration(
              borderRadius: BorderRadius.circular(22),
              border: Border.all(color: isSelected ? AppColors.primary : AppColors.border),
            ),
            child: Row(
              children: [
                Icon(icon, size: 16, color: isSelected ? AppColors.accentLight : AppColors.textSecondary),
                const SizedBox(width: 7),
                Text(
                  label,
                  style: TextStyle(
                    fontSize: 13,
                    fontWeight: FontWeight.w600,
                    color: isSelected ? Colors.white : AppColors.textPrimary,
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}

// ---------------------------------------------------------------------------
// Filter panel: search + type pills + date pills
// ---------------------------------------------------------------------------

class EventFilterPanel extends StatefulWidget {
  final EventFilters filters;
  final EventFilters defaults;
  final ValueChanged<EventFilters> onChanged;
  final int resultCount;

  /// Custom event types (created by owners) present in the data, shown after the built-ins.
  final List<String> customCategories;

  const EventFilterPanel({
    super.key,
    required this.filters,
    required this.defaults,
    required this.onChanged,
    required this.resultCount,
    this.customCategories = const [],
  });

  @override
  State<EventFilterPanel> createState() => _EventFilterPanelState();
}

class _EventFilterPanelState extends State<EventFilterPanel> {
  late final TextEditingController _searchController;

  @override
  void initState() {
    super.initState();
    _searchController = TextEditingController(text: widget.filters.query);
  }

  @override
  void didUpdateWidget(covariant EventFilterPanel oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (_searchController.text != widget.filters.query) {
      _searchController.text = widget.filters.query;
    }
  }

  @override
  void dispose() {
    _searchController.dispose();
    super.dispose();
  }

  Future<void> _pickRange() async {
    final now = DateTime.now();
    final picked = await showDateRangePicker(
      context: context,
      firstDate: DateTime(now.year - 2),
      lastDate: DateTime(now.year + 3),
      initialDateRange: widget.filters.customRange,
      helpText: 'Show events between',
    );
    if (picked != null) {
      widget.onChanged(widget.filters.copyWith(
        dateFilter: EventDateFilter.custom,
        customRange: () => picked,
      ));
    }
  }

  String _customLabel(DateTimeRange range) {
    final f = DateFormat('MMM d');
    if (range.start == range.end) return f.format(range.start);
    return '${f.format(range.start)} - ${f.format(range.end)}';
  }

  @override
  Widget build(BuildContext context) {
    final filters = widget.filters;
    final isDefault = filters.sameAs(widget.defaults);

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        TextField(
          controller: _searchController,
          onChanged: (value) => widget.onChanged(filters.copyWith(query: value)),
          textInputAction: TextInputAction.search,
          decoration: InputDecoration(
            hintText: 'Search events, restaurants, themes...',
            prefixIcon: const Icon(Icons.search, color: AppColors.textSecondary),
            suffixIcon: filters.query.isEmpty
                ? null
                : IconButton(
                    icon: const Icon(Icons.close, size: 18),
                    onPressed: () => widget.onChanged(filters.copyWith(query: '')),
                  ),
            filled: true,
            fillColor: AppColors.surface,
            contentPadding: const EdgeInsets.symmetric(vertical: 16),
            border: OutlineInputBorder(
              borderRadius: BorderRadius.circular(18),
              borderSide: const BorderSide(color: AppColors.border),
            ),
            enabledBorder: OutlineInputBorder(
              borderRadius: BorderRadius.circular(18),
              borderSide: const BorderSide(color: AppColors.border),
            ),
            focusedBorder: OutlineInputBorder(
              borderRadius: BorderRadius.circular(18),
              borderSide: const BorderSide(color: AppColors.accent, width: 1.6),
            ),
          ),
        ),
        const SizedBox(height: 18),
        _label('Type'),
        const SizedBox(height: 8),
        SizedBox(
          height: 42,
          child: ListView(
            scrollDirection: Axis.horizontal,
            children: [
              _pill(
                label: 'All types',
                icon: Icons.apps,
                selected: filters.categoryKey == null,
                onTap: () => widget.onChanged(filters.copyWith(categoryKey: () => null)),
              ),
              for (final option in eventCategoryOptions)
                _pill(
                  label: option.label,
                  icon: option.icon,
                  selected: filters.categoryKey == option.key,
                  onTap: () => widget.onChanged(filters.copyWith(categoryKey: () => option.key)),
                ),
              for (final custom in widget.customCategories)
                _pill(
                  label: custom,
                  icon: Icons.local_activity,
                  selected: filters.categoryKey == custom,
                  onTap: () => widget.onChanged(filters.copyWith(categoryKey: () => custom)),
                ),
            ],
          ),
        ),
        const SizedBox(height: 16),
        _label('When'),
        const SizedBox(height: 8),
        SizedBox(
          height: 42,
          child: ListView(
            scrollDirection: Axis.horizontal,
            children: [
              for (final option in const [
                EventDateFilter.upcoming,
                EventDateFilter.today,
                EventDateFilter.next7Days,
                EventDateFilter.thisMonth,
                EventDateFilter.past,
                EventDateFilter.any,
              ])
                _pill(
                  label: option.label,
                  icon: option.icon,
                  selected: filters.dateFilter == option,
                  onTap: () => widget.onChanged(filters.copyWith(dateFilter: option)),
                ),
              _pill(
                label: filters.dateFilter == EventDateFilter.custom && filters.customRange != null
                    ? _customLabel(filters.customRange!)
                    : EventDateFilter.custom.label,
                icon: EventDateFilter.custom.icon,
                selected: filters.dateFilter == EventDateFilter.custom,
                onTap: _pickRange,
              ),
            ],
          ),
        ),
        const SizedBox(height: 14),
        Row(
          children: [
            Text(
              '${widget.resultCount} ${widget.resultCount == 1 ? 'event' : 'events'}',
              style: const TextStyle(
                fontSize: 13,
                fontWeight: FontWeight.w600,
                color: AppColors.textSecondary,
              ),
            ),
            const Spacer(),
            if (!isDefault)
              TextButton.icon(
                onPressed: () => widget.onChanged(widget.defaults),
                icon: const Icon(Icons.filter_alt_off, size: 16),
                label: const Text('Reset filters'),
              ),
          ],
        ),
      ],
    );
  }

  Widget _label(String text) {
    return Text(
      text.toUpperCase(),
      style: const TextStyle(
        fontSize: 11,
        fontWeight: FontWeight.w800,
        letterSpacing: 1.1,
        color: AppColors.textHint,
      ),
    );
  }

  Widget _pill({
    required String label,
    required IconData icon,
    required bool selected,
    required VoidCallback onTap,
  }) {
    return Padding(
      padding: const EdgeInsets.only(right: 10),
      child: Material(
        color: selected ? AppColors.primary : AppColors.surface,
        borderRadius: BorderRadius.circular(22),
        child: InkWell(
          onTap: onTap,
          borderRadius: BorderRadius.circular(22),
          child: Container(
            padding: const EdgeInsets.symmetric(horizontal: 14),
            decoration: BoxDecoration(
              borderRadius: BorderRadius.circular(22),
              border: Border.all(color: selected ? AppColors.primary : AppColors.border),
            ),
            child: Row(
              children: [
                Icon(icon, size: 16, color: selected ? AppColors.accentLight : AppColors.textSecondary),
                const SizedBox(width: 7),
                Text(
                  label,
                  style: TextStyle(
                    fontSize: 13,
                    fontWeight: FontWeight.w600,
                    color: selected ? Colors.white : AppColors.textPrimary,
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}

// ---------------------------------------------------------------------------
// Event card
// ---------------------------------------------------------------------------

class EventCard extends StatelessWidget {
  final String title;
  final String restaurantName;
  final String image;
  final DateTime date;
  final String category;
  final double price;
  final int availableSpots;
  final int bookedSpots;
  final VoidCallback? onTap;
  final Widget? actions;

  const EventCard({
    super.key,
    required this.title,
    required this.restaurantName,
    required this.image,
    required this.date,
    required this.category,
    required this.price,
    required this.availableSpots,
    required this.bookedSpots,
    this.onTap,
    this.actions,
  });

  bool get _isPast => date.isBefore(DateTime.now());
  int get _spotsLeft => (availableSpots - bookedSpots).clamp(0, availableSpots);
  bool get _isFull => _spotsLeft <= 0;

  @override
  Widget build(BuildContext context) {
    final option = eventCategoryOf(category);

    return Material(
      color: AppColors.surface,
      borderRadius: BorderRadius.circular(22),
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(22),
        child: Container(
          decoration: BoxDecoration(
            borderRadius: BorderRadius.circular(22),
            border: Border.all(color: AppColors.border),
            boxShadow: [
              BoxShadow(
                color: Colors.black.withOpacity(0.05),
                blurRadius: 14,
                offset: const Offset(0, 6),
              ),
            ],
          ),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              _buildBanner(option),
              Padding(
                padding: const EdgeInsets.fromLTRB(16, 14, 16, 16),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      title,
                      maxLines: 2,
                      overflow: TextOverflow.ellipsis,
                      style: const TextStyle(
                        fontSize: 18,
                        height: 1.2,
                        fontWeight: FontWeight.w800,
                        color: AppColors.textPrimary,
                      ),
                    ),
                    const SizedBox(height: 8),
                    Row(
                      children: [
                        const Icon(Icons.storefront_outlined, size: 15, color: AppColors.textSecondary),
                        const SizedBox(width: 6),
                        Expanded(
                          child: Text(
                            restaurantName,
                            maxLines: 1,
                            overflow: TextOverflow.ellipsis,
                            style: const TextStyle(fontSize: 13.5, color: AppColors.textSecondary),
                          ),
                        ),
                      ],
                    ),
                    const SizedBox(height: 6),
                    Row(
                      children: [
                        const Icon(Icons.schedule, size: 15, color: AppColors.textSecondary),
                        const SizedBox(width: 6),
                        Text(
                          DateFormat('EEE, MMM d • h:mm a').format(date),
                          style: const TextStyle(fontSize: 13.5, color: AppColors.textSecondary),
                        ),
                      ],
                    ),
                    const SizedBox(height: 14),
                    _buildAvailability(),
                    if (actions != null) ...[
                      const SizedBox(height: 10),
                      const Divider(height: 1),
                      const SizedBox(height: 4),
                      actions!,
                    ],
                  ],
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildBanner(EventCategoryOption option) {
    final fallback = Container(
      decoration: BoxDecoration(
        gradient: LinearGradient(
          colors: [AppColors.primary, AppColors.accent.withOpacity(0.85)],
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
        ),
      ),
      child: Center(child: Icon(option.icon, size: 54, color: Colors.white.withOpacity(0.75))),
    );

    return ClipRRect(
      borderRadius: const BorderRadius.vertical(top: Radius.circular(22)),
      child: SizedBox(
        height: 160,
        width: double.infinity,
        child: Stack(
          fit: StackFit.expand,
          children: [
            image.isEmpty
                ? fallback
                : Image.network(
                    image,
                    fit: BoxFit.cover,
                    errorBuilder: (context, error, stackTrace) => fallback,
                  ),
            DecoratedBox(
              decoration: BoxDecoration(
                gradient: LinearGradient(
                  begin: Alignment.topCenter,
                  end: Alignment.bottomCenter,
                  colors: [
                    Colors.black.withOpacity(_isPast ? 0.45 : 0.05),
                    Colors.black.withOpacity(_isPast ? 0.7 : 0.5),
                  ],
                ),
              ),
            ),
            Positioned(
              top: 12,
              left: 12,
              child: Container(
                width: 54,
                padding: const EdgeInsets.symmetric(vertical: 7),
                decoration: BoxDecoration(
                  color: Colors.white,
                  borderRadius: BorderRadius.circular(14),
                ),
                child: Column(
                  children: [
                    Text(
                      DateFormat('d').format(date),
                      style: const TextStyle(
                        fontSize: 21,
                        height: 1,
                        fontWeight: FontWeight.w900,
                        color: AppColors.textPrimary,
                      ),
                    ),
                    const SizedBox(height: 3),
                    Text(
                      DateFormat('MMM').format(date).toUpperCase(),
                      style: const TextStyle(
                        fontSize: 11,
                        fontWeight: FontWeight.w800,
                        letterSpacing: 0.8,
                        color: Color(0xFF9A6B3A),
                      ),
                    ),
                  ],
                ),
              ),
            ),
            Positioned(
              top: 12,
              right: 12,
              child: Container(
                padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
                decoration: BoxDecoration(
                  color: Colors.black.withOpacity(0.45),
                  borderRadius: BorderRadius.circular(20),
                ),
                child: Row(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    Icon(option.icon, size: 14, color: AppColors.accentLight),
                    const SizedBox(width: 6),
                    Text(
                      option.label,
                      style: const TextStyle(
                        fontSize: 12,
                        fontWeight: FontWeight.w700,
                        color: Colors.white,
                      ),
                    ),
                  ],
                ),
              ),
            ),
            Positioned(
              left: 12,
              bottom: 12,
              child: Container(
                padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
                decoration: BoxDecoration(
                  color: AppColors.accent,
                  borderRadius: BorderRadius.circular(20),
                ),
                child: Text(
                  price <= 0 ? 'Free' : '\$${price.toStringAsFixed(price % 1 == 0 ? 0 : 2)}',
                  style: const TextStyle(
                    fontSize: 13,
                    fontWeight: FontWeight.w800,
                    color: AppColors.primaryDark,
                  ),
                ),
              ),
            ),
            if (_isPast)
              Positioned(
                right: 12,
                bottom: 12,
                child: Container(
                  padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 5),
                  decoration: BoxDecoration(
                    color: Colors.white.withOpacity(0.9),
                    borderRadius: BorderRadius.circular(20),
                  ),
                  child: const Text(
                    'Ended',
                    style: TextStyle(fontSize: 12, fontWeight: FontWeight.w800, color: AppColors.textSecondary),
                  ),
                ),
              ),
          ],
        ),
      ),
    );
  }

  Widget _buildAvailability() {
    final ratio = availableSpots == 0 ? 1.0 : (bookedSpots / availableSpots).clamp(0.0, 1.0);
    final Color color;
    final String label;

    if (_isPast) {
      color = AppColors.textHint;
      label = '$bookedSpots attended';
    } else if (_isFull) {
      color = AppColors.error;
      label = 'Sold out';
    } else if (_spotsLeft <= (availableSpots * 0.25).ceil()) {
      color = AppColors.warning;
      label = 'Only $_spotsLeft ${_spotsLeft == 1 ? 'spot' : 'spots'} left';
    } else {
      color = AppColors.success;
      label = '$_spotsLeft spots left';
    }

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          children: [
            Text(
              label,
              style: TextStyle(fontSize: 12.5, fontWeight: FontWeight.w700, color: color),
            ),
            const Spacer(),
            Text(
              '$bookedSpots / $availableSpots',
              style: const TextStyle(fontSize: 12, color: AppColors.textSecondary),
            ),
          ],
        ),
        const SizedBox(height: 7),
        ClipRRect(
          borderRadius: BorderRadius.circular(6),
          child: LinearProgressIndicator(
            value: ratio,
            minHeight: 6,
            backgroundColor: AppColors.background,
            valueColor: AlwaysStoppedAnimation<Color>(color),
          ),
        ),
      ],
    );
  }
}

/// One column on phones, two on tablets, three on wide desktop windows.
class EventCardGrid extends StatelessWidget {
  final List<Widget> children;

  const EventCardGrid({super.key, required this.children});

  @override
  Widget build(BuildContext context) {
    return LayoutBuilder(
      builder: (context, constraints) {
        const gap = 18.0;
        final columns = constraints.maxWidth >= 1100 ? 3 : (constraints.maxWidth >= 700 ? 2 : 1);
        final cardWidth = (constraints.maxWidth - gap * (columns - 1)) / columns;

        return Wrap(
          spacing: gap,
          runSpacing: gap,
          children: [for (final child in children) SizedBox(width: cardWidth, child: child)],
        );
      },
    );
  }
}

class EventEmptyState extends StatelessWidget {
  final bool filtered;
  final VoidCallback? onReset;

  const EventEmptyState({super.key, required this.filtered, this.onReset});

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 40, horizontal: 24),
      child: Column(
        children: [
          Container(
            padding: const EdgeInsets.all(22),
            decoration: BoxDecoration(
              color: AppColors.accent.withOpacity(0.12),
              shape: BoxShape.circle,
            ),
            child: Icon(
              filtered ? Icons.search_off : Icons.event_busy,
              size: 42,
              color: AppColors.accent,
            ),
          ),
          const SizedBox(height: 18),
          Text(
            filtered ? 'No events match your filters' : 'No events yet',
            style: const TextStyle(fontSize: 18, fontWeight: FontWeight.bold, color: AppColors.textPrimary),
          ),
          const SizedBox(height: 6),
          Text(
            filtered
                ? 'Try a different type, another date range or a shorter search.'
                : 'New events will show up here as soon as restaurants publish them.',
            textAlign: TextAlign.center,
            style: const TextStyle(fontSize: 13.5, height: 1.4, color: AppColors.textSecondary),
          ),
          if (filtered && onReset != null) ...[
            const SizedBox(height: 18),
            OutlinedButton.icon(
              onPressed: onReset,
              icon: const Icon(Icons.filter_alt_off, size: 18),
              label: const Text('Reset filters'),
            ),
          ],
        ],
      ),
    );
  }
}
