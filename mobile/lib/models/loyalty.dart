/// The four daily mini-games. They only ever give points.
enum GameType { luckyWheel, scratchCard, cardPick, luckyDice }

extension GameTypeApi on GameType {
  /// Name used in the API path (/games/{name}/play) and in responses.
  String get apiName {
    switch (this) {
      case GameType.luckyWheel:
        return 'LUCKY_WHEEL';
      case GameType.scratchCard:
        return 'SCRATCH_CARD';
      case GameType.cardPick:
        return 'CARD_MEMORY';
      case GameType.luckyDice:
        return 'LUCKY_DICE';
    }
  }

  static GameType? fromApi(String? value) {
    for (final type in GameType.values) {
      if (type.apiName == value) return type;
    }
    return null;
  }
}

/// Where the user stands with one daily game.
class GameStatus {
  final GameType type;
  final String title;
  final String description;
  final bool playedToday;

  /// Another game was already played today (one game per day).
  final bool locked;
  final int? lastPoints;
  final String? lastRestaurantName;

  const GameStatus({
    required this.type,
    required this.title,
    required this.description,
    required this.playedToday,
    this.locked = false,
    this.lastPoints,
    this.lastRestaurantName,
  });

  static GameStatus? tryParse(Map<String, dynamic> json) {
    final type = GameTypeApi.fromApi(json['gameType']);
    if (type == null) return null;
    return GameStatus(
      type: type,
      title: json['title'] ?? '',
      description: json['description'] ?? '',
      playedToday: json['playedToday'] == true,
      locked: json['locked'] == true,
      lastPoints: (json['lastPoints'] as num?)?.toInt(),
      lastRestaurantName: json['lastRestaurantName'],
    );
  }
}

/// One exchange a restaurant offers, and whether the user can afford it.
class RewardOffer {
  final String type;
  final String title;
  final String description;
  final int cost;
  final bool canRedeem;
  final int missingPoints;

  const RewardOffer({
    required this.type,
    required this.title,
    required this.description,
    required this.cost,
    required this.canRedeem,
    required this.missingPoints,
  });

  factory RewardOffer.fromJson(Map<String, dynamic> json) {
    return RewardOffer(
      type: json['type'] ?? '',
      title: json['title'] ?? '',
      description: json['description'] ?? '',
      cost: (json['cost'] as num?)?.toInt() ?? 0,
      canRedeem: json['canRedeem'] == true,
      missingPoints: (json['missingPoints'] as num?)?.toInt() ?? 0,
    );
  }
}

/// The user's points at one restaurant, with every exchange available there.
class RestaurantLoyalty {
  final String restaurantId;
  final String name;
  final String image;
  final String cuisine;
  final int points;

  /// False when the owner switched the loyalty programme off.
  final bool loyaltyEnabled;
  final List<RewardOffer> offers;

  const RestaurantLoyalty({
    required this.restaurantId,
    required this.name,
    required this.image,
    required this.cuisine,
    required this.points,
    this.loyaltyEnabled = true,
    required this.offers,
  });

  int get affordableCount => offers.where((o) => o.canRedeem).length;

  factory RestaurantLoyalty.fromJson(Map<String, dynamic> json) {
    return RestaurantLoyalty(
      restaurantId: json['restaurantId'] ?? '',
      name: json['name'] ?? '',
      image: json['image'] ?? '',
      cuisine: json['cuisine'] ?? '',
      points: (json['points'] as num?)?.toInt() ?? 0,
      loyaltyEnabled: json['loyaltyEnabled'] != false,
      offers: ((json['offers'] as List?) ?? const [])
          .map((o) => RewardOffer.fromJson(o as Map<String, dynamic>))
          .toList(),
    );
  }
}

/// Everything the Loyalty screen shows, from `GET /loyalty/overview`.
class LoyaltyOverview {
  final int totalPoints;

  /// The client's permanent QR value.
  final String memberCode;
  final int unassignedPoints;
  final List<GameStatus> games;
  final List<RestaurantLoyalty> restaurants;

  const LoyaltyOverview({
    required this.totalPoints,
    required this.memberCode,
    required this.unassignedPoints,
    required this.games,
    required this.restaurants,
  });

  factory LoyaltyOverview.fromJson(Map<String, dynamic> json) {
    return LoyaltyOverview(
      totalPoints: (json['totalPoints'] as num?)?.toInt() ?? 0,
      memberCode: json['memberCode'] ?? '',
      unassignedPoints: (json['unassignedPoints'] as num?)?.toInt() ?? 0,
      games: ((json['games'] as List?) ?? const [])
          .map((g) => GameStatus.tryParse(g as Map<String, dynamic>))
          .whereType<GameStatus>()
          .toList(),
      restaurants: ((json['restaurants'] as List?) ?? const [])
          .map((r) => RestaurantLoyalty.fromJson(r as Map<String, dynamic>))
          .toList(),
    );
  }
}

/// What the server drew when the user played a game.
class GamePlayResult {
  final GameType type;
  final String restaurantId;
  final String restaurantName;
  final int pointsEarned;
  final int totalPoints;
  final int restaurantPoints;

  const GamePlayResult({
    required this.type,
    required this.restaurantId,
    required this.restaurantName,
    required this.pointsEarned,
    required this.totalPoints,
    required this.restaurantPoints,
  });

  factory GamePlayResult.fromJson(Map<String, dynamic> json) {
    return GamePlayResult(
      type: GameTypeApi.fromApi(json['gameType']) ?? GameType.luckyWheel,
      restaurantId: json['restaurantId'] ?? '',
      restaurantName: json['restaurantName'] ?? '',
      pointsEarned: (json['pointsEarned'] as num?)?.toInt() ?? 0,
      totalPoints: (json['totalPoints'] as num?)?.toInt() ?? 0,
      restaurantPoints: (json['restaurantPoints'] as num?)?.toInt() ?? 0,
    );
  }
}

/// A reward the user bought with points, to show at the restaurant.
class Voucher {
  final String id;
  final String code;
  final String restaurantId;
  final String restaurantName;
  final String restaurantImage;
  final String rewardType;
  final String title;
  final String description;
  final int pointsSpent;
  final String status; // ACTIVE, USED, EXPIRED
  final DateTime? createdAt;
  final DateTime? expiresAt;

  const Voucher({
    required this.id,
    required this.code,
    required this.restaurantId,
    required this.restaurantName,
    required this.restaurantImage,
    required this.rewardType,
    required this.title,
    required this.description,
    required this.pointsSpent,
    required this.status,
    this.createdAt,
    this.expiresAt,
  });

  bool get isActive => status == 'ACTIVE';

  factory Voucher.fromJson(Map<String, dynamic> json) {
    DateTime? date(dynamic v) => v == null ? null : DateTime.tryParse(v.toString());
    return Voucher(
      id: json['id'] ?? '',
      code: json['code'] ?? '',
      restaurantId: json['restaurantId'] ?? '',
      restaurantName: json['restaurantName'] ?? '',
      restaurantImage: json['restaurantImage'] ?? '',
      rewardType: json['rewardType'] ?? '',
      title: json['title'] ?? '',
      description: json['description'] ?? '',
      pointsSpent: (json['pointsSpent'] as num?)?.toInt() ?? 0,
      status: json['status'] ?? 'ACTIVE',
      createdAt: date(json['createdAt']),
      expiresAt: date(json['expiresAt']),
    );
  }
}

/// What the cashier sees after scanning a client's QR (limited to the cashier's restaurant).
class MemberLookup {
  final String memberCode;
  final String name;
  final String avatar;
  final DateTime? memberSince;
  final int pointsAtRestaurant;
  final String restaurantName;
  final List<Voucher> activeVouchers;
  final int usedVouchers;

  const MemberLookup({
    required this.memberCode,
    required this.name,
    required this.avatar,
    required this.memberSince,
    required this.pointsAtRestaurant,
    required this.restaurantName,
    required this.activeVouchers,
    required this.usedVouchers,
  });

  factory MemberLookup.fromJson(Map<String, dynamic> json) {
    return MemberLookup(
      memberCode: json['memberCode'] ?? '',
      name: json['name'] ?? '',
      avatar: json['avatar'] ?? '',
      memberSince: json['memberSince'] == null ? null : DateTime.tryParse(json['memberSince'].toString()),
      pointsAtRestaurant: (json['pointsAtRestaurant'] as num?)?.toInt() ?? 0,
      restaurantName: json['restaurantName'] ?? '',
      activeVouchers: ((json['activeVouchers'] as List?) ?? const [])
          .map((v) => Voucher.fromJson(v as Map<String, dynamic>))
          .toList(),
      usedVouchers: (json['usedVouchers'] as num?)?.toInt() ?? 0,
    );
  }
}
