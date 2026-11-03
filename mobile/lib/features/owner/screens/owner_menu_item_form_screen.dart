import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../../core/theme/app_colors.dart';
import '../../../models/restaurant.dart';
import '../../../widgets/menu_widgets.dart';
import '../providers/owner_provider.dart';

/// Add/edit form for a single dish. Pushed via Navigator from
/// OwnerMenuScreen - not one of the module's bottom-nav tabs.
class OwnerMenuItemFormScreen extends StatefulWidget {
  final MenuItem? item;
  final String? initialCategory;

  const OwnerMenuItemFormScreen({super.key, this.item, this.initialCategory});

  @override
  State<OwnerMenuItemFormScreen> createState() => _OwnerMenuItemFormScreenState();
}

class _OwnerMenuItemFormScreenState extends State<OwnerMenuItemFormScreen> {
  final _formKey = GlobalKey<FormState>();
  late final TextEditingController _nameController;
  late final TextEditingController _descriptionController;
  late final TextEditingController _imageController;
  late final TextEditingController _priceController;
  late String _category;
  final List<String> _addedCategories = [];
  late bool _available;
  bool _isSaving = false;

  bool get _isEditing => widget.item != null;

  @override
  void initState() {
    super.initState();
    final item = widget.item;
    _nameController = TextEditingController(text: item?.name ?? '');
    _descriptionController = TextEditingController(text: item?.description ?? '');
    _imageController = TextEditingController(text: item?.image ?? '');
    _priceController = TextEditingController(text: item != null ? item.price.toString() : '');
    _category = item?.category ?? widget.initialCategory ?? 'MAIN_COURSES';
    _available = item?.available ?? true;
    _imageController.addListener(() => setState(() {}));
  }

  @override
  void dispose() {
    _nameController.dispose();
    _descriptionController.dispose();
    _imageController.dispose();
    _priceController.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    if (!(_formKey.currentState?.validate() ?? false)) return;

    setState(() {
      _isSaving = true;
    });

    final item = MenuItem(
      id: widget.item?.id ?? '',
      restaurantId: widget.item?.restaurantId ?? '',
      category: _category,
      name: _nameController.text.trim(),
      description: _descriptionController.text.trim(),
      price: double.tryParse(_priceController.text.trim()) ?? 0,
      image: _imageController.text.trim(),
      available: _available,
    );

    try {
      final ownerProvider = context.read<OwnerProvider>();
      if (_isEditing) {
        await ownerProvider.editMenuItem(widget.item!.id, item);
      } else {
        await ownerProvider.addMenuItem(item);
      }

      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(_isEditing ? 'Dish updated' : 'Dish added to your menu'),
            backgroundColor: AppColors.success,
          ),
        );
        Navigator.of(context).pop();
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text(e.toString().replaceAll('Exception: ', ''))),
        );
      }
    } finally {
      if (mounted) {
        setState(() {
          _isSaving = false;
        });
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColors.background,
      appBar: AppBar(
        title: Text(_isEditing ? 'Edit dish' : 'New dish'),
      ),
      body: Center(
        child: ConstrainedBox(
          constraints: const BoxConstraints(maxWidth: 640),
          child: SingleChildScrollView(
            padding: const EdgeInsets.all(20),
            child: Form(
              key: _formKey,
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  _buildImagePreview(),
                  const SizedBox(height: 14),
                  TextFormField(
                    controller: _imageController,
                    decoration: const InputDecoration(
                      labelText: 'Photo URL',
                      hintText: 'https://...',
                      prefixIcon: Icon(Icons.image_outlined),
                    ),
                    keyboardType: TextInputType.url,
                  ),
                  const SizedBox(height: 24),
                  _sectionLabel('Category'),
                  const SizedBox(height: 10),
                  Wrap(
                    spacing: 10,
                    runSpacing: 10,
                    children: [
                      for (final key in _availableCategories()) _categoryChip(key),
                      _newCategoryChip(),
                    ],
                  ),
                  const SizedBox(height: 6),
                  const Text(
                    'Pick a category or create your own - you are not limited to the list.',
                    style: TextStyle(fontSize: 12, color: AppColors.textSecondary),
                  ),
                  const SizedBox(height: 24),
                  _sectionLabel('Details'),
                  const SizedBox(height: 10),
                  TextFormField(
                    controller: _nameController,
                    textCapitalization: TextCapitalization.sentences,
                    decoration: const InputDecoration(
                      labelText: 'Dish name',
                      prefixIcon: Icon(Icons.fastfood_outlined),
                    ),
                    validator: (v) => (v == null || v.trim().isEmpty) ? 'Name is required' : null,
                  ),
                  const SizedBox(height: 14),
                  TextFormField(
                    controller: _descriptionController,
                    maxLines: 3,
                    decoration: const InputDecoration(
                      labelText: 'Description',
                      alignLabelWithHint: true,
                    ),
                    validator: (v) => (v == null || v.trim().isEmpty) ? 'Description is required' : null,
                  ),
                  const SizedBox(height: 14),
                  TextFormField(
                    controller: _priceController,
                    decoration: const InputDecoration(
                      labelText: 'Price',
                      prefixIcon: Icon(Icons.attach_money),
                    ),
                    keyboardType: const TextInputType.numberWithOptions(decimal: true),
                    validator: (v) {
                      if (v == null || v.trim().isEmpty) return 'Price is required';
                      final parsed = double.tryParse(v.trim());
                      if (parsed == null || parsed < 0) return 'Enter a valid price';
                      return null;
                    },
                  ),
                  const SizedBox(height: 20),
                  Container(
                    padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 6),
                    decoration: BoxDecoration(
                      color: AppColors.surface,
                      borderRadius: BorderRadius.circular(14),
                      border: Border.all(color: AppColors.border),
                    ),
                    child: SwitchListTile(
                      contentPadding: EdgeInsets.zero,
                      title: const Text(
                        'Available to customers',
                        style: TextStyle(fontWeight: FontWeight.w600, color: AppColors.textPrimary),
                      ),
                      subtitle: Text(
                        _available
                            ? 'Shown on the menu and can be pre-ordered'
                            : 'Marked as unavailable on the menu',
                      ),
                      value: _available,
                      activeColor: AppColors.success,
                      onChanged: (value) => setState(() => _available = value),
                    ),
                  ),
                  const SizedBox(height: 28),
                  SizedBox(
                    width: double.infinity,
                    height: 54,
                    child: ElevatedButton.icon(
                      onPressed: _isSaving ? null : _submit,
                      icon: _isSaving
                          ? const SizedBox(
                              height: 20,
                              width: 20,
                              child: CircularProgressIndicator(strokeWidth: 2, color: AppColors.surface),
                            )
                          : Icon(_isEditing ? Icons.check : Icons.add),
                      label: Text(_isEditing ? 'Save changes' : 'Add to menu'),
                    ),
                  ),
                  const SizedBox(height: 20),
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }

  Widget _sectionLabel(String text) {
    return Text(
      text,
      style: const TextStyle(fontSize: 15, fontWeight: FontWeight.bold, color: AppColors.textPrimary),
    );
  }

  Widget _buildImagePreview() {
    final url = _imageController.text.trim();
    final placeholder = Container(
      decoration: BoxDecoration(
        gradient: LinearGradient(
          colors: [AppColors.accentLight.withOpacity(0.7), AppColors.accent.withOpacity(0.5)],
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
        ),
      ),
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Icon(menuCategoryIcon(_category), size: 44, color: Colors.white),
          const SizedBox(height: 8),
          const Text(
            'Paste a photo URL below to preview it',
            style: TextStyle(color: Colors.white, fontSize: 12.5, fontWeight: FontWeight.w600),
          ),
        ],
      ),
    );

    return ClipRRect(
      borderRadius: BorderRadius.circular(20),
      child: SizedBox(
        height: 190,
        width: double.infinity,
        child: url.isEmpty
            ? placeholder
            : Image.network(
                url,
                fit: BoxFit.cover,
                errorBuilder: (context, error, stackTrace) => placeholder,
              ),
      ),
    );
  }

  /// Built-in categories, then the custom ones this restaurant already uses, then
  /// any created in this form (including the current value if it is custom).
  List<String> _availableCategories() {
    final existing = context.read<OwnerProvider>().menuItems.map((i) => i.category);
    final custom = customMenuCategories([...existing, ..._addedCategories, _category]);
    return [...menuCategoryOptions.map((o) => o.key), ...custom];
  }

  Future<void> _createCategory() async {
    final controller = TextEditingController();
    final created = await showDialog<String>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('New category'),
        content: TextField(
          controller: controller,
          autofocus: true,
          maxLength: 40,
          textCapitalization: TextCapitalization.sentences,
          decoration: const InputDecoration(
            hintText: 'e.g. Pizzas, Burgers, Vegan, Kids menu',
          ),
          onSubmitted: (value) => Navigator.pop(context, value),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: const Text('Cancel'),
          ),
          ElevatedButton(
            onPressed: () => Navigator.pop(context, controller.text),
            child: const Text('Use this category'),
          ),
        ],
      ),
    );
    controller.dispose();

    final name = created?.trim().replaceAll(RegExp(r'\s+'), ' ') ?? '';
    if (name.isEmpty) return;

    // Typing a built-in name ("main courses") selects that one instead of duplicating it.
    final asKey = name.toUpperCase().replaceAll(' ', '_');
    setState(() {
      if (isBuiltInMenuCategory(asKey)) {
        _category = asKey;
      } else {
        final existing = _availableCategories().where((c) => c.toLowerCase() == name.toLowerCase());
        if (existing.isNotEmpty) {
          _category = existing.first;
        } else {
          _addedCategories.add(name);
          _category = name;
        }
      }
    });
  }

  Widget _newCategoryChip() {
    return Material(
      color: AppColors.accent.withOpacity(0.12),
      borderRadius: BorderRadius.circular(24),
      child: InkWell(
        onTap: _createCategory,
        borderRadius: BorderRadius.circular(24),
        child: Container(
          padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 10),
          decoration: BoxDecoration(
            borderRadius: BorderRadius.circular(24),
            border: Border.all(color: AppColors.accent),
          ),
          child: const Row(
            mainAxisSize: MainAxisSize.min,
            children: [
              Icon(Icons.add, size: 16, color: AppColors.accent),
              SizedBox(width: 6),
              Text(
                'New category',
                style: TextStyle(fontSize: 13, fontWeight: FontWeight.w700, color: AppColors.accent),
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _categoryChip(String key) {
    final isSelected = _category == key;
    final option = menuCategoryOf(key);

    return Material(
      color: isSelected ? AppColors.primary : AppColors.surface,
      borderRadius: BorderRadius.circular(24),
      child: InkWell(
        onTap: () => setState(() => _category = key),
        borderRadius: BorderRadius.circular(24),
        child: Container(
          padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 10),
          decoration: BoxDecoration(
            borderRadius: BorderRadius.circular(24),
            border: Border.all(color: isSelected ? AppColors.primary : AppColors.border),
          ),
          child: Row(
            mainAxisSize: MainAxisSize.min,
            children: [
              Icon(
                option.icon,
                size: 16,
                color: isSelected ? AppColors.accentLight : AppColors.textSecondary,
              ),
              const SizedBox(width: 8),
              Text(
                option.label,
                style: TextStyle(
                  fontSize: 13,
                  fontWeight: FontWeight.w600,
                  color: isSelected ? Colors.white : AppColors.textPrimary,
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
