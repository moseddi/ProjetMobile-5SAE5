import '../../core/api/api_client.dart';
import '../../models/hangout.dart';
import '../interfaces/hangout_service.dart';

class ApiHangOutService implements HangOutService {
  @override
  Future<List<HangOut>> getAllHangOuts() async {
    final response = await ApiClient.get('/hangouts');
    final data = ApiClient.handleResponse(response);

    if (data == null) return [];

    return (data as List).map((json) => HangOut.fromJson(json)).toList();
  }

  @override
  Future<HangOut> createHangOut(HangOut hangout) async {
    final rawTitle = hangout.message.trim();
    final title = rawTitle.isEmpty
        ? 'Hang Out at ${hangout.restaurantName}'
        : (rawTitle.length > 60 ? rawTitle.substring(0, 60) : rawTitle);

    final body = {
      'restaurantId': hangout.restaurantId,
      'title': title,
      'description': hangout.message,
      'plannedDateTime': hangout.datetime.toIso8601String(),
      'maxParticipants': hangout.maxParticipants,
      'restaurantLatitude': hangout.restaurantLatitude,
      'restaurantLongitude': hangout.restaurantLongitude,
      'meetingPointName': hangout.meetingPointName,
      'meetingPointLatitude': hangout.meetingPointLatitude,
      'meetingPointLongitude': hangout.meetingPointLongitude,
    };

    final response = await ApiClient.post('/hangouts', body);
    final data = ApiClient.handleResponse(response);
    return HangOut.fromJson(data);
  }

  @override
  Future<HangOut> requestToJoin(String hangoutId) async {
    final response = await ApiClient.post('/hangouts/$hangoutId/join', {});
    return HangOut.fromJson(ApiClient.handleResponse(response));
  }

  @override
  Future<void> acceptRequest(String hangoutId, String requestId) async {
    final response = await ApiClient.put('/hangouts/$hangoutId/requests/$requestId/accept', {});
    ApiClient.handleResponse(response);
  }

  @override
  Future<void> declineRequest(String hangoutId, String requestId) async {
    final response = await ApiClient.put('/hangouts/$hangoutId/requests/$requestId/decline', {});
    ApiClient.handleResponse(response);
  }

  /// Update restaurant GPS coordinates for a hangout
  Future<HangOut> updateRestaurantCoordinates(
    String hangoutId,
    double latitude,
    double longitude,
  ) async {
    final body = {
      'restaurantLatitude': latitude,
      'restaurantLongitude': longitude,
    };
    final response = await ApiClient.put(
      '/hangouts/$hangoutId/restaurant-coordinates',
      body,
    );
    return HangOut.fromJson(ApiClient.handleResponse(response));
  }

  /// Update meeting point details for a hangout
  Future<HangOut> updateMeetingPoint(
    String hangoutId,
    String? meetingPointName,
    double? latitude,
    double? longitude,
  ) async {
    final body = {
      'meetingPointName': meetingPointName,
      'meetingPointLatitude': latitude,
      'meetingPointLongitude': longitude,
    };
    final response = await ApiClient.put(
      '/hangouts/$hangoutId/meeting-point',
      body,
    );
    return HangOut.fromJson(ApiClient.handleResponse(response));
  }
}
