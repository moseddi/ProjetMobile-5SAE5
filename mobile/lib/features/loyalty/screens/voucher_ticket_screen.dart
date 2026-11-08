import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import 'package:provider/provider.dart';
import '../../../core/theme/app_colors.dart';
import '../../../models/loyalty.dart';
import '../providers/loyalty_provider.dart';
import 'member_qr_screen.dart';

/// The ticket given after an exchange: the reward, plus the client's permanent QR
/// to show at the cashier, who then validates the reward.
class VoucherTicketScreen extends StatelessWidget {
  final Voucher voucher;

  const VoucherTicketScreen({super.key, required this.voucher});

  @override
  Widget build(BuildContext context) {
    final code = context.watch<LoyaltyProvider>().overview?.memberCode ?? '';
    // Use the freshest copy of this voucher (it may have been validated meanwhile).
    final current = context.watch<LoyaltyProvider>().vouchers.where((v) => v.id == voucher.id).firstOrNull ?? voucher;

    final Color statusColor = current.isActive
        ? AppColors.success
        : current.status == 'USED'
            ? AppColors.textSecondary
            : AppColors.error;
    final String statusText = current.isActive
        ? 'Valid'
        : current.status == 'USED'
            ? 'Already used'
            : 'Expired';

    return Scaffold(
      backgroundColor: AppColors.primary,
      appBar: AppBar(
        title: const Text('Your ticket'),
        backgroundColor: AppColors.primary,
        foregroundColor: Colors.white,
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.fromLTRB(20, 8, 20, 32),
        child: Column(
          children: [
            ClipRRect(
              borderRadius: BorderRadius.circular(24),
              child: Container(
                color: AppColors.surface,
                child: Column(
                  children: [
                    _buildTop(current, statusColor, statusText),
                    const _TearLine(),
                    _buildBottom(context, current, code),
                  ],
                ),
              ),
            ),
            const SizedBox(height: 18),
            Text(
              'Show this ticket to the cashier. They scan your QR and validate the reward.',
              textAlign: TextAlign.center,
              style: TextStyle(fontSize: 13.5, height: 1.4, color: Colors.white.withOpacity(0.85)),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildTop(Voucher v, Color statusColor, String statusText) {
    return Padding(
      padding: const EdgeInsets.fromLTRB(24, 26, 24, 22),
      child: Column(
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              const Text('HANGOUT REWARD',
                  style: TextStyle(fontSize: 12, letterSpacing: 2, fontWeight: FontWeight.w800, color: AppColors.accent)),
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                decoration: BoxDecoration(
                  color: statusColor.withOpacity(0.14),
                  borderRadius: BorderRadius.circular(12),
                ),
                child: Text(statusText,
                    style: TextStyle(fontSize: 12, fontWeight: FontWeight.w800, color: statusColor)),
              ),
            ],
          ),
          const SizedBox(height: 18),
          Text(v.title,
              textAlign: TextAlign.center,
              style: const TextStyle(fontSize: 28, fontWeight: FontWeight.w900, color: AppColors.textPrimary)),
          const SizedBox(height: 6),
          Text(v.description,
              textAlign: TextAlign.center,
              style: const TextStyle(fontSize: 14, color: AppColors.textSecondary)),
          const SizedBox(height: 18),
          Row(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              const Icon(Icons.storefront, size: 18, color: AppColors.accent),
              const SizedBox(width: 6),
              Flexible(
                child: Text(v.restaurantName,
                    overflow: TextOverflow.ellipsis,
                    style: const TextStyle(fontSize: 16, fontWeight: FontWeight.w800)),
              ),
            ],
          ),
          const SizedBox(height: 14),
          Row(
            children: [
              _info('Reward code', v.code),
              _info('Cost', '${v.pointsSpent} pts'),
              _info(
                v.isActive ? 'Valid until' : 'Expiry',
                v.expiresAt != null ? DateFormat('MMM d, y').format(v.expiresAt!.toLocal()) : '-',
              ),
            ],
          ),
        ],
      ),
    );
  }

  Widget _info(String label, String value) {
    return Expanded(
      child: Column(
        children: [
          Text(label, style: const TextStyle(fontSize: 11, color: AppColors.textSecondary)),
          const SizedBox(height: 3),
          Text(value,
              textAlign: TextAlign.center,
              style: const TextStyle(fontSize: 12.5, fontWeight: FontWeight.w800)),
        ],
      ),
    );
  }

  Widget _buildBottom(BuildContext context, Voucher v, String code) {
    return Padding(
      padding: const EdgeInsets.fromLTRB(24, 22, 24, 28),
      child: Column(
        children: [
          if (code.isEmpty)
            const SizedBox(height: 220, child: Center(child: CircularProgressIndicator()))
          else
            Opacity(opacity: v.isActive ? 1 : 0.35, child: QrBox(data: code, size: 200)),
          const SizedBox(height: 12),
          Text(code, style: const TextStyle(fontSize: 16, fontWeight: FontWeight.w900, letterSpacing: 2)),
          const SizedBox(height: 4),
          const Text('Your personal QR • the same in every restaurant',
              style: TextStyle(fontSize: 12, color: AppColors.textSecondary)),
        ],
      ),
    );
  }
}

/// Dashed tear line with a half-circle notch on each side (circles in the page colour,
/// cut by the ticket's rounded clip).
class _TearLine extends StatelessWidget {
  const _TearLine();

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      height: 28,
      child: Stack(
        clipBehavior: Clip.none,
        alignment: Alignment.center,
        children: [
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 26),
            child: LayoutBuilder(
              builder: (context, constraints) {
                final count = (constraints.maxWidth / 12).floor();
                return Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: List.generate(count, (_) => Container(width: 6, height: 2, color: AppColors.border)),
                );
              },
            ),
          ),
          Positioned(left: -14, child: _notch()),
          Positioned(right: -14, child: _notch()),
        ],
      ),
    );
  }

  Widget _notch() => Container(
        width: 28,
        height: 28,
        decoration: const BoxDecoration(color: AppColors.primary, shape: BoxShape.circle),
      );
}
