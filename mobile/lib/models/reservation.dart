import 'restaurant.dart';

class Reservation {
  final String id;
  final String userId;
  final String restaurantId;
  final String restaurantName;
  final String restaurantImage;
  final DateTime datetime;
  final int guests;
  final String notes;
  final List<OrderItem> orderItems;
  final ReservationStatus status;

  Reservation({
    required this.id,
    required this.userId,
    required this.restaurantId,
    required this.restaurantName,
    required this.restaurantImage,
    required this.datetime,
    required this.guests,
    required this.notes,
    required this.orderItems,
    required this.status,
  });

  double get totalOrderPrice {
    return orderItems.fold(0.0, (sum, item) => sum + (item.price * item.quantity));
  }

  factory Reservation.fromJson(Map<String, dynamic> json) {
    return Reservation(
      id: json['id'] ?? '',
      userId: json['userId'] ?? '',
      restaurantId: json['restaurantId'] ?? '',
      restaurantName: json['restaurantName'] ?? '',
      restaurantImage: json['restaurantImage'] ?? '',
      datetime: DateTime.parse(json['reservationDateTime']),
      guests: json['partySize'] ?? 0,
      notes: json['specialRequests'] ?? '',
      orderItems: const [],
      status: _statusFromString(json['status']),
    );
  }

  static ReservationStatus _statusFromString(String? status) {
    switch (status?.toUpperCase()) {
      case 'CONFIRMED':
        return ReservationStatus.confirmed;
      case 'COMPLETED':
        return ReservationStatus.completed;
      case 'CANCELLED':
        return ReservationStatus.cancelled;
      case 'PENDING':
      default:
        return ReservationStatus.pending;
    }
  }
}

class OrderItem {
  final String menuItemId;
  final String name;
  final double price;
  final int quantity;

  OrderItem({
    required this.menuItemId,
    required this.name,
    required this.price,
    required this.quantity,
  });
}

enum ReservationStatus {
  pending,
  confirmed,
  completed,
  cancelled,
}

extension ReservationStatusExtension on ReservationStatus {
  String get displayName {
    switch (this) {
      case ReservationStatus.pending:
        return 'Pending';
      case ReservationStatus.confirmed:
        return 'Confirmed';
      case ReservationStatus.completed:
        return 'Completed';
      case ReservationStatus.cancelled:
        return 'Cancelled';
    }
  }
}
