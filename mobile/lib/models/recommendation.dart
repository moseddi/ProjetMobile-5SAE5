enum SuggestionType { exchange, save, visit, play }

/// One piece of advice, already checked against real data by the server.
class Suggestion {
  final SuggestionType type;
  final String title;
  final String reason;
  final String restaurantId;
  final String restaurantName;
  final String restaurantImage;
  final String rewardType;
  final String rewardTitle;
  final int cost;
  final int restaurantPoints;
  final bool canRedeem;
  final int missingPoints;

  const Suggestion({
    required this.type,
    required this.title,
    required this.reason,
    required this.restaurantId,
    required this.restaurantName,
    required this.restaurantImage,
    required this.rewardType,
    required this.rewardTitle,
    required this.cost,
    required this.restaurantPoints,
    required this.canRedeem,
    required this.missingPoints,
  });

  bool get hasReward => rewardType.isNotEmpty && cost > 0;

  factory Suggestion.fromJson(Map<String, dynamic> json) {
    SuggestionType typeOf(String? v) {
      switch (v) {
        case 'EXCHANGE':
          return SuggestionType.exchange;
        case 'SAVE':
          return SuggestionType.save;
        case 'PLAY':
          return SuggestionType.play;
        default:
          return SuggestionType.visit;
      }
    }

    return Suggestion(
      type: typeOf(json['type']),
      title: json['title'] ?? '',
      reason: json['reason'] ?? '',
      restaurantId: json['restaurantId'] ?? '',
      restaurantName: json['restaurantName'] ?? '',
      restaurantImage: json['restaurantImage'] ?? '',
      rewardType: json['rewardType'] ?? '',
      rewardTitle: json['rewardTitle'] ?? '',
      cost: (json['cost'] as num?)?.toInt() ?? 0,
      restaurantPoints: (json['restaurantPoints'] as num?)?.toInt() ?? 0,
      canRedeem: json['canRedeem'] == true,
      missingPoints: (json['missingPoints'] as num?)?.toInt() ?? 0,
    );
  }
}

class Recommendation {
  /// GEMINI when the language model wrote them, FALLBACK for built-in rules.
  final String source;
  final String summary;
  final String? notice;
  final List<Suggestion> suggestions;

  const Recommendation({
    required this.source,
    required this.summary,
    required this.notice,
    required this.suggestions,
  });

  bool get isGemini => source == 'GEMINI';

  factory Recommendation.fromJson(Map<String, dynamic> json) {
    return Recommendation(
      source: json['source'] ?? 'FALLBACK',
      summary: json['summary'] ?? '',
      notice: json['notice'],
      suggestions: ((json['suggestions'] as List?) ?? const [])
          .map((s) => Suggestion.fromJson(s as Map<String, dynamic>))
          .toList(),
    );
  }
}
