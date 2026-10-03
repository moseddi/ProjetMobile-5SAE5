import '../../core/constants/app_constants.dart';
import '../../models/notification.dart';
import '../interfaces/profile_service.dart';

class MockProfileService implements ProfileService {
  final List<AppNotification> _notifications = [];

  @override
  Future<List<AppNotification>> getNotifications(String userId) async {
    await Future.delayed(AppConstants.shortDelay);
    return _notifications.where((n) => n.userId == userId).toList();
  }

  @override
  Future<void> markNotificationAsRead(String notificationId) async {
    await Future.delayed(AppConstants.shortDelay);
    final index = _notifications.indexWhere((n) => n.id == notificationId);
    if (index != -1) {
      _notifications[index] = _notifications[index].copyWith(read: true);
    }
  }

  @override
  Future<void> addNotification(AppNotification notification) async {
    await Future.delayed(AppConstants.shortDelay);
    _notifications.add(notification);
  }
}
