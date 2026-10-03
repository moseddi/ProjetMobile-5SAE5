import '../../core/api/api_client.dart';
import '../../models/restaurant.dart';
import '../interfaces/restaurant_service.dart';

class ApiRestaurantService implements RestaurantService {
  @override
  Future<List<Restaurant>> getRestaurants() async {
    final response = await ApiClient.get('/restaurants');
    final data = ApiClient.handleResponse(response);
    
    if (data == null) return [];
    
    return (data as List)
        .map((json) => Restaurant.fromJson(json))
        .toList();
  }

  @override
  Future<Restaurant?> getRestaurantById(String id) async {
    final response = await ApiClient.get('/restaurants/$id');
    final data = ApiClient.handleResponse(response);
    return data != null ? Restaurant.fromJson(data) : null;
  }

  @override
  Future<List<Restaurant>> searchRestaurants(String query) async {
    final restaurants = await getRestaurants();
    return restaurants
        .where((r) => 
            r.name.toLowerCase().contains(query.toLowerCase()) ||
            r.cuisine.toLowerCase().contains(query.toLowerCase()))
        .toList();
  }
}
