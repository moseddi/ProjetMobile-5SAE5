import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../../core/navigation/app_router.dart';
import '../../../core/theme/app_colors.dart';
import '../../../models/recommendation.dart';
import '../../auth/providers/auth_provider.dart';
import '../../loyalty/providers/loyalty_provider.dart';
import '../../loyalty/screens/voucher_ticket_screen.dart';
import '../../restaurants/providers/restaurant_provider.dart';
import '../providers/recommendation_provider.dart';

/// "Where should I use my points?": suggestions based on the client's reservations,
/// the places visited and their points per restaurant.
class RecommendationsScreen extends StatefulWidget {
  const RecommendationsScreen({super.key});

  @override
  State<RecommendationsScreen> createState() => _RecommendationsScreenState();
}

class _RecommendationsScreenState extends State<RecommendationsScreen> {
  String? _redeeming;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      final provider = context.read<RecommendationProvider>();
      provider.clear();
      provider.load();
    });
  }

  Future<void> _exchange(Suggestion s) async {
    final confirm = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
        title: Text(s.rewardTitle),
        content: Text('Spend ${s.cost} of your ${s.restaurantPoints} points at ${s.restaurantName}?'),
        actions: [
          TextButton(onPressed: () => Navigator.pop(ctx, false), child: const Text('Cancel')),
          ElevatedButton(onPressed: () => Navigator.pop(ctx, true), child: const Text('Exchange')),
        ],
      ),
    );
    if (confirm != true || !mounted) return;

    final key = '${s.restaurantId}|${s.rewardType}';
    setState(() => _redeeming = key);
    final messenger = ScaffoldMessenger.of(context);
    final loyalty = context.read<LoyaltyProvider>();
    final auth = context.read<AuthProvider>();
    final recommendations = context.read<RecommendationProvider>();
    try {
      final voucher = await loyalty.redeem(s.restaurantId, s.rewardType);
      final user = auth.currentUser;
      if (user != null && loyalty.totalPoints != null) {
        auth.updateUser(user.copyWith(points: loyalty.totalPoints));
      }
      if (!mounted) return;
      await Navigator.of(context).push(MaterialPageRoute(builder: (_) => VoucherTicketScreen(voucher: voucher)));
      if (mounted) recommendations.load();
    } catch (e) {
      messenger.showSnackBar(SnackBar(
        backgroundColor: AppColors.error,
        content: Text(e.toString().replaceAll('Exception: ', '')),
      ));
    } finally {
      if (mounted) setState(() => _redeeming = null);
    }
  }

  void _openRestaurant(Suggestion s) {
    final restaurants = context.read<RestaurantProvider>().restaurants;
    final match = restaurants.where((r) => r.id == s.restaurantId).firstOrNull;
    if (match == null) {
      ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Restaurant not available right now')));
      return;
    }
    Navigator.of(context).pushNamed(AppRouter.restaurantDetails, arguments: match);
  }

  @override
  Widget build(BuildContext context) {
    final provider = context.watch<RecommendationProvider>();
    final result = provider.result;

    return Scaffold(
      backgroundColor: AppColors.background,
      appBar: AppBar(
        title: const Text('Smart suggestions'),
        actions: [
          IconButton(
            tooltip: 'Refresh',
            onPressed: provider.isLoading ? null : provider.load,
            icon: const Icon(Icons.refresh),
          ),
        ],
      ),
      body: provider.isLoading && result == null
          ? _buildLoading()
          : provider.error != null && result == null
              ? _buildError(provider)
              : result == null
                  ? const SizedBox.shrink()
                  : _buildContent(result, provider.isLoading),
    );
  }

  Widget _buildLoading() {
    return const Center(
      child: Padding(
        padding: EdgeInsets.all(32),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            SizedBox(width: 56, height: 56, child: CircularProgressIndicator(strokeWidth: 4)),
            SizedBox(height: 22),
            Text('Analysing your reservations and points...',
                textAlign: TextAlign.center, style: TextStyle(fontSize: 16, fontWeight: FontWeight.w700)),
            SizedBox(height: 6),
            Text('This takes a few seconds.', style: TextStyle(color: AppColors.textSecondary)),
          ],
        ),
      ),
    );
  }

  Widget _buildError(RecommendationProvider provider) {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(32),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            const Icon(Icons.wifi_off, size: 56, color: AppColors.textHint),
            const SizedBox(height: 14),
            Text(provider.error ?? 'Could not load suggestions',
                textAlign: TextAlign.center, style: const TextStyle(color: AppColors.textSecondary)),
            const SizedBox(height: 18),
            ElevatedButton(onPressed: provider.load, child: const Text('Try again')),
          ],
        ),
      ),
    );
  }

  Widget _buildContent(Recommendation result, bool refreshing) {
    return ListView(
      padding: const EdgeInsets.fromLTRB(20, 8, 20, 40),
      children: [
        if (refreshing) const LinearProgressIndicator(),
        _buildSummary(result),
        if (result.notice != null) ...[
          const SizedBox(height: 12),
          Container(
            padding: const EdgeInsets.all(12),
            decoration: BoxDecoration(
              color: AppColors.info.withOpacity(0.08),
              borderRadius: BorderRadius.circular(12),
              border: Border.all(color: AppColors.info.withOpacity(0.3)),
            ),
            child: Row(
              children: [
                const Icon(Icons.info_outline, size: 18, color: AppColors.info),
                const SizedBox(width: 10),
                Expanded(
                  child: Text(result.notice!,
                      style: const TextStyle(fontSize: 12.5, color: AppColors.textSecondary)),
                ),
              ],
            ),
          ),
        ],
        const SizedBox(height: 20),
        if (result.suggestions.isEmpty)
          const Padding(
            padding: EdgeInsets.only(top: 40),
            child: Center(
              child: Text('Nothing to suggest yet. Book a table or play today\'s game first.',
                  textAlign: TextAlign.center, style: TextStyle(color: AppColors.textSecondary)),
            ),
          )
        else
          for (final s in result.suggestions) _buildSuggestion(s),
      ],
    );
  }

  Widget _buildSummary(Recommendation result) {
    return Container(
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        gradient: const LinearGradient(
          colors: [Color(0xFF4F46E5), Color(0xFF9333EA)],
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
        ),
        borderRadius: BorderRadius.circular(22),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              const Icon(Icons.auto_awesome, color: Colors.white),
              const SizedBox(width: 8),
              const Expanded(
                child: Text('For you',
                    style: TextStyle(fontSize: 18, fontWeight: FontWeight.w800, color: Colors.white)),
              ),
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                decoration: BoxDecoration(
                  color: Colors.white.withOpacity(0.22),
                  borderRadius: BorderRadius.circular(12),
                ),
                child: Text(result.isGemini ? 'Powered by Gemini' : 'Smart rules',
                    style: const TextStyle(fontSize: 11.5, fontWeight: FontWeight.w700, color: Colors.white)),
              ),
            ],
          ),
          const SizedBox(height: 12),
          Text(result.summary,
              style: TextStyle(fontSize: 15, height: 1.4, color: Colors.white.withOpacity(0.95))),
        ],
      ),
    );
  }

  ({IconData icon, Color color, String label}) _style(SuggestionType type) {
    switch (type) {
      case SuggestionType.exchange:
        return (icon: Icons.redeem, color: AppColors.success, label: 'Exchange now');
      case SuggestionType.save:
        return (icon: Icons.savings_outlined, color: AppColors.warning, label: 'Almost there');
      case SuggestionType.visit:
        return (icon: Icons.explore_outlined, color: AppColors.info, label: 'Try a new place');
      case SuggestionType.play:
        return (icon: Icons.casino_outlined, color: const Color(0xFFB83280), label: 'Daily game');
    }
  }

  Widget _buildSuggestion(Suggestion s) {
    final style = _style(s.type);
    final key = '${s.restaurantId}|${s.rewardType}';
    final busy = _redeeming == key;

    return Container(
      margin: const EdgeInsets.only(bottom: 14),
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: AppColors.surface,
        borderRadius: BorderRadius.circular(20),
        border: Border.all(color: style.color.withOpacity(0.35)),
        boxShadow: [BoxShadow(color: Colors.black.withOpacity(0.04), blurRadius: 10, offset: const Offset(0, 4))],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Container(
                padding: const EdgeInsets.all(8),
                decoration: BoxDecoration(color: style.color.withOpacity(0.14), shape: BoxShape.circle),
                child: Icon(style.icon, size: 20, color: style.color),
              ),
              const SizedBox(width: 10),
              Text(style.label.toUpperCase(),
                  style: TextStyle(fontSize: 11.5, letterSpacing: 1, fontWeight: FontWeight.w800, color: style.color)),
            ],
          ),
          const SizedBox(height: 10),
          Text(s.title, style: const TextStyle(fontSize: 17, fontWeight: FontWeight.w800, height: 1.25)),
          const SizedBox(height: 6),
          Text(s.reason, style: const TextStyle(fontSize: 14, height: 1.4, color: AppColors.textSecondary)),
          if (s.restaurantName.isNotEmpty) ...[
            const SizedBox(height: 10),
            Row(
              children: [
                const Icon(Icons.storefront, size: 16, color: AppColors.textSecondary),
                const SizedBox(width: 6),
                Expanded(
                  child: Text(s.restaurantName,
                      overflow: TextOverflow.ellipsis,
                      style: const TextStyle(fontSize: 13, fontWeight: FontWeight.w700)),
                ),
              ],
            ),
          ],
          if (s.hasReward && (s.type == SuggestionType.exchange || s.type == SuggestionType.save)) ...[
            const SizedBox(height: 12),
            ClipRRect(
              borderRadius: BorderRadius.circular(6),
              child: LinearProgressIndicator(
                value: s.cost == 0 ? 1 : (s.restaurantPoints / s.cost).clamp(0.0, 1.0),
                minHeight: 7,
                backgroundColor: AppColors.divider,
                valueColor: AlwaysStoppedAnimation<Color>(s.canRedeem ? AppColors.success : AppColors.accent),
              ),
            ),
            const SizedBox(height: 4),
            Text(
              s.canRedeem
                  ? '${s.rewardTitle}: ${s.cost} pts • you have ${s.restaurantPoints}'
                  : '${s.rewardTitle}: ${s.cost} pts • you have ${s.restaurantPoints}, ${s.missingPoints} more needed',
              style: TextStyle(
                fontSize: 12,
                fontWeight: FontWeight.w600,
                color: s.canRedeem ? AppColors.success : AppColors.textSecondary,
              ),
            ),
          ],
          const SizedBox(height: 14),
          _buildAction(s, style.color, busy),
        ],
      ),
    );
  }

  Widget _buildAction(Suggestion s, Color color, bool busy) {
    switch (s.type) {
      case SuggestionType.exchange:
        return SizedBox(
          width: double.infinity,
          height: 46,
          child: ElevatedButton.icon(
            onPressed: busy || _redeeming != null ? null : () => _exchange(s),
            style: ElevatedButton.styleFrom(backgroundColor: color, foregroundColor: Colors.white),
            icon: busy
                ? const SizedBox(width: 16, height: 16, child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white))
                : const Icon(Icons.redeem, size: 18),
            label: Text('Exchange ${s.cost} points'),
          ),
        );
      case SuggestionType.save:
        return SizedBox(
          width: double.infinity,
          height: 46,
          child: OutlinedButton.icon(
            onPressed: () => _openRestaurant(s),
            icon: const Icon(Icons.storefront_outlined, size: 18),
            label: const Text('See the restaurant'),
          ),
        );
      case SuggestionType.visit:
        return SizedBox(
          width: double.infinity,
          height: 46,
          child: ElevatedButton.icon(
            onPressed: () => _openRestaurant(s),
            icon: const Icon(Icons.restaurant_menu, size: 18),
            label: const Text('View restaurant'),
          ),
        );
      case SuggestionType.play:
        return SizedBox(
          width: double.infinity,
          height: 46,
          child: OutlinedButton.icon(
            onPressed: () => Navigator.of(context).pop(),
            icon: const Icon(Icons.casino_outlined, size: 18),
            label: const Text('Go to the daily games'),
          ),
        );
    }
  }
}
