/// Simple dashboard stats for the owner's restaurant, as returned by
/// `/api/owner/stats`.
class OwnerStats {
  final int totalReservations;
  final int pendingReservations;
  final int confirmedReservations;
  final int completedReservations;
  final int totalMenuItems;
  final int totalEvents;
  final int totalReviews;
  final double? averageRating;

  OwnerStats({
    this.totalReservations = 0,
    this.pendingReservations = 0,
    this.confirmedReservations = 0,
    this.completedReservations = 0,
    this.totalMenuItems = 0,
    this.totalEvents = 0,
    this.totalReviews = 0,
    this.averageRating,
  });

  factory OwnerStats.fromJson(Map<String, dynamic> json) {
    return OwnerStats(
      totalReservations: json['totalReservations'] ?? 0,
      pendingReservations: json['pendingReservations'] ?? 0,
      confirmedReservations: json['confirmedReservations'] ?? 0,
      completedReservations: json['completedReservations'] ?? 0,
      totalMenuItems: json['totalMenuItems'] ?? 0,
      totalEvents: json['totalEvents'] ?? 0,
      totalReviews: json['totalReviews'] ?? 0,
      averageRating: (json['averageRating'] as num?)?.toDouble(),
    );
  }
}
