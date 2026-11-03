import '../../core/api/api_client.dart';
import '../../models/notification.dart';
import '../interfaces/profile_service.dart';

class ApiProfileService implements ProfileService {
  @override
  Future<List<AppNotification>> getNotifications(String userId) async {
    final response = await ApiClient.get('/notifications');
    final data = ApiClient.handleResponse(response);

    if (data == null) return [];

    return (data as List).map((json) => AppNotification.fromJson(json)).toList();
  }

  @override
  Future<void> markNotificationAsRead(String notificationId) async {
    final response = await ApiClient.put('/notifications/$notificationId/read', {});
    ApiClient.handleResponse(response);
  }

  @override
  Future<void> addNotification(AppNotification notification) async {
    // Notifications are only ever created server-side as a side effect of a real
    // action (reservation update, hangout request, review, ...); clients don't post them directly.
  }
}
