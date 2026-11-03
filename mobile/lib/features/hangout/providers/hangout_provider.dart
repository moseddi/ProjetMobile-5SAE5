import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart' show DateTimeRange;
import '../../../models/hangout.dart';
import '../../../services/interfaces/hangout_service.dart';
import '../../../widgets/event_widgets.dart' show EventDateFilter, matchesDateFilter;

/// Which hangouts the feed shows.
enum HangOutScope { all, open, mine, joined }

/// A join request waiting for the organizer's answer, with the hangout it is for.
class IncomingRequest {
  final HangOut hangout;
  final JoinRequest request;

  const IncomingRequest(this.hangout, this.request);
}

/// Hang Out module state: the feed with its search / date / scope filters, the
/// requests other people sent to the signed-in user's hangouts, and the actions
/// (post, ask to join, accept, delete a request).
class HangOutProvider with ChangeNotifier {
  final HangOutService _hangoutService;

  HangOutProvider(this._hangoutService);

  List<HangOut> _hangouts = [];
  bool _isLoading = false;
  String? _error;
  String? _userId;

  String _query = '';
  EventDateFilter _dateFilter = EventDateFilter.upcoming;
  DateTimeRange? _customRange;
  HangOutScope _scope = HangOutScope.all;

  List<HangOut> get hangouts => _hangouts;
  bool get isLoading => _isLoading;
  String? get error => _error;
  String get query => _query;
  EventDateFilter get dateFilter => _dateFilter;
  DateTimeRange? get customRange => _customRange;
  HangOutScope get scope => _scope;

  bool get hasActiveFilters =>
      _query.isNotEmpty ||
      _dateFilter != EventDateFilter.upcoming ||
      _scope != HangOutScope.all;

  // ------------------------------------------------------------------ loading

  Future<void> load(String userId) async {
    // Another account signed in: never show the previous account's hangouts or filters.
    if (_userId != userId) {
      _hangouts = [];
      _userId = userId;
      resetFilters(notify: false);
    }

    _isLoading = true;
    _error = null;
    notifyListeners();

    try {
      _hangouts = await _hangoutService.getAllHangOuts();
    } catch (e) {
      _error = e.toString().replaceAll('Exception: ', '');
    } finally {
      _isLoading = false;
      notifyListeners();
    }
  }

  Future<void> _reload() async {
    if (_userId != null) await load(_userId!);
  }

  // --------------------------------------------------------------- derived data

  bool _isMine(HangOut h) => h.creatorId == _userId;
  bool _isJoined(HangOut h) => !_isMine(h) && h.myRequestStatus == JoinRequestStatus.accepted;

  /// Hangouts I organize.
  List<HangOut> get myHangOuts => _hangouts.where(_isMine).toList();

  /// Requests other people sent to my hangouts that I still have to answer (newest first).
  List<IncomingRequest> get incomingRequests {
    final result = <IncomingRequest>[];
    for (final hangout in _hangouts.where(_isMine)) {
      for (final request in hangout.pendingRequests) {
        result.add(IncomingRequest(hangout, request));
      }
    }
    result.sort((a, b) => b.request.requestedAt.compareTo(a.request.requestedAt));
    return result;
  }

  int get incomingRequestCount => incomingRequests.length;

  /// Hangouts I asked to join (waiting, accepted or declined), most recent first.
  List<HangOut> get myRequests {
    final list = _hangouts.where((h) => !_isMine(h) && h.myRequestStatus != null).toList();
    list.sort((a, b) => b.id.compareTo(a.id));
    return list;
  }

  int countFor(HangOutScope scope) => _hangouts.where((h) => _inScope(h, scope)).length;

  bool _inScope(HangOut h, HangOutScope scope) {
    switch (scope) {
      case HangOutScope.all:
        return true;
      case HangOutScope.open:
        return !h.isFull && !h.isPast;
      case HangOutScope.mine:
        return _isMine(h);
      case HangOutScope.joined:
        return _isJoined(h);
    }
  }

  /// The feed after search, date and scope filters, newest post first.
  List<HangOut> get visible {
    final q = _query.trim().toLowerCase();

    final list = _hangouts.where((h) {
      if (!_inScope(h, _scope)) return false;
      if (!matchesDateFilter(h.datetime, _dateFilter, _customRange)) return false;
      if (q.isNotEmpty) {
        final haystack = '${h.title} ${h.message} ${h.restaurantName} ${h.creatorName}'.toLowerCase();
        if (!haystack.contains(q)) return false;
      }
      return true;
    }).toList();

    // Most recently posted first (ids start with their creation time).
    list.sort((a, b) => b.id.compareTo(a.id));
    return list;
  }

  // -------------------------------------------------------------------- filters

  void setQuery(String value) {
    _query = value;
    notifyListeners();
  }

  void setDateFilter(EventDateFilter filter, DateTimeRange? range) {
    _dateFilter = filter;
    _customRange = range;
    notifyListeners();
  }

  void setScope(HangOutScope scope) {
    _scope = scope;
    notifyListeners();
  }

  void resetFilters({bool notify = true}) {
    _query = '';
    _dateFilter = EventDateFilter.upcoming;
    _customRange = null;
    _scope = HangOutScope.all;
    if (notify) notifyListeners();
  }

  // -------------------------------------------------------------------- actions
  // These throw with the server's message when refused; screens show it.

  Future<void> createHangOut(HangOut hangout) async {
    await _hangoutService.createHangOut(hangout);
    await _reload();
  }

  Future<void> requestToJoin(String hangoutId) async {
    await _hangoutService.requestToJoin(hangoutId);
    await _reload();
  }

  Future<void> acceptRequest(String hangoutId, String requestId) async {
    await _hangoutService.acceptRequest(hangoutId, requestId);
    await _reload();
  }

  /// "Delete" in the UI: the request is declined and the person is notified.
  Future<void> declineRequest(String hangoutId, String requestId) async {
    await _hangoutService.declineRequest(hangoutId, requestId);
    await _reload();
  }
}
