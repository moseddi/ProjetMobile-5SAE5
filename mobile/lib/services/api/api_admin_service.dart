import '../../core/api/api_client.dart';
import '../../models/admin_stats.dart';
import '../../models/admin_user.dart';
import '../../models/owner_application.dart';
import '../../models/reservation.dart';
import '../../models/restaurant.dart';
import '../../models/review.dart';
import '../interfaces/admin_service.dart';

class ApiAdminService implements AdminService {
  @override
  Future<List<AdminUser>> getUsers({String? role}) async {
    final endpoint = role != null ? '/admin/users?role=$role' : '/admin/users';
    final response = await ApiClient.get(endpoint);
    final data = ApiClient.handleResponse(response);

    if (data == null) return [];

    return (data as List)
        .map((json) => AdminUser.fromJson(json))
        .toList();
  }

  @override
  Future<AdminUser> getUserById(String id) async {
    final response = await ApiClient.get('/admin/users/$id');
    final data = ApiClient.handleResponse(response);
    return AdminUser.fromJson(data);
  }

  @override
  Future<AdminUser> setUserActive(String id, bool active) async {
    final response = await ApiClient.put('/admin/users/$id/status', {'active': active});
    final data = ApiClient.handleResponse(response);
    return AdminUser.fromJson(data);
  }

  @override
  Future<AdminUser> setUserRole(String id, String role) async {
    final response = await ApiClient.put('/admin/users/$id/role', {'role': role});
    final data = ApiClient.handleResponse(response);
    return AdminUser.fromJson(data);
  }

  @override
  Future<AdminUser> assignOwner(String userId, String restaurantId) async {
    final response = await ApiClient.post(
      '/admin/users/$userId/assign-owner',
      {'restaurantId': restaurantId},
    );
    final data = ApiClient.handleResponse(response);
    return AdminUser.fromJson(data);
  }

  @override
  Future<AdminUser> unassignOwner(String userId) async {
    final response = await ApiClient.post('/admin/users/$userId/unassign-owner', {});
    final data = ApiClient.handleResponse(response);
    return AdminUser.fromJson(data);
  }

  @override
  Future<void> deleteUser(String id) async {
    final response = await ApiClient.delete('/admin/users/$id');
    ApiClient.handleResponse(response);
  }

  @override
  Future<Restaurant> createRestaurant(Map<String, dynamic> data) async {
    final response = await ApiClient.post('/restaurants', data);
    final result = ApiClient.handleResponse(response);
    return Restaurant.fromJson(result);
  }

  @override
  Future<Restaurant> updateRestaurant(String id, Map<String, dynamic> data) async {
    final response = await ApiClient.put('/restaurants/$id', data);
    final result = ApiClient.handleResponse(response);
    return Restaurant.fromJson(result);
  }

  @override
  Future<void> deleteRestaurant(String id) async {
    final response = await ApiClient.delete('/restaurants/$id');
    ApiClient.handleResponse(response);
  }

  @override
  Future<Map<String, dynamic>> getRestaurantRaw(String id) async {
    final response = await ApiClient.get('/restaurants/$id');
    final data = ApiClient.handleResponse(response);
    if (data is Map<String, dynamic>) return data;
    return <String, dynamic>{};
  }

  @override
  Future<List<Review>> getAllReviews() async {
    final response = await ApiClient.get('/admin/reviews');
    final data = ApiClient.handleResponse(response);

    if (data == null) return [];

    return (data as List)
        .map((json) => Review.fromJson(json))
        .toList();
  }

  @override
  Future<void> deleteReview(String id) async {
    final response = await ApiClient.delete('/admin/reviews/$id');
    ApiClient.handleResponse(response);
  }

  @override
  Future<List<Reservation>> getAllReservations() async {
    final response = await ApiClient.get('/admin/reservations');
    final data = ApiClient.handleResponse(response);

    if (data == null) return [];

    return (data as List)
        .map((json) => Reservation.fromJson(json))
        .toList();
  }

  @override
  Future<AdminStats> getStats() async {
    final response = await ApiClient.get('/admin/stats');
    final data = ApiClient.handleResponse(response);
    return AdminStats.fromJson(data);
  }

  @override
  Future<List<OwnerApplication>> getOwnerApplications({String? status}) async {
    final endpoint = status != null ? '/admin/owner-applications?status=$status' : '/admin/owner-applications';
    final data = ApiClient.handleResponse(await ApiClient.get(endpoint));
    if (data == null) return [];
    return (data as List).map((json) => OwnerApplication.fromJson(json)).toList();
  }

  @override
  Future<OwnerApplication> acceptOwnerApplication(String id) async {
    final data = ApiClient.handleResponse(await ApiClient.post('/admin/owner-applications/$id/accept', {}));
    return OwnerApplication.fromJson(data);
  }

  @override
  Future<OwnerApplication> rejectOwnerApplication(String id, {String? reason}) async {
    final data = ApiClient.handleResponse(
      await ApiClient.post('/admin/owner-applications/$id/reject', {'reason': reason ?? ''}),
    );
    return OwnerApplication.fromJson(data);
  }

  @override
  Future<Restaurant> createRestaurantWithOwner(
    Map<String, dynamic> restaurant, {
    Map<String, dynamic>? newOwner,
    String? existingOwnerId,
  }) async {
    final body = <String, dynamic>{
      'restaurant': restaurant,
      if (newOwner != null) 'owner': newOwner,
      if (existingOwnerId != null) 'existingOwnerId': existingOwnerId,
    };
    final data = ApiClient.handleResponse(await ApiClient.post('/admin/restaurants', body));
    return Restaurant.fromJson(data);
  }
}
