enum OwnerApplicationStatus { pending, accepted, rejected }

/// A restaurant owner's request to join the platform, as the admin sees it.
class OwnerApplication {
  final String id;
  final String ownerName;
  final String email;
  final String phone;
  final String restaurantName;
  final String cuisine;
  final String description;
  final String image;
  final String address;
  final String restaurantPhone;
  final String openingHours;
  final String priceRange;
  final OwnerApplicationStatus status;
  final String? rejectionReason;
  final DateTime? createdAt;
  final DateTime? reviewedAt;

  const OwnerApplication({
    required this.id,
    required this.ownerName,
    required this.email,
    required this.phone,
    required this.restaurantName,
    required this.cuisine,
    required this.description,
    required this.image,
    required this.address,
    required this.restaurantPhone,
    required this.openingHours,
    required this.priceRange,
    required this.status,
    this.rejectionReason,
    this.createdAt,
    this.reviewedAt,
  });

  bool get isPending => status == OwnerApplicationStatus.pending;

  factory OwnerApplication.fromJson(Map<String, dynamic> json) {
    OwnerApplicationStatus statusOf(String? value) {
      switch (value) {
        case 'ACCEPTED':
          return OwnerApplicationStatus.accepted;
        case 'REJECTED':
          return OwnerApplicationStatus.rejected;
        default:
          return OwnerApplicationStatus.pending;
      }
    }

    DateTime? date(dynamic value) => value == null ? null : DateTime.tryParse(value.toString());

    return OwnerApplication(
      id: json['id'] ?? '',
      ownerName: json['ownerName'] ?? '',
      email: json['email'] ?? '',
      phone: json['phone'] ?? '',
      restaurantName: json['restaurantName'] ?? '',
      cuisine: json['cuisine'] ?? '',
      description: json['description'] ?? '',
      image: json['image'] ?? '',
      address: json['address'] ?? '',
      restaurantPhone: json['restaurantPhone'] ?? '',
      openingHours: json['openingHours'] ?? '',
      priceRange: json['priceRange'] ?? 'MODERATE',
      status: statusOf(json['status']),
      rejectionReason: json['rejectionReason'],
      createdAt: date(json['createdAt']),
      reviewedAt: date(json['reviewedAt']),
    );
  }
}
