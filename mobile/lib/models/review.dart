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
    };
  }

  factory Review.fromJson(Map<String, dynamic> json) {
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
