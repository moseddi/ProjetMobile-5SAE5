import 'dart:math';
import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../../core/theme/app_colors.dart';
import '../../../models/game_result.dart';
import '../../auth/providers/auth_provider.dart';
import '../providers/loyalty_provider.dart';

class CardGameScreen extends StatefulWidget {
  const CardGameScreen({super.key});

  @override
  State<CardGameScreen> createState() => _CardGameScreenState();
}

class _CardGameScreenState extends State<CardGameScreen> with SingleTickerProviderStateMixin {
  final List<int> _rewards = [10, 20, 50, 0, 10, 20];
  int? _selectedCard;
  int? _wonPoints;
  bool _isRevealing = false;
  late AnimationController _controller;
  late Animation<double> _scaleAnimation;

  @override
  void initState() {
    super.initState();
    _controller = AnimationController(
      duration: const Duration(milliseconds: 300),
      vsync: this,
    );
    _scaleAnimation = Tween<double>(begin: 1.0, end: 1.1).animate(
      CurvedAnimation(parent: _controller, curve: Curves.easeInOut),
    );
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  Future<void> _selectCard(int index) async {
    if (_selectedCard != null) return;

    setState(() {
      _selectedCard = index;
      _isRevealing = true;
    });

    _controller.forward();

    await Future.delayed(const Duration(milliseconds: 500));

    final points = _rewards[index];
    setState(() {
      _wonPoints = points;
      _isRevealing = false;
    });

    if (points > 0) {
      final user = context.read<AuthProvider>().currentUser;
      if (user != null) {
        await context.read<LoyaltyProvider>().playGame(
              user.id,
              GameType.cardGame,
              points,
            );
        
        final updatedUser = user.copyWith(points: user.points + points);
        context.read<AuthProvider>().updateUser(updatedUser);
      }
    }

    if (mounted) {
      _showResultDialog(points);
    }
  }

  void _showResultDialog(int points) {
    showDialog(
      context: context,
      barrierDismissible: false,
      builder: (context) => AlertDialog(
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
        title: Row(
          children: [
            Icon(
              points > 0 ? Icons.celebration : Icons.sentiment_neutral,
              color: points > 0 ? AppColors.success : AppColors.textSecondary,
              size: 32,
            ),
            const SizedBox(width: 12),
            Text(points > 0 ? 'You Won!' : 'Try Again'),
          ],
        ),
        content: Text(
          points > 0
              ? 'Congratulations! You won $points points!'
              : 'Better luck next time!',
        ),
        actions: [
          TextButton(
            onPressed: () {
              Navigator.of(context).pop();
              _resetGame();
            },
            child: const Text('Play Again'),
          ),
          ElevatedButton(
            onPressed: () {
              Navigator.of(context).pop();
              Navigator.of(context).pop();
            },
            child: const Text('Done'),
          ),
        ],
      ),
    );
  }

  void _resetGame() {
    _controller.reset();
    setState(() {
      _selectedCard = null;
      _wonPoints = null;
      _rewards.shuffle();
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColors.background,
      appBar: AppBar(
        title: const Text('Card Game'),
      ),
      body: Padding(
        padding: const EdgeInsets.all(20),
        child: Column(
          children: [
            const Text(
              'Pick a Card',
              style: TextStyle(
                fontSize: 28,
                fontWeight: FontWeight.bold,
                color: AppColors.textPrimary,
              ),
            ),
            const SizedBox(height: 12),
            Text(
              _selectedCard == null
                  ? 'Choose one card to reveal your reward'
                  : _isRevealing
                      ? 'Revealing...'
                      : 'You won ${_wonPoints ?? 0} points!',
              style: const TextStyle(
                fontSize: 16,
                color: AppColors.textSecondary,
              ),
              textAlign: TextAlign.center,
            ),
            const SizedBox(height: 40),
            Expanded(
              child: GridView.builder(
                gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
                  crossAxisCount: 3,
                  childAspectRatio: 0.7,
                  crossAxisSpacing: 12,
                  mainAxisSpacing: 12,
                ),
                itemCount: 6,
                itemBuilder: (context, index) {
                  return _buildCard(index);
                },
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildCard(int index) {
    final isSelected = _selectedCard == index;
    final isRevealed = isSelected && !_isRevealing;

    return GestureDetector(
      onTap: () => _selectCard(index),
      child: AnimatedBuilder(
        animation: _scaleAnimation,
        builder: (context, child) {
          return Transform.scale(
            scale: isSelected ? _scaleAnimation.value : 1.0,
            child: Container(
              decoration: BoxDecoration(
                color: isRevealed
                    ? (_wonPoints! > 0 ? AppColors.success : AppColors.textSecondary)
                    : AppColors.accent,
                borderRadius: BorderRadius.circular(12),
                boxShadow: [
                  BoxShadow(
                    color: Colors.black.withOpacity(0.1),
                    blurRadius: 8,
                    offset: const Offset(0, 4),
                  ),
                ],
              ),
              child: Center(
                child: isRevealed
                    ? Column(
                        mainAxisAlignment: MainAxisAlignment.center,
                        children: [
                          Icon(
                            _wonPoints! > 0 ? Icons.stars : Icons.close,
                            size: 48,
                            color: Colors.white,
                          ),
                          const SizedBox(height: 8),
                          Text(
                            _wonPoints! > 0 ? '+${_wonPoints!}' : 'Try Again',
                            style: const TextStyle(
                              fontSize: 24,
                              fontWeight: FontWeight.bold,
                              color: Colors.white,
                            ),
                          ),
                        ],
                      )
                    : const Icon(
                        Icons.question_mark,
                        size: 48,
                        color: Colors.white,
                      ),
              ),
            ),
          );
        },
      ),
    );
  }
}
