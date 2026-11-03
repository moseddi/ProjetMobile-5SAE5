import '../../core/api/api_client.dart';
import '../../models/restaurant.dart';
import '../../models/reservation.dart';
import '../../models/review.dart';
import '../../models/owner_restaurant.dart';
import '../../models/owner_event.dart';
import '../../models/owner_stats.dart';
import '../interfaces/owner_service.dart';

class ApiOwnerService implements OwnerService {
  @override
  Future<OwnerRestaurant> getMyRestaurant() async {
    final response = await ApiClient.get('/owner/restaurant');
    final data = ApiClient.handleResponse(response);
    return OwnerRestaurant.fromJson(data);
  }

  @override
  Future<OwnerRestaurant> updateMyRestaurant(OwnerRestaurant restaurant) async {
    final response = await ApiClient.put('/owner/restaurant', restaurant.toJson());
    final data = ApiClient.handleResponse(response);
    return OwnerRestaurant.fromJson(data);
  }

  @override
  Future<List<MenuItem>> getMyMenuItems() async {
    final response = await ApiClient.get('/owner/menu-items');
    final data = ApiClient.handleResponse(response);

    if (data == null) return [];

    return (data as List).map((json) => MenuItem.fromJson(json)).toList();
  }

  @override
  Future<MenuItem> createMenuItem(MenuItem item) async {
    final response = await ApiClient.post('/owner/menu-items', item.toJson());
    final data = ApiClient.handleResponse(response);
    return MenuItem.fromJson(data);
  }

  @override
  Future<MenuItem> updateMenuItem(String id, MenuItem item) async {
    final response = await ApiClient.put('/owner/menu-items/$id', item.toJson());
    final data = ApiClient.handleResponse(response);
    return MenuItem.fromJson(data);
  }

  @override
  Future<void> deleteMenuItem(String id) async {
    final response = await ApiClient.delete('/owner/menu-items/$id');
    ApiClient.handleResponse(response);
  }

  @override
  Future<List<Reservation>> getMyReservations() async {
    final response = await ApiClient.get('/owner/reservations');
    final data = ApiClient.handleResponse(response);

    if (data == null) return [];

    return (data as List).map((json) => Reservation.fromJson(json)).toList();
  }

  @override
  Future<Reservation> updateReservationStatus(String id, String status) async {
    final response = await ApiClient.put(
      '/owner/reservations/$id/status',
      {'status': status},
    );
    final data = ApiClient.handleResponse(response);
    return Reservation.fromJson(data);
  }

  @override
  Future<List<OwnerEvent>> getMyEvents() async {
    final response = await ApiClient.get('/owner/events');
    final data = ApiClient.handleResponse(response);

    if (data == null) return [];

    return (data as List).map((json) => OwnerEvent.fromJson(json)).toList();
  }

  @override
  Future<OwnerEvent> createEvent(OwnerEvent event) async {
    final response = await ApiClient.post('/owner/events', event.toJson());
    final data = ApiClient.handleResponse(response);
    return OwnerEvent.fromJson(data);
  }

  @override
  Future<OwnerEvent> updateEvent(String id, OwnerEvent event) async {
    final response = await ApiClient.put('/owner/events/$id', event.toJson());
    final data = ApiClient.handleResponse(response);
    return OwnerEvent.fromJson(data);
  }

  @override
  Future<void> deleteEvent(String id) async {
    final response = await ApiClient.delete('/owner/events/$id');
    ApiClient.handleResponse(response);
  }

  @override
  Future<List<Review>> getMyReviews() async {
    final response = await ApiClient.get('/owner/reviews');
    final data = ApiClient.handleResponse(response);

    if (data == null) return [];

    return (data as List).map((json) => Review.fromJson(json)).toList();
  }

  @override
  Future<OwnerStats> getStats() async {
    final response = await ApiClient.get('/owner/stats');
    final data = ApiClient.handleResponse(response);
    return OwnerStats.fromJson(data);
  }
}
