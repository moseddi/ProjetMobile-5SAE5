import '../../core/api/api_client.dart';
import '../../models/restaurant.dart';
import '../interfaces/menu_item_service.dart';

class ApiMenuItemService implements MenuItemService {
  @override
  Future<List<MenuItem>> getMenuItems(String restaurantId) async {
    final response = await ApiClient.get('/menu-items/restaurant/$restaurantId');
    final data = ApiClient.handleResponse(response);

    if (data == null) return [];

    return (data as List).map((json) => MenuItem.fromJson(json)).toList();
  }
}
