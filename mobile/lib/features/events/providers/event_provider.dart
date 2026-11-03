import 'package:flutter/foundation.dart';
import '../../../models/event.dart';
import '../../../models/reservation.dart';
import '../../../services/interfaces/event_service.dart';
import '../../../widgets/event_widgets.dart';

/// Events module state: the list of events, the search/type/date filters the
/// customer chose, and registering for an event.
class EventProvider with ChangeNotifier {
  static const EventFilters defaultFilters = EventFilters(dateFilter: EventDateFilter.upcoming);

  final EventService _eventService;

  EventProvider(this._eventService);

  List<Event> _events = [];
  bool _isLoading = false;
  String? _error;
  EventFilters _filters = defaultFilters;

  List<Event> get events => _events;
  bool get isLoading => _isLoading;
  String? get error => _error;
  EventFilters get filters => _filters;
  bool get hasEvents => _events.isNotEmpty;

  /// Custom event types (created by owners) present in the data.
  List<String> get customCategories => customEventCategories(_events.map((e) => e.category));

  /// Events matching the filters, newest added first.
  List<Event> get filteredEvents {
    final list = _events
        .where((e) => _filters.matches(
              title: e.title,
              restaurantName: e.restaurantName,
              description: e.description,
              category: e.category,
              date: e.datetime,
            ))
        .toList();

    // Most recently added event first (ids start with their creation time).
    list.sort((a, b) => b.id.compareTo(a.id));
    return list;
  }

  Future<void> loadEvents() async {
    _isLoading = true;
    _error = null;
    notifyListeners();

    try {
      _events = await _eventService.getEvents();
    } catch (e) {
      _error = e.toString().replaceAll('Exception: ', '');
    } finally {
      _isLoading = false;
      notifyListeners();
    }
  }

  void setFilters(EventFilters filters) {
    _filters = filters;
    notifyListeners();
  }

  void resetFilters() => setFilters(defaultFilters);

  /// Sends a registration request. The spots are held right away, so the list
  /// reflects them without another round trip. Throws with the server's message
  /// (e.g. "Only 2 spots are left") when refused.
  Future<Reservation> register(
    Event event, {
    required String name,
    required String phone,
    required int people,
  }) async {
    final reservation = await _eventService.registerForEvent(
      event.id,
      name: name,
      phone: phone,
      people: people,
    );

    final index = _events.indexWhere((e) => e.id == event.id);
    if (index != -1) {
      _events[index] = _events[index].copyWith(bookedSpots: _events[index].bookedSpots + people);
      notifyListeners();
    }
    return reservation;
  }
}
