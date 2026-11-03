import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../../core/theme/app_colors.dart';
import '../../../models/admin_user.dart';
import '../../../models/restaurant.dart';
import '../../../models/user.dart';
import '../../auth/providers/auth_provider.dart';
import '../providers/admin_provider.dart';
import '../../restaurants/providers/restaurant_provider.dart';

class AdminUserDetailScreen extends StatefulWidget {
  final AdminUser user;

  const AdminUserDetailScreen({super.key, required this.user});

  @override
  State<AdminUserDetailScreen> createState() => _AdminUserDetailScreenState();
}

class _AdminUserDetailScreenState extends State<AdminUserDetailScreen> {
  late AdminUser _user;
  bool _isBusy = false;

  @override
  void initState() {
    super.initState();
    _user = widget.user;
  }

  bool get _isSelf => context.read<AuthProvider>().currentUser?.id == _user.id;

  void _showMessage(String message, {bool isError = false}) {
    if (!mounted) return;
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text(message.replaceAll('Exception: ', '')),
        backgroundColor: isError ? AppColors.error : AppColors.success,
      ),
    );
  }

  Future<void> _run(Future<void> Function() action) async {
    setState(() => _isBusy = true);
    try {
      await action();
    } catch (e) {
      _showMessage(e.toString(), isError: true);
    } finally {
      if (mounted) setState(() => _isBusy = false);
    }
  }

  Future<void> _toggleActive() async {
    final activating = !_user.active;
    if (!activating) {
      final confirm = await _confirm(
        title: 'Deactivate Account',
        message: 'Are you sure you want to deactivate ${_user.name}\'s account? '
            'They will no longer be able to sign in.',
        confirmLabel: 'Deactivate',
      );
      if (confirm != true) return;
    }

    await _run(() async {
      final updated = await context.read<AdminProvider>().setUserActive(_user.id, activating);
      setState(() => _user = updated);
      _showMessage(activating ? 'Account activated' : 'Account deactivated');
    });
  }

  Future<void> _toggleAdminRole() async {
    final makingAdmin = _user.role != UserRole.admin;
    final confirm = await _confirm(
      title: makingAdmin ? 'Make Admin' : 'Make Client',
      message: makingAdmin
          ? 'Grant ${_user.name} full administrator access?'
          : 'Remove administrator access from ${_user.name}?',
      confirmLabel: 'Confirm',
      destructive: false,
    );
    if (confirm != true) return;

    await _run(() async {
      final updated = await context
          .read<AdminProvider>()
          .setUserRole(_user.id, makingAdmin ? 'ADMIN' : 'CLIENT');
      setState(() => _user = updated);
      _showMessage(makingAdmin ? 'Promoted to admin' : 'Demoted to client');
    });
  }

  Future<void> _unassignOwner() async {
    final confirm = await _confirm(
      title: 'Unassign Owner',
      message: '${_user.name} will lose access to their restaurant and become a client.',
      confirmLabel: 'Unassign',
    );
    if (confirm != true) return;

    await _run(() async {
      final updated = await context.read<AdminProvider>().unassignOwner(_user.id);
      setState(() => _user = updated);
      _showMessage('Owner unassigned');
    });
  }

  Future<void> _assignOwner() async {
    final restaurantProvider = context.read<RestaurantProvider>();
    await restaurantProvider.loadRestaurants();
    if (!mounted) return;

    final restaurants = [...restaurantProvider.restaurants];
    if (restaurants.isEmpty && restaurantProvider.error != null) {
      _showMessage(restaurantProvider.error!, isError: true);
      return;
    }

    restaurants.sort((a, b) {
      final aRank = a.ownerId == null ? 0 : 1;
      final bRank = b.ownerId == null ? 0 : 1;
      if (aRank != bRank) return aRank.compareTo(bRank);
      return a.name.compareTo(b.name);
    });

    if (restaurants.isEmpty) {
      _showMessage('No restaurants exist yet. Create one first.', isError: true);
      return;
    }

    final selected = await showDialog<Restaurant>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Assign Restaurant'),
        content: SizedBox(
          width: double.maxFinite,
          child: ListView.separated(
            shrinkWrap: true,
            itemCount: restaurants.length,
            separatorBuilder: (_, __) => const Divider(height: 1),
            itemBuilder: (context, index) {
              final restaurant = restaurants[index];
              final unassigned = restaurant.ownerId == null;
              return ListTile(
                title: Text(restaurant.name),
                subtitle: Text(unassigned ? 'No owner assigned' : 'Already has an owner'),
                trailing: unassigned
                    ? const Icon(Icons.flag, color: AppColors.warning)
                    : const Icon(Icons.person, color: AppColors.textHint),
                onTap: () => Navigator.of(context).pop(restaurant),
              );
            },
          ),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(context).pop(),
            child: const Text('Cancel'),
          ),
        ],
      ),
    );

    if (selected == null) return;

    await _run(() async {
      final updated = await context.read<AdminProvider>().assignOwner(_user.id, selected.id);
      setState(() => _user = updated);
      _showMessage('${_user.name} is now the owner of ${selected.name}');
    });
  }

  Future<void> _deleteUser() async {
    final confirm = await _confirm(
      title: 'Delete Account',
      message: 'This permanently deletes ${_user.name}\'s account. This cannot be undone.',
      confirmLabel: 'Delete',
    );
    if (confirm != true) return;

    await _run(() async {
      await context.read<AdminProvider>().deleteUser(_user.id);
      if (mounted) {
        _showMessage('Account deleted');
        Navigator.of(context).pop();
      }
    });
  }

  Future<bool?> _confirm({
    required String title,
    required String message,
    required String confirmLabel,
    bool destructive = true,
  }) {
    return showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: Text(title),
        content: Text(message),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(context).pop(false),
            child: const Text('Cancel'),
          ),
          ElevatedButton(
            onPressed: () => Navigator.of(context).pop(true),
            style: destructive
                ? ElevatedButton.styleFrom(backgroundColor: AppColors.error)
                : null,
            child: Text(confirmLabel),
          ),
        ],
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColors.background,
      appBar: AppBar(
        title: const Text('User Details'),
      ),
      body: AbsorbPointer(
        absorbing: _isBusy,
        child: Opacity(
          opacity: _isBusy ? 0.6 : 1,
          child: ListView(
            padding: const EdgeInsets.all(20),
            children: [
              _buildHeader(),
              const SizedBox(height: 24),
              _buildStatsRow(),
              const SizedBox(height: 24),
              if (_isSelf)
                Container(
                  padding: const EdgeInsets.all(12),
                  margin: const EdgeInsets.only(bottom: 16),
                  decoration: BoxDecoration(
                    color: AppColors.info.withOpacity(0.1),
                    borderRadius: BorderRadius.circular(8),
                  ),
                  child: const Row(
                    children: [
                      Icon(Icons.info_outline, color: AppColors.info, size: 20),
                      SizedBox(width: 8),
                      Expanded(
                        child: Text(
                          'This is your own account. Some actions are disabled.',
                          style: TextStyle(color: AppColors.textSecondary),
                        ),
                      ),
                    ],
                  ),
                ),
              _buildActions(),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildHeader() {
    return Column(
      children: [
        CircleAvatar(
          radius: 40,
          backgroundColor: AppColors.accent,
          backgroundImage: _user.avatar.isNotEmpty ? NetworkImage(_user.avatar) : null,
          child: _user.avatar.isEmpty
              ? Text(
                  _user.name.isNotEmpty ? _user.name[0].toUpperCase() : '?',
                  style: const TextStyle(fontSize: 28, color: AppColors.surface, fontWeight: FontWeight.bold),
                )
              : null,
        ),
        const SizedBox(height: 12),
        Text(
          _user.name,
          style: const TextStyle(fontSize: 20, fontWeight: FontWeight.bold, color: AppColors.textPrimary),
        ),
        const SizedBox(height: 4),
        Text(
          _user.email,
          style: const TextStyle(fontSize: 14, color: AppColors.textSecondary),
        ),
        if (_user.phone != null && _user.phone!.isNotEmpty) ...[
          const SizedBox(height: 2),
          Text(
            _user.phone!,
            style: const TextStyle(fontSize: 14, color: AppColors.textSecondary),
          ),
        ],
        const SizedBox(height: 12),
        Wrap(
          spacing: 8,
          alignment: WrapAlignment.center,
          children: [
            _badge(_user.role.label, AppColors.accent),
            _badge(_user.active ? 'Active' : 'Deactivated', _user.active ? AppColors.success : AppColors.error),
          ],
        ),
        if (_user.restaurantId != null) ...[
          const SizedBox(height: 8),
          Text(
            'Restaurant ID: ${_user.restaurantId}',
            style: const TextStyle(fontSize: 12, color: AppColors.textHint),
          ),
        ],
      ],
    );
  }

  Widget _buildStatsRow() {
    return Row(
      mainAxisAlignment: MainAxisAlignment.spaceEvenly,
      children: [
        _statColumn('Points', _user.loyaltyPoints.toString()),
        _statColumn('Reservations', _user.totalReservations.toString()),
        _statColumn('Orders', _user.totalOrders.toString()),
        _statColumn('Reviews', _user.totalReviews.toString()),
      ],
    );
  }

  Widget _statColumn(String label, String value) {
    return Column(
      children: [
        Text(
          value,
          style: const TextStyle(fontSize: 18, fontWeight: FontWeight.bold, color: AppColors.textPrimary),
        ),
        const SizedBox(height: 4),
        Text(
          label,
          style: const TextStyle(fontSize: 12, color: AppColors.textSecondary),
        ),
      ],
    );
  }

  Widget _buildActions() {
    final isOwner = _user.role == UserRole.restaurantOwner;

    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        OutlinedButton.icon(
          onPressed: _isSelf ? null : _toggleActive,
          icon: Icon(_user.active ? Icons.block : Icons.check_circle_outline),
          label: Text(_user.active ? 'Deactivate Account' : 'Activate Account'),
        ),
        const SizedBox(height: 12),
        if (!isOwner)
          OutlinedButton.icon(
            onPressed: _isSelf ? null : _toggleAdminRole,
            icon: const Icon(Icons.admin_panel_settings_outlined),
            label: Text(_user.role == UserRole.admin ? 'Make Client' : 'Make Admin'),
          ),
        if (!isOwner) const SizedBox(height: 12),
        if (isOwner)
          OutlinedButton.icon(
            onPressed: _unassignOwner,
            icon: const Icon(Icons.storefront_outlined),
            label: const Text('Unassign Restaurant Owner'),
          )
        else
          OutlinedButton.icon(
            onPressed: _assignOwner,
            icon: const Icon(Icons.storefront_outlined),
            label: const Text('Assign as Restaurant Owner'),
          ),
        const SizedBox(height: 24),
        ElevatedButton.icon(
          onPressed: _isSelf ? null : _deleteUser,
          style: ElevatedButton.styleFrom(backgroundColor: AppColors.error),
          icon: const Icon(Icons.delete_outline),
          label: const Text('Delete Account'),
        ),
      ],
    );
  }

  Widget _badge(String label, Color color) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 5),
      decoration: BoxDecoration(
        color: color.withOpacity(0.1),
        borderRadius: BorderRadius.circular(12),
      ),
      child: Text(
        label,
        style: TextStyle(fontSize: 12, fontWeight: FontWeight.w600, color: color),
      ),
    );
  }
}
