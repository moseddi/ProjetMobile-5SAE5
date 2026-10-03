import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:intl/intl.dart';
import '../../../core/theme/app_colors.dart';
import '../../../core/navigation/app_router.dart';
import '../../../models/hangout.dart';
import '../../auth/providers/auth_provider.dart';
import '../providers/hangout_provider.dart';

class HangOutScreen extends StatefulWidget {
  const HangOutScreen({super.key});

  @override
  State<HangOutScreen> createState() => _HangOutScreenState();
}

class _HangOutScreenState extends State<HangOutScreen> {
  @override
  void initState() {
    super.initState();
    _loadHangOuts();
  }

  Future<void> _loadHangOuts() async {
    await context.read<HangOutProvider>().loadHangOuts();
  }

  Future<void> _handleJoinRequest(HangOut hangout) async {
    final user = context.read<AuthProvider>().currentUser;
    if (user == null) return;

    if (hangout.creatorId == user.id) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('You created this hangout')),
      );
      return;
    }

    if (hangout.participants.contains(user.id)) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('You already joined this hangout')),
      );
      return;
    }

    if (hangout.isFull) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('This hangout is full')),
      );
      return;
    }

    final hasPendingRequest = hangout.pendingRequests.any((r) => r.userId == user.id);
    if (hasPendingRequest) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('You already requested to join')),
      );
      return;
    }

    await context.read<HangOutProvider>().requestToJoin(
          hangout.id,
          user.id,
          user.name,
          user.avatar,
        );

    if (mounted) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('Join request sent!'),
          backgroundColor: AppColors.success,
        ),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    final hangoutProvider = context.watch<HangOutProvider>();

    return Scaffold(
      backgroundColor: AppColors.background,
      appBar: AppBar(
        title: const Text('Hang Out'),
        actions: [
          IconButton(
            icon: const Icon(Icons.add),
            onPressed: () {
              Navigator.of(context).pushNamed(AppRouter.createHangout);
            },
          ),
        ],
      ),
      body: hangoutProvider.isLoading
          ? const Center(child: CircularProgressIndicator())
          : hangoutProvider.hangouts.isEmpty
              ? _buildEmptyState()
              : RefreshIndicator(
                  onRefresh: _loadHangOuts,
                  child: ListView.builder(
                    padding: const EdgeInsets.all(20),
                    itemCount: hangoutProvider.hangouts.length,
                    itemBuilder: (context, index) {
                      return _buildHangOutCard(hangoutProvider.hangouts[index]);
                    },
                  ),
                ),
    );
  }

  Widget _buildEmptyState() {
    return Center(
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Icon(
            Icons.group_outlined,
            size: 80,
            color: AppColors.textHint,
          ),
          const SizedBox(height: 16),
          const Text(
            'No hangouts yet',
            style: TextStyle(
              fontSize: 18,
              fontWeight: FontWeight.w600,
              color: AppColors.textPrimary,
            ),
          ),
          const SizedBox(height: 8),
          Text(
            'Create a hangout to dine with others',
            style: TextStyle(
              fontSize: 14,
              color: AppColors.textSecondary,
            ),
          ),
          const SizedBox(height: 24),
          ElevatedButton.icon(
            onPressed: () {
              Navigator.of(context).pushNamed(AppRouter.createHangout);
            },
            icon: const Icon(Icons.add),
            label: const Text('Create Hang Out'),
          ),
        ],
      ),
    );
  }

  Widget _buildHangOutCard(HangOut hangout) {
    final user = context.read<AuthProvider>().currentUser;
    final isCreator = user?.id == hangout.creatorId;
    final hasJoined = hangout.participants.contains(user?.id);

    return Card(
      margin: const EdgeInsets.only(bottom: 16),
      child: InkWell(
        onTap: () {
          Navigator.of(context).pushNamed(
            AppRouter.hangoutDetails,
            arguments: hangout,
          );
        },
        borderRadius: BorderRadius.circular(12),
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                children: [
                  CircleAvatar(
                    radius: 20,
                    backgroundImage: NetworkImage(hangout.creatorAvatar),
                    backgroundColor: AppColors.accent,
                  ),
                  const SizedBox(width: 12),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          hangout.creatorName,
                          style: const TextStyle(
                            fontSize: 15,
                            fontWeight: FontWeight.w600,
                            color: AppColors.textPrimary,
                          ),
                        ),
                        Text(
                          DateFormat('MMM dd • h:mm a').format(hangout.datetime),
                          style: const TextStyle(
                            fontSize: 13,
                            color: AppColors.textSecondary,
                          ),
                        ),
                      ],
                    ),
                  ),
                  if (hangout.isFull)
                    Container(
                      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                      decoration: BoxDecoration(
                        color: AppColors.error.withOpacity(0.1),
                        borderRadius: BorderRadius.circular(12),
                      ),
                      child: const Text(
                        'Full',
                        style: TextStyle(
                          fontSize: 12,
                          fontWeight: FontWeight.w600,
                          color: AppColors.error,
                        ),
                      ),
                    ),
                ],
              ),
              const SizedBox(height: 12),
              Text(
                hangout.message,
                style: const TextStyle(
                  fontSize: 15,
                  height: 1.4,
                  color: AppColors.textPrimary,
                ),
              ),
              const SizedBox(height: 12),
              Container(
                padding: const EdgeInsets.all(12),
                decoration: BoxDecoration(
                  color: AppColors.background,
                  borderRadius: BorderRadius.circular(8),
                ),
                child: Row(
                  children: [
                    ClipRRect(
                      borderRadius: BorderRadius.circular(6),
                      child: Image.network(
                        hangout.restaurantImage,
                        width: 50,
                        height: 50,
                        fit: BoxFit.cover,
                        errorBuilder: (context, error, stackTrace) {
                          return Container(
                            width: 50,
                            height: 50,
                            color: AppColors.border,
                            child: const Icon(Icons.restaurant, size: 24, color: AppColors.textHint),
                          );
                        },
                      ),
                    ),
                    const SizedBox(width: 12),
                    Expanded(
                      child: Text(
                        hangout.restaurantName,
                        style: const TextStyle(
                          fontSize: 15,
                          fontWeight: FontWeight.w600,
                          color: AppColors.textPrimary,
                        ),
                      ),
                    ),
                  ],
                ),
              ),
              const SizedBox(height: 12),
              Row(
                children: [
                  const Icon(Icons.people, size: 18, color: AppColors.textSecondary),
                  const SizedBox(width: 6),
                  Text(
                    '${hangout.participants.length} / ${hangout.maxParticipants} joined',
                    style: const TextStyle(
                      fontSize: 14,
                      color: AppColors.textSecondary,
                    ),
                  ),
                  const Spacer(),
                  if (!isCreator && !hasJoined && !hangout.isFull)
                    OutlinedButton(
                      onPressed: () => _handleJoinRequest(hangout),
                      style: OutlinedButton.styleFrom(
                        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
                      ),
                      child: const Text('Join'),
                    )
                  else if (hasJoined)
                    Container(
                      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
                      decoration: BoxDecoration(
                        color: AppColors.success.withOpacity(0.1),
                        borderRadius: BorderRadius.circular(12),
                      ),
                      child: const Text(
                        'Joined',
                        style: TextStyle(
                          fontSize: 12,
                          fontWeight: FontWeight.w600,
                          color: AppColors.success,
                        ),
                      ),
                    ),
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }
}
