class Event {
  final String id;
  final String restaurantId;
  final String restaurantName;
  final String title;
  final String image;
  final DateTime datetime;
  final String description;
  final EventType type;

  Event({
    required this.id,
    required this.restaurantId,
    required this.restaurantName,
    required this.title,
    required this.image,
    required this.datetime,
    required this.description,
    required this.type,
  });
}

enum EventType {
  liveMusic,
  specialDinner,
  brunch,
  workshop,
  tasting,
}

extension EventTypeExtension on EventType {
  String get displayName {
    switch (this) {
      case EventType.liveMusic:
        return 'Live Music';
      case EventType.specialDinner:
        return 'Special Dinner';
      case EventType.brunch:
        return 'Brunch';
      case EventType.workshop:
        return 'Workshop';
      case EventType.tasting:
        return 'Tasting';
    }
  }
}
