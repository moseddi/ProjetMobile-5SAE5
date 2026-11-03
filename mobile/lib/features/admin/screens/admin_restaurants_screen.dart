import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../../core/theme/app_colors.dart';
import '../../../models/restaurant.dart';
import '../../../widgets/admin_toolbar.dart';
import '../providers/admin_provider.dart';
import 'admin_restaurant_form_screen.dart';
import '../../restaurants/providers/restaurant_provider.dart';

class AdminRestaurantsScreen extends StatefulWidget {
  const AdminRestaurantsScreen({super.key});

  @override
  State<AdminRestaurantsScreen> createState() => _AdminRestaurantsScreenState();
}

class _AdminRestaurantsScreenState extends State<AdminRestaurantsScreen> {
  String _query = '';
  AdminSort _sort = AdminSort.newest;
  String _filter = 'all';

  bool _matches(Restaurant r, String filter) {
    switch (filter) {
      case 'needsOwner':
        return r.ownerId == null;
      case 'hasOwner':
        return r.ownerId != null;
      case 'open':
        return r.isOpen;
      case 'closed':
        return !r.isOpen;
      default:
        return true;
    }
  }

  List<Restaurant> _filtered() {
    final q = _query.trim().toLowerCase();
    final list = _restaurants.where((r) {
      if (!_matches(r, _filter)) return false;
      if (q.isEmpty) return true;
      return r.name.toLowerCase().contains(q) || r.cuisine.toLowerCase().contains(q) || r.address.toLowerCase().contains(q);
    });
    return sortByAdded(list, _sort, (r) => r.id);
  }

  Widget _buildToolbar() {
    return Padding(
      padding: const EdgeInsets.fromLTRB(16, 12, 16, 4),
      child: Column(
        children: [
          AdminSearchBar(
            hint: 'Search name, cuisine or address',
            sort: _sort,
            onQueryChanged: (v) => setState(() => _query = v),
            onSortChanged: (v) => setState(() => _sort = v),
          ),
          const SizedBox(height: 10),
          AdminChips<String>(
            options: const [
              ('All', 'all'),
              ('Needs owner', 'needsOwner'),
              ('Has owner', 'hasOwner'),
              ('Open', 'open'),
              ('Closed', 'closed'),
            ],
            selected: _filter,
            countOf: (v) => _restaurants.where((r) => _matches(r, v)).length,
            onChanged: (v) => setState(() => _filter = v),
          ),
        ],
      ),
    );
  }
  List<Restaurant> get _restaurants => context.read<RestaurantProvider>().restaurants;
  bool get _isLoading => context.read<RestaurantProvider>().isLoading;
  String? get _error => context.read<RestaurantProvider>().error;

  @override
  void initState() {
    super.initState();
    _loadRestaurants();
  }

  Future<void> _loadRestaurants() => context.read<RestaurantProvider>().loadRestaurants();

  Future<void> _openCreate() async {
    final created = await Navigator.of(context).push<bool>(
      MaterialPageRoute(builder: (_) => const AdminRestaurantFormScreen()),
    );
    if (created == true) _loadRestaurants();
  }

  Future<void> _openEdit(Restaurant restaurant) async {
    final updated = await Navigator.of(context).push<bool>(
      MaterialPageRoute(builder: (_) => AdminRestaurantFormScreen(restaurant: restaurant)),
    );
    if (updated == true) _loadRestaurants();
  }

  Future<void> _deleteRestaurant(Restaurant restaurant) async {
    final confirm = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Delete Restaurant'),
        content: Text('Are you sure you want to delete "${restaurant.name}"? This cannot be undone.'),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(context).pop(false),
            child: const Text('Cancel'),
          ),
          ElevatedButton(
            onPressed: () => Navigator.of(context).pop(true),
            style: ElevatedButton.styleFrom(backgroundColor: AppColors.error),
            child: const Text('Delete'),
          ),
        ],
      ),
    );

    if (confirm != true) return;

    try {
      await context.read<AdminProvider>().deleteRestaurant(restaurant.id);
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Restaurant deleted'), backgroundColor: AppColors.success),
        );
      }
      _loadRestaurants();
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text(e.toString().replaceAll('Exception: ', ''))),
        );
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    // Rebuild whenever the shared restaurant list changes.
    context.watch<RestaurantProvider>();
    final items = _filtered();
    return Scaffold(
      backgroundColor: AppColors.background,
      appBar: AppBar(
        title: const Text('Restaurants'),
      ),
      floatingActionButton: FloatingActionButton(
        onPressed: _openCreate,
        child: const Icon(Icons.add),
      ),
      body: Column(
        children: [
          _buildToolbar(),
          Expanded(
            child: RefreshIndicator(
        onRefresh: _loadRestaurants,
        child: _isLoading
            ? const Center(child: CircularProgressIndicator())
            : _error != null
                ? _buildError(_error!)
                : _restaurants.isEmpty
                    ? _buildEmptyState()
                    : items.isEmpty
                        ? ListView(children: [
                            AdminNoResults(onReset: () => setState(() {
                                  _query = '';
                                  _filter = 'all';
                                })),
                          ])
                        : ListView.builder(
                            padding: const EdgeInsets.all(16),
                            itemCount: items.length,
                            itemBuilder: (context, index) => _buildRestaurantCard(items[index]),
                          ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildError(String message) {
    return ListView(
      padding: const EdgeInsets.all(20),
      children: [
        const SizedBox(height: 60),
        const Icon(Icons.error_outline, size: 64, color: AppColors.error),
        const SizedBox(height: 16),
        Center(
          child: Text(
            message.replaceAll('Exception: ', ''),
            textAlign: TextAlign.center,
            style: const TextStyle(color: AppColors.textSecondary),
          ),
        ),
      ],
    );
  }

  Widget _buildEmptyState() {
    return ListView(
      padding: const EdgeInsets.all(20),
      children: [
        const SizedBox(height: 60),
        Icon(Icons.restaurant_outlined, size: 80, color: AppColors.textHint),
        const SizedBox(height: 16),
        const Center(
          child: Text(
            'No restaurants yet',
            style: TextStyle(fontSize: 18, fontWeight: FontWeight.w600, color: AppColors.textPrimary),
          ),
        ),
        const SizedBox(height: 8),
        const Center(
          child: Text(
            'Tap + to add the first one',
            style: TextStyle(fontSize: 14, color: AppColors.textSecondary),
          ),
        ),
      ],
    );
  }

  Widget _buildRestaurantCard(Restaurant restaurant) {
    final needsOwner = restaurant.ownerId == null;

    return Card(
      margin: const EdgeInsets.only(bottom: 12),
      child: InkWell(
        borderRadius: BorderRadius.circular(12),
        onTap: () => _openEdit(restaurant),
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Row(
            children: [
              ClipRRect(
                borderRadius: BorderRadius.circular(8),
                child: Image.network(
                  restaurant.image,
                  width: 56,
                  height: 56,
                  fit: BoxFit.cover,
                  errorBuilder: (context, error, stackTrace) => Container(
                    width: 56,
                    height: 56,
                    color: AppColors.border,
                    child: const Icon(Icons.restaurant, color: AppColors.textHint),
                  ),
                ),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      restaurant.name,
                      style: const TextStyle(fontSize: 16, fontWeight: FontWeight.w600, color: AppColors.textPrimary),
                    ),
                    const SizedBox(height: 2),
                    Text(
                      restaurant.cuisine,
                      style: const TextStyle(fontSize: 13, color: AppColors.textSecondary),
                    ),
                    const SizedBox(height: 8),
                    Wrap(
                      spacing: 8,
                      runSpacing: 4,
                      children: [
                        _badge(
                          restaurant.isOpen ? 'Open' : 'Closed',
                          restaurant.isOpen ? AppColors.openStatus : AppColors.closedStatus,
                        ),
                        if (needsOwner)
                          _badge('Needs owner', AppColors.warning, icon: Icons.flag),
                      ],
                    ),
                  ],
                ),
              ),
              IconButton(
                onPressed: () => _deleteRestaurant(restaurant),
                icon: const Icon(Icons.delete_outline, color: AppColors.error),
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _badge(String label, Color color, {IconData? icon}) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
      decoration: BoxDecoration(
        color: color.withOpacity(0.1),
        borderRadius: BorderRadius.circular(12),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          if (icon != null) ...[
            Icon(icon, size: 12, color: color),
            const SizedBox(width: 4),
          ],
          Text(
            label,
            style: TextStyle(fontSize: 11, fontWeight: FontWeight.w600, color: color),
          ),
        ],
      ),
    );
  }
}
