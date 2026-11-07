class Review {
  final String id;
  final String userId;
  final String userName;
  final String userAvatar;
  final String restaurantId;
  final String restaurantName;
  final String? restaurantImage;
  final double rating;
  final String? title;
  final String comment;
  final String? photoUrl;
  final DateTime createdAt;
  final DateTime? updatedAt;
  final int helpfulCount;
  final ReviewStatus status;

  // AI Multi-Agent ABSA Scores (1 to 10)
  final int? foodScore;
  final int? serviceScore;
  final int? priceScore;
  final int? ambianceScore;

  // AI Multi-Agent CoT Reasonings
  final String? foodThought;
  final String? serviceThought;
  final String? priceThought;
  final String? ambianceThought;

  // Anti-fraud anomaly detection
  final bool? isAnomaly;
  final String? anomalyReason;

  // Corrective RAG recommendations
  final List<RestaurantRecommendation> recommendations;

  Review({
    required this.id,
    required this.userId,
    required this.userName,
    required this.userAvatar,
    required this.restaurantId,
    required this.restaurantName,
    this.restaurantImage,
    required this.rating,
    this.title,
    required this.comment,
    this.photoUrl,
    required this.createdAt,
    this.updatedAt,
    this.helpfulCount = 0,
    this.status = ReviewStatus.published,
    this.foodScore,
    this.serviceScore,
    this.priceScore,
    this.ambianceScore,
    this.foodThought,
    this.serviceThought,
    this.priceThought,
    this.ambianceThought,
    this.isAnomaly,
    this.anomalyReason,
    this.recommendations = const [],
  });

  Review copyWith({
    String? id,
    String? userId,
    String? userName,
    String? userAvatar,
    String? restaurantId,
    String? restaurantName,
    String? restaurantImage,
    double? rating,
    String? title,
    String? comment,
    String? photoUrl,
    DateTime? createdAt,
    DateTime? updatedAt,
    int? helpfulCount,
    ReviewStatus? status,
    int? foodScore,
    int? serviceScore,
    int? priceScore,
    int? ambianceScore,
    String? foodThought,
    String? serviceThought,
    String? priceThought,
    String? ambianceThought,
    bool? isAnomaly,
    String? anomalyReason,
    List<RestaurantRecommendation>? recommendations,
  }) {
    return Review(
      id: id ?? this.id,
      userId: userId ?? this.userId,
      userName: userName ?? this.userName,
      userAvatar: userAvatar ?? this.userAvatar,
      restaurantId: restaurantId ?? this.restaurantId,
      restaurantName: restaurantName ?? this.restaurantName,
      restaurantImage: restaurantImage ?? this.restaurantImage,
      rating: rating ?? this.rating,
      title: title ?? this.title,
      comment: comment ?? this.comment,
      photoUrl: photoUrl ?? this.photoUrl,
      createdAt: createdAt ?? this.createdAt,
      updatedAt: updatedAt ?? this.updatedAt,
      helpfulCount: helpfulCount ?? this.helpfulCount,
      status: status ?? this.status,
      foodScore: foodScore ?? this.foodScore,
      serviceScore: serviceScore ?? this.serviceScore,
      priceScore: priceScore ?? this.priceScore,
      ambianceScore: ambianceScore ?? this.ambianceScore,
      foodThought: foodThought ?? this.foodThought,
      serviceThought: serviceThought ?? this.serviceThought,
      priceThought: priceThought ?? this.priceThought,
      ambianceThought: ambianceThought ?? this.ambianceThought,
      isAnomaly: isAnomaly ?? this.isAnomaly,
      anomalyReason: anomalyReason ?? this.anomalyReason,
      recommendations: recommendations ?? this.recommendations,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'userId': userId,
      'userName': userName,
      'userAvatar': userAvatar,
      'restaurantId': restaurantId,
      'restaurantName': restaurantName,
      'restaurantImage': restaurantImage,
      'rating': rating,
      'title': title,
      'comment': comment,
      'photoUrl': photoUrl,
      'createdAt': createdAt.toIso8601String(),
      'updatedAt': updatedAt?.toIso8601String(),
      'helpfulCount': helpfulCount,
      'status': status.name,
      'foodScore': foodScore,
      'serviceScore': serviceScore,
      'priceScore': priceScore,
      'ambianceScore': ambianceScore,
      'foodThought': foodThought,
      'serviceThought': serviceThought,
      'priceThought': priceThought,
      'ambianceThought': ambianceThought,
      'isAnomaly': isAnomaly,
      'anomalyReason': anomalyReason,
    };
  }

  factory Review.fromJson(Map<String, dynamic> json) {
    List<RestaurantRecommendation> parsedRecommendations = [];
    if (json['recommendations'] is List) {
      parsedRecommendations = (json['recommendations'] as List)
          .map((r) => RestaurantRecommendation.fromJson(Map<String, dynamic>.from(r)))
          .toList();
    }

    return Review(
      id: json['id'] ?? json['_id'] ?? '',
      userId: json['userId'] ?? '',
      userName: json['userName'] ?? '',
      userAvatar: json['userAvatar'] ?? '',
      restaurantId: json['restaurantId'] ?? '',
      restaurantName: json['restaurantName'] ?? '',
      restaurantImage: json['restaurantImage'],
      rating: (json['rating'] as num?)?.toDouble() ?? 0.0,
      title: json['title'],
      comment: json['comment'] ?? '',
      photoUrl: json['photoUrl'],
      createdAt: json['createdAt'] != null 
          ? DateTime.parse(json['createdAt']) 
          : DateTime.now(),
      updatedAt: json['updatedAt'] != null 
          ? DateTime.parse(json['updatedAt']) 
          : null,
      helpfulCount: json['helpfulCount'] ?? 0,
      status: ReviewStatus.values.firstWhere(
        (e) => e.name == json['status'],
        orElse: () => ReviewStatus.published,
      ),
      foodScore: (json['foodScore'] as num?)?.toInt(),
      serviceScore: (json['serviceScore'] as num?)?.toInt(),
      priceScore: (json['priceScore'] as num?)?.toInt(),
      ambianceScore: (json['ambianceScore'] as num?)?.toInt(),
      foodThought: json['foodThought'],
      serviceThought: json['serviceThought'],
      priceThought: json['priceThought'],
      ambianceThought: json['ambianceThought'],
      isAnomaly: json['isAnomaly'] as bool?,
      anomalyReason: json['anomalyReason'],
      recommendations: parsedRecommendations,
    );
  }
}

class RestaurantRecommendation {
  final String id;
  final String name;
  final String cuisine;
  final String description;
  final String image;
  final double rating;
  final String address;
  final String priceRange;
  final String recommendationReason;
  final String strongCriterion;

  RestaurantRecommendation({
    required this.id,
    required this.name,
    required this.cuisine,
    required this.description,
    required this.image,
    required this.rating,
    required this.address,
    required this.priceRange,
    required this.recommendationReason,
    required this.strongCriterion,
  });

  factory RestaurantRecommendation.fromJson(Map<String, dynamic> json) {
    return RestaurantRecommendation(
      id: json['id'] ?? '',
      name: json['name'] ?? '',
      cuisine: json['cuisine'] ?? '',
      description: json['description'] ?? '',
      image: json['image'] ?? '',
      rating: (json['rating'] as num?)?.toDouble() ?? 0.0,
      address: json['address'] ?? '',
      priceRange: json['priceRange'] ?? '',
      recommendationReason: json['recommendationReason'] ?? '',
      strongCriterion: json['strongCriterion'] ?? '',
    );
  }
}

enum ReviewStatus {
  pending,
  published,
  rejected,
  deleted,
}

extension ReviewStatusExtension on ReviewStatus {
  String get displayName {
    switch (this) {
      case ReviewStatus.pending:
        return 'Pending';
      case ReviewStatus.published:
        return 'Published';
      case ReviewStatus.rejected:
        return 'Rejected';
      case ReviewStatus.deleted:
        return 'Deleted';
    }
  }
}
