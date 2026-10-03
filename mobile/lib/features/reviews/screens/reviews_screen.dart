import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:intl/intl.dart';
import '../../../core/theme/app_colors.dart';
import '../../../core/navigation/app_router.dart';
import '../../../models/review.dart';
import '../../../models/restaurant.dart';
import '../../../services/interfaces/restaurant_service.dart';
import '../providers/review_provider.dart';

class ReviewsScreen extends StatefulWidget {
  const ReviewsScreen({super.key});

  @override
  State<ReviewsScreen> createState() => _ReviewsScreenState();
}

class _ReviewsScreenState extends State<ReviewsScreen> {
  List<Restaurant> _restaurants = [];
  String? _selectedRestaurantId;
  double? _selectedRating;
  String _sortBy = 'newest';

  @override
  void initState() {
    super.initState();
    _loadRestaurants();
    _loadReviews();
  }

  Future<void> _loadRestaurants() async {
    final restaurantService = context.read<RestaurantService>();
    final restaurants = await restaurantService.getRestaurants();
    setState(() {
      _restaurants = restaurants;
    });
  }

  Future<void> _loadReviews() async {
    await context.read<ReviewProvider>().loadReviews();
  }

  @override
  Widget build(BuildContext context) {
    final reviewProvider = context.watch<ReviewProvider>();

    return Scaffold(
      backgroundColor: AppColors.background,
      appBar: AppBar(
        title: const Text('Customer Reviews'),
        actions: [
          IconButton(
            icon: const Icon(Icons.add),
            onPressed: () {
              Navigator.of(context).pushNamed(AppRouter.addReview);
            },
          ),
        ],
      ),
      body: Column(
        children: [
          _buildFilters(),
          Expanded(
            child: reviewProvider.isLoading
                ? const Center(child: CircularProgressIndicator())
                : reviewProvider.reviews.isEmpty
                    ? _buildEmptyState()
                    : RefreshIndicator(
                        onRefresh: _loadReviews,
                        child: ListView.builder(
                          padding: const EdgeInsets.all(20),
                          itemCount: reviewProvider.reviews.length,
                          itemBuilder: (context, index) {
                            return _buildReviewCard(reviewProvider.reviews[index]);
                          },
                        ),
                      ),
          ),
        ],
      ),
    );
  }

  Widget _buildFilters() {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: const BoxDecoration(
        color: AppColors.surface,
        border: Border(
          bottom: BorderSide(color: AppColors.border),
        ),
      ),
      child: Column(
        children: [
          Row(
            children: [
              Expanded(
                child: _buildRestaurantFilter(),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: _buildRatingFilter(),
              ),
            ],
          ),
          const SizedBox(height: 12),
          _buildSortOptions(),
        ],
      ),
    );
  }

  Widget _buildRestaurantFilter() {
    return DropdownButtonFormField<String>(
      value: _selectedRestaurantId,
      decoration: const InputDecoration(
        labelText: 'Restaurant',
        contentPadding: EdgeInsets.symmetric(horizontal: 12, vertical: 8),
      ),
      items: [
        const DropdownMenuItem(
          value: null,
          child: Text('All Restaurants'),
        ),
        ..._restaurants.map((restaurant) {
          return DropdownMenuItem(
            value: restaurant.id,
            child: Text(
              restaurant.name,
              overflow: TextOverflow.ellipsis,
            ),
          );
        }),
      ],
      onChanged: (value) {
        setState(() {
          _selectedRestaurantId = value;
        });
        context.read<ReviewProvider>().setRestaurantFilter(value);
      },
    );
  }

  Widget _buildRatingFilter() {
    return DropdownButtonFormField<double>(
      value: _selectedRating,
      decoration: const InputDecoration(
        labelText: 'Rating',
        contentPadding: EdgeInsets.symmetric(horizontal: 12, vertical: 8),
      ),
      items: const [
        DropdownMenuItem(
          value: null,
          child: Text('All Ratings'),
        ),
        DropdownMenuItem(
          value: 5.0,
          child: Text('5 Stars'),
        ),
        DropdownMenuItem(
          value: 4.0,
          child: Text('4+ Stars'),
        ),
        DropdownMenuItem(
          value: 3.0,
          child: Text('3+ Stars'),
        ),
      ],
      onChanged: (value) {
        setState(() {
          _selectedRating = value;
        });
        context.read<ReviewProvider>().setRatingFilter(value);
      },
    );
  }

  Widget _buildSortOptions() {
    return Row(
      children: [
        const Text(
          'Sort by:',
          style: TextStyle(
            fontSize: 14,
            color: AppColors.textSecondary,
          ),
        ),
        const SizedBox(width: 12),
        Expanded(
          child: Wrap(
            spacing: 8,
            children: [
              ChoiceChip(
                label: const Text('Newest'),
                selected: _sortBy == 'newest',
                onSelected: (selected) {
                  if (selected) {
                    setState(() {
                      _sortBy = 'newest';
                    });
                    context.read<ReviewProvider>().setSortBy('newest');
                  }
                },
              ),
              ChoiceChip(
                label: const Text('Highest Rated'),
                selected: _sortBy == 'rating',
                onSelected: (selected) {
                  if (selected) {
                    setState(() {
                      _sortBy = 'rating';
                    });
                    context.read<ReviewProvider>().setSortBy('rating');
                  }
                },
              ),
            ],
          ),
        ),
      ],
    );
  }

  Widget _buildEmptyState() {
    return Center(
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Icon(
            Icons.rate_review_outlined,
            size: 80,
            color: AppColors.textHint,
          ),
          const SizedBox(height: 16),
          const Text(
            'No reviews yet',
            style: TextStyle(
              fontSize: 18,
              fontWeight: FontWeight.w600,
              color: AppColors.textPrimary,
            ),
          ),
          const SizedBox(height: 8),
          const Text(
            'Be the first to write a review',
            style: TextStyle(
              fontSize: 14,
              color: AppColors.textSecondary,
            ),
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
                  radius: 24,
                  backgroundImage: NetworkImage(review.userAvatar),
                  backgroundColor: AppColors.accent,
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        review.userName,
                        style: const TextStyle(
                          fontSize: 16,
                          fontWeight: FontWeight.w600,
                          color: AppColors.textPrimary,
                        ),
                      ),
                      const SizedBox(height: 2),
                      Text(
                        DateFormat('MMM dd, yyyy').format(review.createdAt),
                        style: const TextStyle(
                          fontSize: 13,
                          color: AppColors.textSecondary,
                        ),
                      ),
                    ],
                  ),
                ),
                _buildRatingStars(review.rating),
              ],
            ),
            const SizedBox(height: 12),
            // Restaurant name with image
            Container(
              padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
              decoration: BoxDecoration(
                color: AppColors.accent.withOpacity(0.1),
                borderRadius: BorderRadius.circular(8),
              ),
              child: Row(
                children: [
                  if (review.restaurantImage != null && review.restaurantImage!.isNotEmpty) ...[
                    ClipRRect(
                      borderRadius: BorderRadius.circular(6),
                      child: Image.network(
                        review.restaurantImage!,
                        width: 40,
                        height: 40,
                        fit: BoxFit.cover,
                        errorBuilder: (context, error, stackTrace) {
                          return Container(
                            width: 40,
                            height: 40,
                            color: AppColors.border,
                            child: const Icon(Icons.restaurant, size: 20, color: AppColors.textHint),
                          );
                        },
                      ),
                    ),
                    const SizedBox(width: 10),
                  ],
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          review.restaurantName,
                          style: const TextStyle(
                            fontSize: 15,
                            fontWeight: FontWeight.w600,
                            color: AppColors.accent,
                          ),
                        ),
                        const SizedBox(height: 2),
                        Row(
                          children: [
                            const Icon(Icons.restaurant, size: 12, color: AppColors.textSecondary),
                            const SizedBox(width: 4),
                            const Text(
                              'Restaurant',
                              style: TextStyle(
                                fontSize: 12,
                                color: AppColors.textSecondary,
                              ),
                            ),
                          ],
                        ),
                      ],
                    ),
                  ),
                ],
              ),
            ),
            if (review.title != null) ...[
              const SizedBox(height: 12),
              Text(
                review.title!,
                style: const TextStyle(
                  fontSize: 16,
                  fontWeight: FontWeight.w600,
                  color: AppColors.textPrimary,
                ),
              ),
            ],
            const SizedBox(height: 8),
            Text(
              review.comment,
              style: const TextStyle(
                fontSize: 15,
                height: 1.5,
                color: AppColors.textPrimary,
              ),
            ),
            const SizedBox(height: 12),
            Row(
              children: [
                TextButton.icon(
                  onPressed: () {
                    context.read<ReviewProvider>().markHelpful(review.id);
                  },
                  icon: const Icon(Icons.thumb_up_outlined, size: 18),
                  label: Text('Helpful (${review.helpfulCount})'),
                  style: TextButton.styleFrom(
                    padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
                  ),
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
          size: 18,
          color: AppColors.ratingActive,
        );
      }),
    );
  }
}
