import 'package:flutter/foundation.dart';
import '../../../models/admin_stats.dart';
import '../../../models/admin_user.dart';
import '../../../models/reservation.dart';
import '../../../models/restaurant.dart';
import '../../../models/review.dart';
import '../../../services/interfaces/admin_service.dart';

class AdminProvider extends ChangeNotifier {
  final AdminService _adminService;

  AdminProvider(this._adminService);

  // --- Users -----------------------------------------------------------

  List<AdminUser> _users = [];
  bool _isLoadingUsers = false;
  String? _usersError;
  String? _roleFilter;

  List<AdminUser> get users => _users;
  bool get isLoadingUsers => _isLoadingUsers;
  String? get usersError => _usersError;
  String? get roleFilter => _roleFilter;

  Future<void> loadUsers({String? role}) async {
    _roleFilter = role;
    _isLoadingUsers = true;
    _usersError = null;
    notifyListeners();

    try {
      _users = await _adminService.getUsers(role: role);
    } catch (e) {
      _usersError = e.toString();
    }
    _isLoadingUsers = false;
    notifyListeners();
  }

  Future<void> setRoleFilter(String? role) => loadUsers(role: role);

  Future<AdminUser> setUserActive(String id, bool active) async {
    final updated = await _adminService.setUserActive(id, active);
    _replaceUser(updated);
    return updated;
  }

  Future<AdminUser> setUserRole(String id, String role) async {
    final updated = await _adminService.setUserRole(id, role);
    _replaceUser(updated);
    return updated;
  }

  Future<AdminUser> assignOwner(String userId, String restaurantId) async {
    final updated = await _adminService.assignOwner(userId, restaurantId);
    _replaceUser(updated);
    return updated;
  }

  Future<AdminUser> unassignOwner(String userId) async {
    final updated = await _adminService.unassignOwner(userId);
    _replaceUser(updated);
    return updated;
  }

  Future<void> deleteUser(String id) async {
    await _adminService.deleteUser(id);
    _users = _users.where((u) => u.id != id).toList();
    notifyListeners();
  }

  void _replaceUser(AdminUser updated) {
    final index = _users.indexWhere((u) => u.id == updated.id);
    if (index != -1) {
      _users[index] = updated;
      notifyListeners();
    }
  }

  // --- Restaurants ---------------------------------------------------
  // Reads live on RestaurantService; these are just the ADMIN-only writes.

  Future<Restaurant> createRestaurant(Map<String, dynamic> data) {
    return _adminService.createRestaurant(data);
  }

  /// Creates a restaurant together with its owner: a brand-new account
  /// ([newOwner]) or an existing user ([existingOwnerId]); neither leaves it unassigned.
  Future<Restaurant> createRestaurantWithOwner(
    Map<String, dynamic> data, {
    Map<String, dynamic>? newOwner,
    String? existingOwnerId,
  }) {
    return _adminService.createRestaurantWithOwner(data, newOwner: newOwner, existingOwnerId: existingOwnerId);
  }

  /// Clients that can be promoted to owner (used by the restaurant form).
  Future<List<AdminUser>> getAssignableUsers() async {
    final clients = await _adminService.getUsers(role: 'CLIENT');
    return clients.where((u) => u.active).toList();
  }

  Future<Restaurant> updateRestaurant(String id, Map<String, dynamic> data) {
    return _adminService.updateRestaurant(id, data);
  }

  Future<void> deleteRestaurant(String id) {
    return _adminService.deleteRestaurant(id);
  }

  Future<Map<String, dynamic>> getRestaurantRaw(String id) {
    return _adminService.getRestaurantRaw(id);
  }

  // --- Reviews -----------------------------------------------------------

  List<Review> _reviews = [];
  bool _isLoadingReviews = false;
  String? _reviewsError;

  List<Review> get reviews => _reviews;
  bool get isLoadingReviews => _isLoadingReviews;
  String? get reviewsError => _reviewsError;

  Future<void> loadReviews() async {
    _isLoadingReviews = true;
    _reviewsError = null;
    notifyListeners();

    try {
      _reviews = await _adminService.getAllReviews();
    } catch (e) {
      _reviewsError = e.toString();
    }
    _isLoadingReviews = false;
    notifyListeners();
  }

  Future<void> deleteReview(String id) async {
    await _adminService.deleteReview(id);
    _reviews = _reviews.where((r) => r.id != id).toList();
    notifyListeners();
  }

  // --- Reservations (read-only oversight) --------------------------------

  List<Reservation> _reservations = [];
  bool _isLoadingReservations = false;
  String? _reservationsError;

  List<Reservation> get reservations => _reservations;
  bool get isLoadingReservations => _isLoadingReservations;
  String? get reservationsError => _reservationsError;

  Future<void> loadReservations() async {
    _isLoadingReservations = true;
    _reservationsError = null;
    notifyListeners();

    try {
      _reservations = await _adminService.getAllReservations();
    } catch (e) {
      _reservationsError = e.toString();
    }
    _isLoadingReservations = false;
    notifyListeners();
  }

  // --- Stats -----------------------------------------------------------

  AdminStats? _stats;
  bool _isLoadingStats = false;
  String? _statsError;

  AdminStats? get stats => _stats;
  bool get isLoadingStats => _isLoadingStats;
  String? get statsError => _statsError;

  Future<void> loadStats() async {
    _isLoadingStats = true;
    _statsError = null;
    notifyListeners();

    try {
      _stats = await _adminService.getStats();
    } catch (e) {
      _statsError = e.toString();
    }
    _isLoadingStats = false;
    notifyListeners();
  }
}
