import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../../core/navigation/app_router.dart';
import '../../../core/theme/app_colors.dart';
import '../providers/auth_provider.dart';

const _priceRanges = <String, String>{
  'BUDGET': 'Budget (\$)',
  'MODERATE': 'Moderate (\$\$)',
  'EXPENSIVE': 'Expensive (\$\$\$)',
  'LUXURY': 'Luxury (\$\$\$\$)',
};

/// Public form: a restaurant owner without an account asks to join. After
/// sending, they wait for an admin to accept (confirmation e-mail comes later).
class OwnerSignupScreen extends StatefulWidget {
  const OwnerSignupScreen({super.key});

  @override
  State<OwnerSignupScreen> createState() => _OwnerSignupScreenState();
}

class _OwnerSignupScreenState extends State<OwnerSignupScreen> {
  final _formKey = GlobalKey<FormState>();
  final _ownerName = TextEditingController();
  final _email = TextEditingController();
  final _phone = TextEditingController();
  final _password = TextEditingController();
  final _confirm = TextEditingController();
  final _restaurantName = TextEditingController();
  final _cuisine = TextEditingController();
  final _description = TextEditingController();
  final _image = TextEditingController();
  final _address = TextEditingController();
  final _restaurantPhone = TextEditingController();
  final _hours = TextEditingController();

  String _priceRange = 'MODERATE';
  bool _obscure = true;
  bool _sent = false;
  String _sentEmail = '';

  @override
  void dispose() {
    for (final c in [
      _ownerName, _email, _phone, _password, _confirm, _restaurantName,
      _cuisine, _description, _image, _address, _restaurantPhone, _hours,
    ]) {
      c.dispose();
    }
    super.dispose();
  }

  String? _required(String? v) => (v == null || v.trim().isEmpty) ? 'This field is required' : null;

  Future<void> _submit() async {
    if (!(_formKey.currentState?.validate() ?? false)) return;

    final error = await context.read<AuthProvider>().submitOwnerApplication({
      'ownerName': _ownerName.text.trim(),
      'email': _email.text.trim(),
      'phone': _phone.text.trim(),
      'password': _password.text,
      'restaurantName': _restaurantName.text.trim(),
      'cuisine': _cuisine.text.trim(),
      'description': _description.text.trim(),
      'image': _image.text.trim(),
      'address': _address.text.trim(),
      'restaurantPhone': _restaurantPhone.text.trim(),
      'openingHours': _hours.text.trim(),
      'priceRange': _priceRange,
    });

    if (!mounted) return;
    if (error != null) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(error), backgroundColor: AppColors.error),
      );
      return;
    }
    setState(() {
      _sent = true;
      _sentEmail = _email.text.trim();
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColors.background,
      appBar: AppBar(title: const Text('Register your restaurant')),
      body: SafeArea(child: _sent ? _buildSuccess() : _buildForm()),
    );
  }

  Widget _buildSuccess() {
    return Center(
      child: SingleChildScrollView(
        padding: const EdgeInsets.all(28),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Container(
              width: 96,
              height: 96,
              decoration: BoxDecoration(
                color: AppColors.warning.withOpacity(0.15),
                shape: BoxShape.circle,
              ),
              child: const Icon(Icons.hourglass_top_rounded, size: 48, color: AppColors.warning),
            ),
            const SizedBox(height: 24),
            const Text(
              'Request sent!',
              style: TextStyle(fontSize: 26, fontWeight: FontWeight.w800, color: AppColors.textPrimary),
            ),
            const SizedBox(height: 12),
            Text(
              'Your restaurant "${_restaurantName.text.trim()}" is waiting for the admin to accept it.\n\n'
              'A confirmation will be sent to $_sentEmail once it is approved. '
              'You will then be able to log in and manage your restaurant.',
              textAlign: TextAlign.center,
              style: const TextStyle(fontSize: 15, height: 1.5, color: AppColors.textSecondary),
            ),
            const SizedBox(height: 32),
            SizedBox(
              width: double.infinity,
              height: 50,
              child: ElevatedButton(
                onPressed: () => Navigator.of(context).pushNamedAndRemoveUntil(AppRouter.login, (r) => false),
                child: const Text('Back to login'),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _section(String title, IconData icon) {
    return Padding(
      padding: const EdgeInsets.only(top: 8, bottom: 14),
      child: Row(
        children: [
          Container(
            padding: const EdgeInsets.all(8),
            decoration: BoxDecoration(
              color: AppColors.accent.withOpacity(0.15),
              borderRadius: BorderRadius.circular(10),
            ),
            child: Icon(icon, size: 20, color: AppColors.accent),
          ),
          const SizedBox(width: 12),
          Text(
            title,
            style: const TextStyle(fontSize: 18, fontWeight: FontWeight.w800, color: AppColors.textPrimary),
          ),
        ],
      ),
    );
  }

  Widget _gap() => const SizedBox(height: 14);

  Widget _buildForm() {
    final loading = context.watch<AuthProvider>().isLoading;

    return SingleChildScrollView(
      padding: const EdgeInsets.all(20),
      child: Form(
        key: _formKey,
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Container(
              padding: const EdgeInsets.all(16),
              decoration: BoxDecoration(
                color: AppColors.info.withOpacity(0.08),
                borderRadius: BorderRadius.circular(14),
                border: Border.all(color: AppColors.info.withOpacity(0.3)),
              ),
              child: const Row(
                children: [
                  Icon(Icons.info_outline, color: AppColors.info),
                  SizedBox(width: 12),
                  Expanded(
                    child: Text(
                      'Fill in your details and your restaurant. An admin will review your request before your account is opened.',
                      style: TextStyle(fontSize: 13.5, height: 1.4, color: AppColors.textSecondary),
                    ),
                  ),
                ],
              ),
            ),
            const SizedBox(height: 22),
            _section('About you', Icons.person_outline),
            TextFormField(
              controller: _ownerName,
              textCapitalization: TextCapitalization.words,
              decoration: const InputDecoration(labelText: 'Your full name', prefixIcon: Icon(Icons.badge_outlined)),
              validator: _required,
            ),
            _gap(),
            TextFormField(
              controller: _email,
              keyboardType: TextInputType.emailAddress,
              decoration: const InputDecoration(labelText: 'Email', prefixIcon: Icon(Icons.email_outlined)),
              validator: (v) {
                if (v == null || v.trim().isEmpty) return 'This field is required';
                final ok = RegExp(r'^[^@\s]+@[^@\s]+\.[^@\s]+$').hasMatch(v.trim());
                return ok ? null : 'Please enter a valid email';
              },
            ),
            _gap(),
            TextFormField(
              controller: _phone,
              keyboardType: TextInputType.phone,
              decoration: const InputDecoration(labelText: 'Your phone', prefixIcon: Icon(Icons.phone_outlined)),
              validator: _required,
            ),
            _gap(),
            TextFormField(
              controller: _password,
              obscureText: _obscure,
              decoration: InputDecoration(
                labelText: 'Password',
                prefixIcon: const Icon(Icons.lock_outline),
                suffixIcon: IconButton(
                  icon: Icon(_obscure ? Icons.visibility_outlined : Icons.visibility_off_outlined),
                  onPressed: () => setState(() => _obscure = !_obscure),
                ),
              ),
              validator: (v) => (v == null || v.length < 6) ? 'At least 6 characters' : null,
            ),
            _gap(),
            TextFormField(
              controller: _confirm,
              obscureText: _obscure,
              decoration: const InputDecoration(
                labelText: 'Confirm password',
                prefixIcon: Icon(Icons.lock_reset_outlined),
              ),
              validator: (v) => v != _password.text ? 'Passwords do not match' : null,
            ),
            const SizedBox(height: 22),
            _section('Your restaurant', Icons.storefront_outlined),
            TextFormField(
              controller: _restaurantName,
              textCapitalization: TextCapitalization.words,
              decoration: const InputDecoration(labelText: 'Restaurant name', prefixIcon: Icon(Icons.restaurant)),
              validator: _required,
            ),
            _gap(),
            TextFormField(
              controller: _cuisine,
              decoration: const InputDecoration(
                labelText: 'Cuisine',
                hintText: 'e.g. Italian, Sushi, Tunisian',
                prefixIcon: Icon(Icons.ramen_dining_outlined),
              ),
              validator: _required,
            ),
            _gap(),
            TextFormField(
              controller: _description,
              maxLines: 4,
              decoration: const InputDecoration(labelText: 'Description', alignLabelWithHint: true),
              validator: _required,
            ),
            _gap(),
            TextFormField(
              controller: _address,
              decoration: const InputDecoration(labelText: 'Address', prefixIcon: Icon(Icons.place_outlined)),
              validator: _required,
            ),
            _gap(),
            TextFormField(
              controller: _restaurantPhone,
              keyboardType: TextInputType.phone,
              decoration: const InputDecoration(
                labelText: 'Restaurant phone (optional)',
                prefixIcon: Icon(Icons.call_outlined),
              ),
            ),
            _gap(),
            TextFormField(
              controller: _hours,
              decoration: const InputDecoration(
                labelText: 'Opening hours (optional)',
                hintText: 'e.g. Mon-Sun 9:00 AM - 10:00 PM',
                prefixIcon: Icon(Icons.schedule),
              ),
            ),
            _gap(),
            TextFormField(
              controller: _image,
              keyboardType: TextInputType.url,
              decoration: const InputDecoration(
                labelText: 'Photo URL (optional)',
                prefixIcon: Icon(Icons.image_outlined),
              ),
            ),
            _gap(),
            DropdownButtonFormField<String>(
              initialValue: _priceRange,
              decoration: const InputDecoration(labelText: 'Price range'),
              items: _priceRanges.entries
                  .map((e) => DropdownMenuItem(value: e.key, child: Text(e.value)))
                  .toList(),
              onChanged: (v) {
                if (v != null) setState(() => _priceRange = v);
              },
            ),
            const SizedBox(height: 28),
            SizedBox(
              width: double.infinity,
              height: 52,
              child: ElevatedButton(
                onPressed: loading ? null : _submit,
                child: loading
                    ? const SizedBox(
                        height: 20,
                        width: 20,
                        child: CircularProgressIndicator(strokeWidth: 2, color: AppColors.surface),
                      )
                    : const Text('Send my request'),
              ),
            ),
            const SizedBox(height: 24),
          ],
        ),
      ),
    );
  }
}
