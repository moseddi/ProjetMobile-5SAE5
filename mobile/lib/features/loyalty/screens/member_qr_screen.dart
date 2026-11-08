import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:provider/provider.dart';
import 'package:qr_flutter/qr_flutter.dart';
import '../../../core/theme/app_colors.dart';
import '../../../models/loyalty.dart';
import '../../../models/user.dart';
import '../../auth/providers/auth_provider.dart';
import '../providers/loyalty_provider.dart';

/// White rounded box holding a QR code, big enough to be scanned at the till.
class QrBox extends StatelessWidget {
  final String data;
  final double size;

  const QrBox({super.key, required this.data, this.size = 220});

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(18),
        border: Border.all(color: AppColors.border),
      ),
      child: QrImageView(
        data: data,
        version: QrVersions.auto,
        size: size,
        backgroundColor: Colors.white,
        eyeStyle: const QrEyeStyle(eyeShape: QrEyeShape.square, color: Color(0xFF1A1A1A)),
        dataModuleStyle: const QrDataModuleStyle(dataModuleShape: QrDataModuleShape.square, color: Color(0xFF1A1A1A)),
      ),
    );
  }
}

/// The client's personal QR code. It is the same for every restaurant and never changes:
/// the cashier scans it and the server returns who the client is and what they have.
class MemberQrScreen extends StatelessWidget {
  const MemberQrScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final user = context.watch<AuthProvider>().currentUser;
    final overview = context.watch<LoyaltyProvider>().overview;
    final code = overview?.memberCode ?? '';

    return Scaffold(
      backgroundColor: AppColors.background,
      appBar: AppBar(title: const Text('My QR code')),
      body: ListView(
        padding: const EdgeInsets.all(20),
        children: [
          _buildCard(context, user, code),
          const SizedBox(height: 22),
          if (overview != null) _buildPoints(overview),
        ],
      ),
    );
  }

  Widget _buildCard(BuildContext context, User? user, String code) {
    return Container(
      padding: const EdgeInsets.all(24),
      decoration: BoxDecoration(
        color: AppColors.surface,
        borderRadius: BorderRadius.circular(24),
        boxShadow: [BoxShadow(color: Colors.black.withOpacity(0.08), blurRadius: 18, offset: const Offset(0, 8))],
      ),
      child: Column(
        children: [
          Text(user?.name ?? '',
              style: const TextStyle(fontSize: 22, fontWeight: FontWeight.w900, color: AppColors.textPrimary)),
          const SizedBox(height: 4),
          const Text('HangOut member', style: TextStyle(color: AppColors.textSecondary)),
          const SizedBox(height: 20),
          if (code.isEmpty)
            const SizedBox(height: 220, child: Center(child: CircularProgressIndicator()))
          else
            QrBox(data: code),
          const SizedBox(height: 16),
          InkWell(
            onTap: code.isEmpty
                ? null
                : () {
                    Clipboard.setData(ClipboardData(text: code));
                    ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Code copied')));
                  },
            child: Padding(
              padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
              child: Row(
                mainAxisSize: MainAxisSize.min,
                children: [
                  Text(code, style: const TextStyle(fontSize: 18, fontWeight: FontWeight.w900, letterSpacing: 2)),
                  const SizedBox(width: 8),
                  const Icon(Icons.copy, size: 16, color: AppColors.textSecondary),
                ],
              ),
            ),
          ),
          const SizedBox(height: 14),
          Container(
            padding: const EdgeInsets.all(12),
            decoration: BoxDecoration(color: AppColors.background, borderRadius: BorderRadius.circular(12)),
            child: const Row(
              children: [
                Icon(Icons.lock_outline, size: 18, color: AppColors.accent),
                SizedBox(width: 10),
                Expanded(
                  child: Text(
                    'This is your only code. It is the same in every restaurant and never changes. '
                    'Show it at the cashier to use your rewards.',
                    style: TextStyle(fontSize: 12.5, height: 1.4, color: AppColors.textSecondary),
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildPoints(LoyaltyOverview overview) {
    final withPoints = overview.restaurants.where((r) => r.points > 0).toList();
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          children: [
            const Expanded(
              child: Text('Your points',
                  style: TextStyle(fontSize: 18, fontWeight: FontWeight.w800, color: AppColors.textPrimary)),
            ),
            Text('${overview.totalPoints} total', style: const TextStyle(fontWeight: FontWeight.w800)),
          ],
        ),
        const SizedBox(height: 12),
        if (withPoints.isEmpty)
          const Text('Play a daily game to start collecting points at a restaurant.',
              style: TextStyle(color: AppColors.textSecondary))
        else
          for (final r in withPoints)
            Container(
              margin: const EdgeInsets.only(bottom: 10),
              padding: const EdgeInsets.all(14),
              decoration: BoxDecoration(
                color: AppColors.surface,
                borderRadius: BorderRadius.circular(14),
                border: Border.all(color: AppColors.border),
              ),
              child: Row(
                children: [
                  const Icon(Icons.storefront, color: AppColors.accent),
                  const SizedBox(width: 12),
                  Expanded(child: Text(r.name, style: const TextStyle(fontWeight: FontWeight.w700))),
                  Text('${r.points} pts', style: const TextStyle(fontWeight: FontWeight.w900)),
                ],
              ),
            ),
      ],
    );
  }
}
