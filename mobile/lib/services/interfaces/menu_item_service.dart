import '../../models/restaurant.dart';

abstract class MenuItemService {
  Future<List<MenuItem>> getMenuItems(String restaurantId);
}
