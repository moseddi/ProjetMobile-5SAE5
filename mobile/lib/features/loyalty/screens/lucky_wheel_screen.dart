import 'dart:math';
import 'package:flutter/material.dart';
import '../../../core/theme/app_colors.dart';
import '../../../models/loyalty.dart';
import '../widgets/game_widgets.dart';

/// Lucky Wheel: the server draws the reward, the wheel is only the animation.
class LuckyWheelScreen extends StatefulWidget {
  final String restaurantId;
  final String restaurantName;

  const LuckyWheelScreen({super.key, required this.restaurantId, required this.restaurantName});

  @override
  State<LuckyWheelScreen> createState() => _LuckyWheelScreenState();
}

class _LuckyWheelScreenState extends State<LuckyWheelScreen> with SingleTickerProviderStateMixin {
  // Values printed on the wheel; every value the server can draw appears at least once.
  static const _segments = [5, 10, 20, 30, 50, 100, 10, 20];
  static const _colors = [
    Color(0xFFFF9A3C),
    Color(0xFF2C2C2C),
    Color(0xFFFF5E62),
    Color(0xFFD4A574),
    Color(0xFF4CAF50),
    Color(0xFF7F7FD5),
    Color(0xFFE94E77),
    Color(0xFF42A5F5),
  ];

  late final AnimationController _controller;
  Animation<double> _rotation = const AlwaysStoppedAnimation(0);
  bool _busy = false;
  bool _done = false;

  @override
  void initState() {
    super.initState();
    _controller = AnimationController(vsync: this, duration: const Duration(seconds: 4));
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  Future<void> _spin() async {
    if (_busy || _done) return;
    setState(() => _busy = true);

    final result = await playOnServer(context, GameType.luckyWheel, widget.restaurantId);
    if (!mounted) return;
    if (result == null) {
      setState(() => _busy = false);
      return;
    }

    // Land on a segment showing exactly the points the server drew.
    final matches = [
      for (var i = 0; i < _segments.length; i++)
        if (_segments[i] == result.pointsEarned) i
    ];
    final index = matches.isEmpty ? 0 : matches[Random().nextInt(matches.length)];
    final segmentAngle = 2 * pi / _segments.length;
    final centerOfSegment = (index + 0.5) * segmentAngle;
    final target = 2 * pi * 5 + (2 * pi - centerOfSegment);

    _rotation = Tween<double>(begin: 0, end: target)
        .animate(CurvedAnimation(parent: _controller, curve: Curves.easeOutCubic));
    await _controller.forward(from: 0);

    if (!mounted) return;
    setState(() {
      _busy = false;
      _done = true;
    });
    await showWinDialog(context, result);
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColors.background,
      appBar: AppBar(title: const Text('Lucky Wheel')),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(20),
        child: Column(
          children: [
            GameHeader(
              type: GameType.luckyWheel,
              restaurantName: widget.restaurantName,
              instruction: 'Spin once a day and win points.',
            ),
            const SizedBox(height: 28),
            SizedBox(
              width: 300,
              height: 320,
              child: Stack(
                alignment: Alignment.topCenter,
                children: [
                  Positioned(
                    top: 20,
                    child: AnimatedBuilder(
                      animation: _controller,
                      builder: (context, child) => Transform.rotate(angle: _rotation.value, child: child),
                      child: SizedBox(
                        width: 290,
                        height: 290,
                        child: CustomPaint(painter: _WheelPainter(_segments, _colors)),
                      ),
                    ),
                  ),
                  const Icon(Icons.arrow_drop_down_circle, size: 40, color: AppColors.primary),
                ],
              ),
            ),
            const SizedBox(height: 24),
            SizedBox(
              width: double.infinity,
              height: 54,
              child: ElevatedButton.icon(
                onPressed: _busy || _done ? null : _spin,
                icon: _busy
                    ? const SizedBox(
                        width: 18,
                        height: 18,
                        child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white),
                      )
                    : const Icon(Icons.autorenew),
                label: Text(_done ? 'Played today' : _busy ? 'Spinning...' : 'Spin the wheel'),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _WheelPainter extends CustomPainter {
  final List<int> segments;
  final List<Color> colors;

  _WheelPainter(this.segments, this.colors);

  @override
  void paint(Canvas canvas, Size size) {
    final center = size.center(Offset.zero);
    final radius = size.width / 2;
    final rect = Rect.fromCircle(center: center, radius: radius);
    final segmentAngle = 2 * pi / segments.length;

    for (var i = 0; i < segments.length; i++) {
      final start = -pi / 2 + i * segmentAngle;
      canvas.drawArc(rect, start, segmentAngle, true, Paint()..color = colors[i % colors.length]);
      canvas.drawArc(
        rect,
        start,
        segmentAngle,
        true,
        Paint()
          ..style = PaintingStyle.stroke
          ..strokeWidth = 2
          ..color = Colors.white,
      );

      final labelAngle = start + segmentAngle / 2;
      final labelCenter = center + Offset(cos(labelAngle), sin(labelAngle)) * (radius * 0.68);
      final painter = TextPainter(
        text: TextSpan(
          text: '${segments[i]}',
          style: const TextStyle(color: Colors.white, fontSize: 20, fontWeight: FontWeight.w900),
        ),
        textDirection: TextDirection.ltr,
      )..layout();
      canvas.save();
      canvas.translate(labelCenter.dx, labelCenter.dy);
      canvas.rotate(labelAngle + pi / 2);
      painter.paint(canvas, Offset(-painter.width / 2, -painter.height / 2));
      canvas.restore();
    }

    canvas.drawCircle(center, 22, Paint()..color = Colors.white);
    canvas.drawCircle(center, 22, Paint()
      ..style = PaintingStyle.stroke
      ..strokeWidth = 3
      ..color = AppColors.primary);
  }

  @override
  bool shouldRepaint(covariant _WheelPainter oldDelegate) => false;
}
