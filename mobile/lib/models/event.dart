class Event {
  final String id;
  final String restaurantId;
  final String restaurantName;
  final String restaurantImage;
  final String title;
  final String image;
  final DateTime datetime;
  final String description;

  /// Backend category key: WINE_TASTING, COOKING_CLASS, LIVE_MUSIC,
  /// SPECIAL_DINNER, THEMED_NIGHT or OTHER.
  final String category;
  final double price;
  final int availableSpots;
  final int bookedSpots;

  Event({
    required this.id,
    required this.restaurantId,
    required this.restaurantName,
    this.restaurantImage = '',
    required this.title,
    required this.image,
    required this.datetime,
    required this.description,
    this.category = 'OTHER',
    this.price = 0,
    this.availableSpots = 0,
    this.bookedSpots = 0,
  });

  int get spotsLeft => (availableSpots - bookedSpots).clamp(0, availableSpots);
  bool get isFull => spotsLeft <= 0;
  bool get isPast => datetime.isBefore(DateTime.now());

  Event copyWith({
    String? id,
    String? restaurantId,
    String? restaurantName,
    String? restaurantImage,
    String? title,
    String? image,
    DateTime? datetime,
    String? description,
    String? category,
    double? price,
    int? availableSpots,
    int? bookedSpots,
  }) {
    return Event(
      id: id ?? this.id,
      restaurantId: restaurantId ?? this.restaurantId,
      restaurantName: restaurantName ?? this.restaurantName,
      restaurantImage: restaurantImage ?? this.restaurantImage,
      title: title ?? this.title,
      image: image ?? this.image,
      datetime: datetime ?? this.datetime,
      description: description ?? this.description,
      category: category ?? this.category,
      price: price ?? this.price,
      availableSpots: availableSpots ?? this.availableSpots,
      bookedSpots: bookedSpots ?? this.bookedSpots,
    );
  }

  factory Event.fromJson(Map<String, dynamic> json) {
    return Event(
      id: json['id'] ?? '',
      restaurantId: json['restaurantId'] ?? '',
      restaurantName: json['restaurantName'] ?? '',
      restaurantImage: json['restaurantImage'] ?? '',
      title: json['title'] ?? '',
      image: json['image'] ?? '',
      datetime: json['eventDate'] != null ? DateTime.parse(json['eventDate']) : DateTime.now(),
      description: json['description'] ?? '',
      category: json['category'] ?? 'OTHER',
      price: (json['price'] ?? 0).toDouble(),
      availableSpots: json['availableSpots'] ?? 0,
      bookedSpots: json['bookedSpots'] ?? 0,
    );
  }
}
