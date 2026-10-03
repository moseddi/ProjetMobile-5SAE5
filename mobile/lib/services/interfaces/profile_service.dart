import '../../models/notification.dart';

abstract class ProfileService {
  Future<List<AppNotification>> getNotifications(String userId);
  Future<void> markNotificationAsRead(String notificationId);
  Future<void> addNotification(AppNotification notification);
}
