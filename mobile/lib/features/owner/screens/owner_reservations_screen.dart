import 'dart:async';
import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:intl/intl.dart';
import '../../../core/theme/app_colors.dart';
import '../../../models/reservation.dart';
import '../../../widgets/reservation_filters.dart';
import '../../../widgets/reservation_status.dart';
import '../providers/owner_provider.dart';

class OwnerReservationsScreen extends StatefulWidget {
  const OwnerReservationsScreen({super.key});

  @override
  State<OwnerReservationsScreen> createState() => _OwnerReservationsScreenState();
}

class _OwnerReservationsScreenState extends State<OwnerReservationsScreen> {
  ReservationKind _kind = ReservationKind.all;
  ReservationStatusGroup _group = ReservationStatusGroup.all;
  String _query = '';
  Timer? _ticker;

  @override
  void initState() {
    super.initState();
    context.read<OwnerProvider>().loadReservations();
    // Picks up new requests and flips accepted reservations to "Late" on time.
    _ticker = Timer.periodic(const Duration(seconds: 30), (_) {
      if (mounted) context.read<OwnerProvider>().loadReservations();
    });
  }

  @override
  void dispose() {
    _ticker?.cancel();
    super.dispose();
  }

  Future<void> _reload() => context.read<OwnerProvider>().loadReservations();

  Future<void> _updateStatus(Reservation reservation, String newStatus, String doneMessage) async {
    try {
      await context.read<OwnerProvider>().updateReservationStatus(reservation.id, newStatus);
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text(doneMessage), backgroundColor: AppColors.success),
        );
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text(e.toString().replaceAll('Exception: ', ''))),
        );
        _reload();
      }
    }
  }

  Future<void> _refuse(Reservation reservation) async {
    final confirm = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Refuse this request?'),
        content: Text(
          '${reservation.contactName.isEmpty ? 'The guest' : reservation.contactName} will be told '
          'that you cannot accept ${reservation.isEventRegistration ? 'their registration' : 'their reservation'}.',
        ),
        actions: [
          TextButton(onPressed: () => Navigator.pop(context, false), child: const Text('Keep')),
          ElevatedButton(
            onPressed: () => Navigator.pop(context, true),
            style: ElevatedButton.styleFrom(backgroundColor: AppColors.error),
            child: const Text('Refuse'),
          ),
        ],
      ),
    );
    if (confirm == true) {
      await _updateStatus(reservation, 'REFUSED', 'Request refused');
    }
  }

  List<Reservation> _visible(List<Reservation> all) {
    final q = _query.trim().toLowerCase();

    final list = all.where((r) {
      if (!_kind.matches(r) || !_group.contains(r)) return false;
      if (q.isNotEmpty) {
        final haystack = '${r.contactName} ${r.contactPhone} ${r.eventTitle ?? ''} ${r.notes}'.toLowerCase();
        if (!haystack.contains(q)) return false;
      }
      return true;
    }).toList();

    return sortReservationsForReview(list);
  }

  @override
  Widget build(BuildContext context) {
    final ownerProvider = context.watch<OwnerProvider>();
    final all = ownerProvider.reservations;
    final visible = _visible(all);

    return Scaffold(
      backgroundColor: AppColors.background,
      appBar: AppBar(title: const Text('Reservations')),
      body: RefreshIndicator(
        onRefresh: _reload,
        child: ListView(
          padding: const EdgeInsets.fromLTRB(20, 16, 20, 32),
          children: [
            ReservationKindTabs(
              all: all,
              selected: _kind,
              onChanged: (kind) => setState(() => _kind = kind),
            ),
            const SizedBox(height: 14),
            _buildSearch(),
            const SizedBox(height: 14),
            ReservationStatusPills(
              scoped: all.where(_kind.matches).toList(),
              selected: _group,
              onChanged: (group) => setState(() => _group = group),
              pendingLabel: 'To answer',
            ),
            const SizedBox(height: 14),
            Text(
              '${visible.length} ${visible.length == 1 ? 'request' : 'requests'}',
              style: const TextStyle(fontSize: 13, fontWeight: FontWeight.w600, color: AppColors.textSecondary),
            ),
            const SizedBox(height: 12),
            if (ownerProvider.isLoadingReservations && all.isEmpty)
              const Padding(
                padding: EdgeInsets.symmetric(vertical: 60),
                child: Center(child: CircularProgressIndicator()),
              )
            else if (visible.isEmpty)
              _buildEmptyState(all.isNotEmpty)
            else
              for (final reservation in visible) ...[
                _buildReservationCard(reservation),
                const SizedBox(height: 16),
              ],
          ],
        ),
      ),
    );
  }

  // ---------------------------------------------------------------- filters

  Widget _buildSearch() {
    return TextField(
      onChanged: (value) => setState(() => _query = value),
      decoration: InputDecoration(
        hintText: 'Search by name, phone or event...',
        prefixIcon: const Icon(Icons.search, color: AppColors.textSecondary),
        filled: true,
        fillColor: AppColors.surface,
        contentPadding: const EdgeInsets.symmetric(vertical: 14),
        border: OutlineInputBorder(
          borderRadius: BorderRadius.circular(16),
          borderSide: const BorderSide(color: AppColors.border),
        ),
        enabledBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(16),
          borderSide: const BorderSide(color: AppColors.border),
        ),
      ),
    );
  }

  Widget _buildEmptyState(bool filtered) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 48),
      child: Column(
        children: [
          Container(
            padding: const EdgeInsets.all(22),
            decoration: BoxDecoration(
              color: AppColors.accent.withOpacity(0.12),
              shape: BoxShape.circle,
            ),
            child: Icon(filtered ? Icons.search_off : Icons.event_note, size: 40, color: AppColors.accent),
          ),
          const SizedBox(height: 16),
          Text(
            filtered ? 'Nothing matches these filters' : 'No requests yet',
            style: const TextStyle(fontSize: 18, fontWeight: FontWeight.bold, color: AppColors.textPrimary),
          ),
          const SizedBox(height: 6),
          Text(
            filtered
                ? 'Try another type or status.'
                : 'Table bookings and event registrations will appear here.',
            textAlign: TextAlign.center,
            style: const TextStyle(fontSize: 13.5, color: AppColors.textSecondary),
          ),
        ],
      ),
    );
  }

  // ------------------------------------------------------------------- card

  Widget _buildReservationCard(Reservation r) {
    final status = r.displayStatus;
    final statusColor = reservationStatusColor(status);
    final isEvent = r.isEventRegistration;

    return Container(
      decoration: BoxDecoration(
        color: AppColors.surface,
        borderRadius: BorderRadius.circular(20),
        border: Border.all(color: status == ReservationStatus.pending ? AppColors.warning : AppColors.border,
            width: status == ReservationStatus.pending ? 1.5 : 1),
        boxShadow: [
          BoxShadow(color: Colors.black.withOpacity(0.04), blurRadius: 12, offset: const Offset(0, 5)),
        ],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Container(
            padding: const EdgeInsets.fromLTRB(16, 12, 16, 12),
            decoration: BoxDecoration(
              color: (isEvent ? AppColors.accent : AppColors.primary).withOpacity(0.08),
              borderRadius: const BorderRadius.vertical(top: Radius.circular(20)),
            ),
            child: Row(
              children: [
                Icon(
                  isEvent ? Icons.celebration : Icons.table_restaurant,
                  size: 18,
                  color: isEvent ? AppColors.accent : AppColors.primary,
                ),
                const SizedBox(width: 8),
                Expanded(
                  child: Text(
                    isEvent ? (r.eventTitle ?? 'Event registration') : 'Table reservation',
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                    style: TextStyle(
                      fontSize: 14,
                      fontWeight: FontWeight.w800,
                      color: isEvent ? const Color(0xFF9A6B3A) : AppColors.primary,
                    ),
                  ),
                ),
                Container(
                  padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                  decoration: BoxDecoration(
                    color: statusColor.withOpacity(0.12),
                    borderRadius: BorderRadius.circular(12),
                  ),
                  child: Row(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      Icon(reservationStatusIcon(status), size: 13, color: statusColor),
                      const SizedBox(width: 5),
                      Text(
                        status.displayName,
                        style: TextStyle(fontSize: 12, fontWeight: FontWeight.w800, color: statusColor),
                      ),
                    ],
                  ),
                ),
              ],
            ),
          ),
          Padding(
            padding: const EdgeInsets.all(16),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  children: [
                    CircleAvatar(
                      radius: 20,
                      backgroundColor: AppColors.accent.withOpacity(0.15),
                      child: Text(
                        (r.contactName.isNotEmpty ? r.contactName[0] : '?').toUpperCase(),
                        style: const TextStyle(fontWeight: FontWeight.w800, color: AppColors.accent),
                      ),
                    ),
                    const SizedBox(width: 12),
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(
                            r.contactName.isEmpty ? 'Guest' : r.contactName,
                            style: const TextStyle(fontSize: 16, fontWeight: FontWeight.bold, color: AppColors.textPrimary),
                          ),
                          if (r.contactPhone.isNotEmpty)
                            Padding(
                              padding: const EdgeInsets.only(top: 2),
                              child: SelectableText(
                                r.contactPhone,
                                style: const TextStyle(fontSize: 13.5, color: AppColors.textSecondary),
                              ),
                            ),
                        ],
                      ),
                    ),
                    Container(
                      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
                      decoration: BoxDecoration(
                        color: AppColors.background,
                        borderRadius: BorderRadius.circular(12),
                      ),
                      child: Row(
                        mainAxisSize: MainAxisSize.min,
                        children: [
                          const Icon(Icons.groups_outlined, size: 16, color: AppColors.textSecondary),
                          const SizedBox(width: 5),
                          Text(
                            '${r.guests}',
                            style: const TextStyle(fontSize: 14, fontWeight: FontWeight.w800, color: AppColors.textPrimary),
                          ),
                        ],
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 14),
                Row(
                  children: [
                    const Icon(Icons.event, size: 16, color: AppColors.textSecondary),
                    const SizedBox(width: 6),
                    Text(
                      DateFormat('EEE, MMM d • h:mm a').format(r.datetime),
                      style: const TextStyle(fontSize: 13.5, fontWeight: FontWeight.w600, color: AppColors.textPrimary),
                    ),
                  ],
                ),
                if (r.notes.isNotEmpty) ...[
                  const SizedBox(height: 8),
                  Row(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      const Icon(Icons.notes, size: 16, color: AppColors.textSecondary),
                      const SizedBox(width: 6),
                      Expanded(
                        child: Text(r.notes, style: const TextStyle(fontSize: 13, color: AppColors.textSecondary)),
                      ),
                    ],
                  ),
                ],
                if (r.orderItems.isNotEmpty) ...[
                  const SizedBox(height: 10),
                  Container(
                    padding: const EdgeInsets.all(10),
                    decoration: BoxDecoration(
                      color: AppColors.accent.withOpacity(0.1),
                      borderRadius: BorderRadius.circular(10),
                    ),
                    child: Row(
                      children: [
                        const Icon(Icons.shopping_bag, size: 16, color: AppColors.accent),
                        const SizedBox(width: 8),
                        Expanded(
                          child: Text(
                            'Pre-order: ${r.orderItems.map((i) => '${i.quantity}x ${i.name}').join(', ')}',
                            style: const TextStyle(fontSize: 12.5, fontWeight: FontWeight.w600, color: AppColors.accent),
                          ),
                        ),
                        Text(
                          '\$${r.totalOrderPrice.toStringAsFixed(2)}',
                          style: const TextStyle(fontWeight: FontWeight.bold, color: AppColors.accent),
                        ),
                      ],
                    ),
                  ),
                ],
                const SizedBox(height: 16),
                _buildActions(r),
                _buildHint(r),
              ],
            ),
          ),
        ],
      ),
    );
  }

  /// Refuse / Accept / Arrived - their state follows the reservation's status.
  Widget _buildActions(Reservation r) {
    final status = r.displayStatus;

    final canRefuse = status == ReservationStatus.pending || status == ReservationStatus.confirmed;
    final canAccept = status == ReservationStatus.pending;
    final canArrive = status == ReservationStatus.confirmed;

    // The Accept button reads "Late" in red once the guest is past the grace period.
    final String acceptLabel;
    final IconData acceptIcon;
    Color acceptColor = AppColors.success;
    switch (status) {
      case ReservationStatus.pending:
        acceptLabel = 'Accept';
        acceptIcon = Icons.check;
      case ReservationStatus.late:
        acceptLabel = 'Late';
        acceptIcon = Icons.timer_off_outlined;
        acceptColor = AppColors.error;
      case ReservationStatus.refused:
      case ReservationStatus.cancelled:
        acceptLabel = 'Accept';
        acceptIcon = Icons.check;
        acceptColor = AppColors.textHint;
      case ReservationStatus.confirmed:
      case ReservationStatus.arrived:
      case ReservationStatus.completed:
        acceptLabel = 'Accepted';
        acceptIcon = Icons.check_circle;
    }

    final refused = status == ReservationStatus.refused;
    final arrived = status == ReservationStatus.arrived || status == ReservationStatus.completed;

    return Row(
      children: [
        Expanded(
          child: _actionButton(
            label: refused ? 'Refused' : 'Refuse',
            icon: Icons.close,
            color: canRefuse || refused ? AppColors.error : AppColors.textHint,
            filled: refused,
            onPressed: canRefuse ? () => _refuse(r) : null,
          ),
        ),
        const SizedBox(width: 10),
        Expanded(
          child: _actionButton(
            label: acceptLabel,
            icon: acceptIcon,
            color: acceptColor,
            filled: canAccept || status == ReservationStatus.late,
            onPressed: canAccept ? () => _updateStatus(r, 'CONFIRMED', 'Request accepted') : null,
          ),
        ),
        const SizedBox(width: 10),
        Expanded(
          child: _actionButton(
            label: arrived ? 'Arrived' : 'Arrived',
            icon: Icons.how_to_reg,
            color: canArrive || arrived ? AppColors.primary : AppColors.textHint,
            filled: canArrive || arrived,
            onPressed: canArrive ? () => _updateStatus(r, 'ARRIVED', 'Guest checked in') : null,
          ),
        ),
      ],
    );
  }

  Widget _actionButton({
    required String label,
    required IconData icon,
    required Color color,
    required bool filled,
    required VoidCallback? onPressed,
  }) {
    final enabled = onPressed != null;

    return SizedBox(
      height: 46,
      child: ElevatedButton.icon(
        onPressed: onPressed,
        icon: Icon(icon, size: 17),
        label: Text(label, maxLines: 1, overflow: TextOverflow.ellipsis),
        style: ElevatedButton.styleFrom(
          padding: const EdgeInsets.symmetric(horizontal: 8),
          backgroundColor: filled ? color : color.withOpacity(0.08),
          foregroundColor: filled ? Colors.white : color,
          disabledBackgroundColor: filled ? color : color.withOpacity(0.08),
          disabledForegroundColor: filled ? Colors.white : color.withOpacity(enabled ? 1 : 0.7),
          elevation: 0,
          textStyle: const TextStyle(fontSize: 13, fontWeight: FontWeight.w700),
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(12),
            side: BorderSide(color: filled ? color : color.withOpacity(0.35)),
          ),
        ),
      ),
    );
  }

  Widget _buildHint(Reservation r) {
    final status = r.displayStatus;
    final deadline = DateFormat('h:mm a').format(r.datetime.add(const Duration(minutes: kReservationGraceMinutes)));

    String? text;
    Color color = AppColors.textSecondary;

    if (status == ReservationStatus.confirmed) {
      text = 'Tap Arrived when the guest shows up. Cancelled automatically if not here by $deadline.';
    } else if (status == ReservationStatus.late) {
      text = 'The guest was more than $kReservationGraceMinutes minutes late - cancelled automatically and notified.';
      color = AppColors.error;
    } else if (status == ReservationStatus.pending) {
      text = 'Accept to confirm the table, or refuse - the guest is notified either way.';
    }

    if (text == null) return const SizedBox.shrink();

    return Padding(
      padding: const EdgeInsets.only(top: 10),
      child: Text(text, style: TextStyle(fontSize: 12, height: 1.35, color: color)),
    );
  }
}
