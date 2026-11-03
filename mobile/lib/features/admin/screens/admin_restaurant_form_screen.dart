import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../../core/theme/app_colors.dart';
import '../../../models/admin_user.dart';
import '../../../models/restaurant.dart';
import '../providers/admin_provider.dart';

const _priceRanges = <String, String>{
  'BUDGET': 'Budget (\$)',
  'MODERATE': 'Moderate (\$\$)',
  'EXPENSIVE': 'Expensive (\$\$\$)',
  'LUXURY': 'Luxury (\$\$\$\$)',
};

enum _OwnerMode { none, newAccount, existing }

class AdminRestaurantFormScreen extends StatefulWidget {
  final Restaurant? restaurant;

  const AdminRestaurantFormScreen({super.key, this.restaurant});

  @override
  State<AdminRestaurantFormScreen> createState() => _AdminRestaurantFormScreenState();
}

class _AdminRestaurantFormScreenState extends State<AdminRestaurantFormScreen> {
  final _formKey = GlobalKey<FormState>();
  late final TextEditingController _nameController;
  late final TextEditingController _cuisineController;
  late final TextEditingController _descriptionController;
  late final TextEditingController _imageController;
  late final TextEditingController _addressController;
  late final TextEditingController _phoneController;
  late final TextEditingController _openingHoursController;
  late final TextEditingController _featuresController;

  // Owner assignment (only when creating)
  final _ownerNameController = TextEditingController();
  final _ownerEmailController = TextEditingController();
  final _ownerPasswordController = TextEditingController();
  final _ownerPhoneController = TextEditingController();
  _OwnerMode _ownerMode = _OwnerMode.none;
  List<AdminUser> _clients = [];
  bool _loadingClients = false;
  String? _existingOwnerId;
  bool _obscureOwnerPassword = true;

  bool _isOpen = true;
  String _priceRange = 'MODERATE';
  bool _isSaving = false;
  bool _isLoadingDetails = false;

  bool get _isEditing => widget.restaurant != null;

  @override
  void initState() {
    super.initState();
    final restaurant = widget.restaurant;
    _nameController = TextEditingController(text: restaurant?.name ?? '');
    _cuisineController = TextEditingController(text: restaurant?.cuisine ?? '');
    _descriptionController = TextEditingController(text: restaurant?.description ?? '');
    _imageController = TextEditingController(text: restaurant?.image ?? '');
    _addressController = TextEditingController(text: restaurant?.address ?? '');
    _phoneController = TextEditingController(text: restaurant?.phone ?? '');
    _openingHoursController = TextEditingController(text: restaurant?.openingHours ?? '');
    _featuresController = TextEditingController();
    _isOpen = restaurant?.isOpen ?? true;

    if (restaurant != null) {
      _loadRawDetails(restaurant.id);
    }
  }

  // The shared Restaurant model doesn't carry `features`/`priceRange`
  // (other parts of the app don't need them), so fetch the raw JSON to
  // prefill those two fields when editing.
  Future<void> _loadRawDetails(String id) async {
    setState(() => _isLoadingDetails = true);
    try {
      final raw = await context.read<AdminProvider>().getRestaurantRaw(id);
      if (!mounted) return;
      final features = raw['features'];
      setState(() {
        if (features is List) {
          _featuresController.text = features.map((f) => f.toString()).join(', ');
        }
        final priceRange = raw['priceRange'];
        if (priceRange is String && _priceRanges.containsKey(priceRange)) {
          _priceRange = priceRange;
        }
      });
    } catch (_) {
      // Keep defaults - the rest of the form still works without this.
    } finally {
      if (mounted) setState(() => _isLoadingDetails = false);
    }
  }

  @override
  void dispose() {
    _nameController.dispose();
    _cuisineController.dispose();
    _descriptionController.dispose();
    _imageController.dispose();
    _addressController.dispose();
    _phoneController.dispose();
    _openingHoursController.dispose();
    _featuresController.dispose();
    _ownerNameController.dispose();
    _ownerEmailController.dispose();
    _ownerPasswordController.dispose();
    _ownerPhoneController.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    if (!(_formKey.currentState?.validate() ?? false)) return;

    setState(() => _isSaving = true);

    final features = _featuresController.text
        .split(',')
        .map((f) => f.trim())
        .where((f) => f.isNotEmpty)
        .toList();

    final body = {
      'name': _nameController.text.trim(),
      'cuisine': _cuisineController.text.trim(),
      'description': _descriptionController.text.trim(),
      'image': _imageController.text.trim(),
      'address': _addressController.text.trim(),
      'phone': _phoneController.text.trim(),
      'openingHours': _openingHoursController.text.trim(),
      'isOpen': _isOpen,
      'features': features,
      'priceRange': _priceRange,
    };

    try {
      final admin = context.read<AdminProvider>();
      if (_isEditing) {
        await admin.updateRestaurant(widget.restaurant!.id, body);
      } else {
        if (_ownerMode == _OwnerMode.newAccount) {
          await admin.createRestaurantWithOwner(body, newOwner: {
            'name': _ownerNameController.text.trim(),
            'email': _ownerEmailController.text.trim(),
            'password': _ownerPasswordController.text,
            'phone': _ownerPhoneController.text.trim(),
          });
        } else if (_ownerMode == _OwnerMode.existing) {
          await admin.createRestaurantWithOwner(body, existingOwnerId: _existingOwnerId);
        } else {
          await admin.createRestaurant(body);
        }
      }
      if (mounted) {
        Navigator.of(context).pop(true);
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(_isEditing
                ? 'Restaurant updated'
                : _ownerMode == _OwnerMode.none
                    ? 'Restaurant created'
                    : 'Restaurant created and owner assigned'),
            backgroundColor: AppColors.success,
          ),
        );
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text(e.toString().replaceAll('Exception: ', ''))),
        );
      }
    } finally {
      if (mounted) setState(() => _isSaving = false);
    }
  }

  Future<void> _loadClients() async {
    setState(() => _loadingClients = true);
    try {
      final clients = await context.read<AdminProvider>().getAssignableUsers();
      if (mounted) setState(() => _clients = clients);
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text(e.toString().replaceAll('Exception: ', ''))),
        );
      }
    } finally {
      if (mounted) setState(() => _loadingClients = false);
    }
  }

  Widget _buildOwnerSection() {
    return Container(
      margin: const EdgeInsets.only(top: 8, bottom: 8),
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: AppColors.surface,
        borderRadius: BorderRadius.circular(18),
        border: Border.all(color: AppColors.border),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Row(
            children: [
              Icon(Icons.manage_accounts_outlined, color: AppColors.accent),
              SizedBox(width: 10),
              Text('Restaurant owner',
                  style: TextStyle(fontSize: 17, fontWeight: FontWeight.w800, color: AppColors.textPrimary)),
            ],
          ),
          const SizedBox(height: 4),
          const Text(
            'Give the owner access now, or assign someone later from the restaurant list.',
            style: TextStyle(fontSize: 13, color: AppColors.textSecondary),
          ),
          const SizedBox(height: 14),
          SegmentedButton<_OwnerMode>(
            showSelectedIcon: false,
            segments: const [
              ButtonSegment(value: _OwnerMode.none, label: Text('Later')),
              ButtonSegment(value: _OwnerMode.newAccount, label: Text('New account')),
              ButtonSegment(value: _OwnerMode.existing, label: Text('Existing')),
            ],
            selected: {_ownerMode},
            onSelectionChanged: (value) {
              setState(() => _ownerMode = value.first);
              if (_ownerMode == _OwnerMode.existing && _clients.isEmpty) _loadClients();
            },
          ),
          if (_ownerMode == _OwnerMode.newAccount) ...[
            const SizedBox(height: 16),
            TextFormField(
              controller: _ownerNameController,
              decoration: const InputDecoration(labelText: 'Owner name', prefixIcon: Icon(Icons.person_outline)),
              validator: _requiredValidator,
            ),
            const SizedBox(height: 14),
            TextFormField(
              controller: _ownerEmailController,
              keyboardType: TextInputType.emailAddress,
              decoration: const InputDecoration(
                labelText: 'Owner email (login)',
                prefixIcon: Icon(Icons.email_outlined),
              ),
              validator: (v) {
                if (v == null || v.trim().isEmpty) return 'This field is required';
                return RegExp(r'^[^@\s]+@[^@\s]+\.[^@\s]+$').hasMatch(v.trim()) ? null : 'Enter a valid email';
              },
            ),
            const SizedBox(height: 14),
            TextFormField(
              controller: _ownerPasswordController,
              obscureText: _obscureOwnerPassword,
              decoration: InputDecoration(
                labelText: 'Owner password',
                prefixIcon: const Icon(Icons.lock_outline),
                suffixIcon: IconButton(
                  icon: Icon(_obscureOwnerPassword ? Icons.visibility_outlined : Icons.visibility_off_outlined),
                  onPressed: () => setState(() => _obscureOwnerPassword = !_obscureOwnerPassword),
                ),
              ),
              validator: (v) => (v == null || v.length < 6) ? 'At least 6 characters' : null,
            ),
            const SizedBox(height: 14),
            TextFormField(
              controller: _ownerPhoneController,
              keyboardType: TextInputType.phone,
              decoration: const InputDecoration(
                labelText: 'Owner phone (optional)',
                prefixIcon: Icon(Icons.phone_outlined),
              ),
            ),
          ],
          if (_ownerMode == _OwnerMode.existing) ...[
            const SizedBox(height: 16),
            if (_loadingClients)
              const LinearProgressIndicator()
            else if (_clients.isEmpty)
              const Text('No client account available to promote.',
                  style: TextStyle(color: AppColors.textSecondary))
            else
              DropdownButtonFormField<String>(
                initialValue: _existingOwnerId,
                isExpanded: true,
                decoration: const InputDecoration(labelText: 'Choose an account'),
                items: _clients
                    .map((u) => DropdownMenuItem(
                          value: u.id,
                          child: Text('${u.name} • ${u.email}', overflow: TextOverflow.ellipsis),
                        ))
                    .toList(),
                onChanged: (v) => setState(() => _existingOwnerId = v),
                validator: (v) => v == null ? 'Choose an account' : null,
              ),
          ],
        ],
      ),
    );
  }

  String? _requiredValidator(String? value) {
    if (value == null || value.trim().isEmpty) return 'This field is required';
    return null;
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColors.background,
      appBar: AppBar(
        title: Text(_isEditing ? 'Edit Restaurant' : 'New Restaurant'),
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(20),
        child: Form(
          key: _formKey,
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              if (_isLoadingDetails)
                const Padding(
                  padding: EdgeInsets.only(bottom: 16),
                  child: LinearProgressIndicator(),
                ),
              TextFormField(
                controller: _nameController,
                decoration: const InputDecoration(labelText: 'Name'),
                validator: _requiredValidator,
              ),
              const SizedBox(height: 16),
              TextFormField(
                controller: _cuisineController,
                decoration: const InputDecoration(labelText: 'Cuisine'),
                validator: _requiredValidator,
              ),
              const SizedBox(height: 16),
              TextFormField(
                controller: _descriptionController,
                decoration: const InputDecoration(labelText: 'Description', alignLabelWithHint: true),
                maxLines: 4,
                validator: _requiredValidator,
              ),
              const SizedBox(height: 16),
              TextFormField(
                controller: _imageController,
                decoration: const InputDecoration(labelText: 'Image URL'),
                validator: _requiredValidator,
              ),
              const SizedBox(height: 16),
              TextFormField(
                controller: _addressController,
                decoration: const InputDecoration(labelText: 'Address'),
                validator: _requiredValidator,
              ),
              const SizedBox(height: 16),
              TextFormField(
                controller: _phoneController,
                decoration: const InputDecoration(labelText: 'Phone'),
                keyboardType: TextInputType.phone,
                validator: _requiredValidator,
              ),
              const SizedBox(height: 16),
              TextFormField(
                controller: _openingHoursController,
                decoration: const InputDecoration(
                  labelText: 'Opening Hours',
                  hintText: 'e.g. Mon-Sun 9:00 AM - 10:00 PM',
                ),
                validator: _requiredValidator,
              ),
              const SizedBox(height: 16),
              TextFormField(
                controller: _featuresController,
                decoration: const InputDecoration(
                  labelText: 'Features',
                  hintText: 'Comma-separated, e.g. Outdoor Seating, WiFi, Parking',
                ),
              ),
              const SizedBox(height: 16),
              DropdownButtonFormField<String>(
                initialValue: _priceRange,
                decoration: const InputDecoration(labelText: 'Price Range'),
                items: _priceRanges.entries
                    .map((entry) => DropdownMenuItem(value: entry.key, child: Text(entry.value)))
                    .toList(),
                onChanged: (value) {
                  if (value != null) setState(() => _priceRange = value);
                },
              ),
              const SizedBox(height: 16),
              SwitchListTile(
                contentPadding: EdgeInsets.zero,
                title: const Text('Currently Open'),
                value: _isOpen,
                onChanged: (value) => setState(() => _isOpen = value),
                activeColor: AppColors.accent,
              ),
              if (!_isEditing) _buildOwnerSection(),
              const SizedBox(height: 24),
              SizedBox(
                width: double.infinity,
                height: 52,
                child: ElevatedButton(
                  onPressed: _isSaving ? null : _submit,
                  child: _isSaving
                      ? const SizedBox(
                          height: 20,
                          width: 20,
                          child: CircularProgressIndicator(strokeWidth: 2, color: AppColors.surface),
                        )
                      : Text(_isEditing ? 'Save Changes' : 'Create Restaurant'),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
