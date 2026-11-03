import 'package:flutter/foundation.dart';
import '../../../models/restaurant.dart';
import '../../../services/interfaces/menu_item_service.dart';
import '../../../services/interfaces/restaurant_service.dart';

/// Restaurants module state: the shared list of restaurants and each restaurant's
/// menu. Everything that needs a restaurant (home, details, reservation, review and
/// hang-out pickers, the admin console) reads it from here instead of calling the
/// service itself.
class RestaurantProvider with ChangeNotifier {
  final RestaurantService _restaurantService;
  final MenuItemService _menuItemService;

  RestaurantProvider(this._restaurantService, this._menuItemService);

  List<Restaurant> _restaurants = [];
  bool _isLoading = false;
  String? _error;

  final Map<String, List<MenuItem>> _menus = {};
  final Set<String> _loadingMenus = {};

  List<Restaurant> get restaurants => _restaurants;
  bool get isLoading => _isLoading;
  String? get error => _error;

  Restaurant? byId(String id) {
    for (final restaurant in _restaurants) {
      if (restaurant.id == id) return restaurant;
    }
    return null;
  }

  Future<void> loadRestaurants() async {
    _isLoading = true;
    _error = null;
    notifyListeners();

    try {
      _restaurants = await _restaurantService.getRestaurants();
    } catch (e) {
      _error = e.toString().replaceAll('Exception: ', '');
    } finally {
      _isLoading = false;
      notifyListeners();
    }
  }

  /// The last loaded menu for a restaurant (empty until [loadMenu] completes).
  List<MenuItem> menuFor(String restaurantId) => _menus[restaurantId] ?? const [];

  bool isMenuLoading(String restaurantId) => _loadingMenus.contains(restaurantId);

  /// Always fetches, so a customer sees dishes the owner just added or hid;
  /// the previous menu stays visible meanwhile.
  Future<List<MenuItem>> loadMenu(String restaurantId) async {
    _loadingMenus.add(restaurantId);
    notifyListeners();

    try {
      _menus[restaurantId] = await _menuItemService.getMenuItems(restaurantId);
    } catch (_) {
      // Keep whatever menu we already had.
    } finally {
      _loadingMenus.remove(restaurantId);
      notifyListeners();
    }
    return menuFor(restaurantId);
  }
}
