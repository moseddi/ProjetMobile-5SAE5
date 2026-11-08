import 'dart:math';
import 'package:flutter/material.dart';
import '../../../core/theme/app_colors.dart';
import '../../../models/loyalty.dart';
import '../widgets/game_widgets.dart';

/// Pick a Card: four face-down cards, one tap; the server decides what is behind.
class CardGameScreen extends StatefulWidget {
  final String restaurantId;
  final String restaurantName;

  const CardGameScreen({super.key, required this.restaurantId, required this.restaurantName});

  @override
  State<CardGameScreen> createState() => _CardGameScreenState();
}

class _CardGameScreenState extends State<CardGameScreen> {
  static const _pool = [5, 10, 20, 50];

  int? _picked;
  bool _busy = false;
  bool _revealedAll = false;
  List<int>? _values; // value behind each card, once known

  Future<void> _pick(int index) async {
    if (_busy || _picked != null) return;
    setState(() {
      _busy = true;
      _picked = index;
    });

    final result = await playOnServer(context, GameType.cardPick, widget.restaurantId);
    if (!mounted) return;
    if (result == null) {
      setState(() {
        _busy = false;
        _picked = null;
      });
      return;
    }

    // The picked card shows the real reward; the others show the remaining values.
    final others = [..._pool]..remove(result.pointsEarned);
    others.shuffle(Random());
    final values = List<int>.filled(4, 0);
    var next = 0;
    for (var i = 0; i < 4; i++) {
      values[i] = i == index ? result.pointsEarned : others[next++ % others.length];
    }
    setState(() {
      _values = values;
      _busy = false;
    });

    await Future<void>.delayed(const Duration(milliseconds: 900));
    if (!mounted) return;
    setState(() => _revealedAll = true);
    await Future<void>.delayed(const Duration(milliseconds: 900));
    if (!mounted) return;
    await showWinDialog(context, result);
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColors.background,
      appBar: AppBar(title: const Text('Pick a Card')),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(20),
        child: Column(
          children: [
            GameHeader(
              type: GameType.cardPick,
              restaurantName: widget.restaurantName,
              instruction: 'Choose one card to reveal your points.',
            ),
            const SizedBox(height: 24),
            GridView.count(
              crossAxisCount: 2,
              shrinkWrap: true,
              physics: const NeverScrollableScrollPhysics(),
              crossAxisSpacing: 16,
              mainAxisSpacing: 16,
              childAspectRatio: 0.85,
              children: [for (var i = 0; i < 4; i++) _buildCard(i)],
            ),
            const SizedBox(height: 16),
            Text(
              _picked == null ? 'Tap a card' : _busy ? 'Revealing...' : 'Nice pick!',
              style: const TextStyle(fontSize: 15, color: AppColors.textSecondary),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildCard(int index) {
    final isPicked = _picked == index;
    final value = _values?[index];
    final faceUp = value != null && (isPicked || _revealedAll);
    final style = GameStyle.of(GameType.cardPick);

    return GestureDetector(
      onTap: () => _pick(index),
      child: AnimatedSwitcher(
        duration: const Duration(milliseconds: 450),
        transitionBuilder: (child, animation) => ScaleTransition(scale: animation, child: child),
        child: Container(
          key: ValueKey('$index-$faceUp'),
          decoration: BoxDecoration(
            gradient: faceUp
                ? null
                : LinearGradient(colors: style.colors, begin: Alignment.topLeft, end: Alignment.bottomRight),
            color: faceUp ? (isPicked ? AppColors.surface : AppColors.background) : null,
            borderRadius: BorderRadius.circular(20),
            border: Border.all(
              color: faceUp && isPicked ? AppColors.accent : Colors.transparent,
              width: 3,
            ),
            boxShadow: [BoxShadow(color: Colors.black.withOpacity(0.1), blurRadius: 10, offset: const Offset(0, 5))],
          ),
          child: Center(
            child: faceUp
                ? Column(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      Icon(Icons.stars, size: 40, color: isPicked ? AppColors.accent : AppColors.textHint),
                      const SizedBox(height: 6),
                      Text('+$value',
                          style: TextStyle(
                            fontSize: 28,
                            fontWeight: FontWeight.w900,
                            color: isPicked ? AppColors.textPrimary : AppColors.textHint,
                          )),
                      Text('points',
                          style: TextStyle(color: isPicked ? AppColors.textSecondary : AppColors.textHint)),
                    ],
                  )
                : Icon(
                    _busy && isPicked ? Icons.hourglass_top : Icons.help_outline,
                    size: 56,
                    color: Colors.white,
                  ),
          ),
        ),
      ),
    );
  }
}
