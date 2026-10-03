import '../../core/constants/app_constants.dart';
import '../../models/reservation.dart';
import '../interfaces/reservation_service.dart';

class MockReservationService implements ReservationService {
  final List<Reservation> _reservations = [];

  @override
  Future<Reservation> createReservation(Reservation reservation) async {
    await Future.delayed(AppConstants.apiDelay);
    _reservations.add(reservation);
    return reservation;
  }

  @override
  Future<List<Reservation>> getUserReservations(String userId) async {
    await Future.delayed(AppConstants.shortDelay);
    return _reservations.where((r) => r.userId == userId).toList();
  }

  @override
  Future<void> cancelReservation(String reservationId) async {
    await Future.delayed(AppConstants.shortDelay);
    _reservations.removeWhere((r) => r.id == reservationId);
  }
}
