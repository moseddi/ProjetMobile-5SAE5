import '../../models/restaurant.dart';

abstract class RestaurantService {
  Future<List<Restaurant>> getRestaurants();
  Future<Restaurant?> getRestaurantById(String id);
  Future<List<Restaurant>> searchRestaurants(String query);
}
