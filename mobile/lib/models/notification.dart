class AppNotification {
  final String id;
  final String userId;
  final NotificationType type;
  final String title;
  final String message;
  final String? relatedId;
  final bool read;
  final DateTime createdAt;

  AppNotification({
    required this.id,
    required this.userId,
    required this.type,
    required this.title,
    required this.message,
    this.relatedId,
    required this.read,
    required this.createdAt,
  });

  AppNotification copyWith({
    String? id,
    String? userId,
    NotificationType? type,
    String? title,
    String? message,
    String? relatedId,
    bool? read,
    DateTime? createdAt,
  }) {
    return AppNotification(
      id: id ?? this.id,
      userId: userId ?? this.userId,
      type: type ?? this.type,
      title: title ?? this.title,
      message: message ?? this.message,
      relatedId: relatedId ?? this.relatedId,
      read: read ?? this.read,
      createdAt: createdAt ?? this.createdAt,
    );
  }

  factory AppNotification.fromJson(Map<String, dynamic> json) {
    return AppNotification(
      id: json['id'] ?? '',
      userId: json['userId'] ?? '',
      type: _typeFromString(json['type']),
      title: json['title'] ?? '',
      message: json['message'] ?? '',
      relatedId: json['relatedId'],
      read: json['read'] ?? false,
      createdAt: json['createdAt'] != null ? DateTime.parse(json['createdAt']) : DateTime.now(),
    );
  }

  static NotificationType _typeFromString(String? value) {
    switch (value) {
      case 'RESERVATION':
        return NotificationType.reservation;
      case 'ORDER':
        return NotificationType.order;
      case 'LOYALTY':
        return NotificationType.loyalty;
      case 'EVENT':
        return NotificationType.event;
      case 'HANGOUT':
        return NotificationType.hangout;
      case 'REVIEW':
        return NotificationType.review;
      case 'SYSTEM':
      default:
        return NotificationType.system;
    }
  }
}

enum NotificationType {
  reservation,
  order,
  loyalty,
  event,
  hangout,
  review,
  system,
}

extension NotificationTypeExtension on NotificationType {
  String get label {
    switch (this) {
      case NotificationType.reservation:
        return 'Reservation';
      case NotificationType.order:
        return 'Order';
      case NotificationType.loyalty:
        return 'Loyalty';
      case NotificationType.event:
        return 'Event';
      case NotificationType.hangout:
        return 'Hang Out';
      case NotificationType.review:
        return 'Review';
      case NotificationType.system:
        return 'System';
    }
  }
}
