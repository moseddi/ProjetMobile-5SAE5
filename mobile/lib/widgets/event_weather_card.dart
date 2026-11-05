import 'package:flutter/material.dart';
import '../models/weather.dart';

class EventWeatherCard extends StatelessWidget {
  final Future<Weather> future;
  const EventWeatherCard({super.key, required this.future});

  IconData _icon(String? k) {
    switch (k) {
      case 'sunny': return Icons.wb_sunny;
      case 'rain': return Icons.grain;
      case 'snow': return Icons.ac_unit;
      case 'storm': return Icons.thunderstorm;
      case 'fog': return Icons.cloud;
      default: return Icons.cloud;
    }
  }

  @override
  Widget build(BuildContext context) {
    return FutureBuilder<Weather>(
      future: future,
      builder: (context, snap) {
        if (snap.connectionState != ConnectionState.done) {
          return const Padding(
            padding: EdgeInsets.all(16),
            child: Center(child: CircularProgressIndicator()),
          );
        }
        final w = snap.data;
        if (w == null || !w.available) {
          return Card(
            child: ListTile(
              leading: const Icon(Icons.cloud_off),
              title: const Text('Météo indisponible'),
              subtitle: Text(w?.message ?? ''),
            ),
          );
        }
        return Card(
          child: Padding(
            padding: const EdgeInsets.all(16),
            child: Row(
              children: [
                Icon(_icon(w.icon), size: 48),
                const SizedBox(width: 16),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text('${w.temperature?.round()}°C · ${w.condition}',
                          style: Theme.of(context).textTheme.titleMedium),
                      const SizedBox(height: 4),
                      Text('Min ${w.tempMin?.round()}° / Max ${w.tempMax?.round()}°'),
                      Text('Pluie : ${w.precipitationProbability}%  ·  Vent : ${w.windSpeed?.round()} km/h'),
                    ],
                  ),
                ),
              ],
            ),
          ),
        );
      },
    );
  }
}