import 'user.dart';

/// A richer, admin-facing view of an account, as returned by the
/// `/api/admin/users` endpoints. Deliberately separate from [User]
/// (lib/models/user.dart) since the backend's `UserDto` for admin screens
/// carries different field names (`loyaltyPoints` instead of `points`,
/// counts of reservations/orders/reviews, etc.) and is only ever used by
/// the admin console.
class AdminUser {
  final String id;
  final String email;
  final String name;
  final String? phone;
  final String avatar;
  final int loyaltyPoints;
  final int totalReservations;
  final int totalOrders;
  final int totalReviews;
  final UserRole role;
  final String? restaurantId;
  final bool active;

  AdminUser({
    required this.id,
    required this.email,
    required this.name,
    this.phone,
    required this.avatar,
    required this.loyaltyPoints,
    required this.totalReservations,
    required this.totalOrders,
    required this.totalReviews,
    required this.role,
    this.restaurantId,
    required this.active,
  });

  AdminUser copyWith({
    String? id,
    String? email,
    String? name,
    String? phone,
    String? avatar,
    int? loyaltyPoints,
    int? totalReservations,
    int? totalOrders,
    int? totalReviews,
    UserRole? role,
    String? restaurantId,
    bool? active,
  }) {
    return AdminUser(
      id: id ?? this.id,
      email: email ?? this.email,
      name: name ?? this.name,
      phone: phone ?? this.phone,
      avatar: avatar ?? this.avatar,
      loyaltyPoints: loyaltyPoints ?? this.loyaltyPoints,
      totalReservations: totalReservations ?? this.totalReservations,
      totalOrders: totalOrders ?? this.totalOrders,
      totalReviews: totalReviews ?? this.totalReviews,
      role: role ?? this.role,
      restaurantId: restaurantId ?? this.restaurantId,
      active: active ?? this.active,
    );
  }

  factory AdminUser.fromJson(Map<String, dynamic> json) {
    return AdminUser(
      id: json['id'] ?? '',
      email: json['email'] ?? '',
      name: json['name'] ?? '',
      phone: json['phone'],
      avatar: json['avatar'] ?? '',
      loyaltyPoints: json['loyaltyPoints'] ?? 0,
      totalReservations: json['totalReservations'] ?? 0,
      totalOrders: json['totalOrders'] ?? 0,
      totalReviews: json['totalReviews'] ?? 0,
      role: UserRoleParsing.fromJson(json['role']),
      restaurantId: json['restaurantId'],
      active: json['active'] ?? true,
    );
  }
}

/// Converts a [UserRole] back into the string value the backend expects
/// (the inverse of [UserRoleParsing.fromJson] in lib/models/user.dart).
extension UserRoleApiValue on UserRole {
  String get apiValue {
    switch (this) {
      case UserRole.admin:
        return 'ADMIN';
      case UserRole.restaurantOwner:
        return 'RESTAURANT_OWNER';
      case UserRole.client:
        return 'CLIENT';
    }
  }
}
