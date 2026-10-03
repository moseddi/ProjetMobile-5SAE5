import '../../core/constants/app_constants.dart';
import '../../models/event.dart';
import '../../data/mock/mock_events.dart';
import '../interfaces/event_service.dart';

class MockEventService implements EventService {
  @override
  Future<List<Event>> getEvents() async {
    await Future.delayed(AppConstants.apiDelay);
    return MockEvents.events;
  }

  @override
  Future<Event?> getEventById(String id) async {
    await Future.delayed(AppConstants.shortDelay);
    try {
      return MockEvents.events.firstWhere((e) => e.id == id);
    } catch (e) {
      return null;
    }
  }

  @override
  Future<List<Event>> getEventsByRestaurant(String restaurantId) async {
    await Future.delayed(AppConstants.shortDelay);
    return MockEvents.events.where((e) => e.restaurantId == restaurantId).toList();
  }
}
