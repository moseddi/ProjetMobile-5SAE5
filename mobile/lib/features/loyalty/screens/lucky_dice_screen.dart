import 'dart:async';
import 'dart:math';
import 'package:flutter/material.dart';
import '../../../core/theme/app_colors.dart';
import '../../../models/loyalty.dart';
import '../widgets/game_widgets.dart';

/// Lucky Dice: roll once a day; each pip on the die is worth 5 points.
class LuckyDiceScreen extends StatefulWidget {
  final String restaurantId;
  final String restaurantName;

  const LuckyDiceScreen({super.key, required this.restaurantId, required this.restaurantName});

  @override
  State<LuckyDiceScreen> createState() => _LuckyDiceScreenState();
}

class _LuckyDiceScreenState extends State<LuckyDiceScreen> {
  final _random = Random();
  int _face = 1;
  bool _busy = false;
  bool _done = false;
  Timer? _timer;

  @override
  void dispose() {
    _timer?.cancel();
    super.dispose();
  }

  Future<void> _roll() async {
    if (_busy || _done) return;
    setState(() => _busy = true);

    final result = await playOnServer(context, GameType.luckyDice, widget.restaurantId);
    if (!mounted) return;
    if (result == null) {
      setState(() => _busy = false);
      return;
    }

    final finalFace = (result.pointsEarned ~/ 5).clamp(1, 6);
    var ticks = 0;
    final completer = Completer<void>();
    _timer = Timer.periodic(const Duration(milliseconds: 90), (timer) {
      ticks++;
      if (!mounted) {
        timer.cancel();
        return;
      }
      setState(() => _face = ticks >= 14 ? finalFace : _random.nextInt(6) + 1);
      if (ticks >= 14) {
        timer.cancel();
        completer.complete();
      }
    });
    await completer.future;

    if (!mounted) return;
    setState(() {
      _busy = false;
      _done = true;
    });
    await Future<void>.delayed(const Duration(milliseconds: 500));
    if (!mounted) return;
    await showWinDialog(context, result);
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColors.background,
      appBar: AppBar(title: const Text('Lucky Dice')),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(20),
        child: Column(
          children: [
            GameHeader(
              type: GameType.luckyDice,
              restaurantName: widget.restaurantName,
              instruction: 'Roll the die: every pip is worth 5 points.',
            ),
            const SizedBox(height: 36),
            AnimatedContainer(
              duration: const Duration(milliseconds: 150),
              transform: Matrix4.rotationZ(_busy ? (_random.nextDouble() - 0.5) * 0.4 : 0),
              transformAlignment: Alignment.center,
              child: _Die(face: _face),
            ),
            const SizedBox(height: 18),
            Text(
              _done ? '${_face * 5} points' : _busy ? 'Rolling...' : 'Ready?',
              style: const TextStyle(fontSize: 22, fontWeight: FontWeight.w800, color: AppColors.textPrimary),
            ),
            const SizedBox(height: 28),
            SizedBox(
              width: double.infinity,
              height: 54,
              child: ElevatedButton.icon(
                onPressed: _busy || _done ? null : _roll,
                icon: _busy
                    ? const SizedBox(
                        width: 18,
                        height: 18,
                        child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white),
                      )
                    : const Icon(Icons.casino),
                label: Text(_done ? 'Played today' : _busy ? 'Rolling...' : 'Roll the dice'),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _Die extends StatelessWidget {
  final int face;

  const _Die({required this.face});

  // Pip positions on a 3x3 grid for each face.
  static const _pips = {
    1: [4],
    2: [0, 8],
    3: [0, 4, 8],
    4: [0, 2, 6, 8],
    5: [0, 2, 4, 6, 8],
    6: [0, 2, 3, 5, 6, 8],
  };

  @override
  Widget build(BuildContext context) {
    final active = _pips[face] ?? const [4];
    return Container(
      width: 170,
      height: 170,
      padding: const EdgeInsets.all(22),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(32),
        boxShadow: [BoxShadow(color: Colors.black.withOpacity(0.18), blurRadius: 22, offset: const Offset(0, 10))],
        border: Border.all(color: AppColors.border, width: 2),
      ),
      child: GridView.count(
        crossAxisCount: 3,
        physics: const NeverScrollableScrollPhysics(),
        children: [
          for (var i = 0; i < 9; i++)
            Center(
              child: active.contains(i)
                  ? Container(
                      width: 26,
                      height: 26,
                      decoration: const BoxDecoration(color: Color(0xFFB83280), shape: BoxShape.circle),
                    )
                  : null,
            ),
        ],
      ),
    );
  }
}
