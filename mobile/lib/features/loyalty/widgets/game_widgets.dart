import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../../core/theme/app_colors.dart';
import '../../../models/loyalty.dart';
import '../../auth/providers/auth_provider.dart';
import '../providers/loyalty_provider.dart';

/// Look of each game card (icon + gradient), shared by the strip and the game screens.
class GameStyle {
  final IconData icon;
  final List<Color> colors;

  const GameStyle(this.icon, this.colors);

  static GameStyle of(GameType type) {
    switch (type) {
      case GameType.luckyWheel:
        return const GameStyle(Icons.album, [Color(0xFFFF9A3C), Color(0xFFFF5E62)]);
      case GameType.scratchCard:
        return const GameStyle(Icons.local_activity, [Color(0xFF7F7FD5), Color(0xFF5B5BD6)]);
      case GameType.cardPick:
        return const GameStyle(Icons.style, [Color(0xFF11998E), Color(0xFF38B58A)]);
      case GameType.luckyDice:
        return const GameStyle(Icons.casino, [Color(0xFFE94E77), Color(0xFFB83280)]);
    }
  }
}

/// Fallback titles if the server status is not loaded yet.
String gameTitle(GameType type) {
  switch (type) {
    case GameType.luckyWheel:
      return 'Lucky Wheel';
    case GameType.scratchCard:
      return 'Scratch Card';
    case GameType.cardPick:
      return 'Pick a Card';
    case GameType.luckyDice:
      return 'Lucky Dice';
  }
}

/// Top banner of every game screen: which restaurant the points go to.
class GameHeader extends StatelessWidget {
  final GameType type;
  final String restaurantName;
  final String instruction;

  const GameHeader({super.key, required this.type, required this.restaurantName, required this.instruction});

  @override
  Widget build(BuildContext context) {
    final style = GameStyle.of(type);
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(18),
      decoration: BoxDecoration(
        gradient: LinearGradient(colors: style.colors, begin: Alignment.topLeft, end: Alignment.bottomRight),
        borderRadius: BorderRadius.circular(20),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Icon(style.icon, color: Colors.white, size: 22),
              const SizedBox(width: 8),
              Text(gameTitle(type),
                  style: const TextStyle(fontSize: 18, fontWeight: FontWeight.w800, color: Colors.white)),
            ],
          ),
          const SizedBox(height: 10),
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
            decoration: BoxDecoration(
              color: Colors.white.withOpacity(0.22),
              borderRadius: BorderRadius.circular(20),
            ),
            child: Row(
              mainAxisSize: MainAxisSize.min,
              children: [
                const Icon(Icons.storefront, size: 15, color: Colors.white),
                const SizedBox(width: 6),
                Flexible(
                  child: Text('Points go to $restaurantName',
                      overflow: TextOverflow.ellipsis,
                      style: const TextStyle(fontSize: 13, fontWeight: FontWeight.w600, color: Colors.white)),
                ),
              ],
            ),
          ),
          const SizedBox(height: 10),
          Text(instruction, style: TextStyle(fontSize: 14, color: Colors.white.withOpacity(0.92))),
        ],
      ),
    );
  }
}

/// Asks the server to play. Shows the server's message and returns null if refused
/// (already played today, network, ...). On success the shown balance is refreshed.
Future<GamePlayResult?> playOnServer(BuildContext context, GameType type, String restaurantId) async {
  final messenger = ScaffoldMessenger.of(context);
  final loyalty = context.read<LoyaltyProvider>();
  final auth = context.read<AuthProvider>();
  try {
    final result = await loyalty.play(type, restaurantId);
    final user = auth.currentUser;
    if (user != null) auth.updateUser(user.copyWith(points: result.totalPoints));
    return result;
  } catch (e) {
    messenger.showSnackBar(SnackBar(
      backgroundColor: AppColors.error,
      content: Text(e.toString().replaceAll('Exception: ', '')),
    ));
    return null;
  }
}

/// Celebration dialog once the reward is revealed. Closes the game screen on "Done".
Future<void> showWinDialog(BuildContext context, GamePlayResult result) {
  return showDialog<void>(
    context: context,
    barrierDismissible: false,
    builder: (dialogContext) => AlertDialog(
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(22)),
      contentPadding: const EdgeInsets.fromLTRB(24, 28, 24, 8),
      content: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          Container(
            width: 84,
            height: 84,
            decoration: BoxDecoration(color: AppColors.accent.withOpacity(0.2), shape: BoxShape.circle),
            child: const Icon(Icons.celebration, size: 42, color: AppColors.accent),
          ),
          const SizedBox(height: 18),
          Text('+${result.pointsEarned} points',
              style: const TextStyle(fontSize: 30, fontWeight: FontWeight.w900, color: AppColors.textPrimary)),
          const SizedBox(height: 6),
          Text('added at ${result.restaurantName}',
              textAlign: TextAlign.center,
              style: const TextStyle(fontSize: 15, color: AppColors.textSecondary)),
          const SizedBox(height: 14),
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 10),
            decoration: BoxDecoration(color: AppColors.background, borderRadius: BorderRadius.circular(12)),
            child: Text(
              'You now have ${result.restaurantPoints} points here • ${result.totalPoints} in total',
              textAlign: TextAlign.center,
              style: const TextStyle(fontSize: 13, fontWeight: FontWeight.w600),
            ),
          ),
          const SizedBox(height: 10),
          const Text('Come back tomorrow to play again.',
              style: TextStyle(fontSize: 12.5, color: AppColors.textSecondary)),
        ],
      ),
      actions: [
        SizedBox(
          width: double.infinity,
          child: ElevatedButton(
            onPressed: () {
              Navigator.of(dialogContext).pop();
              Navigator.of(context).pop(true);
            },
            child: const Text('Done'),
          ),
        ),
      ],
    ),
  );
}
