import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../../core/theme/app_colors.dart';
import '../../../models/admin_user.dart';
import '../../../models/user.dart';
import '../../../widgets/admin_toolbar.dart';
import '../providers/admin_provider.dart';
import 'admin_user_detail_screen.dart';

class AdminUsersScreen extends StatefulWidget {
  const AdminUsersScreen({super.key});

  @override
  State<AdminUsersScreen> createState() => _AdminUsersScreenState();
}

class _AdminUsersScreenState extends State<AdminUsersScreen> {
  String _query = '';
  AdminSort _sort = AdminSort.newest;
  bool? _active; // null = all, true = active, false = deactivated

  List<AdminUser> _filtered(List<AdminUser> users) {
    final q = _query.trim().toLowerCase();
    final list = users.where((u) {
      if (_active != null && u.active != _active) return false;
      if (q.isEmpty) return true;
      return u.name.toLowerCase().contains(q) || u.email.toLowerCase().contains(q) || (u.phone ?? '').contains(q);
    });
    return sortByAdded(list, _sort, (u) => u.id);
  }

  Widget _buildToolbar() {
    return Padding(
      padding: const EdgeInsets.fromLTRB(16, 12, 16, 0),
      child: Column(
        children: [
          AdminSearchBar(
            hint: 'Search name, email or phone',
            sort: _sort,
            onQueryChanged: (v) => setState(() => _query = v),
            onSortChanged: (v) => setState(() => _sort = v),
          ),
          const SizedBox(height: 10),
          AdminChips<bool?>(
            options: const [('Any status', null), ('Active', true), ('Deactivated', false)],
            selected: _active,
            onChanged: (v) => setState(() => _active = v),
          ),
        ],
      ),
    );
  }
  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (mounted) context.read<AdminProvider>().loadUsers();
    });
  }

  Future<void> _openUser(AdminUser user) async {
    await Navigator.of(context).push(
      MaterialPageRoute(builder: (_) => AdminUserDetailScreen(user: user)),
    );
    if (mounted) {
      context.read<AdminProvider>().loadUsers(role: context.read<AdminProvider>().roleFilter);
    }
  }

  @override
  Widget build(BuildContext context) {
    final admin = context.watch<AdminProvider>();
    final items = _filtered(admin.users);

    return Scaffold(
      backgroundColor: AppColors.background,
      appBar: AppBar(
        title: const Text('Users'),
      ),
      body: Column(
        children: [
          _buildToolbar(),
          _buildFilterChips(admin),
          const Divider(height: 1),
          Expanded(
            child: RefreshIndicator(
              onRefresh: () => context.read<AdminProvider>().loadUsers(role: admin.roleFilter),
              child: admin.isLoadingUsers && admin.users.isEmpty
                  ? const Center(child: CircularProgressIndicator())
                  : admin.usersError != null && admin.users.isEmpty
                      ? _buildError(admin.usersError!)
                      : admin.users.isEmpty
                          ? _buildEmptyState()
                          : items.isEmpty
                              ? ListView(children: [
                                  AdminNoResults(onReset: () => setState(() {
                                        _query = '';
                                        _active = null;
                                      })),
                                ])
                              : ListView.builder(
                                  padding: const EdgeInsets.all(16),
                                  itemCount: items.length,
                                  itemBuilder: (context, index) => _buildUserCard(items[index]),
                                ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildFilterChips(AdminProvider admin) {
    final filters = <String?, String>{
      null: 'All',
      'CLIENT': 'Clients',
      'RESTAURANT_OWNER': 'Owners',
      'ADMIN': 'Admins',
    };

    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
      child: SingleChildScrollView(
        scrollDirection: Axis.horizontal,
        child: Row(
          children: filters.entries.map((entry) {
            final selected = admin.roleFilter == entry.key;
            return Padding(
              padding: const EdgeInsets.only(right: 8),
              child: ChoiceChip(
                label: Text(entry.value),
                selected: selected,
                onSelected: (_) => context.read<AdminProvider>().setRoleFilter(entry.key),
              ),
            );
          }).toList(),
        ),
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
        Icon(Icons.people_outline, size: 80, color: AppColors.textHint),
        const SizedBox(height: 16),
        const Center(
          child: Text(
            'No users found',
            style: TextStyle(
              fontSize: 18,
              fontWeight: FontWeight.w600,
              color: AppColors.textPrimary,
            ),
          ),
        ),
      ],
    );
  }

  Widget _buildUserCard(AdminUser user) {
    return Card(
      margin: const EdgeInsets.only(bottom: 12),
      child: InkWell(
        borderRadius: BorderRadius.circular(12),
        onTap: () => _openUser(user),
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Row(
            children: [
              CircleAvatar(
                radius: 24,
                backgroundColor: AppColors.accent,
                backgroundImage: user.avatar.isNotEmpty ? NetworkImage(user.avatar) : null,
                child: user.avatar.isEmpty
                    ? Text(
                        user.name.isNotEmpty ? user.name[0].toUpperCase() : '?',
                        style: const TextStyle(color: AppColors.surface, fontWeight: FontWeight.bold),
                      )
                    : null,
              ),
              const SizedBox(width: 12),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      user.name,
                      style: const TextStyle(
                        fontSize: 16,
                        fontWeight: FontWeight.w600,
                        color: AppColors.textPrimary,
                      ),
                    ),
                    const SizedBox(height: 2),
                    Text(
                      user.email,
                      style: const TextStyle(
                        fontSize: 13,
                        color: AppColors.textSecondary,
                      ),
                    ),
                    const SizedBox(height: 8),
                    Row(
                      children: [
                        _badge(user.role.label, AppColors.accent),
                        const SizedBox(width: 8),
                        _badge(
                          user.active ? 'Active' : 'Deactivated',
                          user.active ? AppColors.success : AppColors.error,
                        ),
                      ],
                    ),
                  ],
                ),
              ),
              const Icon(Icons.arrow_forward_ios, size: 16, color: AppColors.textSecondary),
            ],
          ),
        ),
      ),
    );
  }

  Widget _badge(String label, Color color) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
      decoration: BoxDecoration(
        color: color.withOpacity(0.1),
        borderRadius: BorderRadius.circular(12),
      ),
      child: Text(
        label,
        style: TextStyle(
          fontSize: 11,
          fontWeight: FontWeight.w600,
          color: color,
        ),
      ),
    );
  }
}
