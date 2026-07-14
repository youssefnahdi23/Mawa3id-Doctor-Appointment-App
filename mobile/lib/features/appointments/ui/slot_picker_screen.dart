import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:intl/intl.dart';

import '../../../core/l10n/error_l10n.dart';
import '../../../core/l10n/l10n.dart';
import '../../../core/network/api_exception.dart';
import '../../../core/widgets/error_view.dart';
import '../data/appointment_models.dart';
import '../data/appointment_repository.dart';
import '../state/appointments_providers.dart';

class SlotPickerScreen extends ConsumerStatefulWidget {
  const SlotPickerScreen({super.key, required this.doctorId});

  final int doctorId;

  @override
  ConsumerState<SlotPickerScreen> createState() => _SlotPickerScreenState();
}

class _SlotPickerScreenState extends ConsumerState<SlotPickerScreen> {
  DateTime? _selectedDay;

  Future<void> _pickSlot(SlotResponse slot) async {
    final booked = await showModalBottomSheet<AppointmentResponse>(
      context: context,
      isScrollControlled: true,
      builder: (_) =>
          _BookingConfirmSheet(doctorId: widget.doctorId, slot: slot),
    );
    if (booked == null || !mounted) return;
    // A conflicting booking (409) already refreshed the slots inside the
    // sheet; success lands here.
    final l10n = context.l10n;
    ScaffoldMessenger.of(context).showSnackBar(SnackBar(
      content: Text(booked.status == AppointmentStatus.accepted
          ? l10n.bookingConfirmed
          : l10n.bookingPending),
    ));
    context.go('/p/appointments');
  }

  @override
  Widget build(BuildContext context) {
    final l10n = context.l10n;
    final slots = ref.watch(slotsProvider(widget.doctorId));
    final locale = Localizations.localeOf(context).toString();
    return Scaffold(
      appBar: AppBar(title: Text(l10n.chooseSlotTitle)),
      body: slots.when(
        loading: () => const Center(child: CircularProgressIndicator()),
        error: (error, _) => ErrorView(
          error: error,
          onRetry: () => ref.invalidate(slotsProvider(widget.doctorId)),
        ),
        data: (byDay) {
          if (byDay.isEmpty) {
            return Center(
              child: Padding(
                padding: const EdgeInsets.all(24),
                child: Text(l10n.noSlotsAvailable,
                    textAlign: TextAlign.center),
              ),
            );
          }
          final days = byDay.keys.toList()..sort();
          final selectedDay =
              _selectedDay != null && byDay.containsKey(_selectedDay)
                  ? _selectedDay!
                  : days.first;
          final daySlots = byDay[selectedDay]!;
          return Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              SizedBox(
                height: 64,
                child: ListView(
                  scrollDirection: Axis.horizontal,
                  padding:
                      const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
                  children: [
                    for (final day in days)
                      Padding(
                        padding:
                            const EdgeInsetsDirectional.only(end: 8),
                        child: ChoiceChip(
                          label: Text(DateFormat.MMMEd(locale).format(day)),
                          selected: day == selectedDay,
                          onSelected: (_) =>
                              setState(() => _selectedDay = day),
                        ),
                      ),
                  ],
                ),
              ),
              Expanded(
                child: SingleChildScrollView(
                  padding: const EdgeInsets.all(16),
                  child: Wrap(
                    spacing: 8,
                    runSpacing: 8,
                    children: [
                      for (final slot in daySlots)
                        OutlinedButton(
                          onPressed: () => _pickSlot(slot),
                          child:
                              Text(DateFormat.Hm(locale).format(slot.start)),
                        ),
                    ],
                  ),
                ),
              ),
            ],
          );
        },
      ),
    );
  }
}

class _BookingConfirmSheet extends ConsumerStatefulWidget {
  const _BookingConfirmSheet({required this.doctorId, required this.slot});

  final int doctorId;
  final SlotResponse slot;

  @override
  ConsumerState<_BookingConfirmSheet> createState() =>
      _BookingConfirmSheetState();
}

class _BookingConfirmSheetState extends ConsumerState<_BookingConfirmSheet> {
  final _reason = TextEditingController();
  String? _error;
  bool _submitting = false;

  @override
  void dispose() {
    _reason.dispose();
    super.dispose();
  }

  Future<void> _confirm() async {
    setState(() {
      _submitting = true;
      _error = null;
    });
    try {
      final booked = await ref.read(appointmentRepositoryProvider).book(
            doctorId: widget.doctorId,
            startTime: widget.slot.start,
            reason: _reason.text.trim(),
          );
      if (mounted) Navigator.of(context).pop(booked);
    } on ApiException catch (e) {
      if (!mounted) return;
      if (e.kind == ApiErrorKind.conflict) {
        // Someone else took the slot: refresh the picker behind the sheet.
        ref.invalidate(slotsProvider(widget.doctorId));
        setState(() => _error = context.l10n.errorSlotTaken);
      } else {
        setState(() => _error = localizedErrorMessage(context, e));
      }
    } finally {
      if (mounted) setState(() => _submitting = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final l10n = context.l10n;
    final locale = Localizations.localeOf(context).toString();
    final slot = widget.slot;
    final when = '${DateFormat.yMMMEd(locale).format(slot.start)} · '
        '${DateFormat.Hm(locale).format(slot.start)}–'
        '${DateFormat.Hm(locale).format(slot.end)}';
    return Padding(
      padding: EdgeInsets.only(
          bottom: MediaQuery.of(context).viewInsets.bottom),
      child: Padding(
        padding: const EdgeInsets.all(24),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Text(l10n.confirmBookingTitle,
                style: Theme.of(context).textTheme.titleLarge),
            const SizedBox(height: 8),
            Text(when, style: Theme.of(context).textTheme.bodyLarge),
            const SizedBox(height: 16),
            TextField(
              controller: _reason,
              decoration: InputDecoration(
                labelText: l10n.reasonOptional,
                border: const OutlineInputBorder(),
              ),
              maxLength: 500,
              maxLines: 2,
            ),
            if (_error != null)
              Padding(
                padding: const EdgeInsets.only(bottom: 8),
                child: Text(
                  _error!,
                  style:
                      TextStyle(color: Theme.of(context).colorScheme.error),
                ),
              ),
            FilledButton(
              onPressed: _submitting ? null : _confirm,
              child: _submitting
                  ? const SizedBox(
                      height: 20,
                      width: 20,
                      child: CircularProgressIndicator(strokeWidth: 2))
                  : Text(l10n.confirmBooking),
            ),
          ],
        ),
      ),
    );
  }
}
