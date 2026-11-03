import 'package:flutter/foundation.dart';
import '../../../models/reservation.dart';
import '../../../services/interfaces/reservation_service.dart';
import '../../../widgets/reservation_filters.dart';

/// Reservations module state for the signed-in customer: their table bookings and
/// event registrations, the kind/status filters, creating and cancelling.
class ReservationProvider with ChangeNotifier {
  final ReservationService _reservationService;

  ReservationProvider(this._reservationService);

  List<Reservation> _reservations = [];
  bool _isLoading = false;
  String? _error;
  String? _userId;
  ReservationKind _kind = ReservationKind.all;
  ReservationStatusGroup _group = ReservationStatusGroup.all;

  List<Reservation> get reservations => _reservations;
  bool get isLoading => _isLoading;
  String? get error => _error;
  ReservationKind get kind => _kind;
  ReservationStatusGroup get group => _group;

  /// Reservations of the chosen kind (before the status filter), used for the status counters.
  List<Reservation> get inKind => _reservations.where(_kind.matches).toList();

  /// Reservations after both filters, ordered for review.
  List<Reservation> get visible =>
      sortReservationsForReview(_reservations.where((r) => _kind.matches(r) && _group.contains(r)).toList());

  Future<void> load(String userId) async {
    // Another account signed in: never show the previous account's reservations.
    if (_userId != userId) {
      _reservations = [];
      _userId = userId;
      resetFilters(notify: false);
    }

    _isLoading = true;
    _error = null;
    notifyListeners();

    try {
      _reservations = await _reservationService.getUserReservations(userId);
    } catch (e) {
      _error = e.toString().replaceAll('Exception: ', '');
    } finally {
      _isLoading = false;
      notifyListeners();
    }
  }

  void setKind(ReservationKind kind) {
    _kind = kind;
    notifyListeners();
  }

  void setGroup(ReservationStatusGroup group) {
    _group = group;
    notifyListeners();
  }

  void resetFilters({bool notify = true}) {
    _kind = ReservationKind.all;
    _group = ReservationStatusGroup.all;
    if (notify) notifyListeners();
  }

  /// Sends a new table-reservation request. Throws with the server's message when refused.
  Future<Reservation> create(Reservation reservation) async {
    final created = await _reservationService.createReservation(reservation);
    _reservations = [created, ..._reservations];
    notifyListeners();
    return created;
  }

  Future<void> cancel(String reservationId) async {
    await _reservationService.cancelReservation(reservationId);
    if (_userId != null) {
      await load(_userId!);
    }
  }
}
