/// Platform-wide counters shown on the admin dashboard, as returned by
/// `GET /api/admin/stats`.
class AdminStats {
  final int totalUsers;
  final int totalClients;
  final int totalRestaurantOwners;
  final int totalAdmins;
  final int totalRestaurants;
  final int unassignedRestaurants;
  final int totalReservations;
  final int totalReviews;
  final int totalHangOuts;
  final int totalEvents;
  final int pendingOwnerApplications;

  AdminStats({
    required this.totalUsers,
    required this.totalClients,
    required this.totalRestaurantOwners,
    required this.totalAdmins,
    required this.totalRestaurants,
    required this.unassignedRestaurants,
    required this.totalReservations,
    required this.totalReviews,
    required this.totalHangOuts,
    required this.totalEvents,
    this.pendingOwnerApplications = 0,
  });

  factory AdminStats.fromJson(Map<String, dynamic> json) {
    int asInt(dynamic value) {
      if (value is int) return value;
      if (value is num) return value.toInt();
      return 0;
    }

    return AdminStats(
      totalUsers: asInt(json['totalUsers']),
      totalClients: asInt(json['totalClients']),
      totalRestaurantOwners: asInt(json['totalRestaurantOwners']),
      totalAdmins: asInt(json['totalAdmins']),
      totalRestaurants: asInt(json['totalRestaurants']),
      unassignedRestaurants: asInt(json['unassignedRestaurants']),
      totalReservations: asInt(json['totalReservations']),
      totalReviews: asInt(json['totalReviews']),
      totalHangOuts: asInt(json['totalHangOuts']),
      totalEvents: asInt(json['totalEvents']),
      pendingOwnerApplications: asInt(json['pendingOwnerApplications']),
    );
  }
}
