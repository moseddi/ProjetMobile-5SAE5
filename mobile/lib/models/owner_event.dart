/// An event hosted by the owner's restaurant, as returned by `/api/owner/events`.
class OwnerEvent {
  final String id;
  final String restaurantId;
  final String restaurantName;
  final String restaurantImage;
  final String title;
  final String description;
  final String image;
  final DateTime eventDate;
  final double price;
  final int availableSpots;
  final int bookedSpots;
  /// A built-in key (e.g. WINE_TASTING) or a free-text type created by the owner.
  final String category;

  OwnerEvent({
    this.id = '',
    this.restaurantId = '',
    this.restaurantName = '',
    this.restaurantImage = '',
    required this.title,
    this.description = '',
    this.image = '',
    required this.eventDate,
    this.price = 0,
    this.availableSpots = 0,
    this.bookedSpots = 0,
    this.category = 'OTHER',
  });

  int get spotsLeft => (availableSpots - bookedSpots).clamp(0, availableSpots);
  bool get isFull => spotsLeft <= 0;
  bool get isPast => eventDate.isBefore(DateTime.now());

  OwnerEvent copyWith({
    String? title,
    String? description,
    String? image,
    DateTime? eventDate,
    double? price,
    int? availableSpots,
    String? category,
  }) {
    return OwnerEvent(
      id: id,
      restaurantId: restaurantId,
      restaurantName: restaurantName,
      restaurantImage: restaurantImage,
      title: title ?? this.title,
      description: description ?? this.description,
      image: image ?? this.image,
      eventDate: eventDate ?? this.eventDate,
      price: price ?? this.price,
      availableSpots: availableSpots ?? this.availableSpots,
      bookedSpots: bookedSpots,
      category: category ?? this.category,
    );
  }

  factory OwnerEvent.fromJson(Map<String, dynamic> json) {
    return OwnerEvent(
      id: json['id'] ?? '',
      restaurantId: json['restaurantId'] ?? '',
      restaurantName: json['restaurantName'] ?? '',
      restaurantImage: json['restaurantImage'] ?? '',
      title: json['title'] ?? '',
      description: json['description'] ?? '',
      image: json['image'] ?? '',
      eventDate: json['eventDate'] != null
          ? DateTime.parse(json['eventDate'])
          : DateTime.now(),
      price: (json['price'] as num?)?.toDouble() ?? 0.0,
      availableSpots: json['availableSpots'] ?? 0,
      bookedSpots: json['bookedSpots'] ?? 0,
      category: (json['category'] as String?)?.isNotEmpty == true ? json['category'] : 'OTHER',
    );
  }

  /// Body for POST/PUT /api/owner/events.
  Map<String, dynamic> toJson() {
    return {
      'title': title,
      'description': description,
      'image': image,
      'eventDate': eventDate.toIso8601String(),
      'price': price,
      'availableSpots': availableSpots,
      'category': category,
    };
  }
}
