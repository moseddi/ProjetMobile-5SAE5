import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:intl/intl.dart';
import 'package:provider/provider.dart';
import '../../../core/constants/app_constants.dart';
import '../../../core/theme/app_colors.dart';
import '../../../models/loyalty.dart';
import '../../auth/providers/auth_provider.dart';
import '../../recommendations/screens/recommendations_screen.dart';
import '../providers/loyalty_provider.dart';
import '../widgets/game_widgets.dart';
import 'card_game_screen.dart';
import 'lucky_dice_screen.dart';
import 'lucky_wheel_screen.dart';
import 'member_qr_screen.dart';
import 'scratch_card_screen.dart';
import 'voucher_ticket_screen.dart';

class LoyaltyScreen extends StatefulWidget {
  const LoyaltyScreen({super.key});

  @override
  State<LoyaltyScreen> createState() => _LoyaltyScreenState();
}

class _LoyaltyScreenState extends State<LoyaltyScreen> {
  bool _onlyWithPoints = false;
  String? _redeeming; // "restaurantId|rewardType" being bought

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      // Never show another account's data while loading.
      context.read<LoyaltyProvider>().clear();
      _load();
    });
  }

  Future<void> _load() async {
    final loyalty = context.read<LoyaltyProvider>();
    await loyalty.load();
    if (!mounted) return;
    _syncPoints(loyalty.totalPoints);
  }

  /// Keeps the points shown elsewhere in the app equal to the server's total.
  void _syncPoints(int? total) {
    final auth = context.read<AuthProvider>();
    final user = auth.currentUser;
    if (total != null && user != null && user.points != total) {
      auth.updateUser(user.copyWith(points: total));
    }
  }

  // ------------------------------------------------------------------ games

  Future<void> _openGame(GameStatus status, LoyaltyOverview overview) async {
    if (status.playedToday || status.locked) {
      final played = overview.games.where((g) => g.playedToday).firstOrNull;
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(
        content: Text(played == null
            ? 'Only one game per day. Come back tomorrow!'
            : 'You already played ${played.title} today'
                '${played.lastPoints != null ? ' (+${played.lastPoints} points)' : ''}. Only one game per day: come back tomorrow!'),
      ));
      return;
    }

    final restaurant = await _pickRestaurant(overview.restaurants, status);
    if (restaurant == null || !mounted) return;

    Widget screen;
    switch (status.type) {
      case GameType.luckyWheel:
        screen = LuckyWheelScreen(restaurantId: restaurant.restaurantId, restaurantName: restaurant.name);
      case GameType.scratchCard:
        screen = ScratchCardScreen(restaurantId: restaurant.restaurantId, restaurantName: restaurant.name);
      case GameType.cardPick:
        screen = CardGameScreen(restaurantId: restaurant.restaurantId, restaurantName: restaurant.name);
      case GameType.luckyDice:
        screen = LuckyDiceScreen(restaurantId: restaurant.restaurantId, restaurantName: restaurant.name);
    }
    await Navigator.of(context).push(MaterialPageRoute(builder: (_) => screen));
    if (mounted) _load();
  }

  Future<RestaurantLoyalty?> _pickRestaurant(List<RestaurantLoyalty> restaurants, GameStatus status) {
    return showModalBottomSheet<RestaurantLoyalty>(
      context: context,
      isScrollControlled: true,
      backgroundColor: AppColors.surface,
      shape: const RoundedRectangleBorder(borderRadius: BorderRadius.vertical(top: Radius.circular(24))),
      builder: (_) => _RestaurantPicker(restaurants: restaurants, gameTitle: status.title),
    );
  }

  // ------------------------------------------------------------------ exchanges

  Future<void> _redeem(RestaurantLoyalty restaurant, RewardOffer offer) async {
    final confirm = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
        title: Text(offer.title),
        content: Text('Spend ${offer.cost} of your ${restaurant.points} points at ${restaurant.name}?\n\n'
            '${offer.description}.'),
        actions: [
          TextButton(onPressed: () => Navigator.pop(ctx, false), child: const Text('Cancel')),
          ElevatedButton(onPressed: () => Navigator.pop(ctx, true), child: const Text('Exchange')),
        ],
      ),
    );
    if (confirm != true || !mounted) return;

    final key = '${restaurant.restaurantId}|${offer.type}';
    setState(() => _redeeming = key);
    final messenger = ScaffoldMessenger.of(context);
    final loyalty = context.read<LoyaltyProvider>();
    try {
      final voucher = await loyalty.redeem(restaurant.restaurantId, offer.type);
      if (!mounted) return;
      _syncPoints(loyalty.totalPoints);
      // The ticket carries the client's permanent QR for the cashier.
      await Navigator.of(context).push(MaterialPageRoute(builder: (_) => VoucherTicketScreen(voucher: voucher)));
    } catch (e) {
      messenger.showSnackBar(SnackBar(
        backgroundColor: AppColors.error,
        content: Text(e.toString().replaceAll('Exception: ', '')),
      ));
    } finally {
      if (mounted) setState(() => _redeeming = null);
    }
  }

  // ------------------------------------------------------------------ build

  @override
  Widget build(BuildContext context) {
    final user = context.watch<AuthProvider>().currentUser;
    final loyalty = context.watch<LoyaltyProvider>();
    if (user == null) {
      return const Scaffold(body: Center(child: Text('Please login to view loyalty rewards')));
    }

    final overview = loyalty.overview;
    final points = overview?.totalPoints ?? user.points;

    return Scaffold(
      backgroundColor: AppColors.background,
      appBar: AppBar(title: const Text('Loyalty & Rewards')),
      body: RefreshIndicator(
        onRefresh: _load,
        child: ListView(
          physics: const AlwaysScrollableScrollPhysics(),
          padding: const EdgeInsets.fromLTRB(20, 16, 20, 40),
          children: [
            _buildPointsCard(points, overview?.unassignedPoints ?? 0),
            if (overview != null) ...[
              const SizedBox(height: 14),
              _buildQrCard(overview),
              const SizedBox(height: 14),
              _buildAiCard(),
            ],
            const SizedBox(height: 26),
            if (overview == null && loyalty.isLoading)
              const Padding(padding: EdgeInsets.only(top: 60), child: Center(child: CircularProgressIndicator()))
            else if (overview == null)
              _buildError(loyalty.error ?? 'Could not load your rewards')
            else ...[
              _sectionTitle('Daily games', 'Pick one game per day to earn points'),
              const SizedBox(height: 14),
              _buildGameStrip(overview),
              const SizedBox(height: 28),
              _sectionTitle('Exchange your points',
                  'Points are earned per restaurant and spent at the same restaurant'),
              const SizedBox(height: 14),
              _buildCatalogue(overview),
              const SizedBox(height: 14),
              _buildRestaurantFilter(overview),
              const SizedBox(height: 14),
              ..._buildRestaurantCards(overview),
              const SizedBox(height: 28),
              _buildVouchers(loyalty.vouchers),
              const SizedBox(height: 28),
              _buildLevels(points),
            ],
          ],
        ),
      ),
    );
  }

  Widget _sectionTitle(String title, String subtitle) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(title, style: const TextStyle(fontSize: 20, fontWeight: FontWeight.w800, color: AppColors.textPrimary)),
        const SizedBox(height: 3),
        Text(subtitle, style: const TextStyle(fontSize: 13, color: AppColors.textSecondary)),
      ],
    );
  }

  Widget _buildError(String message) {
    return Padding(
      padding: const EdgeInsets.only(top: 50),
      child: Column(
        children: [
          const Icon(Icons.wifi_off, size: 56, color: AppColors.textHint),
          const SizedBox(height: 12),
          Text(message, textAlign: TextAlign.center, style: const TextStyle(color: AppColors.textSecondary)),
          const SizedBox(height: 16),
          ElevatedButton(onPressed: _load, child: const Text('Retry')),
        ],
      ),
    );
  }

  // ---- points card

  String _levelFor(int points) {
    String level = 'Bronze';
    AppConstants.loyaltyLevels.forEach((name, min) {
      if (points >= min) level = name;
    });
    return level;
  }

  int _nextThreshold(int points) {
    final thresholds = AppConstants.loyaltyLevels.values.toList()..sort();
    for (final t in thresholds) {
      if (points < t) return t;
    }
    return thresholds.last;
  }

  Widget _buildPointsCard(int points, int unassigned) {
    final level = _levelFor(points);
    final next = _nextThreshold(points);
    final progress = next == 0 ? 1.0 : (points / next).clamp(0.0, 1.0);

    return Container(
      padding: const EdgeInsets.all(24),
      decoration: BoxDecoration(
        gradient: const LinearGradient(
          colors: [AppColors.accent, AppColors.accentLight],
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
        ),
        borderRadius: BorderRadius.circular(20),
        boxShadow: [BoxShadow(color: AppColors.accent.withOpacity(0.3), blurRadius: 10, offset: const Offset(0, 5))],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              const Text('Your Points',
                  style: TextStyle(fontSize: 16, color: AppColors.textPrimary, fontWeight: FontWeight.w600)),
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
                decoration: BoxDecoration(color: Colors.white.withOpacity(0.35), borderRadius: BorderRadius.circular(20)),
                child: Text(level,
                    style: const TextStyle(fontSize: 12, fontWeight: FontWeight.bold, color: AppColors.textPrimary)),
              ),
            ],
          ),
          const SizedBox(height: 6),
          Text('$points',
              style: const TextStyle(fontSize: 52, fontWeight: FontWeight.w900, color: AppColors.textPrimary)),
          const SizedBox(height: 10),
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Text('Next level', style: TextStyle(fontSize: 13, color: AppColors.textPrimary.withOpacity(0.8))),
              Text('$points / $next',
                  style: TextStyle(fontSize: 13, fontWeight: FontWeight.w700, color: AppColors.textPrimary.withOpacity(0.8))),
            ],
          ),
          const SizedBox(height: 8),
          ClipRRect(
            borderRadius: BorderRadius.circular(10),
            child: LinearProgressIndicator(
              value: progress,
              minHeight: 8,
              backgroundColor: Colors.white.withOpacity(0.35),
              valueColor: const AlwaysStoppedAnimation<Color>(AppColors.textPrimary),
            ),
          ),
          if (unassigned > 0) ...[
            const SizedBox(height: 12),
            Text(
              '$unassigned of these points come from before restaurants were tracked, so they count for your level but cannot be exchanged.',
              style: TextStyle(fontSize: 11.5, height: 1.35, color: AppColors.textPrimary.withOpacity(0.75)),
            ),
          ],
        ],
      ),
    );
  }

  // ---- AI recommendations

  Widget _buildAiCard() {
    return Material(
      borderRadius: BorderRadius.circular(18),
      child: InkWell(
        borderRadius: BorderRadius.circular(18),
        onTap: () => Navigator.of(context).push(MaterialPageRoute(builder: (_) => const RecommendationsScreen())),
        child: Ink(
          padding: const EdgeInsets.all(16),
          decoration: BoxDecoration(
            gradient: const LinearGradient(
              colors: [Color(0xFF4F46E5), Color(0xFF9333EA)],
              begin: Alignment.topLeft,
              end: Alignment.bottomRight,
            ),
            borderRadius: BorderRadius.circular(18),
            boxShadow: [BoxShadow(color: const Color(0xFF4F46E5).withOpacity(0.3), blurRadius: 12, offset: const Offset(0, 6))],
          ),
          child: Row(
            children: [
              Container(
                padding: const EdgeInsets.all(10),
                decoration: BoxDecoration(color: Colors.white.withOpacity(0.2), borderRadius: BorderRadius.circular(12)),
                child: const Icon(Icons.auto_awesome, color: Colors.white, size: 26),
              ),
              const SizedBox(width: 14),
              const Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text('Recommend me', style: TextStyle(fontSize: 16, fontWeight: FontWeight.w800, color: Colors.white)),
                    SizedBox(height: 2),
                    Text('Where to use my points, based on my reservations',
                        style: TextStyle(fontSize: 12.5, color: Colors.white70)),
                  ],
                ),
              ),
              const Icon(Icons.chevron_right, color: Colors.white),
            ],
          ),
        ),
      ),
    );
  }

  // ---- member QR

  Widget _buildQrCard(LoyaltyOverview overview) {
    return Material(
      color: AppColors.surface,
      borderRadius: BorderRadius.circular(18),
      child: InkWell(
        borderRadius: BorderRadius.circular(18),
        onTap: () => Navigator.of(context).push(MaterialPageRoute(builder: (_) => const MemberQrScreen())),
        child: Container(
          padding: const EdgeInsets.all(14),
          decoration: BoxDecoration(
            borderRadius: BorderRadius.circular(18),
            border: Border.all(color: AppColors.border),
          ),
          child: Row(
            children: [
              Container(
                padding: const EdgeInsets.all(10),
                decoration: BoxDecoration(color: AppColors.primary, borderRadius: BorderRadius.circular(12)),
                child: const Icon(Icons.qr_code_2, color: Colors.white, size: 28),
              ),
              const SizedBox(width: 14),
              const Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text('My QR code', style: TextStyle(fontSize: 15, fontWeight: FontWeight.w800)),
                    SizedBox(height: 2),
                    Text('One code for every restaurant. Show it at the cashier.',
                        style: TextStyle(fontSize: 12.5, color: AppColors.textSecondary)),
                  ],
                ),
              ),
              const Icon(Icons.chevron_right, color: AppColors.textSecondary),
            ],
          ),
        ),
      ),
    );
  }

  // ---- games strip

  Widget _buildGameStrip(LoyaltyOverview overview) {
    return SizedBox(
      height: 172,
      child: ListView.separated(
        scrollDirection: Axis.horizontal,
        itemCount: overview.games.length,
        separatorBuilder: (_, __) => const SizedBox(width: 12),
        itemBuilder: (context, index) => _gameCard(overview.games[index], overview),
      ),
    );
  }

  Widget _gameCard(GameStatus status, LoyaltyOverview overview) {
    final style = GameStyle.of(status.type);
    final played = status.playedToday;
    final locked = status.locked;
    final grey = played || locked;

    return GestureDetector(
      onTap: () => _openGame(status, overview),
      child: Container(
        width: 142,
        padding: const EdgeInsets.all(14),
        decoration: BoxDecoration(
          gradient: LinearGradient(
            colors: grey ? [const Color(0xFFB0B0B0), const Color(0xFF8E8E8E)] : style.colors,
            begin: Alignment.topLeft,
            end: Alignment.bottomRight,
          ),
          borderRadius: BorderRadius.circular(20),
          boxShadow: [BoxShadow(color: Colors.black.withOpacity(0.1), blurRadius: 10, offset: const Offset(0, 5))],
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Container(
              padding: const EdgeInsets.all(8),
              decoration: BoxDecoration(color: Colors.white.withOpacity(0.25), shape: BoxShape.circle),
              child: Icon(played ? Icons.check : locked ? Icons.lock_outline : style.icon, color: Colors.white, size: 24),
            ),
            const Spacer(),
            Text(status.title,
                style: const TextStyle(fontSize: 15, fontWeight: FontWeight.w800, color: Colors.white)),
            const SizedBox(height: 2),
            Text(
              played
                  ? 'Played • +${status.lastPoints ?? 0} pts'
                  : locked
                      ? '1 game per day'
                      : status.description,
              maxLines: 1,
              overflow: TextOverflow.ellipsis,
              style: TextStyle(fontSize: 12, color: Colors.white.withOpacity(0.9)),
            ),
            const SizedBox(height: 8),
            Container(
              padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
              decoration: BoxDecoration(color: Colors.white.withOpacity(0.25), borderRadius: BorderRadius.circular(12)),
              child: Text(played || locked ? 'Back tomorrow' : 'Play now',
                  style: const TextStyle(fontSize: 11.5, fontWeight: FontWeight.w800, color: Colors.white)),
            ),
          ],
        ),
      ),
    );
  }

  // ---- exchanges

  Widget _buildCatalogue(LoyaltyOverview overview) {
    final offers = overview.restaurants.isNotEmpty ? overview.restaurants.first.offers : const <RewardOffer>[];
    if (offers.isEmpty) return const SizedBox.shrink();

    const icons = {
      'DISCOUNT_10': Icons.percent,
      'FREE_COFFEE': Icons.coffee,
      'FREE_EVENT_ENTRY': Icons.confirmation_number,
    };

    return Row(
      children: [
        for (var i = 0; i < offers.length; i++) ...[
          Expanded(
            child: Container(
              padding: const EdgeInsets.symmetric(vertical: 14, horizontal: 8),
              decoration: BoxDecoration(
                color: AppColors.surface,
                borderRadius: BorderRadius.circular(16),
                border: Border.all(color: AppColors.border),
              ),
              child: Column(
                children: [
                  Icon(icons[offers[i].type] ?? Icons.card_giftcard, color: AppColors.accent, size: 26),
                  const SizedBox(height: 6),
                  Text(offers[i].title,
                      textAlign: TextAlign.center,
                      style: const TextStyle(fontSize: 12.5, fontWeight: FontWeight.w800)),
                  const SizedBox(height: 2),
                  Text('${offers[i].cost} pts',
                      style: const TextStyle(fontSize: 12, color: AppColors.textSecondary)),
                ],
              ),
            ),
          ),
          if (i < offers.length - 1) const SizedBox(width: 10),
        ],
      ],
    );
  }

  Widget _buildRestaurantFilter(LoyaltyOverview overview) {
    final withPoints = overview.restaurants.where((r) => r.points > 0).length;
    return Row(
      children: [
        ChoiceChip(
          label: Text('All restaurants (${overview.restaurants.length})'),
          selected: !_onlyWithPoints,
          onSelected: (_) => setState(() => _onlyWithPoints = false),
          showCheckmark: false,
          selectedColor: AppColors.primary,
          backgroundColor: AppColors.surface,
          side: const BorderSide(color: AppColors.border),
          labelStyle: TextStyle(
              fontWeight: FontWeight.w700, color: !_onlyWithPoints ? Colors.white : AppColors.textPrimary),
        ),
        const SizedBox(width: 8),
        ChoiceChip(
          label: Text('Where I have points ($withPoints)'),
          selected: _onlyWithPoints,
          onSelected: (_) => setState(() => _onlyWithPoints = true),
          showCheckmark: false,
          selectedColor: AppColors.primary,
          backgroundColor: AppColors.surface,
          side: const BorderSide(color: AppColors.border),
          labelStyle: TextStyle(
              fontWeight: FontWeight.w700, color: _onlyWithPoints ? Colors.white : AppColors.textPrimary),
        ),
      ],
    );
  }

  List<Widget> _buildRestaurantCards(LoyaltyOverview overview) {
    final list = overview.restaurants.where((r) => !_onlyWithPoints || r.points > 0).toList();
    if (list.isEmpty) {
      return [
        const Padding(
          padding: EdgeInsets.symmetric(vertical: 30),
          child: Center(
            child: Text('Play a game for a restaurant to start collecting its points.',
                textAlign: TextAlign.center, style: TextStyle(color: AppColors.textSecondary)),
          ),
        ),
      ];
    }
    return [for (final r in list) _restaurantCard(r)];
  }

  Widget _restaurantCard(RestaurantLoyalty restaurant) {
    final fallback = Container(
      width: 52,
      height: 52,
      color: AppColors.border,
      child: const Icon(Icons.restaurant, color: AppColors.textHint),
    );

    final enabled = restaurant.loyaltyEnabled;

    return Container(
      margin: const EdgeInsets.only(bottom: 14),
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: enabled ? AppColors.surface : AppColors.background,
        borderRadius: BorderRadius.circular(20),
        border: Border.all(color: AppColors.border),
        boxShadow: [BoxShadow(color: Colors.black.withOpacity(0.04), blurRadius: 10, offset: const Offset(0, 4))],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              ClipRRect(
                borderRadius: BorderRadius.circular(12),
                child: restaurant.image.isEmpty
                    ? fallback
                    : Image.network(restaurant.image,
                        width: 52, height: 52, fit: BoxFit.cover, errorBuilder: (_, __, ___) => fallback),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(restaurant.name,
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                        style: const TextStyle(fontSize: 16, fontWeight: FontWeight.w800)),
                    Text(enabled ? restaurant.cuisine : '${restaurant.cuisine} • no rewards',
                        style: const TextStyle(fontSize: 12.5, color: AppColors.textSecondary)),
                  ],
                ),
              ),
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
                decoration: BoxDecoration(
                  color: AppColors.accent.withOpacity(0.18),
                  borderRadius: BorderRadius.circular(16),
                ),
                child: Row(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    const Icon(Icons.stars, size: 16, color: AppColors.accent),
                    const SizedBox(width: 4),
                    Text('${restaurant.points}', style: const TextStyle(fontWeight: FontWeight.w900)),
                  ],
                ),
              ),
            ],
          ),
          const SizedBox(height: 12),
          if (!enabled)
            Container(
              width: double.infinity,
              padding: const EdgeInsets.all(14),
              decoration: BoxDecoration(
                color: AppColors.surface,
                borderRadius: BorderRadius.circular(12),
                border: Border.all(color: AppColors.border),
              ),
              child: Row(
                children: [
                  const Icon(Icons.block, size: 20, color: AppColors.textSecondary),
                  const SizedBox(width: 10),
                  Expanded(
                    child: Text(
                      restaurant.points > 0
                          ? 'No loyalty rewards at this restaurant for now. Your ${restaurant.points} points here are kept.'
                          : 'This restaurant does not offer loyalty rewards.',
                      style: const TextStyle(fontSize: 13, color: AppColors.textSecondary),
                    ),
                  ),
                ],
              ),
            )
          else
            for (final offer in restaurant.offers) _offerRow(restaurant, offer),
        ],
      ),
    );
  }

  Widget _offerRow(RestaurantLoyalty restaurant, RewardOffer offer) {
    final key = '${restaurant.restaurantId}|${offer.type}';
    final busy = _redeeming == key;
    final progress = offer.cost == 0 ? 1.0 : (restaurant.points / offer.cost).clamp(0.0, 1.0);

    return Padding(
      padding: const EdgeInsets.only(top: 10),
      child: Row(
        children: [
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(offer.title, style: const TextStyle(fontSize: 14, fontWeight: FontWeight.w700)),
                const SizedBox(height: 4),
                ClipRRect(
                  borderRadius: BorderRadius.circular(6),
                  child: LinearProgressIndicator(
                    value: progress,
                    minHeight: 6,
                    backgroundColor: AppColors.divider,
                    valueColor: AlwaysStoppedAnimation<Color>(offer.canRedeem ? AppColors.success : AppColors.accent),
                  ),
                ),
                const SizedBox(height: 3),
                Text(
                  offer.canRedeem
                      ? '${offer.cost} points • you have enough'
                      : '${offer.cost} points • ${offer.missingPoints} more needed',
                  style: TextStyle(
                    fontSize: 12,
                    color: offer.canRedeem ? AppColors.success : AppColors.textSecondary,
                    fontWeight: offer.canRedeem ? FontWeight.w700 : FontWeight.w400,
                  ),
                ),
              ],
            ),
          ),
          const SizedBox(width: 12),
          SizedBox(
            width: 104,
            height: 38,
            child: ElevatedButton(
              onPressed: offer.canRedeem && _redeeming == null ? () => _redeem(restaurant, offer) : null,
              style: ElevatedButton.styleFrom(
                backgroundColor: AppColors.success,
                foregroundColor: Colors.white,
                disabledBackgroundColor: AppColors.divider,
                disabledForegroundColor: AppColors.textHint,
                padding: EdgeInsets.zero,
                textStyle: const TextStyle(fontSize: 13, fontWeight: FontWeight.w800),
              ),
              child: busy
                  ? const SizedBox(
                      width: 16, height: 16, child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white))
                  : Text(offer.canRedeem ? 'Exchange' : 'Not enough'),
            ),
          ),
        ],
      ),
    );
  }

  // ---- vouchers

  Widget _buildVouchers(List<Voucher> vouchers) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        _sectionTitle('My vouchers', 'Rewards you unlocked; show the code at the restaurant'),
        const SizedBox(height: 14),
        if (vouchers.isEmpty)
          Container(
            width: double.infinity,
            padding: const EdgeInsets.all(20),
            decoration: BoxDecoration(
              color: AppColors.surface,
              borderRadius: BorderRadius.circular(16),
              border: Border.all(color: AppColors.border),
            ),
            child: const Text('No voucher yet. Exchange your points above to get one.',
                style: TextStyle(color: AppColors.textSecondary)),
          )
        else
          for (final v in vouchers) _voucherCard(v),
      ],
    );
  }

  Widget _voucherCard(Voucher v) {
    final Color color = v.isActive
        ? AppColors.success
        : v.status == 'USED'
            ? AppColors.textSecondary
            : AppColors.error;
    return Container(
      margin: const EdgeInsets.only(bottom: 12),
      decoration: BoxDecoration(
        color: AppColors.surface,
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: color.withOpacity(0.4)),
      ),
      child: InkWell(
        borderRadius: BorderRadius.circular(16),
        onTap: () => Navigator.of(context).push(MaterialPageRoute(builder: (_) => VoucherTicketScreen(voucher: v))),
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Row(
        children: [
          Container(
            padding: const EdgeInsets.all(10),
            decoration: BoxDecoration(color: color.withOpacity(0.12), borderRadius: BorderRadius.circular(12)),
            child: Icon(Icons.confirmation_number_outlined, color: color),
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text('${v.title} • ${v.restaurantName}',
                    maxLines: 1, overflow: TextOverflow.ellipsis, style: const TextStyle(fontWeight: FontWeight.w800)),
                const SizedBox(height: 2),
                Text(
                  v.expiresAt != null
                      ? '${v.isActive ? 'Valid until' : 'Expired on'} ${DateFormat('MMM d, y').format(v.expiresAt!.toLocal())}'
                      : '',
                  style: const TextStyle(fontSize: 12, color: AppColors.textSecondary),
                ),
              ],
            ),
          ),
          Column(
            crossAxisAlignment: CrossAxisAlignment.end,
            children: [
              InkWell(
                onTap: () {
                  Clipboard.setData(ClipboardData(text: v.code));
                  ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Code copied')));
                },
                child: Text(v.code,
                    style: const TextStyle(fontWeight: FontWeight.w900, letterSpacing: 1, fontSize: 13)),
              ),
              const SizedBox(height: 4),
              Text(v.isActive ? 'ACTIVE • view ticket' : v.status,
                  style: TextStyle(fontSize: 11, fontWeight: FontWeight.w800, color: color)),
            ],
          ),
        ],
      ),
        ),
      ),
    );
  }

  // ---- levels

  Widget _buildLevels(int points) {
    const colors = {
      'Bronze': Color(0xFFCD7F32),
      'Silver': Color(0xFFC0C0C0),
      'Gold': AppColors.accent,
      'Platinum': Color(0xFFB0B0C0),
    };
    final current = _levelFor(points);
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        _sectionTitle('Loyalty levels', 'Your level grows with your total points'),
        const SizedBox(height: 14),
        for (final entry in AppConstants.loyaltyLevels.entries)
          Container(
            margin: const EdgeInsets.only(bottom: 10),
            padding: const EdgeInsets.all(14),
            decoration: BoxDecoration(
              color: AppColors.surface,
              borderRadius: BorderRadius.circular(14),
              border: Border.all(color: entry.key == current ? AppColors.accent : AppColors.border, width: entry.key == current ? 2 : 1),
            ),
            child: Row(
              children: [
                Icon(Icons.star, color: colors[entry.key] ?? AppColors.textSecondary, size: 26),
                const SizedBox(width: 12),
                Expanded(
                  child: Text(entry.key, style: const TextStyle(fontSize: 15, fontWeight: FontWeight.w800)),
                ),
                Text('${entry.value}+ points', style: const TextStyle(color: AppColors.textSecondary)),
                if (entry.key == current) ...[
                  const SizedBox(width: 8),
                  const Icon(Icons.check_circle, color: AppColors.accent, size: 18),
                ],
              ],
            ),
          ),
      ],
    );
  }
}

/// Bottom sheet to choose the restaurant a game is played for.
class _RestaurantPicker extends StatefulWidget {
  final List<RestaurantLoyalty> restaurants;
  final String gameTitle;

  const _RestaurantPicker({required this.restaurants, required this.gameTitle});

  @override
  State<_RestaurantPicker> createState() => _RestaurantPickerState();
}

class _RestaurantPickerState extends State<_RestaurantPicker> {
  String _query = '';

  @override
  Widget build(BuildContext context) {
    final q = _query.trim().toLowerCase();
    final list = widget.restaurants
        .where((r) => q.isEmpty || r.name.toLowerCase().contains(q) || r.cuisine.toLowerCase().contains(q))
        .toList();

    return Padding(
      padding: EdgeInsets.only(bottom: MediaQuery.of(context).viewInsets.bottom),
      child: SizedBox(
        height: MediaQuery.of(context).size.height * 0.75,
        child: Column(
          children: [
            const SizedBox(height: 10),
            Container(
              width: 40,
              height: 4,
              decoration: BoxDecoration(color: AppColors.border, borderRadius: BorderRadius.circular(2)),
            ),
            Padding(
              padding: const EdgeInsets.fromLTRB(20, 16, 20, 4),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text('Where do you want to play ${widget.gameTitle}?',
                      style: const TextStyle(fontSize: 18, fontWeight: FontWeight.w800)),
                  const SizedBox(height: 4),
                  const Text('The points you win are added to this restaurant.',
                      style: TextStyle(fontSize: 13, color: AppColors.textSecondary)),
                  const SizedBox(height: 12),
                  TextField(
                    onChanged: (v) => setState(() => _query = v),
                    decoration: InputDecoration(
                      hintText: 'Search a restaurant',
                      prefixIcon: const Icon(Icons.search),
                      filled: true,
                      fillColor: AppColors.background,
                      contentPadding: const EdgeInsets.symmetric(vertical: 12),
                      border: OutlineInputBorder(borderRadius: BorderRadius.circular(14), borderSide: BorderSide.none),
                    ),
                  ),
                ],
              ),
            ),
            Expanded(
              child: list.isEmpty
                  ? const Center(child: Text('No restaurant found', style: TextStyle(color: AppColors.textSecondary)))
                  : ListView.builder(
                      padding: const EdgeInsets.fromLTRB(12, 8, 12, 20),
                      itemCount: list.length,
                      itemBuilder: (context, index) {
                        final r = list[index];
                        return ListTile(
                          enabled: r.loyaltyEnabled,
                          onTap: r.loyaltyEnabled ? () => Navigator.of(context).pop(r) : null,
                          leading: ClipRRect(
                            borderRadius: BorderRadius.circular(10),
                            child: r.image.isEmpty
                                ? Container(
                                    width: 44, height: 44, color: AppColors.border, child: const Icon(Icons.restaurant))
                                : Image.network(r.image,
                                    width: 44,
                                    height: 44,
                                    fit: BoxFit.cover,
                                    errorBuilder: (_, __, ___) => Container(
                                        width: 44, height: 44, color: AppColors.border, child: const Icon(Icons.restaurant))),
                          ),
                          title: Text(r.name, style: const TextStyle(fontWeight: FontWeight.w700)),
                          subtitle: Text(r.loyaltyEnabled
                              ? '${r.cuisine} • you have ${r.points} pts here'
                              : 'No loyalty rewards at this restaurant'),
                          trailing: Icon(r.loyaltyEnabled ? Icons.chevron_right : Icons.block),
                        );
                      },
                    ),
            ),
          ],
        ),
      ),
    );
  }
}
