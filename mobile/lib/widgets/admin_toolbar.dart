import 'package:flutter/material.dart';
import '../core/theme/app_colors.dart';

/// Order of the lists shown to the admin. "Newest" is the default everywhere.
enum AdminSort { newest, oldest }

/// Orders [items] by when they were added. MongoDB ids start with their creation
/// time, so comparing ids is a reliable "added at" order, even for records
/// without a createdAt.
List<T> sortByAdded<T>(Iterable<T> items, AdminSort sort, String Function(T item) id) {
  final list = items.toList();
  list.sort((a, b) => sort == AdminSort.newest ? id(b).compareTo(id(a)) : id(a).compareTo(id(b)));
  return list;
}

/// Search field + "newest / oldest first" menu, shared by every admin list.
class AdminSearchBar extends StatefulWidget {
  final String hint;
  final AdminSort sort;
  final ValueChanged<String> onQueryChanged;
  final ValueChanged<AdminSort> onSortChanged;

  const AdminSearchBar({
    super.key,
    required this.hint,
    required this.sort,
    required this.onQueryChanged,
    required this.onSortChanged,
  });

  @override
  State<AdminSearchBar> createState() => _AdminSearchBarState();
}

class _AdminSearchBarState extends State<AdminSearchBar> {
  final _controller = TextEditingController();

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Row(
      children: [
        Expanded(
          child: TextField(
            controller: _controller,
            onChanged: (value) {
              widget.onQueryChanged(value);
              setState(() {});
            },
            decoration: InputDecoration(
              hintText: widget.hint,
              prefixIcon: const Icon(Icons.search),
              suffixIcon: _controller.text.isEmpty
                  ? null
                  : IconButton(
                      icon: const Icon(Icons.close),
                      onPressed: () {
                        _controller.clear();
                        widget.onQueryChanged('');
                        setState(() {});
                      },
                    ),
              filled: true,
              fillColor: AppColors.surface,
              contentPadding: const EdgeInsets.symmetric(vertical: 12),
              border: OutlineInputBorder(
                borderRadius: BorderRadius.circular(14),
                borderSide: const BorderSide(color: AppColors.border),
              ),
              enabledBorder: OutlineInputBorder(
                borderRadius: BorderRadius.circular(14),
                borderSide: const BorderSide(color: AppColors.border),
              ),
            ),
          ),
        ),
        const SizedBox(width: 8),
        PopupMenuButton<AdminSort>(
          tooltip: 'Sort',
          initialValue: widget.sort,
          onSelected: widget.onSortChanged,
          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(14)),
          itemBuilder: (context) => const [
            PopupMenuItem(value: AdminSort.newest, child: Text('Newest first')),
            PopupMenuItem(value: AdminSort.oldest, child: Text('Oldest first')),
          ],
          child: Container(
            height: 48,
            padding: const EdgeInsets.symmetric(horizontal: 12),
            decoration: BoxDecoration(
              color: AppColors.surface,
              borderRadius: BorderRadius.circular(14),
              border: Border.all(color: AppColors.border),
            ),
            child: Row(
              mainAxisSize: MainAxisSize.min,
              children: [
                Icon(
                  widget.sort == AdminSort.newest ? Icons.arrow_downward : Icons.arrow_upward,
                  size: 16,
                  color: AppColors.accent,
                ),
                const SizedBox(width: 4),
                Text(
                  widget.sort == AdminSort.newest ? 'Newest' : 'Oldest',
                  style: const TextStyle(fontSize: 13, fontWeight: FontWeight.w700),
                ),
              ],
            ),
          ),
        ),
      ],
    );
  }
}

/// A horizontally scrolling row of single-choice chips with counts.
class AdminChips<T> extends StatelessWidget {
  final List<(String, T)> options;
  final T selected;
  final int Function(T value)? countOf;
  final ValueChanged<T> onChanged;

  const AdminChips({
    super.key,
    required this.options,
    required this.selected,
    required this.onChanged,
    this.countOf,
  });

  @override
  Widget build(BuildContext context) {
    return SingleChildScrollView(
      scrollDirection: Axis.horizontal,
      child: Row(
        children: [
          for (final (label, value) in options)
            Padding(
              padding: const EdgeInsets.only(right: 8),
              child: ChoiceChip(
                label: Text(countOf == null ? label : '$label (${countOf!(value)})'),
                selected: selected == value,
                onSelected: (_) => onChanged(value),
                selectedColor: AppColors.primary,
                backgroundColor: AppColors.surface,
                showCheckmark: false,
                side: const BorderSide(color: AppColors.border),
                labelStyle: TextStyle(
                  fontWeight: FontWeight.w700,
                  color: selected == value ? Colors.white : AppColors.textPrimary,
                ),
              ),
            ),
        ],
      ),
    );
  }
}

/// "No result for these filters" placeholder.
class AdminNoResults extends StatelessWidget {
  final VoidCallback onReset;

  const AdminNoResults({super.key, required this.onReset});

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(top: 60),
      child: Column(
        children: [
          const Icon(Icons.search_off, size: 64, color: AppColors.textHint),
          const SizedBox(height: 12),
          const Text('Nothing matches', style: TextStyle(fontSize: 17, fontWeight: FontWeight.w800)),
          const SizedBox(height: 6),
          const Text('Try other words or clear the filters.',
              style: TextStyle(color: AppColors.textSecondary)),
          const SizedBox(height: 16),
          OutlinedButton(onPressed: onReset, child: const Text('Clear filters')),
        ],
      ),
    );
  }
}
