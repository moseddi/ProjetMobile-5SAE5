import '../../models/reservation.dart';

abstract class ReservationService {
  Future<Reservation> createReservation(Reservation reservation);
  Future<List<Reservation>> getUserReservations(String userId);
  Future<void> cancelReservation(String reservationId);
}
