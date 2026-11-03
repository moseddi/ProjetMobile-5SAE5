import '../../models/admin_stats.dart';
import '../../models/admin_user.dart';
import '../../models/owner_application.dart';
import '../../models/reservation.dart';
import '../../models/restaurant.dart';
import '../../models/review.dart';

/// Backend operations available only to a signed-in `ADMIN` user: account
/// management, restaurant mutation (create/edit/delete and owner
/// assignment), review moderation, read-only reservation oversight, and
/// platform stats.
///
/// Restaurant *reads* intentionally are not duplicated here - screens can
/// keep using the existing `RestaurantService` (lib/services/interfaces/
/// restaurant_service.dart) for `getRestaurants()` / `getRestaurantById()`.
/// This service only adds the mutations that service doesn't expose.
///
/// This is API-only by design: there is no mock implementation, since the
/// admin console only makes sense against the real backend.
abstract class AdminService {
  // --- Users ---------------------------------------------------------

  /// Lists accounts, optionally filtered by role ('CLIENT',
  /// 'RESTAURANT_OWNER', or 'ADMIN'). Pass null for all accounts.
  Future<List<AdminUser>> getUsers({String? role});

  Future<AdminUser> getUserById(String id);

  /// Activates or deactivates an account. The backend rejects deactivating
  /// the signed-in admin's own account.
  Future<AdminUser> setUserActive(String id, bool active);

  /// Toggles a user between 'CLIENT' and 'ADMIN'. The backend rejects
  /// 'RESTAURANT_OWNER' here - use [assignOwner] for that instead.
  Future<AdminUser> setUserRole(String id, String role);

  /// Promotes [userId] to RESTAURANT_OWNER and links them to
  /// [restaurantId]. Fails if that restaurant already has a different
  /// owner.
  Future<AdminUser> assignOwner(String userId, String restaurantId);

  /// Demotes a restaurant owner back to CLIENT and unlinks their
  /// restaurant.
  Future<AdminUser> unassignOwner(String userId);

  /// Deletes an account. The backend rejects deleting the signed-in
  /// admin's own account.
  Future<void> deleteUser(String id);

  // --- Restaurants -----------------------------------------------------
  // Reads go through RestaurantService; these cover the ADMIN-only writes.

  /// [data] is the full restaurant payload (name, cuisine, description,
  /// image, address, phone, openingHours, isOpen, features, priceRange).
  Future<Restaurant> createRestaurant(Map<String, dynamic> data);

  Future<Restaurant> updateRestaurant(String id, Map<String, dynamic> data);

  Future<void> deleteRestaurant(String id);

  /// Fetches the raw restaurant JSON, including fields the shared
  /// [Restaurant] model doesn't parse (e.g. `features`, `priceRange`) -
  /// used to prefill the edit form with values the app-wide model drops.
  Future<Map<String, dynamic>> getRestaurantRaw(String id);

  // --- Reviews -----------------------------------------------------------

  Future<List<Review>> getAllReviews();

  /// Soft-deletes (moderates) a review.
  Future<void> deleteReview(String id);

  // --- Reservations (read-only oversight) --------------------------------

  Future<List<Reservation>> getAllReservations();

  // --- Stats ---------------------------------------------------------

  Future<AdminStats> getStats();

  // --- Restaurant owner requests ----------------------------------------

  Future<List<OwnerApplication>> getOwnerApplications({String? status});

  /// Creates the owner account and the restaurant from the request.
  Future<OwnerApplication> acceptOwnerApplication(String id);

  Future<OwnerApplication> rejectOwnerApplication(String id, {String? reason});

  // --- Restaurant + owner in one step ------------------------------------

  /// Creates a restaurant and optionally its owner: pass [newOwner]
  /// (name, email, password, phone) to create an account, or
  /// [existingOwnerId] to assign an existing user.
  Future<Restaurant> createRestaurantWithOwner(
    Map<String, dynamic> restaurant, {
    Map<String, dynamic>? newOwner,
    String? existingOwnerId,
  });
}
