import 'package:flutter/material.dart';
import 'package:google_maps_flutter/google_maps_flutter.dart';
import 'package:hangout_app/services/map_service.dart';

class MeetingPointData {
  final String? name;
  final double latitude;
  final double longitude;

  MeetingPointData({
    this.name,
    required this.latitude,
    required this.longitude,
  });
}

class MeetingPointPicker extends StatefulWidget {
  final LatLng initialCenter;
  final String restaurantName;
  final Function(MeetingPointData) onMeetingPointSelected;

  const MeetingPointPicker({
    Key? key,
    required this.initialCenter,
    required this.restaurantName,
    required this.onMeetingPointSelected,
  }) : super(key: key);

  @override
  State<MeetingPointPicker> createState() => _MeetingPointPickerState();
}

class _MeetingPointPickerState extends State<MeetingPointPicker> {
  late GoogleMapController mapController;
  LatLng? selectedLocation;
  Marker? selectedMarker;
  TextEditingController meetingPointNameController = TextEditingController();

  @override
  void initState() {
    super.initState();
    selectedLocation = widget.initialCenter;
    _createMarker();
  }

  void _createMarker() {
    if (selectedLocation != null) {
      selectedMarker = MapService.createMeetingPointMarker(
        markerId: 'selected_meeting_point',
        position: selectedLocation!,
        title: meetingPointNameController.text.isEmpty
            ? 'Meeting Point'
            : meetingPointNameController.text,
        snippet: '${selectedLocation!.latitude}, ${selectedLocation!.longitude}',
      );
    }
  }

  void _onMapTap(LatLng position) {
    setState(() {
      selectedLocation = position;
      _createMarker();
    });
  }

  void _onMapCreated(GoogleMapController controller) {
    mapController = controller;
  }

  void _confirmSelection() {
    if (selectedLocation != null) {
      final meetingPoint = MeetingPointData(
        name: meetingPointNameController.text.isEmpty
            ? 'Meeting Point'
            : meetingPointNameController.text,
        latitude: selectedLocation!.latitude,
        longitude: selectedLocation!.longitude,
      );
      widget.onMeetingPointSelected(meetingPoint);
      Navigator.pop(context);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Select Meeting Point'),
        centerTitle: true,
        elevation: 0,
      ),
      body: Column(
        children: [
          // Map
          Expanded(
            child: GoogleMap(
              onMapCreated: _onMapCreated,
              initialCameraPosition: CameraPosition(
                target: widget.initialCenter,
                zoom: 16.0,
              ),
              markers: selectedMarker != null ? {selectedMarker!} : {},
              onTap: _onMapTap,
              compassEnabled: true,
              zoomControlsEnabled: true,
            ),
          ),

          // Bottom panel with name input
          Container(
            padding: EdgeInsets.only(
              left: 16,
              right: 16,
              top: 16,
              bottom: MediaQuery.of(context).viewInsets.bottom + 16,
            ),
            decoration: BoxDecoration(
              color: Colors.white,
              boxShadow: [
                BoxShadow(
                  color: Colors.black.withOpacity(0.1),
                  blurRadius: 10,
                  offset: const Offset(0, -2),
                ),
              ],
            ),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Text(
                  'Meeting Point Name',
                  style: TextStyle(
                    fontSize: 14,
                    fontWeight: FontWeight.w500,
                    color: Colors.grey,
                  ),
                ),
                const SizedBox(height: 8),
                TextField(
                  controller: meetingPointNameController,
                  decoration: InputDecoration(
                    hintText: 'e.g., Main Entrance, Near the statue...',
                    border: OutlineInputBorder(
                      borderRadius: BorderRadius.circular(8),
                    ),
                    contentPadding: const EdgeInsets.symmetric(
                      horizontal: 12,
                      vertical: 10,
                    ),
                  ),
                  onChanged: (_) {
                    _createMarker();
                    setState(() {});
                  },
                ),
                const SizedBox(height: 16),
                if (selectedLocation != null)
                  Padding(
                    padding: const EdgeInsets.only(bottom: 16),
                    child: Text(
                      'Coordinates: ${selectedLocation!.latitude.toStringAsFixed(4)}, ${selectedLocation!.longitude.toStringAsFixed(4)}',
                      style: TextStyle(
                        fontSize: 12,
                        color: Colors.grey[600],
                      ),
                    ),
                  ),
                SizedBox(
                  width: double.infinity,
                  child: ElevatedButton(
                    onPressed: _confirmSelection,
                    style: ElevatedButton.styleFrom(
                      backgroundColor: Colors.green,
                      padding: const EdgeInsets.symmetric(vertical: 12),
                      shape: RoundedRectangleBorder(
                        borderRadius: BorderRadius.circular(8),
                      ),
                    ),
                    child: const Text(
                      'Confirm Meeting Point',
                      style: TextStyle(
                        fontSize: 14,
                        fontWeight: FontWeight.bold,
                        color: Colors.white,
                      ),
                    ),
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  @override
  void dispose() {
    mapController.dispose();
    meetingPointNameController.dispose();
    super.dispose();
  }
}
