import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import 'package:provider/provider.dart';
import '../../../core/theme/app_colors.dart';
import '../../../models/review.dart';
import '../../../widgets/admin_toolbar.dart';
import '../providers/admin_provider.dart';

class AdminReviewsScreen extends StatefulWidget {
  const AdminReviewsScreen({super.key});

  @override
  State<AdminReviewsScreen> createState() => _AdminReviewsScreenState();
}

class _AdminReviewsScreenState extends State<AdminReviewsScreen> {
  String _query = '';
  AdminSort _sort = AdminSort.newest;
  int _stars = 0; // 0 = any, 5/4/3 exact, 2 = two stars or fewer

  bool _starMatch(Review r, int stars) {
    final rounded = r.rating.round();
    if (stars == 0) return true;
    if (stars == 2) return rounded <= 2;
    return rounded == stars;
  }

  List<Review> _filtered(List<Review> reviews) {
    final q = _query.trim().toLowerCase();
    final list = reviews.where((r) {
      if (!_starMatch(r, _stars)) return false;
      if (q.isEmpty) return true;
      return r.userName.toLowerCase().contains(q) ||
          r.restaurantName.toLowerCase().contains(q) ||
          r.comment.toLowerCase().contains(q) ||
          (r.title ?? '').toLowerCase().contains(q);
    });
    return sortByAdded(list, _sort, (r) => r.id);
  }

  Widget _buildToolbar(List<Review> all) {
    return Padding(
      padding: const EdgeInsets.fromLTRB(16, 12, 16, 4),
      child: Column(
        children: [
          AdminSearchBar(
            hint: 'Search author, restaurant or comment',
            sort: _sort,
            onQueryChanged: (v) => setState(() => _query = v),
            onSortChanged: (v) => setState(() => _sort = v),
          ),
          const SizedBox(height: 10),
          AdminChips<int>(
            options: const [('All', 0), ('5 stars', 5), ('4 stars', 4), ('3 stars', 3), ('2 or less', 2)],
            selected: _stars,
            countOf: (v) => all.where((r) => _starMatch(r, v)).length,
            onChanged: (v) => setState(() => _stars = v),
          ),
        ],
      ),
    );
  }
  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (mounted) context.read<AdminProvider>().loadReviews();
    });
  }

  Future<void> _deleteReview(Review review) async {
    final confirm = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Delete Review'),
        content: const Text('Are you sure you want to remove this review? This cannot be undone.'),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(context).pop(false),
            child: const Text('Cancel'),
          ),
          ElevatedButton(
            onPressed: () => Navigator.of(context).pop(true),
            style: ElevatedButton.styleFrom(backgroundColor: AppColors.error),
            child: const Text('Delete'),
          ),
        ],
      ),
    );

    if (confirm != true) return;

    try {
      await context.read<AdminProvider>().deleteReview(review.id);
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Review removed'), backgroundColor: AppColors.success),
        );
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text(e.toString().replaceAll('Exception: ', ''))),
        );
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final admin = context.watch<AdminProvider>();
    final items = _filtered(admin.reviews);

    return Scaffold(
      backgroundColor: AppColors.background,
      appBar: AppBar(
        title: const Text('Reviews'),
      ),
      body: Column(
        children: [
          _buildToolbar(admin.reviews),
          Expanded(
            child: RefreshIndicator(
        onRefresh: () => context.read<AdminProvider>().loadReviews(),
        child: admin.isLoadingReviews && admin.reviews.isEmpty
            ? const Center(child: CircularProgressIndicator())
            : admin.reviewsError != null && admin.reviews.isEmpty
                ? _buildError(admin.reviewsError!)
                : admin.reviews.isEmpty
                    ? _buildEmptyState()
                    : items.isEmpty
                        ? ListView(children: [
                            AdminNoResults(onReset: () => setState(() {
                                  _query = '';
                                  _stars = 0;
                                })),
                          ])
                        : ListView.builder(
                            padding: const EdgeInsets.all(16),
                            itemCount: items.length,
                            itemBuilder: (context, index) => _buildReviewCard(items[index]),
                          ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildError(String message) {
    return ListView(
      padding: const EdgeInsets.all(20),
      children: [
        const SizedBox(height: 60),
        const Icon(Icons.error_outline, size: 64, color: AppColors.error),
        const SizedBox(height: 16),
        Center(
          child: Text(
            message.replaceAll('Exception: ', ''),
            textAlign: TextAlign.center,
            style: const TextStyle(color: AppColors.textSecondary),
          ),
        ),
      ],
    );
  }

  Widget _buildEmptyState() {
    return ListView(
      padding: const EdgeInsets.all(20),
      children: [
        const SizedBox(height: 60),
        Icon(Icons.rate_review_outlined, size: 80, color: AppColors.textHint),
        const SizedBox(height: 16),
        const Center(
          child: Text(
            'No reviews yet',
            style: TextStyle(fontSize: 18, fontWeight: FontWeight.w600, color: AppColors.textPrimary),
          ),
        ),
      ],
    );
  }

  Widget _buildReviewCard(Review review) {
    return Card(
      margin: const EdgeInsets.only(bottom: 12),
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        review.restaurantName,
                        style: const TextStyle(fontSize: 15, fontWeight: FontWeight.bold, color: AppColors.textPrimary),
                      ),
                      const SizedBox(height: 2),
                      Text(
                        'by ${review.userName}',
                        style: const TextStyle(fontSize: 13, color: AppColors.textSecondary),
                      ),
                    ],
                  ),
                ),
                Row(
                  children: List.generate(5, (index) {
                    return Icon(
                      index < review.rating.round() ? Icons.star : Icons.star_border,
                      size: 16,
                      color: AppColors.ratingActive,
                    );
                  }),
                ),
                IconButton(
                  onPressed: () => _deleteReview(review),
                  icon: const Icon(Icons.delete_outline, color: AppColors.error),
                ),
              ],
            ),
            if (review.title != null && review.title!.isNotEmpty) ...[
              const SizedBox(height: 8),
              Text(
                review.title!,
                style: const TextStyle(fontSize: 14, fontWeight: FontWeight.w600, color: AppColors.textPrimary),
              ),
            ],
            const SizedBox(height: 4),
            Text(
              review.comment,
              style: const TextStyle(fontSize: 14, color: AppColors.textSecondary),
            ),
            const SizedBox(height: 8),
            Text(
              DateFormat('MMM dd, yyyy').format(review.createdAt),
              style: const TextStyle(fontSize: 12, color: AppColors.textHint),
            ),
          ],
        ),
      ),
    );
  }
}
