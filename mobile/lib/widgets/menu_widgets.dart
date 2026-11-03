import 'package:flutter/material.dart';
import '../core/theme/app_colors.dart';
import '../models/restaurant.dart';

class MenuCategoryOption {
  final String key;
  final String label;
  final IconData icon;

  const MenuCategoryOption(this.key, this.label, this.icon);
}

const List<MenuCategoryOption> menuCategoryOptions = [
  MenuCategoryOption('STARTERS', 'Starters', Icons.tapas),
  MenuCategoryOption('MAIN_COURSES', 'Main Courses', Icons.dinner_dining),
  MenuCategoryOption('DESSERTS', 'Desserts', Icons.icecream),
  MenuCategoryOption('DRINKS', 'Drinks', Icons.local_bar),
  MenuCategoryOption('SPECIALS', 'Specials', Icons.auto_awesome),
];

bool isBuiltInMenuCategory(String key) => menuCategoryOptions.any((c) => c.key == key);

/// Resolves a category key to how it is shown. Anything that isn't a built-in
/// key is a category the restaurant owner created: the text itself is the label.
MenuCategoryOption menuCategoryOf(String key) {
  for (final option in menuCategoryOptions) {
    if (option.key == key) return option;
  }
  return MenuCategoryOption(key, key, Icons.restaurant);
}

IconData menuCategoryIcon(String key) => menuCategoryOf(key).icon;

/// The owner-created categories among [keys], alphabetically, without case-duplicates.
List<String> customMenuCategories(Iterable<String> keys) {
  final seen = <String>{};
  final result = <String>[];
  for (final key in keys) {
    if (key.trim().isEmpty || isBuiltInMenuCategory(key)) continue;
    if (seen.add(key.toLowerCase())) result.add(key);
  }
  result.sort((a, b) => a.toLowerCase().compareTo(b.toLowerCase()));
  return result;
}

/// Categories that actually have dishes: built-ins in their usual order, then custom ones.
List<String> orderedMenuCategories(List<MenuItem> items) {
  final present = items.map((i) => i.category).toSet();
  return [
    ...menuCategoryOptions.map((o) => o.key).where(present.contains),
    ...customMenuCategories(present),
  ];
}

/// Horizontally scrolling pill bar: an "All" pill followed by one pill per
/// category that actually has items, each with its item count.
class MenuCategoryBar extends StatelessWidget {
  final List<MenuItem> items;
  final String? selected;
  final ValueChanged<String?> onSelected;

  const MenuCategoryBar({
    super.key,
    required this.items,
    required this.selected,
    required this.onSelected,
  });

  @override
  Widget build(BuildContext context) {
    final categories = orderedMenuCategories(items);

    return SizedBox(
      height: 44,
      child: ListView(
        scrollDirection: Axis.horizontal,
        children: [
          _pill(label: 'All', count: items.length, icon: Icons.restaurant_menu, isSelected: selected == null, onTap: () => onSelected(null)),
          for (final category in categories)
            _pill(
              label: menuCategoryOf(category).label,
              count: items.where((i) => i.category == category).length,
              icon: menuCategoryIcon(category),
              isSelected: selected == category,
              onTap: () => onSelected(category),
            ),
        ],
      ),
    );
  }

  Widget _pill({
    required String label,
    required int count,
    required IconData icon,
    required bool isSelected,
    required VoidCallback onTap,
  }) {
    return Padding(
      padding: const EdgeInsets.only(right: 10),
      child: Material(
        color: isSelected ? AppColors.primary : AppColors.surface,
        borderRadius: BorderRadius.circular(24),
        child: InkWell(
          onTap: onTap,
          borderRadius: BorderRadius.circular(24),
          child: Container(
            padding: const EdgeInsets.symmetric(horizontal: 14),
            decoration: BoxDecoration(
              borderRadius: BorderRadius.circular(24),
              border: Border.all(color: isSelected ? AppColors.primary : AppColors.border),
            ),
            child: Row(
              children: [
                Icon(icon, size: 16, color: isSelected ? AppColors.accentLight : AppColors.textSecondary),
                const SizedBox(width: 6),
                Text(
                  label,
                  style: TextStyle(
                    fontSize: 13,
                    fontWeight: FontWeight.w600,
                    color: isSelected ? Colors.white : AppColors.textPrimary,
                  ),
                ),
                const SizedBox(width: 6),
                Container(
                  padding: const EdgeInsets.symmetric(horizontal: 7, vertical: 2),
                  decoration: BoxDecoration(
                    color: isSelected ? Colors.white.withOpacity(0.18) : AppColors.background,
                    borderRadius: BorderRadius.circular(10),
                  ),
                  child: Text(
                    '$count',
                    style: TextStyle(
                      fontSize: 11,
                      fontWeight: FontWeight.w700,
                      color: isSelected ? Colors.white : AppColors.textSecondary,
                    ),
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}

/// A dish card: big rounded photo, name, description and a price pill. Pass
/// [actions] to add owner controls (edit/delete) on the right.
class MenuItemCard extends StatelessWidget {
  final MenuItem item;
  final Widget? actions;
  final VoidCallback? onTap;

  const MenuItemCard({super.key, required this.item, this.actions, this.onTap});

  @override
  Widget build(BuildContext context) {
    return Material(
      color: AppColors.surface,
      borderRadius: BorderRadius.circular(18),
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(18),
        child: Container(
          padding: const EdgeInsets.all(12),
          decoration: BoxDecoration(
            borderRadius: BorderRadius.circular(18),
            border: Border.all(color: AppColors.border),
            boxShadow: [
              BoxShadow(
                color: Colors.black.withOpacity(0.04),
                blurRadius: 10,
                offset: const Offset(0, 4),
              ),
            ],
          ),
          child: Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              _buildImage(),
              const SizedBox(width: 14),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      item.name,
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: const TextStyle(
                        fontSize: 16,
                        fontWeight: FontWeight.bold,
                        color: AppColors.textPrimary,
                      ),
                    ),
                    const SizedBox(height: 4),
                    Text(
                      item.description,
                      maxLines: 2,
                      overflow: TextOverflow.ellipsis,
                      style: const TextStyle(
                        fontSize: 13,
                        height: 1.35,
                        color: AppColors.textSecondary,
                      ),
                    ),
                    const SizedBox(height: 10),
                    Row(
                      children: [
                        Container(
                          padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 5),
                          decoration: BoxDecoration(
                            color: AppColors.accent.withOpacity(0.14),
                            borderRadius: BorderRadius.circular(20),
                          ),
                          child: Text(
                            '\$${item.price.toStringAsFixed(2)}',
                            style: const TextStyle(
                              fontSize: 14,
                              fontWeight: FontWeight.w800,
                              color: Color(0xFF9A6B3A),
                            ),
                          ),
                        ),
                        if (!item.available) ...[
                          const SizedBox(width: 8),
                          Container(
                            padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                            decoration: BoxDecoration(
                              color: AppColors.error.withOpacity(0.1),
                              borderRadius: BorderRadius.circular(10),
                            ),
                            child: const Text(
                              'Unavailable',
                              style: TextStyle(
                                fontSize: 11,
                                fontWeight: FontWeight.w700,
                                color: AppColors.error,
                              ),
                            ),
                          ),
                        ],
                      ],
                    ),
                  ],
                ),
              ),
              if (actions != null) ...[
                const SizedBox(width: 4),
                actions!,
              ],
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildImage() {
    final fallback = Container(
      width: 96,
      height: 96,
      decoration: BoxDecoration(
        gradient: LinearGradient(
          colors: [AppColors.accentLight.withOpacity(0.7), AppColors.accent.withOpacity(0.5)],
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
        ),
      ),
      child: Icon(menuCategoryIcon(item.category), size: 34, color: Colors.white),
    );

    return ClipRRect(
      borderRadius: BorderRadius.circular(14),
      child: ColorFiltered(
        colorFilter: item.available
            ? const ColorFilter.mode(Colors.transparent, BlendMode.multiply)
            : const ColorFilter.matrix(<double>[
                0.33, 0.33, 0.33, 0, 0,
                0.33, 0.33, 0.33, 0, 0,
                0.33, 0.33, 0.33, 0, 0,
                0, 0, 0, 1, 0,
              ]),
        child: item.image.isEmpty
            ? fallback
            : Image.network(
                item.image,
                width: 96,
                height: 96,
                fit: BoxFit.cover,
                errorBuilder: (context, error, stackTrace) => fallback,
              ),
      ),
    );
  }
}

/// Lays dish cards out in one column on phones and two on wider screens.
class MenuItemList extends StatelessWidget {
  final List<MenuItem> items;
  final Widget Function(MenuItem item) cardBuilder;

  const MenuItemList({super.key, required this.items, required this.cardBuilder});

  @override
  Widget build(BuildContext context) {
    return LayoutBuilder(
      builder: (context, constraints) {
        const gap = 14.0;
        final twoColumns = constraints.maxWidth >= 720;
        final cardWidth = twoColumns ? (constraints.maxWidth - gap) / 2 : constraints.maxWidth;

        return Wrap(
          spacing: gap,
          runSpacing: gap,
          children: [
            for (final item in items) SizedBox(width: cardWidth, child: cardBuilder(item)),
          ],
        );
      },
    );
  }
}

/// The customer-facing menu: title, category pills, and dishes grouped under
/// category headings.
class MenuSection extends StatefulWidget {
  final List<MenuItem> items;

  const MenuSection({super.key, required this.items});

  @override
  State<MenuSection> createState() => _MenuSectionState();
}

class _MenuSectionState extends State<MenuSection> {
  String? _selected;

  @override
  Widget build(BuildContext context) {
    final items = widget.items;

    if (items.isEmpty) {
      return Container(
        width: double.infinity,
        padding: const EdgeInsets.all(28),
        decoration: BoxDecoration(
          color: AppColors.surface,
          borderRadius: BorderRadius.circular(18),
          border: Border.all(color: AppColors.border),
        ),
        child: const Column(
          children: [
            Icon(Icons.restaurant_menu_outlined, size: 44, color: AppColors.textHint),
            SizedBox(height: 10),
            Text(
              'The menu is coming soon',
              style: TextStyle(fontSize: 15, fontWeight: FontWeight.w600, color: AppColors.textSecondary),
            ),
          ],
        ),
      );
    }

    final visibleCategories =
        orderedMenuCategories(items).where((c) => _selected == null || _selected == c).toList();

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          children: [
            const Text(
              'Menu',
              style: TextStyle(fontSize: 24, fontWeight: FontWeight.bold, color: AppColors.textPrimary),
            ),
            const SizedBox(width: 10),
            Text(
              '${items.length} dishes',
              style: const TextStyle(fontSize: 13, color: AppColors.textSecondary),
            ),
          ],
        ),
        const SizedBox(height: 14),
        MenuCategoryBar(
          items: items,
          selected: _selected,
          onSelected: (category) => setState(() => _selected = category),
        ),
        const SizedBox(height: 18),
        for (final category in visibleCategories) ...[
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
            items: items.where((i) => i.category == category).toList(),
            cardBuilder: (item) => MenuItemCard(item: item),
          ),
          const SizedBox(height: 22),
        ],
      ],
    );
  }
}
