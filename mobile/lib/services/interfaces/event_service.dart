import '../../models/event.dart';
import '../../models/reservation.dart';

abstract class EventService {
  Future<List<Event>> getEvents();
  Future<Event?> getEventById(String id);
  Future<List<Event>> getEventsByRestaurant(String restaurantId);

  /// Registers the current user for an event. This creates a reservation that
  /// awaits the restaurant owner's confirmation - it does not instantly
  /// confirm attendance - and the registration also appears in the user's
  /// own reservations list.
  ///
  /// [name], [phone] and [people] are the contact details the restaurant needs
  /// to handle the request; [people] spots are held on the event.
  Future<Reservation> registerForEvent(
    String eventId, {
    required String name,
    required String phone,
    required int people,
  });
}
