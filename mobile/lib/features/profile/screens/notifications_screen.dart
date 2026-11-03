import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:intl/intl.dart';
import '../../../core/theme/app_colors.dart';
import '../../../models/notification.dart';
import '../../auth/providers/auth_provider.dart';
import '../providers/profile_provider.dart';

class NotificationsScreen extends StatefulWidget {
  const NotificationsScreen({super.key});

  @override
  State<NotificationsScreen> createState() => _NotificationsScreenState();
}

class _NotificationsScreenState extends State<NotificationsScreen> {
  NotificationType? _type;
  bool _unreadOnly = false;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    final userId = context.read<AuthProvider>().currentUser?.id;
    if (userId != null) {
      await context.read<ProfileProvider>().loadNotifications(userId);
    }
  }

  @override
  Widget build(BuildContext context) {
    final profileProvider = context.watch<ProfileProvider>();
    final all = profileProvider.notifications;
    final visible = all
        .where((n) => (_type == null || n.type == _type) && (!_unreadOnly || !n.read))
        .toList();

    return Scaffold(
      backgroundColor: AppColors.background,
      appBar: AppBar(
        title: const Text('Notifications'),
      ),
      body: profileProvider.isLoading && all.isEmpty
          ? const Center(child: CircularProgressIndicator())
          : all.isEmpty
              ? _buildEmptyState()
              : RefreshIndicator(
                  onRefresh: _load,
                  child: ListView(
                    padding: const EdgeInsets.all(20),
                    children: [
                      _buildFilters(all),
                      const SizedBox(height: 16),
                      if (visible.isEmpty)
                        _buildNoMatches()
                      else
                        for (final notification in visible) ...[
                          _buildNotificationCard(notification),
                          const SizedBox(height: 12),
                        ],
                    ],
                  ),
                ),
    );
  }

  Widget _buildFilters(List<AppNotification> all) {
    // Only offer the types the user actually has, in a stable order.
    final types = NotificationType.values.where((t) => all.any((n) => n.type == t)).toList();
    final unread = all.where((n) => !n.read).length;

    return SizedBox(
      height: 42,
      child: ListView(
        scrollDirection: Axis.horizontal,
        children: [
          _pill(
            label: 'All',
            icon: Icons.notifications_outlined,
            count: all.length,
            selected: _type == null && !_unreadOnly,
            onTap: () => setState(() {
              _type = null;
              _unreadOnly = false;
            }),
          ),
          _pill(
            label: 'Unread',
            icon: Icons.mark_email_unread_outlined,
            count: unread,
            selected: _unreadOnly,
            highlight: unread > 0,
            onTap: () => setState(() => _unreadOnly = !_unreadOnly),
          ),
          for (final type in types)
            _pill(
              label: type.label,
              icon: _iconFor(type),
              count: all.where((n) => n.type == type).length,
              selected: _type == type,
              onTap: () => setState(() => _type = _type == type ? null : type),
            ),
        ],
      ),
    );
  }

  Widget _pill({
    required String label,
    required IconData icon,
    required int count,
    required bool selected,
    required VoidCallback onTap,
    bool highlight = false,
  }) {
    return Padding(
      padding: const EdgeInsets.only(right: 10),
      child: Material(
        color: selected ? AppColors.primary : AppColors.surface,
        borderRadius: BorderRadius.circular(22),
        child: InkWell(
          onTap: onTap,
          borderRadius: BorderRadius.circular(22),
          child: Container(
            padding: const EdgeInsets.symmetric(horizontal: 14),
            decoration: BoxDecoration(
              borderRadius: BorderRadius.circular(22),
              border: Border.all(
                color: selected ? AppColors.primary : (highlight ? AppColors.accent : AppColors.border),
              ),
            ),
            child: Row(
              children: [
                Icon(icon, size: 16, color: selected ? AppColors.accentLight : AppColors.textSecondary),
                const SizedBox(width: 7),
                Text(
                  label,
                  style: TextStyle(
                    fontSize: 13,
                    fontWeight: FontWeight.w600,
                    color: selected ? Colors.white : AppColors.textPrimary,
                  ),
                ),
                const SizedBox(width: 7),
                Container(
                  padding: const EdgeInsets.symmetric(horizontal: 7, vertical: 2),
                  decoration: BoxDecoration(
                    color: selected
                        ? Colors.white.withOpacity(0.2)
                        : (highlight ? AppColors.accent : AppColors.background),
                    borderRadius: BorderRadius.circular(10),
                  ),
                  child: Text(
                    '$count',
                    style: TextStyle(
                      fontSize: 11,
                      fontWeight: FontWeight.w800,
                      color: selected || highlight ? Colors.white : AppColors.textSecondary,
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

  Widget _buildNoMatches() {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 48),
      child: Column(
        children: [
          const Icon(Icons.filter_alt_off, size: 44, color: AppColors.textHint),
          const SizedBox(height: 12),
          const Text(
            'No notifications of this kind',
            style: TextStyle(fontSize: 16, fontWeight: FontWeight.w600, color: AppColors.textPrimary),
          ),
          const SizedBox(height: 10),
          TextButton(
            onPressed: () => setState(() {
              _type = null;
              _unreadOnly = false;
            }),
            child: const Text('Show all'),
          ),
        ],
      ),
    );
  }

  Widget _buildEmptyState() {
    return Center(
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Icon(Icons.notifications_none, size: 80, color: AppColors.textHint),
          const SizedBox(height: 16),
          const Text(
            'No notifications yet',
            style: TextStyle(fontSize: 18, fontWeight: FontWeight.w600, color: AppColors.textPrimary),
          ),
          const SizedBox(height: 8),
          Text(
            'Updates on your reservations, hangouts and rewards will show up here',
            textAlign: TextAlign.center,
            style: TextStyle(fontSize: 14, color: AppColors.textSecondary),
          ),
        ],
      ),
    );
  }

  Widget _buildNotificationCard(AppNotification notification) {
    return InkWell(
      onTap: () {
        if (!notification.read) {
          context.read<ProfileProvider>().markAsRead(notification.id);
        }
      },
      borderRadius: BorderRadius.circular(12),
      child: Container(
        padding: const EdgeInsets.all(16),
        decoration: BoxDecoration(
          color: AppColors.surface,
          borderRadius: BorderRadius.circular(12),
          border: Border.all(
            color: notification.read ? AppColors.border : AppColors.accent,
            width: notification.read ? 1 : 1.5,
          ),
        ),
        child: Row(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Container(
              padding: const EdgeInsets.all(10),
              decoration: BoxDecoration(
                color: AppColors.accent.withOpacity(0.1),
                shape: BoxShape.circle,
              ),
              child: Icon(_iconFor(notification.type), color: AppColors.accent, size: 20),
            ),
            const SizedBox(width: 12),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    notification.title,
                    style: const TextStyle(
                      fontSize: 15,
                      fontWeight: FontWeight.w600,
                      color: AppColors.textPrimary,
                    ),
                  ),
                  const SizedBox(height: 4),
                  Text(
                    notification.message,
                    style: const TextStyle(fontSize: 13, color: AppColors.textSecondary),
                  ),
                  const SizedBox(height: 6),
                  Text(
                    DateFormat('MMM dd, h:mm a').format(notification.createdAt),
                    style: const TextStyle(fontSize: 12, color: AppColors.textHint),
                  ),
                ],
              ),
            ),
            if (!notification.read)
              Container(
                width: 10,
                height: 10,
                margin: const EdgeInsets.only(top: 4),
                decoration: const BoxDecoration(
                  color: AppColors.accent,
                  shape: BoxShape.circle,
                ),
              ),
          ],
        ),
      ),
    );
  }

  IconData _iconFor(NotificationType type) {
    switch (type) {
      case NotificationType.reservation:
        return Icons.calendar_today;
      case NotificationType.order:
        return Icons.shopping_bag;
      case NotificationType.loyalty:
        return Icons.stars;
      case NotificationType.event:
        return Icons.event;
      case NotificationType.hangout:
        return Icons.group;
      case NotificationType.review:
        return Icons.rate_review;
      case NotificationType.system:
        return Icons.info;
    }
  }
}
