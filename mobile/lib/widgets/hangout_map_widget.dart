import 'package:flutter/material.dart';
import 'package:flutter_map/flutter_map.dart';
import 'package:latlong2/latlong.dart';
import 'package:hangout_app/models/hangout.dart';

class HangOutMapWidget extends StatefulWidget {
  final HangOut hangOut;
  final double? initialZoom;

  const HangOutMapWidget({
    Key? key,
    required this.hangOut,
    this.initialZoom = 15.0,
  }) : super(key: key);

  @override
  State<HangOutMapWidget> createState() => _HangOutMapWidgetState();
}

class _HangOutMapWidgetState extends State<HangOutMapWidget> {
  late MapController mapController;
  List<Marker> markers = [];
  LatLng? restaurantLocation;
  LatLng? meetingPointLocation;

  @override
  void initState() {
    super.initState();
    mapController = MapController();
    _initializeMarkers();
  }

  void _initializeMarkers() {
    markers.clear();

    // Set restaurant location (red marker)
    if (widget.hangOut.restaurantLatitude != null &&
        widget.hangOut.restaurantLongitude != null) {
      restaurantLocation = LatLng(
        widget.hangOut.restaurantLatitude!,
        widget.hangOut.restaurantLongitude!,
      );

      markers.add(
        Marker(
          point: restaurantLocation!,
          width: 80,
          height: 80,
          child: GestureDetector(
            onTap: () => _showMarkerDialog('Restaurant', widget.hangOut.restaurantName),
            child: Column(
              children: [
                Container(
                  decoration: BoxDecoration(
                    color: Colors.red,
                    shape: BoxShape.circle,
                    boxShadow: [
                      BoxShadow(
                        color: Colors.red.withOpacity(0.5),
                        blurRadius: 8,
                        spreadRadius: 2,
                      ),
                    ],
                  ),
                  padding: const EdgeInsets.all(8),
                  child: const Icon(
                    Icons.restaurant,
                    color: Colors.white,
                    size: 24,
                  ),
                ),
                const SizedBox(height: 4),
                Container(
                  padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                  decoration: BoxDecoration(
                    color: Colors.red,
                    borderRadius: BorderRadius.circular(12),
                  ),
                  child: const Text(
                    'Restaurant',
                    style: TextStyle(
                      color: Colors.white,
                      fontSize: 10,
                      fontWeight: FontWeight.bold,
                    ),
                  ),
                ),
              ],
            ),
          ),
        ),
      );
    }

    // Set meeting point (green marker)
    if (widget.hangOut.meetingPointLatitude != null &&
        widget.hangOut.meetingPointLongitude != null) {
      meetingPointLocation = LatLng(
        widget.hangOut.meetingPointLatitude!,
        widget.hangOut.meetingPointLongitude!,
      );

      markers.add(
        Marker(
          point: meetingPointLocation!,
          width: 80,
          height: 80,
          child: GestureDetector(
            onTap: () => _showMarkerDialog(
              'Meeting Point',
              widget.hangOut.meetingPointName ?? 'Meeting Point',
            ),
            child: Column(
              children: [
                Container(
                  decoration: BoxDecoration(
                    color: Colors.green,
                    shape: BoxShape.circle,
                    boxShadow: [
                      BoxShadow(
                        color: Colors.green.withOpacity(0.5),
                        blurRadius: 8,
                        spreadRadius: 2,
                      ),
                    ],
                  ),
                  padding: const EdgeInsets.all(8),
                  child: const Icon(
                    Icons.location_on,
                    color: Colors.white,
                    size: 24,
                  ),
                ),
                const SizedBox(height: 4),
                Container(
                  padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                  decoration: BoxDecoration(
                    color: Colors.green,
                    borderRadius: BorderRadius.circular(12),
                  ),
                  child: const Text(
                    'Meeting Point',
                    style: TextStyle(
                      color: Colors.white,
                      fontSize: 10,
                      fontWeight: FontWeight.bold,
                    ),
                  ),
                ),
              ],
            ),
          ),
        ),
      );
    }

    setState(() {});
  }

  void _showMarkerDialog(String title, String description) {
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: Text(title),
        content: Text(description),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: const Text('Close'),
          ),
        ],
      ),
    );
  }

  void _fitBounds() {
    if (markers.isEmpty) return;

    if (markers.length == 1) {
      // Center on single marker
      mapController.move(markers.first.point, 15.0);
    } else if (restaurantLocation != null && meetingPointLocation != null) {
      // Calculate bounds to fit both markers
      final northLat = restaurantLocation!.latitude > meetingPointLocation!.latitude
          ? restaurantLocation!.latitude
          : meetingPointLocation!.latitude;
      final southLat = restaurantLocation!.latitude < meetingPointLocation!.latitude
          ? restaurantLocation!.latitude
          : meetingPointLocation!.latitude;
      final eastLng = restaurantLocation!.longitude > meetingPointLocation!.longitude
          ? restaurantLocation!.longitude
          : meetingPointLocation!.longitude;
      final westLng = restaurantLocation!.longitude < meetingPointLocation!.longitude
          ? restaurantLocation!.longitude
          : meetingPointLocation!.longitude;

      // Calculate center and zoom
      final centerLat = (northLat + southLat) / 2;
      final centerLng = (eastLng + westLng) / 2;
      final center = LatLng(centerLat, centerLng);

      // Calculate appropriate zoom level
      final latDiff = northLat - southLat;
      final lngDiff = eastLng - westLng;
      final maxDiff = latDiff > lngDiff ? latDiff : lngDiff;

      double zoom = 15.0;
      if (maxDiff > 0) {
        // Use a simple formula to calculate zoom based on distance
        zoom = (16 - (maxDiff * 50).toDouble()).clamp(5.0, 18.0);
      }

      mapController.move(center, zoom.clamp(5.0, 18.0));
    }
  }

  @override
  Widget build(BuildContext context) {
    // If no coordinates available, show placeholder
    if (restaurantLocation == null) {
      return Container(
        height: 300,
        decoration: BoxDecoration(
          color: Colors.grey[200],
          borderRadius: BorderRadius.circular(12),
        ),
        child: const Center(
          child: Text(
            'Map location not available',
            style: TextStyle(color: Colors.grey),
          ),
        ),
      );
    }

    return Column(
      children: [
        ClipRRect(
          borderRadius: BorderRadius.circular(12),
          child: SizedBox(
            height: 300,
            child: FlutterMap(
              mapController: mapController,
              options: MapOptions(
                initialCenter: restaurantLocation!,
                initialZoom: widget.initialZoom!,
                minZoom: 5.0,
                maxZoom: 18.0,
              ),
              children: [
                // OpenStreetMap tiles
                TileLayer(
                  urlTemplate: 'https://tile.openstreetmap.org/{z}/{x}/{y}.png',
                  userAgentPackageName: 'com.hangout.app',
                  // Fallback tiles
                  fallbackUrl: 'https://tile.openstreetmap.org/{z}/{x}/{y}.png',
                ),
                // Markers layer
                MarkerLayer(
                  markers: markers,
                ),
              ],
            ),
          ),
        ),
        const SizedBox(height: 16),
        _buildLocationInfo(),
      ],
    );
  }

  Widget _buildLocationInfo() {
    return Card(
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
      child: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // Restaurant info
            Row(
              children: [
                Icon(Icons.restaurant, color: Colors.red, size: 20),
                const SizedBox(width: 8),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      const Text(
                        'Restaurant',
                        style: TextStyle(
                          fontSize: 12,
                          color: Colors.grey,
                          fontWeight: FontWeight.w500,
                        ),
                      ),
                      Text(
                        widget.hangOut.restaurantName,
                        style: const TextStyle(
                          fontSize: 14,
                          fontWeight: FontWeight.bold,
                        ),
                      ),
                    ],
                  ),
                ),
              ],
            ),
            const SizedBox(height: 12),

            // Meeting point info (if available)
            if (meetingPointLocation != null)
              Column(
                children: [
                  const Divider(),
                  const SizedBox(height: 12),
                  Row(
                    children: [
                      Icon(Icons.location_on, color: Colors.green, size: 20),
                      const SizedBox(width: 8),
                      Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            const Text(
                              'Meeting Point',
                              style: TextStyle(
                                fontSize: 12,
                                color: Colors.grey,
                                fontWeight: FontWeight.w500,
                              ),
                            ),
                            Text(
                              widget.hangOut.meetingPointName ?? 'Meeting Point',
                              style: const TextStyle(
                                fontSize: 14,
                                fontWeight: FontWeight.bold,
                              ),
                            ),
                          ],
                        ),
                      ),
                    ],
                  ),
                ],
              ),
          ],
        ),
      ),
    );
  }

  @override
  void didUpdateWidget(HangOutMapWidget oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (oldWidget.hangOut.id != widget.hangOut.id) {
      _initializeMarkers();
      _fitBounds();
    }
  }

  @override
  void dispose() {
    mapController.dispose();
    super.dispose();
  }
}
