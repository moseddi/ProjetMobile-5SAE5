import 'package:flutter/material.dart';
import '../core/theme/app_colors.dart';
import '../models/reservation.dart';

/// Table bookings vs event registrations.
enum ReservationKind { all, restaurant, event }

extension ReservationKindMatching on ReservationKind {
  bool matches(Reservation r) {
    switch (this) {
      case ReservationKind.all:
        return true;
      case ReservationKind.restaurant:
        return !r.isEventRegistration;
      case ReservationKind.event:
        return r.isEventRegistration;
    }
  }
}

/// Reservation statuses grouped the way people think about them.
enum ReservationStatusGroup { all, pending, accepted, arrived, late, closed }

extension ReservationStatusGroupMatching on ReservationStatusGroup {
  String label({String pendingLabel = 'Pending'}) {
    switch (this) {
      case ReservationStatusGroup.all:
        return 'All';
      case ReservationStatusGroup.pending:
        return pendingLabel;
      case ReservationStatusGroup.accepted:
        return 'Accepted';
      case ReservationStatusGroup.arrived:
        return 'Arrived';
      case ReservationStatusGroup.late:
        return 'Late';
      case ReservationStatusGroup.closed:
        return 'Refused / cancelled';
    }
  }

  bool contains(Reservation r) {
    final status = r.displayStatus;
    switch (this) {
      case ReservationStatusGroup.all:
        return true;
      case ReservationStatusGroup.pending:
        return status == ReservationStatus.pending;
      case ReservationStatusGroup.accepted:
        return status == ReservationStatus.confirmed;
      case ReservationStatusGroup.arrived:
        return status == ReservationStatus.arrived || status == ReservationStatus.completed;
      case ReservationStatusGroup.late:
        return status == ReservationStatus.late;
      case ReservationStatusGroup.closed:
        return status == ReservationStatus.refused || status == ReservationStatus.cancelled;
    }
  }
}

/// Orders reservations newest first (most recently made on top); the status
/// pills let people jump to the ones that need an answer.
List<Reservation> sortReservationsForReview(List<Reservation> list) {
  final byNewest = [...list]..sort((a, b) => b.id.compareTo(a.id));
  return byNewest;
}

/// Big three-way switch: All / Restaurant / Events, each with its count.
class ReservationKindTabs extends StatelessWidget {
  final List<Reservation> all;
  final ReservationKind selected;
  final ValueChanged<ReservationKind> onChanged;

  const ReservationKindTabs({
    super.key,
    required this.all,
    required this.selected,
    required this.onChanged,
  });

  @override
  Widget build(BuildContext context) {
    Widget tab(ReservationKind kind, String label, IconData icon) {
      final isSelected = selected == kind;
      final count = all.where(kind.matches).length;

      return Expanded(
        child: GestureDetector(
          onTap: () => onChanged(kind),
          child: AnimatedContainer(
            duration: const Duration(milliseconds: 180),
            padding: const EdgeInsets.symmetric(vertical: 12),
            decoration: BoxDecoration(
              color: isSelected ? AppColors.primary : Colors.transparent,
              borderRadius: BorderRadius.circular(14),
            ),
            child: Row(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                Icon(icon, size: 17, color: isSelected ? AppColors.accentLight : AppColors.textSecondary),
                const SizedBox(width: 6),
                Flexible(
                  child: Text(
                    label,
                    overflow: TextOverflow.ellipsis,
                    style: TextStyle(
                      fontSize: 13,
                      fontWeight: FontWeight.w700,
                      color: isSelected ? Colors.white : AppColors.textPrimary,
                    ),
                  ),
                ),
                const SizedBox(width: 6),
                Container(
                  padding: const EdgeInsets.symmetric(horizontal: 7, vertical: 2),
                  decoration: BoxDecoration(
                    color: isSelected ? Colors.white.withOpacity(0.2) : AppColors.background,
                    borderRadius: BorderRadius.circular(10),
                  ),
                  child: Text(
                    '$count',
                    style: TextStyle(
                      fontSize: 11,
                      fontWeight: FontWeight.w800,
                      color: isSelected ? Colors.white : AppColors.textSecondary,
                    ),
                  ),
                ),
              ],
            ),
          ),
        ),
      );
    }

    return Container(
      padding: const EdgeInsets.all(5),
      decoration: BoxDecoration(
        color: AppColors.surface,
        borderRadius: BorderRadius.circular(18),
        border: Border.all(color: AppColors.border),
      ),
      child: Row(
        children: [
          tab(ReservationKind.all, 'All', Icons.apps),
          tab(ReservationKind.restaurant, 'Restaurant', Icons.table_restaurant_outlined),
          tab(ReservationKind.event, 'Events', Icons.celebration_outlined),
        ],
      ),
    );
  }
}

/// Scrollable status pills with counts, computed over [scoped] (the reservations
/// already narrowed by kind).
class ReservationStatusPills extends StatelessWidget {
  final List<Reservation> scoped;
  final ReservationStatusGroup selected;
  final ValueChanged<ReservationStatusGroup> onChanged;
  final String pendingLabel;

  const ReservationStatusPills({
    super.key,
    required this.scoped,
    required this.selected,
    required this.onChanged,
    this.pendingLabel = 'Pending',
  });

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      height: 42,
      child: ListView(
        scrollDirection: Axis.horizontal,
        children: [
          for (final group in ReservationStatusGroup.values)
            _pill(group),
        ],
      ),
    );
  }

  Widget _pill(ReservationStatusGroup group) {
    final isSelected = selected == group;
    final count = scoped.where(group.contains).length;
    final urgent = group == ReservationStatusGroup.pending && count > 0;

    return Padding(
      padding: const EdgeInsets.only(right: 10),
      child: Material(
        color: isSelected ? AppColors.primary : AppColors.surface,
        borderRadius: BorderRadius.circular(22),
        child: InkWell(
          onTap: () => onChanged(group),
          borderRadius: BorderRadius.circular(22),
          child: Container(
            padding: const EdgeInsets.symmetric(horizontal: 14),
            decoration: BoxDecoration(
              borderRadius: BorderRadius.circular(22),
              border: Border.all(
                color: isSelected ? AppColors.primary : (urgent ? AppColors.warning : AppColors.border),
              ),
            ),
            child: Row(
              children: [
                Text(
                  group.label(pendingLabel: pendingLabel),
                  style: TextStyle(
                    fontSize: 13,
                    fontWeight: FontWeight.w600,
                    color: isSelected ? Colors.white : AppColors.textPrimary,
                  ),
                ),
                const SizedBox(width: 7),
                Container(
                  padding: const EdgeInsets.symmetric(horizontal: 7, vertical: 2),
                  decoration: BoxDecoration(
                    color: isSelected
                        ? Colors.white.withOpacity(0.2)
                        : (urgent ? AppColors.warning : AppColors.background),
                    borderRadius: BorderRadius.circular(10),
                  ),
                  child: Text(
                    '$count',
                    style: TextStyle(
                      fontSize: 11,
                      fontWeight: FontWeight.w800,
                      color: isSelected || urgent ? Colors.white : AppColors.textSecondary,
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
}
