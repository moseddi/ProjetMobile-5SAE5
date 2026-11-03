import '../../core/api/api_client.dart';
import '../../models/event.dart';
import '../../models/reservation.dart';
import '../interfaces/event_service.dart';

class ApiEventService implements EventService {
  @override
  Future<List<Event>> getEvents() async {
    final response = await ApiClient.get('/events');
    final data = ApiClient.handleResponse(response);

    if (data == null) return [];

    return (data as List).map((json) => Event.fromJson(json)).toList();
  }

  @override
  Future<Event?> getEventById(String id) async {
    final response = await ApiClient.get('/events/$id');
    final data = ApiClient.handleResponse(response);
    return data != null ? Event.fromJson(data) : null;
  }

  @override
  Future<List<Event>> getEventsByRestaurant(String restaurantId) async {
    final response = await ApiClient.get('/events/restaurant/$restaurantId');
    final data = ApiClient.handleResponse(response);

    if (data == null) return [];

    return (data as List).map((json) => Event.fromJson(json)).toList();
  }

  @override
  Future<Reservation> registerForEvent(
    String eventId, {
    required String name,
    required String phone,
    required int people,
  }) async {
    final response = await ApiClient.post('/events/$eventId/register', {
      'contactName': name,
      'contactPhone': phone,
      'partySize': people,
    });
    final data = ApiClient.handleResponse(response);
    return Reservation.fromJson(data);
  }
}
