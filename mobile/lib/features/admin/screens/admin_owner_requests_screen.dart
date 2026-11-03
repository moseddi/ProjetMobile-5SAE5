import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import 'package:provider/provider.dart';
import '../../../core/theme/app_colors.dart';
import '../../../models/owner_application.dart';
import '../../../widgets/admin_toolbar.dart';
import '../providers/admin_provider.dart';
import '../providers/owner_requests_provider.dart';

/// Admin > Requests: restaurant owners who want to join the platform.
class AdminOwnerRequestsScreen extends StatefulWidget {
  const AdminOwnerRequestsScreen({super.key});

  @override
  State<AdminOwnerRequestsScreen> createState() => _AdminOwnerRequestsScreenState();
}

class _AdminOwnerRequestsScreenState extends State<AdminOwnerRequestsScreen> {
  String? _busyId;
  String _query = '';
  AdminSort _sort = AdminSort.newest;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) => context.read<OwnerRequestsProvider>().load());
  }

  Future<void> _accept(OwnerApplication a) async {
    final ok = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('Accept this request?'),
        content: Text(
          '"${a.restaurantName}" will be created and ${a.ownerName} (${a.email}) '
          'will become its owner and can log in right away.',
        ),
        actions: [
          TextButton(onPressed: () => Navigator.pop(ctx, false), child: const Text('Cancel')),
          ElevatedButton(
            onPressed: () => Navigator.pop(ctx, true),
            style: ElevatedButton.styleFrom(backgroundColor: AppColors.success, foregroundColor: Colors.white),
            child: const Text('Accept'),
          ),
        ],
      ),
    );
    if (ok != true || !mounted) return;
    await _run(a, () => context.read<OwnerRequestsProvider>().accept(a.id),
        '${a.restaurantName} accepted. ${a.ownerName} can now log in.', AppColors.success);
  }

  Future<void> _reject(OwnerApplication a) async {
    final reasonController = TextEditingController();
    final ok = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('Reject this request?'),
        content: TextField(
          controller: reasonController,
          maxLines: 3,
          decoration: const InputDecoration(labelText: 'Reason (optional)', alignLabelWithHint: true),
        ),
        actions: [
          TextButton(onPressed: () => Navigator.pop(ctx, false), child: const Text('Cancel')),
          ElevatedButton(
            onPressed: () => Navigator.pop(ctx, true),
            style: ElevatedButton.styleFrom(backgroundColor: AppColors.error, foregroundColor: Colors.white),
            child: const Text('Reject'),
          ),
        ],
      ),
    );
    final reason = reasonController.text;
    reasonController.dispose();
    if (ok != true || !mounted) return;
    await _run(a, () => context.read<OwnerRequestsProvider>().reject(a.id, reason: reason),
        'Request from ${a.ownerName} rejected.', AppColors.textSecondary);
  }

  Future<void> _run(OwnerApplication a, Future<void> Function() action, String success, Color color) async {
    setState(() => _busyId = a.id);
    final messenger = ScaffoldMessenger.of(context);
    final admin = context.read<AdminProvider>();
    try {
      await action();
      admin.loadStats();
      messenger.showSnackBar(SnackBar(content: Text(success), backgroundColor: color));
    } catch (e) {
      messenger.showSnackBar(SnackBar(
        content: Text(e.toString().replaceAll('Exception: ', '')),
        backgroundColor: AppColors.error,
      ));
    } finally {
      if (mounted) setState(() => _busyId = null);
    }
  }

  @override
  Widget build(BuildContext context) {
    final provider = context.watch<OwnerRequestsProvider>();
    final q = _query.trim().toLowerCase();
    final items = sortByAdded(
      provider.visible.where((a) =>
          q.isEmpty ||
          a.restaurantName.toLowerCase().contains(q) ||
          a.ownerName.toLowerCase().contains(q) ||
          a.email.toLowerCase().contains(q) ||
          a.cuisine.toLowerCase().contains(q)),
      _sort,
      (a) => a.id,
    );

    return Scaffold(
      backgroundColor: AppColors.background,
      appBar: AppBar(title: const Text('Owner Requests')),
      body: RefreshIndicator(
        onRefresh: provider.load,
        child: ListView(
          physics: const AlwaysScrollableScrollPhysics(),
          padding: const EdgeInsets.fromLTRB(16, 8, 16, 32),
          children: [
            AdminSearchBar(
              hint: 'Search restaurant, owner or email',
              sort: _sort,
              onQueryChanged: (v) => setState(() => _query = v),
              onSortChanged: (v) => setState(() => _sort = v),
            ),
            const SizedBox(height: 10),
            _buildFilters(provider),
            const SizedBox(height: 14),
            if (provider.isLoading && provider.applications.isEmpty)
              const Padding(padding: EdgeInsets.only(top: 80), child: Center(child: CircularProgressIndicator()))
            else if (provider.error != null && provider.applications.isEmpty)
              _message(Icons.wifi_off, 'Could not load requests', provider.error!)
            else if (items.isEmpty)
              _message(
                Icons.inbox_outlined,
                provider.filter == OwnerApplicationStatus.pending ? 'No pending request' : 'Nothing here',
                provider.filter == OwnerApplicationStatus.pending
                    ? 'New restaurant owners who register will show up here.'
                    : 'No request matches this filter.',
              )
            else
              for (final a in items) _card(a),
          ],
        ),
      ),
    );
  }

  Widget _buildFilters(OwnerRequestsProvider provider) {
    final options = <(String, OwnerApplicationStatus?)>[
      ('Pending', OwnerApplicationStatus.pending),
      ('Accepted', OwnerApplicationStatus.accepted),
      ('Rejected', OwnerApplicationStatus.rejected),
      ('All', null),
    ];
    return SingleChildScrollView(
      scrollDirection: Axis.horizontal,
      child: Row(
        children: [
          for (final (label, status) in options)
            Padding(
              padding: const EdgeInsets.only(right: 8),
              child: ChoiceChip(
                label: Text('$label (${provider.countFor(status)})'),
                selected: provider.filter == status,
                onSelected: (_) => provider.setFilter(status),
                selectedColor: AppColors.primary,
                backgroundColor: AppColors.surface,
                showCheckmark: false,
                side: const BorderSide(color: AppColors.border),
                labelStyle: TextStyle(
                  fontWeight: FontWeight.w700,
                  color: provider.filter == status ? Colors.white : AppColors.textPrimary,
                ),
              ),
            ),
        ],
      ),
    );
  }

  Widget _message(IconData icon, String title, String text) {
    return Padding(
      padding: const EdgeInsets.only(top: 70),
      child: Column(
        children: [
          Icon(icon, size: 66, color: AppColors.textHint),
          const SizedBox(height: 14),
          Text(title, style: const TextStyle(fontSize: 18, fontWeight: FontWeight.w800)),
          const SizedBox(height: 6),
          Text(text, textAlign: TextAlign.center, style: const TextStyle(color: AppColors.textSecondary)),
        ],
      ),
    );
  }

  Widget _statusChip(OwnerApplication a) {
    final Color color;
    final String label;
    switch (a.status) {
      case OwnerApplicationStatus.pending:
        color = AppColors.warning;
        label = 'Pending';
      case OwnerApplicationStatus.accepted:
        color = AppColors.success;
        label = 'Accepted';
      case OwnerApplicationStatus.rejected:
        color = AppColors.error;
        label = 'Rejected';
    }
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 5),
      decoration: BoxDecoration(color: color.withOpacity(0.14), borderRadius: BorderRadius.circular(14)),
      child: Text(label, style: TextStyle(fontSize: 12, fontWeight: FontWeight.w800, color: color)),
    );
  }

  Widget _info(IconData icon, String text) {
    if (text.trim().isEmpty) return const SizedBox.shrink();
    return Padding(
      padding: const EdgeInsets.only(top: 6),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Icon(icon, size: 16, color: AppColors.textSecondary),
          const SizedBox(width: 8),
          Expanded(child: Text(text, style: const TextStyle(fontSize: 13.5, color: AppColors.textSecondary))),
        ],
      ),
    );
  }

  Widget _card(OwnerApplication a) {
    final busy = _busyId == a.id;
    final fallback = Container(
      height: 120,
      color: AppColors.primary.withOpacity(0.9),
      child: const Center(child: Icon(Icons.storefront, size: 44, color: Colors.white54)),
    );

    return Container(
      margin: const EdgeInsets.only(bottom: 16),
      decoration: BoxDecoration(
        color: AppColors.surface,
        borderRadius: BorderRadius.circular(20),
        border: Border.all(color: AppColors.border),
        boxShadow: [BoxShadow(color: Colors.black.withOpacity(0.05), blurRadius: 12, offset: const Offset(0, 5))],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          ClipRRect(
            borderRadius: const BorderRadius.vertical(top: Radius.circular(20)),
            child: a.image.isEmpty
                ? fallback
                : Image.network(
                    a.image,
                    height: 120,
                    width: double.infinity,
                    fit: BoxFit.cover,
                    errorBuilder: (_, __, ___) => fallback,
                  ),
          ),
          Padding(
            padding: const EdgeInsets.all(16),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  children: [
                    Expanded(
                      child: Text(a.restaurantName,
                          style: const TextStyle(fontSize: 18, fontWeight: FontWeight.w800, color: AppColors.textPrimary)),
                    ),
                    _statusChip(a),
                  ],
                ),
                const SizedBox(height: 2),
                Text(a.cuisine, style: const TextStyle(color: AppColors.accent, fontWeight: FontWeight.w700)),
                if (a.description.isNotEmpty) ...[
                  const SizedBox(height: 8),
                  Text(a.description,
                      maxLines: 3,
                      overflow: TextOverflow.ellipsis,
                      style: const TextStyle(fontSize: 13.5, height: 1.4, color: AppColors.textSecondary)),
                ],
                const Padding(padding: EdgeInsets.symmetric(vertical: 10), child: Divider(height: 1)),
                _info(Icons.person_outline, a.ownerName),
                _info(Icons.email_outlined, a.email),
                _info(Icons.phone_outlined, a.phone),
                _info(Icons.place_outlined, a.address),
                if (a.createdAt != null)
                  _info(Icons.schedule, 'Sent ${DateFormat('MMM d, y • h:mm a').format(a.createdAt!.toLocal())}'),
                if (a.status == OwnerApplicationStatus.rejected && a.rejectionReason != null)
                  _info(Icons.comment_outlined, 'Reason: ${a.rejectionReason}'),
                if (a.isPending) ...[
                  const SizedBox(height: 14),
                  Row(
                    children: [
                      Expanded(
                        child: OutlinedButton.icon(
                          onPressed: busy ? null : () => _reject(a),
                          icon: const Icon(Icons.close, size: 18),
                          label: const Text('Reject'),
                          style: OutlinedButton.styleFrom(
                            foregroundColor: AppColors.error,
                            side: const BorderSide(color: AppColors.error),
                            minimumSize: const Size.fromHeight(46),
                          ),
                        ),
                      ),
                      const SizedBox(width: 10),
                      Expanded(
                        child: ElevatedButton.icon(
                          onPressed: busy ? null : () => _accept(a),
                          icon: busy
                              ? const SizedBox(
                                  width: 16,
                                  height: 16,
                                  child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white),
                                )
                              : const Icon(Icons.check, size: 18),
                          label: const Text('Accept'),
                          style: ElevatedButton.styleFrom(
                            backgroundColor: AppColors.success,
                            foregroundColor: Colors.white,
                            minimumSize: const Size.fromHeight(46),
                          ),
                        ),
                      ),
                    ],
                  ),
                ],
              ],
            ),
          ),
        ],
      ),
    );
  }
}
