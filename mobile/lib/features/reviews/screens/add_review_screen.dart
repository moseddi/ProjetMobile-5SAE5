import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../../core/theme/app_colors.dart';
import '../../../models/restaurant.dart';
import '../../../models/review.dart';
import '../../../services/interfaces/restaurant_service.dart';
import '../../auth/providers/auth_provider.dart';
import '../providers/review_provider.dart';

class AddReviewScreen extends StatefulWidget {
  final String? preselectedRestaurantId;

  const AddReviewScreen({super.key, this.preselectedRestaurantId});

  @override
  State<AddReviewScreen> createState() => _AddReviewScreenState();
}

class _AddReviewScreenState extends State<AddReviewScreen> {
  final _formKey = GlobalKey<FormState>();
  final _titleController = TextEditingController();
  final _commentController = TextEditingController();
  
  List<Restaurant> _restaurants = [];
  String? _selectedRestaurantId;
  double _rating = 5.0;
  bool _isLoading = false;

  @override
  void initState() {
    super.initState();
    _selectedRestaurantId = widget.preselectedRestaurantId;
    _loadRestaurants();
  }

  @override
  void dispose() {
    _titleController.dispose();
    _commentController.dispose();
    super.dispose();
  }

  Future<void> _loadRestaurants() async {
    final restaurantService = context.read<RestaurantService>();
    final restaurants = await restaurantService.getRestaurants();
    setState(() {
      _restaurants = restaurants;
    });
  }

  Future<void> _submitReview() async {
    if (!(_formKey.currentState?.validate() ?? false)) return;
    if (_selectedRestaurantId == null) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('Please select a restaurant'),
          backgroundColor: AppColors.error,
        ),
      );
      return;
    }

    final user = context.read<AuthProvider>().currentUser;
    if (user == null) return;

    final restaurant = _restaurants.firstWhere(
      (r) => r.id == _selectedRestaurantId,
    );

    setState(() {
      _isLoading = true;
    });

    final review = Review(
      id: '',
      userId: user.id,
      userName: user.name,
      userAvatar: user.avatar,
      restaurantId: restaurant.id,
      restaurantName: restaurant.name,
      restaurantImage: restaurant.image,
      rating: _rating,
      title: _titleController.text.trim().isEmpty ? null : _titleController.text.trim(),
      comment: _commentController.text.trim(),
      createdAt: DateTime.now(),
      helpfulCount: 0,
      status: ReviewStatus.published,
    );

    try {
      await context.read<ReviewProvider>().createReview(review);
      
      if (mounted) {
        Navigator.of(context).pop();
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text('Review submitted successfully!'),
            backgroundColor: AppColors.success,
          ),
        );
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text('Failed to submit review: ${e.toString()}'),
            backgroundColor: AppColors.error,
          ),
        );
      }
    } finally {
      setState(() {
        _isLoading = false;
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColors.background,
      appBar: AppBar(
        title: const Text('Write a Review'),
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(20),
        child: Form(
          key: _formKey,
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              const Text(
                'Select Restaurant',
                style: TextStyle(
                  fontSize: 18,
                  fontWeight: FontWeight.bold,
                  color: AppColors.textPrimary,
                ),
              ),
              const SizedBox(height: 12),
              DropdownButtonFormField<String>(
                value: _selectedRestaurantId,
                decoration: const InputDecoration(
                  hintText: 'Choose a restaurant',
                  prefixIcon: Icon(Icons.restaurant),
                ),
                items: _restaurants.map((restaurant) {
                  return DropdownMenuItem(
                    value: restaurant.id,
                    child: Text(restaurant.name),
                  );
                }).toList(),
                onChanged: widget.preselectedRestaurantId == null
                    ? (value) {
                        setState(() {
                          _selectedRestaurantId = value;
                        });
                      }
                    : null,
                validator: (value) {
                  if (value == null || value.isEmpty) {
                    return 'Please select a restaurant';
                  }
                  return null;
                },
              ),
              const SizedBox(height: 24),
              const Text(
                'Your Rating',
                style: TextStyle(
                  fontSize: 18,
                  fontWeight: FontWeight.bold,
                  color: AppColors.textPrimary,
                ),
              ),
              const SizedBox(height: 12),
              Center(
                child: Row(
                  mainAxisSize: MainAxisSize.min,
                  children: List.generate(5, (index) {
                    return IconButton(
                      iconSize: 40,
                      onPressed: () {
                        setState(() {
                          _rating = index + 1.0;
                        });
                      },
                      icon: Icon(
                        index < _rating ? Icons.star : Icons.star_border,
                        color: AppColors.ratingActive,
                      ),
                    );
                  }),
                ),
              ),
              const SizedBox(height: 24),
              const Text(
                'Review Title (Optional)',
                style: TextStyle(
                  fontSize: 18,
                  fontWeight: FontWeight.bold,
                  color: AppColors.textPrimary,
                ),
              ),
              const SizedBox(height: 12),
              TextFormField(
                controller: _titleController,
                decoration: const InputDecoration(
                  hintText: 'Summarize your experience',
                ),
                maxLength: 100,
              ),
              const SizedBox(height: 16),
              const Text(
                'Your Review',
                style: TextStyle(
                  fontSize: 18,
                  fontWeight: FontWeight.bold,
                  color: AppColors.textPrimary,
                ),
              ),
              const SizedBox(height: 12),
              TextFormField(
                controller: _commentController,
                maxLines: 6,
                decoration: const InputDecoration(
                  hintText: 'Share your experience with others...',
                  alignLabelWithHint: true,
                ),
                validator: (value) {
                  if (value == null || value.trim().isEmpty) {
                    return 'Please write your review';
                  }
                  if (value.trim().length < 10) {
                    return 'Review must be at least 10 characters';
                  }
                  return null;
                },
              ),
              const SizedBox(height: 32),
              SizedBox(
                width: double.infinity,
                height: 52,
                child: ElevatedButton(
                  onPressed: _isLoading ? null : _submitReview,
                  child: _isLoading
                      ? const SizedBox(
                          height: 20,
                          width: 20,
                          child: CircularProgressIndicator(
                            strokeWidth: 2,
                            color: AppColors.surface,
                          ),
                        )
                      : const Text('Submit Review'),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
