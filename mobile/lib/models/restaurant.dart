class Restaurant {
  final String id;
  final String? ownerId;
  final String name;
  final String image;
  final String cuisine;
  final String address;
  final double rating;
  final String description;
  final String openingHours;
  final bool isOpen;
  final String phone;
  final List<MenuItem> menu;

  Restaurant({
    required this.id,
    this.ownerId,
    required this.name,
    required this.image,
    required this.cuisine,
    required this.address,
    required this.rating,
    required this.description,
    required this.openingHours,
    required this.isOpen,
    required this.phone,
    required this.menu,
  });

  Restaurant copyWith({
    String? id,
    String? ownerId,
    String? name,
    String? image,
    String? cuisine,
    String? address,
    double? rating,
    String? description,
    String? openingHours,
    bool? isOpen,
    String? phone,
    List<MenuItem>? menu,
  }) {
    return Restaurant(
      id: id ?? this.id,
      ownerId: ownerId ?? this.ownerId,
      name: name ?? this.name,
      image: image ?? this.image,
      cuisine: cuisine ?? this.cuisine,
      address: address ?? this.address,
      rating: rating ?? this.rating,
      description: description ?? this.description,
      openingHours: openingHours ?? this.openingHours,
      isOpen: isOpen ?? this.isOpen,
      phone: phone ?? this.phone,
      menu: menu ?? this.menu,
    );
  }

  factory Restaurant.fromJson(Map<String, dynamic> json) {
    return Restaurant(
      id: json['id'] ?? '',
      ownerId: json['ownerId'],
      name: json['name'] ?? '',
      image: json['image'] ?? '',
      cuisine: json['cuisine'] ?? '',
      address: json['address'] ?? '',
      rating: (json['rating'] ?? 0).toDouble(),
      description: json['description'] ?? '',
      openingHours: json['openingHours'] ?? '',
      isOpen: json['isOpen'] ?? false,
      phone: json['phone'] ?? '',
      menu: const [],
    );
  }
}

class MenuItem {
  final String id;
  final String restaurantId;
  /// Built-in key (STARTERS, MAIN_COURSES, DESSERTS, DRINKS, SPECIALS) or a
  /// free-text category created by the restaurant owner.
  final String category;
  final String name;
  final String description;
  final double price;
  final String image;
  final bool available;

  MenuItem({
    required this.id,
    required this.restaurantId,
    required this.category,
    required this.name,
    required this.description,
    required this.price,
    required this.image,
    required this.available,
  });

  factory MenuItem.fromJson(Map<String, dynamic> json) {
    return MenuItem(
      id: json['id'] ?? '',
      restaurantId: json['restaurantId'] ?? '',
      category: (json['category'] as String?)?.trim().isNotEmpty == true ? json['category'] : 'MAIN_COURSES',
      name: json['name'] ?? '',
      description: json['description'] ?? '',
      price: (json['price'] ?? 0).toDouble(),
      image: json['image'] ?? '',
      available: json['available'] ?? true,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'name': name,
      'description': description,
      'price': price,
      'image': image,
      'category': category,
      'available': available,
    };
  }
}
