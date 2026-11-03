import 'package:flutter/foundation.dart';
import '../../../models/notification.dart';
import '../../../services/interfaces/profile_service.dart';

class ProfileProvider with ChangeNotifier {
  final ProfileService _profileService;

  List<AppNotification> _notifications = [];
  bool _isLoading = false;
  String? _userId;

  ProfileProvider(this._profileService);

  List<AppNotification> get notifications => _notifications;
  bool get isLoading => _isLoading;
  int get unreadCount => _notifications.where((n) => !n.read).length;

  Future<void> loadNotifications(String userId) async {
    _userId = userId;
    _isLoading = true;
    notifyListeners();

    _notifications = await _profileService.getNotifications(userId);

    _isLoading = false;
    notifyListeners();
  }

  Future<void> markAsRead(String notificationId) async {
    await _profileService.markNotificationAsRead(notificationId);
    if (_userId != null) {
      await loadNotifications(_userId!);
    }
  }

  Future<void> addNotification(AppNotification notification) async {
    await _profileService.addNotification(notification);
    _notifications.insert(0, notification);
    notifyListeners();
  }
}
