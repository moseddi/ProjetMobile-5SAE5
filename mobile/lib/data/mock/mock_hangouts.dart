import '../../models/hangout.dart';

class MockHangOuts {
  static List<HangOut> hangouts = [
    HangOut(
      id: 'h1',
      creatorId: 'user2',
      creatorName: 'Emma Wilson',
      creatorAvatar: 'https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=200',
      restaurantId: '3',
      restaurantName: 'Casa Verde',
      restaurantImage: 'https://images.unsplash.com/photo-1514933651103-005eec06c04b?w=800',
      datetime: DateTime.now().add(const Duration(days: 2, hours: 19)),
      message: 'Planning a pizza night at Casa Verde this Saturday! Who wants to join? 🍕',
      maxParticipants: 6,
      participants: ['user2', 'user3'],
      pendingRequests: [],
    ),
    HangOut(
      id: 'h2',
      creatorId: 'user3',
      creatorName: 'Michael Chen',
      creatorAvatar: 'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=200',
      restaurantId: '7',
      restaurantName: 'Sakura Sushi',
      restaurantImage: 'https://images.unsplash.com/photo-1579584425555-c3ce17fd4351?w=800',
      datetime: DateTime.now().add(const Duration(days: 4, hours: 20)),
      message: 'Sushi night next Tuesday! Let\'s try the new omakase menu.',
      maxParticipants: 4,
      participants: ['user3'],
      pendingRequests: [],
    ),
    HangOut(
      id: 'h3',
      creatorId: 'user2',
      creatorName: 'Emma Wilson',
      creatorAvatar: 'https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=200',
      restaurantId: '5',
      restaurantName: 'Urban Fork',
      restaurantImage: 'https://images.unsplash.com/photo-1466978913421-dad2ebd01d17?w=800',
      datetime: DateTime.now().add(const Duration(days: 1, hours: 18)),
      message: 'Friday burger night! Anyone interested? 🍔',
      maxParticipants: 5,
      participants: ['user2'],
      pendingRequests: [],
    ),
  ];
}
