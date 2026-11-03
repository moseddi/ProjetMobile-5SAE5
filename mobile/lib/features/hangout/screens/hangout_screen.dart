import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../../core/theme/app_colors.dart';
import '../../../core/navigation/app_router.dart';
import '../../../models/hangout.dart';
import '../../../widgets/event_widgets.dart' show DateFilterPills;
import '../../../widgets/hangout_widgets.dart';
import '../../auth/providers/auth_provider.dart';
import '../providers/hangout_provider.dart';
import 'my_hangouts_screen.dart';

class HangOutScreen extends StatefulWidget {
  const HangOutScreen({super.key});

  @override
  State<HangOutScreen> createState() => _HangOutScreenState();
}

class _HangOutScreenState extends State<HangOutScreen> {
  final _searchController = TextEditingController();
  String? _joiningId;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) => _load());
  }

  @override
  void dispose() {
    _searchController.dispose();
    super.dispose();
  }

  Future<void> _load() async {
    final user = context.read<AuthProvider>().currentUser;
    if (user == null) return;
    await context.read<HangOutProvider>().load(user.id);
  }

  Future<void> _join(HangOut hangout) async {
    setState(() => _joiningId = hangout.id);
    final messenger = ScaffoldMessenger.of(context);
    try {
      await context.read<HangOutProvider>().requestToJoin(hangout.id);
      messenger.showSnackBar(SnackBar(
        backgroundColor: AppColors.success,
        content: Text('Request sent! Wait for ${hangout.creatorName} to accept.'),
      ));
    } catch (e) {
      messenger.showSnackBar(SnackBar(
        backgroundColor: AppColors.error,
        content: Text(e.toString().replaceAll('Exception: ', '')),
      ));
    } finally {
      if (mounted) setState(() => _joiningId = null);
    }
  }

  void _openMine() {
    Navigator.of(context).push(MaterialPageRoute(builder: (_) => const MyHangOutsScreen()));
  }

  @override
  Widget build(BuildContext context) {
    final provider = context.watch<HangOutProvider>();
    final userId = context.watch<AuthProvider>().currentUser?.id;
    final items = provider.visible;
    final incoming = provider.incomingRequestCount;

    return Scaffold(
      backgroundColor: AppColors.background,
      appBar: AppBar(
        title: const Text('Hang Out'),
        actions: [
          IconButton(
            tooltip: 'My hang outs',
            onPressed: _openMine,
            icon: Badge(
              isLabelVisible: incoming > 0,
              label: Text('$incoming'),
              child: const Icon(Icons.inbox_outlined),
            ),
          ),
          IconButton(
            tooltip: 'Post a hang out',
            icon: const Icon(Icons.add_circle_outline),
            onPressed: () => Navigator.of(context).pushNamed(AppRouter.createHangout),
          ),
        ],
      ),
      body: RefreshIndicator(
        onRefresh: _load,
        child: ListView(
          physics: const AlwaysScrollableScrollPhysics(),
          padding: const EdgeInsets.fromLTRB(16, 8, 16, 32),
          children: [
            _buildSearch(provider),
            const SizedBox(height: 12),
            DateFilterPills(
              selected: provider.dateFilter,
              customRange: provider.customRange,
              onChanged: provider.setDateFilter,
            ),
            const SizedBox(height: 10),
            _buildScopes(provider),
            const SizedBox(height: 14),
            if (provider.isLoading && provider.hangouts.isEmpty)
              const Padding(
                padding: EdgeInsets.only(top: 80),
                child: Center(child: CircularProgressIndicator()),
              )
            else if (provider.error != null && provider.hangouts.isEmpty)
              _buildMessage(Icons.wifi_off, 'Could not load hang outs', provider.error!, 'Retry', _load)
            else if (items.isEmpty)
              provider.hasActiveFilters
                  ? _buildMessage(Icons.search_off, 'No hang out matches', 'Try another date or clear the filters.',
                      'Clear filters', () {
                      _searchController.clear();
                      provider.resetFilters();
                    })
                  : _buildMessage(Icons.groups_outlined, 'No hang outs yet',
                      'Be the first to invite people to dine with you.', 'Post a hang out',
                      () => Navigator.of(context).pushNamed(AppRouter.createHangout))
            else
              for (final h in items)
                Padding(
                  padding: const EdgeInsets.only(bottom: 16),
                  child: HangOutCard(
                    hangout: h,
                    currentUserId: userId,
                    isJoining: _joiningId == h.id,
                    onJoin: () => _join(h),
                    onManage: _openMine,
                    onTap: () => Navigator.of(context).pushNamed(AppRouter.hangoutDetails, arguments: h),
                  ),
                ),
          ],
        ),
      ),
    );
  }

  Widget _buildSearch(HangOutProvider provider) {
    return TextField(
      controller: _searchController,
      onChanged: provider.setQuery,
      decoration: InputDecoration(
        hintText: 'Search by restaurant, host or plan',
        prefixIcon: const Icon(Icons.search),
        suffixIcon: provider.query.isEmpty
            ? null
            : IconButton(
                icon: const Icon(Icons.close),
                onPressed: () {
                  _searchController.clear();
                  provider.setQuery('');
                },
              ),
        filled: true,
        fillColor: AppColors.surface,
        contentPadding: const EdgeInsets.symmetric(vertical: 14),
        border: OutlineInputBorder(
          borderRadius: BorderRadius.circular(16),
          borderSide: const BorderSide(color: AppColors.border),
        ),
        enabledBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(16),
          borderSide: const BorderSide(color: AppColors.border),
        ),
      ),
    );
  }

  Widget _buildScopes(HangOutProvider provider) {
    const labels = {
      HangOutScope.all: 'All',
      HangOutScope.open: 'Open spots',
      HangOutScope.mine: 'My posts',
      HangOutScope.joined: 'Joined',
    };
    return SingleChildScrollView(
      scrollDirection: Axis.horizontal,
      child: Row(
        children: [
          for (final scope in HangOutScope.values)
            Padding(
              padding: const EdgeInsets.only(right: 8),
              child: ChoiceChip(
                label: Text('${labels[scope]} (${provider.countFor(scope)})'),
                selected: provider.scope == scope,
                onSelected: (_) => provider.setScope(scope),
                selectedColor: AppColors.primary,
                labelStyle: TextStyle(
                  fontWeight: FontWeight.w700,
                  color: provider.scope == scope ? Colors.white : AppColors.textPrimary,
                ),
                backgroundColor: AppColors.surface,
                side: const BorderSide(color: AppColors.border),
                showCheckmark: false,
              ),
            ),
        ],
      ),
    );
  }

  Widget _buildMessage(IconData icon, String title, String text, String action, VoidCallback onAction) {
    return Padding(
      padding: const EdgeInsets.only(top: 50),
      child: Column(
        children: [
          Icon(icon, size: 70, color: AppColors.textHint),
          const SizedBox(height: 14),
          Text(title,
              style: const TextStyle(fontSize: 18, fontWeight: FontWeight.w800, color: AppColors.textPrimary)),
          const SizedBox(height: 6),
          Text(text,
              textAlign: TextAlign.center,
              style: const TextStyle(fontSize: 14, color: AppColors.textSecondary)),
          const SizedBox(height: 20),
          ElevatedButton(onPressed: onAction, child: Text(action)),
        ],
      ),
    );
  }
}
