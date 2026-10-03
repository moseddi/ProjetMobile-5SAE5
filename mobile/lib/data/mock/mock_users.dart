import '../../models/user.dart';

class MockUsers {
  static List<User> users = [
    User(
      id: 'user1',
      name: 'John Smith',
      email: 'demo@example.com',
      avatar: 'https://images.unsplash.com/photo-1633332755192-727a05c4013d?w=200',
      points: 1250,
      loyaltyLevel: 'Gold',
    ),
    User(
      id: 'user2',
      name: 'Emma Wilson',
      email: 'emma@example.com',
      avatar: 'https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=200',
      points: 650,
      loyaltyLevel: 'Silver',
    ),
    User(
      id: 'user3',
      name: 'Michael Chen',
      email: 'michael@example.com',
      avatar: 'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=200',
      points: 320,
      loyaltyLevel: 'Bronze',
    ),
  ];
}
