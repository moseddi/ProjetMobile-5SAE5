class HangOut {
  final String id;
  final String creatorId;
  final String creatorName;
  final String creatorAvatar;
  final String restaurantId;
  final String restaurantName;
  final String restaurantImage;
  final DateTime datetime;
  final String message;
  final int maxParticipants;
  final List<String> participants;
  final List<JoinRequest> pendingRequests;

  HangOut({
    required this.id,
    required this.creatorId,
    required this.creatorName,
    required this.creatorAvatar,
    required this.restaurantId,
    required this.restaurantName,
    required this.restaurantImage,
    required this.datetime,
    required this.message,
    required this.maxParticipants,
    required this.participants,
    required this.pendingRequests,
  });

  bool get isFull => participants.length >= maxParticipants;
  
  int get availableSpots => maxParticipants - participants.length;

  HangOut copyWith({
    String? id,
    String? creatorId,
    String? creatorName,
    String? creatorAvatar,
    String? restaurantId,
    String? restaurantName,
    String? restaurantImage,
    DateTime? datetime,
    String? message,
    int? maxParticipants,
    List<String>? participants,
    List<JoinRequest>? pendingRequests,
  }) {
    return HangOut(
      id: id ?? this.id,
      creatorId: creatorId ?? this.creatorId,
      creatorName: creatorName ?? this.creatorName,
      creatorAvatar: creatorAvatar ?? this.creatorAvatar,
      restaurantId: restaurantId ?? this.restaurantId,
      restaurantName: restaurantName ?? this.restaurantName,
      restaurantImage: restaurantImage ?? this.restaurantImage,
      datetime: datetime ?? this.datetime,
      message: message ?? this.message,
      maxParticipants: maxParticipants ?? this.maxParticipants,
      participants: participants ?? this.participants,
      pendingRequests: pendingRequests ?? this.pendingRequests,
    );
  }
}

class JoinRequest {
  final String id;
  final String userId;
  final String userName;
  final String userAvatar;
  final String hangoutId;
  final DateTime requestedAt;
  final JoinRequestStatus status;

  JoinRequest({
    required this.id,
    required this.userId,
    required this.userName,
    required this.userAvatar,
    required this.hangoutId,
    required this.requestedAt,
    required this.status,
  });

  JoinRequest copyWith({
    String? id,
    String? userId,
    String? userName,
    String? userAvatar,
    String? hangoutId,
    DateTime? requestedAt,
    JoinRequestStatus? status,
  }) {
    return JoinRequest(
      id: id ?? this.id,
      userId: userId ?? this.userId,
      userName: userName ?? this.userName,
      userAvatar: userAvatar ?? this.userAvatar,
      hangoutId: hangoutId ?? this.hangoutId,
      requestedAt: requestedAt ?? this.requestedAt,
      status: status ?? this.status,
    );
  }
}

enum JoinRequestStatus {
  pending,
  accepted,
  declined,
}

extension JoinRequestStatusExtension on JoinRequestStatus {
  String get displayName {
    switch (this) {
      case JoinRequestStatus.pending:
        return 'Pending';
      case JoinRequestStatus.accepted:
        return 'Accepted';
      case JoinRequestStatus.declined:
        return 'Declined';
    }
  }
}
