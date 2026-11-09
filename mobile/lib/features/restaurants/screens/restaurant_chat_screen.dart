import 'package:flutter/material.dart';

import '../../../core/navigation/app_router.dart';
import '../../../core/theme/app_colors.dart';
import '../../../services/api/api_chat_service.dart';
import '../../../services/api/api_restaurant_service.dart';

class RestaurantChatScreen extends StatefulWidget {
  const RestaurantChatScreen({super.key});

  @override
  State<RestaurantChatScreen> createState() => _RestaurantChatScreenState();
}

class _ChatMessage {
  final String text;
  final bool fromUser;
  final List<ChatRestaurant> restaurants;

  const _ChatMessage({
    required this.text,
    required this.fromUser,
    this.restaurants = const [],
  });
}

class _RestaurantChatScreenState extends State<RestaurantChatScreen> {
  final _chatService = ApiChatService();
  final _messageController = TextEditingController();
  final _scrollController = ScrollController();
  final List<_ChatMessage> _messages = [];
  bool _sending = false;

  @override
  void initState() {
    super.initState();
    _messages.add(const _ChatMessage(
      text:
          'Ask about dishes, prices, ingredients, or other information shown on restaurant menus.',
      fromUser: false,
    ));
  }

  @override
  void dispose() {
    _messageController.dispose();
    _scrollController.dispose();
    super.dispose();
  }

  Future<void> _sendMessage() async {
    final message = _messageController.text.trim();
    if (message.isEmpty || _sending) return;

    _messageController.clear();
    setState(() {
      _messages.add(_ChatMessage(text: message, fromUser: true));
      _sending = true;
    });
    _scrollToBottom();

    try {
      final response = await _chatService.askAboutMenu(message);
      if (!mounted) return;
      setState(() => _messages.add(_ChatMessage(
            text: response.reply,
            fromUser: false,
            restaurants: response.restaurants,
          )));
    } catch (error) {
      if (!mounted) return;
      setState(() => _messages.add(_ChatMessage(
            text: 'Sorry, I could not answer: $error',
            fromUser: false,
          )));
    } finally {
      if (mounted) {
        setState(() => _sending = false);
        _scrollToBottom();
      }
    }
  }

  void _scrollToBottom() {
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (!_scrollController.hasClients) return;
      _scrollController.animateTo(
        _scrollController.position.maxScrollExtent,
        duration: const Duration(milliseconds: 250),
        curve: Curves.easeOut,
      );
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColors.background,
      appBar: AppBar(
        title: const Text('Menu assistant'),
        backgroundColor: AppColors.surface,
        foregroundColor: AppColors.textPrimary,
      ),
      body: Column(
        children: [
          Expanded(
            child: ListView.builder(
              controller: _scrollController,
              padding: const EdgeInsets.all(16),
              itemCount: _messages.length + (_sending ? 1 : 0),
              itemBuilder: (context, index) {
                if (_sending && index == _messages.length) {
                  return const Align(
                    alignment: Alignment.centerLeft,
                    child: Padding(
                      padding: EdgeInsets.all(12),
                      child: SizedBox(
                        width: 20,
                        height: 20,
                        child: CircularProgressIndicator(strokeWidth: 2),
                      ),
                    ),
                  );
                }
                return _buildMessage(_messages[index]);
              },
            ),
          ),
          SafeArea(
            top: false,
            child: Padding(
              padding: const EdgeInsets.fromLTRB(12, 8, 12, 12),
              child: Row(
                crossAxisAlignment: CrossAxisAlignment.end,
                children: [
                  Expanded(
                    child: TextField(
                      controller: _messageController,
                      maxLength: 500,
                      minLines: 1,
                      maxLines: 4,
                      textInputAction: TextInputAction.send,
                      onSubmitted: (_) => _sendMessage(),
                      decoration: InputDecoration(
                        hintText: 'Ask about dishes, prices, or ingredients...',
                        counterText: '',
                        filled: true,
                        fillColor: AppColors.surface,
                        border: OutlineInputBorder(
                          borderRadius: BorderRadius.circular(24),
                          borderSide: BorderSide.none,
                        ),
                        contentPadding: const EdgeInsets.symmetric(
                          horizontal: 18,
                          vertical: 12,
                        ),
                      ),
                    ),
                  ),
                  const SizedBox(width: 8),
                  IconButton.filled(
                    onPressed: _sending ? null : _sendMessage,
                    icon: const Icon(Icons.send),
                    tooltip: 'Send question',
                  ),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildMessage(_ChatMessage message) {
    final alignment =
        message.fromUser ? Alignment.centerRight : Alignment.centerLeft;
    final color = message.fromUser ? AppColors.primary : AppColors.surface;
    final textColor = message.fromUser ? Colors.white : AppColors.textPrimary;

    return Column(
      crossAxisAlignment:
          message.fromUser ? CrossAxisAlignment.end : CrossAxisAlignment.start,
      children: [
        Align(
          alignment: alignment,
          child: Container(
            constraints: const BoxConstraints(maxWidth: 320),
            margin: const EdgeInsets.only(bottom: 10),
            padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 10),
            decoration: BoxDecoration(
              color: color,
              borderRadius: BorderRadius.circular(18),
            ),
            child: Text(
              message.text,
              style: TextStyle(color: textColor, height: 1.35),
            ),
          ),
        ),
        ...message.restaurants.map(_buildRestaurantResult),
      ],
    );
  }

  Widget _buildRestaurantResult(ChatRestaurant restaurant) {
    return Align(
      alignment: Alignment.centerLeft,
      child: Card(
        margin: const EdgeInsets.only(bottom: 10, left: 8),
        child: ListTile(
          leading: const CircleAvatar(
            backgroundColor: AppColors.accentLight,
            child: Icon(Icons.restaurant, color: AppColors.primary),
          ),
          title: Text(restaurant.name),
          subtitle: Text(restaurant.address),
          trailing: const Icon(Icons.chevron_right),
          onTap: () => _openRestaurant(restaurant),
        ),
      ),
    );
  }

  Future<void> _openRestaurant(ChatRestaurant result) async {
    try {
      final restaurant =
          await ApiRestaurantService().getRestaurantById(result.id);
      if (!mounted) return;
      if (restaurant == null) {
        throw StateError('Restaurant no longer exists.');
      }
      await Navigator.of(context).pushNamed(
        AppRouter.restaurantDetails,
        arguments: restaurant,
      );
    } catch (error) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('Could not open restaurant: $error')),
      );
    }
  }
}
