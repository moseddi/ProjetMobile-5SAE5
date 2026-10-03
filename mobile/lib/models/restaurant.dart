class Restaurant {
  final String id;
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

  factory Restaurant.fromJson(Map<String, dynamic> json) {
    return Restaurant(
      id: json['id'] ?? '',
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
  final MenuCategory category;
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
}

enum MenuCategory {
  starters,
  mainCourses,
  desserts,
  drinks,
}

extension MenuCategoryExtension on MenuCategory {
  String get displayName {
    switch (this) {
      case MenuCategory.starters:
        return 'Starters';
      case MenuCategory.mainCourses:
        return 'Main Courses';
      case MenuCategory.desserts:
        return 'Desserts';
      case MenuCategory.drinks:
        return 'Drinks';
    }
  }
}
