import 'package:flutter/material.dart';
import '../core/theme/app_colors.dart';
import '../models/reservation.dart';

Color reservationStatusColor(ReservationStatus status) {
  switch (status) {
    case ReservationStatus.pending:
      return AppColors.warning;
    case ReservationStatus.confirmed:
      return AppColors.info;
    case ReservationStatus.arrived:
    case ReservationStatus.completed:
      return AppColors.success;
    case ReservationStatus.refused:
    case ReservationStatus.late:
    case ReservationStatus.cancelled:
      return AppColors.error;
  }
}

IconData reservationStatusIcon(ReservationStatus status) {
  switch (status) {
    case ReservationStatus.pending:
      return Icons.hourglass_top;
    case ReservationStatus.confirmed:
      return Icons.thumb_up_alt_outlined;
    case ReservationStatus.arrived:
      return Icons.how_to_reg;
    case ReservationStatus.completed:
      return Icons.task_alt;
    case ReservationStatus.refused:
      return Icons.block;
    case ReservationStatus.late:
      return Icons.timer_off_outlined;
    case ReservationStatus.cancelled:
      return Icons.cancel_outlined;
  }
}

/// A short explanation shown to the guest under their reservation's status.
String? reservationStatusNote(Reservation reservation) {
  switch (reservation.displayStatus) {
    case ReservationStatus.pending:
      return 'Waiting for the restaurant to accept or decline your request.';
    case ReservationStatus.confirmed:
      return 'Accepted! Your reservation will be cancelled automatically if you are '
          'more than $kReservationGraceMinutes minutes late.';
    case ReservationStatus.arrived:
      return 'You were checked in - reservation complete.';
    case ReservationStatus.refused:
      return 'The restaurant could not accept this request.';
    case ReservationStatus.late:
      return 'Cancelled automatically: you were more than $kReservationGraceMinutes minutes late.';
    case ReservationStatus.completed:
    case ReservationStatus.cancelled:
      return null;
  }
}
