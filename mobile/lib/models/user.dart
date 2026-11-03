enum UserRole {
  client,
  restaurantOwner,
  admin,
}

extension UserRoleParsing on UserRole {
  static UserRole fromJson(String? value) {
    switch (value) {
      case 'ADMIN':
        return UserRole.admin;
      case 'RESTAURANT_OWNER':
        return UserRole.restaurantOwner;
      case 'CLIENT':
      default:
        return UserRole.client;
    }
  }

  String get label {
    switch (this) {
      case UserRole.admin:
        return 'Admin';
      case UserRole.restaurantOwner:
        return 'Restaurant Owner';
      case UserRole.client:
        return 'Client';
    }
  }
}

class User {
  final String id;
  final String name;
  final String email;
  final String avatar;
  final String phone;
  final int points;
  final String loyaltyLevel;
  final UserRole role;
  final String? restaurantId;
  final bool active;

  User({
    required this.id,
    required this.name,
    required this.email,
    required this.avatar,
    this.phone = '',
    required this.points,
    required this.loyaltyLevel,
    this.role = UserRole.client,
    this.restaurantId,
    this.active = true,
  });

  bool get isClient => role == UserRole.client;
  bool get isRestaurantOwner => role == UserRole.restaurantOwner;
  bool get isAdmin => role == UserRole.admin;

  User copyWith({
    String? id,
    String? name,
    String? email,
    String? avatar,
    String? phone,
    int? points,
    String? loyaltyLevel,
    UserRole? role,
    String? restaurantId,
    bool? active,
  }) {
    return User(
      id: id ?? this.id,
      name: name ?? this.name,
      email: email ?? this.email,
      avatar: avatar ?? this.avatar,
      phone: phone ?? this.phone,
      points: points ?? this.points,
      loyaltyLevel: loyaltyLevel ?? this.loyaltyLevel,
      role: role ?? this.role,
      restaurantId: restaurantId ?? this.restaurantId,
      active: active ?? this.active,
    );
  }

  factory User.fromJson(Map<String, dynamic> json) {
    return User(
      id: json['id'] ?? '',
      name: json['name'] ?? '',
      email: json['email'] ?? '',
      avatar: json['avatar'] ?? '',
      phone: json['phone'] ?? '',
      points: json['loyaltyPoints'] ?? 0,
      loyaltyLevel: json['loyaltyLevel'] ?? 'Bronze',
      role: UserRoleParsing.fromJson(json['role']),
      restaurantId: json['restaurantId'],
      active: json['active'] ?? true,
    );
  }
}
