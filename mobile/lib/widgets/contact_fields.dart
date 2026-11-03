import 'package:flutter/material.dart';

final RegExp _phonePattern = RegExp(r'^[+]?[0-9 ()-]{6,20}$');

String? validateContactName(String? value) {
  final name = value?.trim() ?? '';
  if (name.isEmpty) return 'Your name is required';
  if (name.length < 2) return 'Enter your full name';
  if (name.length > 80) return 'Name is too long';
  return null;
}

String? validateContactPhone(String? value) {
  final phone = value?.trim() ?? '';
  if (phone.isEmpty) return 'Your phone number is required';
  if (!_phonePattern.hasMatch(phone)) return 'Enter a valid phone number';
  return null;
}

/// The name + phone number every reservation or event registration must carry,
/// so the restaurant knows who to expect and how to reach them.
class ContactFields extends StatelessWidget {
  final TextEditingController nameController;
  final TextEditingController phoneController;

  const ContactFields({
    super.key,
    required this.nameController,
    required this.phoneController,
  });

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        TextFormField(
          controller: nameController,
          textCapitalization: TextCapitalization.words,
          textInputAction: TextInputAction.next,
          decoration: const InputDecoration(
            labelText: 'Full name *',
            prefixIcon: Icon(Icons.person_outline),
          ),
          validator: validateContactName,
        ),
        const SizedBox(height: 14),
        TextFormField(
          controller: phoneController,
          keyboardType: TextInputType.phone,
          textInputAction: TextInputAction.next,
          decoration: const InputDecoration(
            labelText: 'Phone number *',
            hintText: '+216 20 123 456',
            prefixIcon: Icon(Icons.phone_outlined),
          ),
          validator: validateContactPhone,
        ),
      ],
    );
  }
}
