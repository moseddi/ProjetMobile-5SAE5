import '../../core/api/api_client.dart';
import '../../models/reservation.dart';
import '../interfaces/reservation_service.dart';

class ApiReservationService implements ReservationService {
  @override
  Future<List<Reservation>> getUserReservations(String userId) async {
    // The backend derives "my reservations" from the auth token, not a path param.
    final response = await ApiClient.get('/reservations');
    final data = ApiClient.handleResponse(response);

    if (data == null) return [];

    return (data as List)
        .map((json) => Reservation.fromJson(json))
        .toList();
  }

  @override
  Future<Reservation> createReservation(Reservation reservation) async {
    final body = {
      'restaurantId': reservation.restaurantId,
      'reservationDateTime': reservation.datetime.toIso8601String(),
      'partySize': reservation.guests,
      'specialRequests': reservation.notes,
      'contactName': reservation.contactName,
      'contactPhone': reservation.contactPhone,
      if (reservation.orderItems.isNotEmpty)
        'items': reservation.orderItems
            .map((item) => {
                  'menuItemId': item.menuItemId,
                  'quantity': item.quantity,
                })
            .toList(),
    };

    final response = await ApiClient.post('/reservations', body);
    final data = ApiClient.handleResponse(response);
    return Reservation.fromJson(data);
  }

  @override
  Future<void> cancelReservation(String reservationId) async {
    final response = await ApiClient.delete('/reservations/$reservationId');
    ApiClient.handleResponse(response);
  }
}
