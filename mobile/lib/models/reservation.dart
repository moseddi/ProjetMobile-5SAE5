/// A confirmed guest who isn't checked in this long after the reserved time
/// is cancelled automatically by the backend.
const int kReservationGraceMinutes = 30;

class Reservation {
  final String id;
  final String userId;
  final String restaurantId;
  final String restaurantName;
  final String restaurantImage;
  final String? eventId;
  final String? eventTitle;
  final String contactName;
  final String contactPhone;
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
    this.eventId,
    this.eventTitle,
    this.contactName = '',
    this.contactPhone = '',
    required this.datetime,
    required this.guests,
    required this.notes,
    required this.orderItems,
    required this.status,
  });

  bool get isEventRegistration => eventId != null;

  /// Accepted, but the guest is already past the grace period (the backend is about
  /// to - or already did - cancel it for lateness).
  bool get isOverdue =>
      status == ReservationStatus.confirmed &&
      DateTime.now().isAfter(datetime.add(const Duration(minutes: kReservationGraceMinutes)));

  /// The status to show: an overdue accepted reservation reads as Late immediately,
  /// without waiting for the backend's next sweep.
  ReservationStatus get displayStatus => isOverdue ? ReservationStatus.late : status;

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
      eventId: json['eventId'],
      eventTitle: json['eventTitle'],
      contactName: json['contactName'] ?? '',
      contactPhone: json['contactPhone'] ?? '',
      datetime: DateTime.parse(json['reservationDateTime']),
      guests: json['partySize'] ?? 0,
      notes: json['specialRequests'] ?? '',
      orderItems: json['items'] != null
          ? (json['items'] as List).map((i) => OrderItem.fromJson(i)).toList()
          : const [],
      status: _statusFromString(json['status']),
    );
  }

  static ReservationStatus _statusFromString(String? status) {
    switch (status?.toUpperCase()) {
      case 'CONFIRMED':
        return ReservationStatus.confirmed;
      case 'ARRIVED':
        return ReservationStatus.arrived;
      case 'REFUSED':
        return ReservationStatus.refused;
      case 'LATE':
        return ReservationStatus.late;
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

  factory OrderItem.fromJson(Map<String, dynamic> json) {
    return OrderItem(
      menuItemId: json['menuItemId'] ?? '',
      name: json['name'] ?? '',
      price: (json['price'] ?? 0).toDouble(),
      quantity: json['quantity'] ?? 1,
    );
  }
}

enum ReservationStatus {
  pending,
  confirmed,
  arrived,
  refused,
  late,
  completed,
  cancelled,
}

extension ReservationStatusExtension on ReservationStatus {
  String get displayName {
    switch (this) {
      case ReservationStatus.pending:
        return 'Pending';
      case ReservationStatus.confirmed:
        return 'Accepted';
      case ReservationStatus.arrived:
        return 'Arrived';
      case ReservationStatus.refused:
        return 'Refused';
      case ReservationStatus.late:
        return 'Late';
      case ReservationStatus.completed:
        return 'Completed';
      case ReservationStatus.cancelled:
        return 'Cancelled';
    }
  }
}
