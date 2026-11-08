import 'package:flutter/material.dart';
import '../../../core/theme/app_colors.dart';
import '../../../models/loyalty.dart';
import '../widgets/game_widgets.dart';

/// Scratch Card: get a card (the server draws the reward), then scratch it off.
class ScratchCardScreen extends StatefulWidget {
  final String restaurantId;
  final String restaurantName;

  const ScratchCardScreen({super.key, required this.restaurantId, required this.restaurantName});

  @override
  State<ScratchCardScreen> createState() => _ScratchCardScreenState();
}

class _ScratchCardScreenState extends State<ScratchCardScreen> {
  static const _cols = 10;
  static const _rows = 6;
  static const _cardSize = Size(300, 180);

  GamePlayResult? _result;
  bool _busy = false;
  bool _revealed = false;
  final List<Offset> _scratches = [];
  final Set<int> _cells = {};

  Future<void> _getCard() async {
    if (_busy || _result != null) return;
    setState(() => _busy = true);
    final result = await playOnServer(context, GameType.scratchCard, widget.restaurantId);
    if (!mounted) return;
    setState(() {
      _busy = false;
      _result = result;
    });
  }

  void _scratch(Offset position) {
    if (_revealed || _result == null) return;
    final dx = position.dx.clamp(0, _cardSize.width - 1);
    final dy = position.dy.clamp(0, _cardSize.height - 1);
    _cells.add((dy ~/ (_cardSize.height / _rows)) * _cols + (dx ~/ (_cardSize.width / _cols)));
    setState(() => _scratches.add(Offset(dx.toDouble(), dy.toDouble())));

    // Once about half of the card is scratched, reveal the rest.
    if (_cells.length >= _cols * _rows * 0.45) _reveal();
  }

  Future<void> _reveal() async {
    if (_revealed) return;
    setState(() => _revealed = true);
    await Future<void>.delayed(const Duration(milliseconds: 700));
    if (!mounted || _result == null) return;
    await showWinDialog(context, _result!);
  }

  @override
  Widget build(BuildContext context) {
    final result = _result;
    return Scaffold(
      backgroundColor: AppColors.background,
      appBar: AppBar(title: const Text('Scratch Card')),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(20),
        child: Column(
          children: [
            GameHeader(
              type: GameType.scratchCard,
              restaurantName: widget.restaurantName,
              instruction: 'Scratch the silver area with your finger or mouse.',
            ),
            const SizedBox(height: 28),
            SizedBox(
              width: _cardSize.width,
              height: _cardSize.height,
              child: ClipRRect(
                borderRadius: BorderRadius.circular(20),
                child: Stack(
                  fit: StackFit.expand,
                  children: [
                    Container(
                      decoration: const BoxDecoration(
                        gradient: LinearGradient(colors: [Color(0xFFFFF3D6), Color(0xFFFFD98E)]),
                      ),
                      child: Center(
                        child: result == null
                            ? const Icon(Icons.card_giftcard, size: 60, color: AppColors.accent)
                            : Column(
                                mainAxisSize: MainAxisSize.min,
                                children: [
                                  const Text('YOU WON',
                                      style: TextStyle(
                                          fontSize: 14, letterSpacing: 2, fontWeight: FontWeight.w800)),
                                  Text('${result.pointsEarned}',
                                      style: const TextStyle(fontSize: 64, fontWeight: FontWeight.w900)),
                                  const Text('points', style: TextStyle(fontSize: 16)),
                                ],
                              ),
                      ),
                    ),
                    if (result != null)
                      IgnorePointer(
                        ignoring: _revealed,
                        child: AnimatedOpacity(
                          duration: const Duration(milliseconds: 500),
                          opacity: _revealed ? 0 : 1,
                          child: GestureDetector(
                            onPanStart: (d) => _scratch(d.localPosition),
                            onPanUpdate: (d) => _scratch(d.localPosition),
                            onTapDown: (d) => _scratch(d.localPosition),
                            child: CustomPaint(
                              size: _cardSize,
                              painter: _ScratchPainter(List.of(_scratches)),
                            ),
                          ),
                        ),
                      ),
                  ],
                ),
              ),
            ),
            const SizedBox(height: 28),
            if (result == null)
              SizedBox(
                width: double.infinity,
                height: 54,
                child: ElevatedButton.icon(
                  onPressed: _busy ? null : _getCard,
                  icon: _busy
                      ? const SizedBox(
                          width: 18,
                          height: 18,
                          child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white),
                        )
                      : const Icon(Icons.local_activity),
                  label: Text(_busy ? 'Getting your card...' : 'Get my scratch card'),
                ),
              )
            else
              Text(
                _revealed ? 'Revealed!' : 'Keep scratching...',
                style: const TextStyle(fontSize: 15, color: AppColors.textSecondary),
              ),
          ],
        ),
      ),
    );
  }
}

class _ScratchPainter extends CustomPainter {
  final List<Offset> scratches;

  _ScratchPainter(this.scratches);

  @override
  void paint(Canvas canvas, Size size) {
    final rect = Offset.zero & size;
    canvas.saveLayer(rect, Paint());
    canvas.drawRect(
      rect,
      Paint()
        ..shader = const LinearGradient(
          colors: [Color(0xFFBDBDBD), Color(0xFFE0E0E0), Color(0xFF9E9E9E)],
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
        ).createShader(rect),
    );
    final label = TextPainter(
      text: const TextSpan(
        text: 'SCRATCH HERE',
        style: TextStyle(color: Colors.white70, fontSize: 22, fontWeight: FontWeight.w800, letterSpacing: 3),
      ),
      textDirection: TextDirection.ltr,
    )..layout();
    label.paint(canvas, Offset((size.width - label.width) / 2, (size.height - label.height) / 2));

    final eraser = Paint()..blendMode = BlendMode.clear;
    for (final point in scratches) {
      canvas.drawCircle(point, 24, eraser);
    }
    canvas.restore();
  }

  @override
  bool shouldRepaint(covariant _ScratchPainter oldDelegate) => oldDelegate.scratches.length != scratches.length;
}
