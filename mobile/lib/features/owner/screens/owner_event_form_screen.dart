import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:intl/intl.dart';
import '../../../core/theme/app_colors.dart';
import '../../../models/owner_event.dart';
import '../../../widgets/event_widgets.dart';
import '../providers/owner_provider.dart';

/// Add/edit form for a single event. Pushed via Navigator from
/// OwnerEventsScreen - not one of the module's bottom-nav tabs.
class OwnerEventFormScreen extends StatefulWidget {
  final OwnerEvent? event;

  const OwnerEventFormScreen({super.key, this.event});

  @override
  State<OwnerEventFormScreen> createState() => _OwnerEventFormScreenState();
}

class _OwnerEventFormScreenState extends State<OwnerEventFormScreen> {
  final _formKey = GlobalKey<FormState>();
  late final TextEditingController _titleController;
  late final TextEditingController _descriptionController;
  late final TextEditingController _imageController;
  late final TextEditingController _priceController;
  late final TextEditingController _spotsController;
  late String _category;
  final List<String> _addedTypes = [];
  late DateTime _date;
  late TimeOfDay _time;
  bool _isSaving = false;

  bool get _isEditing => widget.event != null;

  @override
  void initState() {
    super.initState();
    final event = widget.event;
    _titleController = TextEditingController(text: event?.title ?? '');
    _descriptionController = TextEditingController(text: event?.description ?? '');
    _imageController = TextEditingController(text: event?.image ?? '');
    _priceController = TextEditingController(text: event != null ? event.price.toString() : '0');
    _spotsController = TextEditingController(text: event != null ? event.availableSpots.toString() : '20');
    _category = event?.category ?? 'OTHER';

    final initialDateTime = event?.eventDate ?? DateTime.now().add(const Duration(days: 7, hours: 1));
    _date = DateTime(initialDateTime.year, initialDateTime.month, initialDateTime.day);
    _time = TimeOfDay(hour: initialDateTime.hour, minute: initialDateTime.minute);
  }

  @override
  void dispose() {
    _titleController.dispose();
    _descriptionController.dispose();
    _imageController.dispose();
    _priceController.dispose();
    _spotsController.dispose();
    super.dispose();
  }

  DateTime get _combinedDateTime =>
      DateTime(_date.year, _date.month, _date.day, _time.hour, _time.minute);

  Future<void> _pickDate() async {
    final date = await showDatePicker(
      context: context,
      initialDate: _date.isBefore(DateTime.now()) ? DateTime.now() : _date,
      firstDate: DateTime.now(),
      lastDate: DateTime.now().add(const Duration(days: 365)),
    );
    if (date != null) {
      setState(() {
        _date = date;
      });
    }
  }

  Future<void> _pickTime() async {
    final time = await showTimePicker(context: context, initialTime: _time);
    if (time != null) {
      setState(() {
        _time = time;
      });
    }
  }

  Future<void> _submit() async {
    if (!(_formKey.currentState?.validate() ?? false)) return;

    if (!_combinedDateTime.isAfter(DateTime.now())) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Event date/time must be in the future')),
      );
      return;
    }

    setState(() {
      _isSaving = true;
    });

    final event = OwnerEvent(
      id: widget.event?.id ?? '',
      restaurantId: widget.event?.restaurantId ?? '',
      restaurantName: widget.event?.restaurantName ?? '',
      restaurantImage: widget.event?.restaurantImage ?? '',
      title: _titleController.text.trim(),
      description: _descriptionController.text.trim(),
      image: _imageController.text.trim(),
      eventDate: _combinedDateTime,
      price: double.tryParse(_priceController.text.trim()) ?? 0,
      availableSpots: int.tryParse(_spotsController.text.trim()) ?? 0,
      bookedSpots: widget.event?.bookedSpots ?? 0,
      category: _category,
    );

    try {
      final ownerProvider = context.read<OwnerProvider>();
      if (_isEditing) {
        await ownerProvider.editEvent(widget.event!.id, event);
      } else {
        await ownerProvider.addEvent(event);
      }

      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(_isEditing ? 'Event updated' : 'Event created'),
            backgroundColor: AppColors.success,
          ),
        );
        Navigator.of(context).pop();
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text(e.toString().replaceAll('Exception: ', ''))),
        );
      }
    } finally {
      if (mounted) {
        setState(() {
          _isSaving = false;
        });
      }
    }
  }

  /// Built-in types, then custom types this restaurant already uses, then any
  /// created in this form (and the current value if it is custom).
  List<String> _availableTypes() {
    final existing = context.read<OwnerProvider>().events.map((e) => e.category);
    final custom = customEventCategories([...existing, ..._addedTypes, _category]);
    return [...eventCategoryOptions.map((o) => o.key), ...custom];
  }

  Future<void> _createType() async {
    final controller = TextEditingController();
    final created = await showDialog<String>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('New event type'),
        content: TextField(
          controller: controller,
          autofocus: true,
          maxLength: 40,
          textCapitalization: TextCapitalization.sentences,
          decoration: const InputDecoration(
            hintText: 'e.g. Karaoke Night, Brunch, Chef Table',
          ),
          onSubmitted: (value) => Navigator.pop(context, value),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: const Text('Cancel'),
          ),
          ElevatedButton(
            onPressed: () => Navigator.pop(context, controller.text),
            child: const Text('Use this type'),
          ),
        ],
      ),
    );
    controller.dispose();

    final name = created?.trim().replaceAll(RegExp(r'\s+'), ' ') ?? '';
    if (name.isEmpty) return;

    // Typing a built-in name ("wine tasting") selects that built-in instead of duplicating it.
    final asKey = name.toUpperCase().replaceAll(' ', '_');
    setState(() {
      if (isBuiltInEventCategory(asKey)) {
        _category = asKey;
      } else {
        final existing = _availableTypes().where((t) => t.toLowerCase() == name.toLowerCase());
        if (existing.isNotEmpty) {
          _category = existing.first;
        } else {
          _addedTypes.add(name);
          _category = name;
        }
      }
    });
  }

  Widget _newTypeChip() {
    return Material(
      color: AppColors.accent.withOpacity(0.12),
      borderRadius: BorderRadius.circular(24),
      child: InkWell(
        onTap: _createType,
        borderRadius: BorderRadius.circular(24),
        child: Container(
          padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 10),
          decoration: BoxDecoration(
            borderRadius: BorderRadius.circular(24),
            border: Border.all(color: AppColors.accent, style: BorderStyle.solid),
          ),
          child: const Row(
            mainAxisSize: MainAxisSize.min,
            children: [
              Icon(Icons.add, size: 16, color: AppColors.accent),
              SizedBox(width: 6),
              Text(
                'New type',
                style: TextStyle(fontSize: 13, fontWeight: FontWeight.w700, color: AppColors.accent),
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _categoryChip(String key) {
    final isSelected = _category == key;
    final option = eventCategoryOf(key);

    return Material(
      color: isSelected ? AppColors.primary : AppColors.surface,
      borderRadius: BorderRadius.circular(24),
      child: InkWell(
        onTap: () => setState(() => _category = key),
        borderRadius: BorderRadius.circular(24),
        child: Container(
          padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 10),
          decoration: BoxDecoration(
            borderRadius: BorderRadius.circular(24),
            border: Border.all(color: isSelected ? AppColors.primary : AppColors.border),
          ),
          child: Row(
            mainAxisSize: MainAxisSize.min,
            children: [
              Icon(
                option.icon,
                size: 16,
                color: isSelected ? AppColors.accentLight : AppColors.textSecondary,
              ),
              const SizedBox(width: 8),
              Text(
                option.label,
                style: TextStyle(
                  fontSize: 13,
                  fontWeight: FontWeight.w600,
                  color: isSelected ? Colors.white : AppColors.textPrimary,
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColors.background,
      appBar: AppBar(
        title: Text(_isEditing ? 'Edit Event' : 'Create Event'),
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(20),
        child: Form(
          key: _formKey,
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              TextFormField(
                controller: _titleController,
                decoration: const InputDecoration(labelText: 'Title'),
                validator: (v) => (v == null || v.trim().isEmpty) ? 'Title is required' : null,
              ),
              const SizedBox(height: 16),
              TextFormField(
                controller: _descriptionController,
                maxLines: 3,
                decoration: const InputDecoration(
                  labelText: 'Description',
                  alignLabelWithHint: true,
                ),
              ),
              const SizedBox(height: 16),
              TextFormField(
                controller: _imageController,
                decoration: const InputDecoration(labelText: 'Image URL'),
                keyboardType: TextInputType.url,
              ),
              const SizedBox(height: 16),
              const Text(
                'Type of event',
                style: TextStyle(fontSize: 15, fontWeight: FontWeight.bold, color: AppColors.textPrimary),
              ),
              const SizedBox(height: 10),
              Wrap(
                spacing: 10,
                runSpacing: 10,
                children: [
                  for (final key in _availableTypes()) _categoryChip(key),
                  _newTypeChip(),
                ],
              ),
              const SizedBox(height: 6),
              const Text(
                'Pick a type or create your own - you are not limited to the list.',
                style: TextStyle(fontSize: 12, color: AppColors.textSecondary),
              ),
              const SizedBox(height: 16),
              Row(
                children: [
                  Expanded(
                    child: InkWell(
                      onTap: _pickDate,
                      child: InputDecorator(
                        decoration: const InputDecoration(labelText: 'Date'),
                        child: Row(
                          children: [
                            const Icon(Icons.calendar_today, size: 18, color: AppColors.accent),
                            const SizedBox(width: 8),
                            Text(DateFormat('MMM dd, yyyy').format(_date)),
                          ],
                        ),
                      ),
                    ),
                  ),
                  const SizedBox(width: 12),
                  Expanded(
                    child: InkWell(
                      onTap: _pickTime,
                      child: InputDecorator(
                        decoration: const InputDecoration(labelText: 'Time'),
                        child: Row(
                          children: [
                            const Icon(Icons.access_time, size: 18, color: AppColors.accent),
                            const SizedBox(width: 8),
                            Text(_time.format(context)),
                          ],
                        ),
                      ),
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 16),
              Row(
                children: [
                  Expanded(
                    child: TextFormField(
                      controller: _priceController,
                      decoration: const InputDecoration(labelText: 'Price', prefixText: '\$ '),
                      keyboardType: const TextInputType.numberWithOptions(decimal: true),
                      validator: (v) {
                        if (v == null || v.trim().isEmpty) return 'Required';
                        final parsed = double.tryParse(v.trim());
                        if (parsed == null || parsed < 0) return 'Invalid';
                        return null;
                      },
                    ),
                  ),
                  const SizedBox(width: 12),
                  Expanded(
                    child: TextFormField(
                      controller: _spotsController,
                      decoration: const InputDecoration(labelText: 'Available Spots'),
                      keyboardType: TextInputType.number,
                      validator: (v) {
                        if (v == null || v.trim().isEmpty) return 'Required';
                        final parsed = int.tryParse(v.trim());
                        if (parsed == null || parsed <= 0) return 'Invalid';
                        return null;
                      },
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 32),
              SizedBox(
                width: double.infinity,
                height: 52,
                child: ElevatedButton(
                  onPressed: _isSaving ? null : _submit,
                  child: _isSaving
                      ? const SizedBox(
                          height: 20,
                          width: 20,
                          child: CircularProgressIndicator(strokeWidth: 2, color: AppColors.surface),
                        )
                      : Text(_isEditing ? 'Save Changes' : 'Create Event'),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
