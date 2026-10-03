import '../../core/api/api_client.dart';
import '../../models/reservation.dart';
import '../interfaces/reservation_service.dart';

class ApiReservationService implements ReservationService {
  @override
  Future<List<Reservation>> getUserReservations(String userId) async {
    final response = await ApiClient.get('/users/$userId/reservations');
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
    };
    
    final response = await ApiClient.post(
      '/reservations?userId=${reservation.userId}',
      body,
    );
    
    final data = ApiClient.handleResponse(response);
    return Reservation.fromJson(data);
  }

  @override
  Future<void> cancelReservation(String reservationId) async {
    await ApiClient.delete('/reservations/$reservationId');
  }
}
