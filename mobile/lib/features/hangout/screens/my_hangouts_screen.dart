import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import 'package:provider/provider.dart';
import '../../../core/navigation/app_router.dart';
import '../../../core/theme/app_colors.dart';
import '../../../models/hangout.dart';
import '../../../widgets/hangout_widgets.dart';
import '../../auth/providers/auth_provider.dart';
import '../providers/hangout_provider.dart';

/// Profile > My Hang Outs: the requests I must answer, what I posted,
/// and the outings I asked to join.
class MyHangOutsScreen extends StatefulWidget {
  const MyHangOutsScreen({super.key});

  @override
  State<MyHangOutsScreen> createState() => _MyHangOutsScreenState();
}

class _MyHangOutsScreenState extends State<MyHangOutsScreen> {
  String? _busyId;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) => _load());
  }

  Future<void> _load() async {
    final user = context.read<AuthProvider>().currentUser;
    if (user == null) return;
    await context.read<HangOutProvider>().load(user.id);
  }

  Future<void> _decide(IncomingRequest item, bool accept) async {
    setState(() => _busyId = item.request.id);
    final messenger = ScaffoldMessenger.of(context);
    final provider = context.read<HangOutProvider>();
    try {
      if (accept) {
        await provider.acceptRequest(item.hangout.id, item.request.id);
      } else {
        await provider.declineRequest(item.hangout.id, item.request.id);
      }
      messenger.showSnackBar(SnackBar(
        backgroundColor: accept ? AppColors.success : AppColors.textSecondary,
        content: Text(accept
            ? '${item.request.userName} accepted. They and the group were notified.'
            : '${item.request.userName}\'s request deleted. They were notified.'),
      ));
    } catch (e) {
      messenger.showSnackBar(SnackBar(
        backgroundColor: AppColors.error,
        content: Text(e.toString().replaceAll('Exception: ', '')),
      ));
    } finally {
      if (mounted) setState(() => _busyId = null);
    }
  }

  @override
  Widget build(BuildContext context) {
    final provider = context.watch<HangOutProvider>();
    final userId = context.watch<AuthProvider>().currentUser?.id;
    final incoming = provider.incomingRequests;
    final mine = provider.myHangOuts;
    final requests = provider.myRequests;

    return DefaultTabController(
      length: 3,
      child: Scaffold(
        backgroundColor: AppColors.background,
        appBar: AppBar(
          title: const Text('My Hang Outs'),
          bottom: TabBar(
            tabs: [
              Tab(text: 'Requests (${incoming.length})'),
              Tab(text: 'My posts (${mine.length})'),
              Tab(text: 'I asked (${requests.length})'),
            ],
          ),
        ),
        body: TabBarView(
          children: [
            _scroll(incoming.isEmpty
                ? [_empty(Icons.inbox_outlined, 'No request to answer',
                    'When someone taps Join on your hang out, they appear here.')]
                : [for (final r in incoming) _incomingCard(r)]),
            _scroll(mine.isEmpty
                ? [_empty(Icons.event_note_outlined, 'You have not posted yet',
                    'Post a hang out from the Hang Out page.')]
                : [
                    for (final h in mine)
                      Padding(
                        padding: const EdgeInsets.only(bottom: 16),
                        child: HangOutCard(
                          hangout: h,
                          currentUserId: userId,
                          onTap: () =>
                              Navigator.of(context).pushNamed(AppRouter.hangoutDetails, arguments: h),
                        ),
                      ),
                  ]),
            _scroll(requests.isEmpty
                ? [_empty(Icons.hourglass_empty, 'No request sent',
                    'Tap Join on a hang out to ask the organizer.')]
                : [for (final h in requests) _myRequestTile(h)]),
          ],
        ),
      ),
    );
  }

  Widget _scroll(List<Widget> children) {
    return RefreshIndicator(
      onRefresh: _load,
      child: ListView(
        physics: const AlwaysScrollableScrollPhysics(),
        padding: const EdgeInsets.all(16),
        children: children,
      ),
    );
  }

  Widget _empty(IconData icon, String title, String text) {
    return Padding(
      padding: const EdgeInsets.only(top: 80),
      child: Column(
        children: [
          Icon(icon, size: 64, color: AppColors.textHint),
          const SizedBox(height: 12),
          Text(title,
              style: const TextStyle(fontSize: 17, fontWeight: FontWeight.w800, color: AppColors.textPrimary)),
          const SizedBox(height: 6),
          Text(text,
              textAlign: TextAlign.center,
              style: const TextStyle(fontSize: 14, color: AppColors.textSecondary)),
        ],
      ),
    );
  }

  Widget _incomingCard(IncomingRequest item) {
    final busy = _busyId == item.request.id;
    final h = item.hangout;
    final full = h.isFull;
    return Container(
      margin: const EdgeInsets.only(bottom: 14),
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: AppColors.surface,
        borderRadius: BorderRadius.circular(18),
        border: Border.all(color: AppColors.border),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              PersonAvatar(name: item.request.userName, url: item.request.userAvatar, radius: 22),
              const SizedBox(width: 12),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(item.request.userName,
                        style: const TextStyle(
                            fontSize: 16, fontWeight: FontWeight.w800, color: AppColors.textPrimary)),
                    const SizedBox(height: 2),
                    const Text('wants to join your hang out',
                        style: TextStyle(fontSize: 13, color: AppColors.textSecondary)),
                  ],
                ),
              ),
            ],
          ),
          const SizedBox(height: 12),
          Container(
            width: double.infinity,
            padding: const EdgeInsets.all(12),
            decoration: BoxDecoration(
              color: AppColors.background,
              borderRadius: BorderRadius.circular(12),
            ),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(h.title.isNotEmpty ? h.title : h.message,
                    maxLines: 2,
                    overflow: TextOverflow.ellipsis,
                    style: const TextStyle(fontWeight: FontWeight.w700, color: AppColors.textPrimary)),
                const SizedBox(height: 4),
                Text('${h.restaurantName} • ${DateFormat('EEE, MMM d • h:mm a').format(h.datetime)}',
                    style: const TextStyle(fontSize: 13, color: AppColors.textSecondary)),
                const SizedBox(height: 4),
                Text('${h.participants.length} / ${h.maxParticipants} going',
                    style: const TextStyle(fontSize: 13, color: AppColors.textSecondary)),
              ],
            ),
          ),
          const SizedBox(height: 12),
          Row(
            children: [
              Expanded(
                child: OutlinedButton.icon(
                  onPressed: busy ? null : () => _decide(item, false),
                  icon: const Icon(Icons.delete_outline, size: 18),
                  label: const Text('Delete'),
                  style: OutlinedButton.styleFrom(
                    foregroundColor: AppColors.error,
                    side: const BorderSide(color: AppColors.error),
                    minimumSize: const Size.fromHeight(44),
                  ),
                ),
              ),
              const SizedBox(width: 10),
              Expanded(
                child: ElevatedButton.icon(
                  onPressed: busy || full ? null : () => _decide(item, true),
                  icon: const Icon(Icons.check, size: 18),
                  label: Text(full ? 'Full' : 'Accept'),
                  style: ElevatedButton.styleFrom(
                    backgroundColor: AppColors.success,
                    foregroundColor: Colors.white,
                    minimumSize: const Size.fromHeight(44),
                  ),
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }

  Widget _myRequestTile(HangOut h) {
    final status = h.myRequestStatus ?? JoinRequestStatus.pending;
    final Color color;
    final String label;
    switch (status) {
      case JoinRequestStatus.accepted:
        color = AppColors.success;
        label = 'Accepted';
      case JoinRequestStatus.declined:
        color = AppColors.error;
        label = 'Declined';
      case JoinRequestStatus.pending:
        color = AppColors.warning;
        label = 'Waiting for ${h.creatorName}';
    }
    return Container(
      margin: const EdgeInsets.only(bottom: 12),
      decoration: BoxDecoration(
        color: AppColors.surface,
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: AppColors.border),
      ),
      child: ListTile(
        onTap: () => Navigator.of(context).pushNamed(AppRouter.hangoutDetails, arguments: h),
        leading: PersonAvatar(name: h.creatorName, url: h.creatorAvatar, radius: 22),
        title: Text(h.title.isNotEmpty ? h.title : h.message,
            maxLines: 1, overflow: TextOverflow.ellipsis, style: const TextStyle(fontWeight: FontWeight.w700)),
        subtitle: Text('${h.restaurantName} • ${DateFormat('MMM d, h:mm a').format(h.datetime)}'),
        trailing: Container(
          padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 5),
          decoration: BoxDecoration(
            color: color.withOpacity(0.12),
            borderRadius: BorderRadius.circular(14),
          ),
          child: Text(label, style: TextStyle(fontSize: 12, fontWeight: FontWeight.w800, color: color)),
        ),
      ),
    );
  }
}
