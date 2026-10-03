import '../../core/constants/app_constants.dart';
import '../../models/restaurant.dart';
import '../../data/mock/mock_restaurants.dart';
import '../interfaces/restaurant_service.dart';

class MockRestaurantService implements RestaurantService {
  @override
  Future<List<Restaurant>> getRestaurants() async {
    await Future.delayed(AppConstants.apiDelay);
    return MockRestaurants.restaurants;
  }

  @override
  Future<Restaurant?> getRestaurantById(String id) async {
    await Future.delayed(AppConstants.shortDelay);
    try {
      return MockRestaurants.restaurants.firstWhere((r) => r.id == id);
    } catch (e) {
      return null;
    }
  }

  @override
  Future<List<Restaurant>> searchRestaurants(String query) async {
    await Future.delayed(AppConstants.shortDelay);
    final lowerQuery = query.toLowerCase();
    return MockRestaurants.restaurants.where((r) {
      return r.name.toLowerCase().contains(lowerQuery) ||
          r.cuisine.toLowerCase().contains(lowerQuery) ||
          r.address.toLowerCase().contains(lowerQuery);
    }).toList();
  }
}
