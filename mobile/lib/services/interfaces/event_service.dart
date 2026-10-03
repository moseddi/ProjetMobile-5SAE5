import '../../models/event.dart';

abstract class EventService {
  Future<List<Event>> getEvents();
  Future<Event?> getEventById(String id);
  Future<List<Event>> getEventsByRestaurant(String restaurantId);
}
