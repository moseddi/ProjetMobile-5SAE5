class GameResult {
  final String id;
  final String userId;
  final GameType gameType;
  final int pointsWon;
  final DateTime date;

  GameResult({
    required this.id,
    required this.userId,
    required this.gameType,
    required this.pointsWon,
    required this.date,
  });
}

enum GameType {
  cardGame,
  luckyWheel,
}

extension GameTypeExtension on GameType {
  String get displayName {
    switch (this) {
      case GameType.cardGame:
        return 'Card Game';
      case GameType.luckyWheel:
        return 'Lucky Wheel';
    }
  }
}
