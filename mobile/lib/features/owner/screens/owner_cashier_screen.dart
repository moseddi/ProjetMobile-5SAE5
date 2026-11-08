import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import 'package:provider/provider.dart';
import '../../../core/theme/app_colors.dart';
import '../../../models/loyalty.dart';
import '../providers/cashier_provider.dart';
import '../providers/owner_provider.dart';

/// Cashier desk: type or paste the code under the client's QR (or the text a QR
/// reader returns), see who it is and validate the rewards they are entitled to.
class OwnerCashierScreen extends StatefulWidget {
  const OwnerCashierScreen({super.key});

  @override
  State<OwnerCashierScreen> createState() => _OwnerCashierScreenState();
}

class _OwnerCashierScreenState extends State<OwnerCashierScreen> {
  final _controller = TextEditingController();
  String? _busyVoucher;
  bool _togglingLoyalty = false;

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  Future<void> _validate(Voucher voucher, MemberLookup member) async {
    final confirm = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
        title: Text('Validate ${voucher.title}?'),
        content: Text('${member.name} gets: ${voucher.description}.\nThis can only be done once.'),
        actions: [
          TextButton(onPressed: () => Navigator.pop(ctx, false), child: const Text('Cancel')),
          ElevatedButton(
            onPressed: () => Navigator.pop(ctx, true),
            style: ElevatedButton.styleFrom(backgroundColor: AppColors.success, foregroundColor: Colors.white),
            child: const Text('Validate'),
          ),
        ],
      ),
    );
    if (confirm != true || !mounted) return;

    setState(() => _busyVoucher = voucher.id);
    final messenger = ScaffoldMessenger.of(context);
    try {
      await context.read<CashierProvider>().useVoucher(voucher.id);
      messenger.showSnackBar(SnackBar(
        backgroundColor: AppColors.success,
        content: Text('${voucher.title} validated for ${member.name}'),
      ));
    } catch (e) {
      messenger.showSnackBar(SnackBar(
        backgroundColor: AppColors.error,
        content: Text(e.toString().replaceAll('Exception: ', '')),
      ));
    } finally {
      if (mounted) setState(() => _busyVoucher = null);
    }
  }

  Future<void> _toggleLoyalty(bool enabled) async {
    if (!enabled) {
      final confirm = await showDialog<bool>(
        context: context,
        builder: (ctx) => AlertDialog(
          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
          title: const Text('Turn off loyalty rewards?'),
          content: const Text(
            'Clients will see that your restaurant has no rewards. They will not be able to earn or '
            'exchange points here. Points they already have are kept, and rewards they already bought '
            'stay valid at your cashier.',
          ),
          actions: [
            TextButton(onPressed: () => Navigator.pop(ctx, false), child: const Text('Cancel')),
            ElevatedButton(
              onPressed: () => Navigator.pop(ctx, true),
              style: ElevatedButton.styleFrom(backgroundColor: AppColors.error, foregroundColor: Colors.white),
              child: const Text('Turn off'),
            ),
          ],
        ),
      );
      if (confirm != true || !mounted) return;
    }

    setState(() => _togglingLoyalty = true);
    final messenger = ScaffoldMessenger.of(context);
    try {
      await context.read<OwnerProvider>().setLoyaltyEnabled(enabled);
      messenger.showSnackBar(SnackBar(
        content: Text(enabled ? 'Loyalty rewards are back on' : 'Loyalty rewards are off'),
      ));
    } catch (e) {
      messenger.showSnackBar(SnackBar(
        backgroundColor: AppColors.error,
        content: Text(e.toString().replaceAll('Exception: ', '')),
      ));
    } finally {
      if (mounted) setState(() => _togglingLoyalty = false);
    }
  }

  Widget _buildLoyaltySwitch() {
    final restaurant = context.watch<OwnerProvider>().restaurant;
    if (restaurant == null) return const SizedBox.shrink();
    final enabled = restaurant.loyaltyEnabled;

    return Container(
      margin: const EdgeInsets.only(bottom: 16),
      padding: const EdgeInsets.fromLTRB(16, 12, 8, 12),
      decoration: BoxDecoration(
        color: AppColors.surface,
        borderRadius: BorderRadius.circular(18),
        border: Border.all(color: enabled ? AppColors.success.withOpacity(0.5) : AppColors.border),
      ),
      child: Row(
        children: [
          Container(
            padding: const EdgeInsets.all(10),
            decoration: BoxDecoration(
              color: (enabled ? AppColors.success : AppColors.textHint).withOpacity(0.15),
              borderRadius: BorderRadius.circular(12),
            ),
            child: Icon(Icons.card_giftcard, color: enabled ? AppColors.success : AppColors.textSecondary),
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Text('Loyalty rewards', style: TextStyle(fontSize: 15, fontWeight: FontWeight.w800)),
                const SizedBox(height: 2),
                Text(
                  enabled
                      ? 'On: clients earn points here and exchange them for rewards.'
                      : 'Off: clients see that you offer no rewards.',
                  style: const TextStyle(fontSize: 12.5, color: AppColors.textSecondary),
                ),
              ],
            ),
          ),
          _togglingLoyalty
              ? const Padding(
                  padding: EdgeInsets.symmetric(horizontal: 14),
                  child: SizedBox(width: 20, height: 20, child: CircularProgressIndicator(strokeWidth: 2)),
                )
              : Switch(
                  value: enabled,
                  activeColor: AppColors.success,
                  onChanged: _toggleLoyalty,
                ),
        ],
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final cashier = context.watch<CashierProvider>();
    final member = cashier.member;

    return Scaffold(
      backgroundColor: AppColors.background,
      appBar: AppBar(title: const Text('Cashier')),
      body: ListView(
        padding: const EdgeInsets.all(20),
        children: [
          _buildLoyaltySwitch(),
          Container(
            padding: const EdgeInsets.all(18),
            decoration: BoxDecoration(
              gradient: const LinearGradient(colors: [AppColors.primary, Color(0xFF444444)]),
              borderRadius: BorderRadius.circular(20),
            ),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Row(
                  children: [
                    Icon(Icons.qr_code_scanner, color: AppColors.accentLight),
                    SizedBox(width: 10),
                    Text('Client QR code',
                        style: TextStyle(fontSize: 18, fontWeight: FontWeight.w800, color: Colors.white)),
                  ],
                ),
                const SizedBox(height: 6),
                Text('Scan the QR with your reader, or type the code printed under it.',
                    style: TextStyle(fontSize: 13, color: Colors.white.withOpacity(0.8))),
                const SizedBox(height: 14),
                TextField(
                  controller: _controller,
                  textCapitalization: TextCapitalization.characters,
                  textInputAction: TextInputAction.search,
                  onSubmitted: (v) => cashier.lookup(v),
                  style: const TextStyle(fontWeight: FontWeight.w800, letterSpacing: 1.5),
                  decoration: InputDecoration(
                    hintText: 'HOM-XXXXXXXXXX',
                    filled: true,
                    fillColor: Colors.white,
                    prefixIcon: const Icon(Icons.badge_outlined),
                    suffixIcon: _controller.text.isEmpty
                        ? null
                        : IconButton(
                            icon: const Icon(Icons.close),
                            onPressed: () {
                              _controller.clear();
                              cashier.clear();
                              setState(() {});
                            },
                          ),
                    border: OutlineInputBorder(borderRadius: BorderRadius.circular(14), borderSide: BorderSide.none),
                  ),
                  onChanged: (_) => setState(() {}),
                ),
                const SizedBox(height: 12),
                SizedBox(
                  width: double.infinity,
                  height: 48,
                  child: ElevatedButton.icon(
                    onPressed: cashier.isLoading ? null : () => cashier.lookup(_controller.text),
                    style: ElevatedButton.styleFrom(
                      backgroundColor: AppColors.accent,
                      foregroundColor: AppColors.primary,
                    ),
                    icon: cashier.isLoading
                        ? const SizedBox(width: 18, height: 18, child: CircularProgressIndicator(strokeWidth: 2))
                        : const Icon(Icons.search),
                    label: const Text('Find client'),
                  ),
                ),
              ],
            ),
          ),
          const SizedBox(height: 20),
          if (cashier.error != null)
            Container(
              padding: const EdgeInsets.all(14),
              decoration: BoxDecoration(
                color: AppColors.error.withOpacity(0.08),
                borderRadius: BorderRadius.circular(14),
                border: Border.all(color: AppColors.error.withOpacity(0.4)),
              ),
              child: Row(
                children: [
                  const Icon(Icons.error_outline, color: AppColors.error),
                  const SizedBox(width: 10),
                  Expanded(child: Text(cashier.error!, style: const TextStyle(color: AppColors.error))),
                ],
              ),
            )
          else if (member != null)
            _buildMember(member),
        ],
      ),
    );
  }

  Widget _buildMember(MemberLookup member) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Container(
          padding: const EdgeInsets.all(18),
          decoration: BoxDecoration(
            color: AppColors.surface,
            borderRadius: BorderRadius.circular(20),
            border: Border.all(color: AppColors.border),
          ),
          child: Row(
            children: [
              CircleAvatar(
                radius: 30,
                backgroundColor: AppColors.accent,
                backgroundImage: member.avatar.isNotEmpty ? NetworkImage(member.avatar) : null,
                child: member.avatar.isEmpty
                    ? Text(member.name.isNotEmpty ? member.name[0].toUpperCase() : '?',
                        style: const TextStyle(fontSize: 22, fontWeight: FontWeight.w900, color: Colors.white))
                    : null,
              ),
              const SizedBox(width: 14),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(member.name, style: const TextStyle(fontSize: 18, fontWeight: FontWeight.w900)),
                    const SizedBox(height: 2),
                    Text(
                      member.memberSince != null
                          ? 'Member since ${DateFormat('MMM y').format(member.memberSince!.toLocal())}'
                          : 'HangOut member',
                      style: const TextStyle(fontSize: 13, color: AppColors.textSecondary),
                    ),
                    const SizedBox(height: 2),
                    Text(member.memberCode,
                        style: const TextStyle(fontSize: 12, letterSpacing: 1.5, fontWeight: FontWeight.w700)),
                  ],
                ),
              ),
            ],
          ),
        ),
        const SizedBox(height: 14),
        Row(
          children: [
            Expanded(child: _stat(Icons.stars, '${member.pointsAtRestaurant}', 'points at ${member.restaurantName}')),
            const SizedBox(width: 12),
            Expanded(child: _stat(Icons.check_circle_outline, '${member.usedVouchers}', 'rewards already used here')),
          ],
        ),
        const SizedBox(height: 22),
        const Text('Rewards to validate',
            style: TextStyle(fontSize: 18, fontWeight: FontWeight.w800, color: AppColors.textPrimary)),
        const SizedBox(height: 12),
        if (member.activeVouchers.isEmpty)
          Container(
            width: double.infinity,
            padding: const EdgeInsets.all(18),
            decoration: BoxDecoration(
              color: AppColors.surface,
              borderRadius: BorderRadius.circular(16),
              border: Border.all(color: AppColors.border),
            ),
            child: const Text('This client has no active reward at your restaurant.',
                style: TextStyle(color: AppColors.textSecondary)),
          )
        else
          for (final v in member.activeVouchers) _voucherTile(v, member),
      ],
    );
  }

  Widget _stat(IconData icon, String value, String label) {
    return Container(
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: AppColors.surface,
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: AppColors.border),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Icon(icon, color: AppColors.accent),
          const SizedBox(height: 8),
          Text(value, style: const TextStyle(fontSize: 24, fontWeight: FontWeight.w900)),
          Text(label, style: const TextStyle(fontSize: 12, color: AppColors.textSecondary)),
        ],
      ),
    );
  }

  Widget _voucherTile(Voucher v, MemberLookup member) {
    final busy = _busyVoucher == v.id;
    return Container(
      margin: const EdgeInsets.only(bottom: 12),
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: AppColors.surface,
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: AppColors.success.withOpacity(0.5)),
      ),
      child: Row(
        children: [
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(v.title, style: const TextStyle(fontSize: 16, fontWeight: FontWeight.w800)),
                const SizedBox(height: 2),
                Text(v.description, style: const TextStyle(fontSize: 12.5, color: AppColors.textSecondary)),
                const SizedBox(height: 4),
                Text(
                  '${v.code}${v.expiresAt != null ? ' • until ${DateFormat('MMM d').format(v.expiresAt!.toLocal())}' : ''}',
                  style: const TextStyle(fontSize: 12, fontWeight: FontWeight.w700),
                ),
              ],
            ),
          ),
          const SizedBox(width: 12),
          ElevatedButton(
            onPressed: busy ? null : () => _validate(v, member),
            style: ElevatedButton.styleFrom(backgroundColor: AppColors.success, foregroundColor: Colors.white),
            child: busy
                ? const SizedBox(
                    width: 16, height: 16, child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white))
                : const Text('Validate'),
          ),
        ],
      ),
    );
  }
}
