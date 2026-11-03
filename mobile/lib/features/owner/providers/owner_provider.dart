import 'package:flutter/foundation.dart';
import '../../../models/restaurant.dart';
import '../../../models/reservation.dart';
import '../../../models/review.dart';
import '../../../models/owner_restaurant.dart';
import '../../../models/owner_event.dart';
import '../../../models/owner_stats.dart';
import '../../../services/interfaces/owner_service.dart';

/// Holds all state for the Restaurant Owner console: the owner's restaurant
/// profile, menu, reservations, events, reviews and dashboard stats.
class OwnerProvider with ChangeNotifier {
  final OwnerService _ownerService;

  OwnerProvider(this._ownerService);

  // Restaurant profile
  OwnerRestaurant? _restaurant;
  bool _isLoadingRestaurant = false;
  String? _restaurantError;

  OwnerRestaurant? get restaurant => _restaurant;
  bool get isLoadingRestaurant => _isLoadingRestaurant;
  String? get restaurantError => _restaurantError;

  // Menu
  List<MenuItem> _menuItems = [];
  bool _isLoadingMenu = false;
  String? _menuError;

  List<MenuItem> get menuItems => _menuItems;
  bool get isLoadingMenu => _isLoadingMenu;
  String? get menuError => _menuError;

  // Reservations
  List<Reservation> _reservations = [];
  bool _isLoadingReservations = false;
  String? _reservationsError;

  List<Reservation> get reservations => _reservations;
  bool get isLoadingReservations => _isLoadingReservations;
  String? get reservationsError => _reservationsError;

  // Events
  List<OwnerEvent> _events = [];
  bool _isLoadingEvents = false;
  String? _eventsError;

  List<OwnerEvent> get events => _events;
  bool get isLoadingEvents => _isLoadingEvents;
  String? get eventsError => _eventsError;

  // Reviews
  List<Review> _reviews = [];
  bool _isLoadingReviews = false;
  String? _reviewsError;

  List<Review> get reviews => _reviews;
  bool get isLoadingReviews => _isLoadingReviews;
  String? get reviewsError => _reviewsError;

  // Stats
  OwnerStats? _stats;
  bool _isLoadingStats = false;
  String? _statsError;

  OwnerStats? get stats => _stats;
  bool get isLoadingStats => _isLoadingStats;
  String? get statsError => _statsError;

  // --- Restaurant ---

  Future<void> loadRestaurant() async {
    _isLoadingRestaurant = true;
    _restaurantError = null;
    notifyListeners();

    try {
      _restaurant = await _ownerService.getMyRestaurant();
    } catch (e) {
      _restaurantError = e.toString();
    } finally {
      _isLoadingRestaurant = false;
      notifyListeners();
    }
  }

  /// Updates the restaurant profile. Throws on failure so the calling screen
  /// can surface the server's error message (e.g. via a SnackBar).
  Future<void> updateRestaurant(OwnerRestaurant restaurant) async {
    try {
      _restaurant = await _ownerService.updateMyRestaurant(restaurant);
      notifyListeners();
    } catch (e) {
      rethrow;
    }
  }

  // --- Menu ---

  Future<void> loadMenuItems() async {
    _isLoadingMenu = true;
    _menuError = null;
    notifyListeners();

    try {
      _menuItems = await _ownerService.getMyMenuItems();
    } catch (e) {
      _menuError = e.toString();
    } finally {
      _isLoadingMenu = false;
      notifyListeners();
    }
  }

  Future<void> addMenuItem(MenuItem item) async {
    await _ownerService.createMenuItem(item);
    await loadMenuItems();
  }

  Future<void> editMenuItem(String id, MenuItem item) async {
    await _ownerService.updateMenuItem(id, item);
    await loadMenuItems();
  }

  Future<void> removeMenuItem(String id) async {
    await _ownerService.deleteMenuItem(id);
    await loadMenuItems();
  }

  // --- Reservations ---

  Future<void> loadReservations() async {
    _isLoadingReservations = true;
    _reservationsError = null;
    notifyListeners();

    try {
      _reservations = await _ownerService.getMyReservations();
    } catch (e) {
      _reservationsError = e.toString();
    } finally {
      _isLoadingReservations = false;
      notifyListeners();
    }
  }

  Future<void> updateReservationStatus(String id, String status) async {
    final updated = await _ownerService.updateReservationStatus(id, status);
    final index = _reservations.indexWhere((r) => r.id == id);
    if (index != -1) {
      _reservations[index] = updated;
    }
    notifyListeners();
  }

  // --- Events ---

  Future<void> loadEvents() async {
    _isLoadingEvents = true;
    _eventsError = null;
    notifyListeners();

    try {
      _events = await _ownerService.getMyEvents();
    } catch (e) {
      _eventsError = e.toString();
    } finally {
      _isLoadingEvents = false;
      notifyListeners();
    }
  }

  Future<void> addEvent(OwnerEvent event) async {
    await _ownerService.createEvent(event);
    await loadEvents();
  }

  Future<void> editEvent(String id, OwnerEvent event) async {
    await _ownerService.updateEvent(id, event);
    await loadEvents();
  }

  Future<void> removeEvent(String id) async {
    await _ownerService.deleteEvent(id);
    await loadEvents();
  }

  // --- Reviews ---

  Future<void> loadReviews() async {
    _isLoadingReviews = true;
    _reviewsError = null;
    notifyListeners();

    try {
      _reviews = await _ownerService.getMyReviews();
    } catch (e) {
      _reviewsError = e.toString();
    } finally {
      _isLoadingReviews = false;
      notifyListeners();
    }
  }

  // --- Stats ---

  Future<void> loadStats() async {
    _isLoadingStats = true;
    _statsError = null;
    notifyListeners();

    try {
      _stats = await _ownerService.getStats();
    } catch (e) {
      _statsError = e.toString();
    } finally {
      _isLoadingStats = false;
      notifyListeners();
    }
  }
}
