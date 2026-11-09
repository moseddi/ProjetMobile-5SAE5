import '../../core/api/api_client.dart';

class ChatRestaurant {
  final String id;
  final String name;
  final String address;

  const ChatRestaurant({
    required this.id,
    required this.name,
    required this.address,
  });

  factory ChatRestaurant.fromJson(Map<String, dynamic> json) {
    return ChatRestaurant(
      id: json['id'] as String,
      name: json['name'] as String? ?? '',
      address: json['address'] as String? ?? '',
    );
  }
}

class ChatReply {
  final String reply;
  final List<ChatRestaurant> restaurants;

  const ChatReply({required this.reply, required this.restaurants});
}

class ApiChatService {
  Future<ChatReply> askAboutMenu(String message) async {
    final response = await ApiClient.post(
      '/chat',
      {'message': message},
      requestTimeout: const Duration(minutes: 10),
    );
    final data = ApiClient.handleResponse(response) as Map<String, dynamic>;
    final reply = data['reply'] as String?;
    if (reply == null || reply.isEmpty) {
      throw ApiException('The menu assistant returned an empty response.');
    }
    final restaurants = data['restaurants'] as List<dynamic>? ?? const [];
    return ChatReply(
      reply: reply,
      restaurants: restaurants
          .map((item) => ChatRestaurant.fromJson(item as Map<String, dynamic>))
          .toList(),
    );
  }
}
