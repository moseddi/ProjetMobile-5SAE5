class Weather {
  final bool available;
  final String? message;
  final double? temperature, tempMin, tempMax, windSpeed;
  final int? precipitationProbability;
  final String? condition, icon;

  Weather({
    required this.available,
    this.message,
    this.temperature,
    this.tempMin,
    this.tempMax,
    this.windSpeed,
    this.precipitationProbability,
    this.condition,
    this.icon,
  });

  factory Weather.fromJson(Map<String, dynamic> j) => Weather(
        available: j['available'] ?? false,
        message: j['message'],
        temperature: (j['temperature'] as num?)?.toDouble(),
        tempMin: (j['tempMin'] as num?)?.toDouble(),
        tempMax: (j['tempMax'] as num?)?.toDouble(),
        windSpeed: (j['windSpeed'] as num?)?.toDouble(),
        precipitationProbability: j['precipitationProbability'],
        condition: j['condition'],
        icon: j['icon'],
      );
}