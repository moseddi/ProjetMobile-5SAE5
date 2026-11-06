import 'package:flutter/material.dart';

class SafeAvatar extends StatelessWidget {
  final String? imageUrl;
  final String name;
  final double radius;
  final Color backgroundColor;

  const SafeAvatar({
    Key? key,
    this.imageUrl,
    required this.name,
    this.radius = 24,
    this.backgroundColor = const Color(0xFFFF6B6B),
  }) : super(key: key);

  String _getInitials(String name) {
    return name
        .split(' ')
        .map((e) => e.isNotEmpty ? e[0].toUpperCase() : '')
        .take(2)
        .join();
  }

  @override
  Widget build(BuildContext context) {
    // Si pas d'URL ou URL vide, afficher initiales
    if (imageUrl == null || imageUrl!.isEmpty || imageUrl == 'null') {
      return CircleAvatar(
        radius: radius,
        backgroundColor: backgroundColor,
        child: Text(
          _getInitials(name),
          style: const TextStyle(
            color: Colors.white,
            fontWeight: FontWeight.bold,
            fontSize: 16,
          ),
        ),
      );
    }

    return CircleAvatar(
      radius: radius,
      backgroundColor: backgroundColor,
      backgroundImage: NetworkImage(imageUrl!),
      onBackgroundImageError: (exception, stackTrace) {
        // En cas d'erreur de chargement, l'icône est déjà affichée
      },
      child: Text(
        _getInitials(name),
        style: const TextStyle(
          color: Colors.white,
          fontWeight: FontWeight.bold,
          fontSize: 16,
        ),
      ),
    );
  }
}
