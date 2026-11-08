import '../../models/loyalty.dart';
import '../../models/restaurant.dart';
import '../../models/reservation.dart';
import '../../models/review.dart';
import '../../models/owner_restaurant.dart';
import '../../models/owner_event.dart';
import '../../models/owner_stats.dart';

/// Backs the "Restaurant Owner" console. Every method here operates on "my
/// restaurant" as inferred by the backend from the signed-in owner's auth
/// token - none of them take a restaurantId.
abstract class OwnerService {
  Future<OwnerRestaurant> getMyRestaurant();
  Future<OwnerRestaurant> updateMyRestaurant(OwnerRestaurant restaurant);

  Future<List<MenuItem>> getMyMenuItems();
  Future<MenuItem> createMenuItem(MenuItem item);
  Future<MenuItem> updateMenuItem(String id, MenuItem item);
  Future<void> deleteMenuItem(String id);

  Future<List<Reservation>> getMyReservations();
  Future<Reservation> updateReservationStatus(String id, String status);

  Future<List<OwnerEvent>> getMyEvents();
  Future<OwnerEvent> createEvent(OwnerEvent event);
  Future<OwnerEvent> updateEvent(String id, OwnerEvent event);
  Future<void> deleteEvent(String id);

  Future<List<Review>> getMyReviews();

  Future<OwnerStats> getStats();

  /// Switches the restaurant's loyalty rewards programme on or off.
  Future<OwnerRestaurant> setLoyaltyEnabled(bool enabled);

  /// Cashier: who a client QR code belongs to, with what they can use at my restaurant.
  Future<MemberLookup> lookupMember(String code);

  /// Cashier: validates one of my restaurant's rewards (once only).
  Future<Voucher> useVoucher(String voucherId);
}
