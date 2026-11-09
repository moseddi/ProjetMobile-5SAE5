import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../../core/theme/app_colors.dart';
import '../../../models/owner_restaurant.dart';
import '../../../widgets/menu_image_picker.dart';
import '../providers/owner_provider.dart';

class OwnerRestaurantProfileScreen extends StatefulWidget {
  const OwnerRestaurantProfileScreen({super.key});

  @override
  State<OwnerRestaurantProfileScreen> createState() => _OwnerRestaurantProfileScreenState();
}

class _OwnerRestaurantProfileScreenState extends State<OwnerRestaurantProfileScreen> {
  final _formKey = GlobalKey<FormState>();
  final _nameController = TextEditingController();
  final _cuisineController = TextEditingController();
  final _descriptionController = TextEditingController();
  final _imageController = TextEditingController();
  final _addressController = TextEditingController();
  final _phoneController = TextEditingController();
  final _openingHoursController = TextEditingController();
  final _featureController = TextEditingController();

  // Menu picture chosen from the device, stored as a data URL ('' = none).
  String _menuImage = '';

  final List<String> _features = [];
  PriceRange _priceRange = PriceRange.moderate;
  bool _isOpen = true;
  bool _isSaving = false;
  bool _initialized = false;

  @override
  void dispose() {
    _nameController.dispose();
    _cuisineController.dispose();
    _descriptionController.dispose();
    _imageController.dispose();
    _addressController.dispose();
    _phoneController.dispose();
    _openingHoursController.dispose();
    _featureController.dispose();
    super.dispose();
  }

  void _initializeFromRestaurant(OwnerRestaurant restaurant) {
    _nameController.text = restaurant.name;
    _cuisineController.text = restaurant.cuisine;
    _descriptionController.text = restaurant.description;
    _imageController.text = restaurant.image;
    _menuImage = restaurant.menuImage;
    _addressController.text = restaurant.address;
    _phoneController.text = restaurant.phone;
    _openingHoursController.text = restaurant.openingHours;
    _features
      ..clear()
      ..addAll(restaurant.features);
    _priceRange = restaurant.priceRange;
    _isOpen = restaurant.isOpen;
    _initialized = true;
  }

  void _addFeature() {
    final value = _featureController.text.trim();
    if (value.isEmpty) return;
    if (_features.contains(value)) {
      _featureController.clear();
      return;
    }
    setState(() {
      _features.add(value);
      _featureController.clear();
    });
  }

  Future<void> _save(OwnerRestaurant current) async {
    if (!(_formKey.currentState?.validate() ?? false)) return;

    setState(() {
      _isSaving = true;
    });

    final updated = current.copyWith(
      name: _nameController.text.trim(),
      cuisine: _cuisineController.text.trim(),
      description: _descriptionController.text.trim(),
      image: _imageController.text.trim(),
      menuImage: _menuImage,
      address: _addressController.text.trim(),
      phone: _phoneController.text.trim(),
      openingHours: _openingHoursController.text.trim(),
      features: List<String>.from(_features),
      priceRange: _priceRange,
      isOpen: _isOpen,
    );

    try {
      await context.read<OwnerProvider>().updateRestaurant(updated);
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text('Restaurant profile updated'),
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
    final ownerProvider = context.watch<OwnerProvider>();
    final restaurant = ownerProvider.restaurant;

    if (restaurant == null) {
      if (!ownerProvider.isLoadingRestaurant) {
        // No restaurant loaded yet and nothing in flight - kick off a load.
        WidgetsBinding.instance.addPostFrameCallback((_) {
          context.read<OwnerProvider>().loadRestaurant();
        });
      }
      return Scaffold(
        appBar: AppBar(title: const Text('Restaurant Profile')),
        body: const Center(child: CircularProgressIndicator()),
      );
    }

    if (!_initialized) {
      _initializeFromRestaurant(restaurant);
    }

    return Scaffold(
      backgroundColor: AppColors.background,
      appBar: AppBar(
        title: const Text('Restaurant Profile'),
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(20),
        child: Form(
          key: _formKey,
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              TextFormField(
                controller: _nameController,
                decoration: const InputDecoration(labelText: 'Restaurant Name'),
                validator: (v) => (v == null || v.trim().isEmpty) ? 'Name is required' : null,
              ),
              const SizedBox(height: 16),
              TextFormField(
                controller: _cuisineController,
                decoration: const InputDecoration(labelText: 'Cuisine'),
                validator: (v) => (v == null || v.trim().isEmpty) ? 'Cuisine is required' : null,
              ),
              const SizedBox(height: 16),
              TextFormField(
                controller: _descriptionController,
                maxLines: 4,
                decoration: const InputDecoration(
                  labelText: 'Description',
                  alignLabelWithHint: true,
                ),
                validator: (v) => (v == null || v.trim().isEmpty) ? 'Description is required' : null,
              ),
              const SizedBox(height: 16),
              TextFormField(
                controller: _imageController,
                decoration: const InputDecoration(labelText: 'Cover photo URL'),
                keyboardType: TextInputType.url,
              ),
              const SizedBox(height: 16),
              MenuImagePicker(
                value: _menuImage,
                onChanged: (v) => setState(() => _menuImage = v),
              ),
              const SizedBox(height: 16),
              TextFormField(
                controller: _addressController,
                decoration: const InputDecoration(labelText: 'Address'),
                validator: (v) => (v == null || v.trim().isEmpty) ? 'Address is required' : null,
              ),
              const SizedBox(height: 16),
              TextFormField(
                controller: _phoneController,
                decoration: const InputDecoration(labelText: 'Phone'),
                keyboardType: TextInputType.phone,
                validator: (v) => (v == null || v.trim().isEmpty) ? 'Phone is required' : null,
              ),
              const SizedBox(height: 16),
              TextFormField(
                controller: _openingHoursController,
                decoration: const InputDecoration(
                  labelText: 'Opening Hours',
                  hintText: 'e.g. Mon-Sun 11:00 AM - 10:00 PM',
                ),
                validator: (v) => (v == null || v.trim().isEmpty) ? 'Opening hours are required' : null,
              ),
              const SizedBox(height: 24),
              DropdownButtonFormField<PriceRange>(
                value: _priceRange,
                decoration: const InputDecoration(labelText: 'Price Range'),
                items: PriceRange.values
                    .map((p) => DropdownMenuItem(value: p, child: Text(p.label)))
                    .toList(),
                onChanged: (value) {
                  if (value != null) {
                    setState(() {
                      _priceRange = value;
                    });
                  }
                },
              ),
              const SizedBox(height: 16),
              SwitchListTile(
                contentPadding: EdgeInsets.zero,
                title: const Text(
                  'Currently Open',
                  style: TextStyle(fontWeight: FontWeight.w600, color: AppColors.textPrimary),
                ),
                subtitle: const Text('Toggle off when closed for business'),
                value: _isOpen,
                activeColor: AppColors.accent,
                onChanged: (value) {
                  setState(() {
                    _isOpen = value;
                  });
                },
              ),
              const SizedBox(height: 16),
              const Text(
                'Features',
                style: TextStyle(fontSize: 16, fontWeight: FontWeight.w600, color: AppColors.textPrimary),
              ),
              const SizedBox(height: 8),
              Row(
                children: [
                  Expanded(
                    child: TextFormField(
                      controller: _featureController,
                      decoration: const InputDecoration(
                        hintText: 'e.g. Outdoor Seating',
                      ),
                      onFieldSubmitted: (_) => _addFeature(),
                    ),
                  ),
                  const SizedBox(width: 8),
                  OutlinedButton(
                    onPressed: _addFeature,
                    child: const Text('Add'),
                  ),
                ],
              ),
              const SizedBox(height: 12),
              if (_features.isNotEmpty)
                Wrap(
                  spacing: 8,
                  runSpacing: 8,
                  children: _features.map((feature) {
                    return Chip(
                      label: Text(feature),
                      onDeleted: () {
                        setState(() {
                          _features.remove(feature);
                        });
                      },
                    );
                  }).toList(),
                ),
              const SizedBox(height: 32),
              SizedBox(
                width: double.infinity,
                height: 52,
                child: ElevatedButton(
                  onPressed: _isSaving ? null : () => _save(restaurant),
                  child: _isSaving
                      ? const SizedBox(
                          height: 20,
                          width: 20,
                          child: CircularProgressIndicator(strokeWidth: 2, color: AppColors.surface),
                        )
                      : const Text('Save Changes'),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}