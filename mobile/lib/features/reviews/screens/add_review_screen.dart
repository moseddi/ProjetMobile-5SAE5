import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../../core/theme/app_colors.dart';
import '../../../models/restaurant.dart';
import '../../../models/review.dart';
import '../../auth/providers/auth_provider.dart';
import '../providers/review_provider.dart';
import '../../restaurants/providers/restaurant_provider.dart';

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
  
  List<Restaurant> get _restaurants => context.read<RestaurantProvider>().restaurants;
  String? _selectedRestaurantId;

  // 4 critères détaillés (1 à 5 étoiles)
  double _foodRating = 5.0;
  double _serviceRating = 5.0;
  double _priceRating = 5.0;
  double _ambianceRating = 5.0;

  double get _overallRating =>
      ((_foodRating + _serviceRating + _priceRating + _ambianceRating) / 4.0 * 10).round() / 10.0;

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

  Future<void> _loadRestaurants() => context.read<RestaurantProvider>().loadRestaurants();

  Future<void> _submitReview() async {
    if (!(_formKey.currentState?.validate() ?? false)) return;
    if (_selectedRestaurantId == null) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('Veuillez sélectionner un restaurant'),
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
      rating: _overallRating,
      title: _titleController.text.trim().isEmpty ? null : _titleController.text.trim(),
      comment: _commentController.text.trim(),
      createdAt: DateTime.now(),
      helpfulCount: 0,
      status: ReviewStatus.published,
      foodScore: (_foodRating * 2).toInt(),
      serviceScore: (_serviceRating * 2).toInt(),
      priceScore: (_priceRating * 2).toInt(),
      ambianceScore: (_ambianceRating * 2).toInt(),
    );

    try {
      final createdReview = await context.read<ReviewProvider>().createReview(review);
      
      if (mounted) {
        await _showEvaluationDialog(context, createdReview);
        if (mounted) {
          Navigator.of(context).pop();
        }
      }
    } catch (e) {
      if (mounted) {
        String err = e.toString();
        if (err.contains('Avis contradictoire') || err.contains('Incohérence') || err.contains('bloqué')) {
          _showAntiFraudDialog(context, err);
        } else {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(
              content: Text('Erreur : $err'),
              backgroundColor: AppColors.error,
            ),
          );
        }
      }
    } finally {
      if (mounted) {
        setState(() {
          _isLoading = false;
        });
      }
    }
  }

  void _showAntiFraudDialog(BuildContext context, String message) {
    showDialog(
      context: context,
      barrierDismissible: false,
      builder: (ctx) => AlertDialog(
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
        title: Row(
          children: const [
            Icon(Icons.shield_outlined, color: AppColors.error, size: 28),
            SizedBox(width: 8),
            Expanded(
              child: Text(
                'Avis Contradictoire Détecté',
                style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold, color: AppColors.error),
              ),
            ),
          ],
        ),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text(
              'Une contradiction majeure a été détectée entre vos notes et votre commentaire écrit :',
              style: TextStyle(fontSize: 14, color: AppColors.textPrimary),
            ),
            const SizedBox(height: 12),
            Container(
              padding: const EdgeInsets.all(12),
              decoration: BoxDecoration(
                color: AppColors.error.withOpacity(0.08),
                borderRadius: BorderRadius.circular(8),
                border: Border.all(color: AppColors.error.withOpacity(0.3)),
              ),
              child: Text(
                message.replaceAll('Exception:', '').replaceAll('ApiException:', '').trim(),
                style: const TextStyle(fontSize: 13, color: AppColors.error, height: 1.4),
              ),
            ),
          ],
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(ctx).pop(),
            child: const Text('Corriger mon avis', style: TextStyle(fontWeight: FontWeight.bold)),
          ),
        ],
      ),
    );
  }

  Future<void> _showEvaluationDialog(BuildContext context, Review review) async {
    await showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.transparent,
      builder: (ctx) => Container(
        constraints: BoxConstraints(
          maxHeight: MediaQuery.of(ctx).size.height * 0.85,
        ),
        decoration: const BoxDecoration(
          color: AppColors.surface,
          borderRadius: BorderRadius.vertical(top: Radius.circular(24)),
        ),
        padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 16),
        child: SingleChildScrollView(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            mainAxisSize: MainAxisSize.min,
            children: [
              Center(
                child: Container(
                  width: 40,
                  height: 4,
                  decoration: BoxDecoration(
                    color: AppColors.border,
                    borderRadius: BorderRadius.circular(2),
                  ),
                ),
              ),
              const SizedBox(height: 16),
              Row(
                children: const [
                  Icon(Icons.check_circle_rounded, color: AppColors.success, size: 24),
                  SizedBox(width: 8),
                  Text(
                    'Détail de votre Expérience',
                    style: TextStyle(
                      fontSize: 18,
                      fontWeight: FontWeight.bold,
                      color: AppColors.textPrimary,
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 4),
              const Text(
                'Récapitulatif de votre note par catégorie :',
                style: TextStyle(fontSize: 13, color: AppColors.textSecondary),
              ),
              const SizedBox(height: 16),

              _buildCriteriaStarDisplay('🍔 Cuisine', (review.foodScore ?? 10) / 2.0),
              const SizedBox(height: 10),
              _buildCriteriaStarDisplay('🤝 Service & Personnel', (review.serviceScore ?? 10) / 2.0),
              const SizedBox(height: 10),
              _buildCriteriaStarDisplay('💶 Rapport Qualité/Prix', (review.priceScore ?? 10) / 2.0),
              const SizedBox(height: 10),
              _buildCriteriaStarDisplay('🏠 Cadre & Ambiance', (review.ambianceScore ?? 10) / 2.0),

              if (review.recommendations.isNotEmpty) ...[
                const SizedBox(height: 24),
                const Divider(),
                const SizedBox(height: 12),
                Row(
                  children: const [
                    Icon(Icons.thumb_up_alt_rounded, color: AppColors.accent, size: 22),
                    SizedBox(width: 8),
                    Expanded(
                      child: Text(
                        'Nos Meilleures Suggestions pour Vous',
                        style: TextStyle(
                          fontSize: 16,
                          fontWeight: FontWeight.bold,
                          color: AppColors.accent,
                        ),
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 4),
                const Text(
                  'Voici des établissements réputés de notre plateforme qui sauront combler vos attentes :',
                  style: TextStyle(fontSize: 13, color: AppColors.textSecondary),
                ),
                const SizedBox(height: 12),
                ...review.recommendations.map((rec) => _buildRecommendationCard(rec)).toList(),
              ],

              const SizedBox(height: 24),
              SizedBox(
                width: double.infinity,
                height: 48,
                child: ElevatedButton(
                  onPressed: () => Navigator.of(ctx).pop(),
                  child: const Text('Fermer & Continuer'),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildCriteriaStarDisplay(String label, double stars) {
    return Row(
      mainAxisAlignment: MainAxisAlignment.spaceBetween,
      children: [
        Text(label, style: const TextStyle(fontSize: 14, fontWeight: FontWeight.w600)),
        Row(
          children: [
            ...List.generate(5, (i) {
              return Icon(
                i < stars.floor()
                    ? Icons.star
                    : (i < stars ? Icons.star_half : Icons.star_border),
                size: 18,
                color: AppColors.ratingActive,
              );
            }),
            const SizedBox(width: 6),
            Text(
              '${stars.toStringAsFixed(1)} ⭐',
              style: const TextStyle(fontSize: 13, fontWeight: FontWeight.bold),
            ),
          ],
        ),
      ],
    );
  }

  Widget _buildRecommendationCard(RestaurantRecommendation rec) {
    return Card(
      margin: const EdgeInsets.only(bottom: 12),
      elevation: 1,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
      child: Padding(
        padding: const EdgeInsets.all(12),
        child: Row(
          children: [
            if (rec.image.isNotEmpty)
              ClipRRect(
                borderRadius: BorderRadius.circular(8),
                child: Image.network(
                  rec.image,
                  width: 65,
                  height: 65,
                  fit: BoxFit.cover,
                  errorBuilder: (ctx, err, _) => Container(
                    width: 65,
                    height: 65,
                    color: AppColors.border,
                    child: const Icon(Icons.restaurant, color: AppColors.textHint),
                  ),
                ),
              ),
            const SizedBox(width: 12),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      Flexible(
                        child: Text(
                          rec.name,
                          style: const TextStyle(fontSize: 15, fontWeight: FontWeight.bold),
                          overflow: TextOverflow.ellipsis,
                        ),
                      ),
                      Row(
                        children: [
                          const Icon(Icons.star, size: 14, color: AppColors.ratingActive),
                          const SizedBox(width: 2),
                          Text('${rec.rating}', style: const TextStyle(fontSize: 12, fontWeight: FontWeight.bold)),
                        ],
                      ),
                    ],
                  ),
                  const SizedBox(height: 2),
                  Text(
                    rec.cuisine,
                    style: const TextStyle(fontSize: 12, color: AppColors.textSecondary),
                  ),
                  const SizedBox(height: 4),
                  Container(
                    padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
                    decoration: BoxDecoration(
                      color: AppColors.accent.withOpacity(0.1),
                      borderRadius: BorderRadius.circular(4),
                    ),
                    child: Text(
                      rec.recommendationReason,
                      style: const TextStyle(fontSize: 11, color: AppColors.accent, fontWeight: FontWeight.w600),
                    ),
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildInteractiveStarRow(String label, double currentVal, ValueChanged<double> onChanged) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 12),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Text(
            label,
            style: const TextStyle(fontSize: 14, fontWeight: FontWeight.w600, color: AppColors.textPrimary),
          ),
          Row(
            mainAxisSize: MainAxisSize.min,
            children: List.generate(5, (index) {
              return IconButton(
                padding: EdgeInsets.zero,
                constraints: const BoxConstraints(),
                iconSize: 28,
                onPressed: () => onChanged(index + 1.0),
                icon: Icon(
                  index < currentVal ? Icons.star : Icons.star_border,
                  color: AppColors.ratingActive,
                ),
              );
            }),
          ),
        ],
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    context.watch<RestaurantProvider>();

    return Scaffold(
      backgroundColor: AppColors.background,
      appBar: AppBar(
        title: const Text('Rédiger un Avis'),
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(20),
        child: Form(
          key: _formKey,
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              const Text(
                'Sélectionner le Restaurant',
                style: TextStyle(
                  fontSize: 16,
                  fontWeight: FontWeight.bold,
                  color: AppColors.textPrimary,
                ),
              ),
              const SizedBox(height: 8),
              DropdownButtonFormField<String>(
                value: _selectedRestaurantId,
                decoration: const InputDecoration(
                  hintText: 'Choisir un restaurant',
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
                    return 'Veuillez choisir un restaurant';
                  }
                  return null;
                },
              ),
              const SizedBox(height: 24),

              // Évaluation par critères (5 étoiles par critère)
              Container(
                padding: const EdgeInsets.all(16),
                decoration: BoxDecoration(
                  color: AppColors.surface,
                  borderRadius: BorderRadius.circular(16),
                  border: Border.all(color: AppColors.border),
                ),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Row(
                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                      children: [
                        const Text(
                          'Votre Évaluation par Critère',
                          style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold),
                        ),
                        Container(
                          padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                          decoration: BoxDecoration(
                            color: AppColors.ratingActive.withOpacity(0.15),
                            borderRadius: BorderRadius.circular(8),
                          ),
                          child: Text(
                            '$_overallRating ⭐',
                            style: const TextStyle(fontWeight: FontWeight.bold, color: Colors.orange),
                          ),
                        ),
                      ],
                    ),
                    const SizedBox(height: 16),
                    _buildInteractiveStarRow('🍔 Cuisine & Goût', _foodRating, (v) => setState(() => _foodRating = v)),
                    _buildInteractiveStarRow('🤝 Service & Accueil', _serviceRating, (v) => setState(() => _serviceRating = v)),
                    _buildInteractiveStarRow('💶 Rapport Qualité/Prix', _priceRating, (v) => setState(() => _priceRating = v)),
                    _buildInteractiveStarRow('🏠 Cadre & Ambiance', _ambianceRating, (v) => setState(() => _ambianceRating = v)),
                  ],
                ),
              ),

              const SizedBox(height: 24),
              const Text(
                'Titre de l\'avis (Optionnel)',
                style: TextStyle(
                  fontSize: 16,
                  fontWeight: FontWeight.bold,
                  color: AppColors.textPrimary,
                ),
              ),
              const SizedBox(height: 8),
              TextFormField(
                controller: _titleController,
                decoration: const InputDecoration(
                  hintText: 'Résumez votre expérience en quelques mots',
                ),
                maxLength: 100,
              ),
              const SizedBox(height: 12),
              const Text(
                'Votre Commentaire',
                style: TextStyle(
                  fontSize: 16,
                  fontWeight: FontWeight.bold,
                  color: AppColors.textPrimary,
                ),
              ),
              const SizedBox(height: 8),
              TextFormField(
                controller: _commentController,
                maxLines: 5,
                decoration: const InputDecoration(
                  hintText: 'Partagez votre expérience avec la communauté...',
                  alignLabelWithHint: true,
                ),
                validator: (value) {
                  if (value == null || value.trim().isEmpty) {
                    return 'Veuillez rédiger un commentaire';
                  }
                  if (value.trim().length < 10) {
                    return 'Votre commentaire doit contenir au moins 10 caractères';
                  }
                  return null;
                },
              ),
              const SizedBox(height: 28),
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
                      : const Text('Publier mon Avis', style: TextStyle(fontSize: 16)),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
