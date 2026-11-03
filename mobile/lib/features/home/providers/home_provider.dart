import 'package:flutter/foundation.dart';
import '../../../models/restaurant.dart';
import '../../restaurants/providers/restaurant_provider.dart';

/// Home screen state: the search box over the shared restaurant list.
class HomeProvider with ChangeNotifier {
  final RestaurantProvider _restaurantProvider;

  HomeProvider(this._restaurantProvider) {
    _restaurantProvider.addListener(notifyListeners);
  }

  String _query = '';

  String get query => _query;
  bool get isLoading => _restaurantProvider.isLoading;
  String? get error => _restaurantProvider.error;
  bool get hasRestaurants => _restaurantProvider.restaurants.isNotEmpty;

  /// Restaurants matching the current search (name, cuisine or address).
  List<Restaurant> get restaurants {
    final q = _query.trim().toLowerCase();
    final all = _restaurantProvider.restaurants;
    if (q.isEmpty) return all;

    return all
        .where((r) =>
            r.name.toLowerCase().contains(q) ||
            r.cuisine.toLowerCase().contains(q) ||
            r.address.toLowerCase().contains(q))
        .toList();
  }

  Future<void> load() => _restaurantProvider.loadRestaurants();

  void setQuery(String value) {
    _query = value;
    notifyListeners();
  }

  @override
  void dispose() {
    _restaurantProvider.removeListener(notifyListeners);
    super.dispose();
  }
}
