import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../../core/theme/app_colors.dart';
import '../../../models/restaurant.dart';
import '../../../widgets/menu_widgets.dart';
import '../providers/owner_provider.dart';
import 'owner_menu_item_form_screen.dart';

class OwnerMenuScreen extends StatefulWidget {
  const OwnerMenuScreen({super.key});

  @override
  State<OwnerMenuScreen> createState() => _OwnerMenuScreenState();
}

class _OwnerMenuScreenState extends State<OwnerMenuScreen> {
  String? _selected;

  @override
  void initState() {
    super.initState();
    context.read<OwnerProvider>().loadMenuItems();
  }

  Future<void> _reload() => context.read<OwnerProvider>().loadMenuItems();

  Future<void> _openForm({MenuItem? item}) async {
    await Navigator.of(context).push(
      MaterialPageRoute(
        builder: (_) => OwnerMenuItemFormScreen(item: item, initialCategory: _selected),
      ),
    );
  }

  void _showError(Object e) {
    if (!mounted) return;
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(content: Text(e.toString().replaceAll('Exception: ', ''))),
    );
  }

  Future<void> _toggleAvailable(MenuItem item) async {
    try {
      await context.read<OwnerProvider>().editMenuItem(
            item.id,
            MenuItem(
              id: item.id,
              restaurantId: item.restaurantId,
              category: item.category,
              name: item.name,
              description: item.description,
              price: item.price,
              image: item.image,
              available: !item.available,
            ),
          );
    } catch (e) {
      _showError(e);
    }
  }

  Future<void> _deleteItem(MenuItem item) async {
    final confirm = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Delete dish'),
        content: Text('Remove "${item.name}" from your menu?'),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context, false),
            child: const Text('Cancel'),
          ),
          ElevatedButton(
            onPressed: () => Navigator.pop(context, true),
            style: ElevatedButton.styleFrom(backgroundColor: AppColors.error),
            child: const Text('Delete'),
          ),
        ],
      ),
    );

    if (confirm != true) return;

    try {
      await context.read<OwnerProvider>().removeMenuItem(item.id);
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Dish deleted'), backgroundColor: AppColors.success),
        );
      }
    } catch (e) {
      _showError(e);
    }
  }

  @override
  Widget build(BuildContext context) {
    final ownerProvider = context.watch<OwnerProvider>();
    final items = ownerProvider.menuItems;

    // If the selected category no longer has dishes (e.g. last one deleted), fall back to All.
    final selected = (_selected != null && items.any((i) => i.category == _selected)) ? _selected : null;

    return Scaffold(
      backgroundColor: AppColors.background,
      appBar: AppBar(
        title: const Text('Menu'),
      ),
      floatingActionButton: FloatingActionButton.extended(
        onPressed: () => _openForm(),
        backgroundColor: AppColors.primary,
        foregroundColor: Colors.white,
        icon: const Icon(Icons.add),
        label: const Text('Add dish'),
      ),
      body: ownerProvider.isLoadingMenu && items.isEmpty
          ? const Center(child: CircularProgressIndicator())
          : items.isEmpty
              ? _buildEmptyState()
              : RefreshIndicator(
                  onRefresh: _reload,
                  child: ListView(
                    padding: const EdgeInsets.fromLTRB(20, 16, 20, 100),
                    children: [
                      _buildSummary(items),
                      const SizedBox(height: 16),
                      MenuCategoryBar(
                        items: items,
                        selected: selected,
                        onSelected: (category) => setState(() => _selected = category),
                      ),
                      const SizedBox(height: 18),
                      for (final category in orderedMenuCategories(items))
                        ..._buildCategorySection(category, items, selected),
                    ],
                  ),
                ),
    );
  }

  Widget _buildSummary(List<MenuItem> items) {
    final unavailable = items.where((i) => !i.available).length;

    return DashboardSummaryBanner(
      total: items.length,
      unavailable: unavailable,
    );
  }

  Widget _buildEmptyState() {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(32),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Container(
              padding: const EdgeInsets.all(22),
              decoration: BoxDecoration(
                color: AppColors.accent.withOpacity(0.12),
                shape: BoxShape.circle,
              ),
              child: const Icon(Icons.restaurant_menu, size: 44, color: AppColors.accent),
            ),
            const SizedBox(height: 18),
            const Text(
              'Your menu is empty',
              style: TextStyle(fontSize: 20, fontWeight: FontWeight.bold, color: AppColors.textPrimary),
            ),
            const SizedBox(height: 8),
            const Text(
              'Add your first dish with a photo, a price and a category - customers will see it right away.',
              textAlign: TextAlign.center,
              style: TextStyle(fontSize: 14, height: 1.4, color: AppColors.textSecondary),
            ),
            const SizedBox(height: 22),
            ElevatedButton.icon(
              onPressed: () => _openForm(),
              icon: const Icon(Icons.add),
              label: const Text('Add your first dish'),
            ),
          ],
        ),
      ),
    );
  }

  List<Widget> _buildCategorySection(
    String category,
    List<MenuItem> allItems,
    String? selected,
  ) {
    if (selected != null && selected != category) return [];
    final items = allItems.where((item) => item.category == category).toList();
    if (items.isEmpty) return [];

    return [
      Padding(
        padding: const EdgeInsets.only(bottom: 12, top: 4),
        child: Row(
          children: [
            Container(
              padding: const EdgeInsets.all(7),
              decoration: BoxDecoration(
                color: AppColors.primary,
                borderRadius: BorderRadius.circular(10),
              ),
              child: Icon(menuCategoryIcon(category), size: 16, color: AppColors.accentLight),
            ),
            const SizedBox(width: 10),
            Text(
              menuCategoryOf(category).label,
              style: const TextStyle(fontSize: 18, fontWeight: FontWeight.bold, color: AppColors.textPrimary),
            ),
          ],
        ),
      ),
      MenuItemList(
        items: items,
        cardBuilder: (item) => MenuItemCard(
          item: item,
          onTap: () => _openForm(item: item),
          actions: _buildActions(item),
        ),
      ),
      const SizedBox(height: 22),
    ];
  }

  Widget _buildActions(MenuItem item) {
    return Column(
      mainAxisSize: MainAxisSize.min,
      children: [
        PopupMenuButton<String>(
          icon: const Icon(Icons.more_vert, color: AppColors.textSecondary),
          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
          onSelected: (value) {
            if (value == 'edit') _openForm(item: item);
            if (value == 'delete') _deleteItem(item);
          },
          itemBuilder: (context) => const [
            PopupMenuItem(
              value: 'edit',
              child: Row(children: [Icon(Icons.edit_outlined, size: 18), SizedBox(width: 10), Text('Edit')]),
            ),
            PopupMenuItem(
              value: 'delete',
              child: Row(
                children: [
                  Icon(Icons.delete_outline, size: 18, color: AppColors.error),
                  SizedBox(width: 10),
                  Text('Delete', style: TextStyle(color: AppColors.error)),
                ],
              ),
            ),
          ],
        ),
        Tooltip(
          message: item.available ? 'Available - tap to hide' : 'Hidden - tap to make available',
          child: Switch(
            value: item.available,
            activeColor: AppColors.success,
            onChanged: (_) => _toggleAvailable(item),
          ),
        ),
      ],
    );
  }
}

/// Small stats banner shown above the owner's menu.
class DashboardSummaryBanner extends StatelessWidget {
  final int total;
  final int unavailable;

  const DashboardSummaryBanner({super.key, required this.total, required this.unavailable});

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(18),
      decoration: BoxDecoration(
        gradient: const LinearGradient(
          colors: [AppColors.primary, AppColors.primaryDark],
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
        ),
        borderRadius: BorderRadius.circular(18),
      ),
      child: Row(
        children: [
          Container(
            padding: const EdgeInsets.all(12),
            decoration: BoxDecoration(
              color: Colors.white.withOpacity(0.12),
              borderRadius: BorderRadius.circular(14),
            ),
            child: const Icon(Icons.restaurant_menu, color: AppColors.accentLight, size: 26),
          ),
          const SizedBox(width: 14),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  '$total ${total == 1 ? 'dish' : 'dishes'} on your menu',
                  style: const TextStyle(fontSize: 16, fontWeight: FontWeight.bold, color: Colors.white),
                ),
                const SizedBox(height: 3),
                Text(
                  unavailable == 0 ? 'Everything is available' : '$unavailable currently hidden from customers',
                  style: TextStyle(fontSize: 12.5, color: Colors.white.withOpacity(0.75)),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}
