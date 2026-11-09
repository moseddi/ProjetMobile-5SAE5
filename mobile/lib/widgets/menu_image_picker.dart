import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:image_picker/image_picker.dart';

import '../core/theme/app_colors.dart';

/// Max size of the picked image, in bytes. The image is stored as text in
/// MongoDB and sent with every restaurant list, so it must stay small.
const int kMaxMenuImageBytes = 600 * 1024;

/// Shows a menu image whatever its form: a `data:image/...;base64,...` value
/// (picked from the device) or a normal http(s) URL (older data).
Widget menuImageView(
  String value, {
  double? width,
  double? height,
  BoxFit fit = BoxFit.cover,
  Widget? error,
}) {
  final fallback = error ??
      Container(
        width: width,
        height: height,
        padding: const EdgeInsets.all(20),
        color: AppColors.surface,
        child: const Text('Menu image unavailable'),
      );

  final v = value.trim();
  if (v.startsWith('data:')) {
    try {
      final bytes = base64Decode(v.substring(v.indexOf(',') + 1));
      return Image.memory(
        bytes,
        width: width,
        height: height,
        fit: fit,
        errorBuilder: (_, __, ___) => fallback,
      );
    } catch (_) {
      return fallback;
    }
  }
  return Image.network(
    v,
    width: width,
    height: height,
    fit: fit,
    errorBuilder: (_, __, ___) => fallback,
  );
}

/// Lets the owner choose the menu picture from the device (gallery / file
/// picker on web). Reports the result as a data URL through [onChanged];
/// an empty string means "no image".
class MenuImagePicker extends StatefulWidget {
  final String value;
  final ValueChanged<String> onChanged;

  const MenuImagePicker({super.key, required this.value, required this.onChanged});

  @override
  State<MenuImagePicker> createState() => _MenuImagePickerState();
}

class _MenuImagePickerState extends State<MenuImagePicker> {
  String? _error;

  Future<void> _pick() async {
    try {
      final picked = await ImagePicker().pickImage(
        source: ImageSource.gallery,
        maxWidth: 1400,
        imageQuality: 75,
      );
      if (picked == null) return;

      final bytes = await picked.readAsBytes();
      if (bytes.length > kMaxMenuImageBytes) {
        setState(() => _error = 'Image too large (max 600 KB). Choose a smaller one.');
        return;
      }

      final mime = (picked.mimeType != null && picked.mimeType!.startsWith('image/'))
          ? picked.mimeType!
          : 'image/jpeg';
      setState(() => _error = null);
      widget.onChanged('data:$mime;base64,${base64Encode(bytes)}');
    } catch (_) {
      setState(() => _error = 'Could not read this image. Try another one.');
    }
  }

  @override
  Widget build(BuildContext context) {
    final hasImage = widget.value.trim().isNotEmpty;

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        SizedBox(
          width: double.infinity,
          height: 52,
          child: OutlinedButton.icon(
            onPressed: _pick,
            icon: const Icon(Icons.restaurant_menu),
            label: Text(hasImage ? 'Change menu image' : 'Choose menu image from device (optional)'),
          ),
        ),
        if (_error != null)
          Padding(
            padding: const EdgeInsets.only(top: 6),
            child: Text(_error!, style: const TextStyle(color: AppColors.error, fontSize: 12.5)),
          ),
        if (hasImage)
          Padding(
            padding: const EdgeInsets.only(top: 10),
            child: Stack(
              children: [
                ClipRRect(
                  borderRadius: BorderRadius.circular(14),
                  child: menuImageView(widget.value, width: double.infinity, height: 180),
                ),
                Positioned(
                  top: 6,
                  right: 6,
                  child: Material(
                    color: Colors.black54,
                    shape: const CircleBorder(),
                    child: IconButton(
                      icon: const Icon(Icons.close, color: Colors.white, size: 18),
                      tooltip: 'Remove',
                      onPressed: () {
                        setState(() => _error = null);
                        widget.onChanged('');
                      },
                    ),
                  ),
                ),
              ],
            ),
          ),
      ],
    );
  }
}