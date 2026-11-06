/// A person already in a hangout (accepted by the organizer).
class HangOutMember {
  final String userId;
  final String name;
  final String avatar;

  const HangOutMember({required this.userId, required this.name, required this.avatar});

  factory HangOutMember.fromJson(Map<String, dynamic> json) {
    return HangOutMember(
      userId: json['userId'] ?? '',
      name: json['userName'] ?? '',
      avatar: json['userAvatar'] ?? '',
    );
  }
}

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

  /// Short title the organizer's text was summarised into (may equal [message]).
  final String title;

  /// Accepted members with their names/avatars, for showing who is going.
  final List<HangOutMember> members;

  /// The signed-in user's own request on this hangout, or null if they never asked.
  final JoinRequestStatus? myRequestStatus;

  /// GPS Coordinates of the restaurant
  final double? restaurantLatitude;
  final double? restaurantLongitude;

  /// Meeting point details
  final String? meetingPointName;
  final double? meetingPointLatitude;
  final double? meetingPointLongitude;

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
    this.title = '',
    this.members = const [],
    this.myRequestStatus,
    this.restaurantLatitude,
    this.restaurantLongitude,
    this.meetingPointName,
    this.meetingPointLatitude,
    this.meetingPointLongitude,
  });

  bool get isPast => datetime.isBefore(DateTime.now());

  bool get isFull => participants.length >= maxParticipants;

  int get availableSpots => maxParticipants - participants.length;

  factory HangOut.fromJson(Map<String, dynamic> json) {
    final id = json['id'] ?? '';
    final participantsJson = (json['participants'] as List?) ?? const [];
    final pendingJson = (json['pendingRequests'] as List?) ?? const [];

    return HangOut(
      id: id,
      creatorId: json['creatorId'] ?? '',
      creatorName: json['creatorName'] ?? '',
      creatorAvatar: json['creatorAvatar'] ?? '',
      restaurantId: json['restaurantId'] ?? '',
      restaurantName: json['restaurantName'] ?? '',
      restaurantImage: json['restaurantImage'] ?? '',
      datetime: json['plannedDateTime'] != null
          ? DateTime.parse(json['plannedDateTime'])
          : DateTime.now(),
      message: json['description'] ?? '',
      maxParticipants: json['maxParticipants'] ?? 0,
      participants: participantsJson.map((p) => (p['userId'] ?? '') as String).toList(),
      pendingRequests: pendingJson.map((p) => JoinRequest.fromJson(p, id)).toList(),
      title: json['title'] ?? '',
      members: participantsJson.map((p) => HangOutMember.fromJson(p)).toList(),
      myRequestStatus: json['myRequestStatus'] == null
          ? null
          : JoinRequest.statusFromString(json['myRequestStatus']),
      restaurantLatitude: json['restaurantLatitude'] != null
          ? (json['restaurantLatitude'] as num).toDouble()
          : null,
      restaurantLongitude: json['restaurantLongitude'] != null
          ? (json['restaurantLongitude'] as num).toDouble()
          : null,
      meetingPointName: json['meetingPointName'] ?? '',
      meetingPointLatitude: json['meetingPointLatitude'] != null
          ? (json['meetingPointLatitude'] as num).toDouble()
          : null,
      meetingPointLongitude: json['meetingPointLongitude'] != null
          ? (json['meetingPointLongitude'] as num).toDouble()
          : null,
    );
  }

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
    String? title,
    List<HangOutMember>? members,
    JoinRequestStatus? myRequestStatus,
    double? restaurantLatitude,
    double? restaurantLongitude,
    String? meetingPointName,
    double? meetingPointLatitude,
    double? meetingPointLongitude,
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
      title: title ?? this.title,
      members: members ?? this.members,
      myRequestStatus: myRequestStatus ?? this.myRequestStatus,
      restaurantLatitude: restaurantLatitude ?? this.restaurantLatitude,
      restaurantLongitude: restaurantLongitude ?? this.restaurantLongitude,
      meetingPointName: meetingPointName ?? this.meetingPointName,
      meetingPointLatitude: meetingPointLatitude ?? this.meetingPointLatitude,
      meetingPointLongitude: meetingPointLongitude ?? this.meetingPointLongitude,
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

  factory JoinRequest.fromJson(Map<String, dynamic> json, String hangoutId) {
    return JoinRequest(
      id: json['id'] ?? '',
      userId: json['userId'] ?? '',
      userName: json['userName'] ?? '',
      userAvatar: json['userAvatar'] ?? '',
      hangoutId: hangoutId,
      requestedAt: json['requestedAt'] != null
          ? DateTime.parse(json['requestedAt'])
          : DateTime.now(),
      status: statusFromString(json['status']),
    );
  }

  static JoinRequestStatus statusFromString(String? value) {
    switch (value) {
      case 'ACCEPTED':
        return JoinRequestStatus.accepted;
      case 'DECLINED':
        return JoinRequestStatus.declined;
      case 'PENDING':
      default:
        return JoinRequestStatus.pending;
    }
  }

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
