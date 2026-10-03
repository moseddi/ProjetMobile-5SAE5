class User {
  final String id;
  final String name;
  final String email;
  final String avatar;
  final int points;
  final String loyaltyLevel;

  User({
    required this.id,
    required this.name,
    required this.email,
    required this.avatar,
    required this.points,
    required this.loyaltyLevel,
  });

  User copyWith({
    String? id,
    String? name,
    String? email,
    String? avatar,
    int? points,
    String? loyaltyLevel,
  }) {
    return User(
      id: id ?? this.id,
      name: name ?? this.name,
      email: email ?? this.email,
      avatar: avatar ?? this.avatar,
      points: points ?? this.points,
      loyaltyLevel: loyaltyLevel ?? this.loyaltyLevel,
    );
  }

  factory User.fromJson(Map<String, dynamic> json) {
    return User(
      id: json['id'] ?? '',
      name: json['name'] ?? '',
      email: json['email'] ?? '',
      avatar: json['avatar'] ?? '',
      points: json['loyaltyPoints'] ?? 0,
      loyaltyLevel: json['loyaltyLevel'] ?? 'Bronze',
    );
  }
}
