import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import '../core/theme/app_colors.dart';
import '../models/hangout.dart';

/// Round avatar that falls back to the person's initial when there is no usable photo.
class PersonAvatar extends StatelessWidget {
  final String name;
  final String url;
  final double radius;

  const PersonAvatar({super.key, required this.name, required this.url, this.radius = 18});

  @override
  Widget build(BuildContext context) {
    final initial = name.isNotEmpty ? name[0].toUpperCase() : '?';
    final fallback = CircleAvatar(
      radius: radius,
      backgroundColor: AppColors.accent.withOpacity(0.2),
      child: Text(
        initial,
        style: TextStyle(fontSize: radius * 0.85, fontWeight: FontWeight.w800, color: AppColors.accent),
      ),
    );

    if (url.isEmpty) return fallback;

    return ClipOval(
      child: SizedBox(
        width: radius * 2,
        height: radius * 2,
        child: Image.network(
          url,
          fit: BoxFit.cover,
          errorBuilder: (context, error, stackTrace) => fallback,
        ),
      ),
    );
  }
}

/// A hangout in the feed: restaurant photo with the date, who organizes it,
/// who is already going, and a call-to-action that reflects my own situation.
class HangOutCard extends StatelessWidget {
  final HangOut hangout;
  final String? currentUserId;
  final bool isJoining;
  final VoidCallback? onTap;
  final VoidCallback? onJoin;
  final VoidCallback? onManage;

  const HangOutCard({
    super.key,
    required this.hangout,
    required this.currentUserId,
    this.isJoining = false,
    this.onTap,
    this.onJoin,
    this.onManage,
  });

  bool get _isMine => hangout.creatorId == currentUserId;

  @override
  Widget build(BuildContext context) {
    final showTitle = hangout.title.isNotEmpty && hangout.title.trim() != hangout.message.trim();

    return Material(
      color: AppColors.surface,
      borderRadius: BorderRadius.circular(22),
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(22),
        child: Container(
          decoration: BoxDecoration(
            borderRadius: BorderRadius.circular(22),
            border: Border.all(color: AppColors.border),
            boxShadow: [
              BoxShadow(color: Colors.black.withOpacity(0.05), blurRadius: 14, offset: const Offset(0, 6)),
            ],
          ),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              _buildBanner(),
              Padding(
                padding: const EdgeInsets.fromLTRB(16, 14, 16, 16),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Row(
                      children: [
                        PersonAvatar(name: hangout.creatorName, url: hangout.creatorAvatar, radius: 16),
                        const SizedBox(width: 10),
                        Expanded(
                          child: Text(
                            _isMine ? 'Hosted by you' : 'Hosted by ${hangout.creatorName}',
                            maxLines: 1,
                            overflow: TextOverflow.ellipsis,
                            style: const TextStyle(
                              fontSize: 13,
                              fontWeight: FontWeight.w600,
                              color: AppColors.textSecondary,
                            ),
                          ),
                        ),
                      ],
                    ),
                    const SizedBox(height: 12),
                    if (showTitle) ...[
                      Text(
                        hangout.title,
                        maxLines: 2,
                        overflow: TextOverflow.ellipsis,
                        style: const TextStyle(
                          fontSize: 18,
                          height: 1.2,
                          fontWeight: FontWeight.w800,
                          color: AppColors.textPrimary,
                        ),
                      ),
                      const SizedBox(height: 6),
                    ],
                    Text(
                      hangout.message,
                      maxLines: showTitle ? 2 : 3,
                      overflow: TextOverflow.ellipsis,
                      style: TextStyle(
                        fontSize: showTitle ? 13.5 : 16,
                        height: 1.35,
                        fontWeight: showTitle ? FontWeight.w400 : FontWeight.w700,
                        color: showTitle ? AppColors.textSecondary : AppColors.textPrimary,
                      ),
                    ),
                    const SizedBox(height: 12),
                    Row(
                      children: [
                        const Icon(Icons.storefront_outlined, size: 15, color: AppColors.textSecondary),
                        const SizedBox(width: 6),
                        Expanded(
                          child: Text(
                            hangout.restaurantName,
                            maxLines: 1,
                            overflow: TextOverflow.ellipsis,
                            style: const TextStyle(fontSize: 13.5, color: AppColors.textSecondary),
                          ),
                        ),
                      ],
                    ),
                    const SizedBox(height: 14),
                    _buildMembers(),
                    const SizedBox(height: 14),
                    _buildAction(),
                  ],
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildBanner() {
    final fallback = Container(
      decoration: BoxDecoration(
        gradient: LinearGradient(
          colors: [AppColors.primary, AppColors.accent.withOpacity(0.85)],
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
        ),
      ),
      child: Center(child: Icon(Icons.groups, size: 50, color: Colors.white.withOpacity(0.7))),
    );

    final past = hangout.isPast;
    final String statusLabel;
    final Color statusColor;
    if (past) {
      statusLabel = 'Ended';
      statusColor = AppColors.textSecondary;
    } else if (hangout.isFull) {
      statusLabel = 'Full';
      statusColor = AppColors.error;
    } else {
      statusLabel = '${hangout.availableSpots} ${hangout.availableSpots == 1 ? 'spot' : 'spots'} left';
      statusColor = AppColors.success;
    }

    return ClipRRect(
      borderRadius: const BorderRadius.vertical(top: Radius.circular(22)),
      child: SizedBox(
        height: 140,
        width: double.infinity,
        child: Stack(
          fit: StackFit.expand,
          children: [
            hangout.restaurantImage.isEmpty
                ? fallback
                : Image.network(
                    hangout.restaurantImage,
                    fit: BoxFit.cover,
                    errorBuilder: (context, error, stackTrace) => fallback,
                  ),
            DecoratedBox(
              decoration: BoxDecoration(
                gradient: LinearGradient(
                  begin: Alignment.topCenter,
                  end: Alignment.bottomCenter,
                  colors: [
                    Colors.black.withOpacity(past ? 0.45 : 0.05),
                    Colors.black.withOpacity(past ? 0.7 : 0.55),
                  ],
                ),
              ),
            ),
            Positioned(
              top: 12,
              left: 12,
              child: Container(
                width: 54,
                padding: const EdgeInsets.symmetric(vertical: 7),
                decoration: BoxDecoration(color: Colors.white, borderRadius: BorderRadius.circular(14)),
                child: Column(
                  children: [
                    Text(
                      DateFormat('d').format(hangout.datetime),
                      style: const TextStyle(
                        fontSize: 21,
                        height: 1,
                        fontWeight: FontWeight.w900,
                        color: AppColors.textPrimary,
                      ),
                    ),
                    const SizedBox(height: 3),
                    Text(
                      DateFormat('MMM').format(hangout.datetime).toUpperCase(),
                      style: const TextStyle(
                        fontSize: 11,
                        fontWeight: FontWeight.w800,
                        letterSpacing: 0.8,
                        color: Color(0xFF9A6B3A),
                      ),
                    ),
                  ],
                ),
              ),
            ),
            Positioned(
              top: 12,
              right: 12,
              child: Container(
                padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
                decoration: BoxDecoration(
                  color: statusColor.withOpacity(0.92),
                  borderRadius: BorderRadius.circular(20),
                ),
                child: Text(
                  statusLabel,
                  style: const TextStyle(fontSize: 12, fontWeight: FontWeight.w800, color: Colors.white),
                ),
              ),
            ),
            Positioned(
              left: 12,
              bottom: 12,
              child: Container(
                padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
                decoration: BoxDecoration(
                  color: Colors.black.withOpacity(0.45),
                  borderRadius: BorderRadius.circular(20),
                ),
                child: Row(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    const Icon(Icons.schedule, size: 14, color: AppColors.accentLight),
                    const SizedBox(width: 6),
                    Text(
                      DateFormat('EEE • h:mm a').format(hangout.datetime),
                      style: const TextStyle(fontSize: 12.5, fontWeight: FontWeight.w700, color: Colors.white),
                    ),
                  ],
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildMembers() {
    final members = hangout.members;
    const maxShown = 4;
    final shown = members.take(maxShown).toList();
    final extra = members.length - shown.length;

    return Row(
      children: [
        if (members.isEmpty)
          const Icon(Icons.groups_outlined, size: 20, color: AppColors.textHint)
        else
          SizedBox(
            height: 32,
            width: 22.0 * shown.length + 14 + (extra > 0 ? 24 : 0),
            child: Stack(
              children: [
                for (var i = 0; i < shown.length; i++)
                  Positioned(
                    left: i * 22.0,
                    child: Container(
                      decoration: BoxDecoration(
                        shape: BoxShape.circle,
                        border: Border.all(color: AppColors.surface, width: 2),
                      ),
                      child: PersonAvatar(name: shown[i].name, url: shown[i].avatar, radius: 14),
                    ),
                  ),
                if (extra > 0)
                  Positioned(
                    left: shown.length * 22.0,
                    child: Container(
                      height: 32,
                      padding: const EdgeInsets.symmetric(horizontal: 8),
                      alignment: Alignment.center,
                      decoration: BoxDecoration(
                        color: AppColors.background,
                        shape: BoxShape.rectangle,
                        borderRadius: BorderRadius.circular(16),
                        border: Border.all(color: AppColors.surface, width: 2),
                      ),
                      child: Text('+$extra', style: const TextStyle(fontSize: 11, fontWeight: FontWeight.w800)),
                    ),
                  ),
              ],
            ),
          ),
        const SizedBox(width: 10),
        Expanded(
          child: Text(
            '${hangout.participants.length} / ${hangout.maxParticipants} going',
            style: const TextStyle(fontSize: 13, fontWeight: FontWeight.w600, color: AppColors.textSecondary),
          ),
        ),
      ],
    );
  }

  Widget _buildAction() {
    // Organizer: shortcut to the requests they have to answer.
    if (_isMine) {
      final pending = hangout.pendingRequests.length;
      return _actionButton(
        label: pending > 0 ? '$pending ${pending == 1 ? 'request' : 'requests'} to answer' : 'Your outing',
        icon: pending > 0 ? Icons.mark_email_unread_outlined : Icons.verified_outlined,
        color: pending > 0 ? AppColors.warning : AppColors.primary,
        filled: pending > 0,
        onPressed: pending > 0 ? onManage : null,
      );
    }

    if (hangout.isPast) {
      return _actionButton(label: 'Ended', icon: Icons.history, color: AppColors.textHint, onPressed: null);
    }

    switch (hangout.myRequestStatus) {
      case JoinRequestStatus.accepted:
        return _actionButton(
          label: "You're in",
          icon: Icons.check_circle,
          color: AppColors.success,
          filled: true,
          onPressed: null,
        );
      case JoinRequestStatus.pending:
        return _actionButton(
          label: 'Waiting for ${hangout.creatorName} to accept',
          icon: Icons.hourglass_top,
          color: AppColors.warning,
          onPressed: null,
        );
      case JoinRequestStatus.declined:
        return _actionButton(
          label: 'Request declined',
          icon: Icons.block,
          color: AppColors.error,
          onPressed: null,
        );
      case null:
        if (hangout.isFull) {
          return _actionButton(label: 'Full', icon: Icons.group_off, color: AppColors.textHint, onPressed: null);
        }
        return _actionButton(
          label: isJoining ? 'Sending...' : 'Join',
          icon: Icons.person_add_alt_1,
          color: AppColors.primary,
          filled: true,
          onPressed: isJoining ? null : onJoin,
        );
    }
  }

  Widget _actionButton({
    required String label,
    required IconData icon,
    required Color color,
    bool filled = false,
    required VoidCallback? onPressed,
  }) {
    return SizedBox(
      width: double.infinity,
      height: 46,
      child: ElevatedButton.icon(
        onPressed: onPressed,
        icon: Icon(icon, size: 18),
        label: Text(label, maxLines: 1, overflow: TextOverflow.ellipsis),
        style: ElevatedButton.styleFrom(
          backgroundColor: filled ? color : color.withOpacity(0.1),
          foregroundColor: filled ? Colors.white : color,
          disabledBackgroundColor: filled ? color : color.withOpacity(0.1),
          disabledForegroundColor: filled ? Colors.white : color,
          elevation: 0,
          textStyle: const TextStyle(fontSize: 14, fontWeight: FontWeight.w700),
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(14),
            side: BorderSide(color: filled ? color : color.withOpacity(0.35)),
          ),
        ),
      ),
    );
  }
}
