/// Price range for a restaurant, as the backend represents it.
enum PriceRange {
  budget,
  moderate,
  expensive,
  luxury,
}

extension PriceRangeExtension on PriceRange {
  String get label {
    switch (this) {
      case PriceRange.budget:
        return '\$ Budget';
      case PriceRange.moderate:
        return '\$\$ Moderate';
      case PriceRange.expensive:
        return '\$\$\$ Expensive';
      case PriceRange.luxury:
        return '\$\$\$\$ Luxury';
    }
  }

  String toJson() {
    switch (this) {
      case PriceRange.budget:
        return 'BUDGET';
      case PriceRange.moderate:
        return 'MODERATE';
      case PriceRange.expensive:
        return 'EXPENSIVE';
      case PriceRange.luxury:
        return 'LUXURY';
    }
  }

  static PriceRange fromJson(String? value) {
    switch (value) {
      case 'BUDGET':
        return PriceRange.budget;
      case 'EXPENSIVE':
        return PriceRange.expensive;
      case 'LUXURY':
        return PriceRange.luxury;
      case 'MODERATE':
      default:
        return PriceRange.moderate;
    }
  }
}

/// The restaurant owned by the signed-in RESTAURANT_OWNER, as returned by
/// `/api/owner/restaurant`. Kept separate from [Restaurant] (lib/models/restaurant.dart)
/// because the owner-facing shape carries several fields (features, priceRange,
/// reviewCount, timestamps) that the public-facing model doesn't need.
class OwnerRestaurant {
  final String id;
  final String? ownerId;
  final String name;
  final String cuisine;
  final String description;
  final String image;
  final String address;
  final String phone;
  final String openingHours;
  final double rating;
  final int reviewCount;
  final bool isOpen;

  /// Whether the restaurant runs the loyalty rewards programme.
  final bool loyaltyEnabled;
  final List<String> features;
  final PriceRange priceRange;
  final DateTime? createdAt;
  final DateTime? updatedAt;

  OwnerRestaurant({
    required this.id,
    this.ownerId,
    required this.name,
    required this.cuisine,
    required this.description,
    required this.image,
    required this.address,
    required this.phone,
    required this.openingHours,
    this.rating = 0.0,
    this.reviewCount = 0,
    this.isOpen = true,
    this.loyaltyEnabled = true,
    this.features = const [],
    this.priceRange = PriceRange.moderate,
    this.createdAt,
    this.updatedAt,
  });

  OwnerRestaurant copyWith({
    String? name,
    String? cuisine,
    String? description,
    String? image,
    String? address,
    String? phone,
    String? openingHours,
    bool? isOpen,
    List<String>? features,
    PriceRange? priceRange,
  }) {
    return OwnerRestaurant(
      id: id,
      ownerId: ownerId,
      name: name ?? this.name,
      cuisine: cuisine ?? this.cuisine,
      description: description ?? this.description,
      image: image ?? this.image,
      address: address ?? this.address,
      phone: phone ?? this.phone,
      openingHours: openingHours ?? this.openingHours,
      rating: rating,
      reviewCount: reviewCount,
      isOpen: isOpen ?? this.isOpen,
      loyaltyEnabled: loyaltyEnabled,
      features: features ?? this.features,
      priceRange: priceRange ?? this.priceRange,
      createdAt: createdAt,
      updatedAt: updatedAt,
    );
  }

  factory OwnerRestaurant.fromJson(Map<String, dynamic> json) {
    return OwnerRestaurant(
      id: json['id'] ?? '',
      ownerId: json['ownerId'],
      name: json['name'] ?? '',
      cuisine: json['cuisine'] ?? '',
      description: json['description'] ?? '',
      image: json['image'] ?? '',
      address: json['address'] ?? '',
      phone: json['phone'] ?? '',
      openingHours: json['openingHours'] ?? '',
      rating: (json['rating'] as num?)?.toDouble() ?? 0.0,
      reviewCount: json['reviewCount'] ?? 0,
      isOpen: json['isOpen'] ?? false,
      loyaltyEnabled: json['loyaltyEnabled'] != false,
      features: json['features'] != null
          ? List<String>.from(json['features'] as List)
          : const [],
      priceRange: PriceRangeExtension.fromJson(json['priceRange']),
      createdAt: json['createdAt'] != null ? DateTime.tryParse(json['createdAt']) : null,
      updatedAt: json['updatedAt'] != null ? DateTime.tryParse(json['updatedAt']) : null,
    );
  }

  /// Body for PUT /api/owner/restaurant. The server ignores id/ownerId/rating/
  /// reviewCount/timestamps even if present, so they're left out here.
  Map<String, dynamic> toJson() {
    return {
      'name': name,
      'cuisine': cuisine,
      'description': description,
      'image': image,
      'address': address,
      'phone': phone,
      'openingHours': openingHours,
      'isOpen': isOpen,
      'features': features,
      'priceRange': priceRange.toJson(),
    };
  }
}
