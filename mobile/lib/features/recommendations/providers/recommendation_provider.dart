import 'package:flutter/foundation.dart';
import '../../../models/recommendation.dart';
import '../../../services/interfaces/recommendation_service.dart';

/// Smart suggestions module: asks the server (Gemini) where the client should use their points.
class RecommendationProvider with ChangeNotifier {
  final RecommendationService _service;

  RecommendationProvider(this._service);

  Recommendation? _result;
  bool _isLoading = false;
  String? _error;

  Recommendation? get result => _result;
  bool get isLoading => _isLoading;
  String? get error => _error;

  Future<void> load() async {
    _isLoading = true;
    _error = null;
    notifyListeners();
    try {
      _result = await _service.getRecommendations();
    } catch (e) {
      _result = null;
      _error = e.toString().replaceAll('Exception: ', '');
    }
    _isLoading = false;
    notifyListeners();
  }

  void clear() {
    _result = null;
    _error = null;
    notifyListeners();
  }
}
