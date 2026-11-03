import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:intl/intl.dart';
import '../../../core/theme/app_colors.dart';
import '../../../models/review.dart';
import '../providers/owner_provider.dart';

/// Read-only list of reviews left for the owner's restaurant. Owners can see
/// feedback but cannot write, edit, or delete reviews.
class OwnerReviewsScreen extends StatefulWidget {
  const OwnerReviewsScreen({super.key});

  @override
  State<OwnerReviewsScreen> createState() => _OwnerReviewsScreenState();
}

class _OwnerReviewsScreenState extends State<OwnerReviewsScreen> {
  @override
  void initState() {
    super.initState();
    context.read<OwnerProvider>().loadReviews();
  }

  Future<void> _reload() => context.read<OwnerProvider>().loadReviews();

  @override
  Widget build(BuildContext context) {
    final ownerProvider = context.watch<OwnerProvider>();

    return Scaffold(
      backgroundColor: AppColors.background,
      appBar: AppBar(
        title: const Text('Reviews'),
      ),
      body: ownerProvider.isLoadingReviews && ownerProvider.reviews.isEmpty
          ? const Center(child: CircularProgressIndicator())
          : ownerProvider.reviews.isEmpty
              ? _buildEmptyState()
              : RefreshIndicator(
                  onRefresh: _reload,
                  child: ListView.builder(
                    padding: const EdgeInsets.all(20),
                    itemCount: ownerProvider.reviews.length,
                    itemBuilder: (context, index) => _buildReviewCard(ownerProvider.reviews[index]),
                  ),
                ),
    );
  }

  Widget _buildEmptyState() {
    return Center(
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          const Icon(Icons.rate_review_outlined, size: 80, color: AppColors.textHint),
          const SizedBox(height: 16),
          const Text(
            'No reviews yet',
            style: TextStyle(fontSize: 18, fontWeight: FontWeight.w600, color: AppColors.textPrimary),
          ),
          const SizedBox(height: 8),
          const Text(
            'Customer feedback will appear here',
            style: TextStyle(fontSize: 14, color: AppColors.textSecondary),
          ),
        ],
      ),
    );
  }

  Widget _buildReviewCard(Review review) {
    return Card(
      margin: const EdgeInsets.only(bottom: 16),
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                CircleAvatar(
                  radius: 22,
                  backgroundImage: review.userAvatar.isNotEmpty ? NetworkImage(review.userAvatar) : null,
                  backgroundColor: AppColors.accent,
                  child: review.userAvatar.isEmpty ? const Icon(Icons.person, color: Colors.white) : null,
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        review.userName,
                        style: const TextStyle(fontSize: 15, fontWeight: FontWeight.w600, color: AppColors.textPrimary),
                      ),
                      const SizedBox(height: 2),
                      Text(
                        DateFormat('MMM dd, yyyy').format(review.createdAt),
                        style: const TextStyle(fontSize: 12, color: AppColors.textSecondary),
                      ),
                    ],
                  ),
                ),
                _buildRatingStars(review.rating),
              ],
            ),
            if (review.title != null && review.title!.isNotEmpty) ...[
              const SizedBox(height: 12),
              Text(
                review.title!,
                style: const TextStyle(fontSize: 16, fontWeight: FontWeight.w600, color: AppColors.textPrimary),
              ),
            ],
            const SizedBox(height: 8),
            Text(
              review.comment,
              style: const TextStyle(fontSize: 14, height: 1.4, color: AppColors.textPrimary),
            ),
            const SizedBox(height: 12),
            Row(
              children: [
                const Icon(Icons.thumb_up_outlined, size: 16, color: AppColors.textSecondary),
                const SizedBox(width: 6),
                Text(
                  '${review.helpfulCount} found this helpful',
                  style: const TextStyle(fontSize: 12, color: AppColors.textSecondary),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildRatingStars(double rating) {
    return Row(
      mainAxisSize: MainAxisSize.min,
      children: List.generate(5, (index) {
        return Icon(
          index < rating.floor()
              ? Icons.star
              : index < rating
                  ? Icons.star_half
                  : Icons.star_border,
          size: 16,
          color: AppColors.ratingActive,
        );
      }),
    );
  }
}
